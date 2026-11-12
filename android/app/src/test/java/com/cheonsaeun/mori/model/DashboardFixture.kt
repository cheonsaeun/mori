package com.cheonsaeun.mori.model

import com.cheonsaeun.mori.model.*

/** Unit-test vehicle fixture, excluded from all app variants. */
object DashboardFixture {
    private fun <T> sample(value: T) = Signal(value, Availability.LIVE)
    fun profiles() = StarterProfiles.profiles()
    fun state() = DashboardState(vehicle=create(),profiles=profiles(),assignments=mapOf(SeatId.DRIVER to "example-dad",SeatId.PASSENGER to "example-mom",SeatId.REAR_RIGHT to "example-daughter"),media=MediaPreference(false,"재즈"))
    fun create() = VehicleSnapshot(
        seats=listOf(
            SeatSnapshot(SeatId.DRIVER,sample(true),sample(28),sample(0),sample(2),slide=sample(48),height=sample(55)),
            SeatSnapshot(SeatId.PASSENGER,sample(true),sample(34),sample(2),sample(0),slide=sample(40),height=sample(60)),
            SeatSnapshot(SeatId.REAR_LEFT,sample(false),sample(24),sample(0),Signal(availability=Availability.UNSUPPORTED)),
            SeatSnapshot(SeatId.REAR_RIGHT,sample(true),sample(30),sample(1),Signal(availability=Availability.UNSUPPORTED)),
        ),
        zones=listOf(ClimateZone("운전석 구역",sample(24f),power=sample(true)),ClimateZone("조수석 구역",sample(24f),power=sample(true)),ClimateZone("앞좌석 공유 구역",fan=sample(2))),
        received=true,
    )
}
