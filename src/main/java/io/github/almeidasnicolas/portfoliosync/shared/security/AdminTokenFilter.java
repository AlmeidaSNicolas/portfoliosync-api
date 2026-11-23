package io.github.almeidasnicolas.portfoliosync.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class AdminTokenFilter extends OncePerRequestFilter {

    private static final String ADMIN_HEADER = "X-Admin-Token";
    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final byte[] adminToken;

    public AdminTokenFilter(@Value("${admin.token.secret}") String adminToken) {
        this.adminToken = adminToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(ADMIN_HEADER);

        if (header != null && MessageDigest.isEqual(header.getBytes(StandardCharsets.UTF_8), adminToken)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "admin",
                    null,
                    List.of(new SimpleGrantedAuthority(ADMIN_AUTHORITY)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
