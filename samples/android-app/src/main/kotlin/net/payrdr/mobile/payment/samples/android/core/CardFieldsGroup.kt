package net.payrdr.mobile.payment.samples.android.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.theme.StoreFonts

/**
 * The card fields as one group, the way the SDK's own form shows them: the number across the top with the
 * network logo, expiry and CVC side by side, the name below, split by hairlines in one frame. The focused cell
 * is outlined in the accent colour, a cell with an error in red; the error itself goes under the group.
 */
@Composable
internal fun CardFieldsGroup(
    vm: SdkCoreViewModel,
    errors: Map<CoreField, String>,
    onEdit: (CoreField) -> Unit,
    colors: CoreColors,
) {
    var focused by remember { mutableStateOf<CoreField?>(null) }

    fun highlight(field: CoreField): Color? =
        when {
            errors[field] != null -> colors.error
            focused == field -> colors.accent
            else -> null
        }

    @Composable
    fun RowScope.cell(
        field: CoreField,
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        edges: CellEdges,
        keyboard: KeyboardOptions,
        trailing: (@Composable () -> Unit)? = null,
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .cellHighlight(highlight(field), edges)
                .padding(start = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // One font for the placeholder and the value, with even digits, so nothing jumps while typing.
            val style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 15.sp, fontFeatureSettings = "tnum", color = colors.text)
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, style = style.copy(color = colors.placeholder), maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = {
                        onValueChange(it)
                        onEdit(field)
                    },
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = keyboard,
                    visualTransformation =
                        when (field) {
                            CoreField.PAN -> PanGroups
                            CoreField.EXPIRY -> ExpirySlash
                            else -> VisualTransformation.None
                        },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    focused = field
                                } else if (focused == field) {
                                    focused = null
                                }
                            }.testTag(coreFieldTag(field)),
                )
            }
            trailing?.invoke()
        }
    }

    val digits = KeyboardOptions(keyboardType = KeyboardType.Number)
    Column {
        Text(
            stringResource(R.string.core_card_group),
            modifier = Modifier.padding(bottom = 6.dp),
            style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = colors.muted,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(GroupShape)
                .background(colors.surface)
                // The frame goes under the cells, so a highlighted cell can draw over it.
                .drawBehind { drawFrame(GroupShape, colors.border) },
        ) {
            val panHighlighted = highlight(CoreField.PAN) != null
            Row(Modifier.fillMaxWidth().height(CELL_HEIGHT).zIndex(if (panHighlighted) 1f else 0f)) {
                cell(
                    CoreField.PAN,
                    vm.pan,
                    { typed ->
                        val digits = typed.filter(Char::isDigit)
                        val pan = digits.take(CardNetwork.of(digits).maxPanLength)
                        vm.setPan(pan)
                        // A new network may take a shorter CVC (Amex has 4 digits, the others 3).
                        val cvcMax = CardNetwork.of(pan).cvcLength
                        if (vm.cvc.length > cvcMax) vm.setCvc(vm.cvc.take(cvcMax))
                    },
                    stringResource(R.string.core_pan),
                    CellEdges(top = true, start = true, end = true),
                    digits,
                ) {
                    val network = CardNetwork.of(vm.pan)
                    if (network != CardNetwork.UNKNOWN) {
                        Text(network.displayName, color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
            Hairline(colors, Modifier.fillMaxWidth().height(STROKE))
            val middleHighlighted = highlight(CoreField.EXPIRY) != null || highlight(CoreField.CVC) != null
            Row(Modifier.fillMaxWidth().height(CELL_HEIGHT).zIndex(if (middleHighlighted) 1f else 0f)) {
                cell(
                    CoreField.EXPIRY,
                    vm.expiry,
                    { vm.setExpiry(it.filter(Char::isDigit).take(4)) },
                    stringResource(R.string.core_expiry_placeholder),
                    CellEdges(start = true),
                    digits,
                )
                Hairline(colors, Modifier.width(STROKE).fillMaxHeight())
                cell(
                    CoreField.CVC,
                    vm.cvc,
                    { vm.setCvc(it.filter(Char::isDigit).take(CardNetwork.of(vm.pan).cvcLength)) },
                    stringResource(R.string.core_cvc),
                    CellEdges(end = true),
                    digits,
                )
            }
            Hairline(colors, Modifier.fillMaxWidth().height(STROKE))
            val holderHighlighted = highlight(CoreField.HOLDER) != null
            Row(Modifier.fillMaxWidth().height(CELL_HEIGHT).zIndex(if (holderHighlighted) 1f else 0f)) {
                cell(
                    CoreField.HOLDER,
                    vm.holder,
                    vm::setHolder,
                    stringResource(R.string.core_holder),
                    CellEdges(bottom = true, start = true, end = true),
                    KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
            }
        }
        val failed = listOf(CoreField.PAN, CoreField.EXPIRY, CoreField.CVC, CoreField.HOLDER).firstOrNull { errors[it] != null }
        if (failed != null) {
            Text(
                stringResource(errorText(errors.getValue(failed))),
                modifier = Modifier.padding(top = 6.dp).testTag(coreErrorTag(failed)),
                style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 12.sp),
                color = colors.error,
            )
        }
    }
}

/** Which sides of a cell lie on the group's outer frame; the other sides border a hairline. */
private data class CellEdges(
    val top: Boolean = false,
    val bottom: Boolean = false,
    val start: Boolean = false,
    val end: Boolean = false,
)

@Composable
private fun Hairline(
    colors: CoreColors,
    modifier: Modifier,
) {
    Box(modifier.background(colors.border))
}

/**
 * Outlines a cell in [color] over the hairlines on its inner sides and over the frame on its outer ones, with
 * the frame's rounded corners where the cell meets them.
 */
private fun Modifier.cellHighlight(
    color: Color?,
    edges: CellEdges,
): Modifier =
    if (color == null) {
        this
    } else {
        zIndex(1f).drawWithContent {
            drawContent()
            val w = STROKE.toPx()
            val left = if (edges.start) 0f else -w
            val up = if (edges.top) 0f else -w
            val right = size.width + if (edges.end) 0f else w
            val down = size.height + if (edges.bottom) 0f else w
            val square = CornerSize(0.dp)
            val shape =
                GroupShape.copy(
                    topStart = if (edges.top && edges.start) GroupShape.topStart else square,
                    topEnd = if (edges.top && edges.end) GroupShape.topEnd else square,
                    bottomStart = if (edges.bottom && edges.start) GroupShape.bottomStart else square,
                    bottomEnd = if (edges.bottom && edges.end) GroupShape.bottomEnd else square,
                )
            translate(left, up) { drawFrame(shape, color, Size(right - left, down - up)) }
        }
    }

/** Strokes [shape] inside [area] (the whole draw area by default), the way `Modifier.border` does. */
private fun DrawScope.drawFrame(
    shape: RoundedCornerShape,
    color: Color,
    area: Size = size,
) {
    val w = STROKE.toPx()
    val outline = shape.createOutline(Size(area.width - w, area.height - w), layoutDirection, this)
    translate(w / 2, w / 2) { drawOutline(outline, color, style = Stroke(w)) }
}

private val GroupShape = RoundedCornerShape(12.dp)
private val CELL_HEIGHT = 48.dp
private val STROKE = 1.5.dp

/** Shows the number's digits in the network's groups ("4111 1111 1111 1111", Amex 4-6-5). */
internal object PanGroups : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = CardNetwork.of(text.text).format(text.text)
        // Where each digit sits in the shown text; the offset after the last digit is the end.
        val positions = shown.indices.filter { shown[it].isDigit() }
        val mapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = if (offset < positions.size) positions[offset] else shown.length

                override fun transformedToOriginal(offset: Int) = positions.count { it < offset }
            }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}
