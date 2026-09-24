package com.agendamanicura.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<ClientEntity>>
    @Query("SELECT * FROM clients ORDER BY id") suspend fun all(): List<ClientEntity>
    @Insert suspend fun insert(client: ClientEntity): Long
    @Update suspend fun update(client: ClientEntity)
    @Delete suspend fun delete(client: ClientEntity)
    @Query("DELETE FROM clients") suspend fun deleteAll()
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY sortOrder, name COLLATE NOCASE") fun observeAll(): Flow<List<ServiceEntity>>
    @Query("SELECT * FROM services ORDER BY sortOrder, name COLLATE NOCASE") suspend fun all(): List<ServiceEntity>
    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM services") suspend fun nextSortOrder(): Long
    @Insert suspend fun insert(service: ServiceEntity): Long
    @Update suspend fun update(service: ServiceEntity)
    @Delete suspend fun delete(service: ServiceEntity)
    @Query("DELETE FROM services") suspend fun deleteAll()
}

@Dao
interface AppointmentDao {
    @Transaction @Query("SELECT * FROM appointments WHERE startAt >= :from AND startAt < :until ORDER BY startAt") fun observeBetween(from: Long, until: Long): Flow<List<AppointmentWithDetails>>
    @Transaction @Query("SELECT * FROM appointments WHERE id = :id") suspend fun getById(id: Long): AppointmentWithDetails?
    @Transaction @Query("SELECT * FROM appointments ORDER BY startAt") suspend fun all(): List<AppointmentWithDetails>
    @Transaction @Query("SELECT * FROM appointments WHERE status = 'PAID' AND startAt >= :from AND startAt < :until ORDER BY startAt DESC") fun observePaidBetween(from: Long, until: Long): Flow<List<AppointmentWithDetails>>
    @Query("SELECT COUNT(*) FROM appointments WHERE id != :excludeId AND status != 'CANCELLED' AND ABS(startAt - :startAt) < :minimumGapMillis") suspend fun minimumGapCount(startAt: Long, minimumGapMillis: Long, excludeId: Long = -1): Int
    @Insert suspend fun insert(appointment: AppointmentEntity): Long
    @Update suspend fun update(appointment: AppointmentEntity)
    @Delete suspend fun delete(appointment: AppointmentEntity)
    @Query("DELETE FROM appointment_services WHERE appointmentId = :appointmentId") suspend fun clearServices(appointmentId: Long)
    @Insert suspend fun insertServices(items: List<AppointmentServiceEntity>)
    @Query("DELETE FROM appointments") suspend fun deleteAll()
}
