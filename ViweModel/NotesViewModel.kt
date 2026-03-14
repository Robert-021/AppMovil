// Eventos de UI (one-shot, no se repiten en rotación)
sealed class NoteEvent {
    object SaveSuccess : NoteEvent()
    data class Error(val message: String) : NoteEvent()
    object DeleteSuccess : NoteEvent()
}

class NotesViewModel(private val repository: NotesRepository) : ViewModel() {

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    // switchMap: cambia automáticamente entre allNotes y búsqueda
    val notes: LiveData<List<Note>> = _searchQuery.switchMap { query ->
        if (query.isNullOrBlank()) repository.allNotes
        else repository.searchNotes(query)
    }

    private val _currentNote = MutableLiveData<Note?>()
    val currentNote: LiveData<Note?> = _currentNote

    private val _event = MutableLiveData<NoteEvent?>()
    val event: LiveData<NoteEvent?> = _event

    fun saveNote(title: String, content: String) {
        if (title.isBlank()) {
            _event.value = NoteEvent.Error("El título no puede estar vacío")
            return
        }
        viewModelScope.launch {
            val existing = _currentNote.value
            if (existing == null) {
                repository.insertNote(Note(title = title.trim(), content = content.trim()))
            } else {
                repository.updateNote(existing.copy(title = title.trim(), content = content.trim()))
            }
            _event.value = NoteEvent.SaveSuccess
        }
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repository.deleteNote(note)
        _event.value = NoteEvent.DeleteSuccess
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        repository.togglePin(note)
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun clearEvent() { _event.value = null }
}

// Factory requerido cuando ViewModel recibe parámetros
class NotesViewModelFactory(private val repository: NotesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NotesViewModel(repository) as T
    }
}