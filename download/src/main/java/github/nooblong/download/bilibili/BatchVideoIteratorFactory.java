package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;

import java.util.Iterator;

public interface BatchVideoIteratorFactory {

    Iterator<SimpleVideoInfo> createUpIterator(String upId, String keyWord, int limitSec, VideoOrder videoOrder, UserVideoOrder userVideoOrder);

    Iterator<SimpleVideoInfo> createPartIterator(String bvid, VideoOrder videoOrder, int limitSec);

    Iterator<SimpleVideoInfo> createFavoriteIterator(String favoriteId, VideoOrder videoOrder, int limitSec);

    Iterator<SimpleVideoInfo> createCollectionIterator(String collectionId, int limitSec, VideoOrder videoOrder, CollectionVideoOrder collectionVideoOrder);

}
