package net.payrdr.mobile.payment.samples.android.store.invest

import net.payrdr.mobile.payment.samples.android.R

/** Periods of the portfolio chart; each shows its own fixed curve (the demo has no price history). */
enum class ChartPeriod(
    val label: Int,
    val points: List<Float>,
) {
    WEEK(R.string.invest_period_1w, listOf(0.55f, 0.5f, 0.58f, 0.52f, 0.6f, 0.57f, 0.66f)),
    MONTH(R.string.invest_period_1m, listOf(0.18f, 0.24f, 0.21f, 0.35f, 0.31f, 0.44f, 0.41f, 0.57f, 0.52f, 0.68f, 0.64f, 0.8f, 0.85f)),
    QUARTER(R.string.invest_period_3m, listOf(0.3f, 0.22f, 0.35f, 0.28f, 0.45f, 0.4f, 0.52f, 0.6f, 0.55f, 0.72f)),
    YEAR(R.string.invest_period_1y, listOf(0.1f, 0.2f, 0.16f, 0.3f, 0.42f, 0.38f, 0.5f, 0.47f, 0.62f, 0.7f, 0.66f, 0.82f)),
    ALL(R.string.invest_period_all, listOf(0.05f, 0.12f, 0.1f, 0.22f, 0.3f, 0.28f, 0.45f, 0.55f, 0.5f, 0.7f, 0.78f, 0.9f)),
}
