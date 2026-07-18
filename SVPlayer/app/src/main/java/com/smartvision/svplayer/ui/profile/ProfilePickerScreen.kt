package com.smartvision.svplayer.ui.profile

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.NumericPinDialog
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val AddKidsFocusKey = "action:add-kids"
private const val AddProfileFocusKey = "action:add-profile"
private const val ProfileCenteringDurationMs = 900
private const val MinimumCenteredLoadingMs = 420L
private const val HomeRevealDurationMs = 620

data class ProfileLoadProgress(
    val message: String,
    val progressPercent: Int? = null,
    val liveItems: Int = 0,
    val liveTotal: Int? = null,
    val movieItems: Int = 0,
    val movieTotal: Int? = null,
    val seriesItems: Int = 0,
    val seriesTotal: Int? = null,
    val startedAtMs: Long = System.currentTimeMillis(),
    val ready: Boolean = false,
    val errorMessage: String? = null,
)

fun profileLoadProgress(
    syncStatus: SyncStatus,
    activationCompleted: Boolean,
    homeReady: Boolean,
    startedAtMs: Long,
    errorMessage: String?,
): ProfileLoadProgress {
    val progress = when (syncStatus) {
        is SyncStatus.Running -> syncStatus.catalogProgress
        is SyncStatus.Success -> syncStatus.catalogProgress
        is SyncStatus.Error -> syncStatus.catalogProgress
        SyncStatus.Idle -> SyncStatus.CatalogProgress()
    }
    val percentages = listOf(progress.live, progress.movies, progress.series)
        .filter { it.phase != SyncStatus.SyncSectionPhase.WAITING }
        .mapNotNull { section -> section.progressPercent ?: section.percent.takeIf { it > 0 } }
    val statusError = (syncStatus as? SyncStatus.Error)?.message
    val resolvedError = errorMessage ?: statusError
    val message = when {
        resolvedError != null -> "Profile loading failed"
        homeReady -> "Ready"
        syncStatus is SyncStatus.Running -> syncStatus.message
        activationCompleted -> "Preparing Home"
        else -> "Activating profile"
    }
    return ProfileLoadProgress(
        message = message,
        progressPercent = percentages.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        liveItems = progress.live.currentItems,
        liveTotal = progress.live.totalItems,
        movieItems = progress.movies.currentItems,
        movieTotal = progress.movies.totalItems,
        seriesItems = progress.series.currentItems,
        seriesTotal = progress.series.totalItems,
        startedAtMs = startedAtMs,
        ready = homeReady,
        errorMessage = resolvedError,
    )
}

