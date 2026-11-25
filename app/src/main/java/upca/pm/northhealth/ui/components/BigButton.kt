package upca.pm.northhealth.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class ButtonKind { Primary, Warning, Outlined }

/** Botão grande (mínimo 56 dp de altura, acima dos 48 dp recomendados). */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    kind: ButtonKind = ButtonKind.Primary
) {
    val sized = modifier.fillMaxWidth().heightIn(min = 56.dp)
    val label: @Composable () -> Unit = {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
    when (kind) {
        ButtonKind.Primary -> Button(onClick = onClick, modifier = sized, enabled = enabled) { label() }
        ButtonKind.Warning -> Button(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) { label() }
        ButtonKind.Outlined -> OutlinedButton(onClick = onClick, modifier = sized, enabled = enabled) { label() }
    }
}
