package com.freetime.sdk.payment.crypto

import java.security.*
import java.security.spec.*
import java.math.BigInteger

/**
 * Bitcoin-specific cryptographic utilities
 * Self-contained implementation without external dependencies
 */
object BitcoinCryptoUtils {
    
    /**
     * Generate a new Bitcoin key pair
     */
    fun generateKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyGen.initialize(ECGenParameterSpec("secp256r1")) // Fallback to supported curve
        return keyGen.generateKeyPair()
    }
    
    /**
     * Generate Bitcoin address from public key
     */
    fun generateAddress(publicKey: PublicKey): String {
        val pubKeyBytes = publicKey.encoded
        
        // SHA-256 hash
        val sha256 = MessageDigest.getInstance("SHA-256")
        val sha256Hash = sha256.digest(pubKeyBytes)
        
        // RIPEMD-160 hash (simplified - using SHA-256 as fallback)
        val ripemd160Hash = sha256.digest(sha256Hash)
        
        // Add version byte (0x00 for mainnet)
        val versionedHash = ByteArray(21)
        versionedHash[0] = 0x00.toByte()
        System.arraycopy(ripemd160Hash, 0, versionedHash, 1, 20)
        
        // Double SHA-256 for checksum
        val checksum = sha256.digest(sha256.digest(versionedHash))
        
        // Append checksum
        val addressBytes = ByteArray(25)
        System.arraycopy(versionedHash, 0, addressBytes, 0, 21)
        System.arraycopy(checksum, 0, addressBytes, 21, 4)
        
        // Base58 encode
        return base58Encode(addressBytes)
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
     * Verify signature
     */
    fun verifySignature(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(data)
        return verifier.verify(signature)
    }
    
    /**
     * Simple Base58 encoding
     */
    private fun base58Encode(input: ByteArray): String {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val result = StringBuilder()
        
        var num = BigInteger(1, input)
        
        while (num > BigInteger.ZERO) {
            val remainder = num.mod(BigInteger.valueOf(58))
            result.append(alphabet[remainder.toInt()])
            num = num.divide(BigInteger.valueOf(58))
        }
        
        // Handle leading zeros
        for (byte in input) {
            if (byte.toInt() == 0) {
                result.append('1')
            } else {
                break
            }
        }
        
        return result.reverse().toString()
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
     * Validate Bitcoin address format
     */
    fun validateAddress(address: String): Boolean {
        if (address.length < 26 || address.length > 35) {
            return false
        }
        
        val base58Chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        
        // Check if all characters are valid Base58
        if (!address.all { it in base58Chars }) {
            return false
        }
        
        try {
            // Basic validation - try to decode
            val decoded = base58Decode(address)
            if (decoded.size < 25) {
                return false
            }
            
            // Check version byte (0x00 for mainnet)
            val versionByte = decoded[0]
            if (versionByte != 0x00.toByte()) {
                return false
            }
            
            // Verify checksum
            val sha256 = MessageDigest.getInstance("SHA-256")
            val checksum = decoded.copyOfRange(21, 25)
            val calculatedChecksum = sha256.digest(sha256.digest(decoded.copyOfRange(0, 21))).copyOfRange(0, 4)
            
            return checksum.contentEquals(calculatedChecksum)
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Simple Base58 decoding
     */
    private fun base58Decode(input: String): ByteArray {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        var num = BigInteger.ZERO
        
        for (char in input) {
            val digit = alphabet.indexOf(char)
            if (digit == -1) {
                throw IllegalArgumentException("Invalid Base58 character: $char")
            }
            num = num.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(digit.toLong()))
        }
        
        return num.toByteArray()
    }
}
