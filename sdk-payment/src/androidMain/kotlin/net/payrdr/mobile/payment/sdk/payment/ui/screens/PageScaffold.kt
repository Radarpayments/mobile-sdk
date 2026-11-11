package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalOverlayCover
import net.payrdr.mobile.payment.sdk.payment.ui.components.OverlayCover
import net.payrdr.mobile.payment.sdk.payment.ui.components.SIDE_PADDING
import net.payrdr.mobile.payment.sdk.payment.ui.components.hiddenWhileCovered
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberReduceMotion
import net.payrdr.mobile.payment.sdk.payment.ui.motion.LocalStepBodyModifier
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors

/**
 * Full-screen step layout: [topBar], scrolling [body] with 20dp gutters, and an optional [footer]
 * pinned to the bottom behind a hairline. While the keyboard is open the footer is hidden so the
 * fields keep the room above the keys — unless [footerOverKeyboard]: then it rides on the keys, for a step
 * whose one field is answered by the footer's button (the CVV of a saved card and Pay). [overlay] draws on top
 * (e.g. a confirmation sheet).
 */
@Composable
internal fun PageScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    footerOverKeyboard: Boolean = false,
    body: @Composable ColumnScope.() -> Unit,
) {
    val sheet = LocalSheetPageHost.current
    if (sheet != null) {
        SheetPage(sheet, modifier, topBar, footer, overlay, footerOverKeyboard, body)
        return
    }
    val colors = LocalPaymentColors.current
    val bottomInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
    val keyboardOpen = rememberKeyboardOpen()
    val showFooter = footer != null && (footerOverKeyboard || !keyboardOpen)
    // On the keys: the keyboard inset lifts the footer, the gap under it is the plain minimum.
    val onKeyboard = footerOverKeyboard && keyboardOpen
    val footerBottom = if (onKeyboard) FOOTER_MIN_BOTTOM else rememberFooterBottomPadding()
    val motion = !rememberReduceMotion()
    val bodyScroll = rememberScrollState()
    val cover = remember { OverlayCover() }
    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .hiddenWhileCovered(cover)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            topBar()
            Column(
                modifier =
                    LocalStepBodyModifier.current
                        .weight(1f)
                        .fillMaxWidth()
                        .then(if (showFooter) Modifier else Modifier.windowInsetsPadding(bottomInsets))
                        .verticalScroll(bodyScroll)
                        .padding(start = SIDE_PADDING, end = SIDE_PADDING, top = 6.dp, bottom = if (showFooter) FOOTER_BODY_GAP else 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = body,
            )
            if (footer != null) {
                // Slides down out of the way when the keyboard opens and rises back when it closes.
                AnimatedVisibility(
                    visible = showFooter,
                    enter = if (motion) footerEnter else EnterTransition.None,
                    exit = if (motion) footerExit else ExitTransition.None,
                ) {
                    Column {
                        FooterDivider(bodyScroll)
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (onKeyboard) {
                                            Modifier.windowInsetsPadding(
                                                WindowInsets.ime.only(WindowInsetsSides.Bottom),
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ).padding(start = SIDE_PADDING, end = SIDE_PADDING, top = 12.dp, bottom = footerBottom),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            content = footer,
                        )
                    }
                }
            }
        }
        CompositionLocalProvider(LocalOverlayCover provides cover) { overlay() }
    }
}

/**
 * Where the checkout draws a step inside its bottom sheet instead of as a full-screen page. The sheet sizes
 * itself to the step, so the page wraps its content; [overlay] is drawn by the checkout over the whole
 * screen, not inside the sheet.
 */
internal class SheetPageHost {
    var overlay: (@Composable BoxScope.() -> Unit)? by mutableStateOf(null)

    /** Covers the sheet while one of [overlay]'s sheets is open. */
    val cover = OverlayCover()
}

/** Set by the checkout while it draws steps inside its sheet; null for full-screen pages. */
internal val LocalSheetPageHost = staticCompositionLocalOf<SheetPageHost?> { null }

/**
 * [PageScaffold] inside the checkout sheet: no background or system-bar insets of its own (the sheet has
 * them), the body scrolls once the sheet reaches its full height, the footer stays under it.
 */
