package upca.pm.northhealth.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.Speaker
import upca.pm.northhealth.deadlineText
import upca.pm.northhealth.models.ChecklistItem

/** Passo da checklist de preparação, com leitura por voz. */
@Composable
fun RowChecklist(
    item: ChecklistItem,
    appointmentDateTime: Long,
    speaker: Speaker,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.done, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null
                )
                Text(
                    deadlineText(appointmentDateTime, item.daysBefore),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = { speaker.speak(item.text) }) { Text("🔊") }
        }
    }
}
