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

package eu.masconsult.bgbanking.banks.procreditbank;

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
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;
import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.banks.BankClient;
import eu.masconsult.bgbanking.banks.RawBankAccount;
import eu.masconsult.bgbanking.utils.Convert;
import eu.masconsult.bgbanking.utils.CookieRequestInterceptor;

public class ProcreditClient implements BankClient {

    /** The tag used to log to adb console. */
    private static final String TAG = BankingApplication.TAG + "ProcreditClient";
    /** POST parameter name for the user's account name */
    private static final String PARAM_USERNAME = "uName";
    /** POST parameter name for the user's password */
    private static final String PARAM_PASSWORD = "uPSW";
    /** POST parameter name for the selected language */
    private static final String PARAM_LANGUAGE = "lng";
    /** POST parameter value for the selected language */
    private static final String PARAM_LANGUAGE_BG = "";
    /** POST parameter name for forcing new session */
    private static final String PARAM_FORCE = "reconect";
    /** POST parameter value for forcing new session */
    private static final String PARAM_FORCE_YES = "Yes";
    /** Timeout (in ms) we specify for each http request */
    private static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;
    /** Domain for ProB@nking website */
    private static final String DOMAIN = "probanking.procreditbank.bg";
    /** Base URL for ProB@nking website */
    private static final String BASE_URL = "https://" + DOMAIN;
    /** URI for authentication service */
    private static final String AUTH_URI = BASE_URL + "/main/entry.asp";
    /** URI for sync service */
    private static final String GET_BANK_ACCOUNTS_URI = BASE_URL + "/ref/accList.asp";
    /** HEADER for location redirect */
    private static final String HEADER_LOCATION = "Location";
    /** HEADER value for location redirect */
    private static final String LOCATION_BAD_AUTH = "/?bad=bad";
    /** HEADER value for location redirect */
    private static final String LOCATION_AUTH_OK = "/";

    /**
     * Configures the httpClient to connect to the URL provided.
     * 
     * @param authToken
     */
    private static DefaultHttpClient getHttpClient(final String authToken) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        final HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        HttpClientParams.setRedirecting(params, false);
        httpClient.addRequestInterceptor(new CookieRequestInterceptor(Arrays.asList(authToken)));
        return httpClient;
    }

    @Override
    public String authenticate(String username, String password) throws ParseException, IOException {
        final HttpResponse resp;
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, username));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
        params.add(new BasicNameValuePair(PARAM_LANGUAGE, PARAM_LANGUAGE_BG));
        params.add(new BasicNameValuePair(PARAM_FORCE, PARAM_FORCE_YES));
        final HttpEntity entity;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new IllegalStateException(e);
        }
        Log.i(TAG, "Authenticating to: " + AUTH_URI);
        final HttpPost post = new HttpPost(AUTH_URI);
        post.addHeader(entity.getContentType());
        post.setHeader("Accept", "*/*");
        post.setEntity(entity);
        try {
            resp = getHttpClient(null).execute(post);

            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // TODO: validate we are told there is an active session
                Log.d(TAG, EntityUtils.toString(resp.getEntity()));
                throw new IOException(
                        "We received that another session is active, while we explicitly asked to force new session. Something is not right!");
            }
            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
                throw new ParseException("status after auth: "
                        + resp.getStatusLine().getStatusCode() + " "
                        + resp.getStatusLine().getReasonPhrase());
            }
            Header[] locations = resp.getHeaders(HEADER_LOCATION);
            if (locations.length != 1) {
                throw new ParseException(locations.length + " locations received!");
            }

            if (LOCATION_BAD_AUTH.equals(locations[0].getValue())) {
                return null;
            }

            if (!LOCATION_AUTH_OK.equals(locations[0].getValue())) {
                throw new ParseException("location after auth: " + locations[0].getValue());
            }

            String authToken = null;
            for (Header cookie : resp.getHeaders("Set-Cookie")) {
                authToken = cookie.getValue();
            }
            if (authToken != null && authToken.length() > 0) {
                Log.v(TAG, "Successful authentication");
                Log.v(TAG, "response = " + EntityUtils.toString(resp.getEntity()));
                return authToken;
            } else {
                Log.e(TAG, "Error authenticating" + resp.getStatusLine());
                throw new ParseException("can't find session cookie");
            }
        } catch (final IOException e) {
            Log.e(TAG, "IOException when getting authtoken", e);
            throw e;
        } finally {
            Log.v(TAG, "getAuthtoken completing");
        }
    }

    @Override
    public List<RawBankAccount> getBankAccounts(String authtoken) throws IOException,
            ParseException, AuthenticationException {

        DefaultHttpClient httpClient = getHttpClient(authtoken);

        // Create an array that will hold the server-side account
        final ArrayList<RawBankAccount> bankAccounts = new ArrayList<RawBankAccount>();

        // Get the accounts list
        Log.i(TAG, "Getting from: " + GET_BANK_ACCOUNTS_URI);
        final HttpGet get = new HttpGet(GET_BANK_ACCOUNTS_URI);
        get.setHeader("Accept", "*/*");

        Log.v(TAG, "sending " + get.toString());
        final HttpResponse resp = httpClient.execute(get);

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            String response = EntityUtils.toString(resp.getEntity());
            Log.v(TAG, "response = " + response);
            // Our request to the server was successful, now we need to parse
            // the result
            Document doc = Jsoup.parse(response, BASE_URL);

            for (Element row : doc.getElementsByTag("table").get(0).getElementsByTag("tbody")
                    .get(0).getElementsByTag("tr")) {
                RawBankAccount bankAccount = obtainBankAccountFromHtmlTableRow(row);
                if (bankAccount != null) {
                    bankAccounts.add(bankAccount);
                }
            }
        } else if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            // TODO: validate session has expired
            Log.e(TAG, "Authentication exception in getting bank accounts");
            throw new AuthenticationException("session has expired");
        } else {
            throw new ParseException("status after get accounts: "
                    + resp.getStatusLine().getStatusCode() + " "
                    + resp.getStatusLine().getReasonPhrase());
        }

        return bankAccounts;
    }

    private static RawBankAccount obtainBankAccountFromHtmlTableRow(Element row) {
        Elements cells = row.getElementsByTag("td");
        // if this is the header, skip it
        if (cells.size() == 0) {
            return null;
        }

        return new RawBankAccount()
                .setServerId(cells.get(0).text())
                .setName(Convert.formatIBAN(cells.get(0).text()))
                .setIBAN(cells.get(0).text())
                .setCurrency(cells.get(1).text())
                .setBalance(Convert.strToFloat(cells.get(4).text()))
                .setAvailableBalance(Convert.strToFloat(cells.get(5).text()));
    }
}
