package com.ar.domain.note.usecase

import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.FakeNoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for GetNotesUseCase.
 *
 * Goal:
 * - Verify that the use case correctly propagates the repository result (Success/Error)
 * - Keep the test isolated from real DB/Firebase by using a Fake repository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetNotesUseCaseTest {

    // Dependencies used in tests
    private lateinit var fakeRepo: FakeNoteRepository
    private lateinit var useCase: GetNotesUseCase

    /**
     * This runs before each test.
     * We create a fresh Fake repository and a fresh UseCase instance,
     * so tests do not affect each other.
     */
    @Before
    fun setup() {
        fakeRepo = FakeNoteRepository()
        useCase = GetNotesUseCase(fakeRepo)
    }

    @Test
    fun `getNotes returns Success with the same list when repository returns Success`() = runTest {
        // GIVEN: repository returns a successful list of notes
        val notes = listOf(
            Note(
                id = "1",
                title = "Note 1",
                content = "Content 1",
                categoryId = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH
            ),
            Note(
                id = "2",
                title = "Note 2",
                content = "Content 2",
                categoryId = "cat-1",
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH
            )
        )
        fakeRepo.setNotes(notes)

        // WHEN: the use case is executed
        val result = useCase().first()

        // THEN: result should be Success
        assertTrue(result is Result.Success)

        // THEN: the returned data should match what we provided
        val data = (result as Result.Success).data
        assertEquals(2, data.size)
        assertEquals("Note 1", data.first().title)
        assertEquals("Note 2", data.last().title)
    }

    @Test
    fun `getNotes returns Error when repository returns Error`() = runTest {
        // GIVEN: repository returns an error result
        val errorMessage = "Network failed"
        fakeRepo.setError(errorMessage)

        // WHEN: the use case is executed
        val result = useCase().first()

        // THEN: result should be Error
        assertTrue(result is Result.Error)

        // THEN: error message should be the same
        val message = (result as Result.Error).message
        assertEquals(errorMessage, message)
    }

    @Test
    fun `getNotes returns Loading first when repository emits Loading first`() = runTest {
        // GIVEN: repository will emit Loading first, then Success
        val notes = listOf(
            Note(
                id = "1",
                title = "Note 1",
                content = "Content 1",
                categoryId = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH
            )
        )
        fakeRepo.setLoadingThenNotes(notes)

        // WHEN: collect only the first emission
        val firstEmission = useCase().first()

        // THEN: the first emission should be Loading
        assertTrue(firstEmission is Result.Loading)
    }


}
