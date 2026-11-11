package com.cheonsaeun.mori.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cheonsaeun.mori.design.*
import com.cheonsaeun.mori.model.*
import java.util.UUID

enum class OverlayKind { MANAGE, OCCUPANT, SAVE, RESTORE, CLIMATE, CLIMATE_MANUAL, MEDIA, SEAT, EDITOR }
data class DashboardOverlay(val kind: OverlayKind, val seat: SeatId = SeatId.DRIVER, val draft: PersonProfile? = null, val bind: Boolean = false)
private data class Confirmation(val title: String, val message: String, val action: () -> Unit)

private fun seatDescription(p: SeatPreference) = listOfNotNull(p.slide?.let {"앞뒤 $it"},p.backrest?.let {"등받이 $it"},p.height?.let {"높이 $it"},p.heat?.let {"열선 $it"},p.ventilation?.let {"통풍 $it"}).joinToString(" / ").ifBlank {"저장된 좌석 설정 없음"}

@Composable
fun DashboardDialog(state: DashboardState, model: MoriViewModel, route: DashboardOverlay, onRoute: (DashboardOverlay) -> Unit, onClose: () -> Unit) {
    var dirty by remember(route) {mutableStateOf(false)}
    var confirmation by remember(route) {mutableStateOf<Confirmation?>(null)}
    val seat=state.vehicle.seats.first {it.id==route.seat}
    val connected=state.profiles.firstOrNull {it.id==state.assignments[route.seat]}
    fun close() {
        if(state.controlBusy) return
        if(dirty) confirmation=Confirmation("작성을 그만둘까요?","저장하지 않은 내용은 사라져요.",onClose) else onClose()
    }
    fun edit(profile: PersonProfile, bind: Boolean) {onRoute(DashboardOverlay(OverlayKind.EDITOR,route.seat,profile,bind))}
    fun capture(profile: PersonProfile? = null) = (profile ?: PersonProfile(UUID.randomUUID().toString(),"")).let {it.copy(seat=SeatPreference.capture(seat,it.seat))}
    fun moveNote(p: PersonProfile): String = state.assignments.entries.firstOrNull {it.value==p.id && it.key!=route.seat}?.let {"${it.key.label}의 연결을 해제하고 ${route.seat.label}으로 옮겨요. "} ?: ""
    Dialog(onDismissRequest=::close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        MoriPanel(Modifier.imePadding().widthIn(max=1150.dp).fillMaxWidth(.94f).fillMaxHeight(.92f)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(when(route.kind) {OverlayKind.MANAGE -> "프로필 관리"; OverlayKind.CLIMATE -> "공조 취향 선택"; OverlayKind.CLIMATE_MANUAL -> "공조 직접 조절"; OverlayKind.MEDIA -> "미디어 취향 선택"; OverlayKind.EDITOR -> "프로필 설정"; OverlayKind.SAVE -> "${route.seat.label} 상태 저장"; OverlayKind.RESTORE -> "저장 설정 복원"; OverlayKind.SEAT -> "${route.seat.label} 조절"; else -> "${route.seat.label} 탑승자 변경"},style=MaterialTheme.typography.headlineMedium,modifier=Modifier.weight(1f))
                LargeAction("닫기",::close,enabled=!state.controlBusy,secondary=true)
            }
            if(state.controlBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(state.controlFeedback!=null) Text(state.controlFeedback,style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary,maxLines=3)
            if(state.preview) Text("예시 시나리오 안에서만 변경돼요",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Primary)
            confirmation?.let { pending ->
                AlertDialog(onDismissRequest={confirmation=null},title={Text(pending.title)},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {Text(pending.message,style=MaterialTheme.typography.bodyLarge)}},confirmButton={LargeAction("확인",{confirmation=null;pending.action()})},dismissButton={LargeAction("취소",{confirmation=null},secondary=true)})
            }
            when(route.kind) {
                OverlayKind.MANAGE -> {
                    ProfileChoices(state,Modifier.weight(1f),action="프로필 수정",summary={"좌석: ${seatDescription(it.seat)}\n공조: ${climateLabel(it.climate)}\n미디어: ${it.media.selection.ifBlank {"미등록"}}"},onChoose={edit(it,false)})
                    LargeAction("+ 새 프로필",{edit(PersonProfile(UUID.randomUUID().toString(),""),false)},Modifier.fillMaxWidth())
                }
                OverlayKind.OCCUPANT -> {
                    Text("프로필을 불러오면 이 좌석의 저장된 자세와 열선/통풍을 적용해요.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        LargeAction("게스트로 이용",{model.clearAssignment(route.seat);onClose()},Modifier.weight(1f),secondary=true)
                        LargeAction("새 프로필 생성",{edit(capture(),true)},Modifier.weight(1f))
                    }
                    ProfileChoices(state,Modifier.weight(1f),summary={seatDescription(it.seat)},onChoose={p ->
                        confirmation=Confirmation("${p.name}의 프로필을 불러올까요?",moveNote(p)+"${route.seat.label}에 저장된 좌석 설정을 적용해요. 공조와 음악은 유지해요.\n\n${seatDescription(p.seat)}") {model.applyProfile(p,target=route.seat);onClose()}
                    })
                }
                OverlayKind.SAVE -> {
                    Text("현재 좌석의 자세와 열선/통풍을 저장해요. 공조와 미디어 선호는 유지해요.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
                    Text(seatDescription(SeatPreference.capture(seat)),style=MaterialTheme.typography.titleLarge)
                    if(connected!=null) {
                        Text("${connected.name}의 기존 좌석 설정을 덮어써요. 차량 설정은 바꾸지 않아요.",style=MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        LargeAction("${connected.name} 프로필에 덮어쓰기",{model.saveProfile(capture(connected),true,onClose)},Modifier.fillMaxWidth(),enabled=!state.controlBusy)
                    } else {
                        LargeAction("새 프로필로 저장",{edit(capture(),true)},Modifier.fillMaxWidth())
                        Text("기존 프로필에 덮어쓰기",style=MaterialTheme.typography.titleLarge)
                        ProfileChoices(state,Modifier.weight(1f),action="이 프로필에 저장",onChoose={p -> edit(capture(p),true)})
                    }
                }
                OverlayKind.RESTORE -> {
                    if(connected==null) {Text("연결된 프로필이 없어요",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.weight(1f))}
                    else {
                        Text("${connected.name}의 저장된 좌석 설정으로 돌아갈까요?",style=MaterialTheme.typography.titleLarge)
                        Text(seatDescription(connected.seat),style=MaterialTheme.typography.bodyLarge)
                        Text("지금 직접 바꾼 좌석 설정은 교체돼요. 공조와 음악은 유지해요.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
                        Spacer(Modifier.weight(1f))
                        LargeAction("저장 설정 적용",{model.applyProfile(connected,target=route.seat);onClose()},Modifier.fillMaxWidth())
                    }
                }
                OverlayKind.CLIMATE -> {
                    Text("선택한 온도를 운전석/조수석 지원 구역에 함께 적용해요. 등록하지 않은 항목은 유지해요.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
                    LargeAction("공조 직접 조절",{onRoute(route.copy(kind=OverlayKind.CLIMATE_MANUAL))},Modifier.fillMaxWidth(),secondary=true)
                    ProfileChoices(state,Modifier.weight(1f),action="이 공조 선택",eligible={it.climate.configured},summary={climateLabel(it.climate)},onChoose={p ->
                        confirmation=Confirmation("${p.name}의 공조 취향을 적용할까요?","${climateLabel(p.climate)}\n\n지원하는 앞좌석 온도 구역과 공유 풍량을 바꿔요.") {model.applyProfile(p,true);onClose()}
                    })
                }
                OverlayKind.CLIMATE_MANUAL -> ClimateAdjustment(state,model,onClose)
                OverlayKind.MEDIA -> {
                    Text("선호하는 장르나 라디오 채널을 선택해요. 실제 음원 재생은 연결 전이에요.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
                    ProfileChoices(state,Modifier.weight(1f),action="이 취향 선택",eligible={it.media.selection.isNotBlank()},summary={"${if(it.media.radio) "라디오" else "음악"} / ${it.media.selection.ifBlank {"선호 미등록"}}"},onChoose={model.applyMedia(it);onClose()})
                }
                OverlayKind.EDITOR -> route.draft?.let { draft ->
                    val existing=state.profiles.any {it.id==draft.id}
                    ProfileEditor(initial=draft,busy=state.controlBusy,onCancel=::close,onDirty={dirty=it},onSave={value ->
                        val save={model.saveProfile(value,route.bind,onClose)}
                        if(existing) confirmation=Confirmation("${value.name}의 프로필을 덮어쓸까요?",(if(route.bind) moveNote(value) else "")+"등록값을 아래 내용으로 바꿔요. 차량 설정은 유지해요.\n\n${seatDescription(value.seat)}\n공조: ${climateLabel(value.climate)}\n미디어: ${value.media.selection.ifBlank {"미등록"}}",save) else save()
                    })
                    if(existing) LargeAction("프로필 삭제",{confirmation=Confirmation("${draft.name} 프로필을 삭제할까요?","저장된 선호와 좌석 연결을 지워요. 차량의 현재 설정은 유지해요.") {model.removeProfile(draft.id);onClose()}},Modifier.fillMaxWidth(),enabled=!state.controlBusy,secondary=true)
                }
                OverlayKind.SEAT -> SeatAdjustment(state,model,route.seat,onClose) {dirty=it}
            }
        }
    }
}

@Composable
private fun ColumnScope.SeatAdjustment(state: DashboardState, model: MoriViewModel, id: SeatId, onClose: () -> Unit, onDirty: (Boolean) -> Unit) {
    val live=state.vehicle.seats.first {it.id==id}
    val original=remember(id) {SeatPreference.capture(live)}
    var draft by remember(id) {mutableStateOf(original)}
    LaunchedEffect(draft,original) {onDirty(draft!=original)}
    Text("차량의 현재 설정만 바꿔요. 프로필에는 자동 저장하지 않아요.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        if(live.slide.value!=null) IntSetting("앞뒤 위치",draft.slide,0,100,5) {draft=draft.copy(slide=it)}
        if(live.backrest.value!=null) IntSetting("등받이 위치",draft.backrest,0,100,5) {draft=draft.copy(backrest=it)}
        if(live.height.value!=null) IntSetting("높이",draft.height,0,100,5) {draft=draft.copy(height=it)}
        val comfort=state.preview || id==SeatId.DRIVER || id==SeatId.PASSENGER
        if(comfort && live.heat.value!=null) IntSetting("열선",draft.heat,0,3,1) {draft=draft.copy(heat=it,ventilation=if((it ?: 0)>0) 0 else draft.ventilation)}
        if(comfort && live.ventilation.value!=null) IntSetting("통풍",draft.ventilation,0,3,1) {draft=draft.copy(ventilation=it,heat=if((it ?: 0)>0) 0 else draft.heat)}
        if(!comfort) Text("이 시뮬레이터에서 뒷좌석 열선/통풍 조절은 지원하지 않아요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
    }
    LargeAction("변경한 설정 적용",{
        val delta=SeatPreference(draft.backrest.takeIf {it!=original.backrest},draft.slide.takeIf {it!=original.slide},draft.height.takeIf {it!=original.height},draft.heat.takeIf {it!=original.heat},draft.ventilation.takeIf {it!=original.ventilation})
        model.applyProfile(PersonProfile("","직접 조정",seat=delta),target=id,connect=false);onClose()
    },Modifier.fillMaxWidth(),enabled=!state.controlBusy && draft!=original && listOf(draft.backrest to original.backrest,draft.slide to original.slide,draft.height to original.height,draft.heat to original.heat,draft.ventilation to original.ventilation).any {it.first!=null && it.first!=it.second})
}

@Composable
private fun ColumnScope.ClimateAdjustment(state: DashboardState, model: MoriViewModel, onClose: () -> Unit) {
    var preference by remember {mutableStateOf(ClimatePreference(state.vehicle.zones.firstNotNullOfOrNull {it.temperature.value},state.vehicle.zones.firstNotNullOfOrNull {it.fan.value}))}
    Text("온도는 지원되는 운전석/조수석 구역에 함께 적용해요.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        if(state.vehicle.zones.any {it.temperature.value!=null}) IntSetting("앞좌석 공통 온도",preference.temperature?.let {(it*2).toInt()},32,60,1,{"${it/2f}℃"}) {preference=preference.copy(temperature=it?.let {v -> v/2f})}
        if(state.vehicle.zones.any {it.fan.value!=null}) IntSetting("공유 풍량",preference.fan,0,8,1) {preference=preference.copy(fan=it)}
        if(state.vehicle.zones.none {it.temperature.value!=null || it.fan.value!=null}) Text("공조 상태 확인 후 조절할 수 있어요.",style=MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            LargeAction("공조 켜기",{model.setClimatePower(true);onClose()},Modifier.weight(1f),enabled=!state.controlBusy && state.vehicle.zones.any {it.power.value!=null},secondary=true)
            LargeAction("공조 끄기",{model.setClimatePower(false);onClose()},Modifier.weight(1f),enabled=!state.controlBusy && state.vehicle.zones.any {it.power.value!=null},secondary=true)
        }
    }
    LargeAction("공조 설정 적용",{model.applyProfile(PersonProfile("","직접 조정",climate=preference),true,connect=false);onClose()},Modifier.fillMaxWidth(),enabled=!state.controlBusy && preference.configured)
}
