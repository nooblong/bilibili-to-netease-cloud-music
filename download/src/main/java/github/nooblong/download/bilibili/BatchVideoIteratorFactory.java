package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;

import java.util.Iterator;
import java.util.Map;

public interface BatchVideoIteratorFactory {

    Iterator<SimpleVideoInfo> createUpIterator(String upId, String keyWord, int limitSec, boolean checkPart, VideoOrder videoOrder, UserVideoOrder userVideoOrder, Map<String, String> bilibiliCookie);

    Iterator<SimpleVideoInfo> createPartIterator(String bvid, VideoOrder videoOrder, int limitSec, Map<String, String> bilibiliCookie);

    Iterator<SimpleVideoInfo> createFavoriteIterator(String favoriteId, VideoOrder videoOrder, int limitSec, boolean checkPart, Map<String, String> bilibiliCookie);

    Iterator<SimpleVideoInfo> createCollectionIterator(String collectionId, int limitSec, VideoOrder videoOrder, CollectionVideoOrder collectionVideoOrder, Map<String, String> bilibiliCookie);

    BilibiliFullVideo getFullVideo(String bvid, Map<String, String> bilibiliCookie);
}
