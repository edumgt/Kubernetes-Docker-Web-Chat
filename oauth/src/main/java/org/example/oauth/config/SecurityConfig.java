package org.example.oauth.config;


import org.example.oauth.service.CustomAuthenticationSuccessHandler;
import org.example.oauth.service.CustomLogoutHandler;
import org.example.oauth.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomLogoutHandler customLogoutHandler;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler, CustomLogoutHandler customLogoutHandler) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.customLogoutHandler = customLogoutHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)throws Exception{
        http
                .csrf(
                        csrf -> csrf.disable()
                )
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/login","/error","/search").permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2Login(
                        oauth -> oauth
                                .loginPage("/login")
                                .userInfoEndpoint(
                                        userInfo -> userInfo
                                                .userService(customOAuth2UserService)
                                )
                                .successHandler(customAuthenticationSuccessHandler)
                )
                .logout(
                        logout -> logout
                                .logoutUrl("/logout")
                                .logoutSuccessUrl("/logout-success")
                                .invalidateHttpSession(true)
                                .deleteCookies("JWT_TOKEN")
                                .addLogoutHandler(customLogoutHandler)
                                .permitAll()
                );
        return http.build();
    }
}
