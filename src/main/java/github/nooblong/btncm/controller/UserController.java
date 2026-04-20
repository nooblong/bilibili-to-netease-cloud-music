package github.nooblong.btncm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import github.nooblong.btncm.entity.SysUser;
import github.nooblong.btncm.entity.Result;
import github.nooblong.btncm.service.IUserService;
import github.nooblong.btncm.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户接口
 */
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
            return Result.fail("token生成失败");
        }
        return Result.ok("登录成功", token);
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody SysUser sysUser) {
        try {
            userService.save(sysUser);
            return Result.ok("注册成功");
        } catch (Exception e) {
            return Result.fail("注册失败，可能存在相同用户名");
        }
    }

    @PostMapping("/refreshToken")
    public Result<String> refreshToken(HttpServletResponse response) {
        try {
            SysUser user = JwtUtil.verifierFromContext();
            String result = JwtUtil.generateTokenByRS256(user);
            response.setHeader("Access-Token", result);
        } catch (Exception e) {
            return Result.fail("刷新失败");
        }
        return Result.ok("刷新成功");
    }

    @GetMapping("/isAuthToken")
    public Result<String> isAuthToken(HttpServletResponse response) {
        try {
            JwtUtil.verifierFromContext();
        } catch (Exception e) {
            return Result.fail("未登录");
        }
        return Result.ok("已登录");
    }

}
