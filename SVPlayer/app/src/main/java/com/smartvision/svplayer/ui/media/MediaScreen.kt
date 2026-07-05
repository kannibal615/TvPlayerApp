package com.smartvision.svplayer.ui.media

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.domain.access.PremiumFeatureGateState
import com.smartvision.svplayer.media.MediaCenterFileType
import com.smartvision.svplayer.media.MediaCenterSource
import com.smartvision.svplayer.media.transfer.MediaTransferMode
import com.smartvision.svplayer.media.transfer.MediaTransferSession
import com.smartvision.svplayer.ui.catalog.MediaCatalogDimens
import com.smartvision.svplayer.ui.catalog.MediaCatalogHeader
import com.smartvision.svplayer.ui.catalog.MediaCatalogPanel
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
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
import kotlinx.coroutines.delay

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
    onPlayFile: (Long) -> Unit,
    onLockedFeature: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: MediaViewModel = viewModel(
        factory = viewModelFactory {
            MediaViewModel(container.mediaRepository, container.mediaTransferServer)
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firstFocusRequester = remember { FocusRequester() }
    var renameTarget by remember { mutableStateOf<MediaFileUi?>(null) }
    var moveTarget by remember { mutableStateOf<MediaFileUi?>(null) }
    var deleteTarget by remember { mutableStateOf<MediaFileUi?>(null) }

    LaunchedEffect(access.allowed) {
        if (access.allowed) {
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
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(MediaCatalogDimens.HeaderGap))

        if (!access.allowed) {
            LockedMediaCenter(
                strings = strings,
                access = access,
                onLockedFeature = onLockedFeature,
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
            onFileSelected = viewModel::selectFile,
            onRefresh = viewModel::refreshStorage,
            onPlayFile = onPlayFile,
            onRename = { renameTarget = state.selectedFile },
            onMove = { moveTarget = state.selectedFile },
            onDelete = { deleteTarget = state.selectedFile },
            transferAccess = transferAccess,
            onImportPhone = {
                if (transferAccess.allowed) viewModel.startPhoneImport() else onLockedFeature()
            },
            onExportPhone = {
                if (transferAccess.allowed) viewModel.startPhoneExport() else onLockedFeature()
            },
            firstFocusRequester = firstFocusRequester,
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
                viewModel.renameSelected(requestedName)
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
                viewModel.moveSelected(targetFolderId)
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
                viewModel.deleteSelected()
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
            )
        }
    }
}

@Composable
private fun MediaWorkspace(
    strings: SmartVisionStrings,
    state: MediaScreenState,
    onAreaSelected: (MediaArea) -> Unit,
    onFileSelected: (Long) -> Unit,
    onRefresh: () -> Unit,
    onPlayFile: (Long) -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    transferAccess: PremiumFeatureGateResult,
    onImportPhone: () -> Unit,
    onExportPhone: () -> Unit,
    firstFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val firstContentFocusRequester = remember { FocusRequester() }
    val previewActionFocusRequester = remember { FocusRequester() }
    var contentFocusSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(contentFocusSignal, state.selectedArea, state.visibleFiles.size, state.folders.size) {
        if (contentFocusSignal > 0) {
            delay(120)
            runCatching { firstContentFocusRequester.requestFocus() }
        }
    }

    fun focusPreviewAction(): Boolean {
        runCatching { previewActionFocusRequester.requestFocus() }
        return true
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MediaLibraryPanel(
            strings = strings,
            state = state,
            onAreaSelected = { area ->
                onAreaSelected(area)
                contentFocusSignal += 1
            },
            transferAccess = transferAccess,
            onImportPhone = onImportPhone,
            firstFocusRequester = firstFocusRequester,
            modifier = Modifier
                .width(252.dp)
                .fillMaxHeight(),
        )

        MediaContentPanel(
            strings = strings,
            state = state,
            onFileSelected = onFileSelected,
            onRefresh = onRefresh,
            firstContentFocusRequester = firstContentFocusRequester,
            onMoveRightToPreview = { focusPreviewAction() },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )

        MediaPreviewPanel(
            strings = strings,
            state = state,
            onPlay = { state.selectedFile?.takeIf { it.isPlayable }?.id?.let(onPlayFile) },
            onRename = onRename,
            onMove = onMove,
            onDelete = onDelete,
            transferAccess = transferAccess,
            onExportPhone = onExportPhone,
            previewActionFocusRequester = previewActionFocusRequester,
            modifier = Modifier
                .width(336.dp)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun MediaLibraryPanel(
    strings: SmartVisionStrings,
    state: MediaScreenState,
    onAreaSelected: (MediaArea) -> Unit,
    transferAccess: PremiumFeatureGateResult,
    onImportPhone: () -> Unit,
    firstFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = strings.mediaLibrary,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MediaArea.values().forEachIndexed { index, area ->
                TvButton(
                    text = "${area.label(strings)}  ${state.countFor(area)}",
                    onClick = { onAreaSelected(area) },
                    selected = state.selectedArea == area,
                    variant = if (state.selectedArea == area) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                    focusRequester = if (index == 0) firstFocusRequester else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        MediaLibraryInfoCard(text = strings.mediaMvpNotice)

        Spacer(Modifier.height(12.dp))

        MediaLibraryInfoCard(text = storageLabel(strings, state), emphasized = true)

        if (transferAccess.showDisabledControl) {
            Spacer(Modifier.height(12.dp))
            TvButton(
                text = strings.mediaImportPhone,
                onClick = onImportPhone,
                enabled = !state.transferInProgress,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            )
        }
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
    onRefresh: () -> Unit,
    firstContentFocusRequester: FocusRequester,
    onMoveRightToPreview: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = if (state.selectedArea == MediaArea.Folders) strings.mediaFolders else strings.mediaRecentFiles,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.mediaCenterTitle,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.TitleM,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = String.format(strings.mediaFilesFound, state.files.size) +
                            " | " +
                            String.format(strings.mediaFoldersFound, state.folders.size),
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TvButton(
                    text = if (state.refreshing) strings.refreshing else strings.refresh,
                    onClick = onRefresh,
                    enabled = !state.refreshing && !state.operationInProgress,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .width(142.dp)
                        .height(42.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
            }

            state.errorMessage?.let { message ->
                MediaStatusMessage(message = message, error = true)
            }
            state.message?.let { message ->
                MediaStatusMessage(message = message.label(strings), error = false)
            }
            if (state.operationInProgress) {
                MediaStatusMessage(message = strings.mediaOperationInProgress, error = false)
            }

            if (state.selectedArea == MediaArea.Folders) {
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
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
                if (active) {
                    SmartVisionColors.PrimaryDark.copy(alpha = 0.58f)
                } else {
                    SmartVisionColors.SurfaceElevated.copy(alpha = 0.68f)
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
                    if (active) SmartVisionColors.CyanAccent else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Theaters,
            contentDescription = null,
            tint = mediaTypeColor(file.mediaType),
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.displayName,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${file.source.label(strings)} | ${file.sizeLabel} | ${file.updatedLabel}",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = file.mediaType.label(),
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            maxLines = 1,
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
                    SmartVisionColors.PrimaryDark.copy(alpha = 0.48f)
                } else {
                    SmartVisionColors.SurfaceElevated.copy(alpha = 0.68f)
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
                    if (focusState.isFocused) SmartVisionColors.CyanAccent else SmartVisionColors.Border,
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
private fun MediaPreviewPanel(
    strings: SmartVisionStrings,
    state: MediaScreenState,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    transferAccess: PremiumFeatureGateResult,
    onExportPhone: () -> Unit,
    previewActionFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedFile
    val playEnabled = selected?.isPlayable == true && !state.operationInProgress
    val editEnabled = selected != null && !state.operationInProgress
    val transferEnabled = selected != null && !state.operationInProgress && !state.transferInProgress
    MediaCatalogPanel(
        title = strings.mediaPreview,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(MediaCatalogDimens.ItemRadius))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                SmartVisionColors.Primary.copy(alpha = 0.30f),
                                Color(0xAA0A1323),
                                Color.Black.copy(alpha = 0.58f),
                            ),
                            radius = 520f,
                        ),
                    )
                    .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(MediaCatalogDimens.ItemRadius)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Theaters,
                        contentDescription = null,
                        tint = selected?.let { mediaTypeColor(it.mediaType) } ?: SmartVisionColors.TextSecondary,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = selected?.displayName ?: state.selectedArea.label(strings),
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.TitleS,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (selected == null) {
                Text(
                    text = strings.mediaNoSelection,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Label,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x660A1323))
                        .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MediaInfoRow(strings.mediaFileSize, selected.sizeLabel)
                    MediaInfoRow(strings.mediaUpdatedAt, selected.updatedLabel)
                    MediaInfoRow(strings.mediaFolderLabel, selected.folderName ?: strings.mediaRootFolder)
                    MediaInfoRow("Source", selected.source.label(strings))
                    MediaInfoRow("Type", selected.mediaType.label())
                    MediaInfoRow(strings.mediaStoragePath, selected.relativePath)
                }
            }

            state.storageInfo?.let { storage ->
                Spacer(Modifier.height(4.dp))
                MediaInfoRow(strings.mediaAvailableSpace, formatBytes(storage.availableBytes))
            }

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvButton(
                    text = strings.mediaPlay,
                    onClick = onPlay,
                    enabled = playEnabled,
                    focusRequester = if (playEnabled) previewActionFocusRequester else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                )
                TvButton(
                    text = strings.mediaRename,
                    onClick = onRename,
                    enabled = editEnabled,
                    focusRequester = if (!playEnabled && editEnabled) previewActionFocusRequester else null,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvButton(
                    text = strings.mediaMove,
                    onClick = onMove,
                    enabled = editEnabled,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                )
                TvButton(
                    text = strings.delete,
                    onClick = onDelete,
                    enabled = editEnabled,
                    variant = TvButtonVariant.Danger,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                )
            }
            if (transferAccess.showDisabledControl) {
                TvButton(
                    text = strings.mediaExportPhone,
                    onClick = onExportPhone,
                    enabled = transferEnabled,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                )
            }
        }
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
    MediaDialogPanel(title = strings.mediaDeleteTitle, onDismiss = onDismiss) {
        Text(
            text = file.displayName,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Label,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.mediaDeleteSubtitle,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
        )
        DialogActions(
            strings = strings,
            confirmText = strings.delete,
            confirmEnabled = true,
            confirmVariant = TvButtonVariant.Danger,
            onDismiss = onDismiss,
            onConfirm = onDelete,
        )
    }
}

@Composable
private fun MediaDialogPanel(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xF20A1323))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(10.dp))
                .padding(22.dp),
        ) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
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
) {
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

private fun MediaCenterFileType.label(): String =
    when (this) {
        MediaCenterFileType.Video -> "Video"
        MediaCenterFileType.Photo -> "Photo"
        MediaCenterFileType.Audio -> "Audio"
        MediaCenterFileType.Other -> "File"
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
