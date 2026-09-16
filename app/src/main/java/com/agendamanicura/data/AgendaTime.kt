package com.agendamanicura.data

import java.time.LocalDate
import java.time.ZoneId

object AgendaTime {
    val zone: ZoneId = ZoneId.of("Europe/Madrid")
    fun today(): LocalDate = LocalDate.now(zone)
}
