package com.notesapp.data.repository

import androidx.lifecycle.LiveData
import com.notesapp.data.local.NoteDao
import com.notesapp.model.Note
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesRepository(
    private val noteDao: NoteDao,
    private val firebaseService: FirebaseNotesService = FirebaseNotesService()
) {

    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): LiveData<List<Note>> {
        return noteDao.searchNotes(query)
    }

    suspend fun insertNote(note: Note) {
        val id = noteDao.insertNote(note)
        val savedNote = note.copy(id = id)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.syncNote(savedNote)
                if (result.isSuccess) {
                    noteDao.updateNote(savedNote.copy(isSynced = true))
                }
            } catch (e: Exception) {
            }
        }
    }

    suspend fun updateNote(note: Note) {
        val updatedNote = note.copy(updatedAt = Date(), isSynced = false)
        noteDao.updateNote(updatedNote)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.syncNote(updatedNote)
                if (result.isSuccess) {
                    noteDao.updateNote(updatedNote.copy(isSynced = true))
                }
            } catch (e: Exception) { }
        }
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
        
        CoroutineScope(Dispatchers.IO).launch {
            firebaseService.deleteNote(note.id)
        }
    }

    suspend fun togglePin(note: Note) {
        val updatedNote = note.copy(isPinned = !note.isPinned, updatedAt = Date(), isSynced = false)
        noteDao.updateNote(updatedNote)
        
        CoroutineScope(Dispatchers.IO).launch {
            firebaseService.syncNote(updatedNote)
        }
    }

    suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)
    }
}
