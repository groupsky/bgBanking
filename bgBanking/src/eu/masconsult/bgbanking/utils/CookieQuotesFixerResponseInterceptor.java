
package eu.masconsult.bgbanking.utils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.protocol.HttpContext;

import android.util.Log;
import eu.masconsult.bgbanking.BankingApplication;

/**
 * Handles cookies of the type COOKIE="somevalue";
 */
public class CookieQuotesFixerResponseInterceptor implements HttpResponseInterceptor {

    private static final Pattern pattern = Pattern.compile("([^=]+)=\"([^\";]+)\".*");
    private static final String TAG = BankingApplication.TAG + "CookieQFixRI";

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException,
            IOException {
        CookieStore cookieStore = (CookieStore) context.getAttribute(ClientContext.COOKIE_STORE);
        for (Header header : response.getAllHeaders()) {
            if (!header.getName().equalsIgnoreCase("Set-Cookie")) {
                continue;
            }
            Matcher matcher = pattern.matcher(header.getValue());
            if (!matcher.find()) {
                continue;
            }
            for (Cookie cookie : cookieStore.getCookies()) {
                if (cookie.getName().equalsIgnoreCase(matcher.group(1))) {
                    if (cookie instanceof BasicClientCookie) {
                        ((BasicClientCookie) cookie).setValue('"' + cookie.getValue() + '"');
                    } else if (cookie instanceof BasicClientCookie2) {
                        ((BasicClientCookie2) cookie).setValue('"' + cookie.getValue() + '"');
                    } else {
                        Log.w(TAG, "unhandled cookie implementation " + cookie.getClass().getName());
                    }
                    break;
                }
            }
        }
    }

}
