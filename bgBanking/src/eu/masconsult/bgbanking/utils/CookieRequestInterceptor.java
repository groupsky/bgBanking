
package eu.masconsult.bgbanking.utils;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

public class CookieRequestInterceptor implements HttpRequestInterceptor {
    /** Name of cookie header */
    public static final String COOKIE = "Cookie";

    private final List<String> cookies;

    public CookieRequestInterceptor(List<String> cookies) {
        this.cookies = cookies;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException,
            IOException {
        for (String cookie : cookies) {
            request.addHeader(COOKIE, cookie);
        }
    }
}
