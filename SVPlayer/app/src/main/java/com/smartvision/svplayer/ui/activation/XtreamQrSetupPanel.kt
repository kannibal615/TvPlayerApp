package com.smartvision.svplayer.ui.activation

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.activation.ActivationSession
import com.smartvision.svplayer.data.activation.StoredActivationState
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun XtreamQrSetupPanel(
    activationRepository: ActivationRepository,
    title: String,
    modifier: Modifier = Modifier,
    onManualAccount: suspend (XtreamAccount) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val localState by activationRepository.localState.collectAsStateWithLifecycle(
        initialValue = StoredActivationState("", "", "pending", false, null, null, null, false),
    )
    var session by remember { mutableStateOf<ActivationSession?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var host by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val hostFocus = remember { FocusRequester() }
    val userFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    fun refresh() {
        loading = true
        error = null
        scope.launch {
            runCatching { activationRepository.createPlaylistSetupSession() }
                .onSuccess {
                    session = it
                    loading = false
                }
                .onFailure {
                    error = when (it) {
                        is ActivationException -> it.message ?: "Lien indisponible."
                        else -> "Lien de configuration indisponible."
                    }
                    loading = false
                }
        }
    }

    fun saveManual() {
        if (saving) return
        val normalizedHost = host.trim().trimEnd('/')
        if (normalizedHost.removePrefix("https://").removePrefix("http://").isBlank() ||
            username.isBlank() ||
            password.isBlank()
        ) {
            error = "Host, utilisateur et mot de passe sont obligatoires."
            return
        }

        saving = true
        error = null
        scope.launch {
            runCatching {
                onManualAccount(
                    XtreamAccount(
                        id = "tv_setup",
                        name = "Compte principal",
                        host = normalizedHost,
                        username = username.trim(),
                        password = password.trim(),
                    ),
                )
            }
                .onSuccess {
                    saving = false
                }
                .onFailure {
                    saving = false
                    error = "Identifiants Xtream invalides ou serveur inaccessible."
                }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
        delay(220)
        hostFocus.requestFocus()
    }

    LaunchedEffect(session?.shortCode) {
        while (session != null && isActive) {
            delay(5_000L)
            runCatching { activationRepository.checkStatus() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.52f),
                        Color(0xFF06101F),
                        Color(0xFF01040B),
                    ),
                    radius = 1500f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(1120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF102038).copy(alpha = 0.88f),
                            Color(0xFF071322).copy(alpha = 0.96f),
                        ),
                    ),
                )
                .border(BorderStroke(1.dp, Color(0xFF2A3B58)), RoundedCornerShape(20.dp))
                .padding(horizontal = 34.dp, vertical = 22.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                ) {
                    SmartVisionLogo()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = title,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.TitleL,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Saisissez vos identifiants Xtream ou configurez-les depuis le site SmartVision.",
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Label,
                    )
                    Spacer(Modifier.height(18.dp))
                    DeviceCodeCard(localState.publicDeviceCode.ifBlank { "------" })
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Identifiants Xtream",
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.Label,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    XtreamField(
                        value = host,
                        onValueChange = { host = it },
                        placeholder = "Host / URL serveur",
                        icon = Icons.Default.Storage,
                        focusRequester = hostFocus,
                        next = userFocus,
                    )
                    Spacer(Modifier.height(10.dp))
                    XtreamField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "Nom d'utilisateur",
                        icon = Icons.Default.Person,
                        focusRequester = userFocus,
                        previous = hostFocus,
                        next = passwordFocus,
                    )
                    Spacer(Modifier.height(10.dp))
                    XtreamField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Mot de passe",
                        icon = Icons.Default.Lock,
                        focusRequester = passwordFocus,
                        previous = userFocus,
                        password = true,
                    )
                    Spacer(Modifier.height(20.dp))
                    TvButton(
                        text = if (saving) "Validation..." else "Continuer",
                        onClick = ::saveManual,
                        enabled = !saving,
                        contentPadding = PaddingValues(horizontal = 26.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    )
                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = it,
                            color = SmartVisionColors.Error,
                            style = SmartVisionType.Label,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Besoin d'aide ? Rendez-vous sur app.smartvisions.net",
                        color = SmartVisionColors.TextSecondary.copy(alpha = 0.82f),
                        style = SmartVisionType.Caption,
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(480.dp)
                        .background(Color(0xFF2D4263).copy(alpha = 0.82f)),
                )

                Column(
                    modifier = Modifier.width(326.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Configurer depuis le site",
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.TitleS,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Scannez pour ouvrir app.smartvisions.net",
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Body,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(22.dp))
                    QrCard(
                        content = session?.qrUrl.orEmpty(),
                        loading = loading,
                        size = 236,
                    )
                    Spacer(Modifier.height(22.dp))
                    XtreamSteps()
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = SmartVisionColors.TextSecondary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Connexion securisee - Vos donnees sont protegees.",
                    color = SmartVisionColors.TextSecondary.copy(alpha = 0.72f),
                    style = SmartVisionType.Caption,
                )
            }
        }
    }
}

