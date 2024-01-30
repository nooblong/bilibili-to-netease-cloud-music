package github.nooblong.download.netmusic;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AesEncryptEnum {

    CBC("CBC"), ECB("ECB");

    private final String type;

}
