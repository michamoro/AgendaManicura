package com.agendamanicura.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormattingTest {
    @Test fun `parses both Spanish and decimal point prices`() {
        assertEquals(2_550, cents("25,50"))
        assertEquals(2_550, cents("25.50"))
    }

    @Test fun `formats cents as Spanish currency`() {
        assertEquals("25,50 €", 2_550L.money())
    }
}
