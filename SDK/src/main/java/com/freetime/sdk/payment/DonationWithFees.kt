package com.freetime.sdk.payment

import com.freetime.sdk.payment.fee.FeeBreakdown
import java.math.BigDecimal

/**
 * Represents a donation with detailed fee breakdown
 */
data class DonationWithFees(
    val donation: Donation,
    val feeBreakdown: FeeBreakdown
) {
    val totalAmount: BigDecimal get() = donation.amount + feeBreakdown.totalFee
    val recipientAmount: BigDecimal get() = donation.amount
    val networkFee: BigDecimal get() = feeBreakdown.networkFee
    val developerFee: BigDecimal get() = feeBreakdown.developerFee
}
