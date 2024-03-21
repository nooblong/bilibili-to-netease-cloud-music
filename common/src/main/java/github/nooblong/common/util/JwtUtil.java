package github.nooblong.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import github.nooblong.common.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.UUID;

public class JwtUtil {

    private static final String ISSUER = "NOOBLONG";
    private static final int EXPIRE_HOUR = 96;

    /*获取签发的token，返回给前端*/
    public static String generateTokenByRS256(SysUser user) throws Exception {
        JWTSigner signer = JWTSignerUtil.createSigner("rs256", SecretKeyUtil.getRSA256Key().getKeyPair());
        SysUser userT = new SysUser();
        userT.setId(user.getId());
        userT.setUsername(user.getUsername());
        return createToken(userT, signer);
    }

    /*签发token*/
    private static String createToken(Object data, JWTSigner signer) {
        return JWT.create()
                .setPayload(JWT.ISSUER, ISSUER)
                .setPayload(JWT.EXPIRES_AT, DateUtil.offsetHour(new Date(), EXPIRE_HOUR))
                .setPayload(JWT.JWT_ID, UUID.randomUUID().toString())
                .setPayload("data", data)
                .setSigner(signer)
                .sign();
    }

    /*验证token*/
    public static SysUser verifierToken(String token) throws ValidateException {
        JWT jwt = JWT.of(token);
        JWTValidator.of(jwt).validateDate(DateUtil.date());
        SysUser sysUser = JSONUtil.toBean(jwt.getPayload("data").toString(), SysUser.class);
        return Db.getById(sysUser.getId(), SysUser.class);
    }

    public static SysUser verifierFromContext() {
        try {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (servletRequestAttributes != null) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                String token = request.getHeader("Access-Token");
                return JwtUtil.verifierToken(token);
            }
        } catch (Exception e) {
            throw new ValidateException("验证用户失败");
        }
        throw new ValidateException("验证用户失败");
    }

}
