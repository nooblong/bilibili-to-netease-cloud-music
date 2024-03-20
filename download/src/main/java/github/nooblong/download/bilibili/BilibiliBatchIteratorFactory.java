package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotal;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
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
    public Iterator<BilibiliVideo> createUpIterator(String upId, String keyWord, int limitSec,
                                                    VideoOrder videoOrder, UserVideoOrder userVideoOrder) {
        return new UpIterator(this, upId, keyWord, limitSec, videoOrder, userVideoOrder);
    }

    @Override
    public Iterator<BilibiliVideo> createPartIterator(String bvid, VideoOrder videoOrder, int limitSec) {
        return new PartIterator(this, limitSec, videoOrder, bvid);
    }

    @Override
    public Iterator<BilibiliVideo> createFavoriteIterator(String favoriteId, VideoOrder videoOrder, int limitSec) {
        return new FavoriteIterator(favoriteId, this, limitSec, videoOrder);
    }

    @Override
    public Iterator<BilibiliVideo> createCollectionIterator(String collectionId, int limitSec,
                                                            VideoOrder videoOrder, CollectionVideoOrder collectionVideoOrder) {
        return new CollectionIterator(this, limitSec, videoOrder, collectionId, collectionVideoOrder);
    }

    public IteratorCollectionTotalList<BilibiliVideo> getUpVideoListFromBilibili(String upId, int ps, int pn, UserVideoOrder userVideoOrder, String keyWord) {
        IteratorCollectionTotal collectionTotal = bilibiliClient.getUpVideos(upId, ps, pn, userVideoOrder, keyWord, bilibiliClient.getCurrentCred());
        List<BilibiliVideo> data = new ArrayList<>();
        collectionTotal.getData().forEach(jsonNode -> {
            BilibiliVideo bilibiliVideo = new BilibiliVideo()
                    .setDuration(BilibiliClient.parseStrTime(jsonNode.get("length").asText()))
                    .setBvid(jsonNode.get("bvid").asText())
                    .setCreateTime(jsonNode.get("created").asLong())
                    .setTitle(jsonNode.get("title").asText());
            data.add(bilibiliVideo);
        });
        IteratorCollectionTotalList<BilibiliVideo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(collectionTotal.getTotalNum());
        return result;
    }

    public IteratorCollectionTotalList<BilibiliVideo> getPartVideosFromBilibili(String bvid) {
        BilibiliVideo video = bilibiliClient.createByUrl(bvid);
        bilibiliClient.init(video, bilibiliClient.getCurrentCred());
        List<BilibiliVideo> data = new ArrayList<>();
        video.getPartVideos().forEach(jsonNode -> {
            BilibiliVideo bilibiliVideo = new BilibiliVideo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(bvid)
                    .setCid(jsonNode.get("cid").asText())
                    .setPartName(jsonNode.get("part").asText())
                    .setTitle(jsonNode.get("part").asText())
                    ;
            data.add(bilibiliVideo);
        });
        IteratorCollectionTotalList<BilibiliVideo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(data.size());
        return result;
    }

    public IteratorCollectionTotalList<BilibiliVideo> getCollectionVideoListFromBilibili(String collectionId, int ps, int pn,
                                                                                         CollectionVideoOrder collectionVideoOrder) {
        RetryTemplate template = RetryTemplate.builder()
                .maxAttempts(15)
                .fixedBackoff(1000)
                .retryOn(Exception.class)
                .build();

        IteratorCollectionTotal collectionVideos = null;
        try {
            collectionVideos = template.execute((RetryCallback<IteratorCollectionTotal, Throwable>)
                    context -> bilibiliClient.getCollectionVideos(collectionId, ps, pn, collectionVideoOrder, bilibiliClient.getCurrentCred()));
        } catch (Throwable e) {
            log.error("获取合集失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        List<BilibiliVideo> data = new ArrayList<>();
        collectionVideos.getData().forEach(jsonNode -> {
            BilibiliVideo bilibiliVideo = new BilibiliVideo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(jsonNode.get("bvid").asText())
                    .setCreateTime(jsonNode.get("pubdate").asLong())
                    .setTitle(jsonNode.get("title").asText());
            data.add(bilibiliVideo);
        });
        IteratorCollectionTotalList<BilibiliVideo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(collectionVideos.getTotalNum());
        return result;
    }

    public IteratorCollectionTotalList<BilibiliVideo> getFavoriteVideoListFromBilibili(String favoriteId, int page) {
        IteratorCollectionTotal favoriteVideos = bilibiliClient.getFavoriteVideos(favoriteId, page, bilibiliClient.getCurrentCred());
        List<BilibiliVideo> data = new ArrayList<>();
        favoriteVideos.getData().forEach(jsonNode -> {
            BilibiliVideo bilibiliVideo = new BilibiliVideo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(jsonNode.get("bvid").asText())
                    .setCreateTime(jsonNode.get("fav_time").asLong())
                    .setTitle(jsonNode.get("title").asText());
            data.add(bilibiliVideo);
        });
        IteratorCollectionTotalList<BilibiliVideo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(favoriteVideos.getTotalNum());
        return result;
    }

}
