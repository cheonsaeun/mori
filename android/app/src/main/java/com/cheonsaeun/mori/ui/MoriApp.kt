package com.cheonsaeun.mori.ui

import android.view.HapticFeedbackConstants
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

@Composable
fun MoriApp(model: MoriViewModel = viewModel(), darkTheme: Boolean? = null) {
    val state by model.state.collectAsStateWithLifecycle()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(isActive) {model.refresh();delay(MoriMotion.PollMillis)}
        }
    }
    var overlay by remember {mutableStateOf<DashboardOverlay?>(null)}
    fun open(kind: OverlayKind, seat: SeatId = state.selectedSeat) {model.clearFeedback();model.selectSeat(seat);overlay=DashboardOverlay(kind,seat)}
    MoriTheme(darkTheme) {
        Surface(Modifier.fillMaxSize(),color=MoriColors.Canvas) {
            BoxWithConstraints(Modifier.background(MoriColors.AmbientGradient).padding(24.dp)) {
                val stacked=maxWidth<1050.dp
                Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        MoriMark(Modifier.width(60.dp).height(52.dp))
                        Column(Modifier.weight(1f)) {
                            Text("MORI",style=MaterialTheme.typography.headlineLarge)
                            if(!stacked) Text("모두의 편안함을 한눈에",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
                        }
                        EmulatorControls(state,model::sendEmulatorCommand)
                        LargeAction("프로필 관리",{open(OverlayKind.MANAGE)},enabled=!state.controlBusy,secondary=true)
                    }
                    if(state.controlFeedback!=null || state.controlBusy) Row(Modifier.fillMaxWidth().background(MoriColors.Soft,MoriShapes.Item).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(state.controlBusy) CircularProgressIndicator(Modifier.size(28.dp),strokeWidth=3.dp)
                        Text(state.controlFeedback ?: "처리하고 있어요…",style=MaterialTheme.typography.bodyMedium,maxLines=3)
                    }
                    if(stacked) Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                        CabinMap(state,{open(OverlayKind.OCCUPANT,it)},{open(OverlayKind.SAVE,it)},{open(OverlayKind.RESTORE,it)},{open(OverlayKind.SEAT,it)},Modifier.fillMaxWidth().height(1150.dp))
                        SharedSummary(state,{open(OverlayKind.CLIMATE)},{open(OverlayKind.MEDIA)},Modifier.fillMaxWidth().height(780.dp))
                    } else Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                        CabinMap(state,{open(OverlayKind.OCCUPANT,it)},{open(OverlayKind.SAVE,it)},{open(OverlayKind.RESTORE,it)},{open(OverlayKind.SEAT,it)},Modifier.weight(.6f).fillMaxHeight())
                        SharedSummary(state,{open(OverlayKind.CLIMATE)},{open(OverlayKind.MEDIA)},Modifier.weight(.4f).fillMaxHeight())
                    }
                }
            }
            overlay?.let { DashboardDialog(state,model,it,{overlay=it},{overlay=null}) }
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
