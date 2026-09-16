package com.agendamanicura.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<ClientEntity>>
    @Insert suspend fun insert(client: ClientEntity): Long
    @Update suspend fun update(client: ClientEntity)
    @Delete suspend fun delete(client: ClientEntity)
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<ServiceEntity>>
    @Insert suspend fun insert(service: ServiceEntity): Long
    @Update suspend fun update(service: ServiceEntity)
    @Delete suspend fun delete(service: ServiceEntity)
}

@Dao
interface AppointmentDao {
    @Transaction @Query("SELECT * FROM appointments WHERE startAt >= :from AND startAt < :until ORDER BY startAt") fun observeBetween(from: Long, until: Long): Flow<List<AppointmentWithDetails>>
    @Transaction @Query("SELECT * FROM appointments WHERE id = :id") suspend fun getById(id: Long): AppointmentWithDetails?
    @Transaction @Query("SELECT * FROM appointments WHERE status = 'PAID' AND startAt >= :from AND startAt < :until ORDER BY startAt DESC") fun observePaidBetween(from: Long, until: Long): Flow<List<AppointmentWithDetails>>
    @Query("SELECT COUNT(*) FROM appointments WHERE id != :excludeId AND status != 'CANCELLED' AND startAt < :endAt AND (startAt + durationMinutes * 60000) > :startAt") suspend fun overlappingCount(startAt: Long, endAt: Long, excludeId: Long = -1): Int
    @Insert suspend fun insert(appointment: AppointmentEntity): Long
    @Update suspend fun update(appointment: AppointmentEntity)
    @Query("DELETE FROM appointment_services WHERE appointmentId = :appointmentId") suspend fun clearServices(appointmentId: Long)
    @Insert suspend fun insertServices(items: List<AppointmentServiceEntity>)
}
