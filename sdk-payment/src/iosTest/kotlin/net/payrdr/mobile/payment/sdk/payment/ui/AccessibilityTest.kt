package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasNoClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.ui.components.ApplePayButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.ComposeTextField
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalWalletButtonLayer
import net.payrdr.mobile.payment.sdk.payment.ui.components.MethodRow
import net.payrdr.mobile.payment.sdk.payment.ui.components.SheetOverlay
import net.payrdr.mobile.payment.sdk.payment.ui.components.SheetSurface
import net.payrdr.mobile.payment.sdk.payment.ui.components.StepHeader
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextFieldContentType
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletButtonLayer
import net.payrdr.mobile.payment.sdk.payment.ui.screens.PageScaffold
import net.payrdr.mobile.payment.sdk.payment.ui.screens.PaymentMethodsSkeleton
import net.payrdr.mobile.payment.sdk.payment.ui.screens.ProcessingContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.ResultContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.SkeletonLayout
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentThemeProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What TalkBack and VoiceOver get from the checkout: names, roles, states and announcements. */
@OptIn(ExperimentalTestApi::class)
class AccessibilityTest {
    private fun ComposeUiTest.show(content: @Composable () -> Unit) = setContent { PaymentThemeProvider(PaymentTheme.Light) { content() } }

    @Test
    fun the_sheet_text_is_read_on_its_own_not_as_one_button() =
        runComposeUiTest {
            show {
                SheetSurface(onDismissRequest = {}, modifier = Modifier, showGrabber = true, sidePadding = 20.dp) {
                    Text("Payment")
                    Text("or pay by card")
                }
            }
            onNodeWithText("Payment").assert(hasNoClickAction())
            // Nor is the handle a nameless button that closes the sheet.
            onAllNodes(hasClickAction()).assertCountEquals(0)
        }

    @Test
    fun the_scrim_behind_a_sheet_is_no_nameless_button() =
        runComposeUiTest {
            show { SheetOverlay(visible = true, onDismiss = {}) { Text("Country") } }
            onAllNodes(hasClickAction()).assertCountEquals(0)
        }

    @Test
    fun the_screen_under_an_open_sheet_is_hidden_from_screen_readers() =
        runComposeUiTest {
            show {
                PageScaffold(overlay = { SheetOverlay(visible = true, onDismiss = {}) { Text("Country") } }) {
                    Text("Card information")
                }
            }
            onNodeWithText("Country").assertExists()
            onAllNodesWithText("Card information").assertCountEquals(0)
        }

    @Test
    fun a_filled_field_keeps_its_name() =
        runComposeUiTest {
            show { field(value = "name@example.com") }
            onNode(hasSetTextAction()).assert(hasText("Email"))
        }

    @Test
    fun an_empty_field_whose_hint_is_its_name_reads_it_once() =
        runComposeUiTest {
            show { field(value = "", placeholder = "Email") }
            val texts = onNode(hasSetTextAction()).fetchSemanticsNode().config.getOrElse(SemanticsProperties.Text) { emptyList() }
            assertEquals(listOf("Email"), texts.map { it.text })
        }

    @Test
    fun a_field_reports_its_error_to_screen_readers() =
        runComposeUiTest {
            show { field(value = "name@", errorMessage = "Check the email") }
            onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Check the email"))
        }

    @Test
    fun screens_read_no_developer_names() =
        runComposeUiTest {
            show {
                Column {
                    ProcessingContent(step = null, order = null)
                    failedResult()
                }
            }
            listOf("ProcessingScreen", "ResultScreen", "FailureIcon").forEach {
                onAllNodesWithContentDescription(it).assertCountEquals(0)
            }
            onNodeWithTag("ProcessingScreen").assertExists()
            onNodeWithTag("ResultScreen").assertExists()
        }

    @Test
    fun the_result_is_announced_as_it_appears() =
        runComposeUiTest {
            show { failedResult() }
            onNode(isHeading()).assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive))
        }

    @Test
    fun the_processing_stage_is_announced_as_it_changes() =
        runComposeUiTest {
            show { ProcessingContent(step = ProcessingStep.LOADING_SESSION, order = null) }
            onNodeWithTag("processing_step", useUnmergedTree = true)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        }

    @Test
    fun a_picked_saved_card_reads_as_selected() =
        runComposeUiTest {
            show { MethodRow(title = "•••• 1234", leading = {}, selected = true, onClick = {}, role = Role.RadioButton) }
            onNode(hasClickAction()).assertIsSelected()
        }

    @Test
    fun the_skeleton_reads_as_loading_not_as_stand_in_cards() =
        runComposeUiTest {
            show { Column { PaymentMethodsSkeleton(SkeletonLayout(wallet = false, savedCards = 2), visible = true, onClose = {}) } }
            onAllNodes(hasText("0000", substring = true)).assertCountEquals(0)
            onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
        }

    @Test
    fun the_apple_pay_button_is_a_named_button_for_voiceover() =
        runComposeUiTest {
            var taps = 0
            // In the checkout the step keeps the button's slot and the sheet's layer draws the native view over it.
            show {
                CompositionLocalProvider(LocalWalletButtonLayer provides WalletButtonLayer()) {
                    ApplePayButton(onClick = { taps++ }, modifier = Modifier)
                }
            }
            val button = onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            val name = button.fetchSemanticsNode().config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
            assertTrue(name.joinToString().contains("Pay"), "no name: $name")
            button.performSemanticsAction(SemanticsActions.OnClick)
            assertTrue(taps == 1, "the screen reader's tap does not pay")
        }

    @Test
    fun a_new_step_is_announced_by_its_title() =
        runComposeUiTest {
            show { StepHeader(title = "New card", onBack = {}, onClose = {}) }
            onNode(isHeading()).assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "New card"))
        }

    @Composable
    private fun field(
        value: String,
        errorMessage: String? = null,
        placeholder: String = "name@example.com",
    ) = ComposeTextField(
        value = value,
        onValueChange = {},
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions.Default,
        contentType = TextFieldContentType.EMAIL,
        enabled = true,
        modifier = Modifier,
        onFocusChange = {},
        label = "Email",
        errorMessage = errorMessage,
    )

    @Composable
    private fun failedResult() =
        ResultContent(
            result = CheckoutResult.Failed("order", SdkException("declined")),
            onClose = {},
            onRetry = null,
            receipt = null,
            order = null,
            fillHeight = false,
        )
}
