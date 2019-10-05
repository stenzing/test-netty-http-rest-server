package sg.sample;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sg.sample.bl.InMemoryTransferService;
import sg.sample.bl.TransferService;
import sg.sample.channel.APIRequestDecoder;
import sg.sample.channel.APIResponseEncoder;
import sg.sample.channel.CommandGateway;

@Slf4j
public class Application {

    private static ObjectMapper mapper = new ObjectMapper();
    private static TransferService ts = new InMemoryTransferService();
    private static CommandGateway handler = new CommandGateway(ts);

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            new ServerBootstrap()
                    .option(ChannelOption.SO_BACKLOG, 50_000)
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpInitializer(mapper, handler))
                    .bind(8080)
                    .sync()
                    .channel()
                    .closeFuture()
                    .sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @RequiredArgsConstructor
    static class HttpInitializer extends ChannelInitializer<Channel> {

        private final ObjectMapper mapper;
        private final CommandGateway commandGateway;

        @Override
        public void initChannel(Channel ch) {
            ch.pipeline()
                    .addLast(new HttpRequestDecoder())
                    .addLast(new HttpObjectAggregator(1048576))
                    .addLast(new HttpResponseEncoder())
                    .addLast(new APIRequestDecoder(mapper))
                    .addLast(new APIResponseEncoder(mapper))
                    .addLast(commandGateway);
        }
    }

}
