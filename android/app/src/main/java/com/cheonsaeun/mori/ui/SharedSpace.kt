package com.cheonsaeun.mori.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cheonsaeun.mori.design.*
import com.cheonsaeun.mori.model.*
import java.util.Locale

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun SharedSpace(state: DashboardState, modifier: Modifier = Modifier) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(20.dp)) {
        MoriPanel(Modifier.fillMaxWidth().weight(1.18f)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    MoriIcon("climate")
                    Text("함께 쓰는 공조",style=MaterialTheme.typography.titleLarge)
                }
                StatusPill("공통 공간")
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                state.vehicle.modes.forEach { mode ->
                    StatusPill("${mode.label} ${mode.enabled.label { if(it) "켜짐" else "꺼짐" }}",if(mode.enabled.value==true) MoriColors.Primary else MoriColors.Secondary)
                }
            }
            if(state.vehicle.zones.isEmpty()) {
                Column(Modifier.weight(1f).fillMaxWidth().background(MoriColors.Soft,MoriShapes.Card).padding(24.dp),verticalArrangement=Arrangement.Center) {
                    Text(if(state.vehicle.received) "공조 상태 확인 불가" else "공조 구역을 확인하고 있어요",style=MaterialTheme.typography.titleMedium)
                    Text("지원되는 공조 구역의 응답을 여기에 표시해요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    val temperatures=state.vehicle.zones.filter { it.temperature.availability!=Availability.UNSUPPORTED }
                    temperatures.chunked(2).forEach { pair ->
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            pair.forEach { zone ->
                                Column(Modifier.weight(1f).background(MoriColors.Soft,MoriShapes.Card).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                    Text(zone.label,style=MaterialTheme.typography.titleSmall,color=MoriColors.Secondary)
                                    Text(zone.temperature.label { String.format(Locale.ROOT,"%.1f°",it) },style=if(zone.temperature.value!=null) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,color=MoriColors.Primary)
                                    Text("설정 온도",style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
                                    if(zone.fan.value!=null) Text("풍량 ${zone.fan.value}",style=MaterialTheme.typography.labelMedium,color=MoriColors.Secondary)
                                }
                            }
                            if(pair.size==1) Spacer(Modifier.weight(1f))
                        }
                    }
                    state.vehicle.zones.filter { it.temperature.availability==Availability.UNSUPPORTED && it.fan.value!=null }.forEach { zone ->
                        Row(Modifier.fillMaxWidth().background(MoriColors.Soft,MoriShapes.Item).padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            MoriIcon("cool",Modifier.size(20.dp))
                            Text(zone.label,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f))
                            Text("풍량 ${zone.fan.value}",style=MaterialTheme.typography.labelLarge,color=MoriColors.Primary)
                        }
                    }
                    val powers=state.vehicle.zones.mapNotNull { it.power.value }.distinct()
                    if(powers.size==1) Text(if(powers.first()) "공조 시스템 켜짐" else "공조 시스템 꺼짐",style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
                    if(temperatures.isEmpty() && state.vehicle.zones.none {it.fan.value!=null}) Text("지원되는 공조 상태를 확인할 수 없어요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
                }
            }
            Text("설정 온도를 표시해요. 지원 구역은 차량마다 달라요.",style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
        }
        MusicPanel(state.preview,Modifier.fillMaxWidth().weight(1f))
        Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha=.75f),MoriShapes.Card).padding(20.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
            MoriIcon("people")
            Column {
                Text("한 공간, 서로 다른 편안함",style=MaterialTheme.typography.titleSmall)
                Text("시트는 각자에게, 공조와 음악은 함께하는 공간에.",style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
            }
        }
    }
}

@Composable
private fun MusicPanel(preview: Boolean, modifier: Modifier) {
    var radio by remember { mutableStateOf(false) }
    Surface(modifier,shape=MoriShapes.Panel,color=MoriColors.Navy) {
        Column(Modifier.background(MoriColors.MediaGradient).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    MoriIcon(if(radio) "radio" else "music",color=Color(0xFFBBB9FF))
                    Text("함께 듣는 음악",style=MaterialTheme.typography.titleLarge,color=Color.White)
                }
                Row {
                    TextButton(onClick={radio=false}) { Text("음악",color=if(!radio) Color.White else Color(0xFFAAAFCB)) }
                    TextButton(onClick={radio=true}) { Text("라디오",color=if(radio) Color.White else Color(0xFFAAAFCB)) }
                }
            }
            Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                MusicArtwork(Modifier.size(110.dp))
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(when { preview && radio -> "주말의 라디오"; preview -> "Soft afternoon"; radio -> "라디오 연결 준비"; else -> "우리의 취향을 모아서" },style=MaterialTheme.typography.titleLarge,color=Color.White)
                    Text(if(preview) "${if(radio) "FM 채널" else "MORI selection"} / 디자인 예시" else "앱 내 재생 기능을 연결할 공간이에요",style=MaterialTheme.typography.bodyMedium,color=Color(0xFFCDD0E7))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        StatusPill("모든 탑승자",Color(0xFFDFDDFF),Color.White.copy(alpha=.10f))
                        if(preview) StatusPill(if(radio) "같은 채널" else "공통 취향",Color(0xFFDFDDFF),Color.White.copy(alpha=.10f))
                    }
                }
            }
            HorizontalDivider(color=Color.White.copy(alpha=.12f))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text(if(preview) "재생 화면 예시 / 실제 음원 미연결" else "차량의 다른 음악 앱 상태는 아직 연결되지 않았어요",style=MaterialTheme.typography.bodySmall,color=Color(0xFFC0C4DF))
                Waveform()
            }
        }
    }
}

@Composable
private fun MusicArtwork(modifier: Modifier) {
    Canvas(modifier) {
        val center=Offset(size.width/2,size.height/2)
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFF7C8BFA),Color(0xFFA89CE8),Color(0xFF5D5EA5))),cornerRadius=androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()))
        drawCircle(Color.White.copy(alpha=.16f),size.width*.39f,center)
        drawCircle(Color.White.copy(alpha=.4f),size.width*.3f,center,style=Stroke(1.dp.toPx()))
        drawCircle(MoriColors.Navy.copy(alpha=.75f),size.width*.19f,center)
        drawCircle(Color(0xFFE9DFFF),size.width*.05f,center)
    }
}

@Composable
private fun Waveform() {
    Row(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
        listOf(8,18,26,14,22,10).forEach { Box(Modifier.width(3.dp).height(it.dp).background(Color(0xFFB9B6FF),MoriShapes.Pill)) }
    }
}
