package com.freetime.sdk.payment.gateway

import com.freetime.sdk.payment.*
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

/**
 * Payment Gateway für automatische Weiterleitung von Zahlungen
 * an eine fest konfigurierte Empfängeradresse
 */
class PaymentGateway(
    private val sdk: FreetimePaymentSDK,
    private val merchantWalletAddress: String,
    private val merchantCoinType: CoinType
) {
    
    private val pendingPayments = ConcurrentHashMap<String, PendingPayment>()
    private val confirmedPayments = ConcurrentHashMap<String, ConfirmedPayment>()
    
    /**
     * Erstellt eine direkte Zahlungsanfrage vom Benutzer an den Händler
     */
    suspend fun createPaymentAddress(
        amount: BigDecimal,
        customerReference: String? = null,
        description: String? = null,
        /** Benutzer-Wallet-Adresse von der die Zahlung gesendet wird */
        providedWallet: String? = null,
        /** Optionale externe Adresse, an die empfangene Gelder weitergeleitet werden sollen */
        forwardToAddress: String? = null
    ): PaymentRequest {
        
        // Verwende die bereitgestellte Benutzer-Wallet-Adresse oder erstelle eine temporäre Adresse
        val userWalletAddress = providedWallet ?: generateTemporaryAddress()
        
        val paymentRequest = PaymentRequest(
            id = generatePaymentId(),
            customerAddress = userWalletAddress,
            merchantAddress = merchantWalletAddress,
            amount = amount,
            coinType = merchantCoinType,
            customerReference = customerReference,
            description = description,
            status = PaymentStatus.PENDING,
            forwardToAddress = forwardToAddress,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + PAYMENT_TIMEOUT
        )
        
        pendingPayments[paymentRequest.id] = PendingPayment(
            paymentRequest = paymentRequest,
            tempAddress = userWalletAddress
        )
        
        return paymentRequest
    }
    
    /**
     * Führt die direkte Zahlung vom Benutzer an den Händler durch
     */
    suspend fun executeDirectPayment(
        userWalletAddress: String,
        amount: BigDecimal,
        customerReference: String? = null,
        description: String? = null,
        forwardToAddress: String? = null
    ): TransactionWithFees {
        
        // Validiere die Benutzer-Wallet-Adresse
        if (!sdk.validateAddress(userWalletAddress, merchantCoinType)) {
            throw IllegalArgumentException("Ungültige Benutzer-Wallet-Adresse: $userWalletAddress")
        }
        
        // Bestimme das Ziel der Zahlung
        val destination = forwardToAddress ?: merchantWalletAddress
        
        // Führe die direkte Zahlung durch
        return sdk.send(
            fromAddress = userWalletAddress,
            toAddress = destination,
            amount = amount,
            coinType = merchantCoinType
        )
    }
    
    /**
     * Überprüft den Zahlungsstatus und leitet bei vollständiger Zahlung weiter
     */
    suspend fun checkPaymentStatus(paymentId: String): PaymentStatus {
        val pendingPayment = pendingPayments[paymentId] 
            ?: return PaymentStatus.NOT_FOUND
        
        val paymentRequest = pendingPayment.paymentRequest
        
        // Prüfe ob die Zahlung abgelaufen ist
        if (System.currentTimeMillis() > paymentRequest.expiresAt) {
            pendingPayments.remove(paymentId)
            return PaymentStatus.EXPIRED
        }
        
        // Prüfe das Guthaben auf der temporären Adresse
        val currentBalance = getBalanceForAddress(pendingPayment.tempAddress, merchantCoinType)
        
        if (currentBalance >= paymentRequest.amount) {
            // Zahlung erhalten - leite an Zieladresse weiter
            try {
                // Wenn eine providedWallet Adresse angegeben wurde, sende direkt dorthin
                // Ansonsten an die forwardToAddress oder Händler-Adresse
                val destination = when {
                    paymentRequest.forwardToAddress != null -> paymentRequest.forwardToAddress
                    pendingPayment.tempAddress != paymentRequest.customerAddress -> {
                        // Wenn providedWallet verwendet wurde, ist dies die Zieladresse
                        pendingPayment.tempAddress
                    }
                    else -> merchantWalletAddress
                }
                
                val txHash = sdk.send(
                    fromAddress = pendingPayment.tempAddress,
                    toAddress = destination,
                    amount = paymentRequest.amount,
                    coinType = merchantCoinType
                )
                
                // Markiere als bestätigt
                val confirmedPayment = ConfirmedPayment(
                    paymentRequest = paymentRequest,
                    receivedAmount = currentBalance,
                    forwardedTxHash = txHash.transaction.id,
                    confirmedAt = System.currentTimeMillis()
                )
                
                confirmedPayments[paymentId] = confirmedPayment
                pendingPayments.remove(paymentId)
                
                return PaymentStatus.CONFIRMED
                
            } catch (e: Exception) {
                // Fehler beim Weiterleiten
                paymentRequest.status = PaymentStatus.FORWARDING_FAILED
                return PaymentStatus.FORWARDING_FAILED
            }
        }
        
        return PaymentStatus.PENDING
    }
    
    /**
     * Ruft detaillierte Zahlungsinformationen ab
     */
    fun getPaymentDetails(paymentId: String): PaymentDetails? {
        val pending = pendingPayments[paymentId]
        if (pending != null) {
            val currentBalance = try {
                // Synchroner Aufruf für Balance-Check
                runBlocking { getBalanceForAddress(pending.tempAddress, merchantCoinType) }
            } catch (e: Exception) {
                BigDecimal.ZERO
            }
            
            return PaymentDetails(
                paymentRequest = pending.paymentRequest,
                currentBalance = currentBalance,
                remainingAmount = maxOf(BigDecimal.ZERO, pending.paymentRequest.amount - currentBalance)
            )
        }
        
        val confirmed = confirmedPayments[paymentId]
        if (confirmed != null) {
            return PaymentDetails(
                paymentRequest = confirmed.paymentRequest,
                currentBalance = confirmed.receivedAmount,
                remainingAmount = BigDecimal.ZERO,
                forwardedTxHash = confirmed.forwardedTxHash,
                confirmedAt = confirmed.confirmedAt
            )
        }
        
        return null
    }
    
    /**
     * Storniert eine ausstehende Zahlung
     */
    fun cancelPayment(paymentId: String): Boolean {
        val pending = pendingPayments.remove(paymentId)
        return pending != null
    }
    
    /**
     * Ruft alle ausstehenden Zahlungen ab
     */
    fun getPendingPayments(): List<PaymentRequest> {
        return pendingPayments.values.map { it.paymentRequest }
    }
    
    /**
     * Ruft alle bestätigten Zahlungen ab
     */
    fun getConfirmedPayments(): List<ConfirmedPayment> {
        return confirmedPayments.values.toList()
    }
    
    /**
     * Generiert eine eindeutige Zahlungs-ID
     */
    private fun generatePaymentId(): String {
        return "pay_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * Generiert eine temporäre Adresse für Zahlungen
     */
    private fun generateTemporaryAddress(): String {
        return "temp_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * Ruft das Guthaben für eine Adresse ab (simuliert)
     */
    private suspend fun getBalanceForAddress(address: String, coinType: CoinType): BigDecimal {
        // Hier würde normalerweise eine echte Blockchain-Abfrage stattfinden
        // Für jetzt geben wir 0 zurück, da wir keine Wallets mehr erstellen
        return BigDecimal.ZERO
    }
    
    companion object {
        private const val PAYMENT_TIMEOUT = 30 * 60 * 1000L // 30 Minuten in Millisekunden
    }
}

/**
 * Repräsentiert eine Zahlungsanfrage
 */
data class PaymentRequest(
    val id: String,
    val customerAddress: String,
    val merchantAddress: String,
    val amount: BigDecimal,
    val coinType: CoinType,
    val customerReference: String? = null,
    val description: String? = null,
    var status: PaymentStatus = PaymentStatus.PENDING,
    /** Falls gesetzt, werden empfangene Gelder an diese Adresse weitergeleitet. */
    val forwardToAddress: String? = null,
    val createdAt: Long,
    val expiresAt: Long
)

/**
 * Repräsentiert eine ausstehende Zahlung
 */
private data class PendingPayment(
    val paymentRequest: PaymentRequest,
    val tempAddress: String
)

/**
 * Repräsentiert eine bestätigte Zahlung
 */
data class ConfirmedPayment(
    val paymentRequest: PaymentRequest,
    val receivedAmount: BigDecimal,
    val forwardedTxHash: String,
    val confirmedAt: Long
)

/**
 * Detaillierte Zahlungsinformationen
 */
data class PaymentDetails(
    val paymentRequest: PaymentRequest,
    val currentBalance: BigDecimal,
    val remainingAmount: BigDecimal,
    val forwardedTxHash: String? = null,
    val confirmedAt: Long? = null
)

/**
 * Zahlungsstatus Enumeration
 */
enum class PaymentStatus {
    PENDING,
    CONFIRMED,
    EXPIRED,
    FORWARDING_FAILED,
    NOT_FOUND
}
