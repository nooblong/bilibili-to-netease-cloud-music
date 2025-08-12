package download.service.impl;

import download.BaseTest;
import org.junit.jupiter.api.Test;

class UserVoicelistServiceImplTest extends BaseTest {

    @Test
    void syncUserVoicelist() {
        userVoicelistService.syncUserVoicelist();
    }

    @Test
    void syncUpImage() {
        subscribeService.syncUpNameAndImage();
    }
}