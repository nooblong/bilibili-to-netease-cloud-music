package github.nooblong.download;

import github.nooblong.download.bilibili.BilibiliVideo;
import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;

import java.util.Iterator;

public interface BatchVideoIteratorFactory {

    Iterator<BilibiliVideo> createUpIterator(String upId, String keyWord, int limitSec, VideoOrder videoOrder, UserVideoOrder userVideoOrder);

    Iterator<BilibiliVideo> createPartIterator(String bvid, VideoOrder videoOrder, int limitSec);

    Iterator<BilibiliVideo> createFavoriteIterator(String favoriteId, VideoOrder videoOrder, int limitSec);

    Iterator<BilibiliVideo> createCollectionIterator(String collectionId, int limitSec, VideoOrder videoOrder, CollectionVideoOrder collectionVideoOrder);

}
