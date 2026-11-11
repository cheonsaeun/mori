package com.cheonsaeun.mori.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MoriPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = MoriShapes.Panel, color = MoriColors.Surface, border = BorderStroke(1.dp, MoriColors.Outline.copy(alpha = .7f))) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
fun StatusPill(text: String, color: Color = MoriColors.Primary, background: Color = MoriColors.Soft, dot: Boolean = false) {
    Row(Modifier.background(background, MoriShapes.Pill).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        if (dot) Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun MoriMark(modifier: Modifier = Modifier) {
    val colors=MoriColors
    Canvas(modifier) {
        val sx = size.width / 108f; val sy = size.height / 88f
        val left = Path().apply { moveTo(15*sx, 68*sy); cubicTo(25*sx,55*sy,33*sx,41*sy,41*sx,31*sy); cubicTo(44*sx,27*sy,46*sx,28*sy,50*sx,34*sy); lineTo(64*sx,54*sy) }
        drawPath(left, Brush.linearGradient(listOf(colors.Blue, colors.Primary)), style = Stroke(25*sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val right = Path().apply { moveTo(49*sx,64*sy); lineTo(72*sx,32*sy); cubicTo(75*sx,28*sy,78*sx,29*sy,81*sx,34*sy); lineTo(96*sx,66*sy) }
        drawPath(right, Brush.linearGradient(listOf(Color(0xFFA0ADFF), colors.Lavender)), style = Stroke(25*sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun MoriIcon(kind: String, modifier: Modifier = Modifier.size(22.dp), color: Color = MoriColors.Primary) {
    Canvas(modifier) {
        val s = size.width / 24f
        fun point(x: Float, y: Float) = Offset(x*s,y*s)
        fun line(x: Float, y: Float, xx: Float, yy: Float) = drawLine(color, point(x,y), point(xx,yy), 1.7f*s, StrokeCap.Round)
        fun curve(path: Path) = drawPath(path,color,style=Stroke(1.7f*s,cap=StrokeCap.Round,join=StrokeJoin.Round))
        when (kind) {
            "heat" -> for (x in listOf(6f,12f,18f)) curve(Path().apply { moveTo(x*s,20*s); cubicTo((x-5)*s,15*s,(x+5)*s,10*s,x*s,4*s) })
            "cool", "climate" -> {
                line(12f,2f,12f,22f); line(3f,7f,21f,17f); line(3f,17f,21f,7f)
                line(9f,4f,12f,7f); line(15f,4f,12f,7f); line(9f,20f,12f,17f); line(15f,20f,12f,17f)
            }
            "seat" -> { line(7f,3f,9f,14f); line(9f,14f,19f,14f); line(5f,18f,19f,18f); line(8f,18f,6f,22f) }
            "music" -> { line(9f,16f,9f,5f); line(9f,5f,19f,3f); line(19f,3f,19f,14f); drawCircle(color,3*s,point(6f,17f)); drawCircle(color,3*s,point(16f,15f)) }
            "radio" -> { drawRoundRect(color,point(3f,8f),Size(18*s,13*s),CornerRadius(3*s),style=Stroke(1.7f*s)); line(5f,7f,18f,2f); drawCircle(color,3*s,point(8f,14f),style=Stroke(1.7f*s)); line(14f,12f,18f,12f); line(14f,16f,18f,16f) }
            "mic" -> { drawRoundRect(color,point(9f,2f),Size(6*s,12*s),CornerRadius(3*s),style=Stroke(1.7f*s)); drawArc(color,0f,180f,false,point(5f,8f),Size(14*s,10*s),style=Stroke(1.7f*s)); line(12f,18f,12f,22f); line(8f,22f,16f,22f) }
            "check" -> { line(5f,12f,10f,17f); line(10f,17f,20f,6f) }
            else -> { drawCircle(color,4*s,point(12f,7f),style=Stroke(1.7f*s)); drawArc(color,180f,180f,false,point(4f,14f),Size(16*s,12*s),style=Stroke(1.7f*s)) }
        }
    }
}

@Composable
fun MetricPill(icon: String, label: String, value: String, tint: Color = MoriColors.Secondary, background: Color = MoriColors.Soft, modifier: Modifier = Modifier) {
    Column(modifier.background(background,MoriShapes.Item).padding(12.dp), verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            MoriIcon(icon, Modifier.size(16.dp),tint)
            Text(label,style=MaterialTheme.typography.labelSmall,color=tint)
        }
        Text(value,style=MaterialTheme.typography.labelLarge,color=tint,maxLines=1)
    }
}
