package com.cheonsaeun.mori.model

import org.junit.Assert.*
import org.junit.Test

class ProfilePreferenceTest {
    private fun live(value: Int) = Signal(value, Availability.LIVE)
    @Test fun unavailableSignalsDoNotOverwriteSavedPreferences() {
        val saved=SeatPreference(backrest=35,slide=40,height=60,heat=2,ventilation=0)
        assertEquals(saved,SeatPreference.capture(SeatSnapshot(SeatId.DRIVER),saved))
    }
    @Test fun newProfileNeverInventsValuesForUnsupportedSignals() {
        assertEquals(SeatPreference(),SeatPreference.capture(SeatSnapshot(SeatId.REAR_LEFT)))
    }
    @Test fun manualChangeIsDetectedWhileUnknownValueIsNotCalledAChange() {
        val saved=SeatPreference(backrest=35,heat=2)
        assertTrue(saved.differsFrom(SeatSnapshot(SeatId.DRIVER,backrest=live(50),heat=live(2))))
        assertFalse(saved.differsFrom(SeatSnapshot(SeatId.DRIVER)))
        assertFalse(saved.matches(SeatSnapshot(SeatId.DRIVER)))
    }
    @Test fun readbackRequiresEveryRequestedValue() {
        val saved=SeatPreference(backrest=35,slide=40)
        assertFalse(saved.matches(SeatSnapshot(SeatId.DRIVER,backrest=live(35))))
        assertTrue(saved.matches(SeatSnapshot(SeatId.DRIVER,backrest=live(35),slide=live(40))))
        assertFalse(SeatPreference().matches(SeatSnapshot(SeatId.DRIVER)))
    }
}
