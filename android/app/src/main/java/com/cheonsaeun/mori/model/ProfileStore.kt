package com.cheonsaeun.mori.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** App-private preferences. No names or preferences leave this device. */
class ProfileStore(context: Context) {
    private val storage = context.getSharedPreferences("mori_profiles_v1", Context.MODE_PRIVATE)
    fun load(): List<PersonProfile> = runCatching {
        val array = JSONArray(storage.getString("profiles", "[]"))
        (0 until array.length()).map { i ->
            val p = array.getJSONObject(i)
            fun int(key: String) = if(p.isNull(key)) null else p.getInt(key)
            PersonProfile(p.getString("id"), p.getString("name"), SeatPreference(int("backrest"), int("slide"), int("height"), int("heat"), int("ventilation")), ClimatePreference(if(p.isNull("temperature")) null else p.getDouble("temperature").toFloat(), int("fan")), MediaPreference(p.optBoolean("radio"), p.optString("selection")), Relationship.entries.firstOrNull {it.name==p.optString("relationship")} ?: Relationship.UNSPECIFIED)
        }
    }.getOrDefault(emptyList())
    fun save(profiles: List<PersonProfile>): Boolean = storage.edit().putString("profiles", JSONArray().apply {
        profiles.forEach { p -> put(JSONObject().apply {
            put("relationship",p.relationship.name); put("id",p.id); put("name",p.name)
            put("backrest",p.seat.backrest); put("slide",p.seat.slide); put("height",p.seat.height)
            put("heat",p.seat.heat); put("ventilation",p.seat.ventilation)
            put("temperature",p.climate.temperature); put("fan",p.climate.fan)
            put("radio",p.media.radio); put("selection",p.media.selection)
        }) }
    }.toString()).commit()
}
