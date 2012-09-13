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

package eu.masconsult.bgbanking.banks.sgexpress;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.net.UrlQuerySanitizer;
import android.util.Log;

import com.google.gson.Gson;

import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.banks.BankClient;
import eu.masconsult.bgbanking.banks.CaptchaException;
import eu.masconsult.bgbanking.banks.RawBankAccount;
import eu.masconsult.bgbanking.utils.Convert;
import eu.masconsult.bgbanking.utils.CookieQuotesFixerResponseInterceptor;
import eu.masconsult.bgbanking.utils.DumpHeadersRequestInterceptor;
import eu.masconsult.bgbanking.utils.DumpHeadersResponseInterceptor;

public class SGExpressClient implements BankClient {

    /** The tag used to log to adb console. */
    private static final String TAG = BankingApplication.TAG + "SGExpressCl";

    /** Timeout (in ms) we specify for each http request */
    private static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

    /** Domain for DSK Direct website */
    private static final String DOMAIN = "bankonweb.sgeb.bg";
    /** Base URL for DSK Direct website */
    private static final String BASE_URL = "https://" + DOMAIN + "/page/default.aspx";
    private static final String XML_ID_PREFIX = "/en-US/";
    /** URI for authentication service */
    private static final String AUTH_XML_ID = XML_ID_PREFIX + ".login";
    /** URI for retrieving bank account */
    private static final String LIST_ACCOUNTS_XML_ID = XML_ID_PREFIX + "ebanking/02accounts/";
    /** URI for retrieving captcha */
    private static final String CAPTCHA_XML_ID = XML_ID_PREFIX + ".CaptchaImage";

    /** POST parameter name for the user's account name */
    private static final String PARAM_USERNAME = "userName";
    /** POST parameter name for the user's password */
    private static final String PARAM_PASSWORD = "pwd";

    private static final String PARAM_USER_TIME = "userTime";

    private static final String XML_ID = "xml_id";

    private static final String ENCODING = "utf8";

    private static final String PARAM_USER_ID = "user_id";

    private static final String PARAM_SESSION_ID = "session_id";

    /** Name of cookie header */
    static final String COOKIE = "Cookie";

