package upca.pm.northhealth.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import upca.pm.northhealth.FirebaseHelper
import upca.pm.northhealth.models.ChecklistItem
import upca.pm.northhealth.ui.components.Screen

/** Os 4 estados de UI da checklist de preparação. */
sealed interface ChecklistUiState {
    data object Loading : ChecklistUiState
    data class Error(val message: String) : ChecklistUiState
    data object Empty : ChecklistUiState
    data class Success(val items: List<ChecklistItem>) : ChecklistUiState
}

class ExamDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // O Navigation Compose coloca o argumento da rota no SavedStateHandle
    private val examId: String = checkNotNull(savedStateHandle.get<String>(Screen.ExamDetail.ARG_ID))

    private var listener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow<ChecklistUiState>(ChecklistUiState.Loading)
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        startListening()
    }

    fun startListening() {
        listener?.remove()
        val uid = FirebaseHelper.currentUid
        if (uid == null) {
            _uiState.value = ChecklistUiState.Error("Sessão inválida. Faz login novamente.")
            return
        }
        _uiState.value = ChecklistUiState.Loading
        listener = FirebaseHelper.listenChecklist(
            uid, examId,
            onData = { items ->
                _uiState.value =
                    if (items.isEmpty()) ChecklistUiState.Empty else ChecklistUiState.Success(items)
            },
            onError = { _uiState.value = ChecklistUiState.Error(it) }
        )
    }

    fun toggle(item: ChecklistItem) {
        val uid = FirebaseHelper.currentUid ?: return
        FirebaseHelper.toggleChecklistItem(uid, examId, item.id, !item.done) { _message.value = it }
    }

    fun clearMessage() {
        _message.value = null
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}
