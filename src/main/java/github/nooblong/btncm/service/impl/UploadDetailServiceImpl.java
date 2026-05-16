package github.nooblong.btncm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.btncm.enums.MusicStatusEnum;
import github.nooblong.btncm.enums.UploadStatusTypeEnum;
import github.nooblong.btncm.entity.UploadDetail;
import github.nooblong.btncm.mapper.UploadDetailMapper;
import github.nooblong.btncm.netmusic.NetMusicClient;
import github.nooblong.btncm.service.UploadDetailService;
import github.nooblong.btncm.utils.Constant;
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
                log.error("声音:{}获取状态失败:", item.getTitle(), e);
                item.setMusicRetryTimes(item.getMusicRetryTimes() + 1);
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
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
    public UploadDetail getToUploadWithCookie() {
        return getBaseMapper().getToUploadWithCookie();
    }

    private String getAuditStatus(String voiceId, Long userId) {
        Map<String, Object> param = new HashMap<>();
        param.put("id", voiceId);
        JsonNode voiceDetail = netMusicClient.getMusicDataByUserId(param, "voiceDetail", userId);
        if (voiceDetail.get("code").asInt() == 301) {
            return "NO_COOKIE";
        }
        String result;
        result = voiceDetail.get("data").get("displayStatus").asText();
        return result;
    }

    @Override
    public Long getTodayUploadNum() {
        return getBaseMapper().getTodayUploadNum();
    }

    @Override
    public Long getTotalUploadNum() {
        return getBaseMapper().getTotalUploadNum();
    }

    @Override
    public Long getTodayUploadSuccessNum() {
        return getBaseMapper().getTodayUploadSuccessNum();
    }

    @Override
    public Long getTodayUploadUserNum() {
        return getBaseMapper().getTodayUploadUserNum();
    }

    @Override
    public Long getTodayHasNewUploadSubscribe() {
        return getBaseMapper().getTodayHasNewUploadSubscribe();
    }

    @Override
    public Long getEnabledSubscribeNum() {
        return getBaseMapper().getEnabledSubscribeNum();
    }

    @Override
    public Long getEnabledSubscribeUserNum() {
        return getBaseMapper().getEnabledSubscribeUserNum();
    }
}
