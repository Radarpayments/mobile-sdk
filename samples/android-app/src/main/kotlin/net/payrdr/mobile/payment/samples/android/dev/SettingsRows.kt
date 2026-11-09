package net.payrdr.mobile.payment.samples.android.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.ChevronRight
import compose.icons.tablericons.Eye
import compose.icons.tablericons.EyeOff
import kotlinx.coroutines.delay
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.StartColors
import net.payrdr.mobile.payment.samples.android.store.theme.StoreFonts
import net.payrdr.mobile.payment.samples.android.ui.mirrorInRtl
import net.payrdr.mobile.payment.samples.android.ui.toCaps

/** The settings page: grey like the iOS grouped form, which the four sample apps share. */
internal val SettingsBackground = Color(0xFFF2F2F7)
private val Error = Color(0xFFB91C1C)
private val Placeholder = Color(0xFF9CA3AF)
private val CardShape = RoundedCornerShape(14.dp)

/** How long "Copied" stays before the row reads Copy again. */
internal const val DONE_MS = 2_000L

/** A titled group of rows on a white card, with an optional explanation under it. */
@Composable
internal fun SettingsSection(
    title: String,
    footer: List<String> = emptyList(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(
        title.toCaps(),
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp).semantics { heading() },
        style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.6.sp),
        color = StartColors.muted,
    )
    Column(Modifier.fillMaxWidth().clip(CardShape).background(StartColors.surface), content = content)
    footer.forEach { Note(it, Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)) }
}

/** The hairline between two rows of a card, indented like the iOS form's. */
@Composable
internal fun RowDivider() = HorizontalDivider(Modifier.padding(start = 16.dp), color = StartColors.border)

@Composable
internal fun Note(
    text: String,
    modifier: Modifier = Modifier,
) = Text(text, modifier, style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 13.sp, lineHeight = 18.sp), color = StartColors.muted)

/**
 * A text field with its name always above the value. A password field masks the value and has a button to show it, since a
 * typo in a merchant password is otherwise impossible to spot.
 */
@Composable
internal fun FieldRow(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
) {
    val secret = keyboard == KeyboardType.Password
    var shown by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp, horizontal = 0.dp)) {
            Text(
                label,
                // The field below carries the label for screen readers; this caption would be read a second time.
                Modifier.clearAndSetSemantics {},
                style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
                color = StartColors.muted,
            )
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = StoreFonts.inter, fontSize = 16.sp, color = StartColors.text),
                cursorBrush = SolidColor(StartColors.text),
                visualTransformation = if (secret && !shown) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard, autoCorrectEnabled = false),
                modifier =
                    modifier.fillMaxWidth().padding(top = 2.dp, end = 12.dp).semantics {
                        contentDescription = label
                        if (error != null) error(error)
                    },
                decorationBox = { field ->
                    Box {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 16.sp), color = Placeholder)
                        }
                        field()
                    }
                },
            )
            if (error != null) {
                Text(error, Modifier.padding(top = 4.dp), style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 12.sp), color = Error)
            }
        }
        if (secret) {
            IconButton(onClick = { shown = !shown }) {
                Icon(
                    if (shown) TablerIcons.EyeOff else TablerIcons.Eye,
                    contentDescription = stringResource(if (shown) R.string.dev_hide_secret else R.string.dev_show_secret),
                    tint = StartColors.muted,
                )
            }
        }
    }
}

/** A choice between two values, as a segmented control next to its name. */
@Composable
internal fun <T> SegmentRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, Modifier.weight(1f), style = rowText, color = StartColors.text)
        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(StartColors.selected)
                .padding(2.dp)
                .selectableGroup(),
        ) {
            options.forEach { (value, label) ->
                val on = value == selected
                Box(
                    Modifier
                        .height(36.dp)
                        .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(8.dp)) else Modifier)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) StartColors.surface else Color.Transparent)
                        .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(value) })
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style =
                            TextStyle(
                                fontFamily = StoreFonts.inter,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                            ),
                        color = StartColors.text,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A choice among several values: the row shows the current one and opens the list in a bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> PickerRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button) { open = true }
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = rowText, color = StartColors.text)
        Text(options.firstOrNull { it.first == selected }?.second.orEmpty(), style = rowText, color = StartColors.muted)
        Icon(
            TablerIcons.ChevronRight,
            contentDescription = null,
            tint = Placeholder,
            modifier = Modifier.padding(start = 4.dp).size(20.dp).mirrorInRtl(),
        )
    }
    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = StartColors.surface,
        ) {
            Text(
                title,
                Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics { heading() },
                style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                color = StartColors.text,
            )
            Column(Modifier.selectableGroup().padding(bottom = 24.dp)) {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(selected = value == selected, role = Role.RadioButton) {
                                onSelect(value)
                                open = false
                            }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null, modifier = Modifier.padding(12.dp))
                        Text(label, style = rowText, color = StartColors.text)
                    }
                }
            }
        }
    }
}

/** A row that opens another screen. */
@Composable
internal fun LinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(title, style = rowText, color = StartColors.text)
            Text(subtitle, style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 13.sp), color = StartColors.muted)
        }
        Icon(TablerIcons.ChevronRight, contentDescription = null, tint = Placeholder, modifier = Modifier.size(20.dp).mirrorInRtl())
    }
}

/** The last registered order, monospaced, with Copy that turns into "Copied" for a moment. */
@Composable
internal fun LastOrderRow(
    mdOrder: String?,
    onCopy: (String) -> Unit,
) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(DONE_MS)
            copied = false
        }
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(stringResource(R.string.dev_last_order), style = rowText, color = StartColors.text)
            Text(
                mdOrder ?: stringResource(R.string.dev_none),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                color = StartColors.muted,
            )
        }
        if (mdOrder != null) {
            if (copied) {
                DoneMark(stringResource(R.string.dev_copied), Modifier.padding(horizontal = 8.dp))
            } else {
                Text(
                    stringResource(R.string.dev_copy),
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(role = Role.Button) {
                            onCopy(mdOrder)
                            copied = true
                        }.padding(horizontal = 12.dp, vertical = 14.dp),
                    style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.Medium, fontSize = 15.sp),
                    color = StartColors.link,
                )
            }
        }
    }
}

@Composable
private fun DoneMark(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Done)
        Spacer(Modifier.size(4.dp))
        Icon(TablerIcons.Check, contentDescription = null, tint = Done, modifier = Modifier.size(18.dp))
    }
}

private val Done = Color(0xFF15803D)
private val rowText = TextStyle(fontFamily = StoreFonts.inter, fontSize = 16.sp)
