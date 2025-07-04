package com.project.farming.global.security;

import com.project.farming.global.jwtToken.CustomUserDetailsService;
import com.project.farming.global.jwtToken.JwtAuthenticationFilter;
import com.project.farming.global.oauth.CustomOAuth2UserService;
import com.project.farming.global.oauth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager; // 이 import를 추가하세요
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration; // 이 import를 추가하세요
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // Lombok이 모든 final 필드를 주입합니다.
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomUserDetailsService customUserDetailsService; // 주입되지만 여기서는 직접 사용되지 않음; AuthenticationManager가 사용
    private final CustomOAuth2UserService customOAuth2UserService;

    // JwtAuthenticationFilter는 이미 @Component로 관리되므로 여기서 별도의 @Bean 메서드로 만들 필요가 없습니다.
    // 스프링이 자동으로 빈으로 등록하고 주입해 줍니다.

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(httpBasic -> httpBasic.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // /auth 경로에서 인증 없이 접근 허용할 API들 추가
                        .requestMatchers("/auth/register").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/auth/token/refresh").permitAll()

                        // 기존에 열려있던 경로들
                        .requestMatchers("/users/sign-in").permitAll()
                        .requestMatchers("/users/sign-up").permitAll()
                        .requestMatchers("/api/notify/**").permitAll()
                        .requestMatchers("/api/alarms/**").permitAll()
                        .requestMatchers("/users/profile/**").permitAll()

                        // 인증 필요 경로
                        .requestMatchers("/api/diaries/**").authenticated()
                        .requestMatchers("/auth/sign-out").authenticated()
                        .requestMatchers("/api/place/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/fcm-token").authenticated()
                        .requestMatchers("/users/test").hasRole("USER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

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
        // BCrypt를 기본으로 사용하는 DelegatingPasswordEncoder를 사용하여 비밀번호를 안전하게 해싱합니다.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        // AuthenticationManager를 빈으로 노출하여 수동 인증 (예: AuthService)에 사용할 수 있도록 합니다.
        return authenticationConfiguration.getAuthenticationManager();
    }
}