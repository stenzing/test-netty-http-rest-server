package sg.sample.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TopupRequest implements APIRequest{
    @Wither
    private String txId;
    @NotBlank
    private String userId;
    @Positive
    @NotNull
    private BigDecimal amount;
    @Wither
    private LocalDateTime transferTs;
}
