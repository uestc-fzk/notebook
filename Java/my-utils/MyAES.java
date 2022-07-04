package com.scrm.marketing.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author fzk
 * @date 2021-07-16 9:34
 * AES对称加密算法，借助Hutool工具完成
 */
@ConfigurationProperties(value = "my.aes")
@Component
public class MyAES {

    private String secret = "1234567890123456";

    /**
     * 加密
     * @param content 需要加密的字符串
     * @return 返回加密后的字符串
     */
    public String encryptHex(String content) {
        //随机生成密钥
//        byte[] key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
//        System.out.println(secret);
        byte[] key = secret.getBytes();

        //构建
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, key);

        //加密
        //byte[] encrypt = aes.encrypt(content);
        //解密
        //byte[] decrypt = aes.decrypt(encrypt);

        //加密为16进制表示
        //String encryptHex = aes.encryptHex(content);
        //解密为字符串
        //String decryptStr = aes.decryptStr(encryptHex, CharsetUtil.CHARSET_UTF_8);

        return aes.encryptHex(content);

    }

    /**
     * 解密
     * @param str 需要解密的字符串
     * @return 返回解密后的密码
     */
    public String decryptStr(String str) {
        // 秘钥
        byte[] key = secret.getBytes();

        //构建
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, key);

        //解密为字符串
        return aes.decryptStr(str, CharsetUtil.CHARSET_UTF_8);
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}

