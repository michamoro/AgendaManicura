package com.agendamanicura.domain

/** Shared scheduling rules; end points that touch are valid consecutive appointments. */
fun overlaps(startA: Long, durationA: Int, startB: Long, durationB: Int): Boolean {
    val endA = startA + durationA * 60_000L
    val endB = startB + durationB * 60_000L
    return startA < endB && startB < endA
}
