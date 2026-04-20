package github.nooblong.btncm.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class QrResponse implements Serializable {

    private String uniqueKey;
    private String image;

}
