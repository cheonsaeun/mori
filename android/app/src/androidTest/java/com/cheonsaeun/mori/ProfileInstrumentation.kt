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
            check(vm.state.value.assignments.isEmpty())
            check(vm.state.value.profiles==backup)
            check(nodes("예시 시나리오").isEmpty() && nodes("실제 차량 보기").isEmpty())
            check(nodes("차량 테스트").isNotEmpty() && nodes("프로필 관리").isNotEmpty())
            screenshot("live-default")
            val isolatedName="mori_import_regression_test"
            val isolatedPrefs=targetContext.getSharedPreferences(isolatedName,0)
            isolatedPrefs.edit().clear().commit()
            try {
                val isolated=ProfileStore(targetContext,isolatedName)
                val own=PersonProfile("legacy","기존 데이터")
                val modified=StarterProfiles.profiles().first().copy(name="기존 수정값")
                isolated.save(listOf(own,modified))
                val imported=isolated.load()
                check(imported.size==7 && imported.contains(own) && imported.contains(modified))
                val editedMom=imported.first {it.id=="example-mom"}.copy(name="수정한 프로필")
                val after=imported.filterNot {it.id==modified.id}.map {if(it.id==editedMom.id) editedMom else it}
                isolated.save(after)
                check(ProfileStore(targetContext,isolatedName).load()==after) {"Deleted profiles reimported or edits lost"}
                check(isolated.load()==after)
            } finally {isolatedPrefs.edit().clear().commit()}
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
            runOnMainSync {vm.applyMedia(profile)}
            check(vm.state.value.media.selection=="인디")
            runOnMainSync {vm.applyProfile(PersonProfile("","직접 조정",seat=SeatPreference(backrest=36)),target=SeatId.DRIVER,connect=false)}
            await("Manual change failed") {!vm.state.value.controlBusy && vm.state.value.vehicle.seats.first().backrest.value==36}
            clickText("지금 상태 저장")
            clickText("테스트 프로필 프로필에 덮어쓰기")
            await("Overwrite failed") {!vm.state.value.controlBusy && vm.state.value.profiles.first {it.id==profile.id}.seat.backrest==36}
            check(store.load().first {it.id==profile.id}.seat.backrest==36)
            runOnMainSync {vm.applyProfile(PersonProfile("","직접 조정",seat=SeatPreference(backrest=42)),target=SeatId.DRIVER,connect=false)}
            await("Manual change timed out") {!vm.state.value.controlBusy}
            clickText("저장 설정 복원")
            clickText("저장 설정 적용")
            await("Restore failed") {!vm.state.value.controlBusy && vm.state.value.vehicle.seats.first().backrest.value==36}
            clickText("다른 취향으로 바꾸기")
            val climateBefore=vm.state.value.vehicle.zones
            clickText("테스트 프로필: 이 공조 선택")
            check(vm.state.value.vehicle.zones==climateBefore)
            clickText("확인")
            await("Confirmed climate failed") {!vm.state.value.controlBusy}
            val friend=PersonProfile("mori-integration-friend","검증 친구",relationship=Relationship.FRIEND)
            runOnMainSync {vm.saveProfile(friend)}
            await("Friend save failed") {!vm.state.value.controlBusy && vm.state.value.profiles.any {it.id==friend.id}}
            clickText("프로필 관리")
            clickText("친구")
            check(nodes("검증 친구").isNotEmpty())
            check(nodes("테스트 프로필").isEmpty())
            screenshot("live-profile-filter")
            clickText("닫기")
            clickText("탑승자 변경")
            screenshot("live-occupant")
            val seatBefore=vm.state.value.vehicle.seats
            clickText("게스트로 이용")
            check(vm.state.value.assignments[SeatId.DRIVER]==null)
            check(vm.state.value.vehicle.seats==seatBefore)
            clickText("프로필로 저장")
            clickText("새 프로필로 저장")
            inputName("검증 게스트")
            clickText("닫기")
            check(nodes("작성을 그만둘까요?").isNotEmpty())
            clickText("취소")
            clickText("저장")
            await("Guest save failed") {!vm.state.value.controlBusy && vm.state.value.profiles.any {it.name=="검증 게스트"}}
            check(store.load().any {it.name=="검증 게스트"})
            for((width,height) in listOf(1256 to 800,648 to 800)) {
                runOnMainSync {activity.setContent { Box(Modifier.requiredSize(width.dp,height.dp)) {MoriApp(vm)} }}
                screenshot("live-layout-${width}x$height")
            }
            runOnMainSync {activity.setContent {MoriApp(vm,true)}}
            screenshot("live-night")
            runOnMainSync {activity.setContent {MoriApp(vm)}}
            val edited=profile.copy(name="프로필 수정 확인",media=MediaPreference(true,"FM 테스트"))
            runOnMainSync {vm.saveProfile(edited)}
            await("Profile update failed") {!vm.state.value.controlBusy && vm.state.value.profiles.any {it==edited}}
            check(store.load().count {it.id==profile.id}==1)
            runOnMainSync {vm.removeProfile(profile.id)}
            await("Profile deletion failed") {!vm.state.value.controlBusy && vm.state.value.profiles.none {it.id==profile.id}}
            check(store.load().none {it.id==profile.id})
            result.putString("result","PASS: live-only UI, one-time profile import, legacy edits preserved, deleted profiles stay deleted, live seat/HVAC, popup filtering, occupant change, persistent overwrite/restore/save, unsaved-close guard, responsive/night rendering")
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
                runOnMainSync {vm.clearAssignment();activity?.setContent {MoriApp(vm)}}
            }
        }
        finish(if(result.containsKey("failure")) Activity.RESULT_CANCELED else Activity.RESULT_OK,result)
    }
}
