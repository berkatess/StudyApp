package com.ar.studyapp.note.list

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
import io.mockk.mockk
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `uiState becomes Success and maps category name`() = runTest {
        // --- Mocks ---
        val getNotesUseCase = mockk<GetNotesUseCase>()
        val observeCategoriesUseCase = mockk<ObserveCategoriesUseCase>()
        val deleteCategoryUseCase = mockk<DeleteCategoryUseCase>(relaxed = true)
        val deleteNoteUseCase = mockk<DeleteNoteUseCase>(relaxed = true)
        val noteRepository = mockk<NoteRepository>()
        val networkMonitor = mockk<NetworkMonitor>()

        // Online flow
        val onlineFlow = MutableStateFlow(true)
        every { networkMonitor.isOnline } returns onlineFlow
        coEvery { networkMonitor.isOnlineNow() } returns true

        // init empty local should not work
        coEvery { noteRepository.hasAnyNotesLocally() } returns true

        val cat1 = Category(id = "c1", name = "Work", colorHex = "#FF0000")
        val categories = listOf(cat1)

        val note1 = Note(
            id = "n1",
            title = "Buy milk",
            content = "Remember to buy milk from market",
            categoryId = "c1",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )
        val note2 = Note(
            id = "n2",
            title = "Random",
            content = "No category note",
            categoryId = null,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )
        val notes = listOf(note1, note2)

        // UseCases flow -> Success
        every { getNotesUseCase(any()) } returns flowOf(Result.Success(notes))
        every { observeCategoriesUseCase(any()) } returns flowOf(Result.Success(categories))

        // --- Create VM ---
        val vm = NoteListViewModel(
            getNotesUseCase = getNotesUseCase,
            observeCategoriesUseCase = observeCategoriesUseCase,
            deleteCategoryUseCase = deleteCategoryUseCase,
            deleteNoteUseCase = deleteNoteUseCase,
            noteRepository = noteRepository,
            networkMonitor = networkMonitor
        )

        // coroutine/flow
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is NotesUiState.Success)

        val success = state as NotesUiState.Success
        assertEquals(2, success.notes.size)

        // n1 kategorili -> categoryName "Work" olmalı
        val uiModelForN1 = success.notes.first { it.id == "n1" }
        assertEquals("Work", uiModelForN1.categoryName)
        assertEquals("#FF0000", uiModelForN1.categoryColorHex)

        // n2 kategorisiz -> null olmalı
        val uiModelForN2 = success.notes.first { it.id == "n2" }
        assertEquals(null, uiModelForN2.categoryName)
        assertEquals(null, uiModelForN2.categoryColorHex)
    }

    @Test
    fun `search query filters notes by title or content`() = runTest {
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

        val categories = emptyList<Category>()

        val note1 = Note(
            id = "n1",
            title = "Buy milk",
            content = "Remember to buy milk",
            categoryId = null,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )
        val note2 = Note(
            id = "n2",
            title = "Workout plan",
            content = "Leg day",
            categoryId = null,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )
        val notes = listOf(note1, note2)

        every { getNotesUseCase(any()) } returns flowOf(Result.Success(notes))
        every { observeCategoriesUseCase(any()) } returns flowOf(Result.Success(categories))

        val vm = NoteListViewModel(
            getNotesUseCase = getNotesUseCase,
            observeCategoriesUseCase = observeCategoriesUseCase,
            deleteCategoryUseCase = deleteCategoryUseCase,
            deleteNoteUseCase = deleteNoteUseCase,
            noteRepository = noteRepository,
            networkMonitor = networkMonitor
        )

        advanceUntilIdle()

        // WHEN: arama
        vm.onSearchQueryChange("milk")
        advanceUntilIdle()

        val state = vm.uiState.value as NotesUiState.Success
        assertEquals(1, state.notes.size)
        assertEquals("n1", state.notes.first().id)

        // WHEN: arama temizle
        vm.clearSearch()
        advanceUntilIdle()

        val state2 = vm.uiState.value as NotesUiState.Success
        assertEquals(2, state2.notes.size)
    }

    @Test
    fun `selecting a category filters notes by categoryId`() = runTest {
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

        val categories = listOf(Category(id = "c1", name = "Work"), Category(id = "c2", name = "Home"))

        val notes = listOf(
            Note("n1", "A", "aaa", "c1", Instant.EPOCH, Instant.EPOCH),
            Note("n2", "B", "bbb", "c2", Instant.EPOCH, Instant.EPOCH),
            Note("n3", "C", "ccc", null, Instant.EPOCH, Instant.EPOCH),
        )

        every { getNotesUseCase(any()) } returns flowOf(Result.Success(notes))
        every { observeCategoriesUseCase(any()) } returns flowOf(Result.Success(categories))

        val vm = NoteListViewModel(
            getNotesUseCase = getNotesUseCase,
            observeCategoriesUseCase = observeCategoriesUseCase,
            deleteCategoryUseCase = deleteCategoryUseCase,
            deleteNoteUseCase = deleteNoteUseCase,
            noteRepository = noteRepository,
            networkMonitor = networkMonitor
        )

        advanceUntilIdle()

        // Başta tüm notlar
        val allState = vm.uiState.value as NotesUiState.Success
        assertEquals(3, allState.notes.size)

        // WHEN: c1 seç
        vm.onCategorySelected("c1")
        advanceUntilIdle()

        val filteredState = vm.uiState.value as NotesUiState.Success
        assertEquals(1, filteredState.notes.size)
        assertEquals("n1", filteredState.notes.first().id)

        // WHEN: aynı category tekrar seç -> toggle -> filtre kalkar
        vm.onCategorySelected("c1")
        advanceUntilIdle()

        val unfilteredState = vm.uiState.value as NotesUiState.Success
        assertEquals(3, unfilteredState.notes.size)
    }
}
