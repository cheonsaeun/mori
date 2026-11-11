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
fun ClimateCurrentPanel(state: DashboardState, modifier: Modifier = Modifier) {
    MoriPanel(modifier) {
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
}

@Composable
fun CommonDashboard(state: DashboardState, model: MoriViewModel, stacked: Boolean, modifier: Modifier = Modifier) {
    if(stacked) Column(modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        CommonColumn(state,model,false,Modifier.fillMaxWidth().height(800.dp))
        CommonColumn(state,model,true,Modifier.fillMaxWidth().height(800.dp))
    } else Row(modifier,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
        CommonColumn(state,model,false,Modifier.weight(1f).fillMaxHeight())
        CommonColumn(state,model,true,Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun CommonColumn(state: DashboardState, model: MoriViewModel, media: Boolean, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        if(media) MediaCurrentPanel(state,Modifier.fillMaxWidth().height(460.dp)) else ClimateCurrentPanel(state,Modifier.fillMaxWidth().height(460.dp))
        MoriPanel(Modifier.fillMaxWidth()) {
            Text(if(media) "탑승자의 미디어 선호" else "탑승자의 공조 선호",style=MaterialTheme.typography.titleLarge)
            Text(if(media) "한 사람의 선호를 함께 들어요" else "선택한 선호를 지원 공조 구역에 적용해요",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
            if(state.profiles.isEmpty()) Text("개별 좌석 탭에서 프로필을 추가해 주세요.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
            state.profiles.forEach { p ->
                Column(Modifier.fillMaxWidth().background(MoriColors.Soft,MoriShapes.Item).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(p.name,style=MaterialTheme.typography.titleLarge)
                    Text(if(media) "${if(p.media.radio) "라디오" else "음악"} / ${p.media.selection.ifBlank {"선호 미등록"}}" else climateLabel(p.climate),style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
                    LargeAction(if(media) "미디어 선호 적용" else "공조에 적용",{if(media) model.applyMedia(p) else model.applyProfile(p,true)},Modifier.fillMaxWidth(),enabled=!state.preview && !state.controlBusy && if(media) p.media.selection.isNotBlank() else p.climate.configured,secondary=true)
                }
            }
        }
    }
}

@Composable
private fun MediaCurrentPanel(state: DashboardState, modifier: Modifier) {
    Surface(modifier,shape=MoriShapes.Panel,color=MoriColors.Navy) {
        Column(Modifier.background(MoriColors.MediaGradient).padding(28.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                MoriIcon(if(state.media.radio) "radio" else "music",Modifier.size(32.dp),Color.White)
                Text("함께 듣는 ${if(state.media.radio) "라디오" else "음악"}",style=MaterialTheme.typography.headlineMedium,color=Color.White)
            }
            Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                MusicArtwork(Modifier.size(100.dp))
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(state.media.selection.ifBlank {"함께 들을 취향을 골라요"},style=MaterialTheme.typography.headlineMedium,color=Color.White,maxLines=2)
                    Text("모든 탑승자의 공통 설정",style=MaterialTheme.typography.bodyLarge,color=Color(0xFFE5E4F4))
                }
            }
            Text("선호 설정만 반영 / 음원 재생 연결 전",style=MaterialTheme.typography.bodyLarge,color=Color(0xFFE5E4F4))
        }
    }
}

@Composable
private fun MusicArtwork(modifier: Modifier) {
    val colors=MoriColors
    Canvas(modifier) {
        val center=Offset(size.width/2,size.height/2)
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFF7C8BFA),Color(0xFFA89CE8),Color(0xFF5D5EA5))),cornerRadius=androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()))
        drawCircle(Color.White.copy(alpha=.16f),size.width*.39f,center)
        drawCircle(Color.White.copy(alpha=.4f),size.width*.3f,center,style=Stroke(1.dp.toPx()))
        drawCircle(colors.Navy.copy(alpha=.75f),size.width*.19f,center)
        drawCircle(Color(0xFFE9DFFF),size.width*.05f,center)
    }
}
