package com.psrm.forms.psbdx.ui.formbuilder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psrm.forms.psbdx.domain.model.PsrmField
import com.psrm.forms.psbdx.domain.model.PsrmFieldType
import com.psrm.forms.psbdx.ui.components.PsrmTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormBuilderScreen(
    viewModel: FormBuilderViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val form = state.form

    // Which field the Add/Edit dialog is working on — null means "adding a
    // new field", non-null means "editing this existing one".
    var dialogField by remember { mutableStateOf<PsrmField?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var isAddingNew by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PsrmTopBar(title = form?.title ?: "Form", session = null, onBack = onBack)
        },
        floatingActionButton = {
            if (form != null) {
                FloatingActionButton(onClick = {
                    dialogField = null
                    isAddingNew = true
                    showDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add field")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

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

            when {
                form == null && state.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                form == null -> {
                    // Not loading and still null: the refresh already ran
                    // and failed (see FormBuilderViewModel) — the error
                    // banner above already explains why, nothing more to
                    // show here.
                }
                form.fields.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No fields yet.", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Tap + to add the first one.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    LazyColumn {
                        items(form.fields.sortedBy { it.order }, key = { it.id }) { field ->
                            FieldCard(
                                field = field,
                                onClick = {
                                    dialogField = field
                                    isAddingNew = false
                                    showDialog = true
                                },
                                onDuplicate = { viewModel.duplicateField(field.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        FieldEditorDialog(
            existing = dialogField,
            onDismiss = { showDialog = false },
            onSave = { type, label, required, choices ->
                if (isAddingNew) {
                    viewModel.addField(type, label, required, choices)
                } else {
                    dialogField?.let { viewModel.updateField(it.id, type, label, required, choices) }
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun FieldCard(field: PsrmField, onClick: () -> Unit, onDuplicate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    PsrmFieldType.fromKey(field.type)?.displayName ?: field.type,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    field.label + if (field.required) " *" else "",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            IconButton(onClick = onDuplicate) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate field")
            }
        }
    }
}

/**
 * One dialog handles both Add and Edit — [existing] null means "add a new
 * field" (blank form, defaults to Text), non-null pre-fills from that
 * field and keeps its type/handle semantics (handle itself isn't edited
 * here at all — see FormsRepository.updateField()'s doc on why).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldEditorDialog(
    existing: PsrmField?,
    onDismiss: () -> Unit,
    onSave: (type: String, label: String, required: Boolean, choices: List<String>?) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(existing?.let { PsrmFieldType.fromKey(it.type) } ?: PsrmFieldType.TEXT)
    }
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var required by remember { mutableStateOf(existing?.required ?: false) }
    var choicesText by remember { mutableStateOf(existing?.choices?.joinToString(", ") ?: "") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add field" else "Edit field") },
        text = {
            Column {
                // Deliberately a plain Box + DropdownMenu here rather than
                // ExposedDropdownMenuBox/ExposedDropdownMenu/menuAnchor() —
                // those are version-sensitive (not resolvable against every
                // Material3 version this project might pin) where plain
                // DropdownMenu has been stable since Compose 1.0. A
                // click-through Box over a read-only field is the classic
                // pattern for this from before Exposed* existed.
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Field type") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { typeMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PsrmFieldType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )

                if (selectedType.needsChoices) {
                    OutlinedTextField(
                        value = choicesText,
                        onValueChange = { choicesText = it },
                        label = { Text("Choices (comma-separated)") },
                        placeholder = { Text("Option A, Option B, Option C") },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Required", modifier = Modifier.weight(1f))
                    Switch(checked = required, onCheckedChange = { required = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val choices = if (selectedType.needsChoices) {
                        choicesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    } else {
                        null
                    }
                    onSave(selectedType.key, label.trim(), required, choices)
                },
                enabled = label.isNotBlank() && (!selectedType.needsChoices || choicesText.isNotBlank())
            ) {
                Text(if (existing == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
