package processing

import alerting.{Alert, SNSNotifier}
import com.typesafe.scalalogging.LazyLogging
import config.AppConfig
import monitoring.{MetricsCollector, PipelineMetrics}

object ProcessingJobWithMetrics extends LazyLogging {
  
  def main(args: Array[String]): Unit = {
    logger.info("="*80)
    logger.info("CRYPTO ANOMALY DETECTION - FULL PIPELINE WITH METRICS")
    logger.info("="*80)
    
    val startTime = System.currentTimeMillis()
    val spark = SparkProcessor.createSparkSession("CryptoAnomalyDetection")
    val notifier = SNSNotifier()
    val metricsCollector = MetricsCollector()
    
    var success = true
    var errorMessage: Option[String] = None
    var recordsProcessed = 0L
    var anomaliesDetected = 0L
    var alertsSent = 0
    
    try {
      val rawDataPath = s"s3a://${AppConfig.AWS.S3.bucketRaw}/*/*/*/*/*"
      val processedPath = s"s3a://${AppConfig.AWS.S3.bucketProcessed}/metrics"
      val anomaliesPath = s"s3a://${AppConfig.AWS.S3.bucketProcessed}/anomalies"
      
      logger.info("Step 1: Transform raw data")
      val transformed = DataTransformer.transform(spark, rawDataPath)
      transformed.cache()
      recordsProcessed = transformed.count()
      
      logger.info("Step 2: Save processed metrics")
      transformed.write
        .mode("overwrite")
        .partitionBy("collection_date", "crypto_id")
        .parquet(processedPath)
      
      logger.info("Step 3: Detect anomalies")
      val anomalies = AnomalyDetector.detectAll(transformed)
      anomaliesDetected = anomalies.count()
      
      logger.info("Step 4: Save anomalies")
      AnomalyDetector.saveAnomalies(anomalies, anomaliesPath)
      
      logger.info("Step 5: Send alerts")
      if (anomaliesDetected > 0) {
        val alerts = anomalies.collect().map { row =>
          Alert(
            cryptoId = row.getAs[String]("crypto_id"),
            anomalyType = row.getAs[String]("anomaly_type"),
            severity = row.getAs[String]("severity"),
            currentPrice = row.getAs[Double]("current_price"),
            variationPct = row.getAs[Double]("variation_pct"),
            description = row.getAs[String]("description"),
            detectedAt = row.getAs[Long]("detected_at")
          )
        }.toList
        
        val (successCount, _) = notifier.sendBatchAlerts(alerts)
        alertsSent = successCount
      }
      
      transformed.unpersist()
      
    } catch {
      case e: Exception =>
        success = false
        errorMessage = Some(e.getMessage)
        logger.error("Pipeline failed", e)
        throw e
    } finally {
      val executionTime = System.currentTimeMillis() - startTime
      
      // Record metrics
      val pipelineMetrics = PipelineMetrics(
        timestamp = System.currentTimeMillis() / 1000,
        pipelineStage = "full_pipeline",
        cryptosProcessed = AppConfig.CoinGecko.cryptoIds.size,
        recordsProcessed = recordsProcessed,
        anomaliesDetected = anomaliesDetected,
        alertsSent = alertsSent,
        executionTimeMs = executionTime,
        success = success,
        errorMessage = errorMessage
      )
      
      metricsCollector.recordPipelineMetrics(pipelineMetrics)
      
      // Record system metrics
      val sysMetrics = metricsCollector.collectSystemMetrics()
      metricsCollector.recordSystemMetrics(sysMetrics)
      
      notifier.close()
      SparkProcessor.stopSparkSession(spark)
    }
    
    logger.info("="*80)
    logger.info("PIPELINE COMPLETED")
    logger.info(s"Records processed: $recordsProcessed")
    logger.info(s"Anomalies detected: $anomaliesDetected")
    logger.info(s"Alerts sent: $alertsSent")
    logger.info("="*80)
  }
}
