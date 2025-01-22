package github.nooblong.download.service.impl;

import github.nooblong.download.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserVoicelistServiceImplTest extends BaseTest {

    @Test
    void syncUserVoicelist() {
        userVoicelistService.syncUserVoicelist();
    }
}