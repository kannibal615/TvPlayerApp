package com.smartvision.svplayer.ui.profile

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.CredentialsMode
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.config.PlaylistProfileStatus
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.AdminProfileAvatarId
import com.smartvision.svplayer.core.config.ProfileAvatarPresetIds
import com.smartvision.svplayer.core.config.KidsProfileAvatarPresetIds
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.core.config.canonicalProfileAvatarId
import com.smartvision.svplayer.core.config.defaultProfileAvatarId
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.data.xtream.XtreamCredentialsValidationResult
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.domain.model.AccountProfile
import com.smartvision.svplayer.domain.model.ParentalControlScope
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.domain.profile.ContentPrefixPolicy
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.NumericPinDialog
import com.smartvision.svplayer.ui.components.PremiumPreviewQr
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
import com.smartvision.svplayer.ui.components.TvDialogSurface
import com.smartvision.svplayer.ui.activation.ScaledActivationLayout
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.HomeVisualBackground
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.ui.home.TvHeader
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.settings.SynchronizationPreferencesContent
import com.smartvision.svplayer.startup.BackgroundSyncScheduler
import com.smartvision.svplayer.sync.SyncFrequencyPolicy
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
internal fun SynchronizationCard(
    lastSync: String,
    syncStatus: SyncStatus,
    onOpenSyncDialog: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.58f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvButton(
            text = syncStatus.buttonLabel,
            onClick = onOpenSyncDialog,
            enabled = syncStatus !is SyncStatus.Running,
            leadingIcon = Icons.Default.CloudSync,
            modifier = Modifier
                .width(if (onDelete == null) 270.dp else 138.dp)
                .height(42.dp),
        )
        if (onDelete != null) {
            Spacer(Modifier.width(8.dp))
            TvButton(
                text = "Supprimer",
                onClick = onDelete,
                enabled = syncStatus !is SyncStatus.Running,
                leadingIcon = Icons.Default.Delete,
                variant = TvButtonVariant.Danger,
                modifier = Modifier
                    .width(132.dp)
                    .height(42.dp),
            )
        }
        Spacer(Modifier.width(20.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(44.dp)
                .background(SmartVisionColors.Border.copy(alpha = 0.74f)),
        )
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            AccountInfoLine(Icons.Default.CheckCircle, "Date de synchronisation", lastSync)
        }
    }
}

private enum class XtreamSyncDialogPhase {
    Confirmation,
    Running,
    Success,
    Error,
}

