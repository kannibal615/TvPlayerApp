package com.smartvision.svplayer.recorder

import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecorderController(
    private val context: Context,
    private val repository: RecordingRepository,
) {
    suspend fun startLiveRecording(
        title: String,
        streamUrl: String,
        durationMs: Long,
        programTitle: String?,
        programStartMs: Long?,
        programStopMs: Long?,
    ): Result<RecordingStartResult> = runCatching {
        withContext(Dispatchers.IO) {
            require(streamUrl.isNotBlank()) { "Recording stream is unavailable." }
            require(durationMs in MIN_DURATION_MS..MAX_DURATION_MS) { "Recording duration is outside the supported range." }
            check(!repository.hasActiveJob()) { "A recording is already running." }
            val request = RecordingRequest(
                jobId = UUID.randomUUID().toString(),
                title = title.ifBlank { "Live TV" },
                streamUrl = streamUrl,
                durationMs = durationMs,
                sourceType = RecordingSourceType.Live,
                programTitle = programTitle,
                programStartMs = programStartMs,
                programStopMs = programStopMs,
            )
            repository.createQueued(request)
            val intent = RecordingService.startIntent(context, request)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            RecordingStartResult(request.jobId, request.durationMs)
        }
    }

    private companion object {
        const val MIN_DURATION_MS = 60_000L
        const val MAX_DURATION_MS = 12 * 60 * 60 * 1000L
    }
}
