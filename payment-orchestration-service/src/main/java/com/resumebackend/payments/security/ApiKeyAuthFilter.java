package com.resumebackend.payments.security;

import com.resumebackend.payments.domain.Merchant;
import com.resumebackend.payments.repository.MerchantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final MerchantRepository merchantRepository;

    @Value("${security.admin-key}")
    private String adminKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String apiKey = request.getHeader("X-API-KEY");
        String adminHeader = request.getHeader("X-ADMIN-KEY");

        if (path.startsWith("/api/v1/admin") && adminKey.equals(adminHeader)) {
            setAuth(new AuthPrincipal(null, "ADMIN"), List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), request);
        } else if (path.startsWith("/api/v1/merchant") && apiKey != null && !apiKey.isBlank()) {
            Optional<Merchant> merchant = merchantRepository.findByApiKey(apiKey);
            merchant.ifPresent(value -> setAuth(
                    new AuthPrincipal(value.getId(), "MERCHANT"),
                    List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")),
                    request));
        }

        filterChain.doFilter(request, response);
    }

    private void setAuth(AuthPrincipal principal, List<SimpleGrantedAuthority> authorities, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
