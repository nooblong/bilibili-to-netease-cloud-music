package github.nooblong.btncm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import github.nooblong.btncm.entity.UploadDetail;
import org.apache.ibatis.annotations.Select;

public interface UploadDetailMapper extends BaseMapper<UploadDetail> {

    /**
     * 获取待上传的视频统计
     */
    @Select("""
            select *
            from upload_detail u
            left join sys_user s on s.id = u.user_id
            where u.upload_status = 'WAIT'
            and s.net_cookies != '' and s.net_cookies is not null
            order by u.priority desc , u.create_time limit 1;""")
    UploadDetail getToUploadWithCookie();

    /**
     * 获取今日上传次数
     */
    @Select("""
            select count(*)
            from upload_detail u
            where u.create_time >= CURDATE() and u.create_time <= CURDATE() + INTERVAL 1 DAY
            and (u.upload_status = 'SUCCESS' or u.upload_status !='ERROR');""")
    Long getTodayUploadNum();

    /**
     * 获取总上传数量
     */
    @Select("""
            select count(u.id)
            from upload_detail u;""")
    Long getTotalUploadNum();

    /**
     * 获取今日上传成功数量
     */
    @Select("""
            select count(*)
            from upload_detail u
            where u.create_time >= CURDATE() and u.create_time <= CURDATE() + INTERVAL 1 DAY
            and u.upload_status = 'SUCCESS';""")
    Long getTodayUploadSuccessNum();

    /**
     * 获取今日上传用户数
     */
    @Select("""
            select count(*) from (
            select distinct user_id
            from upload_detail u
            right join sys_user s on u.user_id = s.id
            where u.create_time >= CURDATE() and u.create_time <= CURDATE() + INTERVAL 1 DAY) a;""")
    Long getTodayUploadUserNum();

    /**
     * 获取今日有更新的订阅数
     */
    @Select("""
            select count(*) from (
            select distinct subscribe_id
            from upload_detail u
            right join subscribe s on u.subscribe_id = s.id
            where u.create_time >= CURDATE() and u.create_time <= CURDATE() + INTERVAL 1 DAY) a;""")
    Long getTodayHasNewUploadSubscribe();

    /**
     * 获取总活跃的订阅数
     */
    @Select("""
            select count(*)
            from subscribe s
            where s.enable = 1;""")
    Long getEnabledSubscribeNum();

    /**
     * 获取有活跃的订阅数的用户数
     */
    @Select("""
            select count(*) from (
            select distinct s.user_id
            from subscribe s
            where s.enable = 1) a;""")
    Long getEnabledSubscribeUserNum();

}




