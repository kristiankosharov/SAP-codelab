package com.sap.codelab.data.memo

import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.GeoPointData
import com.sap.codelab.data.memo.local.MemoDao
import com.sap.codelab.data.memo.local.asDbResult
import com.sap.codelab.data.memo.local.safeDbCall
import com.sap.codelab.data.memo.mapper.toData
import com.sap.codelab.data.memo.mapper.toEntity
import javax.inject.Inject
import kotlin.math.cos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Deliberately below the shortest real degree of latitude (about 110.6 km), so the bounding box errs on the big side. */
private const val METERS_PER_DEGREE = 110_000.0
private const val FULL_LONGITUDE = 180.0
private const val MIN_LONGITUDE_SCALE = 0.01

internal class MemoRepositoryImpl @Inject constructor(private val dao: MemoDao) : MemoRepository {

    override fun observeMemos(includeDone: Boolean): Flow<AppResult<List<MemoData>>> =
        (if (includeDone) dao.observeAll() else dao.observeOpen())
            .map { memos -> memos.map { it.toData() } }
            .asDbResult()

    override suspend fun getMemo(id: Long): AppResult<MemoData> =
        when (val result = safeDbCall { dao.get(id) }) {
            is AppResult.Success -> result.data?.let { AppResult.Success(it.toData()) }
                ?: AppResult.Failure(AppError.Local.NotFound)
            is AppResult.Failure -> result
        }

    override suspend fun getActiveMemos(): AppResult<List<MemoData>> =
        safeDbCall { dao.getActive().map { it.toData() } }

    override suspend fun countActiveMemos(): AppResult<Int> = safeDbCall { dao.countActive() }

    /** The box is a square of about 2 x radius, so it holds the circle plus its corners (up to radius * sqrt(2) away):
        callers still check the exact distance on the few rows returned. The cos is taken at the center only, which is
        accurate at reminder range (hundreds of meters). **/
    override suspend fun getActiveMemosNear(
        center: GeoPointData,
        radiusMeters: Double,
    ): AppResult<List<MemoData>> {
        // A bounding box in degrees around the point, so SQL can pre-filter with plain comparisons.
        // Latitude: one degree is about the same length everywhere, so meters convert to degrees directly.
        val latitudeDelta = radiusMeters / METERS_PER_DEGREE
        // Longitude: meridians converge towards the poles, so a degree of longitude is only cos(latitude) as long
        // as a degree of latitude. Covering the same distance takes 1 / cos(latitude) times more degrees.
        val longitudeScale = cos(Math.toRadians(center.latitude))
        val longitudeDelta = if (longitudeScale < MIN_LONGITUDE_SCALE) FULL_LONGITUDE else latitudeDelta / longitudeScale
        val minLongitude = center.longitude - longitudeDelta
        val maxLongitude = center.longitude + longitudeDelta
        // Near a pole or the antimeridian (longitude jumps from 180 to -180) one BETWEEN cannot describe the box:
        // drop the longitude bound instead. It returns a few extra rows, never misses one.
        val wraps = minLongitude < -FULL_LONGITUDE || maxLongitude > FULL_LONGITUDE
        return safeDbCall {
            dao.getActiveInBounds(
                minLatitude = center.latitude - latitudeDelta,
                maxLatitude = center.latitude + latitudeDelta,
                minLongitude = if (wraps) -FULL_LONGITUDE else minLongitude,
                maxLongitude = if (wraps) FULL_LONGITUDE else maxLongitude,
            ).map { it.toData() }
        }
    }

    override suspend fun getNearestActiveMemos(
        center: GeoPointData,
        limit: Int,
    ): AppResult<List<MemoData>> =
        safeDbCall {
            dao.getNearestActive(center.latitude, center.longitude, cos(Math.toRadians(center.latitude)), limit)
                .map { it.toData() }
        }

    override suspend fun getMemos(ids: List<Long>): AppResult<List<MemoData>> =
        safeDbCall { dao.getByIds(ids).map { it.toData() } }

    override suspend fun addMemo(memo: MemoData): AppResult<Long> =
        safeDbCall { dao.insert(memo.copy(id = 0).toEntity()) }

    override suspend fun markDone(id: Long): AppResult<Unit> =
        when (val result = safeDbCall { dao.markDone(id) }) {
            is AppResult.Success -> if (result.data > 0) AppResult.Success(Unit) else AppResult.Failure(AppError.Local.NotFound)
            is AppResult.Failure -> result
        }

    override suspend fun markReminded(ids: List<Long>): AppResult<Unit> =
        safeDbCall { dao.markReminded(ids) }
}
