package com.sap.codelab.data.memo.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MemoDao {
    @Query("SELECT * FROM memos ORDER BY id DESC")
    fun observeAll(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE isDone = 0 ORDER BY id DESC")
    fun observeOpen(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun get(id: Long): MemoEntity?

    @Query("SELECT * FROM memos WHERE isDone = 0 AND isReminded = 0 ORDER BY id DESC")
    suspend fun getActive(): List<MemoEntity>

    @Query("SELECT COUNT(*) FROM memos WHERE isDone = 0 AND isReminded = 0")
    suspend fun countActive(): Int

    /** Active memos inside a latitude/longitude box: a cheap pre-filter, the caller checks the exact distance. */
    @Query(
        """
        SELECT * FROM memos
        WHERE isDone = 0 AND isReminded = 0
          AND latitude BETWEEN :minLatitude AND :maxLatitude
          AND longitude BETWEEN :minLongitude AND :maxLongitude
        """,
    )
    suspend fun getActiveInBounds(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double,
    ): List<MemoEntity>

    /**
     * The [limit] active memos closest to a point, by squared planar distance in degrees.
     * [longitudeScale] is cos(latitude) of the point: it shrinks longitude degrees to the same length as latitude ones.
     */
    @Query(
        """
        SELECT * FROM memos
        WHERE isDone = 0 AND isReminded = 0
        ORDER BY (latitude - :latitude) * (latitude - :latitude) +
                 (longitude - :longitude) * (longitude - :longitude) * :longitudeScale * :longitudeScale
        LIMIT :limit
        """,
    )
    suspend fun getNearestActive(
        latitude: Double,
        longitude: Double,
        longitudeScale: Double,
        limit: Int,
    ): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<MemoEntity>

    @Insert
    suspend fun insert(memo: MemoEntity): Long

    @Query("UPDATE memos SET isDone = 1 WHERE id = :id")
    suspend fun markDone(id: Long): Int

    @Query("UPDATE memos SET isReminded = 1 WHERE id IN (:ids)")
    suspend fun markReminded(ids: List<Long>)
}
