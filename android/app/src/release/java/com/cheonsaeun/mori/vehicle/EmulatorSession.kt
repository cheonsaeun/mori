package com.cheonsaeun.mori.vehicle

import com.cheonsaeun.mori.model.VehicleSnapshot

/** No emulator endpoints, networking or keep-alive in the release variant. */
class EmulatorSession(private val onStatus: (Boolean?) -> Unit) {
    fun start() { onStatus(null) }
    fun decorate(snapshot: VehicleSnapshot) = snapshot
    @Suppress("UNUSED_PARAMETER")
    fun record(snapshot: VehicleSnapshot) = Unit
    @Suppress("UNUSED_PARAMETER")
    suspend fun send(path: String): Result<Unit> = Result.failure(UnsupportedOperationException("Emulator controls are debug-only"))
    fun close() = Unit
}
