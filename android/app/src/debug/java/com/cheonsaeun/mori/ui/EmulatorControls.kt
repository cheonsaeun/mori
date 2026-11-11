package com.cheonsaeun.mori.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheonsaeun.mori.design.MoriColors
import com.cheonsaeun.mori.model.DashboardState
import com.cheonsaeun.mori.model.SeatId
import kotlin.math.roundToInt

/** Debug controls publish to the official simulator; dashboard values still come from SDK. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorControls(state: DashboardState, onCommand: (String) -> Unit) {
    if(state.simulatorActive==null || state.preview) return
    var open by remember { mutableStateOf(false) }
    OutlinedButton(modifier=Modifier.heightIn(min=80.dp),onClick={open=true},enabled=state.simulatorActive==true, ) { Text("차량 테스트") }
    if(!open) return
    ModalBottomSheet(onDismissRequest={open=false},sheetMaxWidth=1100.dp) {
        Column(Modifier.fillMaxWidth().heightIn(max=760.dp).verticalScroll(rememberScrollState()).padding(horizontal=32.dp,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("에뮬레이터 차량 제어",style=MaterialTheme.typography.headlineMedium)
            Text("명령은 공식 시뮬레이터로 보내고, 에뮬레이터 상태와 SDK 응답을 대시보드에 반영해요.",color=MoriColors.Secondary)
            var selected by remember { mutableStateOf(state.selectedSeat) }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                SeatId.entries.forEach { seat -> FilterChip(modifier=Modifier.heightIn(min=80.dp),selected=seat==selected,onClick={selected=seat},label={Text(seat.label)}) }
            }
            val seat=state.vehicle.seats.first {it.id==selected}
            val area=when(selected) { SeatId.DRIVER -> "driver"; SeatId.PASSENGER -> "passenger"; SeatId.REAR_LEFT -> "rear_left"; SeatId.REAR_RIGHT -> "rear_right" }
            var position by remember(selected,seat.backrest.value) { mutableFloatStateOf((seat.backrest.value ?: 0).toFloat()) }
            Text("등받이 위치 ${position.roundToInt()} / 실제 값 ${seat.backrest.value ?: "확인 중"}",style=MaterialTheme.typography.titleMedium)
            Slider(value=position,onValueChange={position=it},onValueChangeFinished={onCommand("/vehicle/seat?kind=position&control=recline&area=$area&value=${position.roundToInt()}")},valueRange=0f..100f,steps=99,enabled=!state.controlBusy && seat.backrest.value!=null)
            if(selected==SeatId.DRIVER || selected==SeatId.PASSENGER) {
                Text("${selected.label} 열선 / 통풍",style=MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    listOf("꺼짐" to 0,"열선 1" to 1,"열선 2" to 2,"통풍 1" to -1,"통풍 2" to -2).forEach { (label,level) ->
                        OutlinedButton(modifier=Modifier.heightIn(min=80.dp),onClick={onCommand("/vehicle/hvac?${area}_seat=$level")},enabled=!state.controlBusy) { Text(label) }
                    }
                }
            }
            HorizontalDivider()
            Text("공조",style=MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                listOf("driver" to "운전석 구역","passenger" to "조수석 구역").forEach { (key,label) ->
                    val temp=state.vehicle.zones.firstOrNull {it.label==label}?.temperature?.value
                    Column(Modifier.weight(1f)) {
                        Text("$label / ${temp?.let { "$it℃" } ?: "확인 중"}")
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(modifier=Modifier.heightIn(min=80.dp),onClick={onCommand("/vehicle/hvac?${key}_temp=${(temp!!-.5f).coerceIn(16f,30f)}")},enabled=!state.controlBusy && temp!=null) { Text("− 0.5℃") }
                            OutlinedButton(modifier=Modifier.heightIn(min=80.dp),onClick={onCommand("/vehicle/hvac?${key}_temp=${(temp!!+.5f).coerceIn(16f,30f)}")},enabled=!state.controlBusy && temp!=null) { Text("+ 0.5℃") }
                        }
                    }
                }
            }
            val fan=state.vehicle.zones.firstNotNullOfOrNull {it.fan.value}
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(modifier=Modifier.heightIn(min=80.dp),onClick={onCommand("/vehicle/hvac?power=1")},enabled=!state.controlBusy) { Text("공조 켜기") }
                OutlinedButton(modifier=Modifier.heightIn(min=80.dp),onClick={onCommand("/vehicle/hvac?power=0")},enabled=!state.controlBusy) { Text("공조 끄기") }
                OutlinedButton(modifier=Modifier.heightIn(min=80.dp),onClick={onCommand("/vehicle/hvac?fan=${(fan!!-1).coerceIn(0,8)}")},enabled=!state.controlBusy && fan!=null) { Text("풍량 −") }
                Text("풍량 ${fan ?: "확인 중"}",modifier=Modifier.padding(top=16.dp))
                OutlinedButton(modifier=Modifier.heightIn(min=80.dp),onClick={onCommand("/vehicle/hvac?fan=${(fan!!+1).coerceIn(0,8)}")},enabled=!state.controlBusy && fan!=null) { Text("풍량 +") }
            }
            Text(if(state.controlBusy) "명령을 보내고 있어요…" else state.controlFeedback ?: "개발용 제어 기능입니다. 실제 차량용 빌드에는 포함되지 않아요.",color=MoriColors.Secondary)
            Spacer(Modifier.height(24.dp))
        }
    }
}
