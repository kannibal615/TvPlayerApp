package com.smartvision.svplayer.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.smartvision.svplayer.MainActivity
import com.smartvision.svplayer.R
import com.smartvision.svplayer.SVPlayerApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RecordingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager
    private var activeJob: Job? = null
    private var activeJobId: String? = null
    private var stopRequested = false

    private val container by lazy { (application as SVPlayerApplication).appContainer }
    private val engine by lazy { container.recordingEngine }
    private val storageManager by lazy { container.mediaStorageManager }
    private val recordingRepository by lazy { container.recordingRepository }
    private val mediaRepository by lazy { container.mediaRepository }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> handleStop(intent.getStringExtra(EXTRA_JOB_ID))
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine.cancel()
        activeJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleStart(intent: Intent) {
        val request = intent.toRecordingRequest() ?: run {
            stopSelf()
            return
        }
        if (activeJob != null) return
        activeJobId = request.jobId
        stopRequested = false
        startRecorderForeground(buildNotification("Preparing recording", request.title, ongoing = true, request.jobId))
        activeJob = serviceScope.launch {
            runRecording(request)
        }
    }

    private fun handleStop(jobId: String?) {
        if (jobId == null || jobId != activeJobId) return
        stopRequested = true
        engine.cancel()
        activeJob?.cancel()
    }

    private suspend fun runRecording(request: RecordingRequest) {
        val output = storageManager.prepareRecordingTarget(
            title = recordingFileTitle(request),
            extension = request.streamUrl.recordingExtension(),
        )
        try {
            recordingRepository.markRunning(request.jobId, output.finalRelativePath)
            notifyRecorder("Recording in progress", request.title, ongoing = true, request.jobId)
            engine.record(
                request = RecordingEngineRequest(
                    jobId = request.jobId,
                    streamUrl = request.streamUrl,
                    durationMs = request.durationMs,
                    output = output,
                ),
                shouldStop = { stopRequested },
                onProgress = {},
            )
            if (stopRequested) throw CancellationException("Recording cancelled.")
            storageManager.finalizeRecording(output)
            val mediaFileId = mediaRepository.indexRecording(output.finalRelativePath)
            recordingRepository.markCompleted(request.jobId, mediaFileId, output.finalRelativePath)
            notifyRecorder("Recording completed", request.title, ongoing = false, request.jobId)
        } catch (cancelled: CancellationException) {
            storageManager.discardRecording(output)
            recordingRepository.markCancelled(request.jobId)
            notifyRecorder("Recording cancelled", request.title, ongoing = false, request.jobId)
        } catch (throwable: Throwable) {
            storageManager.discardRecording(output)
            recordingRepository.markFailed(request.jobId, throwable.message ?: "Recording failed.")
            notifyRecorder("Recording failed", throwable.message ?: request.title, ongoing = false, request.jobId)
        } finally {
            activeJob = null
            activeJobId = null
            stopRequested = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            stopSelf()
        }
    }

    private fun notifyRecorder(title: String, text: String, ongoing: Boolean, jobId: String) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(title, text, ongoing, jobId),
        )
    }

    private fun startRecorderForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, text: String, ongoing: Boolean, jobId: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_JOB_ID, jobId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openPendingIntent)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .apply {
                if (ongoing) {
                    setProgress(0, 0, true)
                    addAction(0, "Stop", stopPendingIntent)
                }
            }
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "SmartVision Recorder",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Notifications for SmartVision TV recordings."
            },
        )
    }

    private fun Intent.toRecordingRequest(): RecordingRequest? {
        val jobId = getStringExtra(EXTRA_JOB_ID) ?: return null
        val title = getStringExtra(EXTRA_TITLE) ?: return null
        val streamUrl = getStringExtra(EXTRA_STREAM_URL) ?: return null
        val durationMs = getLongExtra(EXTRA_DURATION_MS, 0L).takeIf { it > 0L } ?: return null
        return RecordingRequest(
            jobId = jobId,
            title = title,
            streamUrl = streamUrl,
            durationMs = durationMs,
            sourceType = RecordingSourceType.Live,
            programTitle = getStringExtra(EXTRA_PROGRAM_TITLE),
            programStartMs = getLongExtra(EXTRA_PROGRAM_START_MS, -1L).takeIf { it > 0L },
            programStopMs = getLongExtra(EXTRA_PROGRAM_STOP_MS, -1L).takeIf { it > 0L },
        )
    }

    private fun recordingFileTitle(request: RecordingRequest): String {
        val base = request.programTitle?.takeIf { it.isNotBlank() } ?: request.title
        return "$base ${System.currentTimeMillis()}"
    }

    companion object {
        private const val ACTION_START = "com.smartvision.svplayer.recorder.START"
        private const val ACTION_STOP = "com.smartvision.svplayer.recorder.STOP"
        private const val EXTRA_JOB_ID = "job_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_STREAM_URL = "stream_url"
        private const val EXTRA_DURATION_MS = "duration_ms"
        private const val EXTRA_PROGRAM_TITLE = "program_title"
        private const val EXTRA_PROGRAM_START_MS = "program_start_ms"
        private const val EXTRA_PROGRAM_STOP_MS = "program_stop_ms"
        private const val CHANNEL_ID = "smartvision_recorder"
        private const val NOTIFICATION_ID = 7201

        fun startIntent(context: Context, request: RecordingRequest): Intent =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_JOB_ID, request.jobId)
                putExtra(EXTRA_TITLE, request.title)
                putExtra(EXTRA_STREAM_URL, request.streamUrl)
                putExtra(EXTRA_DURATION_MS, request.durationMs)
                putExtra(EXTRA_PROGRAM_TITLE, request.programTitle)
                putExtra(EXTRA_PROGRAM_START_MS, request.programStartMs ?: -1L)
                putExtra(EXTRA_PROGRAM_STOP_MS, request.programStopMs ?: -1L)
            }
    }
}

private fun String.recordingExtension(): String {
    val lower = substringBefore('?').substringBefore('#').lowercase()
    return when {
        lower.endsWith(".mp4") -> "mp4"
        lower.endsWith(".mkv") -> "mkv"
        lower.endsWith(".webm") -> "webm"
        lower.endsWith(".m3u8") -> "ts"
        else -> "ts"
    }
}
