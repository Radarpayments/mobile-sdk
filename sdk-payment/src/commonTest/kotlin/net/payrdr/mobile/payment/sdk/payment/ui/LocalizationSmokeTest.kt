package net.payrdr.mobile.payment.sdk.payment.ui

import io.kotest.matchers.nulls.shouldNotBeNull
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.action_cancel
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_cvv_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_expiry_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_holder_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_pan_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_pay
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_save_card_only
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_add_new
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_delete
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_3ds_failed
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_3ds_timeout
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_cancelled
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_generic
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_invalid_card
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_network
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.pay_with_apple_pay
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.pay_with_google_pay
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_finalizing
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_loading_public_key
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_loading_session
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_submitting_payment
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_tokenizing_card
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_card_saved_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_close
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_failure_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_success_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_try_again
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.threeds_cancel
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.threeds_title
import kotlin.test.Test

/**
 * Smoke test: every string resource referenced by Plan-3 UI is addressable via the generated
 * `Res.string.*` accessor. If a resource is renamed or dropped, Kotlin compilation of this file
 * fails — catching the regression at compile time rather than at first user interaction.
 */
class LocalizationSmokeTest {
    @Test
    fun `all declared string resources are accessible`() {
        listOf(
            Res.string.card_form_title,
            Res.string.card_form_pan_label,
            Res.string.card_form_expiry_label,
            Res.string.card_form_cvv_label,
            Res.string.card_form_holder_label,
            Res.string.card_form_pay,
            Res.string.card_form_save_card_only,
            Res.string.card_list_title,
            Res.string.card_list_add_new,
            Res.string.card_list_delete,
            Res.string.pay_with_google_pay,
            Res.string.pay_with_apple_pay,
            Res.string.threeds_title,
            Res.string.threeds_cancel,
            Res.string.processing_title,
            Res.string.processing_loading_session,
            Res.string.processing_loading_public_key,
            Res.string.processing_tokenizing_card,
            Res.string.processing_submitting_payment,
            Res.string.processing_finalizing,
            Res.string.result_success_title,
            Res.string.result_card_saved_title,
            Res.string.result_failure_title,
            Res.string.result_try_again,
            Res.string.result_close,
            Res.string.action_cancel,
            Res.string.error_network,
            Res.string.error_generic,
            Res.string.error_invalid_card,
            Res.string.error_3ds_failed,
            Res.string.error_3ds_timeout,
            Res.string.error_cancelled,
        ).forEach { it.shouldNotBeNull() }
    }
}
