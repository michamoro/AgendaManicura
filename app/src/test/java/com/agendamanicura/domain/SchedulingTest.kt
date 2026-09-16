package com.agendamanicura.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulingTest {
    @Test fun `detects intersecting appointments`() = assertTrue(overlaps(1_000, 60, 2_000, 30))
    @Test fun `allows appointments that touch`() = assertFalse(overlaps(1_000, 60, 1_000 + 60 * 60_000L, 30))
}
