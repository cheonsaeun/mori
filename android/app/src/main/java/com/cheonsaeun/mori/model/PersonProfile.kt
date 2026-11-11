package com.cheonsaeun.mori.model

/** Null means leave this setting unchanged, never an invented default. */
data class SeatPreference(val backrest: Int? = null, val slide: Int? = null, val height: Int? = null, val heat: Int? = null, val ventilation: Int? = null) {
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
data class PersonProfile(val id: String, val name: String, val seat: SeatPreference = SeatPreference(), val climate: ClimatePreference = ClimatePreference(), val media: MediaPreference = MediaPreference())
