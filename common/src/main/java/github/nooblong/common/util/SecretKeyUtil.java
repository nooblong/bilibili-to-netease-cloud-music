package github.nooblong.common.util;


import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SecretKeyUtil {

    public static final String KEY_ALGORITHM = "RSA";

    private static final String PUBLIC_KEY = "enter your RSAPublicKey";
    private static final String PRIVATE_KEY = "enter your RSAPrivateKey";

    private static volatile RSA256Key rsa256Key;

    //获得公钥
    public static String getPublicKey(Map<String, Object> keyMap) throws Exception {
        //获得map中的公钥对象 转为key对象
        Key key = (Key) keyMap.get(PUBLIC_KEY);
        //byte[] publicKey = key.getEncoded();
        //编码返回字符串
        return encryptBASE64(key.getEncoded());
    }

    public static String getPublicKey(RSA256Key rsa256Key) throws Exception {
        //获得map中的公钥对象 转为key对象
        Key key = rsa256Key.getPublicKey();
        //byte[] publicKey = key.getEncoded();
        //编码返回字符串
        return encryptBASE64(key.getEncoded());
    }

    //获得私钥
    public static String getPrivateKey(Map<String, Object> keyMap) throws Exception {
        //获得map中的私钥对象 转为key对象
        Key key = (Key) keyMap.get(PRIVATE_KEY);
        //byte[] privateKey = key.getEncoded();
        //编码返回字符串
        return encryptBASE64(key.getEncoded());
    }

    //获得私钥
    public static String getPrivateKey(RSA256Key rsa256Key) throws Exception {
        //获得map中的私钥对象 转为key对象
        Key key = rsa256Key.getPrivateKey();
        //byte[] privateKey = key.getEncoded();
        //编码返回字符串
        return encryptBASE64(key.getEncoded());
    }

    //解码返回byte
    public static byte[] decryptBASE64(String key) throws Exception {
        return (Base64.getDecoder()).decode(key);
    }

    //编码返回字符串
    public static String encryptBASE64(byte[] key) throws Exception {
        return (Base64.getEncoder()).encodeToString(key);
    }

    //使用KeyPairGenerator 生成公私钥，存放于map对象中
    public static Map<String, Object> initKey() throws Exception {
        /* RSA算法要求有一个可信任的随机数源 */
        //获得对象 KeyPairGenerator 参数 RSA 1024个字节
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyPairGen.initialize(1024);

        //通过对象 KeyPairGenerator 生成密匙对 KeyPair
        KeyPair keyPair = keyPairGen.generateKeyPair();

        //通过对象 KeyPair 获取RSA公私钥对象RSAPublicKey RSAPrivateKey
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        //公私钥对象存入map中
        Map<String, Object> keyMap = new HashMap<>(2);
        keyMap.put(PUBLIC_KEY, publicKey);
        keyMap.put(PRIVATE_KEY, privateKey);
        return keyMap;
    }

    /**
     * 获取公私钥
     */
    public static synchronized RSA256Key getRSA256Key() throws Exception {
        if (rsa256Key == null) {
            synchronized (RSA256Key.class) {
                if (rsa256Key == null) {
                    rsa256Key = new RSA256Key();
                    Map<String, Object> map = initKey();
                    rsa256Key.setPrivateKey((RSAPrivateKey) map.get(SecretKeyUtil.PRIVATE_KEY));
                    rsa256Key.setPublicKey((RSAPublicKey) map.get(SecretKeyUtil.PUBLIC_KEY));
                }
            }
        }
        return rsa256Key;
    }
}
