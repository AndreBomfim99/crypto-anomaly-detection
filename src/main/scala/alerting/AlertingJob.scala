package alerting

import com.typesafe.scalalogging.LazyLogging
import config.AppConfig
import processing.SparkProcessor
import org.apache.spark.sql.functions._

object AlertingJob extends LazyLogging {
  
  def main(args: Array[String]): Unit = {
    logger.info("="*80)
    logger.info("CRYPTO ANOMALY DETECTION - ALERTING JOB")
    logger.info("="*80)
    
    val spark = SparkProcessor.createSparkSession("CryptoAnomalyAlerting")
    val notifier = SNSNotifier()
    
    try {
      // Test SNS connection
      if (!notifier.testConnection()) {
        logger.error("SNS connection failed. Aborting alerting job.")
        return
      }
      
      // Read recent anomalies from S3
      val anomaliesPath = s"s3a://${AppConfig.AWS.S3.bucketProcessed}/anomalies"
      logger.info(s"Reading anomalies from: $anomaliesPath")
      
      val anomalies = spark.read
        .parquet(anomaliesPath)
        .filter(col("detection_date") === current_date())
        .orderBy(col("detected_at").desc)
      
      val count = anomalies.count()
      logger.info(s"Found $count anomalies today")
      
      if (count > 0) {
        // Convert to Alert case class
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
        
        // Send alerts
        logger.info(s"Sending ${alerts.size} alerts via SNS...")
        val (success, failure) = notifier.sendBatchAlerts(alerts)
        
        logger.info("="*80)
        logger.info("ALERTING SUMMARY")
        logger.info("="*80)
        logger.info(s"Total anomalies: ${alerts.size}")
        logger.info(s"Alerts sent successfully: $success")
        logger.info(s"Alerts failed: $failure")
        logger.info("="*80)
        
        // Display details
        logger.info("\nAlerts sent:")
        alerts.foreach { alert =>
          logger.info(f"  ${alert.cryptoId}%15s | ${alert.anomalyType}%20s | " +
            f"${alert.severity}%8s | ${alert.description}")
        }
        
      } else {
        logger.info("No anomalies detected today. No alerts to send.")
      }
      
    } catch {
      case e: Exception =>
        logger.error("Alerting job failed", e)
        throw e
    } finally {
      notifier.close()
      SparkProcessor.stopSparkSession(spark)
    }
    
    logger.info("="*80)
    logger.info("ALERTING JOB COMPLETED")
    logger.info("="*80)
  }
}
