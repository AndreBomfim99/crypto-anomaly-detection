package processing

import com.typesafe.scalalogging.LazyLogging
import config.AppConfig

object ProcessingJob extends LazyLogging {
  
  def main(args: Array[String]): Unit = {
    logger.info("="*80)
    logger.info("CRYPTO ANOMALY DETECTION - SPARK PROCESSING JOB")
    logger.info("="*80)
    
    val spark = SparkProcessor.createSparkSession("CryptoAnomalyDetection")
    
    try {
      val rawDataPath = s"s3a://${AppConfig.AWS.S3.bucketRaw}/*/*/*/*/*"
      val processedPath = s"s3a://${AppConfig.AWS.S3.bucketProcessed}/metrics"
      val anomaliesPath = s"s3a://${AppConfig.AWS.S3.bucketProcessed}/anomalies"
      
      logger.info(s"Raw data path: $rawDataPath")
      logger.info(s"Processed path: $processedPath")
      logger.info(s"Anomalies path: $anomaliesPath")
      
      logger.info("Step 1: Transform raw data")
      val transformed = DataTransformer.transform(spark, rawDataPath)
      transformed.cache()
      
      logger.info("Step 2: Save processed metrics")
      transformed.write
        .mode("overwrite")
        .partitionBy("collection_date", "crypto_id")
        .parquet(processedPath)
      
      logger.info("Step 3: Detect anomalies")
      val anomalies = AnomalyDetector.detectAll(transformed)
      
      logger.info("Step 4: Save anomalies")
      AnomalyDetector.saveAnomalies(anomalies, anomaliesPath)
      
      logger.info("Step 5: Display summary")
      displaySummary(transformed, anomalies)
      
      transformed.unpersist()
      
    } catch {
      case e: Exception =>
        logger.error("Processing job failed", e)
        throw e
    } finally {
      SparkProcessor.stopSparkSession(spark)
    }
    
    logger.info("="*80)
    logger.info("PROCESSING JOB COMPLETED")
    logger.info("="*80)
  }
  
  private def displaySummary(metrics: org.apache.spark.sql.DataFrame, 
                            anomalies: org.apache.spark.sql.DataFrame): Unit = {
    import org.apache.spark.sql.functions._
    
    logger.info("="*80)
    logger.info("PROCESSING SUMMARY")
    logger.info("="*80)
    
    val totalRecords = metrics.count()
    logger.info(s"Total crypto price records processed: $totalRecords")
    
    logger.info("\nRecords per crypto:")
    metrics.groupBy("crypto_id")
      .count()
      .orderBy(col("count").desc)
      .collect()
      .foreach { row =>
        logger.info(f"  ${row.getString(0)}%15s: ${row.getLong(1)}%5d records")
      }
    
    val totalAnomalies = anomalies.count()
    logger.info(s"\nTotal anomalies detected: $totalAnomalies")
    
    if (totalAnomalies > 0) {
      logger.info("\nAnomalies by type:")
      anomalies.groupBy("anomaly_type")
        .count()
        .orderBy(col("count").desc)
        .collect()
        .foreach { row =>
          logger.info(f"  ${row.getString(0)}%15s: ${row.getLong(1)}%5d")
        }
      
      logger.info("\nAnomalies by severity:")
      anomalies.groupBy("severity")
        .count()
        .orderBy(col("count").desc)
        .collect()
        .foreach { row =>
          logger.info(f"  ${row.getString(0)}%15s: ${row.getLong(1)}%5d")
        }
      
      logger.info("\nTop 10 recent anomalies:")
      anomalies.select("crypto_id", "anomaly_type", "severity", "description")
        .orderBy(col("detected_at").desc)
        .limit(10)
        .collect()
        .foreach { row =>
          logger.info(f"  ${row.getString(0)}%15s | ${row.getString(1)}%15s | " +
            f"${row.getString(2)}%8s | ${row.getString(3)}")
        }
    }
    
    logger.info("="*80)
  }
}
