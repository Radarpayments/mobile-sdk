package net.payrdr.mobile.payment.samples.android.store.invest

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** A fund in the demo portfolio; prices are fixed, [changeBasisPoints] is the month's change (410 = +4.1%). */
data class Holding(
    val ticker: String,
    val valueMinor: Long,
    val unitPriceMinor: Long,
    val changeBasisPoints: Int,
)

/** Units of the fund the holding is worth. */
fun Holding.quantity(): Double = valueMinor.toDouble() / unitPriceMinor

/** One line of the account history; [orderId] is the gateway order of a top-up. */
data class Operation(
    val kind: Kind,
    val amountMinor: Long,
    val atEpochMillis: Long,
    val orderId: String? = null,
    val ticker: String? = null,
) {
    /** Money in from a card, or into a fund. */
    enum class Kind { TOP_UP, BUY }
}

/**
 * The Northwind Invest demo account, kept on the device. Only [credit] brings money in, and only after the SDK reports
 * a completed transfer; [invest] moves it into a fund locally, without a payment.
 */
data class InvestAccount(
    val freeCashMinor: Long,
    val holdings: List<Holding>,
    val operations: List<Operation>,
) {
    val totalMinor: Long get() = freeCashMinor + holdings.sumOf { it.valueMinor }

    val canInvest: Boolean get() = freeCashMinor >= MIN_INVEST_MINOR

    /** Adds a completed top-up of order [orderId]; a second result of the same order changes nothing. */
    fun credit(
        orderId: String,
        amountMinor: Long,
        at: Long,
    ): InvestAccount {
        if (operations.any { it.kind == Operation.Kind.TOP_UP && it.orderId == orderId }) return this
        return copy(
            freeCashMinor = freeCashMinor + amountMinor,
            operations = listOf(Operation(Operation.Kind.TOP_UP, amountMinor, at, orderId = orderId)) + operations,
        )
    }

    /** Buys [ticker] for [amountMinor], at most the free cash; nothing when there is less than a euro to invest. */
    fun invest(
        amountMinor: Long,
        at: Long,
        ticker: String = GLOBAL_ETF,
    ): InvestAccount {
        if (!canInvest) return this
        val amount = amountMinor.coerceAtMost(freeCashMinor)
        if (amount <= 0) return this
        return copy(
            freeCashMinor = freeCashMinor - amount,
            holdings = holdings.map { if (it.ticker == ticker) it.copy(valueMinor = it.valueMinor + amount) else it },
            operations = listOf(Operation(Operation.Kind.BUY, amount, at, ticker = ticker)) + operations,
        )
    }

    /** The latest [limit] operations, newest first. */
    fun recent(limit: Int = 5): List<Operation> = operations.take(limit)

    /** The account as kept in the app settings. */
    fun toJson(): String =
        buildJsonObject {
            put("freeCashMinor", freeCashMinor)
            put(
                "holdings",
                buildJsonArray {
                    holdings.forEach { h ->
                        add(
                            buildJsonObject {
                                put("ticker", h.ticker)
                                put("valueMinor", h.valueMinor)
                                put("unitPriceMinor", h.unitPriceMinor)
                                put("changeBasisPoints", h.changeBasisPoints)
                            },
                        )
                    }
                },
            )
            put(
                "operations",
                buildJsonArray {
                    operations.forEach { op ->
                        add(
                            buildJsonObject {
                                put("kind", op.kind.name)
                                put("amountMinor", op.amountMinor)
                                put("atEpochMillis", op.atEpochMillis)
                                op.orderId?.let { put("orderId", it) }
                                op.ticker?.let { put("ticker", it) }
                            },
                        )
                    }
                },
            )
        }.toString()

    /** The funds and the account of a fresh install. */
    companion object {
        const val GLOBAL_ETF = "GLB"
        const val TECH_LEADERS = "TCH"
        private const val MIN_INVEST_MINOR = 100L

        // 12 Oct 2026, 10:15 and 10:20 UTC.
        private const val SEED_TOP_UP_AT = 1_791_800_100_000L
        private const val SEED_BUY_AT = 1_791_800_400_000L

        /** The account a fresh install starts with: €4,812.40 in total. */
        val SEED =
            InvestAccount(
                freeCashMinor = 1240,
                holdings =
                    listOf(
                        Holding(GLOBAL_ETF, valueMinor = 290000, unitPriceMinor = 23387, changeBasisPoints = 410),
                        Holding(TECH_LEADERS, valueMinor = 190000, unitPriceMinor = 37255, changeBasisPoints = 200),
                    ),
                operations =
                    listOf(
                        Operation(Operation.Kind.BUY, 25000, SEED_BUY_AT, ticker = GLOBAL_ETF),
                        Operation(Operation.Kind.TOP_UP, 25000, SEED_TOP_UP_AT, orderId = "seed"),
                    ),
            )

        /** The account saved with [toJson]; the seed when there is none or it cannot be read. */
        fun fromJson(json: String?): InvestAccount {
            if (json.isNullOrBlank()) return SEED
            return runCatching { parse(Json.parseToJsonElement(json).jsonObject) }.getOrDefault(SEED)
        }

        private fun parse(o: JsonObject): InvestAccount =
            InvestAccount(
                freeCashMinor = o.getValue("freeCashMinor").jsonPrimitive.long,
                holdings =
                    (o.getValue("holdings") as JsonArray).map {
                        val h = it.jsonObject
                        Holding(
                            ticker = h.getValue("ticker").jsonPrimitive.content,
                            valueMinor = h.getValue("valueMinor").jsonPrimitive.long,
                            unitPriceMinor = h.getValue("unitPriceMinor").jsonPrimitive.long,
                            changeBasisPoints = h.getValue("changeBasisPoints").jsonPrimitive.int,
                        )
                    },
                operations =
                    o.getValue("operations").jsonArray.map {
                        val op = it.jsonObject
                        Operation(
                            kind = Operation.Kind.valueOf(op.getValue("kind").jsonPrimitive.content),
                            amountMinor = op.getValue("amountMinor").jsonPrimitive.long,
                            atEpochMillis = op.getValue("atEpochMillis").jsonPrimitive.long,
                            orderId = op["orderId"]?.jsonPrimitive?.content,
                            ticker = op["ticker"]?.jsonPrimitive?.content,
                        )
                    },
            )
    }
}
