package com.smartvision.svplayer.ui.activation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
fun XtreamSetupDialog(
    onSave: (XtreamAccount) -> Unit,
    onLater: () -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { firstFocus.requestFocus() }

    Dialog(onDismissRequest = onLater) {
        Column(
            modifier = Modifier
                .width(610.dp)
                .background(Color(0xFF081423), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Primary), RoundedCornerShape(8.dp))
                .padding(28.dp),
        ) {
            Text(
                text = "Configurer votre abonnement IPTV",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "SmartVision ne fournit aucun contenu. Saisissez les identifiants Xtream de votre abonnement.",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Body,
            )
            Spacer(Modifier.height(20.dp))
            SetupField("Adresse du serveur", host, { host = it }, Modifier.focusRequester(firstFocus))
            SetupField("Nom d utilisateur", username, { username = it })
            SetupField("Mot de passe", password, { password = it }, password = true)
            error?.let {
                Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Caption)
                Spacer(Modifier.height(10.dp))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                TvButton(
                    text = "Plus tard",
                    onClick = onLater,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(44.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = "Enregistrer et charger",
                    onClick = {
                        if (host.removePrefix("https://").removePrefix("http://").isBlank() || username.isBlank() || password.isBlank()) {
                            error = "Adresse, utilisateur et mot de passe sont obligatoires."
                        } else {
                            onSave(
                                XtreamAccount(
                                    id = "tv_setup",
                                    name = "Compte principal",
                                    host = host,
                                    username = username,
                                    password = password,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.height(44.dp),
                )
            }
        }
    }
}

@Composable
private fun SetupField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: Boolean = false,
) {
    Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
    Spacer(Modifier.height(6.dp))
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(editing) {
        if (editing) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        readOnly = !editing,
        textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
        cursorBrush = SolidColor(SmartVisionColors.CyanAccent),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) {
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
                    else -> false
                }
            }
            .background(SmartVisionColors.Surface, RoundedCornerShape(6.dp))
            .border(
                BorderStroke(
                    if (focused || editing) 2.dp else 1.dp,
                    if (focused || editing) SmartVisionColors.CyanAccent else SmartVisionColors.Border,
                ),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 13.dp, vertical = 11.dp),
    )
    Spacer(Modifier.height(14.dp))
}
