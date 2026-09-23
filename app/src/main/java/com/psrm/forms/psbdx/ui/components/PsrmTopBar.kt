package com.psrm.forms.psbdx.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.psrm.forms.psbdx.domain.model.WpSession

/**
 * Header requirement from spec: "must prominently display the active
 * WordPress username associated with the authenticated Application
 * Password." Always shows `username @ site host`, on every screen, plus a
 * logout affordance so it's unambiguous which credential is currently live
 * — important since the app fully trusts whatever Application Password is
 * stored to act as that user.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsrmTopBar(
    title: String,
    session: WpSession?,
    onBack: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (session != null) {
                    Text(
                        text = "${session.username} · ${session.siteUrl.removePrefix("https://").removePrefix("http://")}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (onLogout != null) {
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors()
    )
}
