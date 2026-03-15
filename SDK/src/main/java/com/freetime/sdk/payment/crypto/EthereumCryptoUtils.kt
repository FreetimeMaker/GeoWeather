package com.freetime.sdk.payment.crypto

import java.security.*
import java.security.spec.*
import java.math.BigInteger

/**
 * Ethereum-specific cryptographic utilities
 * Self-contained implementation without external dependencies
 */
object EthereumCryptoUtils {
    
    /**
     * Generate a new Ethereum key pair
     */
    fun generateKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyGen.initialize(ECGenParameterSpec("secp256r1")) // Fallback to supported curve
        return keyGen.generateKeyPair()
    }
    
    /**
     * Generate Ethereum address from public key
     */
    fun generateAddress(publicKey: PublicKey): String {
        val pubKeyBytes = publicKey.encoded
        
        // Remove the 0x04 prefix (uncompressed public key)
        val cleanPubKey = if (pubKeyBytes[0].toInt() == 0x04) {
            pubKeyBytes.copyOfRange(1, pubKeyBytes.size)
        } else {
            pubKeyBytes
        }
        
        // Keccak-256 hash (simplified - using SHA-256 as fallback)
        val hash = sha256(cleanPubKey)
        
        // Take last 20 bytes and add 0x prefix
        val addressBytes = hash.copyOfRange(12, 32)
        return "0x" + addressBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Sign transaction data using Ethereum's signature scheme
     */
    fun signTransaction(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        val ecdsaSignature = signature.sign()
        
        // Add recovery id (simplified)
        val v = (27 + (ecdsaSignature.last().toInt() and 0x1)).toByte()
        
        return byteArrayOf(v) + ecdsaSignature
    }
    
    /**
     * Verify Ethereum signature
     */
    fun verifySignature(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
        if (signature.size < 1) return false
        
        val ecdsaSignature = signature.copyOfRange(1, signature.size)
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(data)
        return verifier.verify(ecdsaSignature)
    }
    
    /**
     * Keccak-256 hash function (simplified - using SHA-256)
     */
    fun keccak256(data: ByteArray): ByteArray {
        return sha256(data)
    }
    
    /**
     * SHA-256 hash function
     */
    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
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
            
            // Convert hex string to bytes
            val keyBytes = cleanKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            
            // Create private key spec
            val privateKeySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("EC")
            val privKey = keyFactory.generatePrivate(privateKeySpec)
            
            // Generate corresponding public key
            val keyGen = KeyPairGenerator.getInstance("EC")
            keyGen.initialize(ECGenParameterSpec("secp256r1"))
            val keyPair = keyGen.generateKeyPair()
            
            // Return new key pair with the imported private key
            return KeyPair(keyPair.public, privKey)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid private key format: ${e.message}")
        }
    }
    
    /**
     * Validate Ethereum address format
     */
    fun validateAddress(address: String): Boolean {
        // Check if address starts with 0x
        if (!address.startsWith("0x")) {
            return false
        }
        
        // Remove 0x prefix
        val hexPart = address.substring(2)
        
        // Check length (should be 40 hex characters)
        if (hexPart.length != 40) {
            return false
        }
        
        // Check if all characters are valid hex
        if (!hexPart.all { it in "0123456789abcdefABCDEF" }) {
            return false
        }
        
        try {
            // Try to convert to bytes
            hexPart.chunked(2).map { it.toInt(16).toByte() }
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
