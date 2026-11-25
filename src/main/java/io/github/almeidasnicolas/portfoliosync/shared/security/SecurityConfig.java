package io.github.almeidasnicolas.portfoliosync.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,AdminTokenFilter adminTokenFilter){

        return httpSecurity.csrf(AbstractHttpConfigurer::disable ).addFilterBefore(adminTokenFilter, UsernamePasswordAuthenticationFilter.class).authorizeHttpRequests(authorizeConfig -> {
            authorizeConfig.requestMatchers(HttpMethod.GET, "/api/projects").permitAll();
            authorizeConfig.requestMatchers(HttpMethod.POST, "/webhooks/github").permitAll();
            authorizeConfig.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
            authorizeConfig.requestMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll();
            authorizeConfig.requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll();
            authorizeConfig.requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll();
            authorizeConfig.requestMatchers("/error").permitAll();
            authorizeConfig.requestMatchers("/admin/**").authenticated();
            authorizeConfig.anyRequest().denyAll();
        })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(authenticationEntryPoint -> {
                    HttpStatusEntryPoint httpStatusEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
                    authenticationEntryPoint.authenticationEntryPoint(httpStatusEntryPoint);
                })
                .build();
    }

}
