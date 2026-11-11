package com.cheonsaeun.mori.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.cheonsaeun.mori.model.*
import com.cheonsaeun.mori.ui.preview.PreviewSnapshot
import com.cheonsaeun.mori.vehicle.VehicleGateway
import com.cheonsaeun.mori.vehicle.EmulatorSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MoriViewModel(application: Application) : AndroidViewModel(application) {
    private val gateway = VehicleGateway(application)
    private val store = ProfileStore(application)
    private val mutableState = MutableStateFlow(DashboardState(profiles = store.load()))
    private var liveState: DashboardState? = null
    private val refreshLock = Mutex()
    val state = mutableState.asStateFlow()
    private val emulatorSession = EmulatorSession { active ->
        mutableState.update { if(it.preview) it else it.copy(simulatorActive = active) }
    }

    init { emulatorSession.start() }

    suspend fun refresh() = refreshLock.withLock {
        if (state.value.preview) return@withLock
        try {
            val snapshot = emulatorSession.decorate(gateway.snapshot())
            emulatorSession.record(snapshot)
            if (!state.value.preview) mutableState.update { it.copy(vehicle = snapshot,assignments=retainedAssignments(it.assignments,it.vehicle,snapshot)) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("MoriVehicle", "Snapshot unavailable", error)
            mutableState.update { if(it.preview) it else it.copy(vehicle = VehicleSnapshot()) }
        }
    }

    fun sendEmulatorCommand(path: String) {
        if (state.value.preview || state.value.controlBusy || state.value.simulatorActive!=true) return
        viewModelScope.launch {
            mutableState.update { it.copy(controlBusy=true, controlFeedback=null) }
            val result=emulatorSession.send(path)
            mutableState.update { it.copy(controlBusy=false, controlFeedback=if(result.isSuccess) "명령을 전송했어요. 반영 결과를 대시보드에서 확인해 주세요." else "명령을 보내지 못했어요. 시뮬레이터 연결을 확인해 주세요.") }
            if(result.isSuccess) refresh()
        }
    }

    fun togglePreview() {
        if(state.value.controlBusy) return
        if(state.value.preview) {
            mutableState.value=liveState?.copy(controlFeedback=null) ?: DashboardState(profiles=store.load())
            liveState=null
        } else {
            liveState=state.value
            mutableState.value=PreviewSnapshot.state()
        }
    }
    fun clearFeedback() = mutableState.update {it.copy(controlFeedback=null)}
    fun selectSeat(id: SeatId) = mutableState.update { it.copy(selectedSeat = id) }
    fun toggleGuide() = mutableState.update { it.copy(showGuide = !it.showGuide) }


    fun selectTab(common: Boolean) = mutableState.update { it.copy(commonTab=common, controlFeedback=null) }
    fun saveProfile(profile: PersonProfile, bind: Boolean = false, onSaved: () -> Unit = {}) {
        if(state.value.controlBusy || profile.name.isBlank()) return
        val target=state.value.selectedSeat
        viewModelScope.launch {
            mutableState.update { it.copy(controlBusy=true) }
            val clean=profile.copy(name=profile.name.trim().take(20))
            val profiles=if(state.value.profiles.any {it.id==profile.id}) state.value.profiles.map {if(it.id==profile.id) clean else it} else state.value.profiles + clean
            val saved=if(state.value.preview) true else withContext(Dispatchers.IO) { store.save(profiles) }
            mutableState.update { it.copy(controlBusy=false, profiles=if(saved) profiles else it.profiles, assignments=if(saved && bind) connectedAssignments(it.assignments,target,profile.id) else it.assignments, controlFeedback=if(saved) "${if(it.preview) "예시에서 " else ""}프로필을 저장했어요." else "저장하지 못했어요. 다시 시도해 주세요.") }
            if(saved) onSaved()
        }
    }
    fun removeProfile(id: String) {
        if(state.value.controlBusy) return
        viewModelScope.launch {
            mutableState.update { it.copy(controlBusy=true) }
            val profiles=state.value.profiles.filterNot { it.id==id }
            val saved=if(state.value.preview) true else withContext(Dispatchers.IO) { store.save(profiles) }
            mutableState.update { it.copy(controlBusy=false, profiles=if(saved) profiles else it.profiles, assignments=if(saved) it.assignments.filterValues { v -> v!=id } else it.assignments, controlFeedback=if(saved) "프로필을 삭제했어요." else "삭제하지 못했어요.") }
        }
    }
    fun clearAssignment(seat: SeatId = state.value.selectedSeat) {
        if(state.value.controlBusy) return
        mutableState.update {it.copy(assignments=it.assignments-seat,controlFeedback="게스트로 변경했어요. 차량 설정은 그대로 유지해요.")}
    }
    fun setClimatePower(enabled: Boolean) {
        if(state.value.controlBusy) return
        if(state.value.preview) mutableState.update {it.copy(vehicle=it.vehicle.copy(zones=it.vehicle.zones.map {zone -> if(zone.power.value!=null) zone.copy(power=zone.power.copy(value=enabled)) else zone}),controlFeedback="예시 공조를 ${if(enabled) "켰어요" else "껐어요"}.")}
        else sendEmulatorCommand("/vehicle/hvac?power=${if(enabled) 1 else 0}")
    }
    fun applyMedia(profile: PersonProfile) {
        if(state.value.controlBusy || profile.media.selection.isBlank()) return
        mutableState.update { it.copy(media=profile.media,controlFeedback="${profile.name}의 미디어 선호를 선택했어요. 음원 재생은 연결 전이에요.") }
    }
    fun applyProfile(profile: PersonProfile, climateOnly: Boolean = false, target: SeatId = state.value.selectedSeat, connect: Boolean = true) {
        val current=state.value
        if(current.controlBusy) return
        if(current.preview) {
            val vehicle=if(climateOnly) current.vehicle.copy(zones=current.vehicle.zones.map { zone -> zone.copy(
                temperature=if(zone.temperature.value!=null && profile.climate.temperature!=null) zone.temperature.copy(value=profile.climate.temperature) else zone.temperature,
                fan=if(zone.fan.value!=null && profile.climate.fan!=null) zone.fan.copy(value=profile.climate.fan) else zone.fan,
                power=if(zone.power.value!=null) zone.power.copy(value=true) else zone.power,
            ) }) else current.vehicle.copy(seats=current.vehicle.seats.map { seat -> if(seat.id!=target) seat else {
                fun merge(signal: Signal<Int>, value: Int?) = if(signal.value!=null && value!=null) signal.copy(value=value) else signal
                seat.copy(backrest=merge(seat.backrest,profile.seat.backrest),slide=merge(seat.slide,profile.seat.slide),height=merge(seat.height,profile.seat.height),heat=merge(seat.heat,profile.seat.heat),ventilation=merge(seat.ventilation,profile.seat.ventilation))
            } })
            mutableState.update {it.copy(vehicle=vehicle,assignments=if(!climateOnly && connect) connectedAssignments(it.assignments,target,profile.id) else it.assignments,controlFeedback="예시 시나리오에 반영했어요. 실제 차량은 변경하지 않아요.")}
            return
        }
        if(current.simulatorActive!=true) { mutableState.update { it.copy(controlFeedback="연결된 차량 시뮬레이터에서 적용할 수 있어요.") }; return }
        val live=current.vehicle.seats.first { it.id==target }
        val area=when(target) { SeatId.DRIVER -> "driver"; SeatId.PASSENGER -> "passenger"; SeatId.REAR_LEFT -> "rear_left"; SeatId.REAR_RIGHT -> "rear_right" }
        val commands=mutableListOf<String>()
        var skipped=0
        if(climateOnly) {
            profile.climate.temperature?.let { value ->
                listOf("driver" to "운전석 구역", "passenger" to "조수석 구역").forEach { (key,label) ->
                    if(current.vehicle.zones.any { it.label==label && it.temperature.value!=null }) commands += "/vehicle/hvac?power=1&${key}_temp=${value.coerceIn(16f,30f)}" else skipped++
                }
            }
            profile.climate.fan?.let { value -> if(current.vehicle.zones.any {it.fan.value!=null}) commands += "/vehicle/hvac?power=1&fan=${value.coerceIn(0,8)}" else skipped++ }
        } else {
            listOf(Triple("recline",profile.seat.backrest,live.backrest.value),Triple("slide",profile.seat.slide,live.slide.value),Triple("height",profile.seat.height,live.height.value)).forEach { (axis,value,available) ->
                if(value!=null) { if(available!=null) commands += "/vehicle/seat?kind=position&control=$axis&area=$area&value=${value.coerceIn(0,100)}" else skipped++ }
            }
            if(profile.seat.heat!=null || profile.seat.ventilation!=null) {
                if(target==SeatId.DRIVER || target==SeatId.PASSENGER) {
                    val heat=profile.seat.heat ?: live.heat.value
                    val vent=profile.seat.ventilation ?: live.ventilation.value
                    val level=when { (profile.seat.ventilation ?: 0)>0 -> -profile.seat.ventilation!!; (profile.seat.heat ?: 0)>0 -> profile.seat.heat ?: 0; (vent ?: 0)>0 -> -vent!!; else -> heat ?: 0 }
                    commands += "/vehicle/hvac?${area}_seat=${level.coerceIn(-3,3)}"
                } else skipped++
            }
        }
        viewModelScope.launch {
            mutableState.update { it.copy(controlBusy=true, controlFeedback="설정을 적용하고 있어요…") }
            var failed=0
            commands.forEach { if(emulatorSession.send(it).isFailure) failed++ }
            fun readbackMatches(): Boolean {
                val after=state.value.vehicle
                return if(climateOnly) {
                    val temps=after.zones.mapNotNull {it.temperature.value}
                    (profile.climate.temperature==null || temps.isNotEmpty() && temps.all { kotlin.math.abs(it-profile.climate.temperature)<.1f }) && (profile.climate.fan==null || after.zones.any {it.fan.value==profile.climate.fan})
                } else profile.seat.matches(after.seats.first {it.id==target})
            }
            var verified=false
            for(attempt in 0 until 8) {
                delay(500); refresh()
                if(readbackMatches()) {verified=true;break}
            }
            mutableState.update { it.copy(controlBusy=false, assignments=if(!climateOnly && connect && !(live.occupant.value==true && it.vehicle.seats.first {s -> s.id==target}.occupant.value==false)) connectedAssignments(it.assignments,target,profile.id) else it.assignments, controlFeedback=when {
                commands.isEmpty() -> if(climateOnly) "적용 가능한 공조 설정이 없어요." else "프로필을 연결했어요. 적용 가능한 좌석 설정이 없어요."
                failed>0 -> "일부 설정을 보내지 못했어요. 현재 상태를 확인해 주세요."
                skipped>0 -> "지원 항목을 요청했어요. 일부 항목은 미지원이거나 확인이 필요해요."
                verified -> "${profile.name}의 ${if(climateOnly) "공조" else "좌석"} 설정 반영을 확인했어요."
                else -> "설정을 요청했어요. 일부 항목의 반영은 아직 확인되지 않았어요."
            }) }
        }
    }

    override fun onCleared() { emulatorSession.close(); gateway.close(); super.onCleared() }
}
