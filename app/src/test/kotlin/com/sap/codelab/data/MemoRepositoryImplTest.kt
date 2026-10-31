package com.sap.codelab.data

import android.database.SQLException
import app.cash.turbine.test
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.data.memo.MemoRepositoryImpl
import com.sap.codelab.data.memo.local.MemoDao
import com.sap.codelab.data.memo.local.MemoEntity
import com.sap.codelab.data.memo.mapper.toData
import com.sap.codelab.data.location.GeoPointData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemoRepositoryImplTest {

    private val dao = mockk<MemoDao>()
    private val repository = MemoRepositoryImpl(dao)
    private val entity = MemoEntity(7, "Title", "Text", 42.0, 23.0, isDone = false, isReminded = false)

    @Test
    fun `observes open memos as data models`() = runTest {
        every { dao.observeOpen() } returns flowOf(listOf(entity))

        repository.observeMemos(includeDone = false).test {
            val memos = (awaitItem() as AppResult.Success).data
            assertEquals(listOf(7L), memos.map { it.id })
            assertEquals(42.0, memos.single().latitude)
            awaitComplete()
        }
    }

    @Test
    fun `a missing memo is NotFound`() = runTest {
        coEvery { dao.get(1) } returns null
        assertEquals(AppResult.Failure(AppError.Local.NotFound), repository.getMemo(1))
    }

    @Test
    fun `marking an unknown memo done is NotFound`() = runTest {
        coEvery { dao.markDone(1) } returns 0
        assertEquals(AppResult.Failure(AppError.Local.NotFound), repository.markDone(1))
    }

    @Test
    fun `SQLite failures become Database errors`() = runTest {
        coEvery { dao.insert(any()) } throws SQLException("disk I/O error")

        val result = repository.addMemo(entity.toData())

        assertInstanceOf(AppError.Local.Database::class.java, (result as AppResult.Failure).error)
    }

    @Test
    fun `reads and updates go through the dao`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(entity))
        coEvery { dao.get(7) } returns entity
        coEvery { dao.getActive() } returns listOf(entity)
        coEvery { dao.getByIds(listOf(7)) } returns listOf(entity)
        coEvery { dao.markDone(7) } returns 1
        coEvery { dao.markReminded(listOf(7)) } returns Unit

        repository.observeMemos(includeDone = true).test {
            assertEquals(AppResult.Success(listOf(entity.toData())), awaitItem())
            awaitComplete()
        }
        assertEquals(AppResult.Success(entity.toData()), repository.getMemo(7))
        assertEquals(AppResult.Success(listOf(entity.toData())), repository.getActiveMemos())
        assertEquals(AppResult.Success(listOf(entity.toData())), repository.getMemos(listOf(7)))
        assertEquals(AppResult.Success(Unit), repository.markDone(7))
        assertEquals(AppResult.Success(Unit), repository.markReminded(listOf(7)))
    }

    @Test
    fun `failures while reading are returned`() = runTest {
        coEvery { dao.get(1) } throws IllegalStateException("closed")
        coEvery { dao.markDone(1) } throws IllegalStateException("closed")
        assertInstanceOf(AppError.Local.Unknown::class.java, (repository.getMemo(1) as AppResult.Failure).error)
        assertInstanceOf(AppError.Local.Unknown::class.java, (repository.markDone(1) as AppResult.Failure).error)
    }

    @Test
    fun `new memos are inserted with a generated id`() = runTest {
        coEvery { dao.insert(match { it.id == 0L }) } returns 12
        val result = repository.addMemo(entity.toData())
        assertEquals(AppResult.Success(12L), result)
    }

    @Test
    fun `nearby query asks for a bounding box around the point`() = runTest {
        val bounds = List(4) { slot<Double>() }
        coEvery {
            dao.getActiveInBounds(capture(bounds[0]), capture(bounds[1]), capture(bounds[2]), capture(bounds[3]))
        } returns listOf(entity)

        val result = repository.getActiveMemosNear(GeoPointData(42.0, 23.0), radiusMeters = 1_000.0)

        assertEquals(listOf(7L), (result as AppResult.Success).data.map { it.id })
        val (minLat, maxLat, minLng, maxLng) = bounds.map { it.captured }
        assertTrue(minLat < 42.0 && maxLat > 42.0 && maxLat - minLat > 2 * 1_000.0 / 111_320.0)
        assertTrue(minLng < 23.0 && maxLng > 23.0)
        assertTrue(maxLng - minLng > maxLat - minLat) // a degree of longitude is shorter than one of latitude
    }

    @Test
    fun `nearby query drops the longitude bound across the antimeridian`() = runTest {
        val bounds = List(4) { slot<Double>() }
        coEvery {
            dao.getActiveInBounds(capture(bounds[0]), capture(bounds[1]), capture(bounds[2]), capture(bounds[3]))
        } returns emptyList()

        repository.getActiveMemosNear(GeoPointData(10.0, 179.999), radiusMeters = 1_000.0)

        assertEquals(-180.0, bounds[2].captured)
        assertEquals(180.0, bounds[3].captured)
    }

    @Test
    fun `nearest query scales longitude by the cosine of the latitude`() = runTest {
        coEvery { dao.getNearestActive(60.0, 10.0, any(), 100) } returns listOf(entity)

        val result = repository.getNearestActiveMemos(GeoPointData(60.0, 10.0), limit = 100)

        assertEquals(listOf(7L), (result as AppResult.Success).data.map { it.id })
        coVerify { dao.getNearestActive(60.0, 10.0, match { kotlin.math.abs(it - 0.5) < 1e-9 }, 100) }
    }

    @Test
    fun `counts active memos`() = runTest {
        coEvery { dao.countActive() } returns 3
        assertEquals(AppResult.Success(3), repository.countActiveMemos())
    }
}
