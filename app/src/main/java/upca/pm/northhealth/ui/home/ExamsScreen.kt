package upca.pm.northhealth.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.models.Appointment
import upca.pm.northhealth.ui.components.RowAppointment

/** Separador «Exames»: consultas e exames marcados. */
@Composable
fun ExamsScreen(
    uiState: HealthUiState,
    onRetry: () -> Unit,
    onOpen: (Appointment) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        HealthStateHost(uiState, onRetry) { data ->
            val now = System.currentTimeMillis()
            val upcoming = data.appointments.filter { it.dateTime >= now }.sortedBy { it.dateTime }
            val past = data.appointments.filter { it.dateTime < now }.sortedByDescending { it.dateTime }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "upcoming-title") {
                    Text("Próximas consultas e exames", style = MaterialTheme.typography.headlineSmall)
                }
                if (upcoming.isEmpty()) {
                    item(key = "upcoming-empty") {
                        Text(
                            "Não tens consultas ou exames marcados.\nToca em + para adicionar.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                items(items = upcoming, key = { "up-${it.id}" }) { appointment ->
                    RowAppointment(appointment = appointment, onClick = { onOpen(appointment) })
                }
                if (past.isNotEmpty()) {
                    item(key = "past-title") {
                        Text(
                            "Anteriores",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    items(items = past, key = { "past-${it.id}" }) { appointment ->
                        RowAppointment(appointment = appointment, onClick = { onOpen(appointment) })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(64.dp)
        ) {
            Text("+", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
