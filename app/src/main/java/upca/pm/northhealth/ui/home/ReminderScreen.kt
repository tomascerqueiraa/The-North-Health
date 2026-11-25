package upca.pm.northhealth.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.Speaker
import upca.pm.northhealth.formatTime
import upca.pm.northhealth.models.Intake
import upca.pm.northhealth.models.effectiveState
import upca.pm.northhealth.models.isPending
import upca.pm.northhealth.ui.components.stateLabel

/**
 * Ecrã 13: Lembrete de toma - ecrã inteiro em alto contraste.
 * Ações: «Já tomei», «Lembrar daqui a 15 minutos» e «Hoje não vou tomar».
 */
@Composable
fun ReminderScreen(
    intake: Intake?,
    speaker: Speaker,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (intake == null) {
            Text("Toma não encontrada.", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            ReminderButton("Fechar", onClose, Color.White, Color.Black)
            return@Column
        }

        val state = intake.effectiveState(System.currentTimeMillis())
        val spoken = "É hora de tomar ${intake.medicationName}. Dose: ${intake.dose}."

        Text("💊", style = MaterialTheme.typography.headlineMedium)
        Text(
            formatTime(intake.scheduledAt),
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            intake.medicationName,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            intake.dose,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        if (intake.isPending()) {
            ReminderButton("Já tomei", onTaken, Color(0xFF00E676), Color.Black)
            Spacer(Modifier.height(12.dp))
            ReminderButton("Lembrar daqui a 15 minutos", onSnooze, Color(0xFFFFB74D), Color.Black)
            Spacer(Modifier.height(12.dp))
            ReminderButton("Hoje não vou tomar", onSkip, Color(0xFF333333), Color.White)
        } else {
            Text(stateLabel(state), color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            ReminderButton("Fechar", onClose, Color.White, Color.Black)
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { speaker.speak(spoken) },
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text("🔊 Ouvir")
        }
    }
}

@Composable
private fun ReminderButton(text: String, onClick: () -> Unit, container: Color, content: Color) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}