    /** HEADER for location redirect */
    private static final String HEADER_LOCATION = "Location";

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
        HttpClientParams.setRedirecting(params, false);
        HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);
        httpClient.addRequestInterceptor(new DumpHeadersRequestInterceptor());
        httpClient.addResponseInterceptor(new DumpHeadersResponseInterceptor());
        httpClient.addResponseInterceptor(new CookieQuotesFixerResponseInterceptor());
        return httpClient;
    }

    private void initializeCookies(DefaultHttpClient httpClient) {
        BasicClientCookie cookie = new BasicClientCookie("DAISForwardCookie_Check", "NO");
        cookie.setDomain(DOMAIN);
        cookie.setPath("/");
        cookie.setSecure(true);
        httpClient.getCookieStore().addCookie(cookie);
    }

    private void performAuthentication(DefaultHttpClient httpClient, AuthToken authToken)
            throws IOException, AuthenticationException {
        HttpResponse resp;
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, authToken.username));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authToken.password));
        params.add(new BasicNameValuePair(PARAM_USER_TIME, "dummy"));
        final HttpEntity entity;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new IllegalStateException(e);
        }
        String uri = BASE_URL + "?"
                + URLEncodedUtils.format(
                        Arrays.asList(new BasicNameValuePair(XML_ID, AUTH_XML_ID)),
                        ENCODING);
        Log.i(TAG, "Authenticating to: " + uri);

        final HttpPost post = new HttpPost(uri);
        post.addHeader(entity.getContentType());
        post.setHeader("Accept", "*/*");
        post.setEntity(entity);
        resp = httpClient.execute(post);

        // check for bad status
        if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
            throw new ParseException("status after auth: "
                    + resp.getStatusLine().getStatusCode() + " "
                    + resp.getStatusLine().getReasonPhrase());
        }

        // check header redirect
        Header[] locations = resp.getHeaders(HEADER_LOCATION);
        if (locations.length != 1) {
            throw new ParseException(locations.length + " header locations received!");
        }

        String location = "https://" + DOMAIN + locations[0].getValue();
        Log.v(TAG, "location=" + location);

        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(location);
        authToken.userId = sanitizer.getValue(PARAM_USER_ID);
        authToken.sessionId = sanitizer.getValue(PARAM_SESSION_ID);
        String redirectedXmlId = sanitizer.getValue(XML_ID);

        if (authToken.userId == null || authToken.userId.length() == 0
                || authToken.sessionId == null
                || authToken.sessionId.length() == 0
                || AUTH_XML_ID.equalsIgnoreCase(redirectedXmlId)) {
            checkAuthError(sanitizer);
        }
    }

    private void checkAuthError(UrlQuerySanitizer sanitizer) throws AuthenticationException {
        String loginReasonId = sanitizer.getValue("strLoginReason");
        if (loginReasonId == null) {
            throw new ParseException("Unknown reason for bad auth");
        }
        throw new AuthenticationException("Bad auth because of " + loginReasonId);
    }

    private String getUri(String xmlId, String userId, String sessionId) {
        String uri;
        uri = BASE_URL + "?" + URLEncodedUtils.format(
                Arrays.asList(
                        new BasicNameValuePair(XML_ID, xmlId),
                        new BasicNameValuePair(PARAM_USER_ID, userId),
                        new BasicNameValuePair(PARAM_SESSION_ID, sessionId)
                        ), ENCODING);
        return uri;
    }

    private String getPage(DefaultHttpClient httpClient, AuthToken authToken, String xmlId)
            throws ParseException, IOException {
        String uri = getUri(LIST_ACCOUNTS_XML_ID, authToken.userId, authToken.sessionId);

        // Get the accounts list
        Log.i(TAG, "Getting from: " + uri);
        final HttpGet get = new HttpGet(uri);
        get.setHeader("Accept", "*/*");

        HttpResponse resp = httpClient.execute(get);

        if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new ParseException("getPage: unhandled http status "
                    + resp.getStatusLine().getStatusCode() + " "
                    + resp.getStatusLine().getReasonPhrase());
        }

        String response = EntityUtils.toString(resp.getEntity());
        Log.v(TAG, "response = " + response);

        return response;
    }

    private String loadPageWithAuth(DefaultHttpClient httpClient, AuthToken authToken, String xmlId)
            throws IOException, AuthenticationException {
        initializeCookies(httpClient);
        performAuthentication(httpClient, authToken);
        return getPage(httpClient, authToken, xmlId);
    }

    @Override
    public String authenticate(String username, String password) throws IOException,
            ParseException, CaptchaException, AuthenticationException {
        AuthToken authToken = new AuthToken();
        authToken.username = username;
        authToken.password = password;

        loadPageWithAuth(getHttpClient(), authToken, LIST_ACCOUNTS_XML_ID);

        Log.v(TAG, "authToken = " + authToken.toJson());
        return authToken.toJson();
    }

    @Override
    public List<RawBankAccount> getBankAccounts(String authTokenString) throws IOException,
            ParseException, AuthenticationException {
        AuthToken authToken = AuthToken.fromJson(authTokenString);

        String response = loadPageWithAuth(getHttpClient(), authToken, LIST_ACCOUNTS_XML_ID);

        Document doc = Jsoup.parse(response, BASE_URL);

        Element content = doc.getElementById("main");
        if (content == null) {
            throw new ParseException("getBankAccounts: can't find #main");
        }

        Elements tables = content.select("section.result table.data");
        if (tables == null || tables.size() == 0) {
            throw new ParseException("getBankAccounts: can't find table section.result table.data");
        }

        Elements rows = tables.first().getElementsByTag("tr");
        if (rows == null || rows.size() == 0) {
            throw new ParseException("getBankAccounts: first table is empty");
        }

        ArrayList<RawBankAccount> bankAccounts = new ArrayList<RawBankAccount>(rows.size());

        String type = "undef";
        for (Element row : rows) {
            if (row.getElementsByTag("th").size() > 0) {
                // header row
                type = row.child(0).text();
            } else {
                RawBankAccount bankAccount = obtainBankAccountFromHtmlTableRow(type, row);
                if (bankAccount != null) {
                    bankAccounts.add(bankAccount);
                }
            }
        }

        return bankAccounts;
    }

    private RawBankAccount obtainBankAccountFromHtmlTableRow(String type, Element row) {
        if ("detail".equalsIgnoreCase(row.attr("class"))) {
            // detail row
            return null;
        }

        if ("bg0".equalsIgnoreCase(row.attr("class"))) {
            Log.v(TAG, "working row(" + type + "): " + row.html());

            if ("Current Accounts".equalsIgnoreCase(type)) {
                return new RawBankAccount()
                        .setServerId(row.child(2).text())
                        .setName(row.child(0).child(0).text())
                        .setIBAN(row.child(2).text())
                        .setCurrency(row.child(1).text())
                        .setBalance(Convert.strToFloat(row.child(3).text()))
                        .setAvailableBalance(Convert.strToFloat(row.child(4).text()));
            } else if ("Cards".equalsIgnoreCase(type)) {
                // skip cards for now
                return null;
            } else {
                // unknown type
                return null;
            }
        } else {
            return null;
        }
    }

    private static class AuthToken {
        String username;
        String password;
        String userId;
        String sessionId;

        public static AuthToken fromJson(String json) {
            return new Gson().fromJson(json, AuthToken.class);
        }

        public String toJson() {
            return new Gson().toJson(this);
        }
    }
}
