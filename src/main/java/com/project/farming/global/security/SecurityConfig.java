package com.project.farming.global.security;

import com.project.farming.global.jwtToken.CustomUserDetailsService;
import com.project.farming.global.jwtToken.JwtAuthenticationFilter;
import com.project.farming.global.oauth.CustomOAuth2UserService;
import com.project.farming.global.oauth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(httpBasic -> httpBasic.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // --- 1. 인증 없이 허용 (permitAll()) ---

                        // AuthController
                        // 회원가입, 로그인, 토큰 재발급은 인증 없이 접근 가능
                        .requestMatchers("/auth/register").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/auth/token/refresh").permitAll()

                        // OAuth2 로그인 관련 모든 경로 (리다이렉션 포함)
                        .requestMatchers("/oauth2/**").permitAll()

                        // 기존에 열려있던 경로들 (현재 컨트롤러 코드에는 없지만, 이전 기록에 있었으므로 혹시 몰라 유지)
                        .requestMatchers("/users/sign-in").permitAll() // ⭐ 만약 AuthController에서 처리 안하면 제거하세요.
                        .requestMatchers("/users/sign-up").permitAll() // ⭐ 만약 AuthController에서 처리 안하면 제거하세요.
                        .requestMatchers("/api/notify/test").permitAll() // 특정 알림 테스트 API

                        // Swagger UI 관련 (개발 및 테스트 편의를 위해)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/webjars/**",  "/swagger-ui.html").permitAll()


                        // --- 2. 인증 필요 (authenticated()) ---

                        // AuthController
                        // 로그아웃, 현재 사용자 정보 조회는 인증 필요
                        .requestMatchers("/auth/logout").authenticated() // AuthController에 명시된 그대로 '/auth/logout'
                        .requestMatchers("/auth/user/me").authenticated()

                        // PlantController (모든 엔드포인트)
                        // PlantController는 @SecurityRequirement가 없지만, 모든 CRUD는 인증 필요하다고 가정
                        .requestMatchers("/plants/**").authenticated()

                        // UserPlantController (모든 엔드포인트)
                        // UserPlantController는 @SecurityRequirement가 없지만, 모든 CRUD는 인증 필요하다고 가정
                        .requestMatchers("/users/plants/**").authenticated()

                        // DiaryController (모든 엔드포인트)
                        // DiaryController는 @Tag 및 @SecurityRequirement(name = "jwtAuth")가 있으므로 모두 인증 필요
                        .requestMatchers("/api/users/plants/**").authenticated()
                        .requestMatchers("/api/diaries/**").authenticated()

                        // 기타 인증 필요 경로 (제공해주신 기존 설정 유지)
                        .requestMatchers("/api/notify/**").authenticated() // 알림 관련 (테스트용 제외)
                        .requestMatchers("/api/alarms/**").authenticated() // 알람 관련
                        .requestMatchers("/users/profile/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/fcm-token").authenticated()

                        // 그 외 모든 요청은 인증 필요 (기본 보안 정책)
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}