package github.nooblong.download.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

@TableName(value ="user_voicelist")
@Data
public class UserVoicelist implements Serializable {
    private Long id;

    private Long userId;

    private Long voicelistId;

    private String voicelistImage;

    private String voicelistName;

    private static final long serialVersionUID = 1L;
}