package github.nooblong.btncm.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import github.nooblong.btncm.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * 登录 token 工具类。
 */
@Component
public class JwtUtil {

    private static final String ISSUER = "NOOBLONG";
    private static final int EXPIRE_HOUR = 10000;
    private static volatile byte[] tokenSecret;

    @Value("${token.secret:change-this-token-secret}")
    public void setTokenSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("tokenSecret不能为空");
        }
        JwtUtil.tokenSecret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public static String generateToken(SysUser user) {
        return JWT.create()
                .setPayload(JWT.ISSUER, ISSUER)
                .setPayload(JWT.EXPIRES_AT, DateUtil.offsetHour(new Date(), EXPIRE_HOUR))
                .setPayload(JWT.JWT_ID, UUID.randomUUID().toString())
                .setPayload("userId", user.getId())
                .setPayload("username", user.getUsername())
                .sign(signer());
    }

    public static String generateTokenByRS256(SysUser user) {
        return generateToken(user);
    }

    public static SysUser verifierToken(String token) throws ValidateException {
        if (!StringUtils.hasText(token)) {
            throw new ValidateException("token为空");
        }
        JWT jwt = JWT.of(token).setSigner(signer());
        if (!jwt.verify()) {
            throw new ValidateException("token签名错误");
        }
        JWTValidator.of(jwt).validateAlgorithm(signer()).validateDate(DateUtil.date());

        Object userId = jwt.getPayload("userId");
        if (userId == null) {
            throw new ValidateException("token缺少用户信息");
        }
        SysUser sysUser = Db.getById(Long.valueOf(userId.toString()), SysUser.class);
        if (sysUser == null) {
            throw new ValidateException("用户不存在");
        }
        return sysUser;
    }

    public static SysUser verifierFromContext() {
        try {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (servletRequestAttributes != null) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                return verifierToken(request.getHeader("Access-Token"));
            }
        } catch (Exception e) {
            throw new ValidateException("验证用户失败");
        }
        throw new ValidateException("验证用户失败");
    }

    private static JWTSigner signer() {
        if (tokenSecret == null) {
            throw new ValidateException("tokenSecret未配置");
        }
        return JWTSignerUtil.hs256(tokenSecret);
    }
}
