package com.cheonsaeun.mori.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cheonsaeun.mori.design.*
import com.cheonsaeun.mori.model.*

@Composable
fun CabinMap(state: DashboardState, onSeat: (SeatId) -> Unit, onCapture: (SeatId) -> Unit, modifier: Modifier = Modifier) {
    MoriPanel(modifier) {
        Text("각자의 자리",style=MaterialTheme.typography.headlineMedium)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("↑ 차량 앞쪽",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary,modifier=Modifier.align(Alignment.CenterHorizontally))
            state.vehicle.seats.chunked(2).forEach { pair ->
                Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    pair.forEach { seat ->
                        val profile=state.profiles.firstOrNull {it.id==state.assignments[seat.id]}
                        val selected=seat.id==state.selectedSeat
                        Surface(Modifier.weight(1f).clickable(role=Role.Button) {onSeat(seat.id)},shape=MoriShapes.Card,color=if(selected) MoriColors.Soft else MoriColors.Surface,border=BorderStroke(if(selected) 2.dp else 1.dp,if(selected) MoriColors.Primary else MoriColors.Outline)) {
                            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                                    Text(seat.id.label,style=MaterialTheme.typography.titleMedium)
                                    Text(seat.occupant.label {if(it) "탑승" else "빈자리"},style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
                                }
                                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                    SeatGlyph(seat,selected,Modifier.width(58.dp).height(86.dp))
                                    Column {
                                        Text(profile?.name ?: seat.displayName ?: "프로필 미연결",style=MaterialTheme.typography.titleLarge,maxLines=1)
                                        Text(when {profile==null -> "현재 좌석 설정"; profile.seat.differsFrom(seat) -> "저장 후 설정 변경됨"; profile.seat.matches(seat) -> "저장된 설정과 같음"; else -> "설정 확인 중"},style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary,maxLines=2)
                                    }
                                }
                                Text("앞뒤 ${seat.slide.label {it.toString()}} / 등받이 ${seat.backrest.label {it.toString()}} / 높이 ${seat.height.label {it.toString()}}",style=MaterialTheme.typography.bodyMedium,maxLines=2,color=MoriColors.Secondary)
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    MetricPill("heat","열선",seat.heat.label {if(it==0) "꺼짐" else "$it 단계"},MoriColors.Heat,if((seat.heat.value ?: 0)>0) MoriColors.HeatSoft else MoriColors.Surface,Modifier.weight(1f))
                                    MetricPill("cool","통풍",seat.ventilation.label {if(it==0) "꺼짐" else "$it 단계"},MoriColors.Cool,if((seat.ventilation.value ?: 0)>0) MoriColors.CoolSoft else MoriColors.Surface,Modifier.weight(1f))
                                }
                                LargeAction(if(profile==null) "현재 설정 저장" else "현재 설정으로 수정",{onCapture(seat.id)},Modifier.fillMaxWidth(),enabled=!state.preview && !state.controlBusy && listOf(seat.backrest.value,seat.slide.value,seat.height.value,seat.heat.value,seat.ventilation.value).any {it!=null},secondary=!selected)
                            }
                        }
                    }
                }
            }
            Text("자세는 차량의 위치값이에요. 프로필은 오른쪽에서 연결해요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
        }
    }
}

@Composable
fun SeatGlyph(seat: SeatSnapshot, selected: Boolean, modifier: Modifier) {
    val colors=MoriColors
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
        if(cool) for(x in listOf(.30f,.5f,.70f)) drawCircle(colors.Cool.copy(alpha=.6f),2.dp.toPx(),Offset(w*x,h*.80f))
        if(selected) drawRoundRect(colors.Primary.copy(alpha=.25f),Offset(w*.06f,h*.01f),Size(w*.88f,h*.96f),CornerRadius(w*.22f),style=Stroke(1.dp.toPx()))
    }
}
