package com.freetime.sdk.payment.crypto

import java.security.*
import java.security.spec.*
import java.math.BigInteger
import java.util.Base64

/**
 * Cardano cryptographic utilities
 * Simplified implementation for educational purposes
 */
object CardanoCryptoUtils {
    
    /**
     * Generate Cardano key pair
     */
    fun generateKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec("secp256k1")
        keyGen.initialize(ecSpec, SecureRandom())
        return keyGen.generateKeyPair()
    }
    
    /**
     * Generate Cardano address from public key
     */
    fun generateAddress(publicKey: PublicKey): String {
        // Simplified address generation for Cardano
        val pubKeyBytes = publicKey.encoded
        val hash = sha256(pubKeyBytes)
        val addressBytes = hash.copyOf(28) // Cardano uses 28-byte addresses
        
        // Add Cardano prefix
        val prefixed = byteArrayOf(0x01) + addressBytes
        
        // Add checksum
        val checksum = sha256(sha256(prefixed)).copyOf(4)
        val fullAddress = prefixed + checksum
        
        // Base58 encode
        return base58Encode(fullAddress)
    }
    
    /**
     * Convert private key string to KeyPair
     */
    fun privateKeyToKeyPair(privateKey: String): KeyPair {
        try {
            // Remove hex prefix if present
            val cleanKey = if (privateKey.startsWith("0x")) {
                privateKey.substring(2)
            } else {
                privateKey
            }
            
            // Convert hex string to byte array
            val keyBytes = cleanKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            
            // Create private key specification
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            
            // Generate key pair from private key bytes
            val keyFactory = KeyFactory.getInstance("EC")
            val privateKey = keyFactory.generatePrivate(keySpec)
            
            // Generate public key from private key
            val keyGen = KeyPairGenerator.getInstance("EC")
            val ecSpec = ECGenParameterSpec("secp256k1")
            keyGen.initialize(ecSpec, SecureRandom())
            val keyPair = keyGen.generateKeyPair()
            
            // Create new key pair with the imported private key and generated public key
            return KeyPair(keyPair.public, privateKey)
            
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid private key format for Cardano: ${e.message}")
        }
    }
    
    /**
     * Sign transaction data
     */
    fun signTransaction(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }
    
    /**
     * Verify transaction signature
     */
    fun verifySignature(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
        try {
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(data)
            return sig.verify(signature)
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Create Cardano transaction
     */
    fun createTransaction(
        fromAddress: String,
        toAddress: String,
        amount: Double,
        privateKey: PrivateKey
    ): ByteArray {
        // Simplified Cardano transaction creation
        val amountLovelace = (amount * 1_000_000).toLong() // ADA has 6 decimal places
        
        val txData = ByteArray(200) // Simplified transaction size
        var offset = 0
        
        // Transaction header
        txData[offset++] = 0x00.toByte() // Transaction type
        
        // Input count
        txData[offset++] = 0x01.toByte()
        
        // Input (simplified)
        offset += 32 // Previous tx hash
        offset += 4  // Previous tx output index
        
        // Output count
        txData[offset++] = 0x01.toByte()
        
        // Output address (simplified)
        offset += 28 // Address hash
        
        // Output amount
        val amountBytes = amountLovelace.toBytes()
        System.arraycopy(amountBytes, 0, txData, offset, 8)
        offset += 8
        
        // Fee
        val feeBytes = 17000L.toBytes() // Minimum ADA fee
        System.arraycopy(feeBytes, 0, txData, offset, 8)
        offset += 8
        
        // TTL (Time to Live)
        txData[offset++] = 0x00.toByte()
        txData[offset++] = 0x00.toByte()
        txData[offset++] = 0x00.toByte()
        txData[offset++] = 0x00.toByte()
        
        return txData
    }
    
    /**
     * Calculate Cardano transaction fee
     */
    fun calculateFee(txData: ByteArray): Double {
        // Cardano minimum fee is 0.17 ADA
        return 0.17
    }
    
    private fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }
    
    private fun base58Encode(data: ByteArray): String {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        var num = BigInteger(1, data)
        val encoded = StringBuilder()
        
        while (num > BigInteger.ZERO) {
            val remainder = num.mod(BigInteger.valueOf(58))
            encoded.insert(0, alphabet[remainder.toInt()])
            num = num.divide(BigInteger.valueOf(58))
        }
        
        return encoded.toString()
    }
    
    private fun Long.toByteArray(): ByteArray {
        return byteArrayOf(
            (this shr 0).toByte(),
            (this shr 8).toByte(),
            (this shr 16).toByte(),
            (this shr 24).toByte(),
            (this shr 32).toByte(),
            (this shr 40).toByte(),
            (this shr 48).toByte(),
            (this shr 56).toByte()
        )
    }
    
    private fun Long.toBytes(): ByteArray = this.toByteArray()
}
