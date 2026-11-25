package upca.pm.northhealth.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Preferências de acessibilidade (RF15): tamanho da letra e alto contraste. */
data class AccessSettings(
    val fontScale: Float = 1f,
    val highContrast: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        AccessSettings(
            fontScale = prefs.getFloat("fontScale", 1f),
            highContrast = prefs.getBoolean("highContrast", false)
        )
    )
    val settings: StateFlow<AccessSettings> = _settings.asStateFlow()

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat("fontScale", scale).apply()
        _settings.update { it.copy(fontScale = scale) }
    }

    fun setHighContrast(enabled: Boolean) {
        prefs.edit().putBoolean("highContrast", enabled).apply()
        _settings.update { it.copy(highContrast = enabled) }
    }
}
