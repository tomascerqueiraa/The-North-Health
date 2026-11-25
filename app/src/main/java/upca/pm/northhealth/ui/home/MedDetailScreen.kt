package upca.pm.northhealth.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.Speaker
import upca.pm.northhealth.models.Medication
import upca.pm.northhealth.ui.components.BigButton
import upca.pm.northhealth.ui.components.ButtonKind
import upca.pm.northhealth.ui.components.ConfirmDialog
import upca.pm.northhealth.ui.components.EmptyView
import upca.pm.northhealth.ui.components.SpeakButton

/** Ecrã 3: Explicação do medicamento (a simplificação validada virá da ULSBE). */
@Composable
fun MedDetailScreen(
    medication: Medication?,
    speaker: Speaker,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (medication == null) {
        EmptyView("Medicamento não encontrado.", modifier)
        return
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val spoken = "${medication.name}. Forma: ${medication.form}. Dose: ${medication.dose}. " +
        "Horas: ${medication.times.joinToString(", ")}."

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("💊 ${medication.name}", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Forma: ${medication.form}", style = MaterialTheme.typography.bodyLarge)
                Text("Dose: ${medication.dose}", style = MaterialTheme.typography.bodyLarge)
                Text("Horas: ${medication.times.joinToString(", ")}", style = MaterialTheme.typography.bodyLarge)
                if (medication.activeSubstance.isNotBlank()) {
                    Text("Substância ativa: ${medication.activeSubstance}", style = MaterialTheme.typography.bodyLarge)
                }
                Text("Duração: ${medication.durationDays} dias", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Para que serve e se me esquecer", style = MaterialTheme.typography.titleMedium)
                Text(
                    "A explicação simplificada deste medicamento ainda não está disponível. " +
                        "Será fornecida e validada pela ULSBE. Em caso de dúvida, fala com o teu médico ou farmacêutico.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        SpeakButton(text = spoken, speaker = speaker)
        BigButton("Apagar medicamento", { confirmDelete = true }, kind = ButtonKind.Warning)
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Apagar medicamento",
            text = "Queres apagar ${medication.name} e todas as tomas previstas?",
            confirmText = "Apagar",
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
