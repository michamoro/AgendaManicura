package com.agendamanicura.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulingTest {
    @Test fun `blocks appointments less than fifteen minutes apart`() = assertTrue(hasMinimumAppointmentGap(1_000, 1_000 + 14 * 60_000L))
    @Test fun `allows appointments exactly fifteen minutes apart`() = assertFalse(hasMinimumAppointmentGap(1_000, 1_000 + 15 * 60_000L))
}
