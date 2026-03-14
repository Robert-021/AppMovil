package com.notesapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.notesapp.model.Note
import kotlinx.coroutines.tasks.await

class FirebaseNotesService {

    private val db = FirebaseFirestore.getInstance()
    private val notesCollection = db.collection("notes")

    // Upload a note to Firestore
    suspend fun syncNote(note: Note): Result<Unit> {
        return try {
            val noteMap = mapOf(
                "id" to note.id,
                "title" to note.title,
                "content" to note.content,
                "createdAt" to note.createdAt.time,
                "updatedAt" to note.updatedAt.time,
                "isPinned" to note.isPinned
            )
            notesCollection.document(note.id.toString())
                .set(noteMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete a note from Firestore
    suspend fun deleteNote(noteId: Long): Result<Unit> {
        return try {
            notesCollection.document(noteId.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetch all notes from Firestore (for backup/restore)
    suspend fun fetchAllNotes(): Result<List<Note>> {
        return try {
            val snapshot = notesCollection.get().await()
            val notes = snapshot.documents.mapNotNull { doc ->
                try {
                    Note(
                        id = doc.getLong("id") ?: 0L,
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        createdAt = java.util.Date(doc.getLong("createdAt") ?: System.currentTimeMillis()),
                        updatedAt = java.util.Date(doc.getLong("updatedAt") ?: System.currentTimeMillis()),
                        isPinned = doc.getBoolean("isPinned") ?: false,
                        isSynced = true
                    )
                } catch (e: Exception) { null }
            }
            Result.success(notes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}