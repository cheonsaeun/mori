package com.cheonsaeun.mori.ui.preview

import com.cheonsaeun.mori.model.*

/** An isolated, editable family scenario. Never persisted or sent to the vehicle. */
object PreviewSnapshot {
    private fun <T> sample(value: T) = Signal(value, Availability.LIVE)
    fun profiles() = listOf(
        PersonProfile("example-dad","아빠",SeatPreference(28,48,55,0,2),ClimatePreference(22f,3),MediaPreference(false,"재즈"),Relationship.FAMILY),
        PersonProfile("example-mom","엄마",SeatPreference(34,40,60,2,0),ClimatePreference(24f,2),MediaPreference(false,"어쿠스틱"),Relationship.FAMILY),
        PersonProfile("example-daughter","서연",SeatPreference(backrest=30,heat=1),ClimatePreference(),MediaPreference(false,"팝"),Relationship.FAMILY),
        PersonProfile("example-grandma","할머니",SeatPreference(backrest=35,heat=2),ClimatePreference(25f,1),MediaPreference(true,"클래식 라디오"),Relationship.FAMILY),
        PersonProfile("example-friend","민지",SeatPreference(backrest=32),ClimatePreference(23f,2),MediaPreference(false,"인디"),Relationship.FRIEND),
        PersonProfile("example-coworker","지훈",media=MediaPreference(false,"클래식"),relationship=Relationship.COWORKER),
    )
    fun state() = DashboardState(vehicle=create(),profiles=profiles(),assignments=mapOf(SeatId.DRIVER to "example-dad",SeatId.PASSENGER to "example-mom",SeatId.REAR_RIGHT to "example-daughter"),media=MediaPreference(false,"재즈"),preview=true)
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
