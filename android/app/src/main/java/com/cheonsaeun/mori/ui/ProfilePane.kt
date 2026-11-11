package com.cheonsaeun.mori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheonsaeun.mori.design.*
import com.cheonsaeun.mori.model.*
import java.util.UUID

@Composable
fun ProfilePane(state: DashboardState, model: MoriViewModel, editing: PersonProfile?, bindOnSave: Boolean, onEdit: (PersonProfile?,Boolean) -> Unit, modifier: Modifier = Modifier) {
    MoriPanel(modifier) {
        if(editing!=null) {
            ProfileEditor(editing,state.controlBusy,{onEdit(null,false)},{model.saveProfile(it,bindOnSave);onEdit(null,false)})
        } else {
            var detailId by remember { mutableStateOf<String?>(null) }
            var deleting by remember { mutableStateOf(false) }
            val detail=state.profiles.firstOrNull {it.id==detailId}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(if(detail==null) "탑승자 프로필" else detail.name,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.weight(1f))
                if(detail!=null) LargeAction("목록",{detailId=null},secondary=true)
            }
            Text("선택 좌석: ${state.selectedSeat.label}",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
            if(detail==null) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    if(state.profiles.isEmpty()) {
                        Text("기억할 사람을 추가해 보세요",style=MaterialTheme.typography.titleLarge)
                        Text("좌석의 현재 설정을 저장하거나 새 프로필을 만들 수 있어요.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
                    }
                    state.profiles.forEach { profile ->
                        Surface(shape=MoriShapes.Card,color=MoriColors.Soft) {
                            Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                Text(profile.name,style=MaterialTheme.typography.titleLarge)
                                Text(if(profile.id==state.assignments[state.selectedSeat]) "${state.selectedSeat.label}에 연결됨" else "저장된 프로필",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
                                LargeAction("프로필 보기",{detailId=profile.id},Modifier.fillMaxWidth(),secondary=true)
                            }
                        }
                    }
                }
                LargeAction("+ 새 프로필",{onEdit(PersonProfile(UUID.randomUUID().toString(),""),false)},Modifier.fillMaxWidth(),enabled=!state.preview && !state.controlBusy)
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Text("좌석",style=MaterialTheme.typography.titleMedium)
                    Text("앞뒤 ${detail.seat.slide ?: "유지"} / 등받이 ${detail.seat.backrest ?: "유지"} / 높이 ${detail.seat.height ?: "유지"}",style=MaterialTheme.typography.bodyLarge)
                    Text("열선 ${detail.seat.heat ?: "유지"} / 통풍 ${detail.seat.ventilation ?: "유지"}",style=MaterialTheme.typography.bodyLarge)
                    Text("공조: ${climateLabel(detail.climate)}",style=MaterialTheme.typography.bodyLarge)
                    Text("${if(detail.media.radio) "라디오" else "음악"}: ${detail.media.selection.ifBlank {"미등록"}}",style=MaterialTheme.typography.bodyLarge)
                    LargeAction("프로필 수정",{onEdit(detail,false)},Modifier.fillMaxWidth(),enabled=!state.preview && !state.controlBusy,secondary=true)
                    if(detail.id==state.assignments[state.selectedSeat]) LargeAction("이 좌석에서 연결 해제",model::clearAssignment,Modifier.fillMaxWidth(),enabled=!state.controlBusy,secondary=true)
                    LargeAction("프로필 삭제",{deleting=true},Modifier.fillMaxWidth(),enabled=!state.preview && !state.controlBusy,secondary=true)
                }
                Text("저장한 자세와 열선/통풍을 적용해요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
                LargeAction("${state.selectedSeat.label}에 불러오기",{model.applyProfile(detail)},Modifier.fillMaxWidth(),enabled=!state.preview && !state.controlBusy)
                if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text("${detail.name} 프로필을 삭제할까요?")},confirmButton={LargeAction("삭제",{model.removeProfile(detail.id);deleting=false;detailId=null})},dismissButton={LargeAction("취소",{deleting=false},secondary=true)})
            }
        }
    }
}

fun climateLabel(value: ClimatePreference): String = listOfNotNull(value.temperature?.let {"${it}℃"},value.fan?.let {"풍량 $it"}).joinToString(" / ").ifBlank {"선호 미등록"}

