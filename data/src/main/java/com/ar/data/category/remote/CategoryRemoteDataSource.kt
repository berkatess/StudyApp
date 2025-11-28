package com.ar.data.category.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CategoryRemoteDataSource(
    private val firestore: FirebaseFirestore
) {

    private val categoriesCollection get() = firestore.collection("categories")

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

    suspend fun updateCategory(id: String, dto: CategoryRemoteDto): Pair<String, CategoryRemoteDto> {
        categoriesCollection.document(id).set(dto).await()
        return id to dto
    }

    suspend fun deleteCategory(id: String) {
        categoriesCollection.document(id).delete().await()
    }
}