package github.nooblong.download.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddToMyRequest {
    @NotBlank
    String voiceDetailId;
    @NotBlank
    String voiceListId;
}
