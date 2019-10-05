package sg.sample.channel;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import sg.sample.bl.TransferService;
import sg.sample.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@ChannelHandler.Sharable
@Slf4j
public class CommandGateway extends ChannelInboundHandlerAdapter {
    private final Map<Class<? extends APIRequest>, Function<APIRequest, APIResponse>> commands = new HashMap<>();

    public CommandGateway(TransferService service) {
        commands.put(TransferRequest.class, apiRequest -> service.process((TransferRequest) apiRequest));
        commands.put(BalanceRequest.class, apiRequest -> service.process((BalanceRequest) apiRequest));
        commands.put(TopupRequest.class, apiRequest -> service.process((TopupRequest) apiRequest));
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof APIRequest) {
            log.debug("[{}] Received request {}", ((APIRequest) msg).getTxId(), msg);
            if (commands.containsKey(msg.getClass())) {
                APIResponse response = commands.get(msg.getClass()).apply((APIRequest) msg);
                log.debug("[{}] Generated response {}", response.getTxId(), response);
                ctx.writeAndFlush(response);
            } else {
                ctx.writeAndFlush(ErrorResponse.builder()
                        .txId(((APIRequest) msg).getTxId())
                        .exception(new UnsupportedOperationException("Command handler not found"))
                        .build());
            }
        } else {
            ctx.writeAndFlush(ErrorResponse.builder()
                    .exception(new UnsupportedOperationException("Incorrect call."))
                    .build());
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.writeAndFlush(ErrorResponse.builder()
                .exception(new RuntimeException(cause))
                .build());
        ctx.close();
    }
}
