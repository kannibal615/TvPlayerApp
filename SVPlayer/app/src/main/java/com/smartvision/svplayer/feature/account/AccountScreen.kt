package com.smartvision.svplayer.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.designsystem.FocusableButton
import com.smartvision.svplayer.core.designsystem.GlassPanel
import com.smartvision.svplayer.core.designsystem.SVColors
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.domain.model.AccountProfile

@Composable
fun AccountRoute() {
    val container = LocalAppContainer.current
    val viewModel: AccountViewModel = viewModel(
        factory = viewModelFactory { AccountViewModel(container.catalogRepository) },
    )
    val account by viewModel.account.collectAsStateWithLifecycle()
    AccountScreen(account)
}

@Composable
private fun AccountScreen(account: AccountProfile) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = SVColors.Cyan)
            Text(
                text = "Mon compte",
                color = SVColors.TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                    AccountMetric("Profil", account.name, Modifier.weight(1f))
                    AccountMetric("Serveur", account.host, Modifier.weight(1f))
                    AccountMetric("Utilisateur", account.usernameMasked, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                    AccountMetric("Statut", account.status, Modifier.weight(1f))
                    AccountMetric("Expiration", account.expirationDate ?: "N/A", Modifier.weight(1f))
                    AccountMetric("Derniere sync", account.lastSync ?: "Jamais", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                    AccountMetric("Connexions", "${account.activeConnections ?: 0}/${account.maxConnections ?: 0}", Modifier.weight(1f))
                    AccountMetric("Chaines", account.liveCount.toString(), Modifier.weight(1f))
                    AccountMetric("Films", account.movieCount.toString(), Modifier.weight(1f))
                    AccountMetric("Series", account.seriesCount.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FocusableButton("Rafraichir compte", onClick = {}, icon = Icons.Default.Refresh, modifier = Modifier.weight(1f))
                    FocusableButton("Supprimer donnees", onClick = {}, icon = Icons.Default.Delete, accent = SVColors.Danger, modifier = Modifier.weight(1f))
                    FocusableButton("Parametres", onClick = {}, icon = Icons.Default.Settings, accent = SVColors.Purple, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AccountMetric(label: String, value: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier.height(94.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, color = SVColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
            Text(value, color = SVColors.TextPrimary, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        }
    }
}
