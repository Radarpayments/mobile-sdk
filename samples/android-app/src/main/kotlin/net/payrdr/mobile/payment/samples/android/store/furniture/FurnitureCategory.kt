package net.payrdr.mobile.payment.samples.android.store.furniture

import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.model.Catalog
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Product

/** Furniture categories behind the catalogue chips. */
enum class FurnitureCategory(
    val label: Int,
) {
    ALL(R.string.furniture_all),
    CHAIRS(R.string.furniture_chairs),
    SOFAS(R.string.furniture_sofas),
    LIGHT(R.string.furniture_light),
}

/** Products of [category]; [FurnitureCategory.ALL] keeps every product. */
fun furnitureFilter(category: FurnitureCategory): List<Product> =
    Catalog.products(CoverId.FURNITURE).filter { category == FurnitureCategory.ALL || categoryOf(it.id) == category }

private fun categoryOf(id: String) =
    when (id) {
        "velvet_sofa" -> FurnitureCategory.SOFAS
        "arc_lamp" -> FurnitureCategory.LIGHT
        else -> FurnitureCategory.CHAIRS
    }
