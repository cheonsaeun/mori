package com.cheonsaeun.mori.model

import org.junit.Assert.*
import org.junit.Test

class StarterProfilesTest {
    @Test fun importPreservesExistingEditsAndAddsEachMissingProfileOnce() {
        val edited=StarterProfiles.profiles().first().copy(name="수정한 이름",seat=SeatPreference(backrest=45))
        val own=PersonProfile("user-created","기존 프로필")
        val imported=importStarterProfiles(listOf(edited,own))
        assertEquals(edited,imported.first {it.id==edited.id})
        assertTrue(imported.contains(own))
        assertEquals(7,imported.size)
        assertEquals(imported,importStarterProfiles(imported))
    }
}
