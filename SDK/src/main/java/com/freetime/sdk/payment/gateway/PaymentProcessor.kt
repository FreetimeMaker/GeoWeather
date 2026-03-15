package com.freetime.sdk.payment.gateway

import com.freetime.sdk.payment.*
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.util.concurrent.Executors

/**
 * Automatischer Payment Processor für die Verarbeitung von eingehenden Zahlungen
 */
class PaymentProcessor(
    private val paymentGateway: PaymentGateway,
    private val checkInterval: Long = 5000L // 5 Sekunden
) {
    
    private val processorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isProcessing = false
    private var processingJob: Job? = null
    
    private val paymentListeners = mutableListOf<PaymentListener>()
    
    /**
     * Startet die automatische Zahlungsüberprüfung
     */
    fun startProcessing() {
        if (isProcessing) return
        
        isProcessing = true
        processingJob = processorScope.launch {
            while (isProcessing) {
                try {
                    processPendingPayments()
                    delay(checkInterval)
                } catch (e: Exception) {
                    // Logge Fehler aber fahre fort
                    println("Fehler bei der Zahlungsverarbeitung: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Stoppt die automatische Zahlungsüberprüfung
     */
    fun stopProcessing() {
        isProcessing = false
        processingJob?.cancel()
        processingJob = null
    }
    
    /**
     * Verarbeitet alle ausstehenden Zahlungen
     */
    private suspend fun processPendingPayments() {
        val pendingPayments = paymentGateway.getPendingPayments()
        
        for (payment in pendingPayments) {
            val oldStatus = payment.status
            val newStatus = paymentGateway.checkPaymentStatus(payment.id)
            
            if (oldStatus != newStatus) {
                // Status hat sich geändert - benachrichtige Listener
                notifyStatusChange(payment.id, oldStatus, newStatus)
                
                when (newStatus) {
                    PaymentStatus.CONFIRMED -> {
                        val details = paymentGateway.getPaymentDetails(payment.id)
                        details?.let { notifyPaymentConfirmed(it) }
                    }
                    PaymentStatus.EXPIRED -> {
                        notifyPaymentExpired(payment.id)
                    }
                    PaymentStatus.FORWARDING_FAILED -> {
                        notifyForwardingFailed(payment.id)
                    }
                    else -> { /* andere Status */ }
                }
            }
        }
    }
    
    /**
     * Fügt einen Payment Listener hinzu
     */
    fun addPaymentListener(listener: PaymentListener) {
        paymentListeners.add(listener)
    }
    
    /**
     * Entfernt einen Payment Listener
     */
    fun removePaymentListener(listener: PaymentListener) {
        paymentListeners.remove(listener)
    }
    
    /**
     * Benachrichtigt alle Listener über Statusänderungen
     */
    private fun notifyStatusChange(paymentId: String, oldStatus: PaymentStatus, newStatus: PaymentStatus) {
        paymentListeners.forEach { listener ->
            try {
                listener.onPaymentStatusChanged(paymentId, oldStatus, newStatus)
            } catch (e: Exception) {
                println("Fehler im Payment Listener: ${e.message}")
            }
        }
    }
    
    /**
     * Benachrichtigt alle Listener über bestätigte Zahlungen
     */
    private fun notifyPaymentConfirmed(details: PaymentDetails) {
        paymentListeners.forEach { listener ->
            try {
                listener.onPaymentConfirmed(details)
            } catch (e: Exception) {
                println("Fehler im Payment Listener: ${e.message}")
            }
        }
    }
    
    /**
     * Benachrichtigt alle Listener über abgelaufene Zahlungen
     */
    private fun notifyPaymentExpired(paymentId: String) {
        paymentListeners.forEach { listener ->
            try {
                listener.onPaymentExpired(paymentId)
            } catch (e: Exception) {
                println("Fehler im Payment Listener: ${e.message}")
            }
        }
    }
    
    /**
     * Benachrichtigt alle Listener über Weiterleitungsfehler
     */
    private fun notifyForwardingFailed(paymentId: String) {
        paymentListeners.forEach { listener ->
            try {
                listener.onForwardingFailed(paymentId)
            } catch (e: Exception) {
                println("Fehler im Payment Listener: ${e.message}")
            }
        }
    }
    
    /**
     * Prüft ob der Processor aktiv ist
     */
    fun isRunning(): Boolean = isProcessing
}

/**
 * Interface für Payment Event Listener
 */
interface PaymentListener {
    /**
     * Wird aufgerufen wenn sich der Zahlungsstatus ändert
     */
    fun onPaymentStatusChanged(paymentId: String, oldStatus: PaymentStatus, newStatus: PaymentStatus) {}
    
    /**
     * Wird aufgerufen wenn eine Zahlung bestätigt wurde
     */
    fun onPaymentConfirmed(details: PaymentDetails) {}
    
    /**
     * Wird aufgerufen wenn eine Zahlung abgelaufen ist
     */
    fun onPaymentExpired(paymentId: String) {}
    
    /**
     * Wird aufgerufen wenn die Weiterleitung fehlgeschlagen ist
     */
    fun onForwardingFailed(paymentId: String) {}
}

/**
 * Einfacher Payment Listener für Logging
 */
class LoggingPaymentListener : PaymentListener {
    override fun onPaymentStatusChanged(paymentId: String, oldStatus: PaymentStatus, newStatus: PaymentStatus) {
        println("Zahlungsstatus geändert: $paymentId von $oldStatus zu $newStatus")
    }
    
    override fun onPaymentConfirmed(details: PaymentDetails) {
        println("Zahlung bestätigt: ${details.paymentRequest.id}")
        println("Betrag: ${details.currentBalance} ${details.paymentRequest.coinType.symbol}")
        println("Weiterleitung: ${details.forwardedTxHash}")
    }
    
    override fun onPaymentExpired(paymentId: String) {
        println("Zahlung abgelaufen: $paymentId")
    }
    
    override fun onForwardingFailed(paymentId: String) {
        println("Weiterleitung fehlgeschlagen: $paymentId")
    }
}
