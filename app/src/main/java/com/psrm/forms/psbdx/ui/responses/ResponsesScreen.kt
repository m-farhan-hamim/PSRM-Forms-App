package com.psrm.forms.psbdx.ui.responses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psrm.forms.psbdx.domain.model.PsrmResponse
import com.psrm.forms.psbdx.ui.components.PsrmTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsesScreen(
    viewModel: ResponsesViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { PsrmTopBar(title = "Responses", session = null, onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            // Previously captured in state but never rendered — a failed
            // load (wrong permission, a network error, a plugin version
            // without the REST routes) looked identical to "this form
            // genuinely has zero responses."
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

            if (state.responses.isEmpty() && !state.isLoading && state.error == null) {
                Text(
                    "No responses yet for this form.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn {
                items(state.responses, key = { it.id }) { response ->
                    ResponseCard(
                        response = response,
                        sending = state.replySending,
                        onSendReply = { message, notify ->
                            viewModel.sendReply(response.id, message, notify) {}
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponseCard(
    response: PsrmResponse,
    sending: Boolean,
    onSendReply: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("#${response.ticketId}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        response.reporterName ?: response.reporterEmail ?: "Anonymous",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(response.status, style = MaterialTheme.typography.labelMedium)
            }

            Text(
                response.submittedAt,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            response.answers.forEach { (label, value) ->
                Text("$label: $value", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            }

            if (response.replies.isNotEmpty()) {
                Text(
                    "${response.replies.size} repl${if (response.replies.size == 1) "y" else "ies"}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (response.repliesEnabled) {
                Button(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(if (expanded) "Cancel" else "Reply")
                }

                if (expanded) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Your reply") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Button(
                        onClick = {
                            onSendReply(replyText, true)
                            replyText = ""
                            expanded = false
                        },
                        enabled = !sending && replyText.isNotBlank(),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(if (sending) "Sending…" else "Send reply (notify by email)")
                    }
                }
            }
        }
    }
}
