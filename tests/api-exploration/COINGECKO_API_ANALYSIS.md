
CoinGecko API - Análise e Documentação

Endpoints Utilizados

1. Simple Price
URL: `GET /simple/price`

Parâmetros:
- `ids`: Lista de criptomoedas (bitcoin,ethereum,cardano,binancecoin,solana)
- `vs_currencies`: Moedas de referência (usd,brl)
- `include_24hr_vol`: true
- `include_24hr_change`: true
- `include_last_updated_at`: true

Response Structure (JSON):

```
json

{
  "bitcoin": {
    "usd": 43250.50,
    "brl": 215678.90,
    "usd_24h_vol": 25000000000,
    "usd_24h_change": 2.5,
    "last_updated_at": 1707253200
  }
}

```

Caso de Uso: Coleta em tempo real (a cada 1 minuto)

---

2. Market Chart

URL: `GET /coins/{id}/market_chart`

Parâmetros:
- `vs_currency`: usd
- `days`: 1 (últimas 24h)
- `interval`: hourly

Response Structure:

```
json

{
  "prices": [[timestamp_ms, price], ...],
  "market_caps": [[timestamp_ms, market_cap], ...],
  "total_volumes": [[timestamp_ms, volume], ...]
}

```

Caso de Uso: Análise histórica para baseline de anomalias

---

 Rate Limits

- Free Tier: 10-50 calls/minuto
- Recomendação: Coletar a cada 1 minuto (60 calls/hora)
- Header: `x-ratelimit-remaining`

---

 Anomalias a Detectar

1. Variação de Preço Extrema
   - Trigger: >10% em 5 minutos
   - Campo: `usd_24h_change`

2. Volume Atípico
   - Trigger: Volume > 3x média móvel 24h
   - Campo: `usd_24h_vol`

3. Pump & Dump Pattern
   - Trigger: Subida abrupta + queda rápida
   - Análise: Comparar `prices` array

---

 Modelagem Scala

```
scala

case class CryptoPrice(
  usd: Double,
  brl: Double,
  usd_24h_vol: Double,
  usd_24h_change: Double,
  last_updated_at: Long
)

case class MarketDataPoint(
  timestamp: Long,
  price: Double
)

```
