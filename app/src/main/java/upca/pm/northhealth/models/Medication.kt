package upca.pm.northhealth.models

import com.google.firebase.firestore.DocumentId

/** users/{uid}/medications/{id} */
data class Medication(
    @DocumentId var id: String = "",
    var name: String = "",
    var activeSubstance: String = "",
    var form: String = "",
    var dose: String = "",
    var times: List<String> = emptyList(), // ex.: ["08:00", "20:00"]
    var durationDays: Int = 7,
    var createdAt: Long = 0L
)
