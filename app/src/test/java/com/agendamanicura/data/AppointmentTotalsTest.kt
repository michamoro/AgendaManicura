package com.agendamanicura.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppointmentTotalsTest {
    @Test fun `total includes service snapshots and tip`() {
        val details = AppointmentWithDetails(AppointmentEntity(clientId = 1, startAt = 0, tipCents = 250), ClientEntity(1, "Ana"), listOf(AppointmentServiceEntity(1, 3, "Manicura", "manicure", 2_500)))
        assertEquals(2_750, details.totalCents)
    }
    @Test fun `snapshot retains original price independently from service catalogue`() {
        val bookedLine = AppointmentServiceEntity(1, 3, "Manicura", "manicure", 2_500)
        val catalogueNow = ServiceEntity(3, "Manicura", "manicure", 3_000)
        assertEquals(2_500, bookedLine.priceCents)
        assertEquals(3_000, catalogueNow.basePriceCents)
    }
}
