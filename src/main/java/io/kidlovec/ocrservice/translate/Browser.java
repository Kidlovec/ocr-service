package io.kidlovec.ocrservice.translate;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * @author kidlovec
 * @date 2020-03-13
 * @since 1.0.0
 */
public class Browser {
    public Proxy proxy;
    public String url;

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public void setProxy(String ip, Integer port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port.intValue()));
    }

    public String executeGet()
            throws Exception {
        String result;
        if (this.proxy != null)
            result = HttpClientUtil.doGetWithProxy(this.url, this.proxy);
        else {
            result = HttpClientUtil.doGet(this.url);
        }

        return result;
    }
}