@Composable
internal fun XtreamSynchronizationDialog(
    state: ProfileUiState,
    syncStatus: SyncStatus,
    onStartSync: suspend () -> Result<Unit>,
    onCancel: () -> Unit,
    onReturn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val startFocusRequester = remember { FocusRequester() }
    val returnFocusRequester = remember { FocusRequester() }
    var phase by remember { mutableStateOf(XtreamSyncDialogPhase.Confirmation) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val blocking = phase == XtreamSyncDialogPhase.Running
    val account = state.activeXtreamAccount
    val source = state.activePlaylistSource
    val sourceConfigured = when (source) {
        PlaylistSource.Xtream -> state.hasXtream
        PlaylistSource.M3u -> state.m3uUrl.isNotBlank()
    }
    val sourceTitle = if (source == PlaylistSource.M3u) "Synchronisation M3U" else "Synchronisation Xtream"
    val progress = syncStatus.catalogProgressOrDefault(state.account)
    val showProgress = phase != XtreamSyncDialogPhase.Confirmation

    fun closeAllowed() {
        if (phase == XtreamSyncDialogPhase.Confirmation) onCancel() else onReturn()
    }

    BackHandler(enabled = true) {
        if (!blocking) closeAllowed()
    }
    LaunchedEffect(Unit) {
        delay(ProfileFocusRequestDelayMillis)
        runCatching { startFocusRequester.requestFocus() }
    }
    LaunchedEffect(phase) {
        if (phase == XtreamSyncDialogPhase.Success || phase == XtreamSyncDialogPhase.Error) {
            delay(ProfileFocusRequestDelayMillis)
            runCatching { returnFocusRequester.requestFocus() }
        }
    }

    Dialog(
        onDismissRequest = { if (!blocking) closeAllowed() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .width(860.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFA071629),
                            Color(0xF5050B16),
                        ),
                    ),
                )
                .border(BorderStroke(1.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.62f)), RoundedCornerShape(8.dp))
                .onPreviewKeyEvent { blocking }
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = SmartVisionColors.CyanAccent,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sourceTitle,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.TitleS,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (source == PlaylistSource.M3u) {
                            "Controlez le lien M3U avant de relancer le chargement Live TV."
                        } else {
                            "Controlez les donnees catalogue avant de lancer la mise a jour."
                        },
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                XtreamSyncCountCard(
                    title = "Live TV",
                    progress = progress.live,
                    showProgress = showProgress,
                    modifier = Modifier.weight(1f),
                )
                XtreamSyncCountCard(
                    title = "Films",
                    progress = progress.movies,
                    showProgress = showProgress,
                    enabled = source == PlaylistSource.Xtream,
                    modifier = Modifier.weight(1f),
                )
                XtreamSyncCountCard(
                    title = "Series",
                    progress = progress.series,
                    showProgress = showProgress,
                    enabled = source == PlaylistSource.Xtream,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(SmartVisionColors.Surface.copy(alpha = 0.48f))
                    .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.72f)), RoundedCornerShape(7.dp))
                    .padding(14.dp),
            ) {
                SyncDialogInfoRow("Code TV", state.tvCode)
                if (source == PlaylistSource.M3u) {
                    SyncDialogInfoRow("Source active", "Lien M3U")
                    SyncDialogInfoRow("Lien M3U", state.m3uUrl.ifBlank { "Non configure" })
                    SyncDialogInfoRow("URL EPG", state.epgUrl.ifBlank { "Non configure" })
                } else {
                    SyncDialogInfoRow("Source active", "Xtream")
                    SyncDialogInfoRow("Url Xtream", account?.host?.ifBlank { state.xtreamHost } ?: state.xtreamHost.ifBlank { "Non configure" })
                    SyncDialogInfoRow("Username", account?.username?.ifBlank { state.xtreamUsername } ?: state.xtreamUsername.ifBlank { "Non configure" })
                    SyncDialogInfoRow("Password", if (account?.password.isNullOrBlank()) "********" else "********")
                }
                SyncDialogInfoRow("Derniere synchro", state.account.lastSync ?: "Jamais")
            }
            if (phase == XtreamSyncDialogPhase.Success) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = syncStatus.successMessageOrDefault(),
                    color = SmartVisionColors.Success,
                    style = SmartVisionType.Label,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(SmartVisionColors.Success.copy(alpha = 0.14f))
                        .border(BorderStroke(1.dp, SmartVisionColors.Success.copy(alpha = 0.58f)), RoundedCornerShape(7.dp))
                        .padding(vertical = 10.dp),
                )
            }
            if (phase == XtreamSyncDialogPhase.Error) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = errorMessage ?: syncStatus.errorMessageOrDefault(),
                    color = SmartVisionColors.Error,
                    style = SmartVisionType.Label,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(SmartVisionColors.Error.copy(alpha = 0.14f))
                        .border(BorderStroke(1.dp, SmartVisionColors.Error.copy(alpha = 0.58f)), RoundedCornerShape(7.dp))
                        .padding(vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TvButton(
                    text = if (phase == XtreamSyncDialogPhase.Confirmation) "Annuler" else "Retour",
                    onClick = { if (!blocking) closeAllowed() },
                    enabled = !blocking,
                    leadingIcon = Icons.Default.ArrowBack,
                    focusRequester = returnFocusRequester,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .width(210.dp)
                        .height(44.dp),
                )
                TvButton(
                    text = if (blocking) "Synchronisation en cours" else "Lancer la synchronisation",
                    onClick = {
                        if (phase == XtreamSyncDialogPhase.Confirmation) {
                            phase = XtreamSyncDialogPhase.Running
                            errorMessage = null
                            scope.launch {
                                val result = onStartSync()
                                phase = if (result.isSuccess) {
                                    XtreamSyncDialogPhase.Success
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Synchronisation impossible"
                                    XtreamSyncDialogPhase.Error
                                }
                            }
                        }
                    },
                    enabled = phase == XtreamSyncDialogPhase.Confirmation && sourceConfigured,
                    leadingIcon = if (blocking) null else Icons.Default.CloudSync,
                    leadingContent = if (blocking) {
                        {
                            CircularProgressIndicator(
                                color = com.smartvision.svplayer.ui.theme.LocalLoadingColor.current,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
                    },
                    focusRequester = startFocusRequester,
                    modifier = Modifier
                        .width(286.dp)
                        .height(44.dp),
                )
            }
        }
    }
}

