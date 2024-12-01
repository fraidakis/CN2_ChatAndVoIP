package com.cn2.communication;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;
import java.security.spec.X509EncodedKeySpec;

/**
 * Enhanced Security Module implementing:
 * 1. Public Key Infrastructure (PKI)
 * 2. Hybrid Encryption (RSA for key exchange, AES for communication)
 * 3. Secure key generation and exchange
 * 4. Message encryption and decryption
 */
public class SecurityModule {
    // RSA Key Pair for asymmetric encryption
    private KeyPair rsaKeyPair;    
    
    // Symmetric AES Secret Key
    private SecretKey symmetricKey;

    // Remote party's public key
    private PublicKey remotePublicKey;

    // Encryption flag
    private boolean isSecureConnectionEstablished = false;

    /**
     * Constructor to initialize key pairs
     * 
     */
    public SecurityModule() {
        try {
            // Generate RSA Key Pair for asymmetric encryption
            KeyPairGenerator rsaKeyPairGen = KeyPairGenerator.getInstance("RSA");
            rsaKeyPairGen.initialize(2048); // 2048-bit RSA key
            rsaKeyPair = rsaKeyPairGen.generateKeyPair();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the public key for sharing with other party
     */
    public String getPublicKey() {
        // Return the public key as Base64 encoded string because it is in binary format and cannot be directly sent
        return Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
    }

    /**
     * Set (receive and process) the remote party's public key 
     */
    public boolean setRemotePublicKey(String base64PublicKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes); // Wraps the raw binary public key (keyBytes) into a standard format to reconstruct the key
            KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Initialize RSA key factory
            remotePublicKey = keyFactory.generatePublic(keySpec); 

            // Generate AES symmetric key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256); // 256-bit AES key
            symmetricKey = keyGen.generateKey();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get the encrypted symmetric key to send to the remote party
     */
    public String getEncryptedSymmetricKey() {
        if (remotePublicKey == null || symmetricKey == null) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // Define the encryption algorithm
            cipher.init(Cipher.ENCRYPT_MODE, remotePublicKey); // Initialize the cipher with remote party's public key
            byte[] encryptedKey = cipher.doFinal(symmetricKey.getEncoded()); // Encrypt the symmetric key
            return Base64.getEncoder().encodeToString(encryptedKey); // Return the encrypted key as Base64 encoded string
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypt and set the symmetric key *received* from remote party
     */
    public boolean setRemoteSymmetricKey(String encryptedSymmetricKey) {
        try {
            // Decrypt the symmetric key using local private key
            byte[] encryptedKeyBytes = Base64.getDecoder().decode(encryptedSymmetricKey);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate()); // Decrypt symmetric key using local private key
            byte[] decryptedKeyBytes = cipher.doFinal(encryptedKeyBytes);

            // Reconstruct the symmetric key
            symmetricKey = new SecretKeySpec(decryptedKeyBytes, "AES");
            isSecureConnectionEstablished = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Encrypt a message using symmetric key
     */
    public String encrypt(String message) {

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypt a message using symmetric key
     */
    public String decrypt(String encryptedMessage) {

        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if a secure connection is established
     * @return true if secure connection is established, false otherwise
     */
    public boolean isSecureConnectionEstablished() {
        return isSecureConnectionEstablished;
    }
}