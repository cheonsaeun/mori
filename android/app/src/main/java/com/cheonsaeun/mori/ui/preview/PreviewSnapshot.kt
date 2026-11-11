package com.cheonsaeun.mori.ui.preview

import com.cheonsaeun.mori.model.*

/** Explicit design fixture; never used as a fallback for unavailable vehicle data. */
object PreviewSnapshot {
    private fun <T> sample(value: T) = Signal(value, Availability.LIVE)
    fun create() = VehicleSnapshot(
        seats = listOf(
            SeatSnapshot(SeatId.DRIVER, sample(true), sample(28), sample(0), sample(2), "나"),
            SeatSnapshot(SeatId.PASSENGER, sample(true), sample(34), sample(2), sample(0), "민지"),
            SeatSnapshot(SeatId.REAR_LEFT, sample(false), sample(24), sample(0), sample(0)),
            SeatSnapshot(SeatId.REAR_RIGHT, sample(true), sample(30), sample(1), sample(0), "준호"),
        ),
        zones = listOf(
            ClimateZone("운전석 구역", sample(22f), sample(2), sample(true)),
            ClimateZone("조수석 구역", sample(23.5f), sample(2), sample(true)),
        ),
        received = true,
    )
}
