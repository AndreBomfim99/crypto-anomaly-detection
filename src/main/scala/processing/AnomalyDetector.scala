package processing

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window

case class Anomaly(
  crypto_id: String,
  anomaly_type: String,
  severity: String,
  detected_at: Long,
  current_price: Double,
  reference_value: Double,
  variation_pct: Double,
  description: String
)

object AnomalyDetector extends LazyLogging {
  
  private val VOLUME_MULTIPLIER_THRESHOLD = 2.5
  
  def detectVolumeSpikes(df: DataFrame): DataFrame = {
    logger.info(s"Detecting volume spikes (threshold: ${VOLUME_MULTIPLIER_THRESHOLD}x MA)")
    
    val spikes = df
      .filter(col("volume_ma_24h").isNotNull)
      .filter(col("volume_ma_24h") > 0)
      .withColumn("volume_multiplier", col("volume_24h_usd") / col("volume_ma_24h"))
      .filter(col("volume_multiplier") > VOLUME_MULTIPLIER_THRESHOLD)
      .withColumn("anomaly_type", lit("volume_spike"))
      .withColumn("severity",
        when(col("volume_multiplier") > 5, "high")
        .when(col("volume_multiplier") > 3.5, "medium")
        .otherwise("low")
      )
      .withColumn("description",
        concat(
          lit("Volume is "),
          format_number(col("volume_multiplier"), 2),
          lit("x the 24h average")
        )
      )
      .select(
        col("crypto_id"),
        col("anomaly_type"),
        col("severity"),
        col("collection_timestamp").as("detected_at"),
        col("volume_24h_usd").as("current_price"),
        col("volume_ma_24h").as("reference_value"),
        col("volume_multiplier").as("variation_pct"),
        col("description")
      )
    
    val count = spikes.count()
    logger.info(s"Detected $count volume spike anomalies")
    spikes
  }
  
  def detectPriceVariations(df: DataFrame): DataFrame = {
    logger.info("Detecting price variations using 24h change from API")
    
    val variations = df
      .filter(abs(col("change_24h_pct")) > 5.0)
      .withColumn("anomaly_type", lit("price_variation"))
      .withColumn("severity",
        when(abs(col("change_24h_pct")) > 15, "high")
        .when(abs(col("change_24h_pct")) > 10, "medium")
        .otherwise("low")
      )
      .withColumn("description",
        concat(
          lit("Price changed "),
          format_number(col("change_24h_pct"), 2),
          lit("% in 24 hours")
        )
      )
      .select(
        col("crypto_id"),
        col("anomaly_type"),
        col("severity"),
        col("collection_timestamp").as("detected_at"),
        col("price_usd").as("current_price"),
        lit(0.0).as("reference_value"),
        col("change_24h_pct").as("variation_pct"),
        col("description")
      )
    
    val count = variations.count()
    logger.info(s"Detected $count price variation anomalies")
    variations
  }
  
  def detectAll(df: DataFrame): DataFrame = {
    logger.info("Running all anomaly detection algorithms...")
    
    val volumeSpikes = detectVolumeSpikes(df)
    val priceVariations = detectPriceVariations(df)
    
    val allAnomalies = volumeSpikes
      .union(priceVariations)
      .orderBy(col("detected_at").desc)
    
    val totalCount = allAnomalies.count()
    logger.info(s"Total anomalies detected: $totalCount")
    
    allAnomalies
  }
  
  def saveAnomalies(anomalies: DataFrame, s3Path: String): Unit = {
    logger.info(s"Saving anomalies to: $s3Path")
    
    if (anomalies.count() > 0) {
      anomalies
        .withColumn("detection_date", to_date(from_unixtime(col("detected_at"))))
        .write
        .mode("append")
        .partitionBy("detection_date", "severity")
        .parquet(s3Path)
      
      logger.info("Anomalies saved successfully")
    } else {
      logger.info("No anomalies to save")
    }
  }
}
