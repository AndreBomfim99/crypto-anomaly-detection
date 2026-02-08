package ingestion

import com.typesafe.scalalogging.LazyLogging
import config.AppConfig

import java.time.Instant
import scala.util.{Failure, Success, Try}

object DataIngestion extends LazyLogging {
  
  def main(args: Array[String]): Unit = {
    logger.info(s"Starting ${AppConfig.App.name} v${AppConfig.App.version}")
    logger.info(s"Collection interval: ${AppConfig.CoinGecko.collectionIntervalMinutes} minutes")
    logger.info(s"Target cryptos: ${AppConfig.CoinGecko.cryptoIds.mkString(", ")}")
    
    val client = new CoinGeckoClient()
    val s3Writer = new S3Writer()
    
    try {
      runCollection(client, s3Writer)
    } finally {
      client.close()
      s3Writer.close()
      logger.info("Data ingestion completed")
    }
  }
  
  private def runCollection(client: CoinGeckoClient, s3Writer: S3Writer): Unit = {
    val collectionTimestamp = Instant.now().getEpochSecond
    
    logger.info(s"Starting data collection at timestamp: $collectionTimestamp")
    
    client.fetchPrices() match {
      case Right(prices) =>
        val cryptoDataList = transformData(prices, collectionTimestamp)
        val result = CollectionResult(
          metadata = CollectionMetadata(
            collectionTimestamp = collectionTimestamp,
            totalCryptos = AppConfig.CoinGecko.cryptoIds.size,
            successCount = cryptoDataList.size,
            failureCount = AppConfig.CoinGecko.cryptoIds.size - cryptoDataList.size,
            errors = findMissingCryptos(prices)
          ),
          data = cryptoDataList
        )
        
        s3Writer.writeCollectionResult(result) match {
          case Right(s3Path) =>
            logger.info(s"Collection completed successfully: $s3Path")
            logCollectionSummary(result)
          case Left(error) =>
            logger.error(s"Failed to write to S3: $error")
        }
        
      case Left(error) =>
        logger.error(s"Failed to fetch prices: $error")
        val emptyResult = CollectionResult(
          metadata = CollectionMetadata(
            collectionTimestamp = collectionTimestamp,
            totalCryptos = AppConfig.CoinGecko.cryptoIds.size,
            successCount = 0,
            failureCount = AppConfig.CoinGecko.cryptoIds.size,
            errors = List(error)
          ),
          data = List.empty
        )
        s3Writer.writeCollectionResult(emptyResult)
    }
  }
  
  private def transformData(
    prices: Map[String, CryptoPrice],
    collectionTimestamp: Long
  ): List[CryptoPriceData] = {
    prices.map { case (cryptoId, price) =>
      CryptoPriceData(
        cryptoId = cryptoId,
        usd = price.usd,
        brl = price.brl,
        usd24hVol = price.usd_24h_vol,
        usd24hChange = price.usd_24h_change,
        lastUpdatedAt = price.last_updated_at,
        collectedAt = collectionTimestamp
      )
    }.toList
  }
  
  private def findMissingCryptos(prices: Map[String, CryptoPrice]): List[String] = {
    val fetchedIds = prices.keySet
    AppConfig.CoinGecko.cryptoIds.filterNot(fetchedIds.contains).map { id =>
      s"Missing data for crypto: $id"
    }
  }
  
  private def logCollectionSummary(result: CollectionResult): Unit = {
    logger.info("="*80)
    logger.info("COLLECTION SUMMARY")
    logger.info("="*80)
    logger.info(s"Total cryptos expected: ${result.metadata.totalCryptos}")
    logger.info(s"Successfully collected: ${result.metadata.successCount}")
    logger.info(s"Failed: ${result.metadata.failureCount}")
    
    result.data.foreach { crypto =>
      logger.info(f"${crypto.cryptoId}%15s: USD ${crypto.usd}%12.2f | " +
        f"Change ${crypto.usd24hChange}%6.2f%% | " +
        f"Vol ${crypto.usd24hVol}%15.0f")
    }
    
    if (result.metadata.errors.nonEmpty) {
      logger.warn("Errors encountered:")
      result.metadata.errors.foreach(err => logger.warn(s"  - $err"))
    }
    logger.info("="*80)
  }
}
