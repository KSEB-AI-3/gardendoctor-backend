package com.project.farming.global.security;

import com.project.farming.global.jwtToken.JwtAuthenticationFilter;
import com.project.farming.global.oauth.CustomOAuth2UserService;
import com.project.farming.global.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.project.farming.global.oauth.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final AdminAuthenticationSuccessHandler adminAuthenticationSuccessHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .httpBasic(h -> h.disable())
            .csrf(c -> c.disable())

            .authorizeHttpRequests(auth -> auth
                // --- 1. 인증 없이 허용 ---
                .requestMatchers("/auth/register").permitAll()
                .requestMatchers("/auth/login", "/logindashboard", "/login-success").permitAll()
                .requestMatchers("/auth/token/refresh").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/api/notify/test").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/webjars/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/", "/home", "/login", "/denied", "/expired", "/css/**").permitAll()

                // --- 2. 인증 필요 ---
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

                // --- 3. 관리자 ---
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            .oauth2Login(oauth2 -> oauth2
                // ✅ redirect_uri 쿠키 저장소 연결
                .authorizationEndpoint(a -> a.authorizationRequestRepository(cookieOAuth2AuthorizationRequestRepository))
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
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
                    if (accept != null && accept.contains("text/html")) {
                        response.sendRedirect("/denied");
                        return;
                    }
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다.\"}");
                })
            )

            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/expired")
            )

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
