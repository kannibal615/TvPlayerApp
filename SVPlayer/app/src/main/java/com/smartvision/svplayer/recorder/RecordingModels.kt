package com.smartvision.svplayer.recorder

data class RecordingRequest(
    val jobId: String,
    val title: String,
    val streamUrl: String,
    val durationMs: Long,
    val sourceType: RecordingSourceType,
    val programTitle: String?,
    val programStartMs: Long?,
    val programStopMs: Long?,
)

data class RecordingStartResult(
    val jobId: String,
    val durationMs: Long,
)

enum class RecordingSourceType(val key: String) {
    Live("live"),
}

enum class RecordingJobStatus(val key: String) {
    Queued("queued"),
    Running("running"),
    Completed("completed"),
    Failed("failed"),
    Cancelled("cancelled"),
}

data class RecordingOutputTarget(
    val tempAbsolutePath: String,
    val finalAbsolutePath: String,
    val finalRelativePath: String,
)

data class RecordingEngineRequest(
    val jobId: String,
    val streamUrl: String,
    val durationMs: Long,
    val output: RecordingOutputTarget,
)

class RecordingException(message: String, cause: Throwable? = null) : Exception(message, cause)
