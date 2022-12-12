package com.lgt.cwm.util;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by giangtpu on 7/25/22.
 */
public class EncryptionAESByte extends Encryption {

    protected final static String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    protected static Cipher cipher;

    public EncryptionAESByte() {
        try {
            if (cipher == null) {
                cipher = Cipher.getInstance(TRANSFORMATION);
            }
        } catch (Exception e) {
            cipher = null;
        }
    }

    @Override
    public byte[] encrypt(byte[] key, byte[] data, byte[] iv) {
        try {
            if (data.length == 0 || key.length == 0 || iv.length == 0) {
                return null;
            }

            IvParameterSpec ivParam = new IvParameterSpec(iv);
            SecretKey secretKey = getSecretKey(key);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParam);
            byte[] encryptedByte = cipher.doFinal(data);
            return encryptedByte;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public byte[] decrypt(byte[] key, byte[] data, byte[] iv) {
        try {
            if (data.length == 0 || key.length == 0 || iv.length == 0) {
                return null;
            }
            IvParameterSpec ivParam = new IvParameterSpec(iv);
            SecretKey secretKey = getSecretKey(key);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParam);
            byte[] decryptedByte = cipher.doFinal(data);
            return decryptedByte;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected SecretKey getSecretKey(byte[] key) {
        return new SecretKeySpec(key, "AES");
    }
}
