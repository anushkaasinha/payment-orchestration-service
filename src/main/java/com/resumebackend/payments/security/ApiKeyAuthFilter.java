package com.resumebackend.payments.security;

import com.resumebackend.payments.repository.MerchantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/** X-ADMIN-KEY -> ROLE_ADMIN, X-API-KEY -> ROLE_MERCHANT. Deliberately NOT a @Component (avoids double registration). */
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private final String adminKey;
    private final MerchantRepository merchants;

    public ApiKeyAuthFilter(String adminKey, MerchantRepository merchants) {
        this.adminKey = adminKey;
        this.merchants = merchants;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String admin = req.getHeader("X-ADMIN-KEY");
        String key = req.getHeader("X-API-KEY");
        if (admin != null && MessageDigest.isEqual(admin.getBytes(StandardCharsets.UTF_8), adminKey.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        } else if (key != null) {
            merchants.findByApiKey(key).ifPresent(m ->
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                            new MerchantPrincipal(m.getId(), m.getName()), null,
                            List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")))));
        }
        chain.doFilter(req, res);
    }
}
