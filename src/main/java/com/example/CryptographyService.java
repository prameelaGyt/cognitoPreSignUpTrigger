package com.example;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptographyService {

    String secretKey = "aIuP6EtBhqQ8Uvd4";

    String algorithm = "AES";

    public String encrypt(String value) throws Exception {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        }catch (Exception e){
            throw new RuntimeException("Failed to encrypt email/phoneNumber");
        }
    }

    public String decrypt(String encrypted) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        }catch (Exception e){
            throw new RuntimeException("Failed to decrypt email/phoneNumber");
        }
    }


}
