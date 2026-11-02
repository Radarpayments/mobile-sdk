package net.payrdr.mobile.payment.samples.android.core

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.ChevronLeft
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.prefs.rememberPrefs
import net.payrdr.mobile.payment.samples.android.store.theme.StoreFonts
import net.payrdr.mobile.payment.samples.android.store.theme.SystemBarIcons
import net.payrdr.mobile.payment.sdk.core.validation.ValidationCodes

/** Test tag of the "Get token" button. */
const val CORE_GENERATE_TAG = "core_generate"

const val CORE_TOKEN_TAG = "core_token"

fun coreFieldTag(field: CoreField) = "core_field_${field.name}"

fun coreErrorTag(field: CoreField) = "core_error_${field.name}"

/**
 * SDK Core: a card form of the merchant's own, encrypted on the device into the token the merchant's
 * server sends to the gateway. Card data stay in memory; mdOrder, key, bindingId and the variant are
 * remembered between launches.
 */
@Composable
fun SdkCoreScreen(onBack: () -> Unit) {
    val (prefsState, prefs) = rememberPrefs()
    val snapshot = prefsState.value ?: return
    val scope = rememberCoroutineScope()
    val vm = remember { SdkCoreViewModel() }
    LaunchedEffect(Unit) {
        vm.setVariant(SdkCoreVariant.fromStorageKey(snapshot.sdkCoreVariant))
        vm.setMdOrder(snapshot.mdOrder)
        vm.setPubKeyPem(snapshot.pubKeyPem)
        vm.setBindingId(snapshot.bindingId)
    }
    var token by remember { mutableStateOf<String?>(null) }
    var errors by remember { mutableStateOf<Map<CoreField, String>>(emptyMap()) }
    val colors = coreColors()
    SystemBarIcons(lightBackground = !colors.dark)
    BackHandler(enabled = token != null) { token = null }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
    ) {
        Header(
            subtitle = if (token == null) null else stringResource(R.string.core_done_subtitle),
            onBack = { if (token != null) token = null else onBack() },
            colors = colors,
        )
        val ready = token
        if (ready != null) {
            TokenPanel(token = ready, onAgain = { token = null }, colors = colors)
        } else {
            CoreForm(
                vm = vm,
                errors = errors,
                onEdit = { field -> errors = errors - field },
                onVariant = { variant ->
                    vm.setVariant(variant)
                    errors = emptyMap()
                    scope.launch { prefs.setSdkCoreVariant(variant.storageKey) }
                },
                onRemembered = { field, value ->
                    scope.launch {
                        when (field) {
                            CoreField.MD_ORDER -> prefs.setMdOrder(value)
                            CoreField.PUB_KEY -> prefs.setPubKeyPem(value)
                            CoreField.BINDING_ID -> prefs.setBindingId(value)
                            else -> Unit
                        }
                    }
                },
                onGenerate = {
                    val result = vm.generate()
                    errors = result.errors.mapNotNull { (field, code) -> field.toCoreField()?.let { it to code } }.toMap()
                    // An error no field explains (e.g. an unusable key) still needs a place: the key field.
                    if (!result.isSuccessful && errors.isEmpty()) errors = mapOf(CoreField.PUB_KEY to ValidationCodes.invalid)
                    if (result.isSuccessful) token = result.token
                },
                colors = colors,
            )
        }
    }
}

@Composable
private fun Header(
    subtitle: String?,
    onBack: () -> Unit,
    colors: CoreColors,
) {
    Row(Modifier.padding(top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
            Icon(TablerIcons.ChevronLeft, contentDescription = stringResource(R.string.store_back), tint = colors.text)
        }
        Column {
            Text(
                stringResource(R.string.core_title),
                style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                color = colors.text,
            )
            if (subtitle != null) Text(subtitle, style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 13.sp), color = colors.muted)
        }
    }
}

