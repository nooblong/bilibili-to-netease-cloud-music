package github.nooblong.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.mapper.UserMapper;
import github.nooblong.common.service.IUserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements IUserService {
}
