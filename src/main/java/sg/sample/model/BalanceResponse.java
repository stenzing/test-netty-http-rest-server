package sg.sample.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@Getter
@ToString
public class BalanceResponse implements APIResponse{
    private final String txId;
    private final BigDecimal amount;
}
