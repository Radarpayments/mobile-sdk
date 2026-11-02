package net.payrdr.mobile.payment.samples.android.store.coffee

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.theme.coverStyle

/** The coffee shop's success headline: the order number in large type, to show the barista. */
@Composable
fun CoffeeOrderNumber(number: Int) {
    val style = coverStyle(CoverId.COFFEE)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.result_success_coffee),
            style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 17.sp),
            color = style.muted,
        )
        Text(
            number.toString(),
            style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.ExtraBold, fontSize = 88.sp, lineHeight = 96.sp),
            color = style.text,
        )
    }
}
