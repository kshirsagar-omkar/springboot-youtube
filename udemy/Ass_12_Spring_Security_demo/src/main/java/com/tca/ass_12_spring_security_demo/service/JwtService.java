package com.tca.ass_12_spring_security_demo.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {


    @Value("${jwt.secret}")
    private String secretKey;



//    public JwtService(){
//        secretKey = generateSecretKey();
//    }
//
//
//    public String generateSecretKey(){
//        try{
//            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
//            SecretKey secretKey = keyGen.generateKey();
//            System.out.println("Secret key : " + Base64.getEncoder().encodeToString(secretKey.getEncoded()));
//            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
//
//        }catch(NoSuchAlgorithmException e){
//            throw  new RuntimeException("Error generating secret key", e);
//        }
//    }

    public String generateToken(String username) {

        System.out.println("SECRET USED FOR SIGNING: " + secretKey);

        Map<String, Object> claims = new HashMap<>();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 3))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Key getKey() {
        if (secretKey == null) {
            throw new IllegalStateException("Secret key is NULL! Properties not loaded yet.");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
