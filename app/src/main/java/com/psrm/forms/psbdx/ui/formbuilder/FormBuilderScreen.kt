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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateMapOf
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
                                onDuplicate = { viewModel.duplicateField(field.id) },
                                onDelete = { viewModel.deleteField(field.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog && form != null) {
        FieldEditorDialog(
            existing = dialogField,
            allFields = form.fields,
            onDismiss = { showDialog = false },
            onSave = { type, label, required, choices, conditionalEnabled, conditionalMode, conditionalRules, nextAction ->
                if (isAddingNew) {
                    viewModel.addField(type, label, required, choices, conditionalEnabled, conditionalMode, conditionalRules, nextAction)
                } else {
                    dialogField?.let {
                        viewModel.updateField(it.id, type, label, required, choices, conditionalEnabled, conditionalMode, conditionalRules, nextAction)
                    }
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun FieldCard(
    field: PsrmField,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (field.protectedField) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Protected field — can't be removed",
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
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
            // A protected field (the form's leading Section Break, or the
            // rating form's Review field) is restored by the server on the
            // very next save even if deleted, so there's nothing useful a
            // delete button here could do — same reasoning as the PC
            // editor's locked, disabled delete control for these fields.
            if (!field.protectedField) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove field")
                }
            }
        }
    }
}

/**
 * A read-only text field that opens a [DropdownMenu] when tapped — the
 * same click-through-overlay pattern used everywhere a dropdown is needed
 * in this dialog, kept as one helper instead of three near-duplicates.
 * Deliberately a plain Box + DropdownMenu rather than
 * ExposedDropdownMenuBox/ExposedDropdownMenu/menuAnchor() — those are
 * version-sensitive (not resolvable against every Material3 version this
 * project might pin) where plain DropdownMenu has been stable since
 * Compose 1.0.
 */
@Composable
private fun DropdownField(
    label: String,
    selectedText: String,
    options: List<String>,
    enabled: Boolean = true,
    modifierTop: Int = 12,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = modifierTop.dp)) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        onSelected(index)
                    }
                )
            }
        }
    }
}

