# ADR 0003: Ultra-Lean Cost Minimization Architecture & 98%+ Gross Margin Governance

## Status
Accepted

## Context
Following the acceptance of ADR 0002 (Asymmetric Acceleration Hub), the engineering team conducted an architectural cost review to aggressively optimize operational expenditures across cloud infrastructure, AI tokens, databases, model delivery, and third-party APIs. The goal: drive blended COGS down from $0.92/user/month to under $0.18/user/month, achieving 98%+ gross margins while enhancing speed and privacy.

## Decision
We adopt the **Ultra-Lean Cost Minimization Architecture**:

1. **Extreme Edge Offload (95% Local Baseline)**:
   - Quantized 4-bit domain SLMs (SmolLM2-360M / Qwen2.5-0.5B / Gemma-2-2B INT4) handle 95% of user requests on-device at $0 marginal compute cost.
2. **Nightly Asynchronous Batch API Arbitrage**:
   - Daily Briefings are submitted to Anthropic/DeepSeek asynchronous Batch endpoints at 02:00 AM off-peak, unlocking a 50% API price reduction ($0.0015/brief) and delivered before 07:00 AM wake-up.
3. **Zero-Kafka Lean Cloud Core**:
   - Defer dedicated Kafka clusters until 200,000 MAU. Utilize PostgreSQL LISTEN/NOTIFY with transactional outbox and Redis 7 Streams.
   - Run GraalVM native binaries on Hetzner / Fly.io ARM64 instances, consuming < 40MB RAM per microservice.
4. **Zero-Egress Distribution & Native OS Neural Substrate**:
   - Model weights and micro-LoRA adapters hosted exclusively on Cloudflare R2 ($0 egress fees).
   - Dynamically detect and leverage native Android AICore or iOS Apple Foundation Models to eliminate custom base model downloads.
5. **Event-Driven Webhook Subscriptions**:
   - Replace continuous 5-minute calendar polling with event-driven Webhook push notifications (Google Watch API & Microsoft Graph Subscriptions), cutting 98% of sync worker compute.
6. **SMS Fallback Quota & Pass-Through**:
   - Free tier: Push-only ($0). Pro tier: 5 critical SMS/month. Additional SMS sold as pass-through add-on packs ($1 for 20 SMS).
7. **Negative Working Capital Annual Packaging**:
   - Default annual billing of $149/year upfront, saving 44% in Stripe payment processing fees while providing upfront cash flow to fund infrastructure.

## Consequences
- Blended COGS reduced by **84.8%** from $0.92 to **$0.14 / user / month**.
- Pro tier gross margin expands to **99.2%** ($18/mo) and **98.9%** ($149/yr).
- 95% edge execution enhances user privacy, eliminates network latency, and lowers compliance audit overhead.
- Sustainable infrastructure cost profile from day one.
