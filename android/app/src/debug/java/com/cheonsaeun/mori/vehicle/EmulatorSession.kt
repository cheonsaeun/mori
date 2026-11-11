package com.cheonsaeun.mori.vehicle

import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.cheonsaeun.mori.model.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Development-only lease for the official Pleos 2.1 Virtual Console emulator. */
class EmulatorSession(private val onStatus: (Boolean?) -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = "mori-debug-${Process.myPid()}"
    private var started = false
    @Volatile private var seatPositions: Map<SeatId,Int> = emptyMap()
    private var lastRecorded: VehicleSnapshot? = null

    fun start() {
        if (started) return
        started = true
        if (!Build.PRODUCT.startsWith("crp_avd")) { onStatus(null); return }
        scope.launch {
            var configured = false
            var lastHealth = 0L
            try {
                while (isActive) {
                    try {
                        request("POST", "/browser/session?client=$client")
                        if (!configured) {
                            request("PUT", "/vehicle/hvac/mode?mode=subscribe_hvac_base")
                            configured = true
                        }
                        if (SystemClock.elapsedRealtime() - lastHealth >= 2000) {
                            val state = request("GET", "/vehicle/state")
                            val hvac = state.getJSONObject("hvac")
                            val positions=state.optJSONObject("seat")?.optJSONObject("positions")?.optJSONObject("recline")
                            seatPositions=mapOf(SeatId.DRIVER to "driver",SeatId.PASSENGER to "passenger",SeatId.REAR_LEFT to "rear_left",SeatId.REAR_RIGHT to "rear_right").mapNotNull { (id,key) ->
                                if(positions!=null && positions.has(key)) id to positions.getInt(key) else null
                            }.toMap()
                            if (isActive) onStatus(hvac.optBoolean("emulator_active") && hvac.optJSONObject("publisher_ready")?.optBoolean("any") == true)
                            lastHealth = SystemClock.elapsedRealtime()
                        }
                    } catch (_: Exception) {
                        if (isActive) onStatus(false)
                        configured = false
                        seatPositions = emptyMap()
                    }
                    delay(800)
                }
            } finally {
                withContext(NonCancellable) {
                    runCatching { request("POST", "/browser/session?client=$client&closing=1") }
                }
            }
        }
    }

    private fun request(method: String, path: String): JSONObject {
        val connection = URL("http://127.0.0.1:8088$path").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 700
            connection.readTimeout = 700
            connection.useCaches = false
            check(connection.responseCode in 200..299) { "Emulator session unavailable" }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }

    fun decorate(snapshot: VehicleSnapshot): VehicleSnapshot {
        val positions=seatPositions
        return snapshot.copy(seats=snapshot.seats.map { seat ->
            positions[seat.id]?.let { position -> seat.copy(backrest=Signal(position,Availability.LIVE,SignalSource.EMULATOR)) } ?: seat
        })
    }

    fun record(snapshot: VehicleSnapshot) {
        val stable=snapshot.copy(updatedAt=null)
        if(stable!=lastRecorded) {
            lastRecorded=stable
            Log.i("MoriState",stable.toString())
        }
    }

    suspend fun send(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(Build.PRODUCT.startsWith("crp_avd"))
            require(path.startsWith("/vehicle/hvac?") || path.startsWith("/vehicle/seat?"))
            val response=request("PUT", path)
            if(path.startsWith("/vehicle/seat?")) {
                val positions=response.optJSONObject("positions")?.optJSONObject("recline")
                if(positions!=null) seatPositions=mapOf(SeatId.DRIVER to "driver",SeatId.PASSENGER to "passenger",SeatId.REAR_LEFT to "rear_left",SeatId.REAR_RIGHT to "rear_right").mapNotNull { (id,key) ->
                    if(positions.has(key)) id to positions.getInt(key) else null
                }.toMap()
            }
            Unit
        }
    }

    fun close() { scope.cancel() }
}
