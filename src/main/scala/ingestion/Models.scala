package ingestion

case class CryptoPrice(
  usd: Double,
  brl: Double,
  usd_24h_vol: Double,
  usd_24h_change: Double,
  last_updated_at: Long
)

case class CryptoPriceData(
  cryptoId: String,
  usd: Double,
  brl: Double,
  usd24hVol: Double,
  usd24hChange: Double,
  lastUpdatedAt: Long,
  collectedAt: Long
)

case class CollectionMetadata(
  collectionTimestamp: Long,
  totalCryptos: Int,
  successCount: Int,
  failureCount: Int,
  errors: List[String]
)

case class CollectionResult(
  metadata: CollectionMetadata,
  data: List[CryptoPriceData]
)
