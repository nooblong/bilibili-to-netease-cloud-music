package github.nooblong.download.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.mapper.UploadDetailMapper;
import github.nooblong.download.mq.MessageSender;
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
    final MessageSender messageSender;

    public UploadDetailServiceImpl(NetMusicClient netMusicClient, MessageSender messageSender) {
        this.netMusicClient = netMusicClient;
        this.messageSender = messageSender;
    }

    @Override
    public void checkAllAuditStatus() {

        List<UploadDetail> uploadDetailList = lambdaQuery().notIn(UploadDetail::getDisplayStatus,
                        Arrays.asList("ONLINE", "ONLY_SELF_SEE", "FAILED", "TRANSCODE_FAILED"))
                .lt(UploadDetail::getGetDisplayStatusTimes, Constant.MAX_GET_AUDIT_STATUS_TIMES)
                .gt(UploadDetail::getVoiceId, 0)
                .list();

        String auditStatus;
        for (UploadDetail item : uploadDetailList) {
            try {
                auditStatus = getAuditStatus(String.valueOf(item.getVoiceId()), item.getUserId());
                item.setDisplayStatus(auditStatus);
            } catch (Exception e) {
                log.warn("声音ID:{}获取状态失败:{}", item.getVoiceId(), e.getMessage());
                item.setGetDisplayStatusTimes(item.getGetDisplayStatusTimes() + 1)
                        .setUpdateTime(new Date());
            }
            item.setGetDisplayStatusTimes(item.getGetDisplayStatusTimes() + 1);
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
                .eq(UploadDetail::getDisplayStatus, "ONLINE")
                .count();
        return count > 0;
    }

    @Override
    public void uploadAllOnlySelfSee(Long voiceListId) {
        List<UploadDetail> onlySelfSee = lambdaQuery()
                .eq(UploadDetail::getVoiceListId, voiceListId)
                .and(i -> i.eq(UploadDetail::getDisplayStatus, "ONLY_SELF_SEE").or().eq(UploadDetail::getDisplayStatus, "FINISHED"))
                .list();
        List<Long> toProcessList = new ArrayList<>();
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
            log.info("处理: {}, {}", uploadDetail.getUploadName(), uploadDetail.getDisplayStatus());
            uploadDetail.setStatus("NOT_UPLOAD")
                    .setCrack(1L)
                    .setUseVideoCover(1L)
                    .setGetDisplayStatusTimes(0L)
                    .setUploadName("");
            toProcessList.add(uploadDetail.getId());
        }
        updateBatchById(onlySelfSee, 100);
        for (Long uploadDetailId : toProcessList) {
            messageSender.sendUploadDetailId(uploadDetailId, 1);
        }
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




