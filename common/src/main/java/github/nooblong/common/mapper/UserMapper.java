package github.nooblong.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import github.nooblong.common.entity.SysUser;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<SysUser> {

    @Select("select sum(visit_times) from sys_user where username != 'countNotRegisterUser'")
    Integer sumVisitTime();

}
