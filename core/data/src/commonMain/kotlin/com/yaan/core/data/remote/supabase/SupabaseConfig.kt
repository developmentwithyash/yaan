package com.yaan.core.data.remote.supabase

import kotlinx.serialization.Serializable

/**
 * Supabase configuration tokens and endpoints for YAAN.
 */
@Serializable
data class SupabaseConfig(
    val url: String = DEFAULT_URL,
    val anonKey: String = DEFAULT_ANON_KEY
) {
    companion object {
        const val DEFAULT_URL = "https://hohvnkootsvoesefoxjw.supabase.co"
        const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhvaHZua29vdHN2b2VzZWZveGp3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE2NDM1NTQsImV4cCI6MjEwNzIxOTU1NH0.vsRBL06SaBfVL2s5egQowbi0ATZH2sj0bSs0IsW9mQ4"
        const val DEFAULT_PUBLISHABLE_KEY = "sb_publishable_QLAIKaTzkqp0G0AStO5jcg_UCq__vjA"
    }
}