@Composable
fun ProfilePickerScreen(
    profiles: List<PlaylistProfile>,
    activeProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onSaveProfile: (PlaylistProfile) -> Unit,
    multiProfileAccess: PremiumFeatureGateResult,
    selectionRequestId: Long?,
    selectionLoadingProfileId: String?,
    homeReadyRequestId: Long?,
    homeProfileAvatarBounds: Rect?,
    loadProgress: ProfileLoadProgress?,
    onSelectionTransitionFinished: (Long, String) -> Unit,
    onRetrySelection: () -> Unit,
    onCancelSelection: () -> Unit,
    onLockedFeature: () -> Unit,
    onVerifyPin: (String) -> Boolean,
) {
    val settings by LocalAppContainer.current.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = PlayerSettings(),
    )
    val strings = smartVisionStrings(settings.language)
    val orderedProfiles = remember(profiles) { orderProfilePickerProfiles(profiles) }
    val adminProfile = orderedProfiles.firstOrNull { it.type == ProfileType.ADMIN }
    val initialProfileId = initialProfilePickerId(orderedProfiles, activeProfileId)
    val orderedProfileIds = remember(orderedProfiles) { orderedProfiles.map { it.id } }
    val profileFocusTargets = remember(orderedProfileIds) {
        orderedProfiles.associate { profile ->
            profile.id to ProfileFocusTarget(FocusRequester(), FocusRequester())
        }
    }
    val addKidsFocus = remember { FocusRequester() }
    val addProfileFocus = remember { FocusRequester() }
    var profileToEdit by remember { mutableStateOf<PlaylistProfile?>(null) }
    var createProfileType by remember { mutableStateOf<ProfileType?>(null) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var protectedAction by remember { mutableStateOf<PickerProtectedAction?>(null) }
    var lastFocusKey by remember { mutableStateOf(initialProfileId.orEmpty()) }
    var restoreFocusRequest by remember { mutableIntStateOf(0) }
    var pickerRootBounds by remember { mutableStateOf<Rect?>(null) }
    var profileCardBounds by remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    var centeredAtMs by remember { mutableStateOf(0L) }
    val centeringProgress = remember { Animatable(0f) }
    val homeRevealProgress = remember { Animatable(0f) }
    val latestTransitionFinished by rememberUpdatedState(onSelectionTransitionFinished)
    val selectionInProgress = selectionLoadingProfileId != null
    val selectedProfile = remember(orderedProfiles, selectionLoadingProfileId) {
        orderedProfiles.firstOrNull { it.id == selectionLoadingProfileId }
    }

    LaunchedEffect(selectionRequestId, selectionLoadingProfileId, homeReadyRequestId) {
        val requestId = selectionRequestId
        val selectedId = selectionLoadingProfileId
        if (requestId == null || selectedId == null) {
            centeringProgress.snapTo(0f)
            homeRevealProgress.snapTo(0f)
            centeredAtMs = 0L
            return@LaunchedEffect
        }
        if (homeRevealProgress.value >= 1f) {
            if (homeReadyRequestId == requestId) {
                latestTransitionFinished(requestId, selectedId)
            }
            return@LaunchedEffect
        }
        val remainingDuration = (
            ProfileCenteringDurationMs * (1f - centeringProgress.value)
            ).roundToInt().coerceAtLeast(1)
        if (centeringProgress.value < 1f) {
            centeringProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remainingDuration,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
        if (centeredAtMs == 0L) {
            centeredAtMs = SystemClock.uptimeMillis()
        }
        if (homeReadyRequestId != requestId) return@LaunchedEffect
        val centeredElapsed = SystemClock.uptimeMillis() - centeredAtMs
        delay((MinimumCenteredLoadingMs - centeredElapsed).coerceAtLeast(0L))
        homeRevealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = HomeRevealDurationMs,
                easing = FastOutSlowInEasing,
            ),
        )
        latestTransitionFinished(requestId, selectedId)
    }

    fun performAction(action: PickerProtectedAction) {
        when (action) {
            is PickerProtectedAction.Select -> onSelectProfile(action.profile.id)
            is PickerProtectedAction.Edit -> {
                profileToEdit = action.profile
                createProfileType = null
                showProfileEditor = true
            }
            is PickerProtectedAction.Add -> {
                profileToEdit = null
                createProfileType = action.type
                showProfileEditor = true
            }
        }
    }

    fun requestAction(action: PickerProtectedAction, pinRequired: Boolean) {
        if (pinRequired) protectedAction = action else performAction(action)
    }

    LaunchedEffect(initialProfileId, orderedProfileIds, restoreFocusRequest, selectionInProgress) {
        if (selectionInProgress) return@LaunchedEffect
        delay(if (restoreFocusRequest == 0) 180 else 90)
        val requested = when {
            lastFocusKey == AddKidsFocusKey -> addKidsFocus
            lastFocusKey == AddProfileFocusKey -> addProfileFocus
            lastFocusKey.startsWith("edit:") -> profileFocusTargets[lastFocusKey.removePrefix("edit:")]?.edit
            else -> profileFocusTargets[lastFocusKey]?.card
                ?: initialProfileId?.let { profileFocusTargets[it]?.card }
                ?: addKidsFocus
        }
        requested?.let { focusRequester -> runCatching { focusRequester.requestFocus() } }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                pickerRootBounds = coordinates.boundsInRoot()
            },
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val cardWidth = (screenWidth * 0.158f).coerceIn(148.dp, 205.dp)
        val cardHeight = (screenHeight * 0.345f).coerceIn(185.dp, 255.dp)
        val avatarSize = (cardWidth * 0.63f).coerceIn(84.dp, 130.dp)
        val itemGap = (screenWidth * 0.016f).coerceIn(15.dp, 28.dp)
        val pickerItemCount = orderedProfiles.size + 2
        val pickerContentWidth = cardWidth * pickerItemCount + itemGap * (pickerItemCount - 1).coerceAtLeast(0)
        val pickerHorizontalPadding = ((screenWidth - pickerContentWidth) * 0.5f)
            .coerceAtLeast(screenWidth * 0.05f)
        val revealAlpha = 1f - homeRevealProgress.value
        val secondaryContentAlpha = if (selectionInProgress) {
            (1f - centeringProgress.value / 0.52f).coerceIn(0f, 1f)
        } else {
            1f
        }

        Image(
            painter = painterResource(R.drawable.startup_neon_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = revealAlpha },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF083E9F).copy(alpha = 0.46f), Color.Transparent),
                        radius = 1040f,
                    ),
                )
                .background(Color(0xFF010511).copy(alpha = 0.46f))
                .graphicsLayer { alpha = revealAlpha },
        )

        Image(
            painter = painterResource(R.drawable.smartvision_logo_wide),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = screenWidth * 0.038f, top = screenHeight * 0.045f)
                .width((screenWidth * 0.15f).coerceIn(180.dp, 285.dp))
                .graphicsLayer { alpha = secondaryContentAlpha * revealAlpha },
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = screenHeight * 0.11f, bottom = screenHeight * 0.07f),
        ) {
            Text(
                text = strings.whoIsWatching,
                color = Color.White,
                fontSize = (screenHeight.value * 0.055f).coerceIn(36f, 58f).sp,
                lineHeight = 62.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = secondaryContentAlpha * revealAlpha },
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = strings.chooseYourProfile,
                color = Color(0xFFB7C4DF),
                fontSize = (screenHeight.value * 0.026f).coerceIn(20f, 28f).sp,
                modifier = Modifier.graphicsLayer { alpha = secondaryContentAlpha * revealAlpha },
            )
            Spacer(Modifier.height((screenHeight * 0.045f).coerceIn(22.dp, 46.dp)))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = pickerHorizontalPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(itemGap),
                verticalAlignment = Alignment.Top,
            ) {
                itemsIndexed(
                    items = orderedProfiles,
                    key = { _, profile -> profile.id },
                ) { index, profile ->
                    val targets = requireNotNull(profileFocusTargets[profile.id])
                    ProfilePickerCard(
                        profile = profile,
                        active = profile.id == activeProfileId,
                        selected = profile.id == selectionLoadingProfileId,
                        enabled = !selectionInProgress,
                        editEnabled = multiProfileAccess.allowed && !selectionInProgress,
                        cardFocusRequester = targets.card,
                        editFocusRequester = targets.edit,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        avatarSize = avatarSize,
                        itemIndex = index,
                        adminBadge = strings.adminBadge,
                        editDescription = strings.editProfile,
                        onCardFocused = { lastFocusKey = profile.id },
                        onEditFocused = { lastFocusKey = "edit:${profile.id}" },
                        contentAlpha = when {
                            !selectionInProgress -> 1f
                            profile.id == selectionLoadingProfileId &&
                                profileCardBounds[profile.id] != null -> 0f
                            else -> secondaryContentAlpha
                        },
                        onCardBoundsChanged = { bounds ->
                            if (profileCardBounds[profile.id] != bounds) {
                                profileCardBounds = profileCardBounds + (profile.id to bounds)
                            }
                        },
                        onClick = {
                            requestAction(PickerProtectedAction.Select(profile), profile.isLocked)
                        },
                        onEdit = {
                            if (multiProfileAccess.allowed) {
                                requestAction(PickerProtectedAction.Edit(profile), adminProfile?.isLocked == true)
                            } else {
                                onLockedFeature()
                            }
                        },
                    )
                }

                item(key = AddKidsFocusKey) {
                    AddProfileCard(
                        focusRequester = addKidsFocus,
                        enabled = !selectionInProgress,
                        locked = !multiProfileAccess.allowed || adminProfile?.isLocked == true,
                        label = strings.addKidsProfile,
                        avatarDrawableResId = R.drawable.avatar_action_add_kids,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        avatarSize = avatarSize,
                        itemIndex = orderedProfiles.size,
                        onFocused = { lastFocusKey = AddKidsFocusKey },
                        contentAlpha = secondaryContentAlpha * revealAlpha,
                        onClick = {
                            if (multiProfileAccess.allowed) {
                                requestAction(PickerProtectedAction.Add(ProfileType.KIDS), adminProfile?.isLocked == true)
                            } else {
                                onLockedFeature()
                            }
                        },
                    )
                }
                item(key = AddProfileFocusKey) {
                    AddProfileCard(
                        focusRequester = addProfileFocus,
                        enabled = !selectionInProgress,
                        locked = !multiProfileAccess.allowed || adminProfile?.isLocked == true,
                        label = strings.addProfile,
                        avatarDrawableResId = R.drawable.avatar_action_add_profile,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        avatarSize = avatarSize,
                        itemIndex = orderedProfiles.size + 1,
                        onFocused = { lastFocusKey = AddProfileFocusKey },
                        contentAlpha = secondaryContentAlpha * revealAlpha,
                        onClick = {
                            if (multiProfileAccess.allowed) {
                                requestAction(PickerProtectedAction.Add(ProfileType.NORMAL), adminProfile?.isLocked == true)
                            } else {
                                onLockedFeature()
                            }
                        },
                    )
                }
            }
        }

        val transitionProfile = selectedProfile
        val transitionStartBounds = selectionLoadingProfileId?.let(profileCardBounds::get)
        val rootBounds = pickerRootBounds
        if (transitionProfile != null && transitionStartBounds != null && rootBounds != null) {
            SelectedProfileTransition(
                profile = transitionProfile,
                startBounds = transitionStartBounds,
                rootBounds = rootBounds,
                homeAvatarBounds = homeProfileAvatarBounds,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                avatarSize = avatarSize,
                centeringProgress = centeringProgress.value,
                homeRevealProgress = homeRevealProgress.value,
                loadProgress = loadProgress,
                french = settings.language.equals("French", ignoreCase = true),
                onRetry = onRetrySelection,
                onCancel = onCancelSelection,
            )
        }
    }

    if (showProfileEditor) {
        PlaylistProfileEditorDialog(
            strings = strings,
            initial = profileToEdit,
            createType = createProfileType,
            adminProfile = adminProfile,
            existingNames = profiles.filterNot { it.id == profileToEdit?.id }.map { it.name },
            onDismiss = {
                showProfileEditor = false
                profileToEdit = null
                restoreFocusRequest++
            },
            onSave = { profile ->
                showProfileEditor = false
                profileToEdit = null
                onSaveProfile(profile)
                restoreFocusRequest++
            },
        )
    }

    protectedAction?.let { action ->
        NumericPinDialog(
            title = strings.enterPin,
            strings = strings,
            onDismiss = {
                protectedAction = null
                restoreFocusRequest++
            },
            onSubmit = { pin ->
                if (onVerifyPin(pin)) {
                    protectedAction = null
                    performAction(action)
                    true
                } else {
                    false
                }
            },
        )
    }
}

