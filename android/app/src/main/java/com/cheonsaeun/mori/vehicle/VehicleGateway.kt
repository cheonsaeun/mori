package com.cheonsaeun.mori.vehicle

import android.content.Context
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import com.cheonsaeun.mori.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Read-only adapter. Areas are taken from capabilities, never assumed per feature. */
class VehicleGateway(context: Context) {
    private val vehicle = Vehicle(context.applicationContext)
    private var initialized = false
    private val capabilities = mutableMapOf<String, Set<SeatArea>>()
    private val seatAreas = mapOf(
        SeatId.DRIVER to SeatArea(SeatValue.ROW_1_LEFT),
        SeatId.PASSENGER to SeatArea(SeatValue.ROW_1_RIGHT),
        SeatId.REAR_LEFT to SeatArea(SeatValue.ROW_2_LEFT),
        SeatId.REAR_RIGHT to SeatArea(SeatValue.ROW_2_RIGHT),
    )

    private suspend fun <T> read(call: ((T?) -> Unit, (Exception) -> Unit) -> Unit): Signal<T> =
        withTimeoutOrNull(1800) {
            suspendCancellableCoroutine { continuation ->
                try {
                    call({ value ->
                        if (continuation.isActive) continuation.resume(
                            Signal(value, if (value == null) Availability.UNAVAILABLE else Availability.LIVE)
                        )
                    }, { _ ->
                        if (continuation.isActive) continuation.resume(Signal(availability = Availability.UNAVAILABLE))
                    })
                } catch (_: Exception) {
                    if (continuation.isActive) continuation.resume(Signal(availability = Availability.UNAVAILABLE))
                }
            }
        } ?: Signal(availability = Availability.UNAVAILABLE)

    private suspend fun areas(key: String, call: ((Set<SeatArea>?) -> Unit, (Exception) -> Unit) -> Unit): Set<SeatArea>? {
        capabilities[key]?.let { return it }
        return read(call).value?.also { capabilities[key] = it }
    }

    private suspend fun <T> inArea(supported: Set<SeatArea>?, area: SeatArea, call: ((T?) -> Unit, (Exception) -> Unit) -> Unit): Signal<T> = when {
        supported == null -> Signal(availability = Availability.UNAVAILABLE)
        area !in supported -> Signal(availability = Availability.UNSUPPORTED)
        else -> read(call)
    }

    suspend fun snapshot(): VehicleSnapshot = coroutineScope {
        if (!initialized) { vehicle.initialize(); initialized = true }
        val seat = vehicle.getSeat()
        val hvac = vehicle.getHVAC()
        fun readable(name: String) = name == "READ" || name == "READ_WRITE"
        val occupancy = async { areas("occupancy") { ok, fail -> seat.checkSeatOccupancyCapabilities({ config, list -> ok(if (readable(config.accessType.name)) list.toSet() else emptySet()) }, fail) } }
        val backrest = async { areas("backrest") { ok, fail -> seat.checkSeatBackrestAnglePositionCapabilities({ config, map -> ok(if (readable(config.accessType.name)) map.keys else emptySet()) }, fail) } }
        val heat = async { areas("heat") { ok, fail -> seat.checkSeatTemperatureCapabilities({ config, map -> ok(if (readable(config.accessType.name)) map.keys else emptySet()) }, fail) } }
        val vent = async { areas("vent") { ok, fail -> seat.checkSeatVentilationCapabilities({ config, map -> ok(if (readable(config.accessType.name)) map.keys else emptySet()) }, fail) } }
        val temperature = async { areas("temperature") { ok, fail -> hvac.checkHvacTemperatureCapabilities({ config, map, _ -> ok(if (readable(config.accessType.name)) map.keys else emptySet()) }, fail) } }
        val fan = async { areas("fan") { ok, fail -> hvac.checkHvacFanSpeedCapabilities({ config, map -> ok(if (readable(config.accessType.name)) map.keys else emptySet()) }, fail) } }
        val power = async { areas("power") { ok, fail -> hvac.checkHvacPowerCapabilities({ config, list -> ok(if (readable(config.accessType.name)) list.toSet() else emptySet()) }, fail) } }
        val oc = occupancy.await(); val ba = backrest.await(); val he = heat.await(); val ve = vent.await()
        val te = temperature.await(); val fa = fan.await(); val po = power.await()
        val seats = seatAreas.map { (id, area) -> async {
            val o = async { inArea<Boolean>(oc, area) { ok, fail -> seat.getCurrentSeatOccupancy(area, ok, fail) } }
            val b = async { inArea<Int>(ba, area) { ok, fail -> seat.getCurrentSeatBackrestAnglePosition(area, ok, fail) } }
            val h = async { inArea<Int>(he, area) { ok, fail -> seat.getHvacSeatTemperature(area, ok, fail) } }
            val v = async { inArea<Int>(ve, area) { ok, fail -> seat.getHvacSeatVentilationState(area, ok, fail) } }
            SeatSnapshot(id, o.await(), b.await(), h.await(), v.await())
        } }
        val zones = ((te ?: emptySet()) + (fa ?: emptySet()) + (po ?: emptySet())).sortedBy { it.values.sumOf { v -> v.id } }.map { area -> async {
            val t = async { inArea<Float>(te, area) { ok, fail -> hvac.getHvacTemperature(area, TemperatureUnit.CELSIUS, ok, fail) } }
            val f = async { inArea<Int>(fa, area) { ok, fail -> hvac.getHvacFanSpeed(area, ok, fail) } }
            val p = async { inArea<Boolean>(po, area) { ok, fail -> hvac.getHvacPowerState(area, ok, fail) } }
            ClimateZone(zoneLabel(area), t.await(), f.await(), p.await())
        } }
        val result = seats.map { it.await() }
        val climate = zones.map { it.await() }
        val received = result.any { s -> listOf(s.occupant.availability, s.backrest.availability, s.heat.availability, s.ventilation.availability).any { it == Availability.LIVE } } || climate.any { it.temperature.availability == Availability.LIVE || it.fan.availability == Availability.LIVE || it.power.availability == Availability.LIVE }
        VehicleSnapshot(result, climate, received, if (received) System.currentTimeMillis() else null)
    }

    private fun zoneLabel(area: SeatArea): String {
        val values = area.values.toSet()
        return when {
            values.size == 1 -> when (values.first()) {
                SeatValue.ROW_1_LEFT -> "운전석 구역"
                SeatValue.ROW_1_RIGHT -> "조수석 구역"
                SeatValue.ROW_2_LEFT -> "2열 왼쪽 구역"
                SeatValue.ROW_2_RIGHT -> "2열 오른쪽 구역"
                else -> "좌석 구역 ${values.first().name}"
            }
            values == setOf(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT) -> "앞좌석 공유 구역"
            else -> "공유 구역 (${values.size}좌석)"
        }
    }

    fun close() {
        if (initialized) runCatching { vehicle.release() }
        initialized = false
        capabilities.clear()
    }
}
