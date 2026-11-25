package upca.pm.northhealth.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.ReminderScheduler
import upca.pm.northhealth.ui.components.BigButton
import upca.pm.northhealth.ui.components.ButtonKind

private val FONT_SIZES = listOf("Normal" to 1f, "Grande" to 1.25f, "Muito grande" to 1.5f)

/** Ecrã 8: Acessibilidade e perfil (letra, contraste, alarmes e sessão). */
@Composable
fun ProfileScreen(
    name: String?,
    email: String?,
    settings: AccessSettings,
    onFontScale: (Float) -> Unit,
    onHighContrast: (Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canExact = ReminderScheduler.canScheduleExact(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("👤 ${name ?: "Utilizador"}", style = MaterialTheme.typography.titleLarge)
                if (!email.isNullOrBlank()) Text(email, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tamanho da letra", style = MaterialTheme.typography.titleMedium)
                FONT_SIZES.forEach { (label, scale) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.fontScale == scale,
                            onClick = { onFontScale(scale) }
                        )
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Alto contraste",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = settings.highContrast, onCheckedChange = onHighContrast)
                }
            }
        }

        if (!canExact) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⏰ Alarmes exatos desativados", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Para os lembretes tocarem à hora certa, permite os alarmes e lembretes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    BigButton(
                        text = "Permitir alarmes",
                        kind = ButtonKind.Outlined,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🔒 Privacidade", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Os teus dados ficam guardados no Firebase, numa região da União Europeia, " +
                        "e só tu tens acesso a eles. A app não usa rastreio publicitário.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        BigButton("Terminar sessão", onLogout, kind = ButtonKind.Warning)
    }
}
