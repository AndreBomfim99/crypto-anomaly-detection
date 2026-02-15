package monitoring

import com.typesafe.scalalogging.LazyLogging
import processing.SparkProcessor
import config.AppConfig
import org.apache.spark.sql.functions._

object MonitoringDashboard extends LazyLogging {
  
  def main(args: Array[String]): Unit = {
    logger.info("="*80)
    logger.info("CRYPTO ANOMALY DETECTION - MONITORING DASHBOARD")
    logger.info("="*80)
    
    val spark = SparkProcessor.createSparkSession("MonitoringDashboard")
    val metricsCollector = MetricsCollector()
    
    try {
      logger.info("\n--- SYSTEM METRICS ---")
      val sysMetrics = metricsCollector.collectSystemMetrics()
      metricsCollector.recordSystemMetrics(sysMetrics)
      
      logger.info("\n--- PIPELINE METRICS REPORT ---")
      metricsCollector.generateReport()
      
      logger.info("\n--- DATA METRICS FROM S3 ---")
      displayDataMetrics(spark)
      
      logger.info("\n--- ANOMALY METRICS ---")
      displayAnomalyMetrics(spark)
      
    } catch {
      case e: Exception =>
        logger.error("Monitoring dashboard failed", e)
    } finally {
      SparkProcessor.stopSparkSession(spark)
    }
    
    logger.info("="*80)
    logger.info("MONITORING DASHBOARD COMPLETED")
    logger.info("="*80)
  }
  
  private def displayDataMetrics(spark: org.apache.spark.sql.SparkSession): Unit = {
    try {
      val metricsPath = s"s3a://${AppConfig.AWS.S3.bucketProcessed}/metrics"
      val metrics = spark.read.parquet(metricsPath)
      
      val totalRecords = metrics.count()
      logger.info(f"Total records in processed data: $totalRecords%,d")
      
      logger.info("\nRecords per crypto:")
      metrics.groupBy("crypto_id")
        .count()
        .orderBy(col("count").desc)
        .collect()
        .foreach { row =>
          logger.info(f"  ${row.getString(0)}%15s: ${row.getLong(1)}%,5d records")
        }
      
      logger.info("\nLatest prices:")
      metrics
        .groupBy("crypto_id")
        .agg(
          max("collection_timestamp").as("latest_timestamp"),
          last("price_usd").as("latest_price")
        )
        .orderBy(col("crypto_id"))
        .collect()
        .foreach { row =>
          val price = row.getDouble(2)
          logger.info(f"  ${row.getString(0)}%15s: $$${price}%,.2f")
        }
      
    } catch {
      case e: Exception =>
        logger.warn(s"Could not load data metrics: ${e.getMessage}")
    }
  }
  
  private def displayAnomalyMetrics(spark: org.apache.spark.sql.SparkSession): Unit = {
    try {
      val anomaliesPath = s"s3a://${AppConfig.AWS.S3.bucketProcessed}/anomalies"
      val anomalies = spark.read.parquet(anomaliesPath)
      
      val totalAnomalies = anomalies.count()
      logger.info(f"Total anomalies detected: $totalAnomalies%,d")
      
      if (totalAnomalies > 0) {
        logger.info("\nAnomalies by type:")
        anomalies.groupBy("anomaly_type")
          .count()
          .orderBy(col("count").desc)
          .collect()
          .foreach { row =>
            logger.info(f"  ${row.getString(0)}%20s: ${row.getLong(1)}%5d")
          }
        
        logger.info("\nAnomalies by severity:")
        anomalies.groupBy("severity")
          .count()
          .orderBy(col("count").desc)
          .collect()
          .foreach { row =>
            logger.info(f"  ${row.getString(0)}%15s: ${row.getLong(1)}%5d")
          }
        
        logger.info("\nAnomalies by crypto:")
        anomalies.groupBy("crypto_id")
          .count()
          .orderBy(col("count").desc)
          .collect()
          .foreach { row =>
            logger.info(f"  ${row.getString(0)}%15s: ${row.getLong(1)}%5d")
          }
      }
      
    } catch {
      case e: Exception =>
        logger.warn(s"Could not load anomaly metrics: ${e.getMessage}")
    }
  }
}
