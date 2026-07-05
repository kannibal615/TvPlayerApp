package com.smartvision.svplayer.recorder

import com.smartvision.svplayer.data.local.dao.MediaCenterDao
import com.smartvision.svplayer.data.local.entity.RecordingJobEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordingRepository(
    private val dao: MediaCenterDao,
) {
    suspend fun hasActiveJob(): Boolean = withContext(Dispatchers.IO) {
        val activeJob = dao.getLatestRecordingJobByStatuses(
            listOf(
                RecordingJobStatus.Queued.key,
                RecordingJobStatus.Running.key,
            ),
        ) ?: return@withContext false
        val stale = System.currentTimeMillis() - activeJob.updatedAt > STALE_ACTIVE_JOB_MS
        if (stale) {
            dao.updateRecordingJobState(
                id = activeJob.id,
                status = RecordingJobStatus.Failed.key,
                mediaFileId = activeJob.mediaFileId,
                outputRelativePath = activeJob.outputRelativePath,
                startedAt = activeJob.startedAt,
                endedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                errorMessage = "Recording job was interrupted before completion.",
            )
            false
        } else {
            true
        }
    }

    suspend fun createQueued(request: RecordingRequest) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.upsertRecordingJob(
            RecordingJobEntity(
                id = request.jobId,
                mediaFileId = null,
                status = RecordingJobStatus.Queued.key,
                sourceType = request.sourceType.key,
                title = request.programTitle?.takeIf { it.isNotBlank() } ?: request.title,
                outputRelativePath = null,
                startedAt = null,
                endedAt = null,
                updatedAt = now,
                errorMessage = null,
            ),
        )
    }

    suspend fun markRunning(jobId: String, outputRelativePath: String) = updateState(
        jobId = jobId,
        status = RecordingJobStatus.Running,
        outputRelativePath = outputRelativePath,
        startedAt = System.currentTimeMillis(),
        endedAt = null,
        errorMessage = null,
    )

    suspend fun markCompleted(jobId: String, mediaFileId: Long?, outputRelativePath: String) = updateState(
        jobId = jobId,
        status = RecordingJobStatus.Completed,
        mediaFileId = mediaFileId,
        outputRelativePath = outputRelativePath,
        endedAt = System.currentTimeMillis(),
        errorMessage = null,
    )

    suspend fun markFailed(jobId: String, message: String) = updateState(
        jobId = jobId,
        status = RecordingJobStatus.Failed,
        endedAt = System.currentTimeMillis(),
        errorMessage = message,
    )

    suspend fun markCancelled(jobId: String) = updateState(
        jobId = jobId,
        status = RecordingJobStatus.Cancelled,
        endedAt = System.currentTimeMillis(),
        errorMessage = null,
    )

    private suspend fun updateState(
        jobId: String,
        status: RecordingJobStatus,
        mediaFileId: Long? = null,
        outputRelativePath: String? = null,
        startedAt: Long? = null,
        endedAt: Long? = null,
        errorMessage: String?,
    ) = withContext(Dispatchers.IO) {
        val current = dao.getRecordingJob(jobId)
        dao.updateRecordingJobState(
            id = jobId,
            status = status.key,
            mediaFileId = mediaFileId ?: current?.mediaFileId,
            outputRelativePath = outputRelativePath ?: current?.outputRelativePath,
            startedAt = startedAt ?: current?.startedAt,
            endedAt = endedAt,
            updatedAt = System.currentTimeMillis(),
            errorMessage = errorMessage,
        )
    }

    private companion object {
        const val STALE_ACTIVE_JOB_MS = 13 * 60 * 60 * 1000L
    }
}
