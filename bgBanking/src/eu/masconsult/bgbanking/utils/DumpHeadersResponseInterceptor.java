
package eu.masconsult.bgbanking.utils;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

import android.util.Log;
import eu.masconsult.bgbanking.BankingApplication;

public class DumpHeadersResponseInterceptor implements HttpResponseInterceptor {

    private static final String TAG = BankingApplication.TAG + "DumpHeadersRI";

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException,
            IOException {
        for (Header header : response.getAllHeaders()) {
            Log.v(TAG, "< " + header);
        }
    }

}
