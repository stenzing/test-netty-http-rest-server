package sg.sample.bl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sg.sample.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class InMemoryTransferServiceTest {

    private static final String USER_A = "userA";
    private static final String USER_B = "userB";

    private InMemoryTransferService target = new InMemoryTransferService();

    @BeforeEach
    void initAccounts() {
        target.ledger.clear();
    }

    @Test
    void testTransferRequest() {

        target.ledger.add(InMemoryTransferService.AccountingEntry.builder()
                .timestamp(LocalDateTime.now())
                .accountId(USER_A)
                .amount(BigDecimal.TEN)
                .build());

        TransferRequest request = TransferRequest.builder()
                .amount(BigDecimal.TEN)
                .txId("SomeId")
                .userIdFrom(USER_A)
                .userIdTo(USER_B)
                .transferTs(LocalDateTime.now()).build();
        TransferResponse result = target.process(request);
        assertNotNull(result);
        assertEquals(request.getTxId(), result.getTxId());
        assertTrue(result.isSuccess());
    }

    @Test
    void testBalanceRequest() {

        target.ledger.add(InMemoryTransferService.AccountingEntry.builder()
                .timestamp(LocalDateTime.now())
                .accountId(USER_A)
                .amount(BigDecimal.TEN)
                .build());

        BalanceRequest request = BalanceRequest.builder()
                .txId("SomeId")
                .userId(USER_A)
                .build();
        BalanceResponse result = target.process(request);
        assertNotNull(result);
        assertEquals(request.getTxId(), result.getTxId());
        assertTrue(BigDecimal.TEN.subtract(result.getAmount()).abs().doubleValue() < 0.0001d);
    }


    @Test
    void testTopup() {

        TopupRequest request = TopupRequest.builder()
                .txId("SomeId")
                .userId(USER_A)
                .amount(BigDecimal.ONE)
                .build();
        TopupResponse result = target.process(request);
        assertNotNull(result);
        assertEquals(request.getTxId(), result.getTxId());
        InMemoryTransferService.AccountingEntry newEntry = target.ledger.stream()
                .filter(e -> e.getAccountId().equals(USER_A))
                .findFirst()
                .orElseThrow();

        assertTrue(BigDecimal.ONE.subtract(newEntry.getAmount()).abs().doubleValue() < 0.0001d);
    }

    @Test
    void testTransferRequestFailBecauseOfLowBalance() {

        target.ledger.add(InMemoryTransferService.AccountingEntry.builder()
                .timestamp(LocalDateTime.now())
                .accountId(USER_A)
                .amount(BigDecimal.ONE)
                .build());

        TransferRequest request = TransferRequest.builder()
                .amount(BigDecimal.TEN)
                .txId("SomeId")
                .userIdFrom(USER_A)
                .userIdTo(USER_B)
                .transferTs(LocalDateTime.now()).build();
        TransferResponse result = target.process(request);
        assertNotNull(result);
        assertEquals(request.getTxId(), result.getTxId());
        assertFalse(result.isSuccess());
    }


    @Test
    void testParallelCalls() {

        target.process(TopupRequest.builder()
                .userId("userA")
                .amount(BigDecimal.valueOf(90))
                .build());

        IntStream.range(1, 20)
                .mapToObj(i -> TransferRequest.builder()
                        .txId(UUID.randomUUID().toString())
                        .transferTs(LocalDateTime.now())
                        .userIdFrom("userA")
                        .userIdTo("userB")
                        .amount(BigDecimal.TEN)
                        .build())
                .parallel()
                .forEach(request -> {
                    target.process(request);
                });

        assertEquals(19, target.ledger.size());
        assertTrue(target.process(
                BalanceRequest.builder().userId("userA").build())
                .getAmount().doubleValue() < 0.0001d
        );
    }

}
