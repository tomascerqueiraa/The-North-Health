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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.formatTime
import upca.pm.northhealth.models.Intake
import upca.pm.northhealth.models.IntakeState
import upca.pm.northhealth.models.effectiveState

fun stateLabel(state: String): String = when (state) {
    IntakeState.TOMADA -> "✅ Tomada"
    IntakeState.TOMADA_COM_ATRASO -> "✅ Tomada com atraso"
    IntakeState.ADIADA -> "⏰ Adiada"
    IntakeState.DISPENSADA -> "Dispensada"
    IntakeState.ESQUECIDA -> "⚠️ Esquecida"
    else -> "Agendada"
}

/** Linha reutilizável de uma toma. */
@Composable
fun RowIntake(
    intake: Intake,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = intake.effectiveState(now)
    val stateColor: Color = when (state) {
        IntakeState.ESQUECIDA -> MaterialTheme.colorScheme.tertiary
        IntakeState.TOMADA, IntakeState.TOMADA_COM_ATRASO -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("💊", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${formatTime(intake.scheduledAt)} · ${intake.medicationName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(intake.dose, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                stateLabel(state),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = stateColor
            )
        }
    }
}
