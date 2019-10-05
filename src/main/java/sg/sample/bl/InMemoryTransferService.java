package sg.sample.bl;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sg.sample.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class InMemoryTransferService implements TransferService {

    public static final String ERROR_WHILE_EXECUTING_COMMAND = "Error while Executing command: {}";
    final List<AccountingEntry> ledger = new LinkedList<>();

    private final ExecutorService ex = Executors.newSingleThreadExecutor();

    @Override
    public TopupResponse process(TopupRequest request) {
        List<AccountingEntry> newEntries = List.of(
                AccountingEntry.builder()
                        .txId(request.getTxId())
                        .timestamp(request.getTransferTs())
                        .accountId(request.getUserId())
                        .amount(request.getAmount())
                        .build()
        );
        Future<TopupResponse> result = ex.submit(() -> {
            boolean b = ledger.addAll(newEntries);
            return TopupResponse.builder().txId(request.getTxId()).success(b).build();
        });
        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(ERROR_WHILE_EXECUTING_COMMAND, e.getMessage());
            return TopupResponse.builder().txId(request.getTxId()).success(false).build();
        }
    }

    @Override
    public TransferResponse process(TransferRequest request) {

        List<AccountingEntry> newEntries = List.of(
                AccountingEntry.builder()
                        .txId(request.getTxId())
                        .timestamp(request.getTransferTs())
                        .accountId(request.getUserIdFrom())
                        .amount(request.getAmount().negate())
                        .build(),
                AccountingEntry.builder()
                        .txId(request.getTxId())
                        .timestamp(request.getTransferTs())
                        .accountId(request.getUserIdTo())
                        .amount(request.getAmount())
                        .build()
        );
        Future<TransferResponse> result = ex.submit(() -> {
            if (getCurrentBalance(request.getUserIdFrom()).compareTo(request.getAmount()) < 0) {
                return TransferResponse.builder().txId(request.getTxId()).success(false).build();
            }
            boolean b = ledger.addAll(newEntries);
            return TransferResponse.builder().txId(request.getTxId()).success(b).build();
        });
        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(ERROR_WHILE_EXECUTING_COMMAND, e.getMessage());
            return TransferResponse.builder().txId(request.getTxId()).success(false).build();
        }
    }

    @Override
    public BalanceResponse process(BalanceRequest request) {
        Future<BalanceResponse> result = ex.submit(BalanceResponse.builder()
                .txId(request.getTxId())
                .amount(getCurrentBalance(request.getUserId()))::build);

        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(ERROR_WHILE_EXECUTING_COMMAND, e.getMessage());
            return BalanceResponse.builder().txId(request.getTxId()).build();
        }
    }

    private BigDecimal getCurrentBalance(String userId) {
        return BigDecimal.valueOf(ledger
                .stream()
                .filter(item -> item.getAccountId().equals(userId))
                .map(AccountingEntry::getAmount)
                .mapToDouble(BigDecimal::doubleValue)
                .sum());
    }

    @Builder
    @Getter
    static class AccountingEntry {
        private String txId;
        private String accountId;
        private LocalDateTime timestamp;
        private BigDecimal amount;
    }
}