@Composable
private fun ColumnScope.ProfileEditor(initial: PersonProfile, busy: Boolean, onCancel: () -> Unit, onSave: (PersonProfile) -> Unit) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var section by remember(initial.id) { mutableIntStateOf(0) }
    Text(if(initial.name.isBlank()) "새 프로필" else "프로필 수정",style=MaterialTheme.typography.headlineMedium)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("좌석","공조","미디어").forEachIndexed { index,label -> LargeTab(label,section==index,{section=index},Modifier.weight(1f)) }
    }
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value=draft.name,onValueChange={draft=draft.copy(name=it.take(20))},label={Text("이름 또는 별명")},singleLine=true,modifier=Modifier.fillMaxWidth().heightIn(min=80.dp),textStyle=MaterialTheme.typography.titleLarge)
        when(section) {
            0 -> {
                Text("등록하지 않은 항목은 현재 설정을 유지해요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
                IntSetting("앞뒤 위치",draft.seat.slide,0,100,5) {draft=draft.copy(seat=draft.seat.copy(slide=it))}
                IntSetting("등받이 위치",draft.seat.backrest,0,100,5) {draft=draft.copy(seat=draft.seat.copy(backrest=it))}
                IntSetting("높이",draft.seat.height,0,100,5) {draft=draft.copy(seat=draft.seat.copy(height=it))}
                IntSetting("열선",draft.seat.heat,0,3,1) {draft=draft.copy(seat=draft.seat.copy(heat=it,ventilation=if((it ?: 0)>0) 0 else draft.seat.ventilation))}
                IntSetting("통풍",draft.seat.ventilation,0,3,1) {draft=draft.copy(seat=draft.seat.copy(ventilation=it,heat=if((it ?: 0)>0) 0 else draft.seat.heat))}
            }
            1 -> {
                Text("공통 기능에서 지원 공조 구역에 적용해요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
                IntSetting("온도 / 0.5℃ 단위",draft.climate.temperature?.let {(it*2).toInt()},32,60,1,{"${it/2f}℃"}) {draft=draft.copy(climate=draft.climate.copy(temperature=it?.let {v -> v/2f}))}
                IntSetting("풍량",draft.climate.fan,0,8,1) {draft=draft.copy(climate=draft.climate.copy(fan=it))}
            }
            else -> {
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    LargeTab("음악",!draft.media.radio,{draft=draft.copy(media=MediaPreference(false))},Modifier.weight(1f))
                    LargeTab("라디오",draft.media.radio,{draft=draft.copy(media=MediaPreference(true))},Modifier.weight(1f))
                }
                OutlinedTextField(value=draft.media.selection,onValueChange={draft=draft.copy(media=draft.media.copy(selection=it.take(40)))},label={Text(if(draft.media.radio) "선호 방송 / 채널" else "선호 장르")},modifier=Modifier.fillMaxWidth(),singleLine=true,textStyle=MaterialTheme.typography.titleLarge)
                if(!draft.media.radio) listOf("인디","재즈","팝","클래식").chunked(2).forEach { pair -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { pair.forEach { label -> LargeTab(label,draft.media.selection==label,{draft=draft.copy(media=draft.media.copy(selection=label))},Modifier.weight(1f)) } } }
                Text("미디어 선호를 저장해요. 음원 재생은 연결 전이에요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
            }
        }
        Text("동승자가 동의한 선호를 이 기기에 저장해요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
    }
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        LargeAction("취소",onCancel,Modifier.weight(1f),enabled=!busy,secondary=true)
        LargeAction("저장",{onSave(draft)},Modifier.weight(1f),enabled=draft.name.isNotBlank() && !busy)
    }
}

@Composable
private fun IntSetting(label: String, value: Int?, minimum: Int, maximum: Int, step: Int, format: (Int) -> String = {it.toString()}, onChange: (Int?) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MoriColors.Soft,MoriShapes.Item).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("$label: ${value?.let(format) ?: "현재 설정 유지"}",style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            LargeAction("−",{onChange(((value ?: minimum+step)-step).coerceIn(minimum,maximum))},Modifier.weight(1f),secondary=true)
            LargeAction("+",{onChange(((value ?: minimum-step)+step).coerceIn(minimum,maximum))},Modifier.weight(1f),secondary=true)
            LargeAction("유지",{onChange(null)},Modifier.weight(1.3f),secondary=true)
        }
    }
}
