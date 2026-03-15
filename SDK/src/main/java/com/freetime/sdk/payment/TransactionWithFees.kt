package com.freetime.sdk.payment

import com.freetime.sdk.payment.fee.FeeBreakdown

/**
 * Transaction result with fee breakdown
 */
data class TransactionWithFees(
    val transaction: Transaction,
    val feeBreakdown: FeeBreakdown
) {
    /**
     * Get formatted transaction summary
     */
    fun getFormattedSummary(): String {
        return buildString {
            appendLine("Transaction Summary:")
            appendLine("Transaction ID: ${transaction.id}")
            appendLine("From: ${transaction.fromAddress}")
            appendLine("To: ${transaction.toAddress}")
            appendLine("Coin: ${transaction.coinType.symbol}")
            appendLine()
            appendLine("Amount Details:")
            appendLine("Original Amount: ${feeBreakdown.originalAmount} ${transaction.coinType.symbol}")
            appendLine("Network Fee: ${feeBreakdown.networkFee} ${transaction.coinType.symbol}")
            appendLine("Developer Fee (1%): ${feeBreakdown.developerFee} ${transaction.coinType.symbol}")
            appendLine("Total Fee: ${feeBreakdown.totalFee} ${transaction.coinType.symbol}")
            appendLine("Recipient Receives: ${feeBreakdown.recipientAmount} ${transaction.coinType.symbol}")
            appendLine()
            appendLine("Developer Wallet: ${feeBreakdown.developerWalletAddress}")
            appendLine("Status: ${transaction.status}")
        }
    }
    
    /**
     * Broadcast the transaction
     */
    suspend fun broadcast(): String {
        // In a real implementation, this would broadcast to the network
        transaction.status = TransactionStatus.CONFIRMED
        return transaction.id
    }
}
