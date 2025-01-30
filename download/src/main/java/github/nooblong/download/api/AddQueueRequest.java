package github.nooblong.download.api;

import github.nooblong.download.entity.UploadDetail;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AddQueueRequest {
    List<UploadDetail> uploadDetails;
}
