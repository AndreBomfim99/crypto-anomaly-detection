import requests
import json
from datetime import datetime
import time

# CoinGecko API - Free Tier (sem autenticacao)
BASE_URL = "https://api.coingecko.com/api/v3"

# Criptomoedas que vamos monitorar
CRYPTO_IDS = ["bitcoin", "ethereum", "cardano", "binancecoin", "solana"]

def test_simple_price():
    """
    Testa endpoint /simple/price
    Retorna preco atual, volume 24h, mudanca 24h
    """
    print("\n" + "="*80)
    print("TESTE 1: Simple Price Endpoint")
    print("="*80)
    
    url = f"{BASE_URL}/simple/price"
    params = {
        "ids": ",".join(CRYPTO_IDS),
        "vs_currencies": "usd,brl",
        "include_24hr_vol": "true",
        "include_24hr_change": "true",
        "include_last_updated_at": "true"
    }
    
    start_time = time.time()
    response = requests.get(url, params=params)
    elapsed_time = time.time() - start_time
    
    print(f"\nURL: {response.url}")
    print(f"Status Code: {response.status_code}")
    print(f"Response Time: {elapsed_time:.2f}s")
    print(f"\nHeaders relevantes:")
    print(f"  - Rate Limit Remaining: {response.headers.get('x-ratelimit-remaining', 'N/A')}")
    print(f"  - Content-Type: {response.headers.get('content-type')}")
    
    if response.status_code == 200:
        data = response.json()
        print(f"\nResposta JSON (formatada):")
        print(json.dumps(data, indent=2))
        
        # Analise dos dados
        print(f"\n--- ANALISE DOS DADOS ---")
        for crypto_id in CRYPTO_IDS:
            if crypto_id in data:
                crypto_data = data[crypto_id]
                print(f"\n{crypto_id.upper()}:")
                print(f"  Preco USD: ${crypto_data.get('usd', 'N/A'):,.2f}")
                print(f"  Preco BRL: R${crypto_data.get('brl', 'N/A'):,.2f}")
                print(f"  Volume 24h USD: ${crypto_data.get('usd_24h_vol', 'N/A'):,.0f}")
                print(f"  Mudanca 24h: {crypto_data.get('usd_24h_change', 'N/A'):.2f}%")
                print(f"  Last Updated: {datetime.fromtimestamp(crypto_data.get('last_updated_at', 0))}")
    else:
        print(f"\nERRO: {response.text}")
    
    return response.status_code == 200

def test_market_chart():
    """
    Testa endpoint /coins/{id}/market_chart
    Retorna historico de precos (ultimas 24h)
    """
    print("\n" + "="*80)
    print("TESTE 2: Market Chart Endpoint (Bitcoin - ultimas 24h)")
    print("="*80)
    
    crypto_id = "bitcoin"
    url = f"{BASE_URL}/coins/{crypto_id}/market_chart"
    params = {
        "vs_currency": "usd",
        "days": "1",
        "interval": "hourly"
    }
    
    start_time = time.time()
    response = requests.get(url, params=params)
    elapsed_time = time.time() - start_time
    
    print(f"\nURL: {response.url}")
    print(f"Status Code: {response.status_code}")
    print(f"Response Time: {elapsed_time:.2f}s")
    
    if response.status_code == 200:
        data = response.json()
        
        print(f"\nEstrutura da resposta:")
        print(f"  - Campos: {list(data.keys())}")
        print(f"  - Numero de pontos (prices): {len(data.get('prices', []))}")
        print(f"  - Numero de pontos (market_caps): {len(data.get('market_caps', []))}")
        print(f"  - Numero de pontos (total_volumes): {len(data.get('total_volumes', []))}")
        
        # Mostrar primeiros 3 pontos de preco
        print(f"\nPrimeiros 3 pontos de preco:")
        for i, price_point in enumerate(data.get('prices', [])[:3]):
            timestamp = datetime.fromtimestamp(price_point[0] / 1000)
            price = price_point[1]
            print(f"  {i+1}. {timestamp} - ${price:,.2f}")
        
        # Analise de variacao
        prices = [p[1] for p in data.get('prices', [])]
        if len(prices) >= 2:
            min_price = min(prices)
            max_price = max(prices)
            variation = ((max_price - min_price) / min_price) * 100
            print(f"\nAnalise 24h:")
            print(f"  Preco minimo: ${min_price:,.2f}")
            print(f"  Preco maximo: ${max_price:,.2f}")
            print(f"  Variacao: {variation:.2f}%")
    else:
        print(f"\nERRO: {response.text}")
    
    return response.status_code == 200

def test_rate_limits():
    """
    Testa rate limits fazendo multiplas requisicoes
    """
    print("\n" + "="*80)
    print("TESTE 3: Rate Limits (10 requisicoes consecutivas)")
    print("="*80)
    
    url = f"{BASE_URL}/simple/price"
    params = {
        "ids": "bitcoin",
        "vs_currencies": "usd"
    }
    
    print("\nFazendo 10 requisicoes...")
    for i in range(10):
        start = time.time()
        response = requests.get(url, params=params)
        elapsed = time.time() - start
        
        remaining = response.headers.get('x-ratelimit-remaining', 'N/A')
        print(f"  Req {i+1}: Status {response.status_code} | "
              f"Time {elapsed:.2f}s | Remaining: {remaining}")
        
        time.sleep(0.5)  # Pequeno delay entre requests
    
    print("\nConclusao: Rate limit Free Tier = ~10-50 calls/min")

def main():
    print("\n" + "="*80)
    print("TESTE DA API COINGECKO - EXPLORACAO E VALIDACAO")
    print("="*80)
    print(f"Data/Hora: {datetime.now()}")
    print(f"Base URL: {BASE_URL}")
    print(f"Criptomoedas: {', '.join(CRYPTO_IDS)}")
    
    # Executar testes
    test1_ok = test_simple_price()
    time.sleep(2)  # Delay entre testes
    
    test2_ok = test_market_chart()
    time.sleep(2)
    
    test_rate_limits()
    
    # Resumo
    print("\n" + "="*80)
    print("RESUMO DOS TESTES")
    print("="*80)
    print(f"Teste 1 (Simple Price): {'OK' if test1_ok else 'FALHOU'}")
    print(f"Teste 2 (Market Chart): {'OK' if test2_ok else 'FALHOU'}")
    
    print("\n--- ESTRUTURA DOS DADOS PARA SCALA ---")
    print("Simple Price Response:")
    print("  Map[String, CryptoPrice]")
    print("  CryptoPrice: usd, brl, usd_24h_vol, usd_24h_change, last_updated_at")
    print("\nMarket Chart Response:")
    print("  prices: List[(timestamp, price)]")
    print("  market_caps: List[(timestamp, market_cap)]")
    print("  total_volumes: List[(timestamp, volume)]")

if __name__ == "__main__":
    main()
