package com.ar.data.category.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Categories are stored under the authenticated user's document.
     * This keeps categories isolated per UID (anonymous or signed-in).
     */
    private fun categoriesCollection(uid: String) =
        firestore.collection("users").document(uid).collection("categories")

    fun observeCategories(uid: String): Flow<List<Pair<String, CategoryRemoteDto>>> = callbackFlow {
        val registration = categoriesCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val list = snapshot?.documents?.mapNotNull { doc ->
                val dto = doc.toObject(CategoryRemoteDto::class.java)
                dto?.let { doc.id to it }
            } ?: emptyList()

            trySend(list)
        }

        awaitClose { registration.remove() }
    }

    suspend fun getCategories(uid: String): List<Pair<String, CategoryRemoteDto>> {
        val snapshot = categoriesCollection(uid).orderBy("order").get().await()
        return snapshot.documents.mapNotNull { doc ->
            val dto = doc.toObject(CategoryRemoteDto::class.java)
            dto?.let { doc.id to it }
        }
    }

    suspend fun getCategoryById(uid: String, id: String): Pair<String, CategoryRemoteDto>? {
        val doc = categoriesCollection(uid).document(id).get().await()
        val dto = doc.toObject(CategoryRemoteDto::class.java) ?: return null
        return doc.id to dto
    }

    suspend fun createCategory(uid: String, dto: CategoryRemoteDto): Pair<String, CategoryRemoteDto> {
        val docRef = categoriesCollection(uid).document()
        docRef.set(dto).await()
        return docRef.id to dto
    }

    suspend fun createCategory(uid: String, id: String, dto: CategoryRemoteDto) {
        categoriesCollection(uid).document(id).set(dto).await()
    }

    suspend fun updateCategory(uid: String, id: String, dto: CategoryRemoteDto): Pair<String, CategoryRemoteDto> {
        categoriesCollection(uid).document(id).set(dto).await()
        return id to dto
    }

    suspend fun fetchCategoriesOnce(uid: String): List<Pair<String, CategoryRemoteDto>> {
        val snapshot = categoriesCollection(uid).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val dto = doc.toObject(CategoryRemoteDto::class.java)
            dto?.let { doc.id to it }
        }
    }

    suspend fun deleteCategory(uid: String, id: String) {
        categoriesCollection(uid).document(id).delete().await()
    }
}
