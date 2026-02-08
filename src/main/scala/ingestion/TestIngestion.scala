package ingestion

import com.typesafe.scalalogging.LazyLogging

object TestIngestion extends LazyLogging {
  
  def main(args: Array[String]): Unit = {
    logger.info("Testing CoinGecko API connection...")
    
    val client = new CoinGeckoClient()
    
    try {
      client.fetchPrices() match {
        case Right(prices) =>
          logger.info(s"Successfully fetched ${prices.size} crypto prices")
          prices.foreach { case (id, price) =>
            logger.info(f"$id%15s: USD $$${price.usd}%12.2f | " +
              f"Change ${price.usd_24h_change}%6.2f%% | " +
              f"Vol $$${price.usd_24h_vol}%15.0f")
          }
        case Left(error) =>
          logger.error(s"Failed to fetch prices: $error")
      }
    } finally {
      client.close()
    }
  }
}
