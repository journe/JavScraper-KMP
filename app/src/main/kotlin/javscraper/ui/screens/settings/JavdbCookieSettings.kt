package javscraper.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import javscraper.auth.JavdbLoginState
import javscraper.auth.JavdbLoginStatus
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn

@Composable
internal fun JavdbCookieDialog(
    configuredCookie: String,
    loginState: JavdbLoginState,
    onSave: (String) -> Unit,
    onStartLogin: () -> Unit,
    onCancelLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    val t = LocalTranslations.current
    var cookie by remember(configuredCookie) { mutableStateOf(configuredCookie) }
    val trimmed = cookie.trim()
    val isValid = trimmed.isEmpty() ||
            trimmed.none { it.isWhitespace() || it == ';' || it == ',' }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.settingsJavdbCookie) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cookie,
                    onValueChange = { cookie = it },
                    label = { Text(t.settingsJavdbSessionCookie) },
                    isError = !isValid,
                    supportingText = if (!isValid) {
                        { Text(t.settingsJavdbCookieInvalid) }
                    } else {
                        null
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onStartLogin,
                        enabled = !loginState.running,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, null)
                        Spacer(Modifier.width(8.dp))
                        Text(t.settingsJavdbLogin)
                    }
                    if (loginState.running) {
                        TextButton(onClick = onCancelLogin) {
                            Text(t.commonCancel)
                        }
                    }
                }
                LoginStatusRow(loginState)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(cookie) }, enabled = isValid && !loginState.running) {
                Text(t.commonConfirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t.commonCancel) }
        }
    )
}

@Composable
private fun LoginStatusRow(state: JavdbLoginState) {
    val t = LocalTranslations.current
    when {
        state.running -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(javdbLoginStatusMessage(t, state))
        }
        state.status == JavdbLoginStatus.SUCCESS -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(t.settingsJavdbLoginSuccess)
        }
        state.status == JavdbLoginStatus.ERROR -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        ) {
            Icon(
                Icons.Default.Error,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                state.error ?: t.settingsJavdbLoginFailed,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

internal fun javdbLoginStatusMessage(
    t: TranslationEn,
    state: JavdbLoginState
): String = when (state.status) {
    JavdbLoginStatus.WAITING_BROWSER -> t.settingsJavdbLoginWaitingBrowser
    JavdbLoginStatus.WAITING_LOGIN -> t.settingsJavdbLoginWaiting
    JavdbLoginStatus.SAVING -> t.settingsJavdbLoginSaving
    JavdbLoginStatus.SUCCESS -> t.settingsJavdbLoginSuccess
    JavdbLoginStatus.ERROR -> state.error ?: t.settingsJavdbLoginFailed
    JavdbLoginStatus.IDLE -> ""
}
