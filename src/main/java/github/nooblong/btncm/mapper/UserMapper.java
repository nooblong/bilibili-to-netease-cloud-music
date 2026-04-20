package github.nooblong.btncm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import github.nooblong.btncm.entity.SysUser;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<SysUser> {

    /**
     * 统计信息
     */
    @Select("select sum(visit_times) from sys_user;")
    Integer visitTimes();

    /**
     * 统计信息
     */
    @Select("select sum(visit_today) from sys_user;")
    Integer visitToday();

    /**
     * 统计信息
     */
    @Select("select sum(visit_today_times) from sys_user")
    Integer visitTodayTimes();
}
