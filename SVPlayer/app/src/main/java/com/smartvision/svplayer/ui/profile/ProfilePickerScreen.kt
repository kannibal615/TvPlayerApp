package com.smartvision.svplayer.ui.profile

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private const val AddKidsFocusKey = "action:add-kids"
private const val AddProfileFocusKey = "action:add-profile"

@Composable
fun ProfilePickerScreen(
    profiles: List<PlaylistProfile>,
    activeProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onSaveProfile: (PlaylistProfile) -> Unit,
    multiProfileAccess: PremiumFeatureGateResult,
    selectionLoadingProfileId: String?,
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
    val selectionInProgress = selectionLoadingProfileId != null

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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

        Image(
            painter = painterResource(R.drawable.startup_neon_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
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
                .background(Color(0xFF010511).copy(alpha = 0.46f)),
        )

        Image(
            painter = painterResource(R.drawable.smartvision_logo_wide),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = screenWidth * 0.038f, top = screenHeight * 0.045f)
                .width((screenWidth * 0.15f).coerceIn(180.dp, 285.dp)),
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
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = strings.chooseYourProfile,
                color = Color(0xFFB7C4DF),
                fontSize = (screenHeight.value * 0.026f).coerceIn(20f, 28f).sp,
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
                        kids = true,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        avatarSize = avatarSize,
                        itemIndex = orderedProfiles.size,
                        onFocused = { lastFocusKey = AddKidsFocusKey },
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
                        kids = false,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        avatarSize = avatarSize,
                        itemIndex = orderedProfiles.size + 1,
                        onFocused = { lastFocusKey = AddProfileFocusKey },
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
    }

    if (showProfileEditor) {
        PlaylistProfileEditorDialog(
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
                alpha = reveal.value
                translationY = (1f - reveal.value) * 12.dp.toPx()
            },
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .shadow(if (focused || selected) 24.dp else 5.dp, shape, ambientColor = borderColor, spotColor = borderColor)
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
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(54.dp),
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
private fun ProfileName(name: String, focused: Boolean) {
    val fontSize = when {
        name.length > 42 -> 13.sp
        name.length > 28 -> 15.sp
        name.length > 18 -> 17.sp
        else -> 20.sp
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
    kids: Boolean,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    avatarSize: androidx.compose.ui.unit.Dp,
    itemIndex: Int,
    onFocused: () -> Unit,
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
                alpha = reveal.value
                translationY = (1f - reveal.value) * 12.dp.toPx()
            }
            .shadow(if (focused) 24.dp else 5.dp, shape, ambientColor = Color(0xFF168BFF), spotColor = Color(0xFF168BFF))
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
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(Color(0xFF0A2E70).copy(alpha = 0.42f))
                    .border(1.dp, Color(0xFF1F70DD), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (kids) {
                    Image(
                        painter = painterResource(R.drawable.kids_home_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color(0xFF19D8FF),
                        modifier = Modifier.size(50.dp),
                    )
                }
                if (locked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(24.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                color = if (focused) Color.White else Color(0xFFE8EEFA),
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            )
        }
    }
}
