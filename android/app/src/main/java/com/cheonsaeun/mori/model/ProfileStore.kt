package com.cheonsaeun.mori.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** App-private preferences. No names or preferences leave this device. */
class ProfileStore(context: Context, preferenceName: String = "mori_profiles_v1") {
    private val storage = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    companion object { private val importLock=Any() }
    fun load(): List<PersonProfile> = synchronized(importLock) {
        val existing=runCatching { readProfiles() }.getOrElse { return@synchronized emptyList() }
        if(storage.getBoolean("starter_profiles_imported_v1",false)) return@synchronized existing
        val merged=importStarterProfiles(existing)
        if(storage.edit().putString("profiles",encode(merged)).putBoolean("starter_profiles_imported_v1",true).commit()) merged else existing
    }
    private fun readProfiles(): List<PersonProfile> {
        val array = JSONArray(storage.getString("profiles", "[]"))
        return (0 until array.length()).map { i ->
            val p = array.getJSONObject(i)
            fun int(key: String) = if(p.isNull(key)) null else p.getInt(key)
            PersonProfile(p.getString("id"), p.getString("name"), SeatPreference(int("backrest"), int("slide"), int("height"), int("heat"), int("ventilation")), ClimatePreference(if(p.isNull("temperature")) null else p.getDouble("temperature").toFloat(), int("fan")), MediaPreference(p.optBoolean("radio"), p.optString("selection")), Relationship.entries.firstOrNull {it.name==p.optString("relationship")} ?: Relationship.UNSPECIFIED)
        }
    }
    fun save(profiles: List<PersonProfile>): Boolean = storage.edit().putString("profiles",encode(profiles)).commit()
    private fun encode(profiles: List<PersonProfile>): String = JSONArray().apply {
        profiles.forEach { p -> put(JSONObject().apply {
            put("relationship",p.relationship.name); put("id",p.id); put("name",p.name)
            put("backrest",p.seat.backrest); put("slide",p.seat.slide); put("height",p.seat.height)
            put("heat",p.seat.heat); put("ventilation",p.seat.ventilation)
            put("temperature",p.climate.temperature); put("fan",p.climate.fan)
            put("radio",p.media.radio); put("selection",p.media.selection)
        }) }
    }.toString()
}
