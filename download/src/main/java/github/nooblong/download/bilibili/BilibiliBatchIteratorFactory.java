package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotal;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@Slf4j
public class BilibiliBatchIteratorFactory implements BatchVideoIteratorFactory {

    final BilibiliClient bilibiliClient;

    public BilibiliBatchIteratorFactory(BilibiliClient bilibiliClient) {
        this.bilibiliClient = bilibiliClient;
    }

    @Override
    public Iterator<SimpleVideoInfo> createUpIterator(String upId, String keyWord, int limitSec,
                                                      VideoOrder videoOrder, UserVideoOrder userVideoOrder) {
        return new UpIterator(this, upId, keyWord, limitSec, videoOrder, userVideoOrder);
    }

    @Override
    public Iterator<SimpleVideoInfo> createPartIterator(String bvid, VideoOrder videoOrder, int limitSec) {
        return new PartIterator(this, limitSec, videoOrder, bvid);
    }

    @Override
    public Iterator<SimpleVideoInfo> createFavoriteIterator(String favoriteId, VideoOrder videoOrder, int limitSec) {
        return new FavoriteIterator(favoriteId, this, limitSec);
    }

    @Override
    public Iterator<SimpleVideoInfo> createCollectionIterator(String collectionId, int limitSec,
                                                              VideoOrder videoOrder, CollectionVideoOrder collectionVideoOrder) {
        return new CollectionIterator(this, limitSec, videoOrder, collectionId, collectionVideoOrder);
    }

    public IteratorCollectionTotalList<SimpleVideoInfo> getUpVideoListFromBilibili(String upId, int ps, int pn, UserVideoOrder userVideoOrder, String keyWord) {
        IteratorCollectionTotal collectionTotal = bilibiliClient.getUpVideos(upId, ps, pn, userVideoOrder, keyWord, bilibiliClient.getCurrentCred());
        List<SimpleVideoInfo> data = new ArrayList<>();
        collectionTotal.getData().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(BilibiliClient.parseStrTime(jsonNode.get("length").asText()))
                    .setBvid(jsonNode.get("bvid").asText())
                    .setCreateTime(jsonNode.get("created").asLong())
                    .setTitle(jsonNode.get("title").asText());
            data.add(simpleVideoInfo);
        });
        IteratorCollectionTotalList<SimpleVideoInfo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(collectionTotal.getTotalNum());
        return result;
    }

    public IteratorCollectionTotalList<SimpleVideoInfo> getPartVideosFromBilibili(String bvid) {
        SimpleVideoInfo video = bilibiliClient.createByUrl(bvid);
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(video, bilibiliClient.getCurrentCred());
        List<SimpleVideoInfo> data = new ArrayList<>();
        bilibiliFullVideo.getPartVideos().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(bvid)
                    .setCid(jsonNode.get("cid").asText())
                    .setPartName(jsonNode.get("part").asText())
                    .setTitle(jsonNode.get("part").asText());
            data.add(simpleVideoInfo);
        });
        IteratorCollectionTotalList<SimpleVideoInfo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(data.size());
        return result;
    }

    public IteratorCollectionTotalList<SimpleVideoInfo> getCollectionVideoListFromBilibili(String collectionId, int ps, int pn,
                                                                                           CollectionVideoOrder collectionVideoOrder) {
        IteratorCollectionTotal collectionVideos = null;
        try {
            collectionVideos = bilibiliClient.getCollectionVideos(collectionId, ps, pn, collectionVideoOrder, bilibiliClient.getCurrentCred());
        } catch (Throwable e) {
            log.error("获取合集失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        List<SimpleVideoInfo> data = new ArrayList<>();
        collectionVideos.getData().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(jsonNode.get("bvid").asText())
                    .setCreateTime(jsonNode.get("pubdate").asLong())
                    .setTitle(jsonNode.get("title").asText());
            data.add(simpleVideoInfo);
        });
        IteratorCollectionTotalList<SimpleVideoInfo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(collectionVideos.getTotalNum());
        return result;
    }

    public IteratorCollectionTotalList<SimpleVideoInfo> getFavoriteVideoListFromBilibili(String favoriteId, int page) {
        IteratorCollectionTotal favoriteVideos = bilibiliClient.getFavoriteVideos(favoriteId, page, bilibiliClient.getCurrentCred());
        List<SimpleVideoInfo> data = new ArrayList<>();
        favoriteVideos.getData().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(jsonNode.get("bvid").asText())
                    .setCreateTime(jsonNode.get("fav_time").asLong())
                    .setTitle(jsonNode.get("title").asText());
            data.add(simpleVideoInfo);
        });
        IteratorCollectionTotalList<SimpleVideoInfo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(favoriteVideos.getTotalNum());
        return result;
    }

}
