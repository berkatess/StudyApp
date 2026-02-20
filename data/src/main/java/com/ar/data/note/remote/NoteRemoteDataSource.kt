package com.ar.data.note.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Notes are stored under the authenticated user's document.
     * This prevents cross-user access and keeps data isolated per UID.
     */
    private fun notesCollection(uid: String) =
        firestore.collection("users").document(uid).collection("notes")

    fun getNotes(uid: String): Flow<List<Pair<String, NoteRemoteDto>>> = callbackFlow {
        val registration = notesCollection(uid).addSnapshotListener { snapshot, error ->
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

    fun observeNoteById(uid: String, id: String): Flow<Pair<String, NoteRemoteDto>?> = callbackFlow {
        val docRef = notesCollection(uid).document(id)

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

    suspend fun getNotesByCategory(uid: String, categoryId: String): List<Pair<String, NoteRemoteDto>> {
        val snapshot = notesCollection(uid)
            .whereEqualTo("categoryId", categoryId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val dto = doc.toObject(NoteRemoteDto::class.java)
            dto?.let { doc.id to it }
        }
    }

    suspend fun fetchNotesByCategoryOnce(
        uid: String,
        categoryId: String
    ): List<Pair<String, NoteRemoteDto>> {
        val snapshot = notesCollection(uid)
            .whereEqualTo("categoryId", categoryId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val dto = doc.toObject(NoteRemoteDto::class.java)
            dto?.let { doc.id to it }
        }
    }

    fun observeNotesByCategory(
        uid: String,
        categoryId: String
    ): Flow<List<Pair<String, NoteRemoteDto>>> = callbackFlow {
        val registration = notesCollection(uid)
            .whereEqualTo("categoryId", categoryId)
            .addSnapshotListener { snapshot, error ->
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

    suspend fun getNoteById(uid: String, id: String): Pair<String, NoteRemoteDto>? {
        val doc = notesCollection(uid).document(id).get().await()
        val dto = doc.toObject(NoteRemoteDto::class.java) ?: return null
        return doc.id to dto
    }

    suspend fun createNote(uid: String, id: String, dto: NoteRemoteDto): Pair<String, NoteRemoteDto> {
        val docRef = notesCollection(uid).document(id)
        docRef.set(dto).await()
        return id to dto
    }

    suspend fun updateNote(uid: String, id: String, dto: NoteRemoteDto): Pair<String, NoteRemoteDto> {
        notesCollection(uid).document(id).set(dto).await()
        return id to dto
    }

    suspend fun deleteNote(uid: String, id: String) {
        notesCollection(uid).document(id).delete().await()
    }

    suspend fun fetchNotesOnce(uid: String): List<Pair<String, NoteRemoteDto>> {
        val snapshot = notesCollection(uid).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val dto = doc.toObject(NoteRemoteDto::class.java)
            dto?.let { doc.id to it }
        }
    }
}