@Composable
internal fun XtreamSyncCountCard(
    title: String,
    progress: SyncStatus.SyncSectionProgress,
    showProgress: Boolean,
    enabled: Boolean = true,
    visualStyle: HomeVisualStyle? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(7.dp)
    val valueColor = when {
        !enabled -> SmartVisionColors.TextSecondary
        progress.completed -> Color(0xFF7CFFB2)
        else -> SmartVisionColors.TextPrimary
    }
    Box(
        modifier = modifier
            .height(126.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        SmartVisionColors.Primary.copy(alpha = 0.24f),
                        SmartVisionColors.Surface.copy(alpha = 0.70f),
                    ),
                ),
            )
            .border(
                BorderStroke(1.dp, if (enabled) SmartVisionColors.CyanAccent.copy(alpha = 0.38f) else SmartVisionColors.Border.copy(alpha = 0.44f)),
                shape,
            ),
    ) {
        visualStyle?.let { style ->
            HomeVisualBackground(style = style, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020712).copy(alpha = 0.62f)),
            )
        }
        if (showProgress && enabled && progress.percent < 100) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(1f - progress.fraction)
                    .align(Alignment.CenterEnd)
                    .background(Color.Black.copy(alpha = 0.38f)),
            )
        }
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
                Spacer(Modifier.weight(1f))
                if (showProgress && enabled) {
                    Text(
                        text = "${progress.percent}%",
                        color = if (progress.completed) Color(0xFF7CFFB2) else SmartVisionColors.CyanAccent,
                        style = SmartVisionType.Caption,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (enabled) progress.currentItems.toString() else "N/A",
                color = valueColor,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (showProgress && enabled) {
                Text(
                    text = buildString {
                        append(progress.message ?: progress.phase.profileSyncPhaseLabel())
                        progress.totalItems?.takeIf { it > 0 }?.let { append(" â€¢ ").append(progress.currentItems).append("/").append(it) }
                    },
                    color = if (progress.phase == SyncStatus.SyncSectionPhase.ERROR) SmartVisionColors.Error else SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progress.keptItems != null || progress.excludedItems != null) {
                    Text(
                        text = listOfNotNull(
                            progress.keptItems?.let { "$it conserves" },
                            progress.excludedItems?.let { "$it exclus" },
                        ).joinToString(" â€¢ "),
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (showProgress && enabled) {
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (progress.completed) Color(0xFF7CFFB2) else com.smartvision.svplayer.ui.theme.LocalLoadingColor.current,
                    trackColor = Color.White.copy(alpha = 0.10f),
                )
            }
        }
    }
}

private fun SyncStatus.SyncSectionPhase.profileSyncPhaseLabel(): String = when (this) {
    SyncStatus.SyncSectionPhase.WAITING -> "En attente"
    SyncStatus.SyncSectionPhase.RUNNING -> "Telechargement"
    SyncStatus.SyncSectionPhase.FILTERING -> "Filtrage Kids"
    SyncStatus.SyncSectionPhase.IMPORTING -> "Import local"
    SyncStatus.SyncSectionPhase.LOADING_TRENDS -> "Finalisation"
    SyncStatus.SyncSectionPhase.COMPLETED -> "Termine"
    SyncStatus.SyncSectionPhase.ERROR -> "Erreur"
}

@Composable
private fun SyncDialogInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label :", color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Text(
            text = value,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

internal fun SyncStatus.catalogProgressOrDefault(account: AccountProfile): SyncStatus.CatalogProgress =
    when (this) {
        is SyncStatus.Running -> catalogProgress
        is SyncStatus.Success -> catalogProgress
        is SyncStatus.Error -> catalogProgress
        SyncStatus.Idle -> SyncStatus.CatalogProgress(
            live = SyncStatus.SyncSectionProgress(currentItems = account.liveCount, previousItems = account.liveCount),
            movies = SyncStatus.SyncSectionProgress(currentItems = account.movieCount, previousItems = account.movieCount),
            series = SyncStatus.SyncSectionProgress(currentItems = account.seriesCount, previousItems = account.seriesCount),
        )
    }

private fun SyncStatus.successMessageOrDefault(): String =
    (this as? SyncStatus.Success)?.message ?: "Synchronisation terminee avec succes."

private fun SyncStatus.errorMessageOrDefault(): String =
    (this as? SyncStatus.Error)?.message ?: "Synchronisation impossible."
