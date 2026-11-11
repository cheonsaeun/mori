package com.cheonsaeun.mori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cheonsaeun.mori.design.*

@Composable
fun DesignGuide(modifier: Modifier=Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        Text("MORI Design System / 01",style=MaterialTheme.typography.headlineLarge)
        Text("선명한 블루에서 부드러운 라벤더로. 이동과 기억을 연결하는 차분한 인터페이스.",style=MaterialTheme.typography.bodyLarge,color=MoriColors.Secondary)
        MoriPanel {
            Text("Brand palette",style=MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                listOf(Triple("Primary","#555DEB",MoriColors.Primary),Triple("Blue","#748FFF",MoriColors.Blue),Triple("Lavender","#C5A9F4",MoriColors.Lavender),Triple("Navy","#171A36",MoriColors.Navy),Triple("Canvas","#F5F5FC",MoriColors.Canvas)).forEach { (name,hex,color) ->
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.fillMaxWidth().height(80.dp).background(color,MoriShapes.Item))
                        Text(name,style=MaterialTheme.typography.titleSmall)
                        Text(hex,style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(80.dp).background(MoriColors.BrandGradient,MoriShapes.Item))
            Text("그라데이션은 브랜드, 선택 강조, 음악 아트워크에 사용해요. 본문과 상태값은 단색으로 읽기 쉽게 표시해요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
            MoriPanel(Modifier.weight(1f)) {
                Text("Typography",style=MaterialTheme.typography.titleLarge)
                Text("모두의 편안함",style=MaterialTheme.typography.displaySmall)
                Text("각자의 자리",style=MaterialTheme.typography.titleLarge)
                Text("시트는 개별 설정, 공조와 음악은 공통 공간",style=MaterialTheme.typography.bodyLarge)
                Text("36/48  /  24/34  /  18/28",style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
            }
            MoriPanel(Modifier.weight(1f)) {
                Text("상태와 의미",style=MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    StatusPill("연결됨",MoriColors.Success,MoriColors.SuccessSoft,true)
                    StatusPill("선택됨")
                    StatusPill("미지원",MoriColors.Secondary,MoriColors.Soft)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    MetricPill("heat","열선","2 단계",MoriColors.Heat,MoriColors.HeatSoft,Modifier.weight(1f))
                    MetricPill("cool","통풍","2 단계",MoriColors.Cool,MoriColors.CoolSoft,Modifier.weight(1f))
                }
                Text("색과 함께 아이콘, 상태 문구를 사용해요. 확인되지 않은 값은 0으로 바꾸지 않아요.",style=MaterialTheme.typography.bodySmall,color=MoriColors.Secondary)
            }
        }
        MoriPanel {
            Text("공간과 인터랙션",style=MaterialTheme.typography.titleLarge)
            Text("간격 4 / 8 / 12 / 16 / 24 / 32   |   모서리 16 / 22 / 28   |   주요 터치 영역 64 이상",style=MaterialTheme.typography.bodyLarge)
            Text("선택 전환 220ms. 그림자와 발광은 최소화하고, 흰 표면과 옅은 보라색 경계로 깊이를 표현해요.",style=MaterialTheme.typography.bodyMedium,color=MoriColors.Secondary)
        }
    }
}
