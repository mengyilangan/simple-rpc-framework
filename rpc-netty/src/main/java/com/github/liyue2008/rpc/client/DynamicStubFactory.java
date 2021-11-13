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
package com.github.liyue2008.rpc.client;

import com.github.liyue2008.rpc.NettyRpcAccessPoint;
import com.github.liyue2008.rpc.client.stubs.InvocationHandlerImpl;
import com.github.liyue2008.rpc.transport.Transport;
import org.slf4j.Logger;

import java.lang.reflect.Proxy;

/**
 * @author LiYue
 * Date: 2019/9/27
 */
public class DynamicStubFactory implements StubFactory {
    private static final Logger log = NettyRpcAccessPoint.log;

    private final static String STUB_SOURCE_TEMPLATE =
        "package com.github.liyue2008.rpc.client.stubs;\n" +
            "import com.github.liyue2008.rpc.serialize.SerializeSupport;\n" +
            "\n" +
            "public class %s extends AbstractStub implements %s {\n" +
            "    @Override\n" +
            "    public String %s(String arg) {\n" +
            "        return SerializeSupport.parse(\n" +
            "                invokeRemote(\n" +
            "                        new RpcRequest(\n" +
            "                                \"%s\",\n" +
            "                                \"%s\",\n" +
            "                                SerializeSupport.serialize(arg)\n" +
            "                        )\n" +
            "                )\n" +
            "        );\n" +
            "    }\n" +
            "}";

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createStub(Transport transport, Class<T> serviceClass) {
        try {
            log.info("客户端发起请求创建stub,serviceClass={}", serviceClass);
            // 填充模板
            return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{serviceClass}, new InvocationHandlerImpl(transport));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
