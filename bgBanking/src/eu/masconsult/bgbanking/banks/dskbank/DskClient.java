/*******************************************************************************
 * Copyright (c) 2012 MASConsult Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package eu.masconsult.bgbanking.banks.dskbank;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.net.UrlQuerySanitizer;
import android.util.Log;
import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.banks.BankClient;
import eu.masconsult.bgbanking.banks.RawBankAccount;

public class DskClient implements BankClient {

    /** The tag used to log to adb console. */
    private static final String TAG = BankingApplication.TAG + "DskClient";

    /** Timeout (in ms) we specify for each http request */
    private static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

    /** Domain for DSK Direct website */
    private static final String DOMAIN = "www.dskdirect.bg";
    /** Base URL for DSK Direct website */
    private static final String BASE_URL = "https://" + DOMAIN + "/page/default.aspx";
    private static final String XML_ID_PREFIX = "/bg-BG/";
    /** URI for authentication service */
    private static final String AUTH_XML_ID = XML_ID_PREFIX + ".processlogin";

    /** POST parameter name for the user's account name */
    private static final String PARAM_USERNAME = "userName";
    /** POST parameter name for the user's password */
    private static final String PARAM_PASSWORD = "pwd";

    private static final String XML_ID = "xml_id";

    private static final String ENCODING = "utf8";

    private static final String PARAM_USER_ID = "user_id";

    private static final String PARAM_SESSION_ID = "session_id";

    /**
     * Configures the httpClient to connect to the URL provided.
     * 
     * @param authToken
     */
    private static DefaultHttpClient getHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        final HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        return httpClient;
    }

    @Override
    public String authenticate(String username, String password) throws IOException,
            ParseException {
        final HttpResponse resp;
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, username));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
        final HttpEntity entity;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new IllegalStateException(e);
        }
        String uri = BASE_URL + "?"
                + URLEncodedUtils.format(
                        Arrays.asList(new BasicNameValuePair(XML_ID, AUTH_XML_ID)), ENCODING);
        Log.i(TAG, "Authenticating to: " + uri);
        final HttpPost post = new HttpPost(uri);
        post.addHeader(entity.getContentType());
        post.setHeader("Accept", "*/*");
        post.setEntity(entity);
        try {
            resp = getHttpClient().execute(post);

            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new ParseException("login: unhandled http status "
                        + resp.getStatusLine().getStatusCode() + " "
                        + resp.getStatusLine().getReasonPhrase());
            }

            String response = EntityUtils.toString(resp.getEntity());
            Log.v(TAG, "response = " + response);

            Document doc = Jsoup.parse(response, BASE_URL);
            Element mainForm = doc.getElementById("mainForm");
            if (mainForm == null) {
                throw new ParseException("login: missing mainForm");
            }

            String action = BASE_URL + mainForm.attr("action");
            Log.v(TAG, "action=" + action);
            UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(action);
            String user_id = sanitizer.getValue(PARAM_USER_ID);
            String session_id = sanitizer.getValue(PARAM_SESSION_ID);

            if (user_id == null || "".equals(user_id) || session_id == null
                    || "".equals(session_id)) {
                if (doc.getElementsByClass("redtext").size() > 0) {
                    // bad authentication
                    return null;
                } else {
                    // TODO handle captcha
                    throw new ParseException("no user_id or session_id: " + action);
                }
            }

            return URLEncodedUtils.format(Arrays.asList(
                    new BasicNameValuePair(PARAM_USER_ID, user_id),
                    new BasicNameValuePair(PARAM_SESSION_ID, session_id)),
                    ENCODING);
        } catch (ClientProtocolException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<RawBankAccount> getBankAccounts(String authtoken) throws IOException,
            ParseException, AuthenticationException {
        // TODO Auto-generated method stub
        return new LinkedList<RawBankAccount>();
    }

}
