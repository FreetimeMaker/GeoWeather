package com.freetime.sdk.payment

import java.math.BigDecimal

/**
 * Represents a cryptocurrency transaction
 */
data class Transaction(
    val id: String,
    val fromAddress: String,
    val toAddress: String,
    val amount: BigDecimal,
    val fee: BigDecimal,
    val coinType: CoinType,
    val rawData: ByteArray,
    val signature: ByteArray? = null,
    var status: TransactionStatus = TransactionStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Transaction

        if (id != other.id) return false
        if (fromAddress != other.fromAddress) return false
        if (toAddress != other.toAddress) return false
        if (amount != other.amount) return false
        if (fee != other.fee) return false
        if (coinType != other.coinType) return false
        if (!rawData.contentEquals(other.rawData)) return false
        if (signature != null) {
            if (other.signature == null) return false
            if (!signature.contentEquals(other.signature)) return false
        } else if (other.signature != null) return false
        if (status != other.status) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fromAddress.hashCode()
        result = 31 * result + toAddress.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + fee.hashCode()
        result = 31 * result + coinType.hashCode()
        result = 31 * result + rawData.contentHashCode()
        result = 31 * result + (signature?.contentHashCode() ?: 0)
        result = 31 * result + status.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Transaction status enumeration
 */
enum class TransactionStatus {
    PENDING,
    CONFIRMED,
    FAILED,
    CANCELLED
}
