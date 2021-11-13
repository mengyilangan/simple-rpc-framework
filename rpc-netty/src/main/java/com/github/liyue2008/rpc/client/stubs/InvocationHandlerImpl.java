package com.github.liyue2008.rpc.client.stubs;

import com.github.liyue2008.rpc.NettyRpcAccessPoint;
import com.github.liyue2008.rpc.client.RequestIdSupport;
import com.github.liyue2008.rpc.client.ServiceTypes;
import com.github.liyue2008.rpc.serialize.SerializeSupport;
import com.github.liyue2008.rpc.transport.Transport;
import com.github.liyue2008.rpc.transport.command.Code;
import com.github.liyue2008.rpc.transport.command.Command;
import com.github.liyue2008.rpc.transport.command.Header;
import com.github.liyue2008.rpc.transport.command.ResponseHeader;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

/**
 * @author mylg
 * @date 2021-11-12
 */
public class InvocationHandlerImpl implements InvocationHandler {
    private final Transport transport;

    private final Logger log = NettyRpcAccessPoint.log;

    public InvocationHandlerImpl(Transport transport) {
        this.transport = transport;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String classFullName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        byte[] bytes = null;
        if (args.length == 1 && args[0] instanceof String) {
            String arg = (String) args[0];
            bytes = SerializeSupport.serialize(arg);
        }
        RpcRequest rpcRequest = new RpcRequest(classFullName, methodName, bytes);
        return SerializeSupport.parse(invokeRemote(rpcRequest));
    }

    private byte[] invokeRemote(RpcRequest request) {
        log.info("向远端发起请求,request={}", request);
        Header header = new Header(ServiceTypes.TYPE_RPC_REQUEST, 1, RequestIdSupport.next());
        byte[] payload = SerializeSupport.serialize(request);
        Command requestCommand = new Command(header, payload);
        try {
            log.info("最终发送命令是,command={}", requestCommand);
            Command responseCommand = transport.send(requestCommand).get();
            log.info("收到的命令式,command={}", responseCommand);
            ResponseHeader responseHeader = (ResponseHeader) responseCommand.getHeader();
            if (responseHeader.getCode() == Code.SUCCESS.getCode()) {
                return responseCommand.getPayload();
            } else {
                throw new Exception(responseHeader.getError());
            }

        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
