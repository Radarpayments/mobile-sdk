package net.payrdr.mobile.payment.samples.android.store.invest

import net.payrdr.mobile.payment.samples.android.merchant.ApiVersion
import net.payrdr.mobile.payment.samples.android.merchant.OrderType
import net.payrdr.mobile.payment.samples.android.store.model.CoverId

/**
 * The developer settings ask for AFT transfers: the start screen then shows Northwind Invest, where a transfer tops up
 * an account, instead of the stores, where it would look like buying goods. Transfers go through API V1 only.
 */
fun isAftDemo(
    apiVersion: ApiVersion,
    orderType: OrderType,
): Boolean = apiVersion == ApiVersion.V1 && orderType == OrderType.AFT

/** The businesses on the start screen. */
fun visibleCovers(aftDemo: Boolean): List<CoverId> =
    if (aftDemo) listOf(CoverId.INVEST) else listOf(CoverId.FURNITURE, CoverId.COFFEE, CoverId.TICKETS)

/**
 * Whether [cover] belongs to the mode: a business left open while the settings switched modes would register the other
 * kind of order (a purchase in Northwind, a transfer for a cart).
 */
fun coverFits(
    cover: CoverId,
    aftDemo: Boolean,
): Boolean = cover in visibleCovers(aftDemo)

/** The business to reopen at launch: the last one, unless the settings have since hidden it. */
fun reopenCover(
    last: CoverId?,
    aftDemo: Boolean,
): CoverId? = last?.takeIf { coverFits(it, aftDemo) }
