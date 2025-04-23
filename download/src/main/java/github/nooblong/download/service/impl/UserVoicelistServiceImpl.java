package github.nooblong.download.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.download.entity.UserVoicelist;
import github.nooblong.download.mapper.UserVoicelistMapper;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UserVoicelistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UserVoicelistServiceImpl extends ServiceImpl<UserVoicelistMapper, UserVoicelist>
        implements UserVoicelistService {

    private final NetMusicClient netMusicClient;

    public UserVoicelistServiceImpl(NetMusicClient netMusicClient) {
        this.netMusicClient = netMusicClient;
    }

    @CacheEvict(value = "uploadDetail/listVoicelist", allEntries = true)
    @Override
    public void syncUserVoicelist() {
        log.info("开始刷新用户播客列表...");
        List<SysUser> userList = SimpleQuery.list(Wrappers.lambdaQuery(SysUser.class), i -> i);
        for (SysUser user : userList) {
            syncVoicelistByUser(user);
            log.info("{}刷新成功", user.getUsername());
        }
    }

    @Override
    public void syncUserVoicelist(Long userId) {
        SysUser user = Db.getById(userId, SysUser.class);
        syncVoicelistByUser(user);
    }

    private void syncVoicelistByUser(SysUser user) {
        if (StrUtil.isNotBlank(user.getNetCookies())) {
            try {
                JsonNode userVoiceList = netMusicClient.getUserVoiceList(user.getId());
                if (userVoiceList != null && userVoiceList.get("total") != null &&
                        userVoiceList.get("total").asInt() > 0) {
                    List<UserVoicelist> toAdd = new ArrayList<>();
                    ArrayNode list = (ArrayNode) userVoiceList.get("list");
                    for (JsonNode jsonNode : list) {
                        String coverUrl = jsonNode.get("coverUrl").asText();
                        Long voiceListId = jsonNode.get("voiceListId").asLong();
                        String voiceListName = jsonNode.get("voiceListName").asText();
                        UserVoicelist userVoicelist = new UserVoicelist();
                        userVoicelist.setUserId(user.getId());
                        userVoicelist.setVoicelistId(voiceListId);
                        userVoicelist.setVoicelistName(voiceListName);
                        userVoicelist.setVoicelistImage(coverUrl);
                        toAdd.add(userVoicelist);
                    }
                    if (!toAdd.isEmpty()) {
                        Db.remove(Wrappers.lambdaQuery(UserVoicelist.class)
                                .eq(UserVoicelist::getUserId, user.getId()));
                        Db.saveBatch(toAdd);
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.error("用户播客列表查询失败: {}", e.getMessage());
            }
        } else {
            Db.remove(Wrappers.lambdaQuery(UserVoicelist.class)
                    .eq(UserVoicelist::getUserId, user.getId()));
        }
    }
}




