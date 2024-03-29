package github.nooblong.download.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.StatusTypeEnum;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.mapper.UploadDetailMapper;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.download.utils.Constant;
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

        List<UploadDetail> uploadDetailList = lambdaQuery().notIn(UploadDetail::getStatus,
                        Arrays.asList(StatusTypeEnum.ONLINE.name(),
                                StatusTypeEnum.ONLY_SELF_SEE.name(),
                                StatusTypeEnum.FAILED.name(),
                                StatusTypeEnum.TRANSCODE_FAILED.name()))
                .lt(UploadDetail::getStatus, Constant.MAX_RETRY_TIMES)
                .gt(UploadDetail::getVoiceId, 0)
                .list();

        String auditStatus;
        for (UploadDetail item : uploadDetailList) {
            try {
                auditStatus = getAuditStatus(String.valueOf(item.getVoiceId()), item.getUserId());
                try {
                    item.setStatus(StatusTypeEnum.valueOf(auditStatus));
                } catch (Exception e) {
                    item.setStatus(StatusTypeEnum.UNKNOWN);
                }
            } catch (Exception e) {
                log.error("声音ID:{}获取状态失败:{}", item.getVoiceId(), e.getMessage());
                item.setRetryTimes(item.getRetryTimes() + 1);
            }
            item.setRetryTimes(item.getRetryTimes() + 1);
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
    public boolean hasUploaded(Long userId) {
        Long count = lambdaQuery().eq(UploadDetail::getUserId, userId)
                .eq(UploadDetail::getStatus, StatusTypeEnum.ONLINE.name())
                .count();
        return count > 0;
    }

    @Override
    public void uploadAllOnlySelfSee(Long voiceListId) {
        List<UploadDetail> onlySelfSee = lambdaQuery()
                .eq(UploadDetail::getVoiceListId, voiceListId)
                .and(i -> i.eq(UploadDetail::getStatus, StatusTypeEnum.ONLY_SELF_SEE.name()).or()
                        .eq(UploadDetail::getStatus, StatusTypeEnum.ONLY_SELF_SEE.name()))
                .list();
        List<UploadDetail> toProcessList = new ArrayList<>();
        List<String> duplicateBvid = new ArrayList<>();
//        JsonNode voiceListDetail = netMusicClient.getVoiceListDetail(String.valueOf(voiceListId));
//        System.out.println(voiceListDetail);
        for (UploadDetail uploadDetail : onlySelfSee) {
            if (duplicateBvid.contains(uploadDetail.getBvid()) && StrUtil.isBlank(uploadDetail.getCid())) {
                log.info("跳过重复: {}", uploadDetail.getBvid());
                continue;
            } else {
                duplicateBvid.add(uploadDetail.getBvid());
            }
            log.info("处理: {}, {}", uploadDetail.getUploadName(), uploadDetail.getStatus());
            uploadDetail.setStatus(StatusTypeEnum.WAIT)
                    .setCrack(1L)
                    .setUseVideoCover(1L)
                    .setRetryTimes(0)
                    .setUploadName("");
            toProcessList.add(uploadDetail);
        }
        updateBatchById(onlySelfSee, 100);
        for (UploadDetail uploadDetail : toProcessList) {
//            musicQueue.enQueue(uploadDetail);
        }
    }

    @Override
    public List<UploadDetail> listAllWait() {
        return lambdaQuery().eq(UploadDetail::getStatus, StatusTypeEnum.WAIT.name())
                .le(UploadDetail::getRetryTimes, Constant.MAX_RETRY_TIMES)
                .list();
    }

    private String getAuditStatus(String voiceId, Long userId) {
        Map<String, Object> param = new HashMap<>();
        param.put("id", voiceId);
        JsonNode voicedetail = netMusicClient.getMusicDataByUserId(param, "voicedetail", userId);
        if (voicedetail.get("code").asInt() == 301) {
            return "用户的登录失效了";
        }
        String result;
        result = voicedetail.get("data").get("displayStatus").asText();
        return result;
    }
}




