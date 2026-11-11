package com.cheonsaeun.mori.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
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
    private val mutableState = MutableStateFlow(DashboardState())
    val state = mutableState.asStateFlow()
    private val emulatorSession = EmulatorSession { active ->
        mutableState.update { it.copy(simulatorActive = active) }
    }

    init { emulatorSession.start() }

    suspend fun refresh() {
        if (state.value.preview) return
        try {
            val snapshot = emulatorSession.decorate(gateway.snapshot())
            emulatorSession.record(snapshot)
            if (!state.value.preview) mutableState.update { it.copy(vehicle = snapshot) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("MoriVehicle", "Snapshot unavailable", error)
            mutableState.update { it.copy(vehicle = VehicleSnapshot()) }
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

    fun togglePreview() = mutableState.update {
        it.copy(preview = !it.preview, vehicle = if (!it.preview) PreviewSnapshot.create() else VehicleSnapshot())
    }
    fun selectSeat(id: SeatId) = mutableState.update { it.copy(selectedSeat = id) }
    fun toggleGuide() = mutableState.update { it.copy(showGuide = !it.showGuide) }

    override fun onCleared() { emulatorSession.close(); gateway.close(); super.onCleared() }
}
