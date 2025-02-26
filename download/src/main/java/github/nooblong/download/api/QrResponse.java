package github.nooblong.download.api;

import lombok.Data;

import java.io.Serializable;

@Data
public class QrResponse implements Serializable {

    private String uniqueKey;
    private String image;

}
