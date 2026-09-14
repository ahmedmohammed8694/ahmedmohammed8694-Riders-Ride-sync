package com.ridesync.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPing(entity: BufferedTelemetryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPings(entities: List<BufferedTelemetryEntity>)

    @Query("SELECT * FROM buffered_telemetry ORDER BY timestamp ASC LIMIT :batchSize")
    suspend fun getFifoBatch(batchSize: Int = 50): List<BufferedTelemetryEntity>

    @Query("DELETE FROM buffered_telemetry WHERE id IN (:ids)")
    suspend fun deletePingsByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM buffered_telemetry")
    fun getBufferedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM buffered_telemetry")
    suspend fun getBufferedCount(): Int

    @Query("DELETE FROM buffered_telemetry")
    suspend fun clearAll()
}
