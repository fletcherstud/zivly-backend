package com.zivly.edge.security;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class AppleClientSecretGenerator {

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.apple.team-id}")
    private String teamId;

    @Value("${spring.security.oauth2.client.registration.apple.key-id}")
    private String keyId;

    @Value("${spring.security.oauth2.client.registration.apple.private-key-path:#{null}}")
    private String privateKeyPath; // Optional, used in dev

    @Value("${spring.security.oauth2.client.registration.apple.aws-secret-id:#{null}}")
    private String awsSecretId; // Optional, used in prod

    public String generate() {
        return Jwts.builder()
                .header().add("kid", keyId).and()
                .issuer(teamId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
                .claim("aud", "https://appleid.apple.com")
                .subject(clientId)
                .signWith(getPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey getPrivateKey() {
        try {
            String pem;
            if (privateKeyPath != null) {
                // Development: Load from file
                pem = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            } else if (awsSecretId != null) {
                // Production: Placeholder for AWS Secrets Manager (uncomment and add dependency if needed)
                throw new UnsupportedOperationException("AWS Secrets Manager not implemented yet");
                // Example (uncomment when ready):
                // GetSecretValueRequest request = GetSecretValueRequest.builder().secretId(awsSecretId).build();
                // String secretJson = secretsManagerClient.getSecretValue(request).secretString();
                // pem = new ObjectMapper().readTree(secretJson).get("apple-private-key").asText();
            } else {
                throw new IllegalStateException("Neither private-key-path nor aws-secret-id is configured");
            }

            pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Apple private key", e);
        }
    }
}