package com.smartvision.svplayer.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.smartvision.svplayer.domain.parental.ParentalHiddenContentType
import com.smartvision.svplayer.ui.components.NumericPinDialog
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
import com.smartvision.svplayer.ui.components.TvSectionCard
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private enum class ParentalPanelSection {
    Activation,
    Keywords,
    Results,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ParentalControlPanel(
    strings: SmartVisionStrings,
    state: ParentalControlUiState,
    viewModel: ParentalControlViewModel,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester? = null,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val ownedActivationSectionRequester = remember { FocusRequester() }
    val activationSectionRequester = initialFocusRequester ?: ownedActivationSectionRequester
    val keywordsSectionRequester = remember { FocusRequester() }
    val resultsSectionRequester = remember { FocusRequester() }
    val toggleRequester = remember { FocusRequester() }
    val changePinRequester = remember { FocusRequester() }
    val keywordInputRequester = remember { FocusRequester() }
    val addRequester = remember { FocusRequester() }
    val firstFolderRequester = remember { FocusRequester() }
    val firstItemRequester = remember { FocusRequester() }
    val retryRequester = remember { FocusRequester() }
    var enteredSection by remember { mutableStateOf<ParentalPanelSection?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var editingKeywordIndex by remember { mutableIntStateOf(-1) }
    var deletingKeywordIndex by remember { mutableIntStateOf(-1) }
    var pendingKeywordFocus by remember { mutableIntStateOf(-1) }
    var keywordEditRequest by remember { mutableIntStateOf(0) }
    var panelHasFocus by remember { mutableStateOf(false) }
    val editRequesters = remember(state.keywords.size) { List(state.keywords.size) { FocusRequester() } }
    val profileToggleRequesters = remember(state.profiles.map { it.id }) {
        state.profiles.associate { it.id to FocusRequester() }
    }

    BackHandler(enabled = panelHasFocus) {
        if (enteredSection == null) {
            onExitToMenu()
        } else {
            val section = enteredSection
            enteredSection = null
            scope.launch {
                delay(40)
                runCatching {
                    when (section) {
                        ParentalPanelSection.Activation -> activationSectionRequester.requestFocus()
                        ParentalPanelSection.Keywords -> keywordsSectionRequester.requestFocus()
                        ParentalPanelSection.Results -> resultsSectionRequester.requestFocus()
                        null -> Unit
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(140)
        runCatching { activationSectionRequester.requestFocus() }
    }

    LaunchedEffect(enteredSection, state.folders, state.items, state.resultsError) {
        delay(45)
        runCatching {
            when (enteredSection) {
                ParentalPanelSection.Activation -> toggleRequester.requestFocus()
                ParentalPanelSection.Keywords -> keywordInputRequester.requestFocus()
                ParentalPanelSection.Results -> when {
                    state.resultsError -> retryRequester.requestFocus()
                    state.folders.isNotEmpty() -> firstFolderRequester.requestFocus()
                    state.items.isNotEmpty() -> firstItemRequester.requestFocus()
                    else -> Unit
                }
                null -> Unit
            }
        }
    }

    LaunchedEffect(state.enabled, enteredSection) {
        if (!state.enabled && enteredSection == ParentalPanelSection.Activation) {
            delay(45)
            runCatching { toggleRequester.requestFocus() }
        }
    }

    LaunchedEffect(state.keywords, pendingKeywordFocus) {
        if (pendingKeywordFocus >= 0) {
            delay(70)
            if (state.keywords.isEmpty()) {
                runCatching { keywordInputRequester.requestFocus() }
            } else {
                val index = pendingKeywordFocus.coerceIn(0, state.keywords.lastIndex)
                runCatching { editRequesters.getOrNull(index)?.requestFocus() }
            }
            pendingKeywordFocus = -1
        }
    }

    Column(
        modifier = modifier
            .onFocusChanged { panelHasFocus = it.hasFocus },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Lock, null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(9.dp))
            Text(strings.parentalControl, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TvButton(
                text = strings.changePin,
                onClick = { showPinDialog = true },
                focusRequester = changePinRequester,
                modifier = Modifier
                    .widthIn(min = 120.dp)
                    .height(32.dp)
                    .focusProperties { down = toggleRequester },
            )
        }
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xEA0B1526), Color(0xF207101E)),
                    ),
                )
                .border(1.dp, SmartVisionColors.Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "parental-activation") {
                ParentalSectionFrame(
                    title = strings.parentalActivation,
                    icon = Icons.Default.Security,
                    focusedRequester = activationSectionRequester,
                    entered = enteredSection == ParentalPanelSection.Activation,
                    onEnter = { enteredSection = ParentalPanelSection.Activation },
                    onFocused = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier.fillMaxWidth(),
                    headerTrailing = {
                        CompactParentalToggle(
                            checked = state.enabled,
                            onToggle = { viewModel.setEnabled(!state.enabled) },
                            focusRequester = toggleRequester,
                            canFocus = enteredSection == ParentalPanelSection.Activation,
                            upFocusRequester = changePinRequester,
                            downFocusRequester = if (state.enabled) {
                                state.profiles.firstOrNull()?.id?.let(profileToggleRequesters::get)
                            } else {
                                null
                            },
                            onExitLeft = {
                                enteredSection = null
                                scope.launch {
                                    delay(40)
                                    runCatching { activationSectionRequester.requestFocus() }
                                }
                            },
                        )
                    },
                ) {
                    if (state.profiles.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().height(116.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(state.profiles, key = { _, profile -> profile.id }) { index, profile ->
                                ParentalProfileCard(
                                    profile = profile,
                                    globalEnabled = state.enabled,
                                    focusRequester = profileToggleRequesters.getValue(profile.id),
                                    upFocusRequester = toggleRequester,
                                    leftFocusRequester = state.profiles.getOrNull(index - 1)?.id
                                        ?.let(profileToggleRequesters::get) ?: toggleRequester,
                                    rightFocusRequester = state.profiles.getOrNull(index + 1)?.id
                                        ?.let(profileToggleRequesters::get),
                                    canFocus = enteredSection == ParentalPanelSection.Activation,
                                    onToggle = { viewModel.setProfileEnabled(profile.id, !profile.enabled) },
                                )
                            }
                        }
                    }
                }
            }

                item(key = "parental-keywords") {
                ParentalSectionFrame(
                    title = strings.parentalFilterKeywords,
                    icon = Icons.AutoMirrored.Filled.List,
                    focusedRequester = keywordsSectionRequester,
                    entered = enteredSection == ParentalPanelSection.Keywords,
                    onEnter = {
                        enteredSection = ParentalPanelSection.Keywords
                        keywordEditRequest += 1
                    },
                    onFocused = { scope.launch { listState.animateScrollToItem(1) } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ParentalKeywordInput(
                                value = state.draft,
                                onValueChange = viewModel::updateDraft,
                                placeholder = strings.parentalAddKeywordPlaceholder,
                                focusRequester = keywordInputRequester,
                                nextFocusRequester = addRequester,
                                canFocus = enteredSection == ParentalPanelSection.Keywords,
                                startEditingSignal = keywordEditRequest,
                                onStartEditing = {
                                    enteredSection = ParentalPanelSection.Keywords
                                    keywordEditRequest += 1
                                },
                                modifier = Modifier.weight(1f),
                            )
                            TvButton(
                                text = strings.parentalAddKeyword,
                                leadingIcon = Icons.Default.Add,
                                onClick = { viewModel.addDraft() },
                                focusRequester = addRequester,
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(38.dp)
                                    .focusProperties { canFocus = enteredSection == ParentalPanelSection.Keywords },
                            )
                        }
                        ParentalMessageLine(strings, state)
                        if (state.keywords.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val columns = when {
                                    maxWidth >= 720.dp -> 4
                                    maxWidth >= 420.dp -> 3
                                    maxWidth >= 280.dp -> 2
                                    else -> 1
                                }
                                val gap = 8.dp
                                val cardWidth = (maxWidth - gap * (columns - 1)) / columns
                                FlowRow(
                                    maxItemsInEachRow = columns,
                                    horizontalArrangement = Arrangement.spacedBy(gap),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    state.keywords.forEachIndexed { index, keyword ->
                                        key(keyword.lowercase(Locale.ROOT)) {
                                            KeywordChip(
                                                keyword = keyword,
                                                canFocus = enteredSection == ParentalPanelSection.Keywords,
                                                editFocusRequester = editRequesters[index],
                                                onEdit = { editingKeywordIndex = index },
                                                onDelete = {
                                                    deletingKeywordIndex = index
                                                },
                                                modifier = Modifier.width(cardWidth),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

                item(key = "parental-results") {
                ParentalSectionFrame(
                    title = strings.parentalFilterResults,
                    icon = Icons.Default.FilterAlt,
                    focusedRequester = resultsSectionRequester,
                    entered = enteredSection == ParentalPanelSection.Results,
                    onEnter = {
                        if (state.resultsError || state.folders.isNotEmpty() || state.items.isNotEmpty()) {
                            enteredSection = ParentalPanelSection.Results
                        }
                    },
                    onFocused = { scope.launch { listState.animateScrollToItem(2) } },
                    modifier = Modifier.fillMaxWidth(),
                    headerTrailing = {
                        CountPill(String.format(strings.parentalFoldersCount, state.counts.folders), Icons.Default.Folder)
                        Spacer(Modifier.width(8.dp))
                        CountPill(String.format(strings.parentalItemsCount, state.counts.items), Icons.AutoMirrored.Filled.List)
                    },
                ) {
                    when {
                        !state.enabled -> ResultInfoText(strings.parentalResultsDisabled)
                        state.resultsLoading && state.folders.isEmpty() && state.items.isEmpty() -> Box(
                            Modifier.fillMaxWidth().height(90.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = SmartVisionColors.CyanAccent, modifier = Modifier.size(28.dp))
                        }
                        state.resultsError && state.folders.isEmpty() && state.items.isEmpty() -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            ResultInfoText(strings.parentalResultsError)
                            Spacer(Modifier.height(8.dp))
                            TvButton(
                                text = strings.parentalRetry,
                                onClick = viewModel::retry,
                                focusRequester = retryRequester,
                                modifier = Modifier
                                    .height(38.dp)
                                    .focusProperties { canFocus = enteredSection == ParentalPanelSection.Results },
                            )
                        }
                        else -> Column(Modifier.fillMaxWidth()) {
                            if (state.resultsError) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(strings.parentalResultsError, color = Color(0xFFFF7777), style = SmartVisionType.Caption)
                                    TvButton(
                                        text = strings.parentalRetry,
                                        onClick = viewModel::retry,
                                        focusRequester = retryRequester,
                                        modifier = Modifier.height(36.dp).focusProperties {
                                            canFocus = enteredSection == ParentalPanelSection.Results
                                        },
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HiddenFoldersList(
                                    strings = strings,
                                    state = state,
                                    firstFocusRequester = firstFolderRequester,
                                    canFocus = enteredSection == ParentalPanelSection.Results,
                                    onLoadMore = viewModel::loadMoreFolders,
                                    onFolder = viewModel::selectFolder,
                                    rightFocusRequester = firstItemRequester,
                                    modifier = Modifier.weight(0.88f),
                                )
                                HiddenItemsList(
                                    strings = strings,
                                    state = state,
                                    firstFocusRequester = firstItemRequester,
                                    canFocus = enteredSection == ParentalPanelSection.Results,
                                    onLoadMore = viewModel::loadMoreItems,
                                    leftFocusRequester = firstFolderRequester,
                                    modifier = Modifier.weight(1.42f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (showPinDialog) {
        NumericPinDialog(
            title = strings.changePin,
            strings = strings,
            requireConfirmation = true,
            onDismiss = { showPinDialog = false },
            onSubmit = { pin ->
                viewModel.setPin(pin)
                showPinDialog = false
                true
            },
        )
    }

    if (editingKeywordIndex in state.keywords.indices) {
        KeywordEditDialog(
            strings = strings,
            initialValue = state.keywords[editingKeywordIndex],
            error = state.keywordError?.let { strings.keywordErrorText(it) },
            onDismiss = {
                val index = editingKeywordIndex
                editingKeywordIndex = -1
                viewModel.clearTransientMessage()
                pendingKeywordFocus = index
            },
            onSubmit = { value ->
                val index = editingKeywordIndex
                if (viewModel.updateKeyword(index, value)) {
                    editingKeywordIndex = -1
                    pendingKeywordFocus = index
                    true
                } else {
                    false
                }
            },
        )
    }

    if (deletingKeywordIndex in state.keywords.indices) {
        val index = deletingKeywordIndex
        val keyword = state.keywords[index]
        TvConfirmationDialog(
            title = strings.parentalKeywordDeleteTitle,
            itemLabel = keyword,
            message = strings.destructiveActionWarning,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onDismiss = {
                deletingKeywordIndex = -1
                pendingKeywordFocus = index
            },
            onConfirm = {
                deletingKeywordIndex = -1
                pendingKeywordFocus = if (state.keywords.size <= 1) {
                    0
                } else {
                    index.coerceAtMost(state.keywords.lastIndex - 1)
                }
                viewModel.deleteKeyword(index)
            },
        )
    }
}

@Composable
private fun ParentalSectionFrame(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    focusedRequester: FocusRequester,
    entered: Boolean,
    onEnter: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    TvSectionCard(
        title = title,
        icon = icon,
        modifier = modifier
            .focusRequester(focusedRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                if (focused && event.type == KeyEventType.KeyDown &&
                    (event.isConfirmationKey() || event.key == Key.DirectionRight)
                ) {
                    onEnter()
                    true
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onEnter,
            )
            .focusProperties { canFocus = !entered }
            .focusable(),
        backgroundColor = if (focused || entered) focusStyle.background else Color(0xB8081628),
        borderColor = if (focused || entered) focusStyle.accent else SmartVisionColors.Border.copy(alpha = 0.86f),
        borderWidth = if (focused || entered) focusStyle.borderWidth else 1.dp,
        headerTrailing = headerTrailing,
    ) {
        content()
    }
}

@Composable
private fun CompactParentalToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    focusRequester: FocusRequester,
    canFocus: Boolean,
    enabled: Boolean = true,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    onExitLeft: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focusStyle = LocalTvFocusStyle.current
    val color = (if (checked) Color(0xFF16C96B) else Color(0xFFE15454))
        .copy(alpha = if (enabled) 1f else 0.42f)
    Row(
        modifier = Modifier
            .width(54.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
            .border(if (focused) focusStyle.borderWidth else 1.dp, if (focused) focusStyle.accent else Color.White.copy(alpha = 0.24f), RoundedCornerShape(50))
            .focusRequester(focusRequester)
            .focusProperties {
                this.canFocus = canFocus && enabled
                upFocusRequester?.let { up = it }
                downFocusRequester?.let { down = it }
                leftFocusRequester?.let { left = it }
                rightFocusRequester?.let { right = it }
            }
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && onExitLeft != null) {
                    onExitLeft()
                    true
                } else if (enabled && event.type == KeyEventType.KeyDown && event.isConfirmationKey()) {
                    onToggle()
                    true
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onToggle,
            )
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (checked) Arrangement.End else Arrangement.Start,
    ) {
        Text(if (checked) "ON" else "OFF", color = Color.White, style = SmartVisionType.Caption, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ParentalProfileCard(
    profile: ParentalProfileUiState,
    globalEnabled: Boolean,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester?,
    canFocus: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xC30B192B))
            .border(1.dp, SmartVisionColors.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ProfileAvatarImage(
            avatarId = profile.avatarId,
            profileType = profile.type,
            modifier = Modifier.size(42.dp),
        )
        Text(
            text = profile.name,
            color = if (globalEnabled) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CompactParentalToggle(
            checked = profile.enabled,
            onToggle = onToggle,
            focusRequester = focusRequester,
            canFocus = canFocus,
            enabled = true,
            upFocusRequester = upFocusRequester,
            leftFocusRequester = leftFocusRequester,
            rightFocusRequester = rightFocusRequester,
        )
    }
}

@Composable
private fun ParentalKeywordInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    canFocus: Boolean,
    startEditingSignal: Int,
    onStartEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(7.dp)
    LaunchedEffect(startEditingSignal) {
        if (startEditingSignal > 0) {
            editing = true
            delay(40)
            keyboard?.show()
        }
    }
    LaunchedEffect(editing) {
        if (editing) {
            delay(40)
            keyboard?.show()
        }
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = !editing,
        singleLine = true,
        textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
        cursorBrush = SolidColor(focusStyle.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            editing = false
            keyboard?.hide()
            runCatching { nextFocusRequester.requestFocus() }
        }),
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(if (focused || editing) focusStyle.background else SmartVisionColors.Surface.copy(alpha = 0.82f))
            .border(if (focused) focusStyle.borderWidth else 1.dp, if (focused || editing) focusStyle.accent else SmartVisionColors.Border, shape)
            .focusRequester(focusRequester)
            .focusProperties { this.canFocus = canFocus }
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) {
                    editing = false
                    keyboard?.hide()
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.isConfirmationKey() && !editing -> {
                        editing = true
                        true
                    }
                    event.key == Key.Back && editing -> {
                        editing = false
                        keyboard?.hide()
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(canFocus) {
                detectTapGestures {
                    onStartEditing()
                }
            }
            .padding(horizontal = 12.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (value.isBlank()) Text(placeholder, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
                inner()
            }
        },
    )
}

@Composable
private fun KeywordChip(
    keyword: String,
    canFocus: Boolean,
    editFocusRequester: FocusRequester,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.76f))
            .border(1.dp, SmartVisionColors.Border, RoundedCornerShape(7.dp))
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.DragIndicator, null, tint = SmartVisionColors.TextSecondary, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(keyword, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        ParentalIconAction(Icons.Default.Edit, onEdit, editFocusRequester, canFocus)
        ParentalIconAction(Icons.Default.Delete, onDelete, null, canFocus, danger = true)
    }
}

@Composable
private fun ParentalIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    canFocus: Boolean,
    danger: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) focusStyle.background else Color.Transparent)
            .border(if (focused) focusStyle.borderWidth else 0.dp, if (focused) focusStyle.accent else Color.Transparent, RoundedCornerShape(6.dp))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties { this.canFocus = canFocus }
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isConfirmationKey()) {
                    onClick()
                    true
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(enabled = canFocus, interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (danger) Color(0xFFFF6B6B) else SmartVisionColors.TextPrimary, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun HiddenFoldersList(
    strings: SmartVisionStrings,
    state: ParentalControlUiState,
    firstFocusRequester: FocusRequester,
    canFocus: Boolean,
    onLoadMore: () -> Unit,
    onFolder: (com.smartvision.svplayer.domain.parental.ParentalHiddenFolder) -> Unit,
    rightFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    ResultListFrame(strings.parentalHiddenFolders, Icons.Default.Folder, modifier) {
        if (state.folders.isEmpty()) {
            ResultInfoText(strings.parentalNoHiddenFolders)
        } else {
            LazyColumn(Modifier.fillMaxWidth().height(198.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(state.folders, key = { _, item -> item.stableKey }) { index, folder ->
                    if (index >= state.folders.lastIndex - 4 && state.hasMoreFolders) LaunchedEffect(index) { onLoadMore() }
                    ResultFocusRow(
                        canFocus = canFocus,
                        focusRequester = if (index == 0) firstFocusRequester else null,
                        selected = folder.stableKey == state.selectedFolderKey,
                        onClick = { onFolder(folder) },
                        onRight = { runCatching { rightFocusRequester.requestFocus() } },
                    ) {
                        Icon(Icons.Default.Folder, null, tint = SmartVisionColors.TextSecondary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Column(Modifier.weight(1f)) {
                            Text(folder.name, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(folder.section, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
                        }
                        CountBubble(folder.hiddenCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenItemsList(
    strings: SmartVisionStrings,
    state: ParentalControlUiState,
    firstFocusRequester: FocusRequester,
    canFocus: Boolean,
    onLoadMore: () -> Unit,
    leftFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    ResultListFrame(strings.parentalHiddenItems, Icons.Default.Movie, modifier) {
        if (state.items.isEmpty()) {
            ResultInfoText(strings.parentalNoHiddenItems)
        } else {
            LazyColumn(Modifier.fillMaxWidth().height(198.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(state.items, key = { _, item -> item.stableKey }) { index, item ->
                    if (index >= state.items.lastIndex - 6 && state.hasMoreItems) LaunchedEffect(index) { onLoadMore() }
                    ResultFocusRow(
                        canFocus = canFocus,
                        focusRequester = if (index == 0) firstFocusRequester else null,
                        onLeft = { runCatching { leftFocusRequester.requestFocus() } },
                    ) {
                        if (!item.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(width = 44.dp, height = 34.dp).clip(RoundedCornerShape(5.dp)),
                            )
                        } else {
                            Icon(Icons.Default.Movie, null, tint = SmartVisionColors.TextSecondary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val type = when (item.type) {
                                ParentalHiddenContentType.Channel -> strings.parentalContentChannel
                                ParentalHiddenContentType.Movie -> strings.parentalContentMovie
                                ParentalHiddenContentType.Series -> strings.parentalContentSeries
                                ParentalHiddenContentType.Episode -> strings.parentalContentEpisode
                            }
                            Text(listOf(type, item.secondaryLabel.ifBlank { item.folderName }).filter { it.isNotBlank() }.joinToString(" · "), color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        item.duration?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultListFrame(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .height(240.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0x9E081425))
            .border(1.dp, SmartVisionColors.Border, RoundedCornerShape(7.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun ResultFocusRow(
    canFocus: Boolean,
    focusRequester: FocusRequester?,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused || selected) focusStyle.background else Color.Transparent)
            .border(
                if (focused) focusStyle.borderWidth else if (selected) 1.dp else 0.dp,
                if (focused) focusStyle.accent else if (selected) focusStyle.selectedAccent else Color.Transparent,
                RoundedCornerShape(6.dp),
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties { this.canFocus = canFocus }
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.DirectionLeft && onLeft != null -> { onLeft(); true }
                    event.key == Key.DirectionRight && onRight != null -> { onRight(); true }
                    event.isConfirmationKey() && onClick != null -> { onClick(); true }
                    else -> false
                }
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .focusable()
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun CountPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x41157DE8))
            .border(1.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.72f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = SmartVisionColors.CyanAccent, style = SmartVisionType.Caption)
    }
}

@Composable
private fun CountBubble(value: Int) {
    Box(
        Modifier
            .height(24.dp)
            .widthIn(min = 30.dp)
            .background(Color(0x4A157DE8), RoundedCornerShape(50))
            .border(1.dp, SmartVisionColors.Primary.copy(alpha = 0.55f), RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        Text(value.toString(), color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption)
    }
}

@Composable
private fun ResultInfoText(text: String) {
    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
        Text(text, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
    }
}

@Composable
private fun ParentalMessageLine(strings: SmartVisionStrings, state: ParentalControlUiState) {
    val error = state.keywordError?.let { strings.keywordErrorText(it) }
    val feedback = when (state.feedback) {
        ParentalFeedback.KeywordAdded -> strings.parentalKeywordAdded
        ParentalFeedback.KeywordUpdated -> strings.parentalKeywordUpdated
        ParentalFeedback.KeywordDeleted -> strings.parentalKeywordDeleted
        ParentalFeedback.PinUpdated -> strings.parentalPinUpdated
        null -> null
    }
    val text = error ?: feedback ?: return
    Text(
        text,
        color = if (error != null) Color(0xFFFF7777) else Color(0xFF47DC8A),
        style = SmartVisionType.Caption,
        modifier = Modifier.padding(top = 5.dp),
    )
}

private fun SmartVisionStrings.keywordErrorText(error: ParentalKeywordError): String = when (error) {
    ParentalKeywordError.Empty -> parentalKeywordEmpty
    ParentalKeywordError.Duplicate -> parentalKeywordDuplicate
    ParentalKeywordError.TooLong -> parentalKeywordTooLong
}

@Composable
private fun KeywordEditDialog(
    strings: SmartVisionStrings,
    initialValue: String,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val fieldRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusStyle = LocalTvFocusStyle.current
    LaunchedEffect(Unit) {
        delay(80)
        runCatching { fieldRequester.requestFocus() }
        keyboard?.show()
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF07111F))
                .border(1.dp, SmartVisionColors.Border, RoundedCornerShape(10.dp))
                .padding(20.dp),
        ) {
            Text(strings.edit, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
                cursorBrush = SolidColor(focusStyle.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit(value) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(focusStyle.background, RoundedCornerShape(7.dp))
                    .border(focusStyle.borderWidth, focusStyle.accent, RoundedCornerShape(7.dp))
                    .focusRequester(fieldRequester)
                    .padding(horizontal = 12.dp),
                decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() } },
            )
            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = Color(0xFFFF7777), style = SmartVisionType.Caption)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                TvButton(text = strings.back, onClick = onDismiss, variant = TvButtonVariant.Secondary, modifier = Modifier.height(40.dp))
                TvButton(text = strings.apply, onClick = { onSubmit(value) }, modifier = Modifier.height(40.dp))
            }
        }
    }
}

private fun androidx.compose.ui.input.key.KeyEvent.isConfirmationKey(): Boolean =
    key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter
