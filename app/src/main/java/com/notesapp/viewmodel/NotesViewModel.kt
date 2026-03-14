package com.notesapp.viewmodel

import androidx.lifecycle.*
import com.notesapp.data.repository.NotesRepository
import com.notesapp.model.Note
import kotlinx.coroutines.launch

sealed class NoteEvent {
    object SaveSuccess : NoteEvent()
    data class Error(val message: String) : NoteEvent()
    object DeleteSuccess : NoteEvent()
}

class NotesViewModel(private val repository: NotesRepository) : ViewModel() {

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    val notes: LiveData<List<Note>> = _searchQuery.switchMap { query ->
        if (query.isNullOrBlank()) repository.allNotes
        else repository.searchNotes(query)
    }

    private val _currentNote = MutableLiveData<Note?>()
    val currentNote: LiveData<Note?> = _currentNote

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _event = MutableLiveData<NoteEvent?>()
    val event: LiveData<NoteEvent?> = _event

    fun saveNote(title: String, content: String) {
        if (title.isBlank()) {
            _event.value = NoteEvent.Error("El título no puede estar vacío")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val existing = _currentNote.value
                if (existing == null) {
                    repository.insertNote(Note(title = title.trim(), content = content.trim()))
                } else {
                    repository.updateNote(existing.copy(title = title.trim(), content = content.trim()))
                }
                _event.value = NoteEvent.SaveSuccess
            } catch (e: Exception) {
                _event.value = NoteEvent.Error(e.message ?: "Error al guardar")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        _isLoading.value = true
        try {
            repository.deleteNote(note)
            _event.value = NoteEvent.DeleteSuccess
        } catch (e: Exception) {
            _event.value = NoteEvent.Error(e.message ?: "Error al eliminar")
        } finally {
            _isLoading.value = false
        }
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        repository.togglePin(note)
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun clearEvent() { _event.value = null }
    
    fun prepareNewNote() {
        _currentNote.value = null
    }

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _currentNote.value = repository.getNoteById(noteId)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class NotesViewModelFactory(private val repository: NotesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
