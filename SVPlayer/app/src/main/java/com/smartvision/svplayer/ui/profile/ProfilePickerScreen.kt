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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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
import com.smartvision.svplayer.ui.components.NumericPinDialog
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val AddKidsFocusKey = "action:add-kids"
private const val AddProfileFocusKey = "action:add-profile"
private const val ProfileCenteringDurationMs = 900
private const val MinimumCenteredLoadingMs = 420L
private const val HomeRevealDurationMs = 620
private val ProfileCardContentPadding = 14.dp
private val ProfileAvatarNameSpacing = 12.dp

@Composable
fun ProfilePickerScreen(
    profiles: List<PlaylistProfile>,
    activeProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onSaveProfile: (PlaylistProfile) -> Unit,
    multiProfileAccess: PremiumFeatureGateResult,
    selectionRequestId: Long?,
    selectionLoadingProfileId: String?,
    activationReadyRequestId: Long?,
    homeProfileAvatarBounds: Rect?,
    onSelectionTransitionFinished: (Long, String) -> Unit,
    onLockedFeature: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onFirstFrameReady: () -> Unit = {},
    initialContentVisible: Boolean = true,
) {
    val settings by LocalAppContainer.current.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = PlayerSettings(),
    )
    val strings = smartVisionStrings(settings.language)
    val orderedProfiles = remember(profiles) { orderProfilePickerProfiles(profiles) }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
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
    val pickerListState = rememberLazyListState()
    val pickerFocusScope = rememberCoroutineScope()
    var profileToEdit by remember { mutableStateOf<PlaylistProfile?>(null) }
    var createProfileType by remember { mutableStateOf<ProfileType?>(null) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var protectedAction by remember { mutableStateOf<PickerProtectedAction?>(null) }
    var lastFocusKey by remember { mutableStateOf(initialProfileId.orEmpty()) }
    var restoreFocusRequest by remember { mutableIntStateOf(0) }
    var pickerRootBounds by remember { mutableStateOf<Rect?>(null) }
    var profileCardBounds by remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    var centeredAtMs by remember { mutableStateOf(0L) }
    var firstFrameReported by remember { mutableStateOf(false) }
    val centeringProgress = remember { Animatable(0f) }
    val homeRevealProgress = remember { Animatable(0f) }
    val latestTransitionFinished by rememberUpdatedState(onSelectionTransitionFinished)
    val latestFirstFrameReady by rememberUpdatedState(onFirstFrameReady)
    val selectionInProgress = selectionLoadingProfileId != null
    val selectedProfile = remember(orderedProfiles, selectionLoadingProfileId) {
        orderedProfiles.firstOrNull { it.id == selectionLoadingProfileId }
    }
    val firstProfileId = orderedProfiles.firstOrNull()?.id
    val addProfileIndex = orderedProfiles.size + 1

    fun requestPickerFocus(index: Int, focusKey: String, requester: FocusRequester) {
        lastFocusKey = focusKey
        pickerFocusScope.launch {
            pickerListState.scrollToItem(index)
            delay(16)
            runCatching { requester.requestFocus() }
        }
    }

    fun wrapFocusToFirstProfile(): Boolean {
        val targetId = firstProfileId ?: return false
        val target = profileFocusTargets[targetId]?.card ?: return false
        requestPickerFocus(index = 0, focusKey = targetId, requester = target)
        return true
    }

    fun wrapFocusToAddProfile(): Boolean {
        requestPickerFocus(index = addProfileIndex, focusKey = AddProfileFocusKey, requester = addProfileFocus)
        return true
    }

    LaunchedEffect(selectionRequestId, selectionLoadingProfileId, activationReadyRequestId) {
        val requestId = selectionRequestId
        val selectedId = selectionLoadingProfileId
        if (requestId == null || selectedId == null) {
            centeringProgress.snapTo(0f)
            homeRevealProgress.snapTo(0f)
            centeredAtMs = 0L
            return@LaunchedEffect
        }
        if (homeRevealProgress.value >= 1f) {
            if (activationReadyRequestId == requestId) {
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
        if (activationReadyRequestId != requestId) return@LaunchedEffect
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
                if (!firstFrameReported && coordinates.size.width > 0 && coordinates.size.height > 0) {
                    firstFrameReported = true
                    latestFirstFrameReady()
                }
            },
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val cardWidth = (screenWidth * 0.158f).coerceIn(148.dp, 205.dp)
        val avatarSize = (cardWidth * 0.63f).coerceIn(84.dp, 130.dp)
        val profileLabels = remember(orderedProfiles, strings.addKidsProfile, strings.addProfile) {
            orderedProfiles.map(PlaylistProfile::name) + listOf(strings.addKidsProfile, strings.addProfile)
        }
        val nameWidthPx = with(density) {
            (cardWidth - ProfileCardContentPadding * 2).roundToPx()
        }
        val profileNameHeight = remember(profileLabels, nameWidthPx, density.fontScale) {
            profileLabels.maxOfOrNull { label ->
                with(density) {
                    textMeasurer.measure(
                        text = AnnotatedString(label),
                        style = profileNameTextStyle(label),
                        overflow = TextOverflow.Clip,
                        maxLines = 3,
                        constraints = Constraints(maxWidth = nameWidthPx),
                    ).size.height.toDp()
                }
            } ?: 21.dp
        }
        val cardHeight = ProfileCardContentPadding * 2 +
            avatarSize + ProfileAvatarNameSpacing + profileNameHeight
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
                .graphicsLayer { alpha = secondaryContentAlpha * revealAlpha * if (initialContentVisible) 1f else 0f },
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
                modifier = Modifier.graphicsLayer { alpha = secondaryContentAlpha * revealAlpha * if (initialContentVisible) 1f else 0f },
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = strings.chooseYourProfile,
                color = Color(0xFFB7C4DF),
                fontSize = (screenHeight.value * 0.026f).coerceIn(20f, 28f).sp,
                modifier = Modifier.graphicsLayer { alpha = secondaryContentAlpha * revealAlpha * if (initialContentVisible) 1f else 0f },
            )
            Spacer(Modifier.height((screenHeight * 0.045f).coerceIn(22.dp, 46.dp)))

            LazyRow(
                state = pickerListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || selectionInProgress) {
                            false
                        } else {
                            when {
                                event.key == Key.DirectionRight && lastFocusKey == AddProfileFocusKey ->
                                    wrapFocusToFirstProfile()
                                event.key == Key.DirectionLeft && lastFocusKey == firstProfileId ->
                                    wrapFocusToAddProfile()
                                else -> false
                            }
                        }
                    },
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
                        profileNameHeight = profileNameHeight,
                        itemIndex = index,
                        revealEnabled = initialContentVisible,
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
                            if (
                                profile.id == selectionLoadingProfileId &&
                                profileCardBounds[profile.id] != bounds
                            ) {
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
                        profileNameHeight = profileNameHeight,
                        itemIndex = orderedProfiles.size,
                        revealEnabled = initialContentVisible,
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
                        profileNameHeight = profileNameHeight,
                        itemIndex = orderedProfiles.size + 1,
                        revealEnabled = initialContentVisible,
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
                profileNameHeight = profileNameHeight,
                centeringProgress = centeringProgress.value,
                homeRevealProgress = homeRevealProgress.value,
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
    profileNameHeight: androidx.compose.ui.unit.Dp,
    itemIndex: Int,
    revealEnabled: Boolean,
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
            selected -> 1.06f
            else -> 1f
        },
        animationSpec = tween(90),
        label = "profile-card-scale",
    )
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(revealEnabled) {
        if (!revealEnabled) {
            reveal.snapTo(0f)
        } else {
            delay((itemIndex.coerceAtMost(7) * 40L))
            reveal.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
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
                    if (focused || selected) 12.dp else 3.dp,
                    shape,
                    ambientColor = SmartVisionColors.CardFocusGlow,
                    spotColor = SmartVisionColors.CardFocusGlow,
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SmartVisionColors.ProfileCardSurfaceTop,
                            SmartVisionColors.ProfileCardSurfaceBottom,
                        ),
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
                .padding(ProfileCardContentPadding),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(avatarSize),
                    contentAlignment = Alignment.Center,
                ) {
                    ProfilePickerAvatar(
                        profile = profile,
                        modifier = Modifier
                            .size(avatarSize)
                            .graphicsLayer { clip = true; this.shape = ProfileAvatarShape },
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
                        ProfileAvatarLoadingIndicator(
                            color = SmartVisionColors.CyanAccent,
                            strokeWidth = 2.25.dp,
                            modifier = Modifier.size(avatarSize),
                        )
                    }
                }
                Spacer(Modifier.height(ProfileAvatarNameSpacing))
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
                ProfileName(profile.name, focused, profileNameHeight)
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
    profileNameHeight: androidx.compose.ui.unit.Dp,
    centeringProgress: Float,
    homeRevealProgress: Float,
) {
    val density = LocalDensity.current
    val cardWidthPx = with(density) { cardWidth.toPx() }
    val cardHeightPx = with(density) { cardHeight.toPx() }
    val avatarSizePx = with(density) { avatarSize.toPx() }
    val cardPaddingPx = with(density) { ProfileCardContentPadding.toPx() }
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
                                SmartVisionColors.ProfileCardSurfaceTop,
                                SmartVisionColors.ProfileCardSurfaceBottom,
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
                    .padding(ProfileCardContentPadding),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(avatarSize),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cardAvatarVisible) {
                        ProfilePickerAvatar(
                            profile = profile,
                            modifier = Modifier.size(avatarSize),
                        )
                    }
                    if (loadingAlpha > 0f && cardAvatarVisible) {
                        ProfileAvatarLoadingIndicator(
                            color = SmartVisionColors.CyanAccent,
                            strokeWidth = 2.25.dp,
                            modifier = Modifier
                                .size(avatarSize)
                                .graphicsLayer { alpha = loadingAlpha },
                        )
                    }
                }
                Spacer(Modifier.height(ProfileAvatarNameSpacing))
                Box(
                    modifier = Modifier
                        .height(profileNameHeight)
                        .graphicsLayer { alpha = shellAlpha },
                ) {
                    ProfileName(profile.name, focused = true, profileNameHeight = profileNameHeight)
                }
            }
        }

        if (homeRevealProgress > 0f) {
            val avatarLocalCenterY = cardPaddingPx + avatarSizePx / 2f
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
                        shape = ProfileAvatarShape,
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
                    ProfileAvatarLoadingIndicator(
                        color = SmartVisionColors.CyanAccent,
                        strokeWidth = 2.25.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = flyingLoadingAlpha },
                    )
                }
            }
        }

    }
}

