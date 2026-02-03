import asyncio
import json
import hashlib
import os
from aiokafka import AIOKafkaConsumer, errors as aiokafka_errors
import aiohttp
import aioredis

BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
REDIS_HOST = os.getenv("REDIS_HOST", "redis")
ANALYTICS_URL = "http://analytics-mock:8080/analytics/data"

customer_cache = {}
inventory_cache = {}

def generate_key(payload):
    """Generate a unique idempotency key for a payload."""
    return hashlib.sha256(json.dumps(payload, sort_keys=True).encode()).hexdigest()


async def send_to_analytics(payload, retries=3):
    """Send merged payload to analytics service with retries."""
    for attempt in range(1, retries + 1):
        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(ANALYTICS_URL, json=payload) as resp:
                    if resp.status == 200:
                        print(f"[Analytics] Sent: {payload}")
                        return True
                    else:
                        print(f"[Analytics] Failed with status {resp.status}")
        except Exception as e:
            print(f"[Analytics] Attempt {attempt} failed: {e}")
        await asyncio.sleep(2 ** attempt)
    return False


async def start_consumer_with_retry(consumer, retries=10, delay=5):
    """Wait for Kafka to be ready before starting the consumer."""
    for attempt in range(1, retries + 1):
        try:
            await consumer.start()
            print("[Kafka] Connected successfully!")
            return
        except aiokafka_errors.KafkaConnectionError as e:
            print(f"[Kafka] Connection attempt {attempt}/{retries} failed: {e}")
            await asyncio.sleep(delay)
    raise RuntimeError("Failed to connect to Kafka after multiple attempts.")

async def consume():
    consumer = AIOKafkaConsumer(
        "customer_data",
        "inventory_data",
        bootstrap_servers=BOOTSTRAP,
        group_id="analytics-group",
        enable_auto_commit=False,
        auto_offset_reset="earliest"  # start from beginning if group_id is new
    )

    await start_consumer_with_retry(consumer)

    redis = await aioredis.from_url(f"redis://{REDIS_HOST}")

    try:
        async for msg in consumer:
            payload = json.loads(msg.value)
            key = generate_key(payload)

            if await redis.exists(key):
                await consumer.commit()
                continue

            if msg.topic == "customer_data":
                customer_cache[payload["id"]] = payload
            elif msg.topic == "inventory_data":
                inventory_cache[payload["id"]] = payload

            if payload["id"] in customer_cache and payload["id"] in inventory_cache:
                merged = {
                    "customer": customer_cache[payload["id"]],
                    "inventory": inventory_cache[payload["id"]]
                }
                success = await send_to_analytics(merged)
                if success:
                    await redis.set(key, "1", ex=86400)
                    await consumer.commit()
    finally:
        await consumer.stop()
        await redis.close()

if __name__ == "__main__":
    try:
        asyncio.run(consume())
    except KeyboardInterrupt:
        print("\n[Consumer] Shutting down.")
