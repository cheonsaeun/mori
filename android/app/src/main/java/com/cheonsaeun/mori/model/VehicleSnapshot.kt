package com.cheonsaeun.mori.model

enum class Availability { WAITING, LIVE, UNSUPPORTED, UNAVAILABLE }

data class Signal<T>(val value: T? = null, val availability: Availability = Availability.WAITING) {
    fun label(format: (T) -> String): String = value?.let(format) ?: when (availability) {
        Availability.WAITING -> "확인 중"
        Availability.UNSUPPORTED -> "미지원"
        else -> "확인 불가"
    }
}

enum class SeatId(val label: String) {
    DRIVER("운전석"), PASSENGER("조수석"), REAR_LEFT("2열 왼쪽"), REAR_RIGHT("2열 오른쪽")
}

data class SeatSnapshot(
    val id: SeatId,
    val occupant: Signal<Boolean> = Signal(),
    val backrest: Signal<Int> = Signal(),
    val heat: Signal<Int> = Signal(),
    val ventilation: Signal<Int> = Signal(),
    val displayName: String? = null,
)

data class ClimateZone(
    val label: String,
    val temperature: Signal<Float> = Signal(availability = Availability.UNSUPPORTED),
    val fan: Signal<Int> = Signal(availability = Availability.UNSUPPORTED),
    val power: Signal<Boolean> = Signal(availability = Availability.UNSUPPORTED),
)

data class VehicleSnapshot(
    val seats: List<SeatSnapshot> = SeatId.entries.map { SeatSnapshot(it) },
    val zones: List<ClimateZone> = emptyList(),
    val received: Boolean = false,
    val updatedAt: Long? = null,
)

data class DashboardState(
    val vehicle: VehicleSnapshot = VehicleSnapshot(),
    val preview: Boolean = false,
    val selectedSeat: SeatId = SeatId.DRIVER,
    val showGuide: Boolean = false,
)
