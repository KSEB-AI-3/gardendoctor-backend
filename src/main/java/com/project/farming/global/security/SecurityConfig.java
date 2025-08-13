package com.project.farming.global.security;

import com.project.farming.global.jwtToken.CustomUserDetailsService;
import com.project.farming.global.jwtToken.JwtAuthenticationFilter;
import com.project.farming.global.oauth.CustomOAuth2UserService;
import com.project.farming.global.oauth.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
    private final AdminAuthenticationSuccessHandler adminAuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(httpBasic -> httpBasic.disable())
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // --- 1. 인증 없이 허용 (permitAll()) ---
                        .requestMatchers("/auth/register").permitAll()
                        .requestMatchers("/auth/login", "/logindashboard", "/login-success").permitAll()
                        .requestMatchers("/auth/token/refresh").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/api/notify/test").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/webjars/**",  "/swagger-ui.html").permitAll()
                        .requestMatchers("/", "/home", "/login", "/denied", "/expired", "/css/**", "/favicon.ico").permitAll() // 관리자 페이지 관련
                        // --- 2. 인증 필요 (authenticated()) ---
                        .requestMatchers("/auth/logout").authenticated()
                        .requestMatchers("/auth/user/me").authenticated()
                        .requestMatchers("/api/farms/**").authenticated()
                        .requestMatchers("/api/plants/**").authenticated()
                        .requestMatchers("/api/user-plants/**").authenticated()
                        .requestMatchers("/api/diaries/**").authenticated()
                        .requestMatchers("/api/notify/**").authenticated()
                        .requestMatchers("/api/alarms/**").authenticated()
                        .requestMatchers("/users/profile/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/fcm-token").authenticated()
                        // --- 3. 접근 제한 (hasRole("ADMIN")) ---
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/loginProc")
                        .usernameParameter("id")
                        .passwordParameter("password")
                        .successHandler(adminAuthenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String accept = request.getHeader("Accept");
                            // 웹 브라우저 요청인 경우
                            if (accept != null && accept.contains("text/html")) {
                                response.sendRedirect("/denied");
                                return;
                            }
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다.\"}");
                        })
                )
                .sessionManagement(session-> session
                        .maximumSessions(1)
                        .expiredUrl("/expired")
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