package com.cheonsaeun.mori.vehicle

import com.cheonsaeun.mori.model.*
import org.junit.Assert.*
import org.junit.Test

class SeatComfortTest {
    private fun live(value: Int)=Signal(value,Availability.LIVE)

    @Test fun negativeTemperatureMeansVentilation() {
        val (heat,vent)=normalizeSeatComfort(live(-2),live(0),false)
        assertEquals(0,heat.value)
        assertEquals(2,vent.value)
        assertEquals(Availability.LIVE,vent.availability)
    }
    @Test fun combinedHeatingClearsVentilation() {
        val (heat,vent)=normalizeSeatComfort(live(3),live(2),true)
        assertEquals(3,heat.value)
        assertEquals(0,vent.value)
    }
    @Test fun dedicatedVentilationRemainsIndependent() {
        val (_,vent)=normalizeSeatComfort(live(0),live(2),false)
        assertEquals(2,vent.value)
    }
    @Test fun missingTemperatureIsNotOff() {
        val unavailable=Signal<Int>(availability=Availability.UNAVAILABLE)
        val (heat,vent)=normalizeSeatComfort(unavailable,live(1),true)
        assertNull(heat.value)
        assertEquals(Availability.UNAVAILABLE,heat.availability)
        assertEquals(1,vent.value)
    }
}
