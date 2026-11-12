package com.resumebackend.payments.config;

import com.resumebackend.payments.repository.MerchantRepository;
import com.resumebackend.payments.security.ApiKeyAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, MerchantRepository merchants,
                                    @Value("${payments.admin-key}") String adminKey) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/app", "/app/**", "/webhook-sink", "/error", "/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/merchant/**").hasRole("MERCHANT")
                .anyRequest().denyAll())
            .addFilterBefore(new ApiKeyAuthFilter(adminKey, merchants), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> json(res, 401, "UNAUTHORIZED", "Missing or invalid API key"))
                .accessDeniedHandler((req, res, ex) -> json(res, 403, "FORBIDDEN", "This key cannot access this endpoint")));
        return http.build();
    }

    private static void json(HttpServletResponse res, int status, String code, String msg) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + msg + "\"}");
    }
}
