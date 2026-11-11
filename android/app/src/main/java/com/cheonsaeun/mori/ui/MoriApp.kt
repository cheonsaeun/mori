package com.cheonsaeun.mori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val Forest = Color(0xFF183D32)
private val Canvas = Color(0xFFF5F7F1)

@Composable
fun MoriApp(model: MoriViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, background = Canvas)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(40.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("MORI", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Forest)
                    Text("첫 번째 출발", color = Forest, fontSize = 16.sp)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().background(Forest, RoundedCornerShape(28.dp)).padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("함께 타는 사람을 위한\n다정한 출발 준비", color = Color.White, fontSize = 36.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold)
                    Text("동승자의 선호와 이번 이동의 요청을 함께 살피는 카 컨시어지", color = Color(0xFFD3E7D7), fontSize = 18.sp)
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("차량 연결 확인", fontWeight = FontWeight.Bold, color = Forest, fontSize = 22.sp)
                        Text(state.title, fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)
                        Text(state.detail, color = Color(0xFF59665F), fontSize = 17.sp)
                        Button(onClick = model::checkVehicle, enabled = !state.loading) {
                            Text(if (state.loading) "확인 중…" else "차량 상태 조회", modifier = Modifier.padding(8.dp), fontSize = 18.sp)
                        }
                        if (state.success) Text("실제 차량 응답을 확인했습니다", color = Forest)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("이제 함께 만들어 갈 경험", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Forest)
                    Text("음성으로 동승자와 좌석 등록 → 공조와 음악 준비안 → 확인 후 적용", color = Color(0xFF59665F), fontSize = 17.sp)
                    Text("현재는 시작 화면과 차량 조회가 준비되어 있습니다.", color = Color(0xFF59665F), fontSize = 15.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
