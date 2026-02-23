package com.ar.studyapp.note.list

import app.cash.turbine.test
import com.ar.core.data.FetchStrategy
import com.ar.core.network.NetworkMonitor
import com.ar.core.result.Result
import com.ar.domain.category.model.Category
import com.ar.domain.category.usecase.DeleteCategoryUseCase
import com.ar.domain.category.usecase.ObserveCategoriesUseCase
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import com.ar.domain.note.usecase.DeleteNoteUseCase
import com.ar.domain.note.usecase.GetNotesUseCase
import com.ar.studyapp.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import kotlinx.coroutines.test.advanceUntilIdle
import com.ar.domain.auth.model.UserInfo
import com.ar.domain.auth.usecase.ObserveGoogleUserUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModelTurbineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()


    @Test
    fun `uiState emits Loading then Success`() = runTest {
        // --- Mocks ---
        val getNotesUseCase = mockk<GetNotesUseCase>()
        val observeCategoriesUseCase = mockk<ObserveCategoriesUseCase>()
        val deleteCategoryUseCase = mockk<DeleteCategoryUseCase>(relaxed = true)
        val deleteNoteUseCase = mockk<DeleteNoteUseCase>(relaxed = true)
        val noteRepository = mockk<NoteRepository>()
        val networkMonitor = mockk<NetworkMonitor>()

        val onlineFlow = MutableStateFlow(true)
        every { networkMonitor.isOnline } returns onlineFlow
        coEvery { networkMonitor.isOnlineNow() } returns true
        coEvery { noteRepository.hasAnyNotesLocally() } returns true
        val observeGoogleUserUseCase = mockk<ObserveGoogleUserUseCase>()
        every { observeGoogleUserUseCase() } returns flowOf(UserInfo(uid = "u1", email = null, isAnonymous = false))

        val categories = listOf(Category(id = "c1", name = "Work", colorHex = "#FF0000"))
        val notes = listOf(
            Note(
                id = "n1",
                title = "Buy milk",
                content = "Remember",
                categoryId = "c1",
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH
            )
        )

        // Burada kritik olan şey: Flow sıralı emisyon üretsin.
        // Turbine "Loading geldi mi, sonra Success geldi mi" diye bakacak.
        every { getNotesUseCase(any()) } returns flow {
            emit(Result.Loading)
            emit(Result.Success(notes))
        }
        every { observeCategoriesUseCase(any()) } returns flow {
            emit(Result.Loading)
            emit(Result.Success(categories))
        }

        val vm = NoteListViewModel(
            getNotesUseCase = getNotesUseCase,
            observeCategoriesUseCase = observeCategoriesUseCase,
            deleteCategoryUseCase = deleteCategoryUseCase,
            deleteNoteUseCase = deleteNoteUseCase,
            noteRepository = noteRepository,
            networkMonitor = networkMonitor,
            observeGoogleUserUseCase = observeGoogleUserUseCase
        )

        // --- Turbine ile "zaman içindeki sıra" testi ---
        vm.uiState.test {
            // ViewModel init içindeki coroutine’lerin çalışması için test scheduler’ı ilerlet
            advanceUntilIdle()

            val first = awaitItem()
            val loadingState = if (first is NotesUiState.Loading) first else awaitItem()
            assertTrue(loadingState is NotesUiState.Loading)

            // Success’e geçiş için tekrar ilerlet (özellikle combine/map zincirlerinde işe yarar)
            advanceUntilIdle()

            val successState = awaitItem()
            assertTrue(successState is NotesUiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState emits Loading then Error when notes fail`() = runTest {
        val getNotesUseCase = mockk<GetNotesUseCase>()
        val observeCategoriesUseCase = mockk<ObserveCategoriesUseCase>()
        val deleteCategoryUseCase = mockk<DeleteCategoryUseCase>(relaxed = true)
        val deleteNoteUseCase = mockk<DeleteNoteUseCase>(relaxed = true)
        val noteRepository = mockk<NoteRepository>()
        val networkMonitor = mockk<NetworkMonitor>()

        val onlineFlow = MutableStateFlow(true)
        every { networkMonitor.isOnline } returns onlineFlow
        coEvery { networkMonitor.isOnlineNow() } returns true
        coEvery { noteRepository.hasAnyNotesLocally() } returns true

        // Notes akışı Loading -> Error
        every { getNotesUseCase(any()) } returns flow {
            emit(Result.Loading)
            emit(Result.Error("boom"))
        }

        // Categories akışı Success (boş liste)
        every { observeCategoriesUseCase(any()) } returns flow {
            emit(Result.Success(emptyList()))
        }

        val observeGoogleUserUseCase = mockk<ObserveGoogleUserUseCase>()
        every { observeGoogleUserUseCase() } returns flowOf(UserInfo(uid = "u1", email = null, isAnonymous = false))

        val vm = NoteListViewModel(
            getNotesUseCase = getNotesUseCase,
            observeCategoriesUseCase = observeCategoriesUseCase,
            deleteCategoryUseCase = deleteCategoryUseCase,
            deleteNoteUseCase = deleteNoteUseCase,
            noteRepository = noteRepository,
            networkMonitor = networkMonitor,
            observeGoogleUserUseCase = observeGoogleUserUseCase
        )

        vm.uiState.test {
            // init işleri çalışsın
            advanceUntilIdle()

            val first = awaitItem()
            val loading = if (first is NotesUiState.Loading) first else awaitItem()
            assertTrue(loading is NotesUiState.Loading)

            advanceUntilIdle()

            val error = awaitItem()
            assertTrue(error is NotesUiState.Error)

            cancelAndIgnoreRemainingEvents()
        }
    }

}
