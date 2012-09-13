
package eu.masconsult.bgbanking.utils;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import android.util.Log;
import eu.masconsult.bgbanking.BankingApplication;

public class DumpHeadersRequestInterceptor implements HttpRequestInterceptor {

    private static final String TAG = BankingApplication.TAG + "DumpHeadersRI";

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        for (Header header : request.getAllHeaders()) {
            Log.v(TAG, "> " + header);
        }
    }

}
