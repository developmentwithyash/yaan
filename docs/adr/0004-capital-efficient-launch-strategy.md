# ADR 0004: Capital-Efficient Bootstrap Launch Strategy & Infinite Runway Governance

## Status
Accepted (Unanimous 12/12 Acquittal by Idea Jury — Docket `JURY-2026-10-YAAN-004`)

## Context
Standard AI product launches incur heavy upfront capital burn: expensive cloud Kubernetes clusters, paid marketing ad spend, and bloated customer support SaaS. To launch YAAN with maximum capital efficiency, the executive team formulated the "Bootstrap Velocity Launch Plan" designed to achieve Day-1 profitability, infinite runway, and $37,000+ upfront cash buffer on less than $30/month fixed infrastructure burn.

## Decision
We adopt the **Bootstrap Velocity Launch Architecture**:

1. **Local-Only Zero-Cloud Free Tier**:
   - Free users operate 100% on-device (local SQLite + quantized Edge SLM).
   - Zero server-side compute, database, or token consumption for non-paying users ($0 marginal serving cost).
2. **Micro-Sovereign Infrastructure ($22.50/Month Fixed Burn)**:
   - Single Hetzner Cloud ARM64 instance ($4.50/mo) hosting GraalVM Native Ktor services, PostgreSQL 16, and Redis 7 Streams.
   - Litestream continuous WAL replication to Cloudflare R2 ($1.50/mo), achieving 1-second RPO disaster recovery without cloud database management overhead.
   - Zero-egress asset distribution via Cloudflare R2 and Cloudflare CDN ($0.00 bandwidth fees).
3. **Pre-Funded Launch via "Founding 250" Club**:
   - Early bird pre-sale of 250 Lifetime Member passes at $149–$199 (generating $37,250 in non-dilutive upfront capital).
   - Enforce fair-use cloud token quotas (1,000 tokens/day) on lifetime passes to prevent long-term liability.
4. **Organic $0-CAC Distribution Waves**:
   - Focus launch exclusively on high-conversion tech communities: Show HN (Hacker News), r/LocalLLaMA, r/ObsidianMD, r/selfhosted, and Product Hunt.
   - Embed viral onboarding hooks via client-decrypted "Share My Week" encrypted briefing links (`https://yaan.ai/s/abc#key=xyz`).
5. **Zero-SaaS Lean Tooling**:
   - Discord community and GitHub Discussions replace Intercom/Zendesk ($0).
   - Self-hosted Umami analytics and GlitchTip error tracking replace Mixpanel/Datadog ($0).

## Consequences
- Day-1 profitability with over $37,000 net free cash buffer in the bank.
- Fixed monthly burn capped at $22.50/month (over 1,600 months of runway).
- Eliminates paid ad dependencies and SaaS vendor lock-in.
- Unanimously acquitted by the 12 Institutional Jurors with zero fatal charges.
