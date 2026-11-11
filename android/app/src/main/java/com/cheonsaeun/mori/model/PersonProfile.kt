package com.cheonsaeun.mori.model

/** Null means leave this setting unchanged, never an invented default. */
data class SeatPreference(val backrest: Int? = null, val slide: Int? = null, val height: Int? = null, val heat: Int? = null, val ventilation: Int? = null) {
    val configured: Boolean get() = listOf(backrest,slide,height,heat,ventilation).any {it!=null}
    fun differsFrom(seat: SeatSnapshot): Boolean = listOf(backrest to seat.backrest.value, slide to seat.slide.value, height to seat.height.value, heat to seat.heat.value, ventilation to seat.ventilation.value).any { (saved, live) -> saved != null && live != null && saved != live }
    fun matches(seat: SeatSnapshot): Boolean = listOf(backrest to seat.backrest.value, slide to seat.slide.value, height to seat.height.value, heat to seat.heat.value, ventilation to seat.ventilation.value).filter { it.first != null }.let { values -> values.isNotEmpty() && values.all { it.first == it.second } }
    companion object {
        fun capture(seat: SeatSnapshot, previous: SeatPreference = SeatPreference()) = SeatPreference(seat.backrest.value ?: previous.backrest, seat.slide.value ?: previous.slide, seat.height.value ?: previous.height, seat.heat.value ?: previous.heat, seat.ventilation.value ?: previous.ventilation)
    }
}
data class ClimatePreference(val temperature: Float? = null, val fan: Int? = null) {
    val configured: Boolean get() = temperature != null || fan != null
}
data class MediaPreference(val radio: Boolean = false, val selection: String = "")
data class PersonProfile(val id: String, val name: String, val seat: SeatPreference = SeatPreference(), val climate: ClimatePreference = ClimatePreference(), val media: MediaPreference = MediaPreference(), val relationship: Relationship = Relationship.UNSPECIFIED)

enum class Relationship(val label: String) { UNSPECIFIED("미지정"), FAMILY("가족"), FRIEND("친구"), COWORKER("동료"), OTHER("기타") }

fun DashboardState.onboardSeat(profileId: String): SeatId? = assignments.entries.firstOrNull { (seat,id) -> id==profileId && vehicle.seats.any {it.id==seat && it.occupant.value==true} }?.key
fun DashboardState.profileChoices(relationship: Relationship? = null): List<PersonProfile> = profiles.filter { relationship==null || it.relationship==relationship }.sortedWith(compareBy<PersonProfile> {onboardSeat(it.id)==null}.thenBy {it.name}.thenBy {it.id})
fun connectedAssignments(current: Map<SeatId,String>, seat: SeatId, profileId: String) = current.filterValues {it!=profileId} + (seat to profileId)
fun ClimatePreference.matches(vehicle: VehicleSnapshot): Boolean {
    if(!configured || vehicle.zones.any {it.power.value==false}) return false
    val temperatures=vehicle.zones.mapNotNull {it.temperature.value}
    return (temperature==null || temperatures.isNotEmpty() && temperatures.all {kotlin.math.abs(it-temperature)<.1f}) && (fan==null || vehicle.zones.any {it.fan.value==fan})
}

/** Only a confirmed occupied -> vacant transition clears an explicitly chosen identity. */
fun retainedAssignments(assignments: Map<SeatId,String>, before: VehicleSnapshot, after: VehicleSnapshot): Map<SeatId,String> = assignments.filterKeys {id -> !(before.seats.firstOrNull {it.id==id}?.occupant?.value==true && after.seats.firstOrNull {it.id==id}?.occupant?.value==false)}