/**
 * One dialog handles both Add and Edit — [existing] null means "add a new
 * field" (blank form, defaults to Text), non-null pre-fills from that
 * field and keeps its type/handle semantics (handle itself isn't edited
 * here at all — see FormsRepository.updateField()'s doc on why).
 *
 * [allFields] is the rest of the form's current schema — needed to build
 * the "Continue to section X" / "jump to section" / "reveal field" target
 * lists, same way the PC editor's getConditionalSectionOptions() and
 * getSectionNextActionOptions() read the live canvas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldEditorDialog(
    existing: PsrmField?,
    allFields: List<PsrmField>,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        label: String,
        required: Boolean,
        choices: List<String>?,
        conditionalEnabled: Boolean,
        conditionalMode: String?,
        conditionalRules: Map<String, String>?,
        nextAction: String?
    ) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(existing?.let { PsrmFieldType.fromKey(it.type) } ?: PsrmFieldType.TEXT)
    }
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var required by remember { mutableStateOf(existing?.required ?: false) }
    var choicesText by remember { mutableStateOf(existing?.choices?.joinToString(", ") ?: "") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    // Conditional logic (Select/Radio only).
    var conditionalEnabled by remember { mutableStateOf(existing?.conditionalEnabled ?: false) }
    var conditionalMode by remember { mutableStateOf(existing?.conditionalMode ?: "section") }
    val conditionalRules = remember {
        mutableStateMapOf<String, String>().apply { existing?.conditionalRules?.let { putAll(it) } }
    }

    // Section's own "After this section" fall-through.
    var nextAction by remember { mutableStateOf(existing?.nextAction ?: "next") }

    val isProtected = existing?.protectedField ?: false
    val choiceList = choicesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add field" else "Edit field") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (isProtected) {
                    Text(
                        "This field is required by the form and can't be removed or changed to a different type.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                DropdownField(
                    label = "Field type",
                    selectedText = selectedType.displayName,
                    options = PsrmFieldType.entries.map { it.displayName },
                    enabled = !isProtected,
                    modifierTop = 0,
                    onSelected = { index -> selectedType = PsrmFieldType.entries[index] }
                )

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

                // Title/Section are read-only structural fields — they never
                // collect a value, so "Required" is meaningless for them
                // (sanitize_fields_schema() force-clears it server-side too).
                if (!selectedType.isStructural) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Required", modifier = Modifier.weight(1f))
                        Switch(checked = required, onCheckedChange = { required = it })
                    }
                }

                // Conditional logic — Select/Radio only, same two independent
                // modes as the PC editor: 'section' overrides which page Next
                // goes to per answer, 'field' reveals another field on the
                // same page when a specific answer is chosen.
                if (selectedType.supportsConditional) {
                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Conditional logic", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Switch(checked = conditionalEnabled, onCheckedChange = { conditionalEnabled = it })
                    }

                    if (conditionalEnabled) {
                        DropdownField(
                            label = "Based on the answer…",
                            selectedText = if (conditionalMode == "field") "Reveal another field" else "Jump to a section",
                            options = listOf("Jump to a section", "Reveal another field"),
                            onSelected = { index -> conditionalMode = if (index == 1) "field" else "section" }
                        )

                        if (choiceList.isEmpty()) {
                            Text(
                                "Add choices above first.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        choiceList.forEach { choice ->
                            val targetOptions: List<Pair<String, String>> = if (conditionalMode == "field") {
                                listOf("" to "— Don’t show a field —") +
                                    allFields.filter { f ->
                                        f.id != existing?.id &&
                                            PsrmFieldType.fromKey(f.type)?.isStructural != true &&
                                            f.type != "captcha"
                                    }.map { f -> f.id to f.label.ifBlank { f.type } }
                            } else {
                                listOf("" to "— Continue as normal —") +
                                    allFields.filter { f -> f.type == "section" && f.id != existing?.id }
                                        .map { f -> f.id to "Section: " + f.label.ifBlank { "Section" } } +
                                    listOf("__submit__" to "Submit the form")
                            }
                            val currentTarget = conditionalRules[choice] ?: ""
                            val currentLabel = targetOptions.firstOrNull { it.first == currentTarget }?.second
                                ?: targetOptions.first().second

                            DropdownField(
                                label = choice,
                                selectedText = currentLabel,
                                options = targetOptions.map { it.second },
                                onSelected = { index ->
                                    val value = targetOptions[index].first
                                    if (value.isEmpty()) {
                                        conditionalRules.remove(choice)
                                    } else {
                                        conditionalRules[choice] = value
                                    }
                                }
                            )
                        }
                    }
                }

                // Section's own fall-through — what happens when the visitor
                // reaches the end of the page this section starts. Same idea
                // as the PC editor's "After this section" setting, with the
                // same three options: continue in order, submit early, or
                // jump straight to a specific other section.
                if (selectedType == PsrmFieldType.SECTION) {
                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    val sectionTargets: List<Pair<String, String>> = listOf(
                        "next" to "Continue to next section",
                        "submit" to "Submit the form"
                    ) + allFields.filter { f -> f.type == "section" && f.id != existing?.id }
                        .map { f -> f.id to "Continue to section: " + f.label.ifBlank { "Section" } }

                    val currentNextLabel = sectionTargets.firstOrNull { it.first == nextAction }?.second
                        ?: sectionTargets.first().second

                    DropdownField(
                        label = "After this section",
                        selectedText = currentNextLabel,
                        options = sectionTargets.map { it.second },
                        modifierTop = 0,
                        onSelected = { index -> nextAction = sectionTargets[index].first }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val choices = if (selectedType.needsChoices) choiceList else null
                    val finalConditionalEnabled = selectedType.supportsConditional && conditionalEnabled
                    onSave(
                        selectedType.key,
                        label.trim(),
                        required,
                        choices,
                        finalConditionalEnabled,
                        if (finalConditionalEnabled) conditionalMode else null,
                        if (finalConditionalEnabled) conditionalRules.toMap() else null,
                        if (selectedType == PsrmFieldType.SECTION) nextAction else null
                    )
                },
                enabled = label.isNotBlank() && (!selectedType.needsChoices || choiceList.isNotEmpty())
            ) {
                Text(if (existing == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
