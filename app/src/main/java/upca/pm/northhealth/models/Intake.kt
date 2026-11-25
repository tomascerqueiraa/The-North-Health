package upca.pm.northhealth.models

import com.google.firebase.firestore.DocumentId

/** Estados de uma toma (diagrama de estados da secção 10.1 da proposta). */
object IntakeState {
    const val AGENDADA = "AGENDADA"
    const val ADIADA = "ADIADA"
    const val TOMADA = "TOMADA"
    const val TOMADA_COM_ATRASO = "TOMADA_COM_ATRASO"
    const val DISPENSADA = "DISPENSADA"
    const val ESQUECIDA = "ESQUECIDA"
}

/** Sem resposta durante 60 minutos, a toma passa a «Esquecida». */
const val FORGOTTEN_AFTER_MS = 60 * 60 * 1000L

/** Adiar: o alarme repete-se passados 15 minutos. */
const val SNOOZE_MS = 15 * 60 * 1000L

/** users/{uid}/intakes/{id} - cada toma prevista e o seu resultado. */
data class Intake(
    @DocumentId var id: String = "",
    var medicationId: String = "",
    var medicationName: String = "",
    var dose: String = "",
    var scheduledAt: Long = 0L,
    var remindAt: Long = 0L,   // próximo alarme (muda quando a toma é adiada)
    var takenAt: Long = 0L,
    var state: String = IntakeState.AGENDADA
)

fun Intake.isPending(): Boolean =
    state == IntakeState.AGENDADA || state == IntakeState.ADIADA

/** Estado a mostrar: uma toma pendente há mais de 60 minutos é «Esquecida». */
fun Intake.effectiveState(now: Long): String =
    if (isPending() && now > scheduledAt + FORGOTTEN_AFTER_MS) IntakeState.ESQUECIDA else state
