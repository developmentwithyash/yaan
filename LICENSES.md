# YAAN — Model & Runtime Commercial Licensing Audit (LICENSES.md)

**Document Status**: ACTIVE & AUDITED  
**Auditor**: Staff ML & Security Reviewer  
**Audit Date**: 2026-10-15  
**Policy**: **DO NOT SHIP ANY MODEL OR RUNTIME WHOSE LICENSE HAS NOT BEEN INDEPENDENTLY CONFIRMED FOR COMMERCIAL SAAS DISTRIBUTION.**

---

## 1. Executive Summary & Audit Matrix

| Model / Runtime | Upstream Author / Org | License Type | Commercial Use | Attribution Required | Redistribution Allowed | Status | Action / Constraints |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **SmolLM2-360M / 1.7B** | Hugging Face TB | **Apache 2.0** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Primary candidate for edge intent routing. Clean commercial use. |
| **Llama 3.3 (70B / 8B / 3B / 1B)** | Meta Platforms, Inc. | **Llama 3.3 Community License** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Free commercial use up to 700M monthly active users. Must display "Built with Llama". |
| **Gemma 2 (2B / 9B)** | Google LLC | **Gemma Terms of Use** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Commercial use permitted. Prohibited from training competing non-Gemma foundational models. |
| **Qwen 2.5 (0.5B to 72B)** | Alibaba Cloud | **Apache 2.0** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Alibaba transitioned Qwen 2.5 models to Apache 2.0. Unrestricted commercial distribution. |
| **Whisper (Tiny / Base / Small)** | OpenAI | **MIT** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Permissive commercial license. Applicable for on-device STT quantization. |
| **Silero VAD** | Silero / Snakers4 | **MIT** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Lightweight voice activity detection (< 2 MB). Permissive commercial license. |
| **BGE (bge-small-en-v1.5)** | BAAI | **MIT / Apache 2.0** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Permissive. (Note: Currently rejected for v1 on empirical accuracy grounds). |
| **Kokoro-82M TTS** | hexgrad | **Apache 2.0** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Ultra-compact 82M TTS model. Clean commercial terms. |
| **Piper TTS (Engine)** | Rhasspy / Michael Hansen | **MIT** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Core engine is MIT. |
| **Piper Voice Checkpoints** | Various Contributors | **Mixed (CC0, CC-BY, CC-BY-NC)** | ⚠️ **CONDITIONAL** | ⚠️ **VARIES** | ⚠️ **VARIES** | **RESTRICTED** | **CRITICAL WARNING**: Certain Piper voices are licensed under CC-BY-NC (Non-Commercial). Only CC0 or MIT-verified voice datasets may be bundled. |
| **llama.cpp** | Georgi Gerganov & Contributors | **MIT** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Universal CPU/GPU quantized inference engine. Clean commercial license. |
| **ExecuTorch** | Meta Platforms / PyTorch | **BSD-3-Clause** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Mobile inference runtime. Clean commercial license. |
| **ONNX Runtime Mobile** | Microsoft Corporation | **MIT** | ✅ YES | ✅ YES | ✅ YES | **CONFIRMED** | Cross-platform inference engine. Clean commercial license. |

---

## 2. Detailed License Terms & Compliance Invariants

### 2.1 SmolLM2 (Apache 2.0)
- **Permissions**: Commercial use, modification, distribution, sublicensing, private use.
- **Conditions**: Include copyright notice (`Copyright 2024 Hugging Face`) and Apache 2.0 license copy.
- **Patent Grant**: Express patent license granted by contributors.
- **Risk Assessment**: **Zero Residual Risk**. Ideal candidate for edge intent parsing.

### 2.2 Meta Llama 3.3 Community License
- **User Threshold**: If monthly active users exceed 700,000,000 in any calendar month, a commercial license must be requested from Meta. (YAAN is well below this threshold).
- **Attribution Invariant**: Any UI or documentation presenting Llama outputs must state: *"Built with Llama"*.
- **Data Use Invariant**: Output from Llama models may not be used to improve any model other than Llama derivatives.

### 2.3 Google Gemma Terms of Use
- **Safety Policy**: Must adhere to Google Prohibited Use Policy (no malware generation, cyberattacks, or illegal exploitation).
- **Redistribution**: Must distribute a copy of the Gemma Terms of Use and Attribution Notice.

### 2.4 Piper Voice Checkpoint Warning
> [!CAUTION]
> While the Piper engine binary is MIT-licensed, individual voice models on Hugging Face were trained on heterogeneous corpora:
> - **CC0 / Public Domain voices** (e.g. `en_US-libritts-high`): **APPROVED FOR COMMERCIAL SHIP**.
> - **CC-BY-NC voices**: **STRICTLY PROHIBITED**. Shipping CC-BY-NC voices in a paid subscription SaaS violates copyright law and exposes YAAN to statutory infringement damages.
> - **Founder Mandate**: Every voice model bundled into the APK must be individually audited against `rhasspy/piper-voices` metadata.

---

## 3. Shipping Decision for Ask YAAN v1
1. **On-Device Default**: **No model weights shipped in the base APK download**. The base app relies on zero-weight Lexical FTS5 + Rule Parser.
2. **Optional Edge SLM Download**: `SmolLM2-360M INT4` (Apache 2.0) is configured in `ModelDownloadManager` for optional on-demand download on Tier-2/Tier-3 devices over Wi-Fi.
3. **Cloud Gateway Inference**: Hosted Llama 3.3 70B via Groq/Fireworks APIs (commercial SaaS compliant).
