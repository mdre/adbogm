package net.adbogm.proxy;

import asm.proxy.EasyProxy;
import asm.proxy.IEasyProxyInterceptor;
import com.arcadedb.database.MutableDocument;
import com.arcadedb.database.RID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import org.junit.Test;

public class ObjectProxyInvocationTest {

    @Test
    public void preservesDeclaredCheckedExceptionThroughEasyProxy() throws Exception {
        MutableDocument baseElement = new StubMutableDocument();

        ObjectProxy interceptor = new ObjectProxy(
                ProxyThrowingService.class, baseElement, null);
        Class<ProxyThrowingService> proxyClass = new EasyProxy().getProxyClass(
                ProxyThrowingService.class, IObjectProxy.class);
        ProxyThrowingService proxy = proxyClass
                .getConstructor(IEasyProxyInterceptor.class)
                .newInstance(interceptor);

        ProxyCheckedException thrown = assertThrows(
                ProxyCheckedException.class,
                proxy::fail);

        assertEquals("domain failure", thrown.getMessage());
    }

    private static class StubMutableDocument extends MutableDocument {

        StubMutableDocument() {
            super(null, null, new RID(1, 1));
        }
    }
}