private data class ProfileFocusTarget(
    val card: FocusRequester,
    val edit: FocusRequester,
)

private sealed interface PickerProtectedAction {
    data class Select(val profile: PlaylistProfile) : PickerProtectedAction
    data class Edit(val profile: PlaylistProfile) : PickerProtectedAction
    data class Add(val type: ProfileType) : PickerProtectedAction
}

@Composable
private fun ProfilePickerCard(
    profile: PlaylistProfile,
    active: Boolean,
    selected: Boolean,
    enabled: Boolean,
    editEnabled: Boolean,
    cardFocusRequester: FocusRequester,
    editFocusRequester: FocusRequester,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    avatarSize: androidx.compose.ui.unit.Dp,
    itemIndex: Int,
    adminBadge: String,
    editDescription: String,
    onCardFocused: () -> Unit,
    onEditFocused: () -> Unit,
    contentAlpha: Float,
    onCardBoundsChanged: (Rect) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            selected -> 1.07f
            focused -> 1.05f
            else -> 1f
        },
        animationSpec = tween(if (selected) 150 else 150),
        label = "profile-card-scale",
    )
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((itemIndex.coerceAtMost(7) * 40L))
        reveal.animateTo(1f, tween(220))
    }
    val shape = RoundedCornerShape(20.dp)
    val borderColor = when {
        selected -> SmartVisionColors.CyanAccent
        focused -> Color(0xFF61A8FF)
        active -> Color(0xFF20D4C7)
        else -> Color(0xFF53698D).copy(alpha = 0.72f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(cardWidth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = reveal.value * contentAlpha
                translationY = (1f - reveal.value) * 12.dp.toPx()
            },
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .onGloballyPositioned { coordinates ->
                    onCardBoundsChanged(coordinates.boundsInRoot())
                }
                .shadow(
                    if (focused || selected) 25.dp else 3.dp,
                    shape,
                    ambientColor = SmartVisionColors.CardFocusGlow,
                    spotColor = SmartVisionColors.CardFocusGlow,
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF14274B).copy(alpha = if (focused) 0.98f else 0.86f), Color(0xFF071329).copy(alpha = 0.97f)),
                    ),
                )
                .border(BorderStroke(if (focused || selected) 2.5.dp else 1.5.dp, borderColor), shape)
                .focusRequester(cardFocusRequester)
                .then(if (editEnabled) Modifier.focusProperties { down = editFocusRequester } else Modifier)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onCardFocused()
                }
                .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
                .focusable(enabled = enabled, interactionSource = interactionSource)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    ProfilePickerAvatar(
                        profile = profile,
                        modifier = Modifier
                            .size(avatarSize)
                            .graphicsLayer { clip = true; this.shape = CircleShape },
                    )
                    if (profile.isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.TopEnd).size(21.dp),
                        )
                    }
                    if (selected) {
                        CircularProgressIndicator(
                            color = SmartVisionColors.CyanAccent,
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier.size(avatarSize),
                        )
                    }
                }
               /*  if (profile.type == ProfileType.ADMIN) {
                    Text(
                        text = adminBadge,
                        color = Color(0xFF61C8FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF021229).copy(alpha = 0.82f), RoundedCornerShape(50))
                            .border(1.dp, Color(0xFF1678D7), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    Spacer(Modifier.height(5.dp))
                } */
                ProfileName(profile.name, focused)
            }
        }
        Spacer(Modifier.height(8.dp))
        PickerEditButton(
            focusRequester = editFocusRequester,
            cardFocusRequester = cardFocusRequester,
            enabled = editEnabled,
            contentDescription = editDescription,
            onFocused = onEditFocused,
            onClick = onEdit,
            modifier = Modifier,
        )
    }
}

