package upca.pm.northhealth.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.formatHm
import upca.pm.northhealth.formatIsoDate
import upca.pm.northhealth.models.ExamCatalog
import upca.pm.northhealth.models.ExamType
import upca.pm.northhealth.toMillis
import upca.pm.northhealth.ui.components.BigButton
import upca.pm.northhealth.ui.components.ButtonKind
import java.time.LocalDate

/** Ecrã 11: Nova consulta ou exame (tipo, data, hora e local). */
@Composable
fun AddExamScreen(
    onSave: (type: ExamType, place: String, dateTime: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var typeIndex by rememberSaveable { mutableStateOf(0) }
    var date by rememberSaveable { mutableStateOf("") } // yyyy-MM-dd
    var time by rememberSaveable { mutableStateOf("") } // HH:mm
    var place by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Tipo", style = MaterialTheme.typography.titleMedium)
        ExamCatalog.types.forEachIndexed { index, type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { typeIndex = index }
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = typeIndex == index, onClick = { typeIndex = index })
                Text("${type.emoji} ${type.name}", style = MaterialTheme.typography.bodyLarge)
            }
        }

        BigButton(
            text = if (date.isBlank()) "📅 Escolher data" else "📅 ${formatIsoDate(date)}",
            kind = ButtonKind.Outlined,
            onClick = {
                val today = LocalDate.now()
                DatePickerDialog(context, { _, year, month, day ->
                    date = LocalDate.of(year, month + 1, day).toString()
                }, today.year, today.monthValue - 1, today.dayOfMonth).show()
            }
        )
        BigButton(
            text = if (time.isBlank()) "⏰ Escolher hora" else "⏰ $time",
            kind = ButtonKind.Outlined,
            onClick = {
                TimePickerDialog(context, { _, hour, minute ->
                    time = formatHm(hour, minute)
                }, 9, 0, true).show()
            }
        )

        OutlinedTextField(
            value = place,
            onValueChange = { place = it },
            label = { Text("Local (ex.: Hospital de Barcelos)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        BigButton(
            text = "Guardar e ver preparação",
            onClick = {
                if (date.isBlank() || time.isBlank()) {
                    error = "Escolhe a data e a hora."
                } else {
                    error = null
                    onSave(ExamCatalog.types[typeIndex], place, toMillis(date, time))
                }
            }
        )
    }
}
