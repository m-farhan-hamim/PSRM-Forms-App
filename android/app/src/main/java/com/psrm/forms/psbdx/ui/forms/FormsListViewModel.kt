package com.psrm.forms.psbdx.ui.forms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psrm.forms.psbdx.data.repository.AuthRepository
import com.psrm.forms.psbdx.data.repository.FormsRepository
import com.psrm.forms.psbdx.domain.model.PsrmForm
import com.psrm.forms.psbdx.domain.model.WpSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class FormsListUiState(
    val session: WpSession? = null,
    val forms: List<PsrmForm> = emptyList(),
    val isRefreshing: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
    val createdFormId: Long? = null
)

class FormsListViewModel(
    private val formsRepository: FormsRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FormsListUiState())
    val uiState: StateFlow<FormsListUiState> = _uiState.asStateFlow()

    init {
        combine(formsRepository.observeForms(), authRepository.session) { forms, session ->
            _uiState.value.copy(forms = forms, session = session)
        }.onEach { _uiState.value = it }.launchIn(viewModelScope)

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            val result = formsRepository.refresh()
            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun deleteForm(id: Long) {
        viewModelScope.launch {
            val result = formsRepository.deleteForm(id)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }

    /** The Forms list's + button. On success, sets [FormsListUiState.createdFormId]
     *  as a one-shot navigation signal — the screen consumes it and calls
     *  [onCreatedFormIdConsumed] to clear it. */
    fun createForm(title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, error = null)
            val result = formsRepository.createForm(title)
            _uiState.value = _uiState.value.copy(
                isCreating = false,
                error = result.exceptionOrNull()?.message,
                createdFormId = result.getOrNull()?.id
            )
        }
    }

    fun onCreatedFormIdConsumed() {
        _uiState.value = _uiState.value.copy(createdFormId = null)
    }
}
