package github.nooblong.common.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.service.IUserService;
import github.nooblong.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

@RestController
@Slf4j
public class UserController {

    @Autowired
    IUserService userService;

    @PostMapping("/login")
    public Result<String> login(@RequestBody SysUser sysUser) {

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, sysUser.getUsername());
        wrapper.eq(SysUser::getPassword, sysUser.getPassword());
        List<SysUser> userList = userService.list(wrapper);
        if (userList.isEmpty()) {
            return Result.fail("登录失败");
        }

        String token;
        try {
            token = JwtUtil.generateTokenByRS256(userList.get(0));
        } catch (Exception e) {
            log.error("token生成失败", e);
            return Result.fail("内部错误");
        }

        return Result.ok("登录成功", token);
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody SysUser sysUser) {
        userService.save(sysUser);
        return Result.ok("注册成功");
    }

    @PostMapping("/refreshToken")
    public Result<String> refreshToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = request.getHeader("Access-Token");
        SysUser user;
        try {
            user = JwtUtil.verifierToken(token);
        } catch (Exception e) {
            return Result.fail("刷新失败");
        }
        String result = JwtUtil.generateTokenByRS256(user);
        response.setHeader("Access-Token", result);
        return Result.ok("刷新成功");
    }

}
