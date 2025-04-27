package github.nooblong.download.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.MusicStatusEnum;
import github.nooblong.download.UploadStatusTypeEnum;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.mapper.UploadDetailMapper;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.common.util.Constant;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author lyl
 * @description 针对表【upload_detail】的数据库操作Service实现
 * @createDate 2023-09-19 14:46:56
 */
@Service
@Slf4j
public class UploadDetailServiceImpl extends ServiceImpl<UploadDetailMapper, UploadDetail>
        implements UploadDetailService {

    final NetMusicClient netMusicClient;

    public UploadDetailServiceImpl(NetMusicClient netMusicClient) {
        this.netMusicClient = netMusicClient;
    }

    @Override
    public void checkAllAuditStatus() {

        List<UploadDetail> uploadDetailList = lambdaQuery().notIn(
                UploadDetail::getMusicStatus,
                        Arrays.asList(MusicStatusEnum.ONLINE.name(),
                                MusicStatusEnum.ONLY_SELF_SEE.name(),
                                MusicStatusEnum.FAILED.name(),
                                MusicStatusEnum.TRANSCODE_FAILED.name()))
                .eq(UploadDetail::getUploadStatus, UploadStatusTypeEnum.SUCCESS.name())
                .le(UploadDetail::getMusicRetryTimes, Constant.MAX_RETRY_TIMES)
                .gt(UploadDetail::getVoiceId, 0)
                .list();

        String auditStatus;
        for (UploadDetail item : uploadDetailList) {
            try {
                auditStatus = getAuditStatus(String.valueOf(item.getVoiceId()), item.getUserId());
                try {
                    item.setMusicStatus(MusicStatusEnum.valueOf(auditStatus));
                } catch (Exception e) {
                    item.setMusicStatus(MusicStatusEnum.UNKNOWN);
                }
                log.info("声音:{},状态:{}", item.getTitle(), auditStatus);
            } catch (Exception e) {
                log.error("声音:{}获取状态失败:{}", item.getTitle(), e.getMessage());
                item.setMusicRetryTimes(item.getMusicRetryTimes() + 1);
            }
            item.setMusicRetryTimes(item.getMusicRetryTimes() + 1);
            item.setUpdateTime(new Date());
            updateById(item);
        }

    }

    @Override
    public boolean isUnique(@Nonnull String uniqueSourceId, @Nonnull String secondUniqueSourceId, @Nonnull Long voiceListId) {
        // 使用两个uniqueId来判断重复
        LambdaQueryWrapper<UploadDetail> wrapper = Wrappers.lambdaQuery(UploadDetail.class)
                .eq(UploadDetail::getBvid, uniqueSourceId)
                .eq(UploadDetail::getCid, secondUniqueSourceId)
                .eq(UploadDetail::getVoiceListId, voiceListId);
        return count(wrapper) == 0;
    }

    @Override
    public void logNow(Long uploadDetailId, String content) {
        log.info(content);
        UploadDetail uploadDetail = getById(uploadDetailId);
        uploadDetail.setLog(uploadDetail.getLog() +
                DateUtil.now() + " " + content + "\n");
        updateById(uploadDetail);
    }

    private String getAuditStatus(String voiceId, Long userId) {
        Map<String, Object> param = new HashMap<>();
        param.put("id", voiceId);
        JsonNode voicedetail = netMusicClient.getMusicDataByUserId(param, "voicedetail", userId);
        if (voicedetail.get("code").asInt() == 301) {
            return "NO_COOKIE";
        }
        String result;
        result = voicedetail.get("data").get("displayStatus").asText();
        return result;
    }
}




