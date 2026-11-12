package com.cheonsaeun.mori.model

import org.junit.Assert.*
import org.junit.Test

class UnifiedDashboardTest {
    @Test fun occupiedPeopleAreFirstAndRelationshipFilterIsStillRespected() {
        val state=DashboardFixture.state()
        val profiles=state.profileChoices()
        assertTrue(profiles.take(3).all {state.onboardSeat(it.id)!=null})
        assertTrue(profiles.drop(3).all {state.onboardSeat(it.id)==null})
        assertTrue(state.profileChoices(Relationship.FRIEND).all {it.relationship==Relationship.FRIEND})
        assertEquals(1,state.profileChoices(Relationship.FRIEND).size)
    }
    @Test fun aProfileCannotBeAssignedToTwoSeats() {
        val result=connectedAssignments(mapOf(SeatId.PASSENGER to "mom",SeatId.REAR_RIGHT to "kid"),SeatId.DRIVER,"mom")
        assertEquals("mom",result[SeatId.DRIVER]);assertFalse(result.containsKey(SeatId.PASSENGER));assertEquals("kid",result[SeatId.REAR_RIGHT])
    }
    @Test fun onlyConfirmedDepartureClearsIdentity() {
        val state=DashboardFixture.state()
        val missing=state.vehicle.copy(seats=state.vehicle.seats.map {it.copy(occupant=Signal())})
        assertEquals(state.assignments,retainedAssignments(state.assignments,state.vehicle,missing))
        val vacant=state.vehicle.copy(seats=state.vehicle.seats.map {if(it.id==SeatId.PASSENGER) it.copy(occupant=Signal(false,Availability.LIVE)) else it})
        assertFalse(retainedAssignments(state.assignments,state.vehicle,vacant).containsKey(SeatId.PASSENGER))
    }
    @Test fun sharedClimateMustMatchAllTemperatureZonesAndPower() {
        val vehicle=DashboardFixture.create()
        assertTrue(ClimatePreference(24f,2).matches(vehicle))
        assertFalse(ClimatePreference().matches(vehicle))
        val split=vehicle.copy(zones=vehicle.zones.mapIndexed {i,z -> if(i==0) z.copy(temperature=Signal(22f,Availability.LIVE)) else z})
        assertFalse(ClimatePreference(24f,2).matches(split))
        val off=vehicle.copy(zones=vehicle.zones.map {it.copy(power=Signal(false,Availability.LIVE))})
        assertFalse(ClimatePreference(24f,2).matches(off))
    }
    @Test fun freshLiveDashboardNeverAssignsKnownProfilesAutomatically() {
        val state=DashboardState(vehicle=DashboardFixture.create(),profiles=DashboardFixture.profiles())
        assertTrue(state.assignments.isEmpty());assertTrue(state.profileChoices().all {state.onboardSeat(it.id)==null})
    }
}
