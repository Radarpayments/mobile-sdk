package net.payrdr.mobile.payment.samples.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.payrdr.mobile.payment.samples.android.ui.toCaps

/**
 * Labeled input shell with floating UPPERCASE label above the value. Emulates the polished
 * form inputs from the sample design mockups. `error` renders outline red and shows helper text.
 */
@Composable
fun InputShell(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor =
        when {
            error != null -> Color(0xFFE11D48)
            focused -> Color(0xFF6366F1)
            else -> Color(0xFFE1E6EE)
        }
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column {
                Text(
                    label.toCaps(),
                    color = Color(0xFF8A95B0),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                )
                Spacer(Modifier.height(2.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = TextStyle(color = Color(0xFF1A2341), fontSize = 14.sp),
                    decorationBox = { inner ->
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, color = Color(0xFFBBBBBB), fontSize = 14.sp)
                        }
                        inner()
                    },
                )
            }
        }
        if (error != null) {
            Spacer(Modifier.height(3.dp))
            Text(error, color = Color(0xFFE11D48), fontSize = 10.sp)
        }
    }
}
