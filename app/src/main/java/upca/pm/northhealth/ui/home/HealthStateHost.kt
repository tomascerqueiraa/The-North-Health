package upca.pm.northhealth.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import upca.pm.northhealth.ui.components.ErrorView
import upca.pm.northhealth.ui.components.LoadingView

/** Trata os estados Loading e Error; só chama o conteúdo quando há dados (Success). */
@Composable
fun HealthStateHost(
    uiState: HealthUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (HealthData) -> Unit
) {
    when (uiState) {
        HealthUiState.Loading -> LoadingView(modifier)
        is HealthUiState.Error -> ErrorView(uiState.message, onRetry, modifier)
        is HealthUiState.Success -> content(uiState.data)
    }
}
