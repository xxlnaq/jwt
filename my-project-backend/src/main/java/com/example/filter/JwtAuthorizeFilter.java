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
        //从HTTP请求的Authorization头中提取令牌
        String authorization = request.getHeader("Authorization");
        //jwtUtils.resolveJWT 负责验证令牌签名、检查过期时间，并返回解析后的DecodedJWT对象。
        DecodedJWT jwt= jwtUtils.resolveJWT(authorization);
        if (jwt !=null){
            //将JWT中的用户信息（如用户名、权限）转换为Spring Security的UserDetails对象，这是Spring Security的标准用户凭证
            UserDetails user= jwtUtils.toUserDetails(jwt);
            //创建认证令牌：
            //生成一个已认证的Authentication对象，其中：
            //user：用户身份信息。
            //null：密码（JWT认证无需密码）。
            //user.getAuthorities()：用户的权限列表（如ROLE_ADMIN）。
            UsernamePasswordAuthenticationToken authentication = new
                    UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
            //绑定请求
            //附加请求的IP、Session ID等细节到认证对象。
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            //将认证信息存入SecurityContext，后续的授权（如@PreAuthorize）会直接使用此信息
            SecurityContextHolder.getContext().setAuthentication(authentication);
            //将JWT中的用户ID（如sub字段）存入请求属性，供后续控制器或服务直接使用。
            request.setAttribute("id",jwtUtils.toID(jwt));
        }
        filterChain.doFilter(request, response);
    }
}
