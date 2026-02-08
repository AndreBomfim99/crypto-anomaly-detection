 CoinGecko API - Análise e Documentação

Data da Análise: 08/02/2026  
Plano: Free Tier (sem API key)

---

LIMITAÇÕES DESCOBERTAS

 Rate Limit Real
- Documentado: 10-50 calls/min
- Real (testado): ~2-3 calls/min
- Status Code: 429 (Too Many Requests)
- Estratégia: Coletar a cada 3 minutos (20 calls/hora - seguro)

 Endpoint Market Chart
- Status: Indisponível no Free Tier
- Motivo: `interval=hourly` exclusivo para Enterprise
- Impacto: Não teremos histórico pronto da API
- Solução: Construir nosso próprio histórico via coletas frequentes

---

Endpoint Disponível: Simple Price

 URL
```
GET https://api.coingecko.com/api/v3/simple/price
```

 Parâmetros
```
ids: bitcoin,ethereum,cardano,binancecoin,solana
vs_currencies: usd,brl
include_24hr_vol: true
include_24hr_change: true
include_last_updated_at: true
```

 Response Structure (JSON)
```json
{
  "bitcoin": {
    "usd": 70956.00,
    "brl": 370185.00,
    "usd_24h_vol": 40022567994.55898,
    "usd_24h_change": 2.09,
    "last_updated_at": 1770586071
  }
}
```

 Campos Relevantes
- `usd`: Preço em dólares
- `brl`: Preço em reais
- `usd_24h_vol`: Volume de negociação 24h (USD)
- `usd_24h_change`: Variação percentual 24h
- `last_updated_at`: Timestamp Unix (segundos)

---

Estratégia de Coleta Ajustada

 Frequência
- Intervalo: A cada 3 minutos
- Total/hora: 20 coletas
- Total/dia: 480 coletas
- Margem de segurança: Rate limit respeitado

 Pipeline de Dados

```

CoinGecko API   (a cada 3 min)


S3 raw/            (JSON bruto timestampado)
Particionado por   (ano/mes/dia/hora)
data              
        

Spark Processing    (batch a cada 15 min)
- Agregação        
- Detecção         


 S3 processed/  (dados processados)

```

---

Regras de Detecção de Anomalias

 1. Variação de Preço Extrema
Trigger: Variação > 10% em 15 minutos (5 coletas consecutivas)

Cálculo:
```
variation = ((price_current - price_15min_ago) / price_15min_ago) * 100
if abs(variation) > 10.0:
    ALERTA!
```

2. Volume Atípico
Trigger: Volume atual > 3x média móvel (últimas 24h)

Cálculo:
```
avg_volume_24h = mean(volumes_last_480_readings)
if current_volume > (avg_volume_24h * 3):
    ALERTA!
```

3. Pump & Dump Pattern
Trigger: Subida rápida (>8%) seguida de queda rápida (>8%) em 30 min

Cálculo:
```
 10 coletas = 30 minutos
prices_30min = last_10_readings
max_price = max(prices_30min)
min_price = min(prices_30min)
current_price = prices_30min[-1]

pump = ((max_price - min_price) / min_price) * 100
dump = ((max_price - current_price) / max_price) * 100

if pump > 8 and dump > 8:
    ALERTA! (possível pump & dump)
```

---

Modelagem Scala

 Case Classes
```scala
case class CryptoPrice(
  cryptoId: String,
  usd: Double,
  brl: Double,
  usd24hVol: Double,
  usd24hChange: Double,
  lastUpdatedAt: Long,
  collectedAt: Long  // timestamp da nossa coleta
)

case class AnomalyDetection(
  cryptoId: String,
  anomalyType: String,  // "price_spike", "volume_spike", "pump_dump"
  severity: String,     // "low", "medium", "high"
  detectedAt: Long,
  metrics: Map[String, Double]
)
```

 Estrutura S3
```
crypto-data-raw/
  └── year=2026/
      └── month=02/
          └── day=08/
              └── hour=18/
                  └── crypto_prices_20260208_180000.json
                  └── crypto_prices_20260208_180300.json
                  └── crypto_prices_20260208_180600.json

crypto-data-processed/
  └── year=2026/
      └── month=02/
          └── day=08/
              └── anomalies_20260208.parquet
              └── aggregated_metrics_20260208.parquet
```

---

Exemplo de Dados Coletados

Teste realizado: 08/02/2026 18:27:52

| Crypto | Preço USD | Variação 24h | Volume 24h USD | Status |
|--------|-----------|--------------|----------------|--------|
| Bitcoin | $70,956 | +2.09% | $40.02B | Normal |
| Ethereum | $2,110 | +0.23% | $18.69B | Normal |
| Solana | $87.18 | -1.73% | $2.97B | Normal |
| BNB | $645.03 | -0.97% | $924M | Normal |
| Cardano | $0.27 | -0.97% | $708M | Normal |

Conclusão: Mercado estável, sem anomalias detectadas neste momento.

---

Configurações Recomendadas
```hocon
coingecko {
  base-url = "https://api.coingecko.com/api/v3"
  collection-interval = 3 minutes
  crypto-ids = ["bitcoin", "ethereum", "cardano", "binancecoin", "solana"]
  vs-currencies = ["usd", "brl"]
  retry-attempts = 3
  retry-delay = 5 seconds
  timeout = 10 seconds
}

anomaly-detection {
  price-variation-threshold = 10.0  // percentual
  volume-multiplier-threshold = 3.0
  pump-dump-threshold = 8.0
  lookback-period = 24 hours
}
```
