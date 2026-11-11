package com.cheonsaeun.mori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cheonsaeun.mori.design.*
import com.cheonsaeun.mori.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID

@Composable
fun MoriApp(model: MoriViewModel = viewModel(), darkTheme: Boolean? = null) {
    val state by model.state.collectAsStateWithLifecycle()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle,state.preview) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(isActive && !state.preview) { model.refresh(); delay(MoriMotion.PollMillis) }
        }
    }
    var editing by remember { mutableStateOf<PersonProfile?>(null) }
    var bindOnSave by remember { mutableStateOf(false) }
    fun capture(id: SeatId) {
        model.selectSeat(id)
        val saved=state.profiles.firstOrNull {it.id==state.assignments[id]}
        val seat=state.vehicle.seats.first {it.id==id}
        editing=(saved ?: PersonProfile(UUID.randomUUID().toString(), "", climate=ClimatePreference(state.vehicle.zones.firstNotNullOfOrNull {it.temperature.value},state.vehicle.zones.firstNotNullOfOrNull {it.fan.value}),media=state.media)).let { it.copy(seat=SeatPreference.capture(seat,it.seat)) }
        bindOnSave=true
    }
    MoriTheme(darkTheme) {
        Surface(Modifier.fillMaxSize(),color=MoriColors.Canvas) {
            BoxWithConstraints(Modifier.background(MoriColors.AmbientGradient).padding(24.dp)) {
                val stacked=maxWidth<1000.dp
                Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    if(stacked) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            MoriMark(Modifier.size(58.dp)); Text("MORI",style=MaterialTheme.typography.headlineLarge)
                            Spacer(Modifier.weight(1f))
                            StatusPill(if(state.preview) "예시 데이터" else if(state.vehicle.received) "차량 연결됨" else "연결 확인 중",dot=true)
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            EmulatorControls(state,model::sendEmulatorCommand)
                            OutlinedButton(enabled=!state.controlBusy,onClick={editing=null; model.togglePreview()},modifier=Modifier.heightIn(min=80.dp)) { Text(if(state.preview) "실제 차량 보기" else "디자인 미리보기") }
                        }
                    } else Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                        MoriMark(Modifier.width(64.dp).height(58.dp))
                        Column(Modifier.weight(1f)) {
                            Text("MORI",style=MaterialTheme.typography.headlineLarge)
                            Text("모두의 편안함을 한눈에",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
                        }
                        StatusPill(when {state.preview -> "예시 데이터"; state.simulatorActive==true -> "차량 시뮬레이터 연결됨"; state.vehicle.received -> "차량 상태 연결됨"; else -> "차량 연결 확인 중"},dot=true)
                        EmulatorControls(state,model::sendEmulatorCommand)
                        OutlinedButton(enabled=!state.controlBusy,onClick={editing=null; model.togglePreview()},modifier=Modifier.heightIn(min=80.dp)) { Text(if(state.preview) "실제 차량 보기" else "디자인 미리보기") }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        LargeTab("공통 기능",state.commonTab,{model.selectTab(true)},Modifier.weight(1f))
                        LargeTab("개별 좌석",!state.commonTab,{model.selectTab(false)},Modifier.weight(1f))
                    }
                    if(state.controlFeedback!=null || state.controlBusy) {
                        Row(Modifier.fillMaxWidth().background(MoriColors.Soft,MoriShapes.Item).padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            if(state.controlBusy) CircularProgressIndicator(Modifier.size(28.dp),strokeWidth=3.dp)
                            Text(state.controlFeedback ?: "처리하고 있어요…",style=MaterialTheme.typography.bodyLarge,maxLines=3)
                        }
                    }
                    if(state.commonTab) {
                        CommonDashboard(state,model,stacked,Modifier.weight(1f))
                    } else if(stacked) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                            CabinMap(state,{editing=null; model.selectSeat(it)},::capture,Modifier.fillMaxWidth().height(850.dp))
                            ProfilePane(state,model,editing,bindOnSave,{p,bind -> editing=p;bindOnSave=bind},Modifier.fillMaxWidth().height(900.dp))
                        }
                    } else Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                        CabinMap(state,{editing=null;model.selectSeat(it)},::capture,Modifier.weight(.56f).fillMaxHeight())
                        ProfilePane(state,model,editing,bindOnSave,{p,bind -> editing=p;bindOnSave=bind},Modifier.weight(.44f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
fun LargeTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val view=LocalView.current
    Button(onClick={view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);onClick()},modifier=modifier.heightIn(min=88.dp),shape=MoriShapes.Item,colors=ButtonDefaults.buttonColors(containerColor=if(selected) MoriColors.Primary else MoriColors.Surface,contentColor=if(selected) MoriColors.OnPrimary else MoriColors.Secondary)) {
        Text(label,style=MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun LargeAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, secondary: Boolean = false) {
    val view=LocalView.current
    val action={view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);onClick()}
    if(secondary) OutlinedButton(onClick=action,enabled=enabled,modifier=modifier.heightIn(min=80.dp),shape=MoriShapes.Item) { Text(label,style=MaterialTheme.typography.titleMedium,maxLines=2) }
    else Button(onClick=action,enabled=enabled,modifier=modifier.heightIn(min=80.dp),shape=MoriShapes.Item) { Text(label,style=MaterialTheme.typography.titleMedium,maxLines=2) }
}
