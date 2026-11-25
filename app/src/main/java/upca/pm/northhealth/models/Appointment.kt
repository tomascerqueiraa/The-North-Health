package upca.pm.northhealth.models

import com.google.firebase.firestore.DocumentId

/** users/{uid}/appointments/{id} - consulta ou exame marcado. */
data class Appointment(
    @DocumentId var id: String = "",
    var type: String = "",
    var place: String = "",
    var dateTime: Long = 0L,
    var totalSteps: Int = 0,
    var doneSteps: Int = 0,
    var createdAt: Long = 0L
)

/** users/{uid}/appointments/{id}/checklist/{itemId} - passo da checklist de preparação. */
data class ChecklistItem(
    @DocumentId var id: String = "",
    var text: String = "",
    var daysBefore: Int = 0,
    var done: Boolean = false,
    var order: Int = 0
)
