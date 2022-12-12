package com.lgt.cwm.util;

/**
 * Created by giangtpu on 7/25/22.
 */
public abstract class Encryption {
    public abstract byte[] encrypt(byte[] key, byte[] data, byte[] iv) throws Exception;

    public abstract byte[] decrypt(byte[] key, byte[] data, byte[] iv) throws Exception;
}
