package alerting

import com.typesafe.scalalogging.LazyLogging
import config.AppConfig
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.{PublishRequest, PublishResponse, SnsException}

import java.net.URI

case class Alert(
  cryptoId: String,
  anomalyType: String,
  severity: String,
  currentPrice: Double,
  variationPct: Double,
  description: String,
  detectedAt: Long
)

class SNSNotifier extends LazyLogging {
  
  private val snsClient: SnsClient = createSnsClient()
  private val topicArn: String = getTopicArn()
  
  private def createSnsClient(): SnsClient = {
    val credentials = AwsBasicCredentials.create("test", "test")
    
    SnsClient.builder()
      .endpointOverride(URI.create(AppConfig.AWS.endpoint))
      .region(Region.of(AppConfig.AWS.region))
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .build()
  }
  
  private def getTopicArn(): String = {
    s"arn:aws:sns:${AppConfig.AWS.region}:000000000000:${AppConfig.AWS.SNS.topicAlerts}"
  }
  
  def sendAlert(alert: Alert): Either[String, String] = {
    val subject = formatSubject(alert)
    val message = formatMessage(alert)
    
    logger.info(s"Sending SNS alert for ${alert.cryptoId} - ${alert.anomalyType}")
    
    try {
      val request = PublishRequest.builder()
        .topicArn(topicArn)
        .subject(subject)
        .message(message)
        .build()
      
      val response: PublishResponse = snsClient.publish(request)
      val messageId = response.messageId()
      
      logger.info(s"Alert sent successfully. MessageId: $messageId")
      Right(messageId)
      
    } catch {
      case e: SnsException =>
        val errorMsg = s"SNS error: ${e.getMessage}"
        logger.error(errorMsg, e)
        Left(errorMsg)
      case e: Exception =>
        val errorMsg = s"Unexpected error sending alert: ${e.getMessage}"
        logger.error(errorMsg, e)
        Left(errorMsg)
    }
  }
  
  def sendBatchAlerts(alerts: List[Alert]): (Int, Int) = {
    logger.info(s"Sending ${alerts.size} alerts...")
    
    var successCount = 0
    var failureCount = 0
    
    alerts.foreach { alert =>
      sendAlert(alert) match {
        case Right(_) => successCount += 1
        case Left(_) => failureCount += 1
      }
    }
    
    logger.info(s"Batch complete: $successCount succeeded, $failureCount failed")
    (successCount, failureCount)
  }
  
  private def formatSubject(alert: Alert): String = {
    val severityEmoji = alert.severity match {
      case "high" => "HIGH"
      case "medium" => "MEDIUM"
      case _ => "_"
    }
    
    s"$severityEmoji Crypto Alert: ${alert.cryptoId.toUpperCase()} - ${alert.anomalyType}"
  }
  
  private def formatMessage(alert: Alert): String = {
    val timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      .format(new java.util.Date(alert.detectedAt * 1000))
    
    s"""
       |═══════════════════════════════════════════════════════
       |CRYPTO ANOMALY ALERT
       |═══════════════════════════════════════════════════════
       |
       |Cryptocurrency: ${alert.cryptoId.toUpperCase()}
       |Anomaly Type:   ${alert.anomalyType}
       |Severity:       ${alert.severity.toUpperCase()}
       |
       |Current Price:  $$${alert.currentPrice}
       |Variation:      ${alert.variationPct}%
       |
       |Description:    ${alert.description}
       |
       |Detected At:    $timestamp UTC
       |
       |═══════════════════════════════════════════════════════
       |This is an automated alert from Crypto Anomaly Detection System
       |═══════════════════════════════════════════════════════
       """.stripMargin
  }
  
  def testConnection(): Boolean = {
    logger.info("Testing SNS connection...")
    
    val testAlert = Alert(
      cryptoId = "test",
      anomalyType = "connection_test",
      severity = "low",
      currentPrice = 0.0,
      variationPct = 0.0,
      description = "SNS connection test",
      detectedAt = System.currentTimeMillis() / 1000
    )
    
    sendAlert(testAlert) match {
      case Right(_) =>
        logger.info("SNS connection test successful")
        true
      case Left(error) =>
        logger.error(s"SNS connection test failed: $error")
        false
    }
  }
  
  def close(): Unit = {
    snsClient.close()
  }
}

object SNSNotifier {
  def apply(): SNSNotifier = new SNSNotifier()
}
