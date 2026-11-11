package com.cheonsaeun.mori

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.cheonsaeun.mori.design.MoriTheme
import com.cheonsaeun.mori.model.*
import com.cheonsaeun.mori.ui.*
import java.io.File

/** Local emulator integration checks, without adding a network test dependency. */
class ProfileInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private fun await(message: String, condition: () -> Boolean) {
        val deadline=System.currentTimeMillis()+15000
        while(!condition()) { check(System.currentTimeMillis()<deadline) {message}; Thread.sleep(100) }
    }
    private fun screenshot(name: String) {
        waitForIdleSync(); Thread.sleep(1800)
        val bitmap=checkNotNull(uiAutomation.takeScreenshot()) { "Screenshot unavailable: $name" }
        File(targetContext.filesDir,"$name.png").outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()

    }
    override fun onStart() {
        val result=Bundle()
        val store=ProfileStore(targetContext)
        val backup=store.load()
        var activity: MainActivity?=null
        var model: MoriViewModel?=null
        var original: VehicleSnapshot?=null
        try {
            activity=startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
            runOnMainSync { model=ViewModelProvider(activity)[MoriViewModel::class.java] }
            val vm=model!!
            await("Simulator not ready") {vm.state.value.simulatorActive==true && vm.state.value.vehicle.seats.first().slide.value!=null}
            original=vm.state.value.vehicle
            val profile=PersonProfile("mori-integration-only","테스트 프로필",SeatPreference(backrest=41,slide=32,height=58),ClimatePreference(24f,3),MediaPreference(false,"인디"))
            runOnMainSync {vm.saveProfile(profile)}
            await("Profile save failed") {vm.state.value.profiles.any {it.id==profile.id} && !vm.state.value.controlBusy}
            check(store.load().first {it.id==profile.id}==profile) {"Stored profile differs"}
            runOnMainSync {vm.applyProfile(profile); check(vm.state.value.controlBusy) {"Apply did not start: ${vm.state.value.controlFeedback}"}}
            await("Seat apply timed out") {!vm.state.value.controlBusy}
            check(profile.seat.matches(vm.state.value.vehicle.seats.first())) {"Live seat readback mismatch: ${vm.state.value.vehicle.seats.first()}, feedback=${vm.state.value.controlFeedback}"}
            check(vm.state.value.assignments[SeatId.DRIVER]==profile.id)
            screenshot("seats-profiles")
            runOnMainSync {vm.applyProfile(profile,true)}
            await("Climate apply timed out") {!vm.state.value.controlBusy}
            check(vm.state.value.vehicle.zones.filter {it.temperature.value!=null}.all {it.temperature.value==24f})
            check(vm.state.value.vehicle.zones.any {it.fan.value==3})
            runOnMainSync {vm.applyMedia(profile);vm.selectTab(true)}
            check(vm.state.value.media.selection=="인디")
            screenshot("common-profiles")
            runOnMainSync {activity.setContent {MoriApp(vm,true)}}
            screenshot("night-common")
            for((width,height) in listOf(1256 to 800,648 to 800)) {
                runOnMainSync {activity.setContent { Box(Modifier.requiredSize(width.dp,height.dp)) {MoriApp(vm)} }}
                screenshot("layout-${width}x$height")
            }
            runOnMainSync {activity.setContent {MoriApp(vm)}}
            screenshot("common-profiles")
            val editorState=vm.state.value
            runOnMainSync {activity.setContent { MoriTheme { Column(Modifier.requiredSize(700.dp,900.dp)) {ProfilePane(editorState,vm,profile,true,{_,_ ->},Modifier.fillMaxSize())} } }}
            screenshot("profile-editor")
            val edited=profile.copy(name="프로필 수정 확인",media=MediaPreference(true,"FM 테스트"))
            runOnMainSync {vm.saveProfile(edited)}
            await("Profile update failed") {!vm.state.value.controlBusy && vm.state.value.profiles.any {it==edited}}
            check(store.load().count {it.id==profile.id}==1)
            runOnMainSync {vm.removeProfile(profile.id)}
            await("Profile deletion failed") {!vm.state.value.controlBusy && vm.state.value.profiles.none {it.id==profile.id}}
            check(store.load().none {it.id==profile.id})
            result.putString("result","PASS: persistence, update, deletion, three seat axes readback, HVAC readback, media selection, compact/portrait/editor and night rendering")
        } catch(error: Throwable) {
            result.putString("failure",error.stackTraceToString())
        } finally {
            store.save(backup)
            val vm=model
            val before=original
            if(vm!=null && before!=null) {
                fun send(path: String) {runOnMainSync {vm.sendEmulatorCommand(path)};await("Restore timeout") {!vm.state.value.controlBusy}}
                runCatching {
                    val seat=before.seats.first()
                    listOf("recline" to seat.backrest.value,"slide" to seat.slide.value,"height" to seat.height.value).forEach {(axis,value) -> if(value!=null) send("/vehicle/seat?kind=position&control=$axis&area=driver&value=$value")}
                    before.zones.forEach {zone -> zone.temperature.value?.let { t -> val key=if(zone.label=="운전석 구역") "driver" else "passenger";send("/vehicle/hvac?${key}_temp=$t")}}
                    before.zones.firstNotNullOfOrNull {it.fan.value}?.let {send("/vehicle/hvac?fan=$it")}
                    before.zones.firstNotNullOfOrNull {it.power.value}?.let {send("/vehicle/hvac?power=${if(it) 1 else 0}")}
                }.onFailure {result.putString("restoreWarning",it.message)}
                runOnMainSync {vm.clearAssignment();vm.selectTab(false);activity?.setContent {MoriApp(vm)}}
            }
        }
        finish(if(result.containsKey("failure")) Activity.RESULT_CANCELED else Activity.RESULT_OK,result)
    }
}
