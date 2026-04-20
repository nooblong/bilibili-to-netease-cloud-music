package github.nooblong.btncm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import github.nooblong.btncm.entity.UploadDetail;
import org.apache.ibatis.annotations.Select;

public interface UploadDetailMapper extends BaseMapper<UploadDetail> {

    /**
     * 获取待上传的视频
     */
    @Select("""
            select *
            from upload_detail u
            left join sys_user s on s.id = u.user_id
            where u.upload_status = 'WAIT'
            and s.net_cookies != '' and s.net_cookies is not null
            order by u.priority desc , u.create_time limit 1;""")
    UploadDetail getToUploadWithCookie();

}




