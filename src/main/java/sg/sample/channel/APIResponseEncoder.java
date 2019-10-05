package sg.sample.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sg.sample.model.APIRequest;
import sg.sample.model.APIResponse;

import java.util.List;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Slf4j
@RequiredArgsConstructor
public class APIResponseEncoder extends MessageToMessageEncoder<APIResponse> {

    private final ObjectMapper mapper;
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, APIResponse apiResponse, List<Object> list) throws Exception {
        log.trace("[{}] Encoding response message {}", apiResponse.getTxId(), apiResponse);
        byte[] payload = mapper.writeValueAsBytes(apiResponse);
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(payload));
        response.headers().add("Content-length", payload.length);
        list.add(response);
    }
}