private fun transitionLerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)

@Composable
private fun ProfileName(
    name: String,
    focused: Boolean,
    profileNameHeight: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(profileNameHeight),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = name,
            color = if (focused) Color.White else Color(0xFFE8EEFA),
            style = profileNameTextStyle(name),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Clip,
        )
    }
}

private fun profileNameTextStyle(name: String): TextStyle {
    val fontSize = when {
        name.length > 25 -> 12.sp
        name.length > 19 -> 14.sp
        name.length > 14 -> 16.sp
        else -> 18.sp
    }
    return TextStyle(
        fontSize = fontSize,
        lineHeight = (fontSize.value + 3f).sp,
        fontWeight = FontWeight.SemiBold,
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
    profileNameHeight: androidx.compose.ui.unit.Dp,
    itemIndex: Int,
    revealEnabled: Boolean,
    onFocused: () -> Unit,
    contentAlpha: Float,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(revealEnabled) {
        if (!revealEnabled) {
            reveal.snapTo(0f)
        } else {
            delay((itemIndex.coerceAtMost(7) * 40L))
            reveal.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
    }
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .graphicsLayer {
                alpha = reveal.value * contentAlpha
                translationY = (1f - reveal.value) * 12.dp.toPx()
            }
            .shadow(
                if (focused) 12.dp else 3.dp,
                shape,
                ambientColor = SmartVisionColors.CardFocusGlow,
                spotColor = SmartVisionColors.CardFocusGlow,
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        SmartVisionColors.ProfileCardSurfaceTop,
                        SmartVisionColors.ProfileCardSurfaceBottom,
                    ),
                ),
            )
            .border(BorderStroke(if (focused) 2.5.dp else 1.5.dp, if (focused) Color(0xFF61A8FF) else Color(0xFF53698D).copy(alpha = 0.72f)), shape)
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(ProfileCardContentPadding),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(avatarSize),
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
            Spacer(Modifier.height(ProfileAvatarNameSpacing))
            ProfileName(label, focused, profileNameHeight)
        }
    }
}