@Composable
private fun CoreForm(
    vm: SdkCoreViewModel,
    errors: Map<CoreField, String>,
    onEdit: (CoreField) -> Unit,
    onVariant: (SdkCoreVariant) -> Unit,
    onRemembered: (CoreField, String) -> Unit,
    onGenerate: () -> Unit,
    colors: CoreColors,
) {
    val variant = vm.variant
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 24.dp)) {
        VariantSwitch(variant, onVariant, colors)
        Text(
            stringResource(
                when (variant) {
                    SdkCoreVariant.NEW_CARD -> R.string.core_hint_new
                    SdkCoreVariant.INSTANT -> R.string.core_hint_instant
                    SdkCoreVariant.STORED -> R.string.core_hint_stored
                },
            ),
            style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 13.sp, lineHeight = 18.sp),
            color = colors.muted,
        )
        val field: @Composable (CoreField, Int, String, (String) -> Unit, FieldKind) -> Unit = { f, label, value, set, kind ->
            CoreInput(
                field = f,
                label = stringResource(label),
                value = value,
                onValueChange = {
                    set(it)
                    onEdit(f)
                    if (f in REMEMBERED) onRemembered(f, it)
                },
                error = errors[f],
                kind = kind,
                colors = colors,
            )
        }
        if (variant == SdkCoreVariant.STORED) {
            field(CoreField.BINDING_ID, R.string.core_binding, vm.bindingId, vm::setBindingId, FieldKind.Pasted(null))
            field(CoreField.CVC, R.string.core_cvc_optional, vm.cvc, { vm.setCvc(it.filter(Char::isDigit).take(4)) }, FieldKind.Digits)
        } else {
            CardFieldsGroup(vm = vm, errors = errors, onEdit = onEdit, colors = colors)
        }
        if (variant != SdkCoreVariant.INSTANT) {
            field(CoreField.MD_ORDER, R.string.core_md_order, vm.mdOrder, vm::setMdOrder, FieldKind.Pasted(R.string.core_md_order_hint))
        }
        field(CoreField.PUB_KEY, R.string.core_pub_key, vm.pubKeyPem, vm::setPubKeyPem, FieldKind.Pasted(null, singleLine = false))
        Box(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.button)
                .clickable(role = Role.Button, onClick = onGenerate)
                .testTag(CORE_GENERATE_TAG),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.core_generate),
                style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = colors.onButton,
            )
        }
    }
}

@Composable
private fun VariantSwitch(
    selected: SdkCoreVariant,
    onSelect: (SdkCoreVariant) -> Unit,
    colors: CoreColors,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.track)
            .padding(3.dp),
    ) {
        listOf(
            SdkCoreVariant.NEW_CARD to R.string.core_variant_new,
            SdkCoreVariant.INSTANT to R.string.core_variant_instant,
            SdkCoreVariant.STORED to R.string.core_variant_stored,
        ).forEach { (variant, label) ->
            val on = variant == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) colors.surface else Color.Transparent)
                    .clickable(interactionSource = null, indication = null, role = Role.Tab) { onSelect(variant) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    style =
                        TextStyle(
                            fontFamily = StoreFonts.inter,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                    color = if (on) colors.text else colors.muted,
                )
            }
        }
    }
}

/** How an input reads and is filled. */
private sealed interface FieldKind {
    data object Text : FieldKind

    data object Digits : FieldKind

    /** Copied from elsewhere: monospace, with a "Paste" action and an optional [hint]. */
    data class Pasted(
        @StringRes val hint: Int?,
        val singleLine: Boolean = true,
    ) : FieldKind
}

@Composable
private fun CoreInput(
    field: CoreField,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    kind: FieldKind,
    colors: CoreColors,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val mono = kind is FieldKind.Pasted || kind is FieldKind.Digits
    val singleLine = (kind as? FieldKind.Pasted)?.singleLine ?: true
    Column {
        Text(
            label,
            modifier = Modifier.padding(bottom = 6.dp),
            style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = colors.muted,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, if (error != null) colors.error else colors.border, RoundedCornerShape(12.dp))
                .padding(start = 14.dp, end = 4.dp),
            // In a tall field "Paste" stays by the first line.
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        ) {
            val textStyle =
                TextStyle(
                    fontFamily = if (mono) FontFamily.Monospace else StoreFonts.inter,
                    fontSize = if (singleLine) 15.sp else 12.sp,
                    lineHeight = if (singleLine) 20.sp else 16.sp,
                    color = colors.text,
                )
            Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                val hint = (kind as? FieldKind.Pasted)?.hint
                if (value.isEmpty() && hint != null) {
                    Text(stringResource(hint), style = textStyle.copy(fontFamily = StoreFonts.inter, color = colors.placeholder))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    // A whole 2048-bit PEM key: its 64-character lines wrap in two.
                    minLines = if (singleLine) 1 else PASTED_MIN_LINES,
                    maxLines = if (singleLine) 1 else PASTED_MAX_LINES,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = if (kind is FieldKind.Digits) KeyboardType.Number else KeyboardType.Text,
                        ),
                    // The expiry keeps digits only; the slash is drawn, so the cursor never lands on the wrong side of it.
                    visualTransformation = if (field == CoreField.EXPIRY) ExpirySlash else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth().testTag(coreFieldTag(field)),
                )
            }
            if (kind is FieldKind.Pasted) {
                Text(
                    stringResource(R.string.core_paste),
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) {
                                scope.launch {
                                    val pasted =
                                        clipboard
                                            .getClipEntry()
                                            ?.clipData
                                            ?.takeIf { it.itemCount > 0 }
                                            ?.getItemAt(0)
                                            ?.text
                                    if (pasted != null) onValueChange(pasted.toString().trim())
                                }
                            }.padding(horizontal = 10.dp, vertical = 12.dp),
                    style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = colors.accent,
                )
            }
        }
        if (error != null) {
            Text(
                stringResource(errorText(error)),
                modifier = Modifier.padding(top = 4.dp).testTag(coreErrorTag(field)),
                style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 12.sp),
                color = colors.error,
            )
        }
    }
}

