package sg.sample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sg.sample.Application.HttpInitializer;
import sg.sample.bl.InMemoryTransferService;
import sg.sample.bl.TransferService;
import sg.sample.channel.CommandGateway;
import sg.sample.model.APIRequest;
import sg.sample.model.BalanceRequest;
import sg.sample.model.TopupRequest;
import sg.sample.model.TransferRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

@Slf4j
class TestApplication {

    private static ObjectMapper mapper = new ObjectMapper();
    private EmbeddedChannel channel;
    private TransferService transferService;

    @BeforeEach
    void initChannel() {
        channel = new EmbeddedChannel();
        transferService = spy(new InMemoryTransferService());
        CommandGateway handler = new CommandGateway(transferService);
        HttpInitializer initializer = new HttpInitializer(mapper, handler);

        initializer.initChannel(channel);
    }

    @Test
    void testTryTransaction() throws JsonProcessingException {

        TransferRequest payload = TransferRequest.builder()
                .userIdFrom("userA")
                .userIdTo("userB")
                .amount(BigDecimal.TEN)
                .build();
        postRequest("/transfer", payload);

        assertEquals(1,channel.outboundMessages().size());

        Mockito.verify(transferService, Mockito.times(1)).process(any(TransferRequest.class));
        Mockito.verifyNoMoreInteractions(transferService);
    }


    @Test
    void testBalance() {
        getRequest("/balance/userA");

        assertEquals(1,channel.outboundMessages().size());

        Mockito.verify(transferService, Mockito.times(1)).process(any(BalanceRequest.class));
        Mockito.verifyNoMoreInteractions(transferService);
    }


    @Test
    void testBalanceInvalidCall() {

        getRequest("/balance/as-d");

        assertEquals(1,channel.outboundMessages().size());
        Mockito.verifyNoMoreInteractions(transferService);
    }


    @Test
    void testTopup() throws JsonProcessingException {

        TopupRequest payload = TopupRequest.builder()
                .userId("userA")
                .amount(BigDecimal.TEN)
                .build();
        postRequest("/topup", payload);

        assertEquals(1, channel.outboundMessages().size());

        Mockito.verify(transferService, Mockito.times(1)).process(any(TopupRequest.class));
        Mockito.verifyNoMoreInteractions(transferService);
    }

    private void postRequest(String path, APIRequest payload) throws JsonProcessingException {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                path,
                Unpooled.wrappedBuffer(mapper.writeValueAsBytes(payload)));
        channel.writeInbound(request);
        channel.flushInbound();
    }

    private void getRequest(String path) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                path);
        channel.writeInbound(request);
        channel.flushInbound();
    }
}
