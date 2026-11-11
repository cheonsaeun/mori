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

@Composable
fun SharedSummary(state: DashboardState, onClimate: () -> Unit, onMedia: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        MoriPanel(Modifier.fillMaxWidth().heightIn(min=360.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {MoriIcon("climate");Text("함께 쓰는 공조",style=MaterialTheme.typography.headlineMedium)}
            val matches=state.profileChoices().filter {it.climate.matches(state.vehicle)}
            val power=state.vehicle.zones.mapNotNull {it.power.value}.distinct().singleOrNull()
            val title=when {
                power==false -> "공조 시스템이 꺼져 있어요"
                matches.isNotEmpty() -> if(matches.size==1) "${matches.first().name}의 선호와 같아요" else "${matches.first().name} 외 ${matches.size-1}명의 선호와 같아요"
                state.vehicle.zones.any {it.temperature.value!=null || it.fan.value!=null} -> "직접 조정된 공조예요"
                else -> "공조 상태를 확인하고 있어요"
            }
            Column(Modifier.fillMaxWidth().background(MoriColors.Soft,MoriShapes.Card).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(title,style=MaterialTheme.typography.titleLarge)
                Text(if(matches.isNotEmpty() && power!=false) climateLabel(matches.first().climate) else if(power==false) "원하는 사람의 선호로 출발을 준비해 보세요." else "우리 취향에 맞게 바꿔볼까요?",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary,maxLines=3)
            }
            LargeAction("다른 취향으로 바꾸기",onClimate,Modifier.fillMaxWidth(),enabled=!state.controlBusy)
        }
        Surface(Modifier.fillMaxWidth().heightIn(min=370.dp),shape=MoriShapes.Panel,color=MoriColors.Navy) {
            Column(Modifier.background(MoriColors.MediaGradient).padding(24.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {MoriIcon(if(state.media.radio) "radio" else "music",color=Color.White);Text("함께 듣는 ${if(state.media.radio) "라디오" else "음악"}",style=MaterialTheme.typography.headlineMedium,color=Color.White)}
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    MusicArtwork(Modifier.size(92.dp))
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        val people=state.profileChoices().filter {it.media.selection.isNotBlank() && it.media==state.media}
                        Text(if(people.isEmpty()) if(state.media.selection.isBlank()) "함께 들을 취향을 골라요" else "직접 고른 미디어 취향" else "${people.first().name}의 ${if(state.media.radio) "라디오" else "음악"} 취향",style=MaterialTheme.typography.titleLarge,color=Color.White,maxLines=2)
                        Text(state.media.selection.ifBlank {"아직 선택하지 않았어요"},style=MaterialTheme.typography.bodyLarge,color=Color(0xFFE5E4F4),maxLines=2)
                    }
                }
                LargeAction("다른 취향으로 바꾸기",onMedia,Modifier.fillMaxWidth(),enabled=!state.controlBusy)
                Text("선호 선택만 반영 / 음원 재생 연결 전",style=MaterialTheme.typography.bodyMedium,color=Color(0xFFE5E4F4))
            }
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
