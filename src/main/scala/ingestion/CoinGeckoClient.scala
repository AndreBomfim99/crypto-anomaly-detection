package ingestion

import com.typesafe.scalalogging.LazyLogging
import config.AppConfig
import io.circe.generic.auto._
import sttp.client3._
import sttp.client3.circe._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class CoinGeckoClient extends LazyLogging {
  
  private val backend = HttpURLConnectionBackend()
  
  def fetchPrices(): Either[String, Map[String, CryptoPrice]] = {
    val url = buildUrl()
    logger.info(s"Fetching prices from CoinGecko API: $url")
    
    val request = basicRequest
      .get(uri"$url")
      .readTimeout(AppConfig.CoinGecko.timeoutSeconds.seconds)
      .response(asString)
    
    retryRequest(request, AppConfig.CoinGecko.retryAttempts) match {
      case Success(response) =>
        if (response.code.isSuccess) {
          parseResponse(response.body) match {
            case Right(data) =>
              logger.info(s"Successfully fetched ${data.size} crypto prices")
              Right(data)
            case Left(error) =>
              val errorMsg = s"Failed to parse response: $error"
              logger.error(errorMsg)
              Left(errorMsg)
          }
        } else {
          val errorMsg = s"Request failed with status ${response.code}"
          logger.error(errorMsg)
          Left(errorMsg)
        }
      case Failure(exception) =>
        val errorMsg = s"Request failed: ${exception.getMessage}"
        logger.error(errorMsg)
        Left(errorMsg)
    }
  }
  
  private def parseResponse(body: Either[String, String]): Either[String, Map[String, CryptoPrice]] = {
    body match {
      case Right(jsonStr) =>
        import io.circe.parser._
        decode[Map[String, CryptoPrice]](jsonStr) match {
          case Right(data) => Right(data)
          case Left(error) => Left(error.getMessage)
        }
      case Left(error) => Left(error)
    }
  }
  
  private def buildUrl(): String = {
    val params = Map(
      "ids" -> AppConfig.CoinGecko.cryptoIds.mkString(","),
      "vs_currencies" -> AppConfig.CoinGecko.vsCurrencies.mkString(","),
      "include_24hr_vol" -> AppConfig.CoinGecko.include24hrVol.toString,
      "include_24hr_change" -> AppConfig.CoinGecko.include24hrChange.toString,
      "include_last_updated_at" -> AppConfig.CoinGecko.includeLastUpdatedAt.toString
    )
    
    val queryString = params.map { case (k, v) => s"$k=$v" }.mkString("&")
    s"${AppConfig.CoinGecko.fullUrl}?$queryString"
  }
  
  private def retryRequest(
    request: Request[Either[String, String], Any],
    attemptsLeft: Int
  ): Try[Response[Either[String, String]]] = {
    Try(request.send(backend)) match {
      case Success(response) if response.code.isSuccess =>
        Success(response)
      case Success(response) if attemptsLeft > 0 =>
        logger.warn(s"Request failed with status ${response.code}, retrying... (${attemptsLeft} attempts left)")
        Thread.sleep(AppConfig.CoinGecko.retryDelaySeconds * 1000)
        retryRequest(request, attemptsLeft - 1)
      case Success(response) =>
        Failure(new RuntimeException(s"Request failed with status ${response.code}"))
      case Failure(exception) if attemptsLeft > 0 =>
        logger.warn(s"Request failed with exception, retrying... (${attemptsLeft} attempts left)", exception)
        Thread.sleep(AppConfig.CoinGecko.retryDelaySeconds * 1000)
        retryRequest(request, attemptsLeft - 1)
      case failure =>
        failure
    }
  }
  
  def close(): Unit = {
    backend.close()
  }
}
