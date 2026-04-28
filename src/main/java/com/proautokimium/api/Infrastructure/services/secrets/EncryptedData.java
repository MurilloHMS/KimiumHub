package com.proautokimium.api.Infrastructure.services.secrets;

public class EncryptedData {
    private byte[] cipherText;
    private byte[] iv;
    private byte[] authTag;

    public EncryptedData(byte[] cipherText, byte[] iv, byte[] authTag){
        this.cipherText = cipherText;
        this.iv = iv;
        this.authTag = authTag;
    }

    public byte[] getCipherText(){
        return cipherText;
    }

    public byte[] getIv() {
        return iv;
    }

    public byte[] getAuthTag() {
        return authTag;
    }
}
