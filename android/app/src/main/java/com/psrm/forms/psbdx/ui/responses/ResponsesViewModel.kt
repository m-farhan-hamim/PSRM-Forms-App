package com.psrm.forms.psbdx.ui.responses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psrm.forms.psbdx.data.repository.ResponsesRepository
import com.psrm.forms.psbdx.domain.model.PsrmResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ResponsesUiState(
    val responses: List<PsrmResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val replySending: Boolean = false
)

class ResponsesViewModel(
    private val responsesRepository: ResponsesRepository,
    private val formId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResponsesUiState())
    val uiState: StateFlow<ResponsesUiState> = _uiState.asStateFlow()

    init {
        responsesRepository.observeResponses(formId)
            .onEach { list -> _uiState.value = _uiState.value.copy(responses = list) }
            .launchIn(viewModelScope)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = responsesRepository.refresh(formId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun sendReply(responseId: Long, message: String, notifyEmail: Boolean, onSent: () -> Unit) {
        if (message.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(replySending = true, error = null)
            val result = responsesRepository.sendReply(responseId, message, notifyEmail)
            _uiState.value = _uiState.value.copy(
                replySending = false,
                error = result.exceptionOrNull()?.message
            )
            if (result.isSuccess) {
                refresh()
                onSent()
            }
        }
    }
}
