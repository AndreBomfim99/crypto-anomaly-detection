package processing

import com.typesafe.scalalogging.LazyLogging
import config.AppConfig
import org.apache.spark.sql.SparkSession

object SparkProcessor extends LazyLogging {
  
  def createSparkSession(appName: String): SparkSession = {
    logger.info(s"Creating Spark session: $appName")
    
    val spark = SparkSession.builder()
      .appName(appName)
      .master("local[*]")
      .config("spark.hadoop.fs.s3a.endpoint", AppConfig.AWS.endpoint)
      .config("spark.hadoop.fs.s3a.access.key", "test")
      .config("spark.hadoop.fs.s3a.secret.key", "test")
      .config("spark.hadoop.fs.s3a.path.style.access", "true")
      .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .config("spark.hadoop.fs.s3a.connection.ssl.enabled", "false")
      .config("spark.sql.shuffle.partitions", "4")
      .config("spark.sql.adaptive.enabled", "true")
      .getOrCreate()
    
    spark.sparkContext.setLogLevel("WARN")
    
    logger.info(s"Spark session created successfully")
    logger.info(s"Spark version: ${spark.version}")
    logger.info(s"Master: ${spark.sparkContext.master}")
    
    spark
  }
  
  def stopSparkSession(spark: SparkSession): Unit = {
    logger.info("Stopping Spark session...")
    spark.stop()
    logger.info("Spark session stopped")
  }
}
