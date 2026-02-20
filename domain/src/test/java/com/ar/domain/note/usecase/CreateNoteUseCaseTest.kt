package com.ar.domain.note.usecase

import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.FakeNoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class CreateNoteUseCaseTest {

    private lateinit var fakeRepo: FakeNoteRepository
    private lateinit var useCase: CreateNoteUseCase

    @Before
    fun setup() {
        fakeRepo = FakeNoteRepository()
        useCase = CreateNoteUseCase(fakeRepo)
    }

    @Test
    fun `createNote adds note into repository`() = runTest {
        // GIVEN
        val note = Note(
            id = "1",
            title = "Hello",
            content = "World",
            categoryId = null,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )

        // WHEN
        useCase(note)

        // THEN
        // Fake repo has one note
        assertEquals(true, fakeRepo.hasAnyNotesLocally())
    }
}
