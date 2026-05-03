package com.example.projectii;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;

public class AESHelper {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    // ===== Derive a 256-bit AES key from the shared chat room ID =====
    // Both sender and receiver share the same senderRoom/receiverRoom pair
    // so we use the SMALLER of the two (alphabetically sorted) to get the
    // same key on both sides without exchanging it explicitly.
    public static SecretKey deriveKey(String senderRoom, String receiverRoom) throws Exception {

        // Always use the same room string regardless of who is sender/receiver
        // min alphabetically so both users derive the exact same key
        String sharedSecret = senderRoom.compareTo(receiverRoom) < 0
                ? senderRoom : receiverRoom;

        // SHA-256 hash of the shared secret → 32 bytes → AES-256 key
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(sharedSecret.getBytes("UTF-8"));
        keyBytes = Arrays.copyOf(keyBytes, 32); // ensure exactly 32 bytes

        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    // ===== Encrypt: C = E_K(P) =====
    // Returns "Base64(IV):Base64(CipherText)"
    // IV is random per message — required for CBC security
    public static String encrypt(String plainText,
                                 String senderRoom,
                                 String receiverRoom) throws Exception {

        SecretKey key = deriveKey(senderRoom, receiverRoom);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key); // auto-generates random IV

        byte[] iv         = cipher.getIV();
        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

        // Pack IV + ciphertext together so decrypt can unpack them
        String ivB64         = Base64.encodeToString(iv, Base64.NO_WRAP);
        String cipherTextB64 = Base64.encodeToString(cipherText, Base64.NO_WRAP);

        return ivB64 + ":" + cipherTextB64; // stored in Firebase as this string
    }

    // ===== Decrypt: P = D_K(C) =====
    // Expects "Base64(IV):Base64(CipherText)" format from encrypt()
    public static String decrypt(String encryptedText,
                                 String senderRoom,
                                 String receiverRoom) throws Exception {

        SecretKey key = deriveKey(senderRoom, receiverRoom);

        // Split the IV and ciphertext
        String[] parts     = encryptedText.split(":");
        byte[] iv          = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, "UTF-8");
    }
}