package github.nooblong.download.job;

import github.nooblong.download.entity.UploadDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UploadJobParam extends UploadDetail {

}

