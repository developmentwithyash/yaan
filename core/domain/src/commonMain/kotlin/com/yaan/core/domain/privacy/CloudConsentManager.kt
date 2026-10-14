package com.yaan.core.domain.privacy

import kotlinx.serialization.Serializable

/**
 * User consent state for offloading reasoning to external cloud inference.
 */
@Serializable
enum class CloudConsentStatus {
    UNPROMPTED,           // First use; user must be shown disclosure before any cloud call
    CONSENTED_CLOUD,      // User agreed to send minimised, redacted snippets to cloud LLM
    LOCAL_ONLY_OPT_OUT    // User explicitly rejected cloud; all queries remain 100% on-device
}

/**
 * Transparent disclosure payload displayed in UI prior to cloud opt-in.
 */
data class CloudConsentDisclosure(
    val title: String = "Cloud-Assisted Vault Reasoning",
    val whatLeavesDevice: String = "Only top-3 search snippets relevant to your question. Your full vault stays on your device.",
    val privacyProtection: String = "Phone numbers, emails, card numbers, and secret codes are replaced with anonymous placeholders on-device before transmission.",
    val processorAndRetention: String = "Processed via cloud inference gateways. Standard provider data retention applies (up to 30 days for abuse monitoring unless zero-retention is active). No training on your data.",
    val localAlternative: String = "You can choose 'Ask locally only' at any time. Search and manual actions will always work 100% offline."
)

/**
 * State machine managing cloud consent, per-query overrides, and remote kill switch.
 */
class CloudConsentManager(
    private var initialStatus: CloudConsentStatus = CloudConsentStatus.UNPROMPTED,
    private var remoteKillSwitchActive: Boolean = false
) {
    private var currentStatus: CloudConsentStatus = initialStatus

    fun getStatus(): CloudConsentStatus = currentStatus

    fun isRemoteKillSwitchActive(): Boolean = remoteKillSwitchActive

    fun setRemoteKillSwitch(active: Boolean) {
        remoteKillSwitchActive = active
    }

    /**
     * Determines whether cloud inference can be safely invoked for a query.
     * Evaluates consent, remote kill switch, and per-query "local-only" preference.
     */
    fun canUseCloudInference(forceLocalOnlyForQuery: Boolean = false): Boolean {
        if (remoteKillSwitchActive) return false
        if (forceLocalOnlyForQuery) return false
        return currentStatus == CloudConsentStatus.CONSENTED_CLOUD
    }

    /**
     * Records explicit user opt-in to cloud assistance.
     */
    fun grantConsent() {
        currentStatus = CloudConsentStatus.CONSENTED_CLOUD
    }

    /**
     * Records explicit user rejection / revocation of cloud assistance.
     */
    fun revokeConsent() {
        currentStatus = CloudConsentStatus.LOCAL_ONLY_OPT_OUT
    }

    /**
     * Resets consent to prompt the user again.
     */
    fun resetConsent() {
        currentStatus = CloudConsentStatus.UNPROMPTED
    }

    companion object {
        val DEFAULT_DISCLOSURE = CloudConsentDisclosure()
    }
}