private const val PASTED_MIN_LINES = 4
private const val PASTED_MAX_LINES = 18

/** sdk-core reports stable codes; a merchant maps them to its own words, as here. */
internal fun errorText(code: String): Int =
    when (code) {
        ValidationCodes.required -> R.string.core_error_required
        ValidationCodes.invalidFormat -> R.string.core_error_format
        else -> R.string.core_error_invalid
    }

@Composable
private fun TokenPanel(
    token: String,
    onAgain: () -> Unit,
    colors: CoreColors,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember(token) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 24.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.successSoft)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(TablerIcons.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(14.dp))
                Text(
                    stringResource(R.string.core_token_ok),
                    modifier = Modifier.padding(start = 4.dp),
                    style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = colors.success,
                )
            }
            SelectionContainer {
                Text(
                    token,
                    modifier =
                        Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.track)
                            .padding(12.dp)
                            .testTag(CORE_TOKEN_TAG),
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 16.sp),
                    color = colors.text,
                )
            }
            Text(
                stringResource(if (copied) R.string.core_copied else R.string.core_copy),
                modifier =
                    Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("seToken", token)))
                                copied = true
                            }
                        }.padding(horizontal = 8.dp, vertical = 10.dp),
                style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = colors.accent,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(R.string.core_step_send, R.string.core_step_gateway, R.string.core_step_pay).forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(colors.stepSoft), contentAlignment = Alignment.Center) {
                        Text(
                            "${index + 1}",
                            style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = colors.step,
                        )
                    }
                    Text(
                        stringResource(step),
                        modifier = Modifier.padding(start = 10.dp),
                        style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 14.sp, lineHeight = 19.sp),
                        color = colors.text,
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.track)
                .clickable(role = Role.Button, onClick = onAgain),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.core_again),
                style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = colors.text,
            )
        }
    }
}

/** Fields remembered between launches: never card data. */
private val REMEMBERED = setOf(CoreField.MD_ORDER, CoreField.PUB_KEY, CoreField.BINDING_ID)

/** Colours of the SDK Core screen; it follows the phone's light or dark theme. */
@Immutable
internal data class CoreColors(
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val track: Color,
    val text: Color,
    val muted: Color,
    val placeholder: Color,
    val border: Color,
    val accent: Color,
    val button: Color,
    val onButton: Color,
    val error: Color,
    val success: Color,
    val successSoft: Color,
    val step: Color,
    val stepSoft: Color,
)

@Composable
internal fun coreColors(): CoreColors =
    if (isSystemInDarkTheme()) {
        CoreColors(
            dark = true,
            background = Color(0xFF0B1120),
            surface = Color(0xFF111827),
            track = Color(0xFF1F2937),
            text = Color(0xFFF1F5F9),
            muted = Color(0xFF94A3B8),
            placeholder = Color(0xFF64748B),
            border = Color(0xFF1F2937),
            accent = Color(0xFF60A5FA),
            button = Color(0xFFF1F5F9),
            onButton = Color(0xFF0F172A),
            error = Color(0xFFF87171),
            success = Color(0xFF4ADE80),
            successSoft = Color(0xFF052E16),
            step = Color(0xFFC7D2FE),
            stepSoft = Color(0xFF312E81),
        )
    } else {
        CoreColors(
            dark = false,
            background = Color(0xFFF8FAFC),
            surface = Color(0xFFFFFFFF),
            track = Color(0xFFEEF2F6),
            text = Color(0xFF0F172A),
            muted = Color(0xFF475569),
            placeholder = Color(0xFF94A3B8),
            border = Color(0xFFE2E8F0),
            accent = Color(0xFF2563EB),
            button = Color(0xFF0F172A),
            onButton = Color(0xFFFFFFFF),
            error = Color(0xFFDC2626),
            success = Color(0xFF166534),
            successSoft = Color(0xFFDCFCE7),
            step = Color(0xFF3730A3),
            stepSoft = Color(0xFFE0E7FF),
        )
    }

/** Shows `MMYY` digits as `MM/YY`. */
internal object ExpirySlash : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = formatExpiryInput(text.text)
        val mapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = if (offset > 2 && shown.length > 2) offset + 1 else offset

                override fun transformedToOriginal(offset: Int) = if (offset > 2) offset - 1 else offset
            }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}
