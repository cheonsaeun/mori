package com.cheonsaeun.mori.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.cheonsaeun.mori.model.*
import com.cheonsaeun.mori.ui.preview.PreviewSnapshot
import com.cheonsaeun.mori.vehicle.VehicleGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MoriViewModel(application: Application) : AndroidViewModel(application) {
    private val gateway = VehicleGateway(application)
    private val mutableState = MutableStateFlow(DashboardState())
    val state = mutableState.asStateFlow()

    suspend fun refresh() {
        if (state.value.preview) return
        try {
            val snapshot = gateway.snapshot()
            if (!state.value.preview) mutableState.update { it.copy(vehicle = snapshot) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("MoriVehicle", "Snapshot unavailable", error)
            mutableState.update { it.copy(vehicle = VehicleSnapshot()) }
        }
    }

    fun togglePreview() = mutableState.update {
        it.copy(preview = !it.preview, vehicle = if (!it.preview) PreviewSnapshot.create() else VehicleSnapshot())
    }
    fun selectSeat(id: SeatId) = mutableState.update { it.copy(selectedSeat = id) }
    fun toggleGuide() = mutableState.update { it.copy(showGuide = !it.showGuide) }

    override fun onCleared() { gateway.close(); super.onCleared() }
}
