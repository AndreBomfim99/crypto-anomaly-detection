package config

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.JavaConverters._

object AppConfig {
  
  private val config: Config = ConfigFactory.load()
  
  object App {
    val name: String = config.getString("app.name")
    val version: String = config.getString("app.version")
  }
  
  object CoinGecko {
    val baseUrl: String = config.getString("coingecko.base-url")
    val endpoint: String = config.getString("coingecko.endpoint")
    val collectionIntervalMinutes: Int = config.getInt("coingecko.collection-interval-minutes")
    val timeoutSeconds: Int = config.getInt("coingecko.timeout-seconds")
    val retryAttempts: Int = config.getInt("coingecko.retry-attempts")
    val retryDelaySeconds: Int = config.getInt("coingecko.retry-delay-seconds")
    
    val cryptoIds: List[String] = config.getStringList("coingecko.crypto-ids").asScala.toList
    val vsCurrencies: List[String] = config.getStringList("coingecko.vs-currencies").asScala.toList
    
    val include24hrVol: Boolean = config.getBoolean("coingecko.params.include-24hr-vol")
    val include24hrChange: Boolean = config.getBoolean("coingecko.params.include-24hr-change")
    val includeLastUpdatedAt: Boolean = config.getBoolean("coingecko.params.include-last-updated-at")
    
    def fullUrl: String = s"$baseUrl$endpoint"
  }
  
  object AWS {
    val endpoint: String = config.getString("aws.endpoint")
    val region: String = config.getString("aws.region")
    
    object S3 {
      val bucketRaw: String = config.getString("aws.s3.bucket-raw")
      val bucketProcessed: String = config.getString("aws.s3.bucket-processed")
      val bucketCurated: String = config.getString("aws.s3.bucket-curated")
    }
    
    object SNS {
      val topicAlerts: String = config.getString("aws.sns.topic-alerts")
    }
  }
  
  object Logging {
    val level: String = config.getString("logging.level")
    val pattern: String = config.getString("logging.pattern")
  }
}
