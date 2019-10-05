package sg.sample.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sg.sample.model.BalanceRequest;
import sg.sample.model.TopupRequest;
import sg.sample.model.TransferRequest;

import javax.validation.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.util.CharsetUtil.UTF_8;

@RequiredArgsConstructor
@Slf4j
public class APIRequestDecoder extends MessageToMessageDecoder<FullHttpRequest> {

    private static final Pattern balanceCallUserIdPattern = Pattern.compile("/balance/([a-zA-Z0-9]+)");
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    private final ObjectMapper mapper;

    private static void sendErrorReply(ChannelHandlerContext channelHandlerContext, String msg, HttpResponseStatus badRequest) {
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1,
                badRequest,
                Unpooled.wrappedBuffer(msg.getBytes(UTF_8)));
        response.headers().add("Content-length", msg.length());
        channelHandlerContext.writeAndFlush(response);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, FullHttpRequest completeRequest, List<Object> list) {
        log.trace("Received new request to parse");
        try {

            if (completeRequest.method() == HttpMethod.POST && completeRequest.uri().equals("/transfer")) {
                handleTransferRequest(channelHandlerContext, completeRequest, list);
            } else if (completeRequest.method() == HttpMethod.GET && completeRequest.uri().startsWith("/balance")) {
                handleBalanceRequest(channelHandlerContext, completeRequest, list);
            } else if (completeRequest.method() == HttpMethod.POST && completeRequest.uri().startsWith("/topup")) {
                handleTopupRequest(channelHandlerContext, completeRequest, list);
            } else {
                sendErrorReply(channelHandlerContext,
                        "Command not recognized",
                        HttpResponseStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            sendErrorReply(channelHandlerContext,
                    "Request command could not be parsed: " + ex.getMessage(),
                    HttpResponseStatus.BAD_REQUEST);
        }
    }

    private void handleBalanceRequest(ChannelHandlerContext channelHandlerContext, FullHttpRequest completeRequest, List<Object> list)
            throws IOException {

        Matcher matcher = balanceCallUserIdPattern.matcher(completeRequest.uri());
        if (matcher.matches()) {
            BalanceRequest req = BalanceRequest.builder()
                    .userId(matcher.group(1))
                    .txId(UUID.randomUUID().toString())
                    .build();
            Set<ConstraintViolation<BalanceRequest>> violations = validator.validate(req);
            if (violations.isEmpty()) {
                list.add(req);
            } else {
                throw new ValidationException("Validation failed");
            }
            log.trace("[{}] Balance request parsing successful", req.getTxId());
        } else {
            throw new IOException("Could not find user id in request");
        }
    }

    private void handleTransferRequest(ChannelHandlerContext channelHandlerContext, FullHttpRequest completeRequest, List<Object> list)
            throws IOException {
        TransferRequest req = mapper.readValue(completeRequest.content().toString(UTF_8), TransferRequest.class)
                .withTxId(UUID.randomUUID().toString())
                .withTransferTs(LocalDateTime.now());
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(req);
        if (violations.isEmpty()) {
            list.add(req);
        } else {
            throw new ValidationException("Validation failed");
        }
        log.trace("[{}] Transfer request parsing successful", req.getTxId());
    }


    private void handleTopupRequest(ChannelHandlerContext channelHandlerContext, FullHttpRequest completeRequest, List<Object> list)
            throws IOException {
        TopupRequest req = mapper.readValue(completeRequest.content().toString(UTF_8), TopupRequest.class)
                .withTxId(UUID.randomUUID().toString())
                .withTransferTs(LocalDateTime.now());
        Set<ConstraintViolation<TopupRequest>> violations = validator.validate(req);
        if (violations.isEmpty()) {
            list.add(req);
        } else {
            throw new ValidationException("Validation failed");
        }
        log.trace("[{}] Topup request parsing successful", req.getTxId());

    }
}
