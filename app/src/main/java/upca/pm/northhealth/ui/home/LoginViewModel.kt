package upca.pm.northhealth.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import upca.pm.northhealth.FirebaseHelper

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        validate(email, password)?.let { _uiState.value = LoginUiState(error = it); return }
        _uiState.value = LoginUiState(loading = true)
        FirebaseHelper.login(
            email, password,
            onSuccess = { _uiState.value = LoginUiState(); onSuccess() },
            onError = { _uiState.value = LoginUiState(error = it) }
        )
    }

    fun register(name: String, email: String, password: String, consent: Boolean, onSuccess: () -> Unit) {
        when {
            name.isBlank() -> { _uiState.value = LoginUiState(error = "Escreve o teu nome."); return }
            !consent -> { _uiState.value = LoginUiState(error = "Para criar conta, tens de aceitar o tratamento dos dados."); return }
        }
        validate(email, password)?.let { _uiState.value = LoginUiState(error = it); return }
        _uiState.value = LoginUiState(loading = true)
        FirebaseHelper.register(
            name, email, password,
            onSuccess = { _uiState.value = LoginUiState(); onSuccess() },
            onError = { _uiState.value = LoginUiState(error = it) }
        )
    }

    private fun validate(email: String, password: String): String? = when {
        email.isBlank() || password.isBlank() -> "Preenche o email e a palavra-passe."
        password.length < 6 -> "A palavra-passe deve ter pelo menos 6 caracteres."
        else -> null
    }
}