@Composable
private fun SelectedProfileTransition(
    profile: PlaylistProfile,
    startBounds: Rect,
    rootBounds: Rect,
    homeAvatarBounds: Rect?,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    avatarSize: androidx.compose.ui.unit.Dp,
    centeringProgress: Float,
    homeRevealProgress: Float,
    loadProgress: ProfileLoadProgress?,
    french: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val density = LocalDensity.current
    val cardWidthPx = with(density) { cardWidth.toPx() }
    val cardHeightPx = with(density) { cardHeight.toPx() }
    val avatarSizePx = with(density) { avatarSize.toPx() }
    val cardPaddingPx = with(density) { 14.dp.toPx() }
    val profileNameHeightPx = with(density) { 54.dp.toPx() }
    val startLeft = startBounds.left - rootBounds.left
    val startTop = startBounds.top - rootBounds.top
    val centeredLeft = (rootBounds.width - cardWidthPx) / 2f
    val centeredTop = (rootBounds.height - cardHeightPx) / 2f
    val cardLeft = transitionLerp(startLeft, centeredLeft, centeringProgress)
    val cardTop = transitionLerp(startTop, centeredTop, centeringProgress)
    val cardScale = transitionLerp(1f, 1.4f, centeringProgress)
    val shellAlpha = (1f - homeRevealProgress / 0.34f).coerceIn(0f, 1f)
    val cardAvatarVisible = homeRevealProgress <= 0.001f
    val loadingAlpha = (
        (centeringProgress - 0.64f) / 0.24f
        ).coerceIn(0f, 1f) * (1f - homeRevealProgress / 0.30f).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(20.dp)
    var elapsedSeconds by remember(loadProgress?.startedAtMs) { mutableIntStateOf(0) }
    LaunchedEffect(loadProgress?.startedAtMs, loadProgress?.ready, loadProgress?.errorMessage) {
        val startedAt = loadProgress?.startedAtMs ?: return@LaunchedEffect
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - startedAt).coerceAtLeast(0L) / 1_000L).toInt()
            if (loadProgress.ready || loadProgress.errorMessage != null) break
            delay(1_000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(20f),
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = cardLeft.roundToInt(),
                        y = cardTop.roundToInt(),
                    )
                }
                .width(cardWidth)
                .height(cardHeight)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = shellAlpha }
                    .shadow(
                        elevation = 30.dp,
                        shape = shape,
                        ambientColor = SmartVisionColors.CardFocusGlow,
                        spotColor = SmartVisionColors.CardFocusGlow,
                    )
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF16305D).copy(alpha = 0.98f),
                                Color(0xFF071329),
                            ),
                        ),
                    )
                    .border(
                        BorderStroke(2.5.dp, SmartVisionColors.CyanAccent),
                        shape,
                    ),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cardAvatarVisible) {
                        ProfilePickerAvatar(
                            profile = profile,
                            modifier = Modifier.size(avatarSize),
                        )
                    }
                    if (loadingAlpha > 0f && cardAvatarVisible) {
                        CircularProgressIndicator(
                            color = SmartVisionColors.CyanAccent,
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .size(avatarSize)
                                .graphicsLayer { alpha = loadingAlpha },
                        )
                    }
                }
                Box(
                    modifier = Modifier.graphicsLayer { alpha = shellAlpha },
                ) {
                    ProfileName(profile.name, focused = true)
                }
            }
        }

        if (homeRevealProgress > 0f) {
            val availableAvatarHeightPx =
                cardHeightPx - cardPaddingPx * 2f - profileNameHeightPx
            val avatarLocalCenterY = cardPaddingPx + availableAvatarHeightPx / 2f
            val centeredCardCenterX = rootBounds.width / 2f
            val centeredCardCenterY = rootBounds.height / 2f
            val flyingStartCenterX = centeredCardCenterX
            val flyingStartCenterY = centeredCardCenterY +
                (avatarLocalCenterY - cardHeightPx / 2f) * 1.4f
            val fallbackTargetSizePx = with(density) { 34.dp.toPx() }
            val targetBounds = homeAvatarBounds?.let { bounds ->
                Rect(
                    left = bounds.left - rootBounds.left,
                    top = bounds.top - rootBounds.top,
                    right = bounds.right - rootBounds.left,
                    bottom = bounds.bottom - rootBounds.top,
                )
            }
            val targetCenterX = targetBounds?.center?.x ?: rootBounds.width * 0.82f
            val targetCenterY = targetBounds?.center?.y ?: rootBounds.height * 0.065f
            val targetSizePx = targetBounds
                ?.let { minOf(it.width, it.height) - with(density) { 4.dp.toPx() } }
                ?.coerceAtLeast(fallbackTargetSizePx)
                ?: fallbackTargetSizePx
            val flyingStartSizePx = avatarSizePx * 1.4f
            val pathProgress = homeRevealProgress.coerceIn(0f, 1f)
            val arcLiftPx = sin(pathProgress * PI).toFloat() * rootBounds.height * 0.055f
            val flyingCenterX = transitionLerp(
                flyingStartCenterX,
                targetCenterX,
                pathProgress,
            )
            val flyingCenterY = transitionLerp(
                flyingStartCenterY,
                targetCenterY,
                pathProgress,
            ) - arcLiftPx
            val flyingSizePx = transitionLerp(
                flyingStartSizePx,
                targetSizePx,
                pathProgress,
            )
            val flyingSize = with(density) { flyingSizePx.toDp() }
            val flyingLoadingAlpha =
                (1f - pathProgress / 0.38f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (flyingCenterX - flyingSizePx / 2f).roundToInt(),
                            y = (flyingCenterY - flyingSizePx / 2f).roundToInt(),
                        )
                    }
                    .size(flyingSize)
                    .shadow(
                        elevation = transitionLerp(24f, 0f, pathProgress).dp,
                        shape = CircleShape,
                        ambientColor = SmartVisionColors.CardFocusGlow,
                        spotColor = SmartVisionColors.CardFocusGlow,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ProfilePickerAvatar(
                    profile = profile,
                    modifier = Modifier.fillMaxSize(),
                )
                if (flyingLoadingAlpha > 0f) {
                    CircularProgressIndicator(
                        color = SmartVisionColors.CyanAccent,
                        strokeWidth = 4.dp,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = flyingLoadingAlpha },
                    )
                }
            }
        }

        if (loadProgress != null && homeRevealProgress <= 0.001f) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = cardHeight * 0.92f)
                    .width(560.dp)
                    .background(Color(0xE6121B2D), RoundedCornerShape(14.dp))
                    .border(1.dp, SmartVisionColors.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = when {
                        loadProgress.errorMessage != null && french -> "Echec du chargement du profil"
                        loadProgress.errorMessage != null -> loadProgress.message
                        loadProgress.ready && french -> "Pret"
                        else -> loadProgress.message
                    },
                    color = if (loadProgress.errorMessage == null) Color.White else SmartVisionColors.Error,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(7.dp))
                if (loadProgress.progressPercent == null && !loadProgress.ready && loadProgress.errorMessage == null) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = SmartVisionColors.CyanAccent,
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { (loadProgress.progressPercent ?: 100).coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (loadProgress.errorMessage == null) SmartVisionColors.CyanAccent else SmartVisionColors.Error,
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = listOf(
                        "Live ${loadProgress.liveItems.withTotal(loadProgress.liveTotal)}",
                        "Movies ${loadProgress.movieItems.withTotal(loadProgress.movieTotal)}",
                        "Series ${loadProgress.seriesItems.withTotal(loadProgress.seriesTotal)}",
                        "${elapsedSeconds}s",
                    ).joinToString(" | "),
                    color = Color(0xFFB7C4DF),
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                if (loadProgress.errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = loadProgress.errorMessage,
                        color = Color(0xFFFFB4AB),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvButton(
                            text = if (french) "Reessayer" else "Retry",
                            onClick = onRetry,
                            variant = TvButtonVariant.Primary,
                            modifier = Modifier.height(42.dp),
                        )
                        TvButton(
                            text = if (french) "Retour" else "Back",
                            onClick = onCancel,
                            variant = TvButtonVariant.Secondary,
                            modifier = Modifier.height(42.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun transitionLerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)

private fun Int.withTotal(total: Int?): String =
    total?.takeIf { it > 0 }?.let { "$this/$it" } ?: toString()

@Composable
private fun ProfileName(name: String, focused: Boolean) {
    val fontSize = when {
        name.length > 25 -> 12.sp
        name.length > 19 -> 14.sp
        name.length > 14 -> 16.sp
        else -> 18.sp
    }
    Text(
        text = name,
        color = if (focused) Color.White else Color(0xFFE8EEFA),
        fontSize = fontSize,
        lineHeight = (fontSize.value + 3f).sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Clip,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
    )
}

@Composable
private fun PickerEditButton(
    focusRequester: FocusRequester,
    cardFocusRequester: FocusRequester,
    enabled: Boolean,
    contentDescription: String,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (focused) Color(0xFF0A7BEA) else Color(0xFF020B1D).copy(alpha = 0.84f))
            .border(1.dp, if (focused) Color.White else Color.White.copy(alpha = 0.25f), CircleShape)
            .focusRequester(focusRequester)
            .focusProperties { up = cardFocusRequester }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.32f),
            modifier = Modifier.size(18.dp),
        )

    }
}

