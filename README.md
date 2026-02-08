
Sistema de Detecção de Anomalias em Criptomoedas

Problema de Negócio

O mercado de criptomoedas é altamente volátil e suscetível a manipulações e movimentos especulativos coordenados. Este sistema detecta anomalias em tempo real nas principais criptomoedas, permitindo:

- Alertas imediatos sobre variações anormais de preço (>10% em 5 minutos)
- Detecção de volume atípico (algo como 3x média móvel em 24h)
- Identificação de padrões suspeitos (tipo pump & dump)

Arquitetura

CoinGecko API → Ingestão (Scala) → S3 (LocalStack) → Spark Processing → SNS Alerts
                                                              ↓
                                                        CloudWatch Logs
```

Stack Tecnológica

- Linguagem: Scala 2.12
- Build: SBT
- Processamento: Apache Spark 3.5
- Cloud Simulado: LocalStack (AWS S3, SNS, CloudWatch)
- IaC: Terraform
- Containers: Docker + Docker Compose
- CI/CD: GitHub Actions


Autor: André Bomfim da Silva, Engenheiro de Dados  
[GitHub](https://github.com/andrebomfim) | [LinkedIn](https://linkedin.com/in/andrebomfim)


