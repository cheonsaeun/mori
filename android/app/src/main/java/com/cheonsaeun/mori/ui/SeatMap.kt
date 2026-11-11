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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.cheonsaeun.mori.design.*
import com.cheonsaeun.mori.model.*

@Composable
fun CabinMap(state: DashboardState, onChange: (SeatId) -> Unit, onSave: (SeatId) -> Unit, onRestore: (SeatId) -> Unit, onAdjust: (SeatId) -> Unit, modifier: Modifier = Modifier) {
    MoriPanel(modifier) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("좌석별 상태",style=MaterialTheme.typography.headlineMedium)
            Text("↑ 차량 앞쪽",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            state.vehicle.seats.chunked(2).forEach { pair ->
                Row(Modifier.height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    pair.forEach { seat ->
                        val profile=state.profiles.firstOrNull {it.id==state.assignments[seat.id]}
                        val available=listOf(seat.backrest.value,seat.slide.value,seat.height.value,seat.heat.value,seat.ventilation.value).any {it!=null}
                        Surface(Modifier.weight(1f).fillMaxHeight(),shape=MoriShapes.Card,color=if(profile!=null) MoriColors.Soft else MoriColors.Surface,border=BorderStroke(1.dp,MoriColors.Outline)) {
                            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                    Text(seat.id.label,style=MaterialTheme.typography.titleMedium)
                                    Text(seat.occupant.label {if(it) "탑승" else "빈자리"},style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
                                }
                                Row(Modifier.fillMaxWidth().heightIn(min=100.dp).clickable(enabled=!state.controlBusy && available,role=Role.Button) {onAdjust(seat.id)},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                    SeatGlyph(seat,profile!=null,Modifier.width(54.dp).height(70.dp))
                                    Column {
                                        Text(profile?.name ?: if(seat.occupant.value==false) "빈자리" else "게스트",style=MaterialTheme.typography.titleLarge,maxLines=1)
                                        Text(when {profile==null -> "프로필 미지정"; !profile.seat.configured -> "좌석 선호 미등록"; profile.seat.differsFrom(seat) -> "저장 후 설정 변경됨"; profile.seat.matches(seat) -> "저장된 좌석 설정"; else -> "일부 설정 확인 필요"},style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary,maxLines=2)
                                        Text("앞뒤 ${seat.slide.label {it.toString()}} / 등받이 ${seat.backrest.label {it.toString()}} / 높이 ${seat.height.label {it.toString()}}  ›",style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary,maxLines=2)
                                    }
                                }
                                Row(Modifier.heightIn(min=80.dp).clickable(enabled=!state.controlBusy && available,role=Role.Button) {onAdjust(seat.id)},horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    MetricPill("heat","열선 조절",seat.heat.label {if(it==0) "꺼짐" else "$it 단계"},MoriColors.Heat,if((seat.heat.value ?: 0)>0) MoriColors.HeatSoft else MoriColors.Surface,Modifier.weight(1f))
                                    MetricPill("cool","통풍 조절",seat.ventilation.label {if(it==0) "꺼짐" else "$it 단계"},MoriColors.Cool,if((seat.ventilation.value ?: 0)>0) MoriColors.CoolSoft else MoriColors.Surface,Modifier.weight(1f))
                                }
                                Spacer(Modifier.weight(1f))
                                LargeAction("탑승자 변경",{onChange(seat.id)},Modifier.fillMaxWidth().semantics {contentDescription="${seat.id.label} 탑승자 변경"},enabled=!state.controlBusy)
                                if(profile==null) LargeAction("프로필로 저장",{onSave(seat.id)},Modifier.fillMaxWidth(),enabled=!state.controlBusy && available,secondary=true)
                                else Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    LargeAction("지금 상태 저장",{onSave(seat.id)},Modifier.weight(1f),enabled=!state.controlBusy && available,secondary=true)
                                    LargeAction("저장 설정 복원",{onRestore(seat.id)},Modifier.weight(1f).semantics {contentDescription="${seat.id.label} 저장 설정 복원"},enabled=!state.controlBusy && profile.seat.configured,secondary=true)
                                }
                            }
                        }
                    }
                }
            }
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
