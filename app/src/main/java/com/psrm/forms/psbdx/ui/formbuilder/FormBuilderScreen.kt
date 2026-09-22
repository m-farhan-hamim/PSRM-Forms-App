package com.psrm.forms.psbdx.ui.formbuilder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psrm.forms.psbdx.domain.model.PsrmField
import com.psrm.forms.psbdx.ui.components.PsrmTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormBuilderScreen(
    viewModel: FormBuilderViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val form = state.form

    Scaffold(
        topBar = {
            PsrmTopBar(title = form?.title ?: "Form", session = null, onBack = onBack)
        }
    ) { padding ->
        if (form == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isSaving) {
                androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyColumn {
                items(form.fields.sortedBy { it.order }, key = { it.id }) { field ->
                    FieldCard(
                        field = field,
                        onDuplicate = { viewModel.duplicateField(field.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldCard(field: PsrmField, onDuplicate: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(field.type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall)
                Text(
                    field.label + if (field.required) " *" else "",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            // Copy/Duplicate — the mobile counterpart to the PC editor's
            // per-card button. Full drag-reorder and settings editing are
            // out of scope for this pass; field type/label/required editing
            // reuses the same settings-sheet pattern as the PC editor and
            // slots in here once the plugin's REST controller exists.
            IconButton(onClick = onDuplicate) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate field")
            }
        }
    }
}
