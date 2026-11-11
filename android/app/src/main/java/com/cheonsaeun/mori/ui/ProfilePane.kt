package com.cheonsaeun.mori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.cheonsaeun.mori.design.*
import com.cheonsaeun.mori.model.*
import java.util.UUID

@Composable
fun ProfilePane(state: DashboardState, model: MoriViewModel, editing: PersonProfile?, bindOnSave: Boolean, onEdit: (PersonProfile?,Boolean) -> Unit, modifier: Modifier = Modifier) {
    MoriPanel(modifier) {
        if(editing!=null) ProfileEditor(initial=editing,busy=state.controlBusy,onCancel={onEdit(null,false)},onSave={model.saveProfile(it,bindOnSave) {onEdit(null,false)}})
        else {
            Text("프로필 관리",style=MaterialTheme.typography.headlineMedium)
            ProfileChoices(state,Modifier.weight(1f),action="프로필 수정",onChoose={onEdit(it,false)})
            LargeAction("+ 새 프로필",{onEdit(PersonProfile(UUID.randomUUID().toString(),""),false)},Modifier.fillMaxWidth(),enabled=!state.controlBusy)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelationshipSelector(value: Relationship, onChange: (Relationship) -> Unit) {
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Relationship.entries.forEach { item -> FilterChip(selected=item==value,onClick={onChange(item)},label={Text(item.label,style=MaterialTheme.typography.titleMedium)},modifier=Modifier.heightIn(min=80.dp).widthIn(min=100.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileChoices(state: DashboardState, modifier: Modifier = Modifier, action: String = "선택", eligible: (PersonProfile) -> Boolean = {true}, summary: (PersonProfile) -> String = {"${it.relationship.label} / 저장된 프로필"}, onChoose: (PersonProfile) -> Unit) {
    var filter by remember {mutableStateOf<Relationship?>(null)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=filter==null,onClick={filter=null},label={Text("전체",style=MaterialTheme.typography.titleMedium)},modifier=Modifier.heightIn(min=80.dp).widthIn(min=100.dp))
            Relationship.entries.forEach { item -> FilterChip(selected=filter==item,onClick={filter=item},label={Text(item.label,style=MaterialTheme.typography.titleMedium)},modifier=Modifier.heightIn(min=80.dp).widthIn(min=100.dp)) }
        }
        Text("탑승 중인 프로필이 먼저 표시돼요",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            val profiles=state.profileChoices(filter)
            if(profiles.isEmpty()) {
                Text(if(state.profiles.isEmpty()) "저장된 프로필이 없어요" else "이 관계로 등록한 프로필이 없어요",style=MaterialTheme.typography.titleLarge)
                if(filter!=null) LargeAction("전체 프로필 보기",{filter=null},Modifier.fillMaxWidth(),secondary=true)
            }
            profiles.forEach {p ->
                Surface(shape=MoriShapes.Card,color=MoriColors.Soft) {
                    Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            Text(p.name,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                            Text(p.relationship.label,style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
                        }
                        state.onboardSeat(p.id)?.let { Text("${it.label} 탑승 중",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Primary) }
                        Text(summary(p),style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary,maxLines=3)
                        LargeAction(action,{onChoose(p)},Modifier.fillMaxWidth().semantics {contentDescription="${p.name}: $action"},enabled=!state.controlBusy && eligible(p),secondary=true)
                    }
                }
            }
        }
    }
}

fun climateLabel(value: ClimatePreference): String = listOfNotNull(value.temperature?.let {"${it}℃"},value.fan?.let {"풍량 $it"}).joinToString(" / ").ifBlank {"선호 미등록"}

@Composable
fun ColumnScope.ProfileEditor(initial: PersonProfile, busy: Boolean, onCancel: () -> Unit, onSave: (PersonProfile) -> Unit, onDirty: (Boolean) -> Unit = {}) {
    var draft by remember(initial) { mutableStateOf(initial) }
    LaunchedEffect(draft,initial) {onDirty(draft!=initial)}
    var section by remember(initial.id) { mutableIntStateOf(0) }
    Text(if(initial.name.isBlank()) "새 프로필" else "프로필 수정",style=MaterialTheme.typography.headlineMedium)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("좌석","공조","미디어").forEachIndexed { index,label -> LargeTab(label,section==index,{section=index},Modifier.weight(1f)) }
    }
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value=draft.name,onValueChange={draft=draft.copy(name=it.take(20))},label={Text("이름 또는 별명")},singleLine=true,modifier=Modifier.fillMaxWidth().heightIn(min=80.dp),textStyle=MaterialTheme.typography.titleLarge)
        Text("관계 / 선택사항",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
        RelationshipSelector(draft.relationship) {draft=draft.copy(relationship=it)}
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
                Text("공조 카드에서 지원 구역에 일괄 적용해요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
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
fun IntSetting(label: String, value: Int?, minimum: Int, maximum: Int, step: Int, format: (Int) -> String = {it.toString()}, onChange: (Int?) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MoriColors.Soft,MoriShapes.Item).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("$label: ${value?.let(format) ?: "현재 설정 유지"}",style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            LargeAction("−",{onChange(((value ?: minimum+step)-step).coerceIn(minimum,maximum))},Modifier.weight(1f),secondary=true)
            LargeAction("+",{onChange(((value ?: minimum-step)+step).coerceIn(minimum,maximum))},Modifier.weight(1f),secondary=true)
            LargeAction("유지",{onChange(null)},Modifier.weight(1.3f),secondary=true)
        }
    }
}
