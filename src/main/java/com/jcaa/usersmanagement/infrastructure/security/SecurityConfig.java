package com.jcaa.usersmanagement.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  private static final String AUTH_LOGIN_PATH = "/api/auth/login";
  private static final String USERS_PATH = "/api/users";
  private static final String USERS_DETAIL_PATH = "/api/users/**";
  private static final String[] OPEN_API_PATHS = {
    "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(
      final HttpSecurity http,
      final JwtAuthenticationFilter jwtAuthenticationFilter,
      final RestAuthenticationEntryPoint authenticationEntryPoint,
      final RestAccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(formLogin -> formLogin.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(AUTH_LOGIN_PATH)
                    .permitAll()
                    .requestMatchers(OPEN_API_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, USERS_PATH)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, USERS_PATH, USERS_DETAIL_PATH)
                    .hasAnyRole("ADMIN", "REVIEWER")
                    .requestMatchers(HttpMethod.PUT, USERS_DETAIL_PATH)
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, USERS_DETAIL_PATH)
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
    org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
    configuration.setAllowedOriginPatterns(java.util.List.of("*"));
    configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(java.util.List.of("*"));
    configuration.setAllowCredentials(true);
    org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
