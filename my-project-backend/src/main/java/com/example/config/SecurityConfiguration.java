package com.example.config;

import com.example.entity.RestBean;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
     @Bean
     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
         return http
                 //认证（Authentication）: 验证用户身份。
                 //授权（Authorization）: 确定已认证用户是否有权访问特定资源。
                 .authorizeHttpRequests(conf->conf
                         .requestMatchers("/api/auth/**").permitAll()
                         .anyRequest().authenticated()
                 )
                 .formLogin(conf->conf
                         .loginProcessingUrl("/api/auth/login")
                         .successHandler(this::onAuthenticationSuccess)
                         .failureHandler(this::onAuthenticationFailure)
                 )
                 .logout(conf->conf
                         .logoutUrl("/api/auth/logout")
                         .logoutSuccessHandler(this::onLogoutSuccess)
                 )
                 .csrf(AbstractHttpConfigurer::disable)
                 .sessionManagement(conf->conf
                         .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                 .build();
     }
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
         response.setContentType("application/json;charset=utf-8");
         response.getWriter().write(RestBean.success().asJsonString());
    }
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
           response.getWriter().write(RestBean.fail(401, exception.getMessage()).asJsonString());
    }
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

    }
}
