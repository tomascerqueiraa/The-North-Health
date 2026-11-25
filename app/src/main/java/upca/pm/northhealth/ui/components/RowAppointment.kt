package upca.pm.northhealth.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.countdownText
import upca.pm.northhealth.formatDateTime
import upca.pm.northhealth.models.Appointment
import upca.pm.northhealth.models.ExamCatalog

/** Linha reutilizável de uma consulta ou exame. */
@Composable
fun RowAppointment(
    appointment: Appointment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emoji = ExamCatalog.find(appointment.type).emoji
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(appointment.type, style = MaterialTheme.typography.titleMedium)
                Text(formatDateTime(appointment.dateTime), style = MaterialTheme.typography.bodyMedium)
                if (appointment.place.isNotBlank()) {
                    Text(appointment.place, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "${countdownText(appointment.dateTime)} · ${appointment.doneSteps} de ${appointment.totalSteps} passos",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
