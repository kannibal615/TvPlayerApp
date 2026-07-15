package com.smartvision.svplayer.sync

data class SyncSchedulePolicy(
    val repeatHours: Long? = null,
    val runOnStartup: Boolean = false,
)

object SyncFrequencyPolicy {
    fun from(value: String): SyncSchedulePolicy =
        when (value.trim()) {
            "24h" -> SyncSchedulePolicy(repeatHours = 24L)
            "48h" -> SyncSchedulePolicy(repeatHours = 48L)
            "A chaque demarrage" -> SyncSchedulePolicy(runOnStartup = true)
            "Manuelle", "Jamais" -> SyncSchedulePolicy()
            else -> SyncSchedulePolicy(repeatHours = 24L)
        }

    fun isSynchronizationDue(
        value: String,
        lastSyncAt: Long?,
        hasLocalCatalog: Boolean,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        if (!hasLocalCatalog || lastSyncAt == null) return true
        val policy = from(value)
        if (policy.runOnStartup) return true
        val repeatHours = policy.repeatHours ?: return false
        return nowMs - lastSyncAt >= java.util.concurrent.TimeUnit.HOURS.toMillis(repeatHours)
    }
}
