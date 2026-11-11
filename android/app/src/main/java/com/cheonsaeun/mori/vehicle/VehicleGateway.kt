package com.cheonsaeun.mori.vehicle

import android.content.Context
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue

/** Read-only entry point. This starter never sends vehicle control commands. */
class VehicleGateway(context: Context) {
    private val vehicle = Vehicle(context.applicationContext)
    private var initialized = false

    fun readDriverDoor(onResult: (Result<Int?>) -> Unit) {
        try {
            if (!initialized) {
                vehicle.initialize()
                initialized = true
            }
            val door = vehicle.getDoor()
            val area = DoorArea(DoorValue.ROW_1_LEFT)
            door.checkDoorPositionCapabilities(
                onComplete = { config, supportedAreas ->
                    if (config.accessType.name !in setOf("READ", "READ_WRITE") || area !in supportedAreas) {
                        onResult(Result.failure(IllegalStateException("Driver door read unsupported")))
                    } else {
                        try {
                            door.getCurrentDoorPosition(
                                area = area,
                                onComplete = { value -> onResult(Result.success(value)) },
                                onFailed = { error -> onResult(Result.failure(error)) },
                            )
                        } catch (error: Exception) {
                            onResult(Result.failure(error))
                        }
                    }
                },
                onFailed = { error -> onResult(Result.failure(error)) },
            )
        } catch (error: Exception) {
            onResult(Result.failure(error))
        }
    }

    fun close() {
        if (initialized) {
            runCatching { vehicle.release() }
            initialized = false
        }
    }
}
