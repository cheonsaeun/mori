package com.cheonsaeun.mori.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cheonsaeun.mori.design.*
import com.cheonsaeun.mori.model.*

@Composable
fun CabinMap(state: DashboardState, onSeat: (SeatId) -> Unit, modifier: Modifier = Modifier) {
    MoriPanel(modifier) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Column {
                Text("각자의 자리",style=MaterialTheme.typography.titleLarge)
                Text("좌석별 시트 자세와 열선 / 통풍",style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
            }
            StatusPill("개별 설정")
        }
        Box(Modifier.fillMaxWidth().height(26.dp),contentAlignment=Alignment.Center) {
            Text("↑  차량 앞쪽",style=MaterialTheme.typography.labelSmall,color=MoriColors.Secondary)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            state.vehicle.seats.chunked(2).forEach { pair ->
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    pair.forEach { seat -> SeatTile(seat,seat.id==state.selectedSeat,{onSeat(seat.id)},Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
        val selected = state.vehicle.seats.first { it.id==state.selectedSeat }
        Row(Modifier.fillMaxWidth().background(MoriColors.Soft,MoriShapes.Item).padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            MoriIcon("seat")
            Column(Modifier.weight(1f)) {
                Text("${selected.id.label} 자세",style=MaterialTheme.typography.titleSmall)
                Text("등받이는 차량 위치값으로 표시해요. 실제 각도와 다를 수 있어요.",style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
            }
            Text(selected.backrest.label { "위치 $it" },style=MaterialTheme.typography.titleMedium,color=MoriColors.Primary)
        }
    }
}

@Composable
private fun SeatTile(seat: SeatSnapshot, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val fill by animateColorAsState(if(selected) Color(0xFFF0EFFF) else Color(0xFFFAFAFD),tween(MoriMotion.SelectionMillis),label="seat-selection")
    val occupied = seat.occupant.value
    val status = seat.occupant.label { if(it) "탑승" else "빈 좌석" }
    Surface(
        modifier=modifier.semantics { contentDescription="${seat.id.label}, $status, 열선 ${seat.heat.label { it.toString() }}, 통풍 ${seat.ventilation.label { it.toString() }}" }.clickable(role=Role.Tab,onClick=onClick),
        shape=MoriShapes.Card,color=fill,
        border=BorderStroke(if(selected) 2.dp else 1.dp,if(selected) MoriColors.Primary.copy(alpha=.65f) else MoriColors.Outline),
    ) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text(seat.id.label,style=MaterialTheme.typography.titleSmall,color=MoriColors.Secondary)
                if(occupied==true) StatusPill(status,background=Color.White) else Text(status,style=MaterialTheme.typography.labelMedium,color=MoriColors.Muted)
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                SeatGlyph(seat,selected,Modifier.width(78.dp).height(108.dp))
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(seat.displayName ?: when(occupied) { true -> if(seat.id==SeatId.DRIVER) "운전자" else "동승자"; false -> "편안한 빈자리"; null -> "탑승 상태 확인" },style=MaterialTheme.typography.titleLarge,color=if(occupied==false) MoriColors.Secondary else MoriColors.Ink)
                    Text(if(seat.displayName!=null) "이번 이동에 함께해요" else when(occupied) { true -> "프로필을 연결할 수 있어요"; false -> "좌석 설정은 따로 유지해요"; null -> "차량의 응답을 기다려요" },style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                val hot=(seat.heat.value ?: 0)>0
                val cool=(seat.ventilation.value ?: 0)>0
                MetricPill("seat","등받이",seat.backrest.label { "위치 $it" },if(selected) MoriColors.Primary else MoriColors.Secondary,Color.White,Modifier.weight(1f))
                MetricPill("heat","열선",seat.heat.label { if(it==0) "꺼짐" else "$it 단계" },if(hot) MoriColors.Heat else MoriColors.Secondary,if(hot) MoriColors.HeatSoft else Color.White,Modifier.weight(1f))
                MetricPill("cool","통풍",seat.ventilation.label { if(it==0) "꺼짐" else "$it 단계" },if(cool) MoriColors.Cool else MoriColors.Secondary,if(cool) MoriColors.CoolSoft else Color.White,Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SeatGlyph(seat: SeatSnapshot, selected: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val w=size.width; val h=size.height
        val color=if(selected) Color(0xFF8686EC) else Color(0xFFC1C2D8)
        val hot=(seat.heat.value ?: 0)>0
        val cool=(seat.ventilation.value ?: 0)>0
        val accent=when { hot -> Color(0xFFE9A29C); cool -> Color(0xFF96CDE8); else -> color }
        drawRoundRect(color.copy(alpha=.15f),Offset(w*.08f,h*.12f),Size(w*.84f,h*.85f),CornerRadius(w*.22f))
        drawRoundRect(Brush.linearGradient(listOf(accent.copy(alpha=.7f),color.copy(alpha=.45f))),Offset(w*.22f,h*.23f),Size(w*.56f,h*.43f),CornerRadius(w*.12f))
        drawRoundRect(color,Offset(w*.31f,h*.03f),Size(w*.38f,h*.17f),CornerRadius(w*.07f))
        drawRoundRect(Brush.linearGradient(listOf(accent,color.copy(alpha=.6f))),Offset(w*.21f,h*.69f),Size(w*.58f,h*.23f),CornerRadius(w*.1f))
        for (x in listOf(.34f,.5f,.66f)) drawLine(Color.White.copy(alpha=.55f),Offset(w*x,h*.33f),Offset(w*x,h*.57f),2.dp.toPx(),StrokeCap.Round)
        if(cool) for(x in listOf(.30f,.5f,.70f)) drawCircle(MoriColors.Cool.copy(alpha=.6f),2.dp.toPx(),Offset(w*x,h*.80f))
        if(selected) drawRoundRect(MoriColors.Primary.copy(alpha=.25f),Offset(w*.06f,h*.01f),Size(w*.88f,h*.96f),CornerRadius(w*.22f),style=Stroke(1.dp.toPx()))
    }
}
