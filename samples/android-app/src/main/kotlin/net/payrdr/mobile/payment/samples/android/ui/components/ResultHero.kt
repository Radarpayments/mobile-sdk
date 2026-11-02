package net.payrdr.mobile.payment.samples.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Success/error hero card. `isSuccess = true` → green checkmark + "success" subtitle;
 * `false` → red cross + error-message subtitle. kv-rows render as two-column text list.
 */
@Composable
fun ResultHero(
    isSuccess: Boolean,
    title: String,
    subtitle: String,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val grad =
        if (isSuccess) {
            BrandGradients.Success
        } else {
            Gradient(listOf(Color(0xFFFB7185), Color(0xFFE11D48)))
        }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(grad.colorStops)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isSuccess) "✓" else "✕", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 11.sp, color = Color(0xFF6C7AA5), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        rows.forEach { (k, v) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                // The label keeps its width; a long value (payment id) wraps on the right instead.
                Text(k, fontSize = 11.sp, color = Color(0xFF8A95B0), modifier = Modifier.padding(end = 12.dp))
                Text(
                    v,
                    fontSize = 11.sp,
                    color = Color(0xFF1A2341),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
