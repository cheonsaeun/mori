package com.cheonsaeun.mori.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cheonsaeun.mori.vehicle.VehicleGateway
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VehicleCheckState(
    val loading: Boolean = false,
    val title: String = "차량과 연결을 확인해 보세요",
    val detail: String = "조회 버튼을 누르면 운전석 문 상태를 읽어 옵니다.",
    val success: Boolean = false,
)

class MoriViewModel(application: Application) : AndroidViewModel(application) {
    private val gateway = VehicleGateway(application)
    private val mutableState = MutableStateFlow(VehicleCheckState())
    val state = mutableState.asStateFlow()
    private var requestId = 0
    private var timeout: Job? = null

    fun checkVehicle() {
        if (mutableState.value.loading) return
        val currentRequest = ++requestId
        mutableState.value = VehicleCheckState(loading = true, title = "차량 상태를 확인하고 있어요", detail = "Pleos에서 응답을 기다리고 있습니다.")
        timeout = viewModelScope.launch {
            delay(15_000)
            if (requestId == currentRequest) {
                requestId++
                mutableState.value = VehicleCheckState(title = "응답을 기다리는 시간이 길어졌어요", detail = "에뮬레이터가 완전히 켜졌는지 확인한 뒤 다시 시도해 주세요.")
            }
        }
        gateway.readDriverDoor { result ->
            viewModelScope.launch {
                if (requestId != currentRequest) return@launch
                timeout?.cancel()
                requestId++
                mutableState.value = result.fold(
                    onSuccess = { position ->
                        if (position == null) {
                            VehicleCheckState(title = "현재 문 상태를 확인할 수 없어요", detail = "차량에서 값이 제공되지 않았습니다. 잠시 후 다시 시도해 주세요.")
                        } else {
                            VehicleCheckState(
                                title = if (position == 0) "운전석 문이 닫혀 있어요" else "운전석 문이 열려 있어요",
                                detail = "차량에서 받은 문 위치 값: $position. 조회 시점의 상태입니다.",
                                success = true,
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.w("MoriVehicle", "Door read failed", error)
                        VehicleCheckState(title = "차량 상태를 읽지 못했어요", detail = "에뮬레이터 연결과 차량 조회 권한을 확인한 뒤 다시 시도해 주세요.")
                    },
                )
            }
        }
    }

    override fun onCleared() {
        requestId++
        timeout?.cancel()
        gateway.close()
        super.onCleared()
    }
}
