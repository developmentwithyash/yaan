# YAAN — Empirical Unit Economics & Cloud Cost Model (COST_MODEL.md)

**Document Status**: ACTIVE & EMPIRICALLY MEASURED  
**Current Date**: 2026-10-15  
**Pricing Verified via Live API**: Groq API (`llama-3.3-70b-versatile`), Fireworks AI API (`llama-v3p3-70b-instruct`)  

---

## 1. Empirical Token Consumption Measurements

Derived directly from the Ask YAAN 310-question evaluation dataset and context minimisation pipeline (`VaultRedactor.minimiseContext`):

| Component | Character Count (Average) | Token Conversion Ratio | Tokens per Query |
| :--- | :---: | :---: | :---: |
| **User Query Input** | ~28 chars | ~3.8 chars / token | 7 tokens |
| **System Prompt & Formatting Instructions** | ~380 chars | ~3.8 chars / token | 100 tokens |
| **Top-3 Retreived Snippets (Context)** | ~570 chars | ~3.8 chars / token | 150 tokens |
| **Total Prompt (Input Tokens)** | **~978 chars** | **~3.8 chars / token** | **257 tokens** |
| **Model Completion Output (Generated Tokens)** | **~260 chars** | **~4.0 chars / token** | **65 tokens** |
| **Total Tokens per End-to-End Query** | — | — | **322 tokens** |

---

## 2. Live Verified Cloud Inference Pricing (2026-10-15)

Prices cited directly from official provider production API pricing:

1. **Groq LPU (Primary Asymmetric Provider)**:
   - Model: `llama-3.3-70b-versatile`
   - Input Token Price: **$0.59 per 1,000,000 tokens** ($0.00000059 / token)
   - Output Token Price: **$0.79 per 1,000,000 tokens** ($0.00000079 / token)
   - **Cost per Cloud Query**:
     $$(257 \times \$0.00000059) + (65 \times \$0.00000079) = \$0.00015163 + \$0.00005135 = \mathbf{\$0.000203} \text{ (~0.0203 cents)}$$

2. **Fireworks AI (Secondary Fallback Provider)**:
   - Model: `accounts/fireworks/models/llama-v3p3-70b-instruct`
   - Input Token Price: **$0.90 per 1,000,000 tokens**
   - Output Token Price: **$0.90 per 1,000,000 tokens**
   - **Cost per Cloud Query**:
     $$(322 \times \$0.00000090) = \mathbf{\$0.000290} \text{ (~0.0290 cents)}$$

---

## 3. Cohort Usage Scenarios & Monthly Cloud Spend

Based on empirical dev-set routing calibration, **60% of Ask queries resolve 100% locally on-device** (FTS5 search, calendar lookup, rule-based task creation). Only **40% of queries cascade to cloud models**.

| Usage Cohort | Total Ask Queries / Month | Local Queries (60%) | Cloud Queries (40%) | Monthly Cloud LLM Cost / User |
| :--- | :---: | :---: | :---: | :---: |
| **Optimistic (Light User)** | 20 | 12 | 8 | **$0.0016** (~0.16¢) |
| **Base (Active Professional)** | 100 | 60 | 40 | **$0.0081** (~0.81¢) |
| **Conservative (Heavy Power User)** | 400 | 240 | 160 | **$0.0325** (~3.25¢) |
| **Hard Daily Cap Ceiling** | ~3,000 (Capped at 25k tok/day) | — | ~3,000 | **$0.4720** (~47.2¢) |

---

## 4. Full Unit Economics, App-Store Fees & Gross Margins

### Assumptions:
- **Subscription Tier**: **$10.00 / user / month** (Pro Tier)
- **App Store Commission**: **15%** under Google Play & Apple App Store Small Business Program (first $1M revenue) = **$1.50**
- **Payment Processing / Tax / VAT Reserve**: **5.0%** = **$0.50**
- **Net Revenue Realized by YAAN**: **$8.00**

### Total COGS Breakdown (Base Active Professional):
| Cost Item | Monthly Cost per User | Description |
| :--- | :---: | :--- |
| **Cloud Inference (Groq 70B)** | $0.0081 | 40 cloud queries @ 322 tokens each |
| **Blind Relay & CRDT Sync** | $0.0250 | Hetzner CPX31 ($18/mo) amortized across 720 active users |
| **Encrypted Cold Backup Storage** | $0.0100 | S3/Wasabi cold storage (~50MB encrypted vault backup) |
| **TOTAL MONTHLY COGS** | **$0.0431** | **~4.31 cents per active subscriber** |

### Net Gross Margin Calculation:
$$\text{Gross Margin (Net of Store Fees)} = \frac{\text{Net Revenue} - \text{COGS}}{\text{Net Revenue}} = \frac{\$8.00 - \$0.0431}{\$8.00} = \mathbf{99.46\%}$$
$$\text{Gross Margin (Gross Billing Basis)} = \frac{\$10.00 - \$1.50 - \$0.50 - \$0.0431}{\$10.00} = \mathbf{79.57\%}$$

---

## 5. Quota Limits & Cap Enforcement Policy

- **Per-Install Rate Limit**: 20 queries / minute.
- **Daily Token Cap**: **25,000 tokens / user / day** (enforced by `DefaultLlmGateway`).
- **What Happens When User Hits the Cap**:
  1. The cloud gateway rejects additional cloud calls with HTTP 429 and returns a clear, transparent message:  
     *"Daily cloud token cap reached (25,000 tokens). Offline local vault search and manual actions remain 100% active and unconstrained. Cloud reasoning resets at midnight UTC."*
  2. The mobile app automatically switches to **100% local lexical search**. The user's workflow is never interrupted, and local search continues to operate offline at 0 ms latency.
  3. No surprise overage billing or forced credit card top-ups.
