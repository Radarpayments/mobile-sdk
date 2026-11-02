package net.payrdr.mobile.payment.samples.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronLeft

/** Consistent top bar across all screens: status-bar safe area + back chevron, title, subtitle. */
@Composable
fun NavTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String = "",
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (onBack != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .clickable(onClick = onBack)
                        .padding(vertical = 4.dp),
            ) {
                Icon(
                    imageVector = TablerIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text("Back", color = Color(0xFF4F46E5), fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A2341))
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF6C7AA5))
        }
    }
}
