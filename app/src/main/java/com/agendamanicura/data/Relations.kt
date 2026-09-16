package com.agendamanicura.data

import androidx.room.Embedded
import androidx.room.Relation

data class AppointmentWithDetails(
    @Embedded val appointment: AppointmentEntity,
    @Relation(parentColumn = "clientId", entityColumn = "id") val client: ClientEntity,
    @Relation(parentColumn = "id", entityColumn = "appointmentId") val services: List<AppointmentServiceEntity>
) {
    val serviceTotalCents get() = services.sumOf { it.priceCents }
    val totalCents get() = serviceTotalCents + appointment.tipCents
}
