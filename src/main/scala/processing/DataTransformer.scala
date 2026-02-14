package processing

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object DataTransformer extends LazyLogging {
  
  def readRawData(spark: SparkSession, s3Path: String): DataFrame = {
    logger.info(s"Reading raw data from: $s3Path")
    
    val schema = StructType(Seq(
      StructField("metadata", StructType(Seq(
        StructField("collectionTimestamp", LongType, nullable = false),
        StructField("totalCryptos", IntegerType, nullable = false),
        StructField("successCount", IntegerType, nullable = false),
        StructField("failureCount", IntegerType, nullable = false),
        StructField("errors", ArrayType(StringType), nullable = true)
      ))),
      StructField("data", ArrayType(StructType(Seq(
        StructField("cryptoId", StringType, nullable = false),
        StructField("usd", DoubleType, nullable = false),
        StructField("brl", DoubleType, nullable = false),
        StructField("usd24hVol", DoubleType, nullable = false),
        StructField("usd24hChange", DoubleType, nullable = false),
        StructField("lastUpdatedAt", LongType, nullable = false),
        StructField("collectedAt", LongType, nullable = false)
      ))))
    ))
    
    val df = spark.read
      .schema(schema)
      .json(s3Path)
    
    logger.info(s"Read ${df.count()} collection records")
    df
  }
  
  def explodeAndFlatten(df: DataFrame): DataFrame = {
    logger.info("Exploding and flattening data...")
    
    val exploded = df
      .select(
        col("metadata.collectionTimestamp").as("collection_timestamp"),
        explode(col("data")).as("crypto_data")
      )
      .select(
        col("collection_timestamp"),
        col("crypto_data.cryptoId").as("crypto_id"),
        col("crypto_data.usd").as("price_usd"),
        col("crypto_data.brl").as("price_brl"),
        col("crypto_data.usd24hVol").as("volume_24h_usd"),
        col("crypto_data.usd24hChange").as("change_24h_pct"),
        col("crypto_data.lastUpdatedAt").as("last_updated_at"),
        col("crypto_data.collectedAt").as("collected_at")
      )
    
    logger.info(s"Flattened to ${exploded.count()} crypto price records")
    exploded
  }
  
  def addTimestampColumns(df: DataFrame): DataFrame = {
    logger.info("Adding timestamp columns...")
    
    df
      .withColumn("collection_datetime", from_unixtime(col("collection_timestamp")))
      .withColumn("collection_date", to_date(col("collection_datetime")))
      .withColumn("collection_hour", hour(col("collection_datetime")))
      .withColumn("collection_minute", minute(col("collection_datetime")))
  }
  
  def calculateMovingAverages(df: DataFrame): DataFrame = {
    logger.info("Calculating moving averages...")
    
    import org.apache.spark.sql.expressions.Window
    
    val windowSpec = Window
      .partitionBy("crypto_id")
      .orderBy(col("collection_timestamp").cast("long"))
      .rowsBetween(-480, 0)
    
    val window5 = Window
      .partitionBy("crypto_id")
      .orderBy(col("collection_timestamp").cast("long"))
      .rowsBetween(-5, 0)
    
    df
      .withColumn("price_ma_24h", avg("price_usd").over(windowSpec))
      .withColumn("volume_ma_24h", avg("volume_24h_usd").over(windowSpec))
      .withColumn("price_ma_15min", avg("price_usd").over(window5))
  }
  
  def filterValidData(df: DataFrame): DataFrame = {
    logger.info("Filtering valid data...")
    
    val filtered = df
      .filter(col("price_usd") > 0)
      .filter(col("volume_24h_usd") > 0)
      .filter(col("crypto_id").isNotNull)
    
    logger.info(s"Filtered to ${filtered.count()} valid records")
    filtered
  }
  
  def transform(spark: SparkSession, s3Path: String): DataFrame = {
    logger.info("Starting data transformation pipeline...")
    
    val raw = readRawData(spark, s3Path)
    val flattened = explodeAndFlatten(raw)
    val withTimestamps = addTimestampColumns(flattened)
    val withMovingAvgs = calculateMovingAverages(withTimestamps)
    val validated = filterValidData(withMovingAvgs)
    
    logger.info("Data transformation completed")
    validated
  }
}
