package net.payrdr.mobile.payment.samples.android.nav

import net.payrdr.mobile.payment.samples.android.store.model.CoverId

/** All navigation routes as typed constants; use from NavHost and any `navigate(...)` call. */
object Routes {
    const val START = "start"
    const val DEV = "dev"
    const val CORE = "core"
    const val ADVANCED = "dev/advanced"
    const val HEADLESS = "dev/advanced/headless"
    const val DEV_RESULT = "dev/result"
    const val COVER = "store/{cover}"
    const val RESULT = "store/{cover}/result"

    /** Home screen of [cover]. */
    fun cover(cover: CoverId) = "store/${cover.name}"

    /** Result screen of [cover]. */
    fun result(cover: CoverId) = "store/${cover.name}/result"
}
