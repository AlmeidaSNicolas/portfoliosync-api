package io.github.almeidasnicolas.portfoliosync.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity){

        return httpSecurity.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(authorizeConfig -> {
            authorizeConfig.requestMatchers(HttpMethod.GET, "/api/projects").permitAll();
            authorizeConfig.requestMatchers(HttpMethod.POST, "/webhooks/github").permitAll();
            authorizeConfig.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
            authorizeConfig.requestMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll();
            authorizeConfig.requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll();
            authorizeConfig.requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll();
            authorizeConfig.requestMatchers("/error").permitAll();
            authorizeConfig.requestMatchers("/admin/**").denyAll();//mudar para authenticated quando criar rota
            authorizeConfig.anyRequest().denyAll();
        }).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

}