@Composable
private fun AddProfileCard(
    focusRequester: FocusRequester,
    enabled: Boolean,
    locked: Boolean,
    label: String,
    avatarDrawableResId: Int,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    avatarSize: androidx.compose.ui.unit.Dp,
    itemIndex: Int,
    onFocused: () -> Unit,
    contentAlpha: Float,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(150), label = "add-profile-scale")
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((itemIndex.coerceAtMost(7) * 40L))
        reveal.animateTo(1f, tween(220))
    }
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = reveal.value * contentAlpha
                translationY = (1f - reveal.value) * 12.dp.toPx()
            }
            .shadow(
                if (focused) 25.dp else 3.dp,
                shape,
                ambientColor = SmartVisionColors.CardFocusGlow,
                spotColor = SmartVisionColors.CardFocusGlow,
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF14233F).copy(alpha = 0.87f), Color(0xFF071329).copy(alpha = 0.97f))))
            .border(BorderStroke(if (focused) 2.5.dp else 1.5.dp, if (focused) Color(0xFF61A8FF) else Color(0xFF53698D).copy(alpha = 0.72f)), shape)
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(avatarDrawableResId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(avatarSize),
                )
                if (locked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.TopEnd).size(21.dp),
                    )
                }
            }
            ProfileName(label, focused)
        }
    }
}
