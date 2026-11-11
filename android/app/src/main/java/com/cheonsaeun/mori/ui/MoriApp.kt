package com.cheonsaeun.mori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cheonsaeun.mori.design.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun MoriApp(model: MoriViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle,state.preview) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(isActive && !state.preview) { model.refresh(); delay(MoriMotion.PollMillis) }
        }
    }
    MoriTheme {
        Surface(Modifier.fillMaxSize(),color=MoriColors.Canvas) {
            Column(Modifier.background(MoriColors.AmbientGradient).padding(MoriSpace.Page),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth().height(64.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    MoriMark(Modifier.width(62.dp).height(48.dp))
                    Column {
                        Text("MORI",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
                        Text("Mobility + Memory",style=MaterialTheme.typography.labelSmall,color=MoriColors.Secondary)
                    }
                    Spacer(Modifier.weight(1f))
                    StatusPill(
                        when { state.preview -> "디자인 미리보기"; state.simulatorActive == false -> "시뮬레이터 연결 중"; state.vehicle.received -> if(state.simulatorActive == true) "차량 시뮬레이터 연결됨" else "차량 상태 연결됨"; state.vehicle.seats.any {it.occupant.availability != com.cheonsaeun.mori.model.Availability.WAITING} -> "차량 연결 확인 필요"; else -> "차량 응답 확인 중" },
                        if(state.preview) MoriColors.Primary else if(state.vehicle.received && state.simulatorActive != false) MoriColors.Success else MoriColors.Secondary,
                        if(!state.preview && state.vehicle.received && state.simulatorActive != false) MoriColors.SuccessSoft else Color.White,dot=true,
                    )
                    EmulatorControls(state,model::sendEmulatorCommand)
                    OutlinedButton(onClick=model::togglePreview,modifier=Modifier.height(56.dp),shape=MoriShapes.Pill) {
                        Text(if(state.preview) "실제 차량 보기" else "디자인 미리보기")
                    }
                    TextButton(onClick=model::toggleGuide,modifier=Modifier.height(56.dp)) { Text(if(state.showGuide) "차량 공간" else "스타일 가이드") }
                }
                if(state.showGuide) {
                    DesignGuide(Modifier.weight(1f))
                } else {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom) {
                        Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("모두의 편안함을 한눈에",style=MaterialTheme.typography.headlineLarge)
                            Text("각자의 자리와 함께하는 공간을 살펴보세요.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
                        }
                        val count=state.vehicle.seats.count { it.occupant.value==true }
                        Text(if(state.preview) "예시 탑승자 ${count}명" else if(state.vehicle.seats.any {it.occupant.value!=null}) "확인된 탑승 ${count}명" else "탑승 상태 확인 중",style=MaterialTheme.typography.labelLarge,color=MoriColors.Secondary)
                    }
                    Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                        CabinMap(state,model::selectSeat,Modifier.weight(.56f).fillMaxHeight())
                        SharedSpace(state,Modifier.weight(.44f).fillMaxHeight())
                    }
                    Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha=.86f),MoriShapes.Card).padding(horizontal=24.dp,vertical=18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(48.dp).background(MoriColors.BrandGradient,MoriShapes.Item),contentAlignment=Alignment.Center) { MoriIcon("mic",Modifier.size(24.dp),Color.White) }
                        Column(Modifier.weight(1f)) {
                            Text("“조수석에는 민지, 뒤 오른쪽에는 준호가 탔어.”",style=MaterialTheme.typography.titleMedium)
                            Text(if(state.preview) "미리보기의 이름과 설정은 예시예요. 차량 명령은 보내지 않아요." else "좌석과 공조 상태를 자동으로 갱신해요. 음성 등록은 다음에 연결할 예정이에요.",style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
                        }
                        StatusPill(if(state.preview) "예시 데이터" else if(state.simulatorActive!=null) "개발용 연결" else "조회 전용",color=MoriColors.Secondary,background=MoriColors.Soft)
                    }
                }
            }
        }
    }
}
