package ingestion

import com.typesafe.scalalogging.LazyLogging
import config.AppConfig
import io.circe.syntax._
import io.circe.generic.auto._
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{PutObjectRequest, S3Exception}

import java.net.URI
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

class S3Writer extends LazyLogging {
  
  private val s3Client: S3Client = createS3Client()
  
  private def createS3Client(): S3Client = {
    val credentials = AwsBasicCredentials.create("test", "test")
    
    S3Client.builder()
      .endpointOverride(URI.create(AppConfig.AWS.endpoint))
      .region(Region.of(AppConfig.AWS.region))
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .build()
  }
  
  def writeCollectionResult(result: CollectionResult): Either[String, String] = {
    val timestamp = result.metadata.collectionTimestamp
    val s3Key = generateS3Key(timestamp)
    val jsonContent = result.asJson.noSpaces
    
    logger.info(s"Writing collection result to S3: s3://${AppConfig.AWS.S3.bucketRaw}/$s3Key")
    
    try {
      val putRequest = PutObjectRequest.builder()
        .bucket(AppConfig.AWS.S3.bucketRaw)
        .key(s3Key)
        .contentType("application/json")
        .build()
      
      s3Client.putObject(putRequest, RequestBody.fromString(jsonContent))
      
      val s3Path = s"s3://${AppConfig.AWS.S3.bucketRaw}/$s3Key"
      logger.info(s"Successfully wrote to S3: $s3Path")
      Right(s3Path)
      
    } catch {
      case e: S3Exception =>
        val errorMsg = s"S3 error: ${e.getMessage}"
        logger.error(errorMsg, e)
        Left(errorMsg)
      case e: Exception =>
        val errorMsg = s"Unexpected error writing to S3: ${e.getMessage}"
        logger.error(errorMsg, e)
        Left(errorMsg)
    }
  }
  
  private def generateS3Key(timestamp: Long): String = {
    val instant = Instant.ofEpochSecond(timestamp)
    val zonedDateTime = instant.atZone(ZoneId.of("UTC"))
    
    val year = zonedDateTime.getYear
    val month = f"${zonedDateTime.getMonthValue}%02d"
    val day = f"${zonedDateTime.getDayOfMonth}%02d"
    val hour = f"${zonedDateTime.getHour}%02d"
    
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    val filename = s"crypto_prices_${zonedDateTime.format(formatter)}.json"
    
    s"year=$year/month=$month/day=$day/hour=$hour/$filename"
  }
  
  def close(): Unit = {
    s3Client.close()
  }
}
