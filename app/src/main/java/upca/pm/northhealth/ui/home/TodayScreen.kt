package upca.pm.northhealth.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.countdownText
import upca.pm.northhealth.formatDateTime
import upca.pm.northhealth.formatDate
import upca.pm.northhealth.formatTime
import upca.pm.northhealth.models.Appointment
import upca.pm.northhealth.models.FORGOTTEN_AFTER_MS
import upca.pm.northhealth.models.Intake
import upca.pm.northhealth.models.IntakeState
import upca.pm.northhealth.models.effectiveState
import upca.pm.northhealth.models.isPending
import upca.pm.northhealth.startOfToday
import upca.pm.northhealth.ui.components.BigButton
import upca.pm.northhealth.ui.components.ButtonKind
import upca.pm.northhealth.ui.components.SpeakButton
import upca.pm.northhealth.ui.components.rememberSpeaker

/** Ecrã 1: Início (hoje) - próxima toma, exame mais próximo e atalhos. */
@Composable
fun TodayScreen(
    uiState: HealthUiState,
    userName: String?,
    onRetry: () -> Unit,
    onConfirmTaken: (Intake) -> Unit,
    onOpenIntake: (Intake) -> Unit,
    onOpenExam: (Appointment) -> Unit,
    onOpenMeds: () -> Unit,
    onOpenAlarmSigns: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speaker = rememberSpeaker()

    HealthStateHost(uiState, onRetry, modifier) { data ->
        val now = System.currentTimeMillis()
        val nextIntake = data.intakes
            .filter { it.isPending() && it.scheduledAt >= now - FORGOTTEN_AFTER_MS }
            .minByOrNull { it.scheduledAt }
        val nextExam = data.appointments
            .filter { it.dateTime >= now }
            .minByOrNull { it.dateTime }
        val forgottenToday = data.intakes.count {
            it.scheduledAt >= startOfToday() && it.effectiveState(now) == IntakeState.ESQUECIDA
        }

        val summary = buildString {
            if (nextIntake != null) {
                append("A tua próxima toma é ${nextIntake.medicationName}, às ${formatTime(nextIntake.scheduledAt)}. ")
            } else {
                append("Não tens tomas pendentes. ")
            }
            if (nextExam != null) {
                append("O teu próximo exame é ${nextExam.type}, a ${formatDate(nextExam.dateTime)}.")
            } else {
                append("Não tens exames marcados.")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (userName.isNullOrBlank()) "Olá!" else "Olá, $userName!",
                style = MaterialTheme.typography.headlineMedium
            )

            if (forgottenToday > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "⚠️ Tens $forgottenToday toma(s) esquecida(s) hoje.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        BigButton("Ver tomas de hoje", onOpenMeds, kind = ButtonKind.Outlined)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💊 Próxima toma", style = MaterialTheme.typography.titleLarge)
                    if (nextIntake != null) {
                        Text(
                            "${formatTime(nextIntake.scheduledAt)} · ${nextIntake.medicationName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(nextIntake.dose, style = MaterialTheme.typography.bodyLarge)
                        BigButton("Já tomei", { onConfirmTaken(nextIntake) })
                        BigButton("Ver detalhes", { onOpenIntake(nextIntake) }, kind = ButtonKind.Outlined)
                    } else {
                        Text("Não tens tomas pendentes.", style = MaterialTheme.typography.bodyLarge)
                        BigButton("Ver medicação", onOpenMeds, kind = ButtonKind.Outlined)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🩺 Exame mais próximo", style = MaterialTheme.typography.titleLarge)
                    if (nextExam != null) {
                        Text(nextExam.type, style = MaterialTheme.typography.titleMedium)
                        Text(formatDateTime(nextExam.dateTime), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${countdownText(nextExam.dateTime)} · ${nextExam.doneSteps} de ${nextExam.totalSteps} passos",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        BigButton("Preparar", { onOpenExam(nextExam) })
                    } else {
                        Text("Não tens exames marcados.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            BigButton("🚨 Sinais de alarme: quando pedir ajuda", onOpenAlarmSigns, kind = ButtonKind.Warning)
            SpeakButton(text = summary, speaker = speaker)
        }
    }
}
