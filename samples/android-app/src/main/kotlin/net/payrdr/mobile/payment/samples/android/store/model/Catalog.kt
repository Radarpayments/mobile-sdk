package net.payrdr.mobile.payment.samples.android.store.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.payrdr.mobile.payment.samples.android.R

/** The demo businesses. Every cover runs the same payment path and scenarios. */
enum class CoverId { FURNITURE, COFFEE, TICKETS, INVEST }

/** A thing the buyer can put in the cart; [priceMinor] is in euro cents. */
data class Product(
    val id: String,
    @param:StringRes val name: Int,
    @param:StringRes val subtitle: Int?,
    val priceMinor: Long,
    @param:DrawableRes val image: Int?,
)

/** What each demo business sells. Prices are euro cents. */
object Catalog {
    /** Products of [cover], in display order. */
    fun products(cover: CoverId): List<Product> =
        when (cover) {
            CoverId.FURNITURE -> furniture
            CoverId.COFFEE -> coffee
            CoverId.TICKETS -> tickets
            // Northwind Invest sells nothing: the buyer tops up an account.
            CoverId.INVEST -> emptyList()
        }

    private val furniture =
        listOf(
            Product("velvet_sofa", R.string.product_velvet_sofa, R.string.product_velvet_sofa_sub, 124900, R.drawable.furniture_sofa),
            Product("cloud_chair", R.string.product_cloud_chair, R.string.product_cloud_chair_sub, 32900, R.drawable.furniture_cloud_chair),
            Product("ono_chair", R.string.product_ono_chair, R.string.product_ono_chair_sub, 18900, R.drawable.furniture_ono_chair),
            Product("birch_stool", R.string.product_birch_stool, R.string.product_birch_stool_sub, 9900, R.drawable.furniture_stool),
            Product("arc_lamp", R.string.product_arc_lamp, R.string.product_arc_lamp_sub, 21900, R.drawable.furniture_lamp),
            Product(
                "lume_armchair",
                R.string.product_lume_armchair,
                R.string.product_lume_armchair_sub,
                45900,
                R.drawable.furniture_hero,
            ),
        )

    private val coffee =
        listOf(
            Product("flat_white", R.string.product_flat_white, R.string.product_flat_white_sub, 390, R.drawable.coffee_flat_white),
            Product("cappuccino", R.string.product_cappuccino, R.string.product_cappuccino_sub, 360, R.drawable.coffee_cappuccino),
            Product("espresso", R.string.product_espresso, R.string.product_espresso_sub, 260, R.drawable.coffee_hero),
            Product("iced_latte", R.string.product_iced_latte, R.string.product_iced_latte_sub, 420, R.drawable.coffee_hero),
            Product("croissant", R.string.product_croissant, R.string.product_croissant_sub, 350, R.drawable.coffee_croissant),
        )

    private val tickets =
        listOf(
            Product("floor", R.string.ticket_floor, R.string.ticket_floor_sub, 4500, null),
            Product("balcony", R.string.ticket_balcony, R.string.ticket_balcony_sub, 6500, null),
            Product(TICKET_FEE_ID, R.string.ticket_fee, null, 350, null),
        )
}

/** Id of the per-order service fee line in [CoverId.TICKETS]. */
const val TICKET_FEE_ID = "fee"
