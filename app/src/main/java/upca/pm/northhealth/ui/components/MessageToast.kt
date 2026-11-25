package upca.pm.northhealth.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/** Mostra uma mensagem pontual (erros de escrita, avisos) e limpa-a. */
@Composable
fun MessageToast(message: String?, onShown: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            onShown()
        }
    }
}
