package net.payrdr.mobile.payment.samples.android.store.invest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The portfolio's line with a fading area under it, ending in a dot. */
@Composable
internal fun PortfolioChart(
    period: ChartPeriod,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxWidth()) {
        val points = period.points
        val step = size.width / (points.size - 1)
        val pad = 6.dp.toPx()

        fun at(i: Int) = Offset(i * step, pad + (1f - points[i]) * (size.height - 2 * pad))
        val line = Path().apply { points.indices.forEach { i -> if (i == 0) moveTo(at(i).x, at(i).y) else lineTo(at(i).x, at(i).y) } }
        val area =
            Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(area, Brush.verticalGradient(listOf(InvestColors.accent.copy(alpha = 0.28f), InvestColors.accent.copy(alpha = 0f))))
        drawPath(line, InvestColors.accent, style = Stroke(width = 2.2.dp.toPx(), join = StrokeJoin.Round))
        drawCircle(InvestColors.accent, radius = 4.dp.toPx(), center = at(points.lastIndex))
    }
}

/** 1W / 1M / 3M / 1Y / All. */
@Composable
internal fun PeriodSelector(
    selected: ChartPeriod,
    onSelect: (ChartPeriod) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InvestColors.surface)
            .border(1.dp, InvestColors.divider, RoundedCornerShape(12.dp))
            .padding(3.dp)
            .selectableGroup(),
    ) {
        ChartPeriod.entries.forEach { period ->
            val on = period == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) InvestColors.surface2 else InvestColors.surface)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(period) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(period.label),
                    style =
                        plex(
                            13.sp,
                            if (on) FontWeight.SemiBold else FontWeight.Medium,
                            if (on) InvestColors.text else InvestColors.muted,
                        ),
                )
            }
        }
    }
}
