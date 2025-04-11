package com.example.Utils;

import ch.qos.logback.core.util.TimeUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.TimeoutUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具类 - 负责 Token 的生成、解析和验证
 * 使用 Auth0 的 java-jwt 库实现 JWT 相关操作
 */
@Component
public class JwtUtils {
    @Resource
    StringRedisTemplate template;

    // 从配置文件中注入 JWT 签名密钥
    @Value("${spring.security.jwt.key}")
    private String key;

    // 从配置文件中注入 JWT 有效期（单位：天）
    @Value("${spring.security.jwt.expire}")
    private int expire;

    /**
     * 使jwt无效，实际上是调用了deleteToken方法，使token加入黑名单中
     * @param headerToken
     * @return
     */
    public   Boolean invalidateJwt(String headerToken) {
        String token = this.convertToken(headerToken);
        if (token == null) {
            return false;
        }
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier verifier = JWT.require(algorithm).build();
        try {
            DecodedJWT jwt = verifier.verify(token);
            String id = jwt.getId();
            return deleteToken(id,jwt.getExpiresAt());
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /**
     * 服务器在验证 token 时，不仅检查签名，还会检查 Redis 中是否存在对应的黑名单记录。如果存在，就认为 token 已失效
     * @param uuid
     * @param time
     * @return
     */
    public  boolean deleteToken(String uuid,Date time) {
        if (this.isInvalidToken(uuid))//删除时判断是否在黑名单中，若在返回false，不在就继续操作，将token加入黑名单中
            return false;
        Date now = new Date();
        long expire = Math.max(time.getTime() - now.getTime(), 0);
        template.opsForValue().set(Const.JWT_BLACK_LIST + uuid, "", expire, TimeUnit.MICROSECONDS);
        return true;
    }
    /**
     * 判断是否在黑名单中
     * @param uuid
     * @return
     */
    public boolean isInvalidToken(String uuid){
        return Boolean.TRUE.equals(template.hasKey(Const.JWT_BLACK_LIST + uuid));
    }

    /**
     * 转换请求头中的 Token（去除 Bearer 前缀）
     * @param headerToken 请求头中的完整 Token（格式：Bearer xxxx）
     * @return 去除前缀后的纯 Token，如果格式不合法返回 null
     */
    public String convertToken(String headerToken) {
        if (headerToken == null || !headerToken.startsWith("Bearer ")) {
            return null;
        }
        return headerToken.substring(7); // 截取 "Bearer " 后的内容
    }

    /**
     * 解析并验证 JWT 有效性
     * @param headerToken 请求头中的完整 Token
     * @return 解析后的 DecodedJWT 对象（验证通过且未过期），失败返回 null
     */
    public DecodedJWT resolveJWT(String headerToken) {
        String token = this.convertToken(headerToken);
        if (token == null) {
            return null;
        }
        // 1. 配置算法和密钥
        Algorithm algorithm = Algorithm.HMAC256(key); // 使用 HMAC256 算法，
        // 2. 创建验证器
        JWTVerifier verifier = JWT.require(algorithm).build();//生成一个具体的 JWT 验证器对象。
        // 3.验证并解析 JWT 令牌，返回解码后的 JWT 对象。
        try {
            DecodedJWT jwt = verifier.verify(token); // 验证签名，验证token是否被修改过
            if(this.isInvalidToken(jwt.getId())){//jwt.getId()对应withJWTId(UUID.randomUUID().toString())
                return null;
            }
            Date expiresAt = jwt.getExpiresAt();//对应 创建时侯.withExpiresAt(expired)
            // 检查 Token 是否已过期
            return new Date().after(expiresAt) ? null : jwt;
        } catch (JWTVerificationException e) {
            // 捕获验证异常：签名无效/格式错误等
            return null;
        }
    }

    /**
     * 将 JWT 解析为 Spring Security 用户对象
     * @param jwt 已解析的 JWT 对象
     * @return UserDetails 用户详情对象（注意：密码字段为占位符）
     */
    public UserDetails toUserDetails(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return User
                .withUsername(claims.get("name").asString()) // 从 claims 获取用户名
                .password("********") // 密码占位符（实际认证不依赖此值）
                .authorities(claims.get("authorities").asArray(String.class)) // 获取权限列表
                .build();
    }

    /**
     * 从 JWT 中提取用户ID
     * @param jwt 已解析的 JWT 对象
     * @return 用户ID（整数类型）
     */
    public Integer toID(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return claims.get("id").asInt();
    }

    /**
     * 生成新的 JWT
     * @param details 用户详情（用于获取权限信息）
     * @param id 用户ID
     * @param username 用户名
     * @return 签名后的 JWT 字符串
     */
    public String createJwt(UserDetails details, int id, String username) {
        Algorithm algorithm = Algorithm.HMAC256(key); // 使用密钥生成算法
        Date expired = this.expireTime();
        /**
         * 获取权限集合：details.getAuthorities() 返回一个 Collection<GrantedAuthority>。
         * 转换为流：.stream() 将权限集合转换为一个流。
         * 映射为字符串：.map(GrantedAuthority::getAuthority) 将流中的每个 GrantedAuthority 对象转换为权限字符串。
         * 收集为列表：.toList() 将流中的权限字符串收集到一个 List<String> 中。
         */
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withClaim("id", id)                // 用户ID
                .withClaim("name", username)        // 用户名
                .withClaim("authorities", details.getAuthorities()  // 权限列表
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .withExpiresAt(expired)             // 过期时间
                .withIssuedAt(new Date())           // 签发时间
                .sign(algorithm);                   // 签名
    }

    /**
     * 计算 JWT 过期时间
     * @return 过期时间对象
     * 注意：当前实现按小时计算（expire*24），需确保配置单位与代码逻辑一致
     */
    public Date expireTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expire * 24);   // 将天数转换为小时数
        return calendar.getTime();
    }
}