package net.adbogm.proxy;

public class ProxyThrowingService {

    public ProxyThrowingService() {
    }

    public void fail() throws ProxyCheckedException {
        throw new ProxyCheckedException("domain failure");
    }
}
