/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liyue2008.rpc.transport.netty;

import com.github.liyue2008.rpc.NettyRpcAccessPoint;
import com.github.liyue2008.rpc.transport.InFlightRequests;
import com.github.liyue2008.rpc.transport.ResponseFuture;
import com.github.liyue2008.rpc.transport.Transport;
import com.github.liyue2008.rpc.transport.command.Command;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

/**
 * @author LiYue
 * Date: 2019/9/20
 */
public class NettyTransport implements Transport {
    private final Channel channel;

    private final InFlightRequests inFlightRequests;

    private static final Logger log = NettyRpcAccessPoint.log;

    NettyTransport(Channel channel, InFlightRequests inFlightRequests) {
        this.channel          = channel;
        this.inFlightRequests = inFlightRequests;
    }

    @Override
    public CompletableFuture<Command> send(Command request) {
        // 构建返回值,Java8 Future
        CompletableFuture<Command> completableFuture = new CompletableFuture<>();
        try {
            // 将在途请求放到inFlightRequests中，
            inFlightRequests.put(new ResponseFuture(request.getHeader().getRequestId(), completableFuture));
            // 发送命令
            channel.writeAndFlush(request).addListener((ChannelFutureListener) channelFuture -> {
                // 处理发送失败的情况
                if (!channelFuture.isSuccess()) {
                    completableFuture.completeExceptionally(channelFuture.cause());
                    channel.close();
                }
            });
        } catch (Throwable t) {
            // 处理发送异常
            inFlightRequests.remove(request.getHeader().getRequestId());
            completableFuture.completeExceptionally(t);
        }
        return completableFuture;
    }


}
