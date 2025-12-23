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

    private val categoriesCollection get() = firestore.collection("categories")


    fun observeCategories(): Flow<List<Pair<String, CategoryRemoteDto>>> = callbackFlow {
        val registration = categoriesCollection.addSnapshotListener { snapshot, error ->
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

    suspend fun getCategories(): List<Pair<String, CategoryRemoteDto>> {
        val snapshot = categoriesCollection.orderBy("order").get().await()
        return snapshot.documents.mapNotNull { doc ->
            val dto = doc.toObject(CategoryRemoteDto::class.java)
            dto?.let { doc.id to it }
        }
    }

    suspend fun getCategoryById(id: String): Pair<String, CategoryRemoteDto>? {
        val doc = categoriesCollection.document(id).get().await()
        val dto = doc.toObject(CategoryRemoteDto::class.java) ?: return null
        return doc.id to dto
    }

    suspend fun createCategory(dto: CategoryRemoteDto): Pair<String, CategoryRemoteDto> {
        val docRef = categoriesCollection.document()
        docRef.set(dto).await()
        return docRef.id to dto
    }

    suspend fun createCategory(id: String, dto: CategoryRemoteDto) {
        categoriesCollection.document(id).set(dto).await()
    }


    suspend fun updateCategory(id: String, dto: CategoryRemoteDto): Pair<String, CategoryRemoteDto> {
        categoriesCollection.document(id).set(dto).await()
        return id to dto
    }

    suspend fun fetchCategoriesOnce(): List<Pair<String, CategoryRemoteDto>> {
        val snapshot = categoriesCollection.get().await()
        return snapshot.documents.mapNotNull { doc ->
            val dto = doc.toObject(CategoryRemoteDto::class.java)
            dto?.let { doc.id to it }
        }
    }

    suspend fun deleteCategory(id: String) {
        categoriesCollection.document(id).delete().await()
    }


}