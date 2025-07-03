package com.project.farming.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    // application.properties/yml 에서 설정한 키 파일 경로 주입
    @Value("${firebase.service-account.path}")
    private String serviceAccountPath;

    @PostConstruct // 빈(Bean) 생성 후 초기화 로직 실행
    public void initializeFirebaseAdminSdk() {
        try {
            // 이미 초기화되었는지 확인 (중복 초기화 방지)
            if (FirebaseApp.getApps().isEmpty()) {
                log.info("🚀 Initializing Firebase Admin SDK...");

                // ClassPathResource를 사용하여 classpath에서 리소스 로드
                ClassPathResource resource = new ClassPathResource(serviceAccountPath.replaceFirst("classpath:", ""));
                InputStream serviceAccountStream = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        // .setDatabaseUrl("https://<YOUR_DATABASE_NAME>.firebaseio.com") // Realtime Database 사용하는 경우
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase Admin SDK initialized successfully.");
            } else {
                log.info("ℹ️ Firebase Admin SDK already initialized.");
            }
        } catch (IOException e) {
            log.error("🔥 Failed to initialize Firebase Admin SDK: {}", e.getMessage(), e);
            // 초기화 실패는 심각한 문제일 수 있으므로, 애플리케이션 실행을 중단하거나
            // FCM 기능을 비활성화하는 등의 처리가 필요할 수 있습니다.
            throw new RuntimeException("Failed to initialize Firebase Admin SDK", e);
        }
    }
}