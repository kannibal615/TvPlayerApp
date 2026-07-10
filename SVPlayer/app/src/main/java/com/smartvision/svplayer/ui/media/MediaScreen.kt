@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.smartvision.svplayer.ui.media

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import coil.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.domain.access.PremiumFeatureGateState
import com.smartvision.svplayer.media.MediaCenterFileType
import com.smartvision.svplayer.media.MediaCenterSource
import com.smartvision.svplayer.media.transfer.MediaTransferMode
import com.smartvision.svplayer.media.transfer.MediaTransferSession
import com.smartvision.svplayer.ui.catalog.CatalogPanelTitleStyle
import com.smartvision.svplayer.ui.catalog.MediaCatalogDimens
import com.smartvision.svplayer.ui.catalog.MediaCatalogHeader
import com.smartvision.svplayer.ui.catalog.MediaCatalogPanel
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
import com.smartvision.svplayer.ui.components.TvDialogSurface
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediaScreen(
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    strings: SmartVisionStrings,
    access: PremiumFeatureGateResult,
    transferAccess: PremiumFeatureGateResult,
    privateMediaAccess: PremiumFeatureGateResult,
    onPlayFile: (Long) -> Unit,
    onOpenPrivateMediaDetails: (String) -> Unit,
    onOpenPrivateMediaPlayer: (String) -> Unit,
    onLockedFeature: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: MediaViewModel = viewModel(
        factory = viewModelFactory {
            MediaViewModel(container.mediaRepository, container.privateMediaRepository, container.mediaTransferServer)
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTabFocusRequester = remember { FocusRequester() }
    val firstFocusRequester = remember { FocusRequester() }
    val lockedActionFocusRequester = remember { FocusRequester() }
    var renameTarget by remember { mutableStateOf<MediaFileUi?>(null) }
    var moveTarget by remember { mutableStateOf<MediaFileUi?>(null) }
    var deleteTarget by remember { mutableStateOf<MediaFileUi?>(null) }
    var restoreLibraryFocusRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(access.allowed, access.shouldShowUpgradePrompt) {
        withFrameNanos { }
        delay(160)
        when {
            access.allowed -> runCatching { firstFocusRequester.requestFocus() }
            access.shouldShowUpgradePrompt -> runCatching { lockedActionFocusRequester.requestFocus() }
            else -> runCatching { currentTabFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(restoreLibraryFocusRequest) {
        if (restoreLibraryFocusRequest > 0) {
            withFrameNanos { }
            delay(160)
            runCatching { firstFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(state.message, state.errorMessage) {
        if (state.message != null || state.errorMessage != null) {
            delay(3200)
            viewModel.clearTransientMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.36f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    center = Offset(920f, 120f),
                    radius = 1500f,
                ),
            )
            .padding(horizontal = MediaCatalogDimens.ScreenPadding)
            .padding(top = MediaCatalogDimens.TopPadding, bottom = MediaCatalogDimens.BottomPadding),
    ) {
        MediaCatalogHeader(
            currentRoute = currentRoute,
            tabs = tabs,
            onNavigate = onNavigate,
            onSync = onSync,
            onSettings = onSettings,
            onProfile = onProfile,
            onNotifications = onNotifications,
            onLicenseKey = onLicenseKey,
            showLicenseKey = showLicenseKey,
            hasNewNotifications = hasNewNotifications,
            notificationBadgeCount = notificationBadgeCount,
            currentTabFocusRequester = currentTabFocusRequester,
            contentDownFocusRequester = when {
                access.allowed -> firstFocusRequester
                access.shouldShowUpgradePrompt -> lockedActionFocusRequester
                else -> null
            },
            onContentDown = when {
                access.allowed -> ({ runCatching { firstFocusRequester.requestFocus() } })
                access.shouldShowUpgradePrompt -> ({ runCatching { lockedActionFocusRequester.requestFocus() } })
                else -> null
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(MediaCatalogDimens.HeaderGap))

        if (!access.allowed) {
            LockedMediaCenter(
                strings = strings,
                access = access,
                onLockedFeature = onLockedFeature,
                focusRequester = lockedActionFocusRequester,
                headerFocusRequester = currentTabFocusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            return@Column
        }

        MediaWorkspace(
            strings = strings,
            state = state,
            onAreaSelected = viewModel::selectArea,
            onToggleLocalGroup = viewModel::toggleLocalGroup,
            onPrivateMediaSelected = {
                if (privateMediaAccess.allowed) viewModel.selectPrivateMedia() else onLockedFeature()
            },
            onFileSelected = viewModel::selectFile,
            onPrivateItemSelected = { itemId ->
                val currentPlayback = state.privatePreviewPlayback
                if (state.selectedPrivateItemId == itemId && currentPlayback?.canPlay == true) {
                    onOpenPrivateMediaPlayer(itemId)
                } else {
                    viewModel.selectPrivateItem(itemId)
                }
            },
            onRefresh = viewModel::refreshStorage,
            onRefreshPrivate = viewModel::refreshPrivateMedia,
            onPrivateCategorySelected = viewModel::selectPrivateCategory,
            onTogglePrivateGroup = {
                if (privateMediaAccess.allowed) viewModel.togglePrivateGroup() else onLockedFeature()
            },
            onSearchQueryChanged = viewModel::updateSearchQuery,
            onPlayFile = onPlayFile,
            onOpenPrivateMediaDetails = onOpenPrivateMediaDetails,
            onOpenPrivateMediaPlayer = onOpenPrivateMediaPlayer,
            onRename = { renameTarget = state.selectedFile },
            onMove = { moveTarget = state.selectedFile },
            onDelete = { deleteTarget = state.selectedFile },
            transferAccess = transferAccess,
            privateMediaAccess = privateMediaAccess,
            onImportPhone = {
                if (transferAccess.allowed) viewModel.startPhoneImport() else onLockedFeature()
            },
            onExportPhone = {
                if (transferAccess.allowed) viewModel.startPhoneExport() else onLockedFeature()
            },
            firstFocusRequester = firstFocusRequester,
            headerFocusRequester = currentTabFocusRequester,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }

    renameTarget?.let { file ->
        MediaRenameDialog(
            file = file,
            strings = strings,
            onDismiss = { renameTarget = null },
            onRename = { requestedName ->
                renameTarget = null
                viewModel.renameFile(file.id, requestedName)
            },
        )
    }
    moveTarget?.let { file ->
        MediaMoveDialog(
            file = file,
            folders = state.folders,
            strings = strings,
            onDismiss = { moveTarget = null },
            onMove = { targetFolderId ->
                moveTarget = null
                viewModel.moveFile(file.id, targetFolderId)
            },
        )
    }
    deleteTarget?.let { file ->
        MediaDeleteDialog(
            file = file,
            strings = strings,
            onDismiss = { deleteTarget = null },
            onDelete = {
                deleteTarget = null
                viewModel.deleteFile(file.id)
                restoreLibraryFocusRequest += 1
            },
        )
    }
    state.transferSession?.let { session ->
        MediaTransferDialog(
            session = session,
            strings = strings,
            onDismiss = viewModel::dismissTransferSession,
        )
    }
}

@Composable
private fun LockedMediaCenter(
    strings: SmartVisionStrings,
    access: PremiumFeatureGateResult,
    onLockedFeature: () -> Unit,
    focusRequester: FocusRequester,
    headerFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val subtitle = if (access.state == PremiumFeatureGateState.BlockedExpired) {
        strings.premiumFeatureExpiredSubtitle
    } else {
        strings.premiumFeatureLockedSubtitle
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(680.dp)
                .clip(RoundedCornerShape(MediaCatalogDimens.PanelRadius))
                .background(Color(0xDD07101E))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(MediaCatalogDimens.PanelRadius))
                .padding(horizontal = 34.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Theaters,
                contentDescription = null,
                tint = SmartVisionColors.Warning,
                modifier = Modifier.size(46.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = strings.premiumFeatureLockedTitle,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Body,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            TvButton(
                text = strings.premiumPurchaseTitle,
                onClick = onLockedFeature,
                enabled = access.shouldShowUpgradePrompt,
                variant = TvButtonVariant.Primary,
                focusRequester = focusRequester,
                modifier = Modifier.focusProperties { up = headerFocusRequester },
            )
        }
    }
}

@Composable
private fun MediaWorkspace(
    strings: SmartVisionStrings,
    state: MediaScreenState,
    onAreaSelected: (MediaArea) -> Unit,
    onToggleLocalGroup: () -> Unit,
    onTogglePrivateGroup: () -> Unit,
    onPrivateMediaSelected: () -> Unit,
    onPrivateCategorySelected: (String) -> Unit,
    onFileSelected: (Long) -> Unit,
    onPrivateItemSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onRefreshPrivate: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onPlayFile: (Long) -> Unit,
    onOpenPrivateMediaDetails: (String) -> Unit,
    onOpenPrivateMediaPlayer: (String) -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    transferAccess: PremiumFeatureGateResult,
    privateMediaAccess: PremiumFeatureGateResult,
    onImportPhone: () -> Unit,
    onExportPhone: () -> Unit,
    firstFocusRequester: FocusRequester,
    headerFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val firstContentFocusRequester = remember { FocusRequester() }
    val previewActionFocusRequester = remember { FocusRequester() }
    val previewPlayerFocusRequester = remember { FocusRequester() }
    var contentFocusSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(contentFocusSignal, state.selectedSource, state.selectedArea, state.visibleFiles.size, state.folders.size, state.privateItems.size) {
        if (contentFocusSignal > 0) {
            delay(120)
            runCatching { firstContentFocusRequester.requestFocus() }
        }
    }

    fun focusPreviewAction(): Boolean {
        if (state.selectedSource == MediaSource.Local && state.selectedFile == null) return false
        if (state.selectedSource == MediaSource.Private && state.selectedPrivateItem == null) return false
        if (state.selectedSource == MediaSource.Private && !state.privatePreviewPlayback?.embedUrl.isNullOrBlank()) {
            runCatching { previewPlayerFocusRequester.requestFocus() }
        } else {
            runCatching { previewActionFocusRequester.requestFocus() }
        }
        return true
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MediaCatalogDimens.PanelGap),
    ) {
        val privateMode = state.selectedSource == MediaSource.Private
        MediaLibraryPanel(
            strings = strings,
            state = state,
            onAreaSelected = { area ->
                onAreaSelected(area)
                contentFocusSignal += 1
            },
            onToggleLocalGroup = onToggleLocalGroup,
            onTogglePrivateGroup = {
                onTogglePrivateGroup()
                contentFocusSignal += 1
            },
            onPrivateMediaSelected = {
                onPrivateMediaSelected()
                contentFocusSignal += 1
            },
            onPrivateCategorySelected = { categoryId ->
                onPrivateCategorySelected(categoryId)
                contentFocusSignal += 1
            },
            transferAccess = transferAccess,
            privateMediaAccess = privateMediaAccess,
            onImportPhone = onImportPhone,
            onExportPhone = onExportPhone,
            firstFocusRequester = firstFocusRequester,
            headerFocusRequester = headerFocusRequester,
            modifier = Modifier
                .weight(if (privateMode) 0.20f else 0.24f)
                .fillMaxHeight(),
        )

        MediaContentPanel(
            strings = strings,
            state = state,
            onFileSelected = onFileSelected,
            onPrivateItemSelected = onPrivateItemSelected,
            onRefresh = onRefresh,
            onRefreshPrivate = onRefreshPrivate,
            onSearchQueryChanged = onSearchQueryChanged,
            firstContentFocusRequester = firstContentFocusRequester,
            onMoveRightToPreview = { focusPreviewAction() },
            modifier = Modifier
                .weight(if (privateMode) 0.30f else 0.42f)
                .fillMaxHeight(),
        )

        MediaPreviewPanel(
            strings = strings,
            state = state,
            onPlay = { state.selectedFile?.takeIf { it.isPlayable }?.id?.let(onPlayFile) },
            onOpenPrivateMediaDetails = { state.selectedPrivateItem?.id?.let(onOpenPrivateMediaDetails) },
            onOpenPrivateMediaPlayer = onOpenPrivateMediaPlayer,
            onRename = onRename,
            onMove = onMove,
            onDelete = onDelete,
            transferAccess = transferAccess,
            onExportPhone = onExportPhone,
            previewActionFocusRequester = previewActionFocusRequester,
            previewPlayerFocusRequester = previewPlayerFocusRequester,
            modifier = Modifier
                .weight(if (privateMode) 0.50f else 0.34f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun MediaLibraryPanel(
    strings: SmartVisionStrings,
    state: MediaScreenState,
    onAreaSelected: (MediaArea) -> Unit,
    onToggleLocalGroup: () -> Unit,
    onTogglePrivateGroup: () -> Unit,
    onPrivateMediaSelected: () -> Unit,
    onPrivateCategorySelected: (String) -> Unit,
    transferAccess: PremiumFeatureGateResult,
    privateMediaAccess: PremiumFeatureGateResult,
    onImportPhone: () -> Unit,
    onExportPhone: () -> Unit,
    firstFocusRequester: FocusRequester,
    headerFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = strings.mediaLibrary,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MediaAreaButton(
                label = strings.mediaLocal,
                count = state.files.size,
                icon = if (state.localExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                onClick = onToggleLocalGroup,
                selected = state.selectedSource == MediaSource.Local,
                focusRequester = firstFocusRequester,
                upFocusRequester = headerFocusRequester,
            )
            if (state.localExpanded) {
                listOf(MediaArea.AllFiles, MediaArea.Recordings, MediaArea.Imports, MediaArea.Transfers).forEach { area ->
                    MediaAreaButton(
                        label = area.label(strings),
                        count = state.countFor(area),
                        icon = area.icon(),
                        onClick = { onAreaSelected(area) },
                        selected = state.selectedSource == MediaSource.Local && state.selectedArea == area,
                        focusRequester = null,
                        indent = 16.dp,
                        height = 34.dp,
                    )
                }
                if (transferAccess.showDisabledControl) {
                    MediaAreaButton(
                        label = strings.mediaImportPhone,
                        count = 0,
                        countText = "QR",
                        icon = Icons.Default.QrCode2,
                        onClick = onImportPhone,
                        selected = false,
                        focusRequester = null,
                        indent = 16.dp,
                        height = 34.dp,
                        enabled = !state.transferInProgress,
                        locked = transferAccess.locked,
                    )
                    MediaAreaButton(
                        label = strings.mediaExportPhone,
                        count = 0,
                        countText = "QR",
                        icon = Icons.Default.QrCode2,
                        onClick = onExportPhone,
                        selected = false,
                        focusRequester = null,
                        indent = 16.dp,
                        height = 34.dp,
                        enabled = !state.transferInProgress,
                        locked = transferAccess.locked,
                    )
                }
            }
            MediaAreaButton(
                label = strings.mediaPrivate,
                count = state.privateItems.size,
                icon = if (state.privateExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                onClick = onTogglePrivateGroup,
                selected = state.selectedSource == MediaSource.Private,
                focusRequester = null,
                enabled = privateMediaAccess.showDisabledControl,
                locked = privateMediaAccess.locked,
            )
            if (state.privateExpanded && privateMediaAccess.showDisabledControl && !privateMediaAccess.locked) {
                state.privateCategories.forEach { category ->
                    MediaAreaButton(
                        label = category.title,
                        count = if (category.id == state.selectedPrivateCategoryId) state.privateItems.size else 0,
                        icon = Icons.Default.Theaters,
                        onClick = { onPrivateCategorySelected(category.id) },
                        selected = state.selectedSource == MediaSource.Private && state.selectedPrivateCategoryId == category.id,
                        focusRequester = null,
                        indent = 16.dp,
                        height = 34.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaLibraryHero(
    strings: SmartVisionStrings,
    state: MediaScreenState,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.radialGradient(
                    listOf(
                        SmartVisionColors.CyanAccent.copy(alpha = 0.24f),
                        SmartVisionColors.Primary.copy(alpha = 0.22f),
                        Color(0xAA07101E),
                    ),
                    center = Offset(220f, 20f),
                    radius = 360f,
                ),
            )
            .border(BorderStroke(1.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.32f)), shape)
            .padding(9.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = SmartVisionColors.CyanAccent,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.mediaPremiumStudio,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.Label,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${state.files.size} ${strings.mediaTotalFiles.lowercase()}",
                        color = SmartVisionColors.CyanAccent,
                        style = SmartVisionType.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = strings.mediaLocalVaultSubtitle,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MediaAreaButton(
    label: String,
    count: Int,
    countText: String? = null,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    upFocusRequester: FocusRequester? = null,
    indent: androidx.compose.ui.unit.Dp = 0.dp,
    height: androidx.compose.ui.unit.Dp = 40.dp,
    enabled: Boolean = true,
    locked: Boolean = false,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.018f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = 10.dp,
            )
            .clip(shape)
            .background(
                if (active) {
                    Brush.horizontalGradient(
                        listOf(
                            SmartVisionColors.Primary.copy(alpha = 0.72f),
                            SmartVisionColors.PrimaryDark.copy(alpha = 0.58f),
                        ),
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            SmartVisionColors.SurfaceElevated.copy(alpha = 0.72f),
                            Color.White.copy(alpha = 0.035f),
                        ),
                    )
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
                    if (active) focusStyle.accent.copy(alpha = 0.88f) else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(start = 9.dp + indent, end = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = SmartVisionType.Label,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        MediaCountPill(
            text = if (locked) "LOCK" else countText ?: count.toString(),
            active = active,
        )
    }
}

@Composable
private fun MediaCountPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f))
            .border(
                BorderStroke(1.dp, if (active) Color.White.copy(alpha = 0.30f) else SmartVisionColors.Border),
                RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MediaStorageCard(
    strings: SmartVisionStrings,
    state: MediaScreenState,
) {
    val shape = RoundedCornerShape(12.dp)
    val storage = state.storageInfo
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.055f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border), shape)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = SmartVisionColors.Success,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = strings.mediaSmartStorage,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = storage?.let { formatBytes(it.availableBytes) } ?: strings.mediaStorageReady,
            color = SmartVisionColors.CyanAccent,
            style = SmartVisionType.Caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = storage?.rootPath ?: strings.mediaMvpNotice,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PhoneImportPanel(
    strings: SmartVisionStrings,
    state: MediaScreenState,
    transferAccess: PremiumFeatureGateResult,
    onImportPhone: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val enabled = !state.transferInProgress
    val focused = focusState.isFocused
    val shape = RoundedCornerShape(10.dp)
    val subtitle = when {
        state.transferInProgress -> strings.mediaTransferInProgress
        transferAccess.allowed -> strings.mediaPhoneTransferReady
        transferAccess.state == PremiumFeatureGateState.BlockedExpired -> strings.mediaPhoneTransferExpired
        else -> strings.mediaPhoneTransferLocked
    }
    val buttonText = if (transferAccess.allowed) {
        strings.mediaPhoneTransferReceive
    } else {
        strings.mediaPhoneTransferUpgrade
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.018f,
                glowColor = if (transferAccess.allowed) SmartVisionColors.CyanAccent else SmartVisionColors.Warning,
                cornerRadius = 10.dp,
            )
            .clip(shape)
            .background(
                Brush.radialGradient(
                    if (transferAccess.allowed) {
                        listOf(
                            SmartVisionColors.CyanAccent.copy(alpha = 0.20f),
                            SmartVisionColors.PrimaryDark.copy(alpha = 0.26f),
                            Color.White.copy(alpha = 0.05f),
                        )
                    } else {
                        listOf(
                            SmartVisionColors.Warning.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                        )
                    },
                    center = Offset(245f, 0f),
                    radius = 320f,
                ),
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (transferAccess.allowed) SmartVisionColors.CyanAccent.copy(alpha = 0.34f) else SmartVisionColors.Warning.copy(alpha = 0.30f),
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onImportPhone,
            )
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = if (focused) 0.18f else 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = if (transferAccess.allowed) SmartVisionColors.CyanAccent else SmartVisionColors.Warning,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.mediaTransferHub,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = strings.mediaPhoneTransferTitle,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        MediaCountPill(
            text = buttonText,
            active = focused || transferAccess.allowed,
        )
    }
}

@Composable
private fun PhoneExportPanel(
    strings: SmartVisionStrings,
    state: MediaScreenState,
    transferAccess: PremiumFeatureGateResult,
    onExportPhone: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val selected = state.selectedFile
    val enabled = selected != null && !state.transferInProgress
    val focused = focusState.isFocused
    val shape = RoundedCornerShape(10.dp)
    val subtitle = when {
        state.transferInProgress -> strings.mediaTransferInProgress
        selected == null -> strings.mediaNoSelection
        transferAccess.allowed -> selected.displayName
        transferAccess.state == PremiumFeatureGateState.BlockedExpired -> strings.mediaPhoneTransferExpired
        else -> strings.mediaPhoneTransferLocked
    }
    val actionLabel = if (transferAccess.allowed) strings.mediaExportPhone else strings.mediaPhoneTransferUpgrade

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.018f,
                glowColor = if (transferAccess.allowed) SmartVisionColors.Success else SmartVisionColors.Warning,
                cornerRadius = 10.dp,
            )
            .clip(shape)
            .background(
                Brush.radialGradient(
                    if (transferAccess.allowed) {
                        listOf(
                            SmartVisionColors.Success.copy(alpha = 0.18f),
                            SmartVisionColors.PrimaryDark.copy(alpha = 0.24f),
                            Color.White.copy(alpha = 0.045f),
                        )
                    } else {
                        listOf(
                            SmartVisionColors.Warning.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                        )
                    },
                    center = Offset(245f, 0f),
                    radius = 320f,
                ),
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (transferAccess.allowed) SmartVisionColors.Success.copy(alpha = 0.36f) else SmartVisionColors.Warning.copy(alpha = 0.30f),
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onExportPhone,
            )
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = if (focused) 0.18f else 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = if (transferAccess.allowed) SmartVisionColors.Success else SmartVisionColors.Warning,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.mediaTransferHub,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "TV -> Phone",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        MediaCountPill(
            text = actionLabel,
            active = focused || transferAccess.allowed,
        )
    }
}

@Composable
private fun MediaLibraryInfoCard(
    text: String,
    emphasized: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (emphasized) {
                    SmartVisionColors.PrimaryDark.copy(alpha = 0.30f)
                } else {
                    Color.White.copy(alpha = 0.05f)
                },
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (emphasized) SmartVisionColors.CyanAccent.copy(alpha = 0.38f) else SmartVisionColors.Border,
                ),
                shape,
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(
            text = text,
            color = if (emphasized) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MediaContentPanel(
    strings: SmartVisionStrings,
    state: MediaScreenState,
    onFileSelected: (Long) -> Unit,
    onPrivateItemSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onRefreshPrivate: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    firstContentFocusRequester: FocusRequester,
    onMoveRightToPreview: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = if (state.selectedSource == MediaSource.Private) strings.mediaPrivate else strings.mediaList,
        modifier = modifier,
        titleContent = {
            Column {
                Text(
                    text = if (state.selectedSource == MediaSource.Private) strings.mediaPrivate else strings.mediaList,
                    color = SmartVisionColors.TextPrimary,
                    style = CatalogPanelTitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (state.selectedSource == MediaSource.Private) {
                        state.privateCategories.firstOrNull { it.id == state.selectedPrivateCategoryId }?.title ?: strings.mediaPrivate
                    } else {
                        state.selectedArea.label(strings)
                    },
                    color = SmartVisionColors.CyanAccent,
                    style = SmartVisionType.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailing = {
            MediaSearchField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = strings.mediaSearchPlaceholder,
                modifier = Modifier
                    .width(210.dp)
                    .height(38.dp),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.errorMessage?.let { message ->
                MediaStatusMessage(message = message, error = true)
            }
            state.message?.let { message ->
                MediaStatusMessage(message = message.label(strings), error = false)
            }
            if (state.operationInProgress) {
                MediaStatusMessage(message = strings.mediaOperationInProgress, error = false)
            }
            if (state.transferInProgress) {
                MediaStatusMessage(message = strings.mediaTransferInProgress, error = false)
            }
            state.privateErrorMessage?.takeIf { state.selectedSource == MediaSource.Private }?.let { message ->
                MediaStatusMessage(message = message, error = true)
            }

            if (state.selectedSource == MediaSource.Private) {
                when {
                    state.privateLoading -> MediaPrivateLoading(strings = strings)
                    !state.privateProviderEnabled -> MediaEmptyState(
                        title = strings.mediaPrivateDisabled,
                        subtitle = strings.mediaPrivateDisabledSubtitle,
                    )
                    state.privateItems.isEmpty() -> MediaEmptyState(
                        title = strings.mediaPrivateEmpty,
                        subtitle = strings.mediaPrivateEmptySubtitle,
                    )
                    else -> PrivateMediaList(
                        strings = strings,
                        items = state.privateItems,
                        selectedItemId = state.selectedPrivateItemId,
                        onItemSelected = onPrivateItemSelected,
                        firstFocusRequester = firstContentFocusRequester,
                        onMoveRightToPreview = onMoveRightToPreview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            } else if (state.selectedArea == MediaArea.Folders) {
                MediaFolderList(
                    strings = strings,
                    folders = state.folders,
                    firstFocusRequester = firstContentFocusRequester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                MediaFileList(
                    strings = strings,
                    files = state.visibleFiles,
                    selectedFileId = state.selectedFileId,
                    onFileSelected = onFileSelected,
                    firstFocusRequester = firstContentFocusRequester,
                    onMoveRightToPreview = onMoveRightToPreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MediaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentView = LocalView.current
    var editing by remember { mutableStateOf(false) }
    val active = focusState.isFocused || value.isNotBlank()
    val shape = RoundedCornerShape(10.dp)
    fun focusAndShowKeyboard() {
        editing = true
        runCatching { focusRequester.requestFocus() }
        keyboardController?.show()
        currentView.post {
            val inputMethodManager = currentView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.showSoftInput(currentView.findFocus() ?: currentView, InputMethodManager.SHOW_IMPLICIT)
        }
        currentView.postDelayed({
            keyboardController?.show()
            val inputMethodManager = currentView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.showSoftInput(currentView.findFocus() ?: currentView, InputMethodManager.SHOW_IMPLICIT)
        }, 120L)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        readOnly = false,
        textStyle = SmartVisionType.Label.copy(color = SmartVisionColors.TextPrimary),
        cursorBrush = SolidColor(focusStyle.accent),
        modifier = modifier
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                focusedScale = 1.01f,
                cornerRadius = 10.dp,
            )
            .onFocusChanged {
                focusState.isFocused = it.isFocused || it.hasFocus
                if (!it.isFocused && !it.hasFocus) {
                    editing = false
                    keyboardController?.hide()
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        focusAndShowKeyboard()
                        true
                    }
                    Key.Back -> {
                        if (editing) {
                            editing = false
                            keyboardController?.hide()
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown -> {
                        if (editing) {
                            editing = false
                            keyboardController?.hide()
                        }
                        false
                    }
                    else -> false
                }
            }
            .clip(shape)
            .background(if (active) focusStyle.background else Color.White.copy(alpha = 0.055f))
            .border(
                BorderStroke(1.dp, if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { focusAndShowKeyboard() },
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (active) focusStyle.accent else SmartVisionColors.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = SmartVisionColors.TextSecondary.copy(alpha = 0.72f),
                            style = SmartVisionType.Caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun MediaStatsStrip(
    strings: SmartVisionStrings,
    state: MediaScreenState,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaStatTile(
            label = strings.mediaTotalFiles,
            value = state.files.size.toString(),
            icon = Icons.Default.Menu,
            color = SmartVisionColors.CyanAccent,
            modifier = Modifier.weight(1f),
        )
        MediaStatTile(
            label = strings.mediaRecordings,
            value = state.countFor(MediaArea.Recordings).toString(),
            icon = Icons.Default.Theaters,
            color = SmartVisionColors.Warning,
            modifier = Modifier.weight(1f),
        )
        MediaStatTile(
            label = strings.mediaTransferHub,
            value = state.countFor(MediaArea.Transfers).toString(),
            icon = Icons.Default.Devices,
            color = SmartVisionColors.Success,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MediaStatTile(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        color.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.045f),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, color.copy(alpha = 0.30f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = label,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MediaFileList(
    strings: SmartVisionStrings,
    files: List<MediaFileUi>,
    selectedFileId: Long?,
    onFileSelected: (Long) -> Unit,
    firstFocusRequester: FocusRequester,
    onMoveRightToPreview: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    if (files.isEmpty()) {
        MediaEmptyState(
            title = strings.mediaEmptyTitle,
            subtitle = strings.mediaNoFilesInArea,
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(files, key = { it.id }) { file ->
            val index = files.indexOf(file)
            MediaFileRow(
                strings = strings,
                file = file,
                selected = file.id == selectedFileId,
                onClick = { onFileSelected(file.id) },
                focusRequester = if (index == 0) firstFocusRequester else null,
                onMoveRightToPreview = onMoveRightToPreview,
            )
        }
    }
}

@Composable
private fun MediaFolderList(
    strings: SmartVisionStrings,
    folders: List<MediaFolderUi>,
    firstFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    if (folders.isEmpty()) {
        MediaEmptyState(
            title = strings.mediaFolders,
            subtitle = strings.mediaNoFilesInArea,
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(folders, key = { it.id }) { folder ->
            val index = folders.indexOf(folder)
            MediaFolderRow(
                strings = strings,
                folder = folder,
                focusRequester = if (index == 0) firstFocusRequester else null,
            )
        }
    }
}

@Composable
private fun PrivateMediaList(
    strings: SmartVisionStrings,
    items: List<PrivateMediaItemUi>,
    selectedItemId: String?,
    onItemSelected: (String) -> Unit,
    firstFocusRequester: FocusRequester,
    onMoveRightToPreview: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            val index = items.indexOf(item)
            PrivateMediaRow(
                strings = strings,
                item = item,
                selected = item.id == selectedItemId,
                onClick = { onItemSelected(item.id) },
                focusRequester = if (index == 0) firstFocusRequester else null,
                onMoveRightToPreview = onMoveRightToPreview,
            )
        }
    }
}

@Composable
private fun MediaPrivateLoading(strings: SmartVisionStrings) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = SmartVisionColors.CyanAccent, modifier = Modifier.size(34.dp))
        Text(
            text = strings.mediaPrivateLoading,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PrivateMediaRow(
    strings: SmartVisionStrings,
    item: PrivateMediaItemUi,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    onMoveRightToPreview: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.018f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    onMoveRightToPreview()
                } else {
                    false
                }
            }
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (active) {
                        listOf(focusStyle.background, Color(0xAA07101E))
                    } else {
                        listOf(SmartVisionColors.SurfaceElevated.copy(alpha = 0.76f), Color(0x990A1323))
                    },
                ),
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
                    if (active) focusStyle.accent else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(88.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.38f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Default.Theaters, contentDescription = null, tint = SmartVisionColors.TextSecondary)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(item.durationLabel, item.viewsLabel, item.ratingLabel.takeIf { it.isNotBlank() }?.let { "${strings.mediaPrivateRating} $it" })
                    .filterNotNull()
                    .filter { it.isNotBlank() }
                    .joinToString("  |  "),
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.tags.joinToString("  "),
                color = SmartVisionColors.CyanAccent,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MediaFileRow(
    strings: SmartVisionStrings,
    file: MediaFileUi,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    onMoveRightToPreview: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.018f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    onMoveRightToPreview()
                } else {
                    false
                }
            }
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (active) {
                        listOf(
                            focusStyle.background,
                            SmartVisionColors.PrimaryDark.copy(alpha = 0.56f),
                            Color(0xAA07101E),
                        )
                    } else {
                        listOf(
                            SmartVisionColors.SurfaceElevated.copy(alpha = 0.76f),
                            Color(0x990A1323),
                        )
                    },
                ),
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
                    if (active) focusStyle.accent else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(mediaTypeColor(file.mediaType).copy(alpha = if (active) 0.22f else 0.12f))
                .border(
                    BorderStroke(1.dp, mediaTypeColor(file.mediaType).copy(alpha = if (active) 0.70f else 0.30f)),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = file.mediaIcon(),
                contentDescription = null,
                tint = mediaTypeColor(file.mediaType),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.displayName,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaTinyPill(text = file.source.label(strings), active = active)
                MediaTinyPill(text = file.sizeLabel, active = false)
                Text(
                    text = file.updatedLabel,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        MediaTinyPill(
            text = file.mediaType.label(strings),
            active = active,
            accent = mediaTypeColor(file.mediaType),
        )
    }
}

@Composable
private fun MediaTinyPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = SmartVisionColors.CyanAccent,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.055f))
            .border(
                BorderStroke(1.dp, if (active) accent.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.08f)),
                RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MediaFolderRow(
    strings: SmartVisionStrings,
    folder: MediaFolderUi,
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.018f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .clip(shape)
            .background(
                if (focusState.isFocused) {
                    focusStyle.background
                } else {
                    SmartVisionColors.SurfaceElevated.copy(alpha = 0.68f)
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = {})
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Theaters,
            contentDescription = null,
            tint = SmartVisionColors.CyanAccent,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = folder.relativePath,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = String.format(strings.mediaFilesFound, folder.fileCount),
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            maxLines = 1,
        )
    }
}

@Composable
private fun MediaEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MediaCatalogDimens.ItemRadius))
            .background(Color(0x660A1323))
            .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(MediaCatalogDimens.ItemRadius))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = SmartVisionColors.TextSecondary,
                modifier = Modifier.size(50.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Label,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MediaPreviewHero(
    strings: SmartVisionStrings,
    selected: MediaFileUi?,
    fallbackTitle: String,
    storageRootPath: String?,
) {
    val accent = selected?.let { mediaTypeColor(it.mediaType) } ?: SmartVisionColors.Primary
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(146.dp)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    listOf(
                        accent.copy(alpha = 0.34f),
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.30f),
                        Color(0xDD07101E),
                        Color.Black.copy(alpha = 0.72f),
                    ),
                    center = Offset(270f, 22f),
                    radius = 560f,
                ),
            )
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.45f)), shape)
            .padding(11.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (selected?.mediaType == MediaCenterFileType.Video && storageRootPath != null) {
                    MediaLocalMiniPlayer(
                        file = selected,
                        storageRootPath = storageRootPath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.09f))
                            .border(BorderStroke(1.dp, accent.copy(alpha = 0.38f)), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = selected?.mediaIcon() ?: Icons.Default.Theaters,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = selected?.displayName ?: fallbackTitle,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.Label,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaLocalMiniPlayer(
    file: MediaFileUi,
    storageRootPath: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val audioScope = rememberCoroutineScope()
    var buffering by remember(file.id) { mutableStateOf(true) }
    var errorText by remember(file.id) { mutableStateOf<String?>(null) }
    val volumeFadeJob = remember { arrayOfNulls<Job>(1) }
    val localFile = remember(file.id, file.relativePath, storageRootPath) {
        File(storageRootPath, file.relativePath).canonicalFile
    }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    fun restartPreviewAudioFade() {
        volumeFadeJob[0]?.cancel()
        player.volume = 0f
        volumeFadeJob[0] = audioScope.launch {
            delay(700L)
            repeat(8) { step ->
                delay(90L)
                player.volume = (step + 1).toFloat() / 8f
            }
        }
    }

    LaunchedEffect(localFile.absolutePath) {
        buffering = true
        errorText = null
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(localFile)))
        player.prepare()
        player.playWhenReady = true
        restartPreviewAudioFade()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) errorText = null
            }

            override fun onPlayerError(error: PlaybackException) {
                buffering = false
                errorText = error.message ?: "Preview unavailable"
                volumeFadeJob[0]?.cancel()
                player.volume = 0f
            }
        }
        player.addListener(listener)
        onDispose {
            volumeFadeJob[0]?.cancel()
            player.removeListener(listener)
            player.release()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black)
            .border(BorderStroke(1.dp, mediaTypeColor(file.mediaType).copy(alpha = 0.45f)), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black),
        )
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    this.player = player
                }
            },
            update = {
                it.setBackgroundColor(android.graphics.Color.BLACK)
                it.player = player
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            },
            modifier = Modifier.matchParentSize(),
        )
        if (buffering) {
            CircularProgressIndicator(
                color = SmartVisionColors.CyanAccent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(26.dp),
            )
        }
        errorText?.let { message ->
            Text(
                text = message,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun MediaPreviewPanel(
    strings: SmartVisionStrings,
    state: MediaScreenState,
    onPlay: () -> Unit,
    onOpenPrivateMediaDetails: () -> Unit,
    onOpenPrivateMediaPlayer: (String) -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    transferAccess: PremiumFeatureGateResult,
    onExportPhone: () -> Unit,
    previewActionFocusRequester: FocusRequester,
    previewPlayerFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    if (state.selectedSource == MediaSource.Private) {
        PrivateMediaPreviewPanel(
            strings = strings,
            item = state.selectedPrivateItem,
            playback = state.privatePreviewPlayback,
            loadingPlayback = state.privatePreviewLoading,
            playbackError = state.privatePreviewError,
            onOpenDetails = onOpenPrivateMediaDetails,
            onOpenPlayer = onOpenPrivateMediaPlayer,
            previewActionFocusRequester = previewActionFocusRequester,
            previewPlayerFocusRequester = previewPlayerFocusRequester,
            modifier = modifier,
        )
        return
    }

    val selected = state.selectedFile
    val playEnabled = selected?.isPlayable == true && !state.operationInProgress
    val editEnabled = selected != null && !state.operationInProgress
    val transferEnabled = selected != null && !state.operationInProgress && !state.transferInProgress
    val firstPreviewAction = when {
        playEnabled -> MediaPreviewAction.Play
        editEnabled -> MediaPreviewAction.Rename
        transferAccess.showDisabledControl && transferEnabled -> MediaPreviewAction.Export
        else -> null
    }
    MediaCatalogPanel(
        title = strings.mediaPreview,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            MediaPreviewHero(
                strings = strings,
                selected = selected,
                fallbackTitle = state.selectedArea.label(strings),
                storageRootPath = state.storageInfo?.rootPath,
            )

            if (selected == null) {
                Text(
                    text = strings.mediaNoSelection,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Label,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TvButton(
                        text = strings.mediaPlay,
                        onClick = onPlay,
                        enabled = playEnabled,
                        leadingIcon = Icons.Default.PlayArrow,
                        focusRequester = if (firstPreviewAction == MediaPreviewAction.Play) previewActionFocusRequester else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    )
                    TvButton(
                        text = strings.mediaRename,
                        onClick = onRename,
                        enabled = editEnabled,
                        leadingIcon = Icons.Default.Edit,
                        focusRequester = if (firstPreviewAction == MediaPreviewAction.Rename) previewActionFocusRequester else null,
                        variant = TvButtonVariant.Secondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    )
                    TvButton(
                        text = strings.delete,
                        onClick = onDelete,
                        enabled = editEnabled,
                        leadingIcon = Icons.Default.Delete,
                        variant = TvButtonVariant.Danger,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.055f))
                        .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = strings.mediaDetails,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.Label,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MediaInfoRow(strings.mediaFileSize, selected.sizeLabel)
                    MediaInfoRow(strings.mediaUpdatedAt, selected.updatedLabel)
                    MediaInfoRow(strings.mediaFileType, selected.mediaType.label(strings))
                }
            }

            state.storageInfo?.let { storage ->
                MediaInfoRow(strings.mediaAvailableSpace, formatBytes(storage.availableBytes))
            }

            Spacer(Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TvButton(
                    text = strings.mediaMove,
                    onClick = onMove,
                    enabled = editEnabled,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                )
                if (transferAccess.showDisabledControl) {
                    TvButton(
                        text = strings.mediaExportPhone,
                        onClick = onExportPhone,
                        enabled = transferEnabled,
                        focusRequester = if (firstPreviewAction == MediaPreviewAction.Export) previewActionFocusRequester else null,
                        variant = TvButtonVariant.Secondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivateMediaPreviewPanel(
    strings: SmartVisionStrings,
    item: PrivateMediaItemUi?,
    playback: PrivateMediaPlaybackUi?,
    loadingPlayback: Boolean,
    playbackError: String?,
    onOpenDetails: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    previewActionFocusRequester: FocusRequester,
    previewPlayerFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val canOpenPlayer = item != null
    val previewFocusState = rememberTvFocusState()
    val previewInteractionSource = remember { MutableInteractionSource() }
    val previewPressed by previewInteractionSource.collectIsPressedAsState()
    val previewFocusStyle = LocalTvFocusStyle.current

    MediaCatalogPanel(
        title = "",
        modifier = modifier,
        titleContent = {
            Text(
                text = item?.title ?: strings.mediaPrivate,
                color = SmartVisionColors.TextPrimary,
                style = CatalogPanelTitleStyle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.95f)
                    .aspectRatio(16f / 9f)
                    .tvFocusTarget(
                        state = previewFocusState,
                        enabled = item != null && canOpenPlayer,
                        pressed = previewPressed,
                        focusedScale = 1.02f,
                        glowColor = SmartVisionColors.Primary,
                        cornerRadius = 12.dp,
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(
                        BorderStroke(
                            if (previewFocusState.isFocused) previewFocusStyle.borderWidth else 1.dp,
                            if (previewFocusState.isFocused) previewFocusStyle.accent else SmartVisionColors.Border,
                        ),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable(
                        enabled = item != null && canOpenPlayer,
                        interactionSource = previewInteractionSource,
                        indication = null,
                        onClick = { item?.id?.let(onOpenPlayer) },
                    )
                    .focusable(enabled = item != null && canOpenPlayer, interactionSource = previewInteractionSource),
                contentAlignment = Alignment.Center,
            ) {
                if (playback?.canPlay == true) {
                    PrivateMediaMiniPlayer(
                        playback = playback,
                        embedFocusRequester = previewPlayerFocusRequester,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (!item?.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Theaters,
                        contentDescription = null,
                        tint = SmartVisionColors.TextSecondary,
                        modifier = Modifier.size(54.dp),
                    )
                }
                if (loadingPlayback) {
                    CircularProgressIndicator(
                        color = SmartVisionColors.CyanAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            if (item == null) {
                Text(
                    text = strings.mediaPrivateNoSelection,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Label,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                return@Column
            }
            Text(
                text = item.title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MediaInfoRow(strings.mediaDuration, item.durationLabel.ifBlank { "-" })
            MediaInfoRow(strings.mediaPrivateViews, item.viewsLabel.ifBlank { "-" })
            MediaInfoRow(strings.mediaPrivateRating, item.ratingLabel.ifBlank { "-" })
            if (item.tags.isNotEmpty()) {
                Text(
                    text = item.tags.joinToString("  "),
                    color = SmartVisionColors.CyanAccent,
                    style = SmartVisionType.Caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TvButton(
                    text = strings.mediaPrivateFullscreen,
                    onClick = { onOpenPlayer(item.id) },
                    enabled = canOpenPlayer,
                    leadingIcon = Icons.Default.PlayArrow,
                    focusRequester = if (canOpenPlayer) previewActionFocusRequester else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                )
                TvButton(
                    text = strings.mediaOpenDetails,
                    onClick = onOpenDetails,
                    leadingIcon = Icons.Default.Search,
                    focusRequester = if (!canOpenPlayer) previewActionFocusRequester else null,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                )
            }
            val playbackStatus = when {
                loadingPlayback -> strings.mediaPrivatePreviewStarting
                playbackError != null -> playbackError
                playback?.canPlay != true && !item.isPlayable -> strings.mediaPlaybackUnavailable
                else -> null
            }
            playbackStatus?.let { status ->
                Text(
                    text = status,
                    color = if (playback?.canPlay == true || item.isPlayable) SmartVisionColors.Success else SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PrivateMediaMiniPlayer(
    playback: PrivateMediaPlaybackUi,
    embedFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    when {
        !playback.streamUrl.isNullOrBlank() -> PrivateMediaExoPreview(playback.streamUrl, modifier)
        !playback.embedUrl.isNullOrBlank() -> PrivateMediaWebPreview(
            embedUrl = playback.embedUrl,
            focusRequester = embedFocusRequester,
            modifier = modifier,
        )
        else -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = playback.error ?: "",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PrivateMediaExoPreview(
    streamUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var buffering by remember(streamUrl) { mutableStateOf(true) }
    var errorText by remember(streamUrl) { mutableStateOf<String?>(null) }
    val player = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            volume = 1f
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(streamUrl) {
        buffering = true
        errorText = null
        player.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) errorText = null
            }

            override fun onPlayerError(error: PlaybackException) {
                buffering = false
                errorText = error.message ?: "Playback unavailable"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    useController = true
                    setControllerShowTimeoutMs(0)
                    setControllerHideOnTouch(false)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    this.player = player
                    showController()
                }
            },
            update = {
                it.player = player
                it.showController()
            },
            modifier = Modifier.matchParentSize(),
        )
        if (buffering) {
            CircularProgressIndicator(color = SmartVisionColors.CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
        }
        errorText?.let {
            Text(text = it, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PrivateMediaWebPreview(
    embedUrl: String,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    var webViewRef by remember(embedUrl) { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                focusedScale = 1.01f,
                cornerRadius = 12.dp,
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)) {
                    webViewRef?.requestFocus()
                    false
                } else {
                    false
                }
            }
            .focusable()
            .background(Color.Black)
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else Color.Transparent,
                ),
                RoundedCornerShape(12.dp),
            ),
    ) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { context ->
                PrivateMediaTvWebView(context).apply {
                    webViewRef = this
                    configurePrivateMediaWebView(focusable = true)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    loadUrl(embedUrl)
                }
            },
            update = { webView ->
                webViewRef = webView
                if (webView.url != embedUrl) {
                    webView.loadUrl(embedUrl)
                }
            },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configurePrivateMediaWebView(focusable: Boolean) {
    setBackgroundColor(android.graphics.Color.BLACK)
    isFocusable = focusable
    isFocusableInTouchMode = focusable
    descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    overScrollMode = View.OVER_SCROLL_NEVER
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    webViewClient = WebViewClient()
    webChromeClient = WebChromeClient()
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    settings.userAgentString = settings.userAgentString
        .replace("; wv", "")
        .replace("Version/4.0 ", "")
    CookieManager.getInstance().setAcceptCookie(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
}

@Composable
private fun MediaTransferDialog(
    session: MediaTransferSession,
    strings: SmartVisionStrings,
    onDismiss: () -> Unit,
) {
    val title = when (session.mode) {
        MediaTransferMode.Import -> strings.mediaImportPhoneTitle
        MediaTransferMode.Export -> strings.mediaExportPhoneTitle
    }
    val subtitle = when (session.mode) {
        MediaTransferMode.Import -> strings.mediaImportPhoneSubtitle
        MediaTransferMode.Export -> session.fileName?.let { String.format(strings.mediaExportPhoneSubtitleWithFile, it) }
            ?: strings.mediaExportPhoneSubtitle
    }
    val qrBitmap = remember(session.url) { createTransferQrBitmap(session.url, 520) }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(session.url) {
        withFrameNanos { }
        delay(80)
        runCatching { closeFocusRequester.requestFocus() }
    }

    MediaDialogPanel(title = title, onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(190.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(8.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = subtitle,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Body,
                )
                Text(
                    text = session.url,
                    color = SmartVisionColors.CyanAccent,
                    style = SmartVisionType.Caption,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = strings.mediaTransferSameWifi,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        TvButton(
            text = strings.recorderClose,
            onClick = onDismiss,
            variant = TvButtonVariant.Secondary,
            focusRequester = closeFocusRequester,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
        )
    }
}

@Composable
private fun MediaRenameDialog(
    file: MediaFileUi,
    strings: SmartVisionStrings,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var value by remember(file.id) { mutableStateOf(file.displayName) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        runCatching { focusRequester.requestFocus() }
    }

    MediaDialogPanel(title = strings.mediaRenameTitle, onDismiss = onDismiss) {
        MediaTextField(
            value = value,
            onValueChange = { value = it },
            label = strings.mediaNewName,
            focusRequester = focusRequester,
        )
        DialogActions(
            strings = strings,
            confirmText = strings.mediaRename,
            confirmEnabled = value.trim().isNotEmpty(),
            onDismiss = onDismiss,
            onConfirm = { onRename(value) },
        )
    }
}

@Composable
private fun MediaMoveDialog(
    file: MediaFileUi,
    folders: List<MediaFolderUi>,
    strings: SmartVisionStrings,
    onDismiss: () -> Unit,
    onMove: (Long?) -> Unit,
) {
    val rootFolderFocusRequester = remember { FocusRequester() }

    LaunchedEffect(file.id) {
        withFrameNanos { }
        delay(80)
        runCatching { rootFolderFocusRequester.requestFocus() }
    }

    MediaDialogPanel(title = strings.mediaMoveTitle, onDismiss = onDismiss) {
        Text(
            text = file.displayName,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Label,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.mediaChooseFolder,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TvButton(
                    text = strings.mediaRootFolder,
                    onClick = { onMove(null) },
                    selected = file.folderId == null,
                    variant = TvButtonVariant.Secondary,
                    focusRequester = rootFolderFocusRequester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                )
            }
            items(folders, key = { it.id }) { folder ->
                TvButton(
                    text = folder.name,
                    onClick = { onMove(folder.id) },
                    selected = file.folderId == folder.id,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        TvButton(
            text = strings.cancel,
            onClick = onDismiss,
            variant = TvButtonVariant.Secondary,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
        )
    }
}

@Composable
private fun MediaDeleteDialog(
    file: MediaFileUi,
    strings: SmartVisionStrings,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    TvConfirmationDialog(
        title = strings.mediaDeleteTitle,
        itemLabel = file.displayName,
        message = strings.mediaDeleteSubtitle,
        confirmText = strings.delete,
        cancelText = strings.cancel,
        onDismiss = onDismiss,
        onConfirm = onDelete,
    )
}

@Composable
private fun MediaDialogPanel(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    TvDialogSurface(
        title = title,
        onDismiss = onDismiss,
        width = 520.dp,
    ) {
        content()
    }
}

@Composable
private fun DialogActions(
    strings: SmartVisionStrings,
    confirmText: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmVariant: TvButtonVariant = TvButtonVariant.Primary,
    focusConfirmOnOpen: Boolean = false,
) {
    val confirmFocusRequester = remember { FocusRequester() }

    LaunchedEffect(focusConfirmOnOpen, confirmEnabled) {
        if (focusConfirmOnOpen && confirmEnabled) {
            delay(120)
            runCatching { confirmFocusRequester.requestFocus() }
        }
    }

    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TvButton(
            text = strings.cancel,
            onClick = onDismiss,
            variant = TvButtonVariant.Secondary,
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
        )
        TvButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            variant = confirmVariant,
            focusRequester = if (focusConfirmOnOpen) confirmFocusRequester else null,
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
        )
    }
}

@Composable
private fun MediaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
        cursorBrush = SolidColor(SmartVisionColors.CyanAccent),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .focusRequester(focusRequester)
            .background(SmartVisionColors.Surface, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, SmartVisionColors.CyanAccent), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isBlank()) {
                    Text(
                        text = label,
                        color = SmartVisionColors.TextSecondary.copy(alpha = 0.62f),
                        style = SmartVisionType.Body,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun MediaInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            modifier = Modifier.width(106.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Caption,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MediaStatusMessage(
    message: String,
    error: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (error) SmartVisionColors.Error.copy(alpha = 0.16f) else SmartVisionColors.Success.copy(alpha = 0.16f))
            .border(
                BorderStroke(1.dp, if (error) SmartVisionColors.Error else SmartVisionColors.Success),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = message,
            color = if (error) SmartVisionColors.Error else SmartVisionColors.Success,
            style = SmartVisionType.Caption,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun storageLabel(strings: SmartVisionStrings, state: MediaScreenState): String {
    val storage = state.storageInfo ?: return strings.mediaStorageReady
    return "${strings.mediaStoragePath}: ${storage.rootPath}\n${strings.mediaAvailableSpace}: ${formatBytes(storage.availableBytes)}"
}

private fun MediaArea.label(strings: SmartVisionStrings): String =
    when (this) {
        MediaArea.AllFiles -> strings.mediaAllFiles
        MediaArea.Recordings -> strings.mediaRecordings
        MediaArea.Imports -> strings.mediaImports
        MediaArea.Folders -> strings.mediaFolders
        MediaArea.Transfers -> strings.mediaTransfers
    }

private fun MediaCenterSource.label(strings: SmartVisionStrings): String =
    when (this) {
        MediaCenterSource.Recording -> strings.mediaRecordings
        MediaCenterSource.Import -> strings.mediaImports
        MediaCenterSource.Transfer -> strings.mediaTransfers
        MediaCenterSource.Local -> strings.mediaRootFolder
    }

private fun MediaCenterFileType.label(strings: SmartVisionStrings): String =
    when (this) {
        MediaCenterFileType.Video -> strings.mediaTypeVideo
        MediaCenterFileType.Photo -> strings.mediaTypePhoto
        MediaCenterFileType.Audio -> strings.mediaTypeAudio
        MediaCenterFileType.Other -> strings.mediaTypeFile
    }

private fun MediaArea.icon(): ImageVector =
    when (this) {
        MediaArea.AllFiles -> Icons.Default.Menu
        MediaArea.Recordings -> Icons.Default.Theaters
        MediaArea.Imports -> Icons.Default.FileDownload
        MediaArea.Folders -> Icons.Default.Storage
        MediaArea.Transfers -> Icons.Default.Devices
    }

private fun MediaFileUi.mediaIcon(): ImageVector =
    when (mediaType) {
        MediaCenterFileType.Video -> Icons.Default.Theaters
        MediaCenterFileType.Photo -> Icons.Default.FileDownload
        MediaCenterFileType.Audio -> Icons.Default.PlayArrow
        MediaCenterFileType.Other -> Icons.Default.Storage
    }

private val MediaFileUi.isPlayable: Boolean
    get() = mediaType == MediaCenterFileType.Video ||
        mediaType == MediaCenterFileType.Photo ||
        mediaType == MediaCenterFileType.Audio

private fun MediaActionMessage.label(strings: SmartVisionStrings): String =
    when (this) {
        MediaActionMessage.Renamed -> strings.mediaRenameSuccess
        MediaActionMessage.Moved -> strings.mediaMoveSuccess
        MediaActionMessage.Deleted -> strings.mediaDeleteSuccess
        MediaActionMessage.TransferUploaded -> strings.mediaTransferUploadSuccess
    }

private enum class MediaPreviewAction {
    Play,
    Rename,
    Export,
}

private fun mediaTypeColor(type: MediaCenterFileType): Color =
    when (type) {
        MediaCenterFileType.Video -> SmartVisionColors.CyanAccent
        MediaCenterFileType.Photo -> SmartVisionColors.Success
        MediaCenterFileType.Audio -> SmartVisionColors.Warning
        MediaCenterFileType.Other -> SmartVisionColors.TextSecondary
    }

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return java.lang.String.format(java.util.Locale.US, "%.1f %s", value, units[unitIndex])
}

private fun createTransferQrBitmap(content: String, size: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "UTF-8",
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(
                x,
                y,
                if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE,
            )
        }
    }
    return bitmap
}
