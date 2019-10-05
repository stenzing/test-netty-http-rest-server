package sg.sample.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@Getter
@ToString
public class TransferResponse implements APIResponse{
    private final String txId;
    private final boolean success;
}
