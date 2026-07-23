package com.smartvision.svplayer.ui.activation

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.activation.ActivationSession
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun XtreamQrSetupPanel(
    activationRepository: ActivationRepository,
    strings: SmartVisionStrings,
    title: String,
    modifier: Modifier = Modifier,
    onManualAccount: suspend (XtreamAccount) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
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
                        is ActivationException -> it.message ?: strings.xtreamSetupLinkUnavailable
                        else -> strings.xtreamSetupLinkUnavailable
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
            error = strings.accountRequiredError
            return
        }

        saving = true
        error = null
        scope.launch {
            runCatching {
                onManualAccount(
                    XtreamAccount(
                        id = "tv_setup",
                        name = strings.xtreamDefaultAccountName,
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
                    error = strings.xtreamInvalidCredentials
                }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
        delay(220)
        runCatching { hostFocus.requestFocus() }
    }

    LaunchedEffect(session?.shortCode) {
        while (session != null && isActive) {
            delay(5_000L)
            runCatching { activationRepository.checkStatus() }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.startup_cinema_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF010612).copy(alpha = 0.28f)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            ScaledActivationLayout {
                Row(
                    modifier = Modifier
                        .width(680.dp)
                        .height(410.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0A1A31).copy(alpha = 0.96f),
                                    Color(0xFF030D1C).copy(alpha = 0.98f),
                                ),
                            ),
                        )
                        .border(BorderStroke(1.dp, Color(0xFF2C568C)), RoundedCornerShape(18.dp))
                        .padding(horizontal = 26.dp, vertical = 22.dp)
                        .imePadding(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        SmartVisionLogo()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = title,
                            color = SmartVisionColors.TextPrimary,
                            style = SmartVisionType.TitleM,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(14.dp))
                        XtreamField(
                            value = host,
                            onValueChange = { host = it },
                            placeholder = strings.xtreamServerPlaceholder,
                            icon = Icons.Default.Storage,
                            focusRequester = hostFocus,
                            next = userFocus,
                        )
                        Spacer(Modifier.height(8.dp))
                        XtreamField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = strings.xtreamUsernamePlaceholder,
                            icon = Icons.Default.Person,
                            focusRequester = userFocus,
                            previous = hostFocus,
                            next = passwordFocus,
                        )
                        Spacer(Modifier.height(8.dp))
                        XtreamField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = strings.password,
                            icon = Icons.Default.Lock,
                            focusRequester = passwordFocus,
                            previous = userFocus,
                            password = true,
                        )
                        Spacer(Modifier.height(14.dp))
                        XtreamPrimaryButton(
                            text = if (saving) strings.xtreamValidating else strings.xtreamContinue,
                            onClick = ::saveManual,
                            enabled = !saving,
                            previous = passwordFocus,
                        )
                        error?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = it,
                                color = SmartVisionColors.Error,
                                style = SmartVisionType.Caption,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .height(340.dp)
/*                             .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0C2242).copy(alpha = 0.94f),
                                        Color(0xFF06162B).copy(alpha = 0.98f),
                                    ),
                                ),
                            )
                            .border(BorderStroke(1.dp, Color(0xFF28599B)), RoundedCornerShape(18.dp)) */
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = strings.xtreamScanWebsite,
                            color = SmartVisionColors.TextPrimary,
                            style = SmartVisionType.Label,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(18.dp))
                        QrCard(
                            content = session?.qrUrl.orEmpty(),
                            loading = loading,
                            size = 174,
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                /* .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF071427).copy(alpha = 0.92f))
                                .border(BorderStroke(1.dp, Color(0xFF315D96)), RoundedCornerShape(12.dp)) */
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "${strings.xtreamTvCode} : ",
                                color = SmartVisionColors.TextPrimary,
                                style = SmartVisionType.Label,
                                maxLines = 1,
                            )
                            Text(
                                text = session?.shortCode?.ifBlank { "------" } ?: "------",
                                color = Color(0xFF4C8DFF),
                                style = SmartVisionType.TitleS,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartVisionLogo(
    modifier: Modifier = Modifier
        .width(180.dp)
        .height(42.dp),
) {
    Image(
        painter = painterResource(R.drawable.smartvision_logo_1),
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
    val outerShape = RoundedCornerShape(10.dp)
    val innerShape = RoundedCornerShape(8.dp)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor = if (editing || containerFocused) {
        focusStyle.accent
    } else {
        Color(0xFF2C4A71)
    }

    LaunchedEffect(editing) {
        if (editing) {
            withFrameNanos { }
            delay(40)
            runCatching { fieldFocusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(outerShape)
            .background(
                if (editing || containerFocused) {
                    Color(0xFF1478FF).copy(alpha = 0.58f)
                } else {
                    Color.Transparent
                },
            )
            .border(
                BorderStroke(1.dp, if (editing || containerFocused) Color(0xFF2E8CFF) else Color.Transparent),
                outerShape,
            )
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
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
                            runCatching { focusRequester.requestFocus() }
                            true
                        }
                        event.key == Key.DirectionDown && !editing -> {
                            runCatching { next?.requestFocus() }
                            next != null
                        }
                        event.key == Key.DirectionUp && !editing -> {
                            runCatching { previous?.requestFocus() }
                            previous != null
                        }
                        else -> false
                    }
                }
                .focusable(enabled = !editing)
                .clip(innerShape)
                .background(
                    if (editing || containerFocused) {
                        Color(0xFF102D5B).copy(alpha = 0.96f)
                    } else {
                        Color(0xFF030C19).copy(alpha = 0.88f)
                    },
                )
                .border(
                    BorderStroke(
                        if (editing || containerFocused) 2.dp else 1.dp,
                        if (editing || containerFocused) Color.White else borderColor,
                    ),
                    innerShape,
                )
                .padding(horizontal = 12.dp)
                .semantics { contentDescription = placeholder },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = SmartVisionColors.TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = editing,
                singleLine = true,
                textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
                cursorBrush = SolidColor(focusStyle.accent),
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
            Icon(Icons.Default.Keyboard, contentDescription = null, tint = SmartVisionColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun XtreamPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    previous: FocusRequester,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val outerShape = RoundedCornerShape(10.dp)
    val innerShape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(outerShape)
            .background(if (focused) Color(0xFF1478FF).copy(alpha = 0.58f) else Color.Transparent)
            .border(BorderStroke(1.dp, if (focused) Color(0xFF2E8CFF) else Color.Transparent), outerShape)
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> {
                            runCatching { previous.requestFocus() }
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (enabled) onClick()
                            true
                        }
                        else -> false
                    }
                }
                .clip(innerShape)
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(listOf(Color(0xFF2C8CFF), Color(0xFF1766F2)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFF31547C), Color(0xFF243B5B)))
                    },
                )
                .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF2B83FF)), innerShape)
                .clickable(enabled = enabled, onClick = onClick)
                .focusable(enabled = enabled),
            contentAlignment = Alignment.Center,
        ) {
            if (enabled) {
                Text(
                    text = text,
                    color = Color.White,
                    style = SmartVisionType.Body,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = text, color = Color.White, style = SmartVisionType.Body)
                }
            }
        }
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
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(BorderStroke(2.dp, Color(0xFF8ABEFF)), RoundedCornerShape(14.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading -> CircularProgressIndicator(color = com.smartvision.svplayer.ui.theme.LocalLoadingColor.current)
            content.isBlank() -> Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF10203A), modifier = Modifier.size(92.dp))
            else -> {
                val bitmap = remember(content) { createQrBitmap(content, 520) }
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR code Xtream", modifier = Modifier.fillMaxSize())
            }
        }
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