@Composable
private fun DeviceCodeCard(code: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A182B).copy(alpha = 0.82f))
            .border(BorderStroke(1.dp, Color(0xFF29476E)), RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SmartVisionColors.Primary.copy(alpha = 0.16f))
                .border(BorderStroke(2.dp, SmartVisionColors.Primary), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = SmartVisionColors.Primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(20.dp))
        Text("Identifiant appareil", color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label, modifier = Modifier.weight(1f))
        Text(code, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleM, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmartVisionLogo(
    modifier: Modifier = Modifier
        .width(220.dp)
        .height(56.dp),
) {
    Image(
        painter = painterResource(R.drawable.smartvision_logo_wide),
        contentDescription = "SmartVision IPTV Player",
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
private fun XtreamField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    focusRequester: FocusRequester,
    previous: FocusRequester? = null,
    next: FocusRequester? = null,
    password: Boolean = false,
) {
    val fieldFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }
    var containerFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)
    val borderColor = if (editing || containerFocused) {
        SmartVisionColors.CyanAccent
    } else {
        Color(0xFF2C4A71)
    }

    LaunchedEffect(editing) {
        if (editing) {
            fieldFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { containerFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) -> {
                        editing = true
                        true
                    }
                    event.key == Key.Back && editing -> {
                        editing = false
                        keyboardController?.hide()
                        true
                    }
                    event.key == Key.DirectionDown && !editing -> {
                        next?.requestFocus()
                        next != null
                    }
                    event.key == Key.DirectionUp && !editing -> {
                        previous?.requestFocus()
                        previous != null
                    }
                    else -> false
                }
            }
            .focusable(enabled = !editing)
            .clip(shape)
            .background(Color(0xFF050D1A).copy(alpha = 0.82f))
            .border(BorderStroke(1.dp, borderColor), shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = SmartVisionColors.TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = editing,
            singleLine = true,
            textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
            cursorBrush = SolidColor(SmartVisionColors.CyanAccent),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .weight(1f)
                .focusRequester(fieldFocusRequester)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && editing) {
                        editing = false
                    }
                },
            decorationBox = { inner ->
                if (value.isBlank()) {
                    Text(placeholder, color = SmartVisionColors.TextSecondary.copy(alpha = 0.72f), style = SmartVisionType.Body)
                }
                inner()
            },
        )
        Icon(Icons.Default.Keyboard, contentDescription = null, tint = SmartVisionColors.TextSecondary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun QrCard(
    content: String,
    loading: Boolean,
    size: Int,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFDDE8FF)), RoundedCornerShape(16.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading -> CircularProgressIndicator(color = SmartVisionColors.Primary)
            content.isBlank() -> Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF10203A), modifier = Modifier.size(92.dp))
            else -> {
                val bitmap = remember(content) { createQrBitmap(content, 520) }
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR code Xtream", modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun XtreamSteps() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF071629).copy(alpha = 0.78f))
            .border(BorderStroke(1.dp, Color(0xFF2B4364)), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Step(Icons.Default.QrCode2, "Scanner")
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SmartVisionColors.Primary, modifier = Modifier.size(22.dp))
        Step(Icons.Default.Storage, "Saisir")
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SmartVisionColors.Primary, modifier = Modifier.size(22.dp))
        Step(Icons.Default.CloudSync, "Synchroniser")
    }
}

@Composable
private fun Step(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = SmartVisionColors.Primary, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption, fontWeight = FontWeight.SemiBold)
    }
}

private fun createQrBitmap(content: String, size: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val row = y * size
        for (x in 0 until size) {
            pixels[row + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}
