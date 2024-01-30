package github.nooblong.common.util;

import lombok.Data;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Data
public class RSA256Key {
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public KeyPair getKeyPair() {
        return new KeyPair(publicKey, privateKey);
    }
}
