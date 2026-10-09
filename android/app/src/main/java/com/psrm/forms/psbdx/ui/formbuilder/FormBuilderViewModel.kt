package com.psrm.forms.psbdx.ui.formbuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psrm.forms.psbdx.data.repository.FormsRepository
import com.psrm.forms.psbdx.domain.model.PsrmForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FormBuilderUiState(
    val form: PsrmForm? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class FormBuilderViewModel(
    private val formsRepository: FormsRepository,
    private val formId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(FormBuilderUiState())
    val uiState: StateFlow<FormBuilderUiState> = _uiState.asStateFlow()

    private var hasCheckedCache = false

    init {
        viewModelScope.launch {
            formsRepository.observeForms().collect { forms ->
                val form = forms.firstOrNull { it.id == formId }
                if (form != null) {
                    _uiState.value = _uiState.value.copy(form = form, isLoading = false)
                } else if (!hasCheckedCache) {
                    // Not in the Room cache yet — e.g. opened via a fresh
                    // process, or the Forms list was never refreshed this
                    // session. Without this, the screen would just spin
                    // forever instead of ever loading the form.
                    hasCheckedCache = true
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    val result = formsRepository.refresh()
                    if (result.isFailure) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message
                        )
                    }
                }
            }
        }
    }

    /** Mobile counterpart of the PC editor's per-card Copy/Duplicate button
     *  — same convention (new ID, "(Copy)" label, re-derived handle),
     *  inserted immediately after the original field. */
    fun duplicateField(fieldId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = formsRepository.duplicateField(formId, fieldId)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                form = result.getOrNull() ?: _uiState.value.form,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    /** The mobile builder's "+" action — appends a new field to the end of
     *  the form. */
    fun addField(type: String, label: String, required: Boolean, choices: List<String>?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = formsRepository.addField(formId, type, label, required, choices)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                form = result.getOrNull() ?: _uiState.value.form,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    /** Edits an existing field's type/label/required/choices in place. */
    fun updateField(fieldId: String, type: String, label: String, required: Boolean, choices: List<String>?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = formsRepository.updateField(formId, fieldId, type, label, required, choices)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                form = result.getOrNull() ?: _uiState.value.form,
                error = result.exceptionOrNull()?.message
            )
        }
    }
}
