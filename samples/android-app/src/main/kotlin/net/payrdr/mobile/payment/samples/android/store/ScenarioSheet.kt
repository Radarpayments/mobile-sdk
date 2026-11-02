package net.payrdr.mobile.payment.samples.android.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.model.Scenario
import net.payrdr.mobile.payment.samples.android.store.theme.StoreFonts

internal const val SCENARIO_CARDS_TAG = "scenario_with_cards"

/**
 * The demo panel. Neutral on purpose — it is a tool for the person showing the demo, not part of the
 * store — and the same in every cover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioSheet(
    scenario: Scenario,
    onChange: (Scenario) -> Unit,
    onResetBuyer: () -> Unit,
    onDismiss: () -> Unit,
) {
    MaterialTheme(colorScheme = StartColors.scheme) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = StartColors.surface,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(
                    stringResource(R.string.scenario_title),
                    style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                    color = StartColors.text,
                )
                GroupHeader(R.string.scenario_buyer)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !scenario.buyerWithCards,
                        onClick = { onChange(scenario.copy(buyerWithCards = false)) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(stringResource(R.string.scenario_guest)) }
                    SegmentedButton(
                        selected = scenario.buyerWithCards,
                        onClick = { onChange(scenario.copy(buyerWithCards = true)) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag(SCENARIO_CARDS_TAG),
                    ) { Text(stringResource(R.string.scenario_with_cards)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        stringResource(R.string.scenario_with_cards_hint),
                        modifier = Modifier.weight(1f),
                        style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 13.sp, lineHeight = 18.sp),
                        color = StartColors.muted,
                    )
                    TextButton(onClick = onResetBuyer) { Text(stringResource(R.string.scenario_reset), color = StartColors.link) }
                }
                GroupHeader(R.string.scenario_payment)
                ToggleRow(R.string.scenario_wallet, scenario.wallet) { onChange(scenario.copy(wallet = it)) }
                HorizontalDivider(color = StartColors.border)
                ToggleRow(R.string.scenario_scanner, scenario.scanner) { onChange(scenario.copy(scanner = it)) }
                HorizontalDivider(color = StartColors.border)
                ToggleRow(R.string.scenario_sdk_result, scenario.sdkResultScreen) { onChange(scenario.copy(sdkResultScreen = it)) }
            }
        }
    }
}

@Composable
internal fun GroupHeader(title: Int) {
    Text(
        stringResource(title).uppercase(),
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp),
        color = StartColors.muted,
    )
}

@Composable
private fun ToggleRow(
    title: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(title), style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 15.sp), color = StartColors.text)
        Switch(checked = checked, onCheckedChange = null)
    }
}
