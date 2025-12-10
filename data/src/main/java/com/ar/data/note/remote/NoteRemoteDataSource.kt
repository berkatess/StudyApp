package com.ar.data.note.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val notesCollection get() = firestore.collection("notes")

    fun getNotes(): Flow<List<Pair<String, NoteRemoteDto>>> = callbackFlow {
        val registration = notesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val list = snapshot?.documents?.mapNotNull { doc ->
                val dto = doc.toObject(NoteRemoteDto::class.java)
                dto?.let { doc.id to it }
            } ?: emptyList()

            trySend(list)
        }

        awaitClose { registration.remove() }
    }

    fun observeNoteById(id: String): Flow<Pair<String, NoteRemoteDto>?> = callbackFlow {
        val docRef = notesCollection.document(id)

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }

            val dto = snapshot.toObject(NoteRemoteDto::class.java)
            if (dto != null) {
                trySend(snapshot.id to dto)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun getNotesByCategory(categoryId: String): List<Pair<String, NoteRemoteDto>> {
        val snapshot = notesCollection
            .whereEqualTo("categoryId", categoryId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val dto = doc.toObject(NoteRemoteDto::class.java)
            dto?.let { doc.id to it }
        }
    }

    suspend fun getNoteById(id: String): Pair<String, NoteRemoteDto>? {
        val doc = notesCollection.document(id).get().await()
        val dto = doc.toObject(NoteRemoteDto::class.java) ?: return null
        return doc.id to dto
    }

    suspend fun createNote(dto: NoteRemoteDto): Pair<String, NoteRemoteDto> {
        val docRef = notesCollection.document()
        docRef.set(dto).await()
        return docRef.id to dto
    }

    suspend fun updateNote(id: String, dto: NoteRemoteDto): Pair<String, NoteRemoteDto> {
        notesCollection.document(id).set(dto).await()
        return id to dto
    }

    suspend fun deleteNote(id: String) {
        notesCollection.document(id).delete().await()
    }
}