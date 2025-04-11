package com.example.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.Utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthorizeFilter extends OncePerRequestFilter {
    @Resource
    JwtUtils jwtUtils;



    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
//        // 获取当前请求的路径
//        String path = request.getRequestURI();
//
//        // 跳过 /api/auth/ 路径的 JWT 验证
//        if (path.startsWith("/api/auth/")) {
//            filterChain.doFilter(request, response);
//            return; // 直接放行，不执行后续逻辑
//        }
        String authorization = request.getHeader("Authorization");
        DecodedJWT jwt= jwtUtils.resolveJWT(authorization);
        if (jwt !=null){
            UserDetails user= jwtUtils.toUserDetails(jwt);
            UsernamePasswordAuthenticationToken authentication = new
                    UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("id",jwtUtils.toID(jwt));
        }
        filterChain.doFilter(request, response);
    }
}
