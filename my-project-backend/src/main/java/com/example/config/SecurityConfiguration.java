package com.example.config;

import com.example.Utils.JwtUtils;
import com.example.entity.RestBean;
import com.example.entity.dto.Account;
import com.example.entity.vo.response.AuthorizeVO;
import com.example.filter.JwtAuthorizeFilter;
import com.example.service.AccountService;
import com.fasterxml.jackson.databind.util.BeanUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;
import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Resource
    JwtUtils Utils;
    @Resource
    JwtAuthorizeFilter jwtAuthorizeFilter;
    @Resource
    AccountService service;
     @Bean
     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
         return http
                 //授权（Authorization）: 确定已认证用户是否有权访问特定资源。
                 //认证（Authentication）: 验证用户身份。
                 .authorizeHttpRequests(conf->conf
                         .requestMatchers("/api/auth/**","/error").permitAll()
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
                 //异常处理，authenticationEntryPoint 是用来处理未认证（未登录）的请求。
                 //accessDeniedHandler 是用来处理未授权（无权限访问）的请求。
                 .exceptionHandling(conf->conf
                         .authenticationEntryPoint(this::onUnauthorized)
                         .accessDeniedHandler(this::onAccessDeny))
                 .csrf(AbstractHttpConfigurer::disable)
                 .sessionManagement(conf->conf
                         .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                 .addFilterBefore(jwtAuthorizeFilter, UsernamePasswordAuthenticationFilter.class)

                 .build();
     }
    public void onAccessDeny(HttpServletRequest request,
                             HttpServletResponse response,
                             AccessDeniedException exception) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(RestBean.forbidden(exception.getMessage()).asJsonString());

    }


     public   void onUnauthorized(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException exception) throws IOException, ServletException {
         response.setContentType("application/json;charset=utf-8");
         response.getWriter().write(RestBean.unauthorized(exception.getMessage()).asJsonString());

     }
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
         response.setContentType("application/json;charset=utf-8");
        User user =(User) authentication.getPrincipal();//用于获取UserDetails
        Account account = service.findAccountByNameOrEmail(user.getUsername());
        String token = Utils.createJwt(user,account.getId(),account.getUsername());
        AuthorizeVO vo = new AuthorizeVO();
        vo.setExpire(Utils.expireTime());
//        vo.setRole(account.getRole());
//        vo.setUsername(account.getUsername());
        BeanUtils.copyProperties(account,vo);
        vo.setToken(token);

        response.getWriter().write(RestBean.success(vo).asJsonString());
    }
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
           response.getWriter().write(RestBean.fail(401, exception.getMessage()).asJsonString());
    }
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        String authorization = request.getHeader("Authorization");
        if (Utils.invalidateJwt(authorization)) {
            writer.write(RestBean.success().asJsonString());
        }
        else {
            writer.write(RestBean.fail(400,"退出登录失败").asJsonString());
        }
    }
}
