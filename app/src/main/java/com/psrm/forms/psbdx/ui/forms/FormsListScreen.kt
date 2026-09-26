package com.psrm.forms.psbdx.ui.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.psrm.forms.psbdx.domain.model.PsrmForm
import com.psrm.forms.psbdx.ui.components.PsrmTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormsListScreen(
    viewModel: FormsListViewModel,
    onOpenBuilder: (Long) -> Unit,
    onOpenResponses: (Long) -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showCreateDialog by remember { mutableStateOf(false) }

    // One-shot navigation: once a create succeeds, jump straight into that
    // form's builder, then tell the ViewModel the signal was consumed so a
    // later recomposition doesn't navigate again.
    LaunchedEffect(state.createdFormId) {
        state.createdFormId?.let { id ->
            onOpenBuilder(id)
            viewModel.onCreatedFormIdConsumed()
        }
    }

    Scaffold(
        topBar = { PsrmTopBar(title = "Forms", session = state.session, onLogout = onLogout) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create form")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // This was previously captured in state but never rendered —
            // meaning a real failure (most commonly: the site doesn't have
            // the psbdx-srm/v1 REST routes yet, see FormsRepository) looked
            // identical to "you just have no forms." Always show it when
            // present, on top of whatever else is on screen.
            if (state.error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (state.forms.isEmpty() && !state.isRefreshing && state.error == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No forms yet.", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Tap + to create one, or pull to refresh if this site already has forms.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.forms, key = { it.id }) { form ->
                        FormRow(
                            form = form,
                            onOpenBuilder = { onOpenBuilder(form.id) },
                            onOpenResponses = { onOpenResponses(form.id) },
                            onCopyShortcode = { clipboard.copyText(form.shortcode) },
                            onCopyShareLink = { clipboard.copyText(form.shareUrl) },
                            onDelete = { viewModel.deleteForm(form.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New form") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("Untitled Form") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createForm(title.trim())
                    showCreateDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FormRow(
    form: PsrmForm,
    onOpenBuilder: () -> Unit,
    onOpenResponses: () -> Unit,
    onCopyShortcode: () -> Unit,
    onCopyShareLink: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(form.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${form.status} · ${form.responseCount} response${if (form.responseCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onOpenBuilder) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit fields")
                }
                IconButton(onClick = onOpenResponses) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View responses")
                }
                IconButton(onClick = onCopyShortcode) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy shortcode")
                }
                // share_url is real (the plugin's site-wide URL-popup
                // feature) but only returned when the form is published
                // AND has that per-form option turned on — blank otherwise,
                // so don't offer to copy nothing.
                if (form.shareUrl.isNotBlank()) {
                    IconButton(onClick = onCopyShareLink) {
                        Icon(Icons.Default.Share, contentDescription = "Copy share link")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete form")
                }
            }
        }
    }
}

private fun ClipboardManager.copyText(text: String) = setText(AnnotatedString(text))
