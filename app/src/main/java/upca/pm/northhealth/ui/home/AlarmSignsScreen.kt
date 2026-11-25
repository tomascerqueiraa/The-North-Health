package upca.pm.northhealth.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.models.CONTENT_DISCLAIMER
import upca.pm.northhealth.ui.components.BigButton
import upca.pm.northhealth.ui.components.ButtonKind
import upca.pm.northhealth.ui.components.SpeakButton
import upca.pm.northhealth.ui.components.rememberSpeaker

/** Ecrã 5: Sinais de alarme - quando ligar 112, SNS 24 ou falar com o centro de saúde. */
@Composable
fun AlarmSignsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val speaker = rememberSpeaker()

    fun dial(number: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    val emergency = listOf(
        "Dor forte no peito",
        "Falta de ar intensa",
        "Perda de consciência ou desmaio",
        "Boca ao lado, braço sem força ou dificuldade em falar",
        "Reação alérgica grave (inchaço da cara ou da garganta)"
    )
    val sns24 = listOf(
        "Febre que não passa",
        "Vómitos que não deixam tomar a medicação",
        "Sintomas novos depois de começar um medicamento",
        "Dúvidas sobre o que fazer"
    )

    val spoken = "Se tiveres ${emergency.joinToString(", ")}, liga 112. " +
        "Se tiveres ${sns24.joinToString(", ")}, liga para a SNS 24, número 808 24 24 24."

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🚨 Liga 112 se tiveres", style = MaterialTheme.typography.titleLarge)
                emergency.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
                BigButton("Ligar 112", { dial("112") }, kind = ButtonKind.Outlined)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📞 Liga SNS 24 (808 24 24 24) se tiveres", style = MaterialTheme.typography.titleLarge)
                sns24.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
                BigButton("Ligar SNS 24", { dial("808242424") })
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🏥 Fala com o centro de saúde", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Para dúvidas sobre horários, doses ou efeitos que não são urgentes, " +
                        "marca contacto com o teu médico de família ou fala com o farmacêutico.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        SpeakButton(text = spoken, speaker = speaker)

        Text(
            "Esta aplicação não faz diagnósticos nem substitui um profissional de saúde. $CONTENT_DISCLAIMER",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
