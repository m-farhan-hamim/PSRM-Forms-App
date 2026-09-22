package com.psrm.forms.psbdx.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psrm.forms.psbdx.data.repository.AuthRepository
import com.psrm.forms.psbdx.data.repository.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val siteUrl: String = "",
    val username: String = "",
    val applicationPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onSiteUrlChange(value: String) = _uiState.update { it.copy(siteUrl = value, error = null) }
    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(applicationPassword = value, error = null) }

    fun login() {
        val state = _uiState.value
        val normalizedUrl = normalizeUrl(state.siteUrl)

        if (normalizedUrl.isBlank() || state.username.isBlank() || state.applicationPassword.isBlank()) {
            _uiState.update { it.copy(error = "Fill in the site URL, username, and Application Password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(normalizedUrl, state.username.trim(), state.applicationPassword.trim())) {
                is LoginResult.Success -> _uiState.update { it.copy(isLoading = false, loggedIn = true) }
                is LoginResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }

    private inline fun MutableStateFlow<LoginUiState>.update(transform: (LoginUiState) -> LoginUiState) {
        value = transform(value)
    }
}
