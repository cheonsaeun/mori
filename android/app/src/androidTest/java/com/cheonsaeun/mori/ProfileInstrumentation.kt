package com.cheonsaeun.mori

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
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
@android.annotation.TargetApi(30)
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
    // Compose semantics avoids the car OS accessibility tree omitting embedded app windows.
    private fun semanticNodes(): List<SemanticsNode> {
        val result=mutableListOf<SemanticsNode>()
        fun walk(node: SemanticsNode) {result+=node;node.children.forEach(::walk)}
        fun views(view: View) {
            if(view.javaClass.name=="androidx.compose.ui.platform.AndroidComposeView") {
                val owner=view.javaClass.getMethod("getSemanticsOwner").invoke(view) as SemanticsOwner
                walk(owner.rootSemanticsNode)
            } else if(view is ViewGroup) for(i in 0 until view.childCount) views(view.getChildAt(i))
        }
        WindowInspector.getGlobalWindowViews().lastOrNull {it.isShown}?.let(::views)
        return result
    }
    private fun nodes(text: String): List<SemanticsNode> {
        var found=emptyList<SemanticsNode>()
        runOnMainSync {found=semanticNodes().filter {node -> node.config.getOrNull(SemanticsProperties.Text)?.any {it.text.contains(text)}==true || node.config.getOrNull(SemanticsProperties.ContentDescription)?.any {it.contains(text)}==true}}
        return found
    }
    private fun clickText(text: String) {
        waitForIdleSync();Thread.sleep(500)
        var clicked=false
        runOnMainSync {
            val matches=semanticNodes().filter {node -> node.config.getOrNull(SemanticsProperties.Text)?.any {it.text.trim()==text}==true || node.config.getOrNull(SemanticsProperties.ContentDescription)?.any {it==text}==true}
            for(node in matches) {
                var target: SemanticsNode?=node
                while(target!=null && target.config.getOrNull(SemanticsActions.OnClick)==null) target=target.parent
                if(target!=null && !target.config.contains(SemanticsProperties.Disabled) && target.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()==true) {clicked=true;break}
            }
        }
        check(clicked) {"UI click missing: $text"}
        waitForIdleSync();Thread.sleep(500)
    }
    private fun inputName(value: String) {
        var changed=false
        runOnMainSync {changed=semanticNodes().firstOrNull {it.config.getOrNull(SemanticsActions.SetText)!=null}?.config?.getOrNull(SemanticsActions.SetText)?.action?.invoke(AnnotatedString(value))==true}
        check(changed) {"Profile name input missing"}
        waitForIdleSync();Thread.sleep(500)
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
            val profile=PersonProfile("mori-integration-only","테스트 프로필",SeatPreference(backrest=41,slide=32,height=58),ClimatePreference(24f,3),MediaPreference(false,"인디"),Relationship.COWORKER)
            runOnMainSync {vm.saveProfile(profile)}
            await("Profile save failed") {vm.state.value.profiles.any {it.id==profile.id} && !vm.state.value.controlBusy}
            check(store.load().first {it.id==profile.id}==profile) {"Stored profile differs"}
            runOnMainSync {vm.applyProfile(profile); check(vm.state.value.controlBusy) {"Apply did not start: ${vm.state.value.controlFeedback}"}}
            await("Seat apply timed out") {!vm.state.value.controlBusy}
            check(profile.seat.matches(vm.state.value.vehicle.seats.first())) {"Live seat readback mismatch: ${vm.state.value.vehicle.seats.first()}, feedback=${vm.state.value.controlFeedback}"}
            check(vm.state.value.assignments[SeatId.DRIVER]==profile.id)
            screenshot("unified-live")
            runOnMainSync {vm.applyProfile(profile,true)}
            await("Climate apply timed out") {!vm.state.value.controlBusy}
            check(vm.state.value.vehicle.zones.filter {it.temperature.value!=null}.all {it.temperature.value==24f})
            check(vm.state.value.vehicle.zones.any {it.fan.value==3})
            runOnMainSync {vm.applyMedia(profile);vm.selectTab(true)}
            check(vm.state.value.media.selection=="인디")
            val liveProfiles=vm.state.value.profiles
            val liveAssignments=vm.state.value.assignments
            val actualSeats=vm.state.value.vehicle.seats
            runOnMainSync {vm.clearAssignment()}
            check(vm.state.value.vehicle.seats==actualSeats) {"Guest changed live vehicle settings"}
            check(vm.state.value.assignments[SeatId.DRIVER]==null)
            runOnMainSync {vm.togglePreview()}
            check(vm.state.value.profiles.count {it.relationship==Relationship.FAMILY}>=4)
            check(store.load()==liveProfiles) {"Example profiles leaked to persistent storage"}
            screenshot("unified-family")
            runOnMainSync {vm.applyProfile(PersonProfile("","직접 조정",seat=SeatPreference(backrest=36)),target=SeatId.DRIVER,connect=false)}
            clickText("지금 상태 저장")
            clickText("아빠 프로필에 덮어쓰기")
            await("Overwrite failed") {vm.state.value.profiles.first {it.id=="example-dad"}.seat.backrest==36}
            check(vm.state.value.profiles.first {it.id=="example-dad"}.media.selection=="재즈")
            runOnMainSync {vm.applyProfile(PersonProfile("","직접 조정",seat=SeatPreference(backrest=42)),target=SeatId.DRIVER,connect=false)}
            clickText("저장 설정 복원")
            screenshot("unified-restore")
            clickText("저장 설정 적용")
            check(vm.state.value.vehicle.seats.first().backrest.value==36)
            clickText("다른 취향으로 바꾸기")
            clickText("아빠: 이 공조 선택")
            check(!vm.state.value.profiles.first {it.id=="example-dad"}.climate.matches(vm.state.value.vehicle))
            clickText("확인")
            check(vm.state.value.profiles.first {it.id=="example-dad"}.climate.matches(vm.state.value.vehicle))

            clickText("프로필 관리")
            clickText("친구")
            check(nodes("민지").isNotEmpty())
            check(nodes("엄마").isEmpty())
            screenshot("unified-profile-filter")
            clickText("닫기")
            clickText("탑승자 변경")
            screenshot("unified-occupant")
            clickText("게스트로 이용")
            check(vm.state.value.assignments[SeatId.DRIVER]==null)
            clickText("프로필로 저장")
            clickText("새 프로필로 저장")
            inputName("예시 게스트")
            clickText("닫기")
            check(nodes("작성을 그만둘까요?").isNotEmpty())
            clickText("취소")
            screenshot("unified-profile-editor")
            clickText("저장")
            await("Example save failed") {vm.state.value.profiles.any {it.name=="예시 게스트"}}
            check(store.load()==liveProfiles)
            val mom=vm.state.value.profiles.first {it.id=="example-mom"}
            runOnMainSync {vm.applyProfile(mom,target=SeatId.DRIVER)}
            check(vm.state.value.assignments[SeatId.DRIVER]==mom.id)
            check(vm.state.value.assignments.values.count {it==mom.id}==1)
            runOnMainSync {vm.applyProfile(mom,true)}
            check(mom.climate.matches(vm.state.value.vehicle))
            for((width,height) in listOf(1256 to 800,648 to 800)) {
                runOnMainSync {activity.setContent { Box(Modifier.requiredSize(width.dp,height.dp)) {MoriApp(vm)} }}
                screenshot("unified-layout-${width}x$height")
            }
            runOnMainSync {activity.setContent {MoriApp(vm,true)}}
            screenshot("unified-night")
            runOnMainSync {activity.setContent {MoriApp(vm)};vm.togglePreview()}
            check(vm.state.value.profiles==liveProfiles) {"Live profiles not restored after scenario"}
            check(vm.state.value.assignments==liveAssignments-SeatId.DRIVER)
            check(vm.state.value.vehicle.seats==actualSeats)
            val edited=profile.copy(name="프로필 수정 확인",media=MediaPreference(true,"FM 테스트"))
            runOnMainSync {vm.saveProfile(edited)}
            await("Profile update failed") {!vm.state.value.controlBusy && vm.state.value.profiles.any {it==edited}}
            check(store.load().count {it.id==profile.id}==1)
            runOnMainSync {vm.removeProfile(profile.id)}
            await("Profile deletion failed") {!vm.state.value.controlBusy && vm.state.value.profiles.none {it.id==profile.id}}
            check(store.load().none {it.id==profile.id})
            result.putString("result","PASS: live seat/HVAC, relationship persistence, guest settings preserved, popup filtering, occupant change, overwrite/restore, confirmed HVAC choice, create/save, unsaved-close guard, example isolation, unique assignments, responsive/night rendering")
        } catch(error: Throwable) {
            result.putString("failure",error.stackTraceToString())
        } finally {
            store.save(backup)
            val vm=model
            val before=original
            if(vm!=null && before!=null) {
                if(vm.state.value.preview) runOnMainSync {vm.togglePreview()}
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
