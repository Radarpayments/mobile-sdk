package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Line icons of the checkout (24×24 grid, 1.8 stroke, round caps). Drawn black; callers tint them
 * through `Icon(tint = …)`.
 */
internal object PaymentIcons {
    val Lock: ImageVector by lazy {
        icon(
            "Lock",
            "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z",
            "M8 11V8a4 4 0 0 1 8 0v3",
        )
    }

    // On the Pay button: the heavier stroke matches the semibold label next to it.
    val LockBold: ImageVector by lazy {
        icon(
            "LockBold",
            "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z",
            "M8 11V8a4 4 0 0 1 8 0v3",
            stroke = 2.4f,
        )
    }

    // Point the other way in right-to-left layouts.
    val Back: ImageVector by lazy { icon("Back", "M15 18l-6-6 6-6", mirrored = true) }
    val Close: ImageVector by lazy { icon("Close", "M6 6l12 12M18 6L6 18") }
    val Chevron: ImageVector by lazy { icon("Chevron", "M9 6l6 6-6 6", mirrored = true) }
    val ChevronDown: ImageVector by lazy { icon("ChevronDown", "M6 9l6 6 6-6") }
    val Search: ImageVector by lazy { icon("Search", "M10.5 4a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0-13", "M15.5 15.5L20 20") }
    val Plus: ImageVector by lazy { icon("Plus", "M12 5v14M5 12h14") }
    val Scan: ImageVector by lazy {
        icon(
            "Scan",
            "M4 8V6a2 2 0 0 1 2-2h2M16 4h2a2 2 0 0 1 2 2v2M20 16v2a2 2 0 0 1-2 2h-2M8 20H6a2 2 0 0 1-2-2v-2",
            "M8 9h8a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1v-4a1 1 0 0 1 1-1z",
        )
    }
    val Check: ImageVector by lazy { icon("Check", "M5 12.5l4.5 4.5L19 7.5", stroke = 2.4f) }
    val Cross: ImageVector by lazy { icon("Cross", "M6 6l12 12M18 6L6 18", stroke = 2.4f) }
    val Shield: ImageVector by lazy {
        icon("Shield", "M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6z", "M9 12l2 2 4-4")
    }
    val Info: ImageVector by lazy { icon("Info", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18", "M12 11v5", "M12 7.75v.01") }
    val Trash: ImageVector by lazy { icon("Trash", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3") }
    val Mail: ImageVector by lazy {
        icon(
            "Mail",
            "M4.5 5.5h15A1.5 1.5 0 0 1 21 7v10a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17V7a1.5 1.5 0 0 1 1.5-1.5z",
            "M3.6 6.6l7.5 5.6a1.5 1.5 0 0 0 1.8 0l7.5-5.6",
        )
    }
    val Phone: ImageVector by lazy {
        icon(
            "Phone",
            "M8.5 2.75h7a1.75 1.75 0 0 1 1.75 1.75v15a1.75 1.75 0 0 1-1.75 1.75h-7a1.75 1.75 0 0 1-1.75-1.75v-15A1.75 1.75 0 0 1 8.5 2.75z",
            "M11 18h2",
        )
    }
    val Pin: ImageVector by lazy {
        icon(
            "Pin",
            "M12 21.25s-6.75-5.7-6.75-11.25a6.75 6.75 0 0 1 13.5 0c0 5.55-6.75 11.25-6.75 11.25z",
            "M12 7.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5",
        )
    }

    private fun icon(
        name: String,
        vararg paths: String,
        stroke: Float = 1.8f,
        mirrored: Boolean = false,
    ): ImageVector {
        val builder =
            ImageVector.Builder(
                name = name,
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
                autoMirror = mirrored,
            )
        paths.forEach { d ->
            builder.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