@Composable
private fun SheetPage(
    host: SheetPageHost,
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)?,
    overlay: @Composable BoxScope.() -> Unit,
    footerOverKeyboard: Boolean,
    body: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalPaymentColors.current
    // The sheet itself sits on the keys, so a footer kept over the keyboard needs nothing more.
    val showFooter = footer != null && (footerOverKeyboard || !rememberKeyboardOpen())
    val motion = !rememberReduceMotion()
    val bodyScroll = rememberScrollState()
    val latestOverlay by rememberUpdatedState(overlay)
    DisposableEffect(host) {
        val slot: @Composable BoxScope.() -> Unit = { latestOverlay() }
        host.overlay = slot
        onDispose { if (host.overlay === slot) host.overlay = null }
    }
    // Clear: the sheet's surface is the background, and a zooming step shows the one it replaces.
    val stepBody = LocalStepBodyModifier.current
    Column(modifier = modifier.fillMaxWidth()) {
        topBar()
        Column(
            modifier =
                stepBody
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(bodyScroll)
                    .padding(start = SIDE_PADDING, end = SIDE_PADDING, top = 6.dp, bottom = if (showFooter) FOOTER_BODY_GAP else 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = body,
        )
        if (footer != null) {
            AnimatedVisibility(
                visible = showFooter,
                enter = if (motion) footerEnter else EnterTransition.None,
                exit = if (motion) sheetFooterExit else ExitTransition.None,
            ) {
                Column(stepBody) {
                    FooterDivider(bodyScroll)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = SIDE_PADDING, end = SIDE_PADDING, top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = footer,
                    )
                }
            }
        }
    }
}

/**
 * Hairline over the footer only while the body runs on under it, as in the system's sheets: with everything in view the
 * button stands on the background, like on the steps without fields. Faded rather than removed, so nothing moves.
 */
@Composable
private fun FooterDivider(bodyScroll: ScrollState) {
    val shown by animateFloatAsState(if (bodyScroll.canScrollForward) 1f else 0f, tween(FOOTER_DIVIDER_MS), label = "footer-divider")
    HorizontalDivider(color = LocalPaymentColors.current.outline, modifier = Modifier.alpha(shown))
}

private const val FOOTER_DIVIDER_MS = 150

/** Under the body, over the footer's own 12dp: 16dp to the button in all, the spacing between the form's groups. */
private val FOOTER_BODY_GAP = 4.dp

/**
 * Space under the footer. The system inset (navigation or gesture bar) already keeps it off the
 * screen edge, so the footer's own 16dp is only a minimum, not added on top. [systemBarFooterGap]
 * adds air where the system bar is opaque. While any keyboard inset is present the bottom inset is
 * the keyboard, so the value from before it opened is kept for the footer's exit and return.
 */
@Composable
private fun rememberFooterBottomPadding(): Dp {
    val density = LocalDensity.current
    val inset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
    val keyboardInset = WindowInsets.ime.getBottom(density) > 0
    val current = if (inset > 0.dp) inset + systemBarFooterGap else FOOTER_MIN_BOTTOM
    var kept by remember { mutableStateOf(current) }
    if (!keyboardInset && kept != current) kept = current
    return if (keyboardInset) kept else current
}

/** A keyboard's usual curve and duration (the iOS values), so the footer leaves at the keyboard's pace. */
private val KeyboardEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private const val KEYBOARD_MS = 250

// In the sheet the footer gives its height back as the keyboard rises, at the keyboard's pace: kept to the end
// and dropped then, it lifted the sheet over the keys and let it sink back.
private val sheetFooterExit: ExitTransition =
    fadeOut(tween(FOOTER_EXIT_MS)) +
        shrinkVertically(tween(KEYBOARD_MS, easing = KeyboardEasing), shrinkTowards = Alignment.Top)

// The footer keeps its full height while animating (no expand/shrink): the area under the fields is
// empty at that moment anyway, and clipping a growing box would hide the button until the last frames.
private val footerEnter: EnterTransition =
    slideInVertically(tween(FOOTER_ANIM_MS, easing = LinearOutSlowInEasing)) { it } +
        fadeIn(tween(FOOTER_ANIM_MS))

private val footerExit: ExitTransition =
    slideOutVertically(tween(FOOTER_EXIT_MS, easing = FastOutLinearInEasing)) { it } +
        fadeOut(tween(FOOTER_EXIT_MS))

private val FOOTER_MIN_BOTTOM = 16.dp
private const val FOOTER_ANIM_MS = 260
private const val FOOTER_EXIT_MS = 180
