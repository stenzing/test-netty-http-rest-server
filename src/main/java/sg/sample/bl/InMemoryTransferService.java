package sg.sample.bl;

import lombok.Builder;
import lombok.Getter;
import sg.sample.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class InMemoryTransferService implements TransferService {

    final List<AccountingEntry> ledger = new LinkedList<>();

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
        ledger.addAll(newEntries);
        return TopupResponse.builder().txId(request.getTxId()).success(true).build();
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
        if (getCurrentBalance(request.getUserIdFrom()).compareTo(request.getAmount())<0) {
            return TransferResponse.builder().txId(request.getTxId()).success(false).build();
        }
        ledger.addAll(newEntries);
        return TransferResponse.builder().txId(request.getTxId()).success(true).build();
    }

    @Override
    public BalanceResponse process(BalanceRequest request) {
        return BalanceResponse.builder()
                .txId(request.getTxId())
                .amount(getCurrentBalance(request.getUserId()))
                .build();
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
