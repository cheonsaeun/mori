package com.cheonsaeun.mori.model

enum class Availability { WAITING, LIVE, UNSUPPORTED, UNAVAILABLE }

enum class SignalSource { SDK, EMULATOR }

data class Signal<T>(val value: T? = null, val availability: Availability = Availability.WAITING, val source: SignalSource = SignalSource.SDK) {
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
    val slide: Signal<Int> = Signal(availability = Availability.UNSUPPORTED),
    val height: Signal<Int> = Signal(availability = Availability.UNSUPPORTED),
)

data class ClimateZone(
    val label: String,
    val temperature: Signal<Float> = Signal(availability = Availability.UNSUPPORTED),
    val fan: Signal<Int> = Signal(availability = Availability.UNSUPPORTED),
    val power: Signal<Boolean> = Signal(availability = Availability.UNSUPPORTED),
)

data class ClimateMode(val label: String, val enabled: Signal<Boolean>)

data class VehicleSnapshot(
    val seats: List<SeatSnapshot> = SeatId.entries.map { SeatSnapshot(it) },
    val zones: List<ClimateZone> = emptyList(),
    val received: Boolean = false,
    val updatedAt: Long? = null,
    val modes: List<ClimateMode> = emptyList(),
)

data class DashboardState(
    val vehicle: VehicleSnapshot = VehicleSnapshot(),
    val profiles: List<PersonProfile> = emptyList(),
    val assignments: Map<SeatId, String> = emptyMap(),
    val media: MediaPreference = MediaPreference(),
    val selectedSeat: SeatId = SeatId.DRIVER,
    val simulatorActive: Boolean? = null,
    val controlBusy: Boolean = false,
    val controlFeedback: String? = null,
)
