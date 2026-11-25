package upca.pm.northhealth.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.Speaker
import upca.pm.northhealth.countdownText
import upca.pm.northhealth.formatDateTime
import upca.pm.northhealth.models.Appointment
import upca.pm.northhealth.models.CONTENT_DISCLAIMER
import upca.pm.northhealth.models.ChecklistItem
import upca.pm.northhealth.models.ExamCatalog
import upca.pm.northhealth.ui.components.BigButton
import upca.pm.northhealth.ui.components.ButtonKind
import upca.pm.northhealth.ui.components.ConfirmDialog
import upca.pm.northhealth.ui.components.EmptyView
import upca.pm.northhealth.ui.components.ErrorView
import upca.pm.northhealth.ui.components.LoadingView
import upca.pm.northhealth.ui.components.RowChecklist
import upca.pm.northhealth.ui.components.SpeakButton

/** Ecrã 4: Preparar o exame - contagem decrescente e checklist. */
@Composable
fun ExamDetailScreen(
    appointment: Appointment?,
    checklist: ChecklistUiState,
    speaker: Speaker,
    onToggle: (ChecklistItem) -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (appointment == null) {
        EmptyView("Marcação não encontrada.", modifier)
        return
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val total = appointment.totalSteps
    val progress = if (total == 0) 0f else (appointment.doneSteps.toFloat() / total).coerceIn(0f, 1f)
    val emoji = ExamCatalog.find(appointment.type).emoji
    val spoken = "${appointment.type}. ${countdownText(appointment.dateTime)}. " +
        "Marcado para ${formatDateTime(appointment.dateTime)}. " +
        "Já cumpriste ${appointment.doneSteps} de $total passos."

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$emoji ${appointment.type}", style = MaterialTheme.typography.headlineMedium)
                Text(countdownText(appointment.dateTime), style = MaterialTheme.typography.headlineSmall)
                Text(formatDateTime(appointment.dateTime), style = MaterialTheme.typography.bodyLarge)
                if (appointment.place.isNotBlank()) {
                    Text("📍 ${appointment.place}", style = MaterialTheme.typography.bodyLarge)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                )
                Text(
                    "${appointment.doneSteps} de $total passos cumpridos",
                    style = MaterialTheme.typography.bodyMedium
                )
                SpeakButton(text = spoken, speaker = speaker)
                Text(
                    CONTENT_DISCLAIMER,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        when (checklist) {
            ChecklistUiState.Loading -> item { LoadingView(Modifier.height(120.dp)) }
            is ChecklistUiState.Error -> item {
                ErrorView(checklist.message, onRetry, Modifier.height(240.dp))
            }
            ChecklistUiState.Empty -> item {
                EmptyView("Não há passos de preparação para esta marcação.", Modifier.height(120.dp))
            }
            is ChecklistUiState.Success -> items(items = checklist.items, key = { it.id }) { item ->
                RowChecklist(
                    item = item,
                    appointmentDateTime = appointment.dateTime,
                    speaker = speaker,
                    onToggle = { onToggle(item) }
                )
            }
        }

        item {
            BigButton("Apagar marcação", { confirmDelete = true }, kind = ButtonKind.Warning)
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Apagar marcação",
            text = "Queres apagar ${appointment.type} e a respetiva checklist?",
            confirmText = "Apagar",
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
