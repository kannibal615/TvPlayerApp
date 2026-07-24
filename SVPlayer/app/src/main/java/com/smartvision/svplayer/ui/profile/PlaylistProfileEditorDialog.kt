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
fun PlaylistProfileEditorDialog(
    strings: SmartVisionStrings,
    initial: PlaylistProfile?,
    createType: ProfileType? = null,
    adminProfile: PlaylistProfile? = null,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (PlaylistProfile) -> Unit,
) {
    val container = LocalAppContainer.current
    val validationScope = rememberCoroutineScope()
    val profileType = initial?.type ?: createType ?: ProfileType.NORMAL
    var credentialsMode by remember(initial?.id, profileType) {
        mutableStateOf(
            if (profileType == ProfileType.ADMIN) CredentialsMode.CUSTOM
            else initial?.credentialsMode ?: if (adminProfile != null) CredentialsMode.SHARED_WITH_ADMIN else CredentialsMode.CUSTOM,
        )
    }
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var avatarId by remember(initial?.id) {
        mutableStateOf(
            initial?.avatarId
                ?.takeIf { it.isNotBlank() }
                ?.let { canonicalProfileAvatarId(it, profileType) }
                ?: defaultProfileAvatarId(profileType, initial?.id?.takeIf { it.isNotBlank() }),
        )
    }
    var source by remember(initial?.id) {
        mutableStateOf(initial?.source ?: adminProfile?.source ?: PlaylistSource.Xtream)
    }
    var host by remember(initial?.id) { mutableStateOf(initial?.xtreamHost ?: "") }
    var username by remember(initial?.id) { mutableStateOf(initial?.xtreamUsername ?: "") }
    var password by remember(initial?.id) { mutableStateOf(initial?.xtreamPassword ?: "") }
    var m3uUrl by remember(initial?.id) { mutableStateOf(initial?.m3uUrl ?: "") }
    var epgUrl by remember(initial?.id) { mutableStateOf(initial?.epgUrl ?: "") }
    var selectedContentPrefixes by remember(initial?.id) {
        mutableStateOf(ContentPrefixPolicy.normalize(initial?.selectedContentPrefixes.orEmpty()))
    }
    var manualContentPrefix by remember(initial?.id) { mutableStateOf("") }
    var manualContentPrefixError by remember(initial?.id) { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var validatingCredentials by remember { mutableStateOf(false) }
    var expandedSource by remember(initial?.id, profileType) { mutableStateOf<PlaylistSource?>(null) }
    val nameFocusRequester = remember { FocusRequester() }
    val contentPrefixFocusRequester = remember { FocusRequester() }
    val manualPrefixFocusRequester = remember { FocusRequester() }
    val manualPrefixAddFocusRequester = remember { FocusRequester() }
    val firstSourceFocusRequester = remember { FocusRequester() }
    val hostFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val m3uFocusRequester = remember { FocusRequester() }
    val epgFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }

/*     val configuration = LocalConfiguration.current
    val dialogWidth = (configuration.screenWidthDp.dp - 48.dp).coerceAtMost(820.dp)
    val requestedDialogHeight = when {
        expandedSource != null -> 610.dp
        profileType != ProfileType.ADMIN && adminProfile != null -> 500.dp
        else -> 430.dp
    }
    val dialogHeight = requestedDialogHeight.coerceAtMost(configuration.screenHeightDp.dp - 32.dp) */


val configuration = LocalConfiguration.current
val dialogWidth = (configuration.screenWidthDp.dp * 0.50f)
    .coerceIn(520.dp, 680.dp)
val requestedDialogHeight = when {
    expandedSource != null -> 500.dp
    profileType != ProfileType.ADMIN && adminProfile != null -> 350.dp
    else -> 350.dp}
val dialogHeight = requestedDialogHeight.coerceAtMost(
    configuration.screenHeightDp.dp * 0.75f)




    val avatarPresets = remember(profileType) {
        when (profileType) {
            ProfileType.ADMIN -> listOf(AdminProfileAvatarId) + ProfileAvatarPresetIds
            ProfileType.KIDS -> KidsProfileAvatarPresetIds
            ProfileType.NORMAL -> ProfileAvatarPresetIds
        }
    }
    val contentPrefixOptions = remember(initial?.detectedContentPrefixes, selectedContentPrefixes) {
        ContentPrefixPolicy.optionsWithDetected(
            detectedCodes = initial?.detectedContentPrefixes.orEmpty(),
            selectedCodes = selectedContentPrefixes,
        )
    }

    fun buildProfile(normalizedName: String, normalizedHost: String = host): PlaylistProfile =
        PlaylistProfile(
            id = initial?.id.orEmpty(),
            name = normalizedName,
            source = if (credentialsMode == CredentialsMode.SHARED_WITH_ADMIN) adminProfile?.source ?: source else source,
            type = profileType,
            credentialsMode = credentialsMode,
            isLocked = initial?.isLocked ?: false,
            avatarId = avatarId,
            avatarColorHex = initial?.avatarColorHex.orEmpty(),
            xtreamHost = if (credentialsMode == CredentialsMode.CUSTOM) normalizedHost else "",
            xtreamUsername = if (credentialsMode == CredentialsMode.CUSTOM) username else "",
            xtreamPassword = if (credentialsMode == CredentialsMode.CUSTOM) password else "",
            m3uUrl = if (credentialsMode == CredentialsMode.CUSTOM) m3uUrl else "",
            epgUrl = if (credentialsMode == CredentialsMode.CUSTOM) epgUrl else "",
            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
            lastSyncAt = initial?.lastSyncAt,
            selectedContentPrefixes = selectedContentPrefixes,
            detectedContentPrefixes = initial?.detectedContentPrefixes.orEmpty(),
            lastCatalogFingerprint = initial?.lastCatalogFingerprint.orEmpty(),
        )

    fun validateCustomXtream(onSuccess: (String) -> Unit) {
        validatingCredentials = true
        validationMessage = null
        error = null
        validationScope.launch {
            when (val result = container.xtreamCredentialsValidator.validate(host, username, password)) {
                is XtreamCredentialsValidationResult.Success -> {
                    host = result.normalizedHost
                    validationMessage = "Xtream connection successful."
                    onSuccess(result.normalizedHost)
                }
                is XtreamCredentialsValidationResult.Failure -> error = result.message
            }
            validatingCredentials = false
        }
    }

    fun addManualContentPrefix() {
        val normalizedPrefix = ContentPrefixPolicy.normalizeCode(manualContentPrefix)
        manualContentPrefixError = when {
            normalizedPrefix == null -> strings.profileContentFilterRequired
            normalizedPrefix in selectedContentPrefixes -> strings.profileContentFilterDuplicate
            else -> null
        }
        if (normalizedPrefix != null && manualContentPrefixError == null) {
            selectedContentPrefixes = selectedContentPrefixes + normalizedPrefix
            manualContentPrefix = ""
        }
    }

    LaunchedEffect(Unit) {
        delay(ProfileFocusRequestDelayMillis)
        runCatching { nameFocusRequester.requestFocus() }
    }

    TvDialogSurface(
        title = when {
            initial != null -> "Modifier le profil"
            profileType == ProfileType.KIDS -> "Add Kids Profile"
            else -> "Add Normal Profile"
        },
        onDismiss = onDismiss,
        width = dialogWidth,
        icon = Icons.Default.Person,
        modifier = Modifier.height(dialogHeight).imePadding(),
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(0.42f)) {
                    ProfileEditTextField(
                        label = "Nom du profil",
                        value = name,
                        onValueChange = { name = it },
                        focusRequester = nameFocusRequester,
                        nextFocusRequester = contentPrefixFocusRequester,
                    )
                }
                Column(modifier = Modifier.weight(0.58f)) {
                    Text("Photo de profil", color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
                    Spacer(Modifier.height(5.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    ) {
                        avatarPresets.forEach { presetId ->
                            ProfileAvatarPresetButton(
                                avatarId = presetId,
                                profileType = profileType,
                                selected = avatarId == presetId,
                                onClick = { avatarId = presetId },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                strings.profileContentFilters,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (selectedContentPrefixes.isEmpty()) {
                    strings.profileContentFiltersAllIncluded
                } else {
                    strings.profileContentFiltersSelectedOnly
                },
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 2,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                contentPrefixOptions.forEachIndexed { index, option ->
                    key(option.code) {
                        val selected = option.code in selectedContentPrefixes
                        TvButton(
                            text = option.code,
                            onClick = {
                                selectedContentPrefixes = if (selected) {
                                    selectedContentPrefixes - option.code
                                } else {
                                    selectedContentPrefixes + option.code
                                }
                            },
                            focusRequester = contentPrefixFocusRequester.takeIf { index == 0 },
                            selected = selected,
                            variant = TvButtonVariant.Secondary,
                            modifier = Modifier
                                .height(42.dp)
                                .focusProperties {
                                    up = nameFocusRequester
                                    down = manualPrefixFocusRequester
                                },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ProfileEditTextField(
                        label = strings.profileContentFilterCode,
                        value = manualContentPrefix,
                        onValueChange = { value ->
                            manualContentPrefix = value
                                .filter(Char::isLetter)
                                .take(3)
                                .uppercase(Locale.ROOT)
                            manualContentPrefixError = null
                        },
                        focusRequester = manualPrefixFocusRequester,
                        previousFocusRequester = contentPrefixFocusRequester,
                        nextFocusRequester = firstSourceFocusRequester,
                        rightFocusRequester = manualPrefixAddFocusRequester,
                    )
                }
                TvButton(
                    text = strings.profileContentFilterAdd,
                    onClick = ::addManualContentPrefix,
                    enabled = manualContentPrefix.isNotBlank(),
                    focusRequester = manualPrefixAddFocusRequester,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .width(112.dp)
                        .height(42.dp)
                        .focusProperties {
                            left = manualPrefixFocusRequester
                            up = contentPrefixFocusRequester
                            down = firstSourceFocusRequester
                        },
                )
            }
            manualContentPrefixError?.let { message ->
                Spacer(Modifier.height(5.dp))
                Text(message, color = SmartVisionColors.Error, style = SmartVisionType.Caption)
            }
            Spacer(Modifier.height(12.dp))
            if (profileType != ProfileType.ADMIN && adminProfile != null) {
                Text("Xtream credentials", color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TvButton(
                        text = "Same as administrator",
                        onClick = { credentialsMode = CredentialsMode.SHARED_WITH_ADMIN },
                        focusRequester = firstSourceFocusRequester,
                        variant = if (credentialsMode == CredentialsMode.SHARED_WITH_ADMIN) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .focusProperties {
                                up = manualPrefixFocusRequester
                                if (credentialsMode == CredentialsMode.SHARED_WITH_ADMIN) down = saveFocusRequester
                            },
                    )
                    TvButton(
                        text = "Other credentials",
                        onClick = { credentialsMode = CredentialsMode.CUSTOM },
                        variant = if (credentialsMode == CredentialsMode.CUSTOM) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .focusProperties { up = manualPrefixFocusRequester },
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            if (credentialsMode == CredentialsMode.CUSTOM) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TvButton(
                    text = "Xtream Codes",
                    onClick = {
                        source = PlaylistSource.Xtream
                        expandedSource = if (expandedSource == PlaylistSource.Xtream) null else PlaylistSource.Xtream
                    },
                    focusRequester = if (profileType == ProfileType.ADMIN || adminProfile == null) firstSourceFocusRequester else null,
                    variant = if (expandedSource == PlaylistSource.Xtream) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                    trailingContent = {
                        Icon(
                            if (expandedSource == PlaylistSource.Xtream) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .focusProperties {
                            up = manualPrefixFocusRequester
                            if (expandedSource == null) down = saveFocusRequester
                        },
                )
                TvButton(
                    text = "Playlist M3U",
                    onClick = {
                        source = PlaylistSource.M3u
                        expandedSource = if (expandedSource == PlaylistSource.M3u) null else PlaylistSource.M3u
                    },
                    variant = if (expandedSource == PlaylistSource.M3u) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                    trailingContent = {
                        Icon(
                            if (expandedSource == PlaylistSource.M3u) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .focusProperties {
                            up = manualPrefixFocusRequester
                            if (expandedSource == null) down = saveFocusRequester
                        },
                )
            }
            if (credentialsMode == CredentialsMode.CUSTOM) Spacer(Modifier.height(12.dp))
            if (credentialsMode == CredentialsMode.CUSTOM && expandedSource == PlaylistSource.Xtream) {
                ProfileEditTextField("URL serveur", host, { host = it }, hostFocusRequester, nameFocusRequester, usernameFocusRequester)
                Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Bottom,
    modifier = Modifier.fillMaxWidth()
) {
    Column(modifier = Modifier.weight(1f)) {
        ProfileEditTextField("Username", username, { username = it }, usernameFocusRequester, hostFocusRequester, passwordFocusRequester)
    }
    Column(modifier = Modifier.weight(1f)) {
        ProfileEditTextField("Password", password, { password = it }, passwordFocusRequester, usernameFocusRequester, epgFocusRequester, password = true)
    }
    TvButton(
        text = if (validatingCredentials) "Testing..." else "Test connection",
        enabled = !validatingCredentials && host.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
        onClick = { validateCustomXtream {} },
        variant = TvButtonVariant.Secondary,
        modifier = Modifier
            .width(150.dp)
            .height(42.dp),
    )
}
                ProfileEditTextField("URL EPG optionnelle", epgUrl, { epgUrl = it }, epgFocusRequester, passwordFocusRequester, saveFocusRequester)
            } else if (credentialsMode == CredentialsMode.CUSTOM && expandedSource == PlaylistSource.M3u) {
                ProfileEditTextField("Lien M3U", m3uUrl, { m3uUrl = it }, m3uFocusRequester, nameFocusRequester, epgFocusRequester)
                ProfileEditTextField("Lien EPG optionnel", epgUrl, { epgUrl = it }, epgFocusRequester, m3uFocusRequester, saveFocusRequester)
            }
            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Caption)
            }
            validationMessage?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = SmartVisionColors.Success, style = SmartVisionType.Caption)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = if (validatingCredentials) "Validating..." else "Enregistrer",
                    enabled = !validatingCredentials,
                    onClick = {
                        val normalizedName = name.trim()
                        error = when {
                            normalizedName.isBlank() -> "Le nom du profil est obligatoire."
                            existingNames.any { it.equals(normalizedName, ignoreCase = true) } -> "Un profil porte deja ce nom."
                            credentialsMode == CredentialsMode.CUSTOM && source == PlaylistSource.Xtream && (host.isBlank() || username.isBlank() || password.isBlank()) -> "URL, username et password sont obligatoires."
                            credentialsMode == CredentialsMode.CUSTOM && source == PlaylistSource.M3u && m3uUrl.isBlank() -> "Le lien M3U est obligatoire."
                            credentialsMode == CredentialsMode.CUSTOM && source == PlaylistSource.Xtream && !host.looksLikeUrlHost() -> "URL serveur Xtream invalide."
                            credentialsMode == CredentialsMode.CUSTOM && source == PlaylistSource.M3u && !m3uUrl.looksLikeHttpUrl() -> "Lien M3U invalide."
                            credentialsMode == CredentialsMode.CUSTOM && epgUrl.isNotBlank() && !epgUrl.looksLikeHttpUrl() -> "URL EPG invalide."
                            else -> null
                        }
                        if (error == null) {
                            if (credentialsMode == CredentialsMode.CUSTOM && source == PlaylistSource.Xtream) {
                                validateCustomXtream { normalizedHost -> onSave(buildProfile(normalizedName, normalizedHost)) }
                            } else {
                                onSave(buildProfile(normalizedName))
                            }
                        }
                    },
                    focusRequester = saveFocusRequester,
                    modifier = Modifier.height(42.dp),
                )
        }
    }
}

@Composable
internal fun ProfileAvatarPickerDialog(
    initialAvatarId: String,
    profileType: ProfileType,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var selectedAvatarId by remember(initialAvatarId) {
        mutableStateOf(canonicalProfileAvatarId(initialAvatarId, profileType))
    }
    val saveFocusRequester = remember { FocusRequester() }
    val avatarPresets = when (profileType) {
        ProfileType.ADMIN -> listOf(AdminProfileAvatarId) + ProfileAvatarPresetIds
        ProfileType.KIDS -> KidsProfileAvatarPresetIds
        ProfileType.NORMAL -> ProfileAvatarPresetIds
    }

    TvDialogSurface(
        title = "Modifier la photo de profil",
        onDismiss = onDismiss,
        width = 560.dp,
        icon = Icons.Default.Person,
    ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                avatarPresets.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { presetId ->
                            ProfileAvatarPresetButton(
                                avatarId = presetId,
                                profileType = profileType,
                                selected = selectedAvatarId == presetId,
                                onClick = {
                                    selectedAvatarId = presetId
                                    runCatching { saveFocusRequester.requestFocus() }
                                },
                                modifier = Modifier.size(70.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = "Enregistrer",
                    onClick = { onSave(selectedAvatarId) },
                    focusRequester = saveFocusRequester,
                    modifier = Modifier.height(42.dp),
                )
            }
    }
}

@Composable
private fun ProfileAvatarPresetButton(
    avatarId: String,
    profileType: ProfileType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(48.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(ProfileAvatarShape)
            .border(
                BorderStroke(
                    if (focused || selected) 2.dp else 1.dp,
                    when {
                        focused -> SmartVisionColors.CyanAccent
                        selected -> Color.White
                        else -> Color.White.copy(alpha = 0.20f)
                    },
                ),
                ProfileAvatarShape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        ProfileAvatarImage(
            avatarId = avatarId,
            profileType = profileType,
            modifier = Modifier
                .matchParentSize()
                .padding(3.dp),
        )
    }
}

@Composable
internal fun ConfirmPlaylistProfileDeleteDialog(
    profile: PlaylistProfile,
    strings: SmartVisionStrings,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    TvConfirmationDialog(
        title = strings.profileDeleteTitle,
        itemLabel = profile.name,
        message = strings.profileDeleteMessage,
        confirmText = strings.delete,
        cancelText = strings.cancel,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
internal fun ProfileEditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    previousFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    password: Boolean = false,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val bringIntoViewScope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current

    LaunchedEffect(editing) {
        if (editing) {
            keyboardController?.show()
        }
    }

    Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
    Spacer(Modifier.height(5.dp))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = !editing,
        singleLine = true,
        textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
        cursorBrush = SolidColor(focusStyle.accent),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                focused = focusState.isFocused
                if (focusState.isFocused) {
                    bringIntoViewScope.launch {
                        delay(120)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
                if (!focusState.isFocused) {
                    editing = false
                    keyboardController?.hide()
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        editing = true
                        keyboardController?.show()
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
                    Key.DirectionDown -> {
                        editing = false
                        keyboardController?.hide()
                        runCatching { nextFocusRequester?.requestFocus() }
                        nextFocusRequester != null
                    }
                    Key.DirectionUp -> {
                        editing = false
                        keyboardController?.hide()
                        runCatching { previousFocusRequester?.requestFocus() }
                        previousFocusRequester != null
                    }
                    Key.DirectionRight -> {
                        if (editing || rightFocusRequester == null) {
                            false
                        } else {
                            runCatching { rightFocusRequester.requestFocus() }
                            true
                        }
                    }
                    else -> false
                }
            }
            .background(
                if (focused || editing) focusStyle.background else SmartVisionColors.Surface,
                RoundedCornerShape(6.dp),
            )
            .border(
                BorderStroke(
                    if (focused) focusStyle.borderWidth else 1.dp,
                    if (editing || focused) focusStyle.accent else SmartVisionColors.Primary.copy(alpha = 0.72f),
                ),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
    Spacer(Modifier.height(10.dp))
}
