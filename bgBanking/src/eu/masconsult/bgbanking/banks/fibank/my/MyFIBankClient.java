
package eu.masconsult.bgbanking.banks.fibank.my;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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

import com.google.gson.Gson;

import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.banks.BankClient;
import eu.masconsult.bgbanking.banks.CaptchaException;
import eu.masconsult.bgbanking.banks.RawBankAccount;
import eu.masconsult.bgbanking.utils.Convert;
import eu.masconsult.bgbanking.utils.DumpHeadersRequestInterceptor;
import eu.masconsult.bgbanking.utils.DumpHeadersResponseInterceptor;

public class MyFIBankClient implements BankClient {

    private static final String TAG = BankingApplication.TAG + "MyFIB";

    /** Timeout (in ms) we specify for each http request */
    private static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

    /** Domain for DSK Direct website */
    private static final String DOMAIN = "my.fibank.bg";
    /** Base URL for DSK Direct website */
    private static final String BASE_URL = "https://" + DOMAIN;
    /** Page to obtain the ASP cookie from */
    private static final String OBTAIN_COOKIE_URL = BASE_URL + "/Default";
    /** URL to login */
    private static final String LOGIN_URL = BASE_URL + "/Logon";
    private static final String SUMMARY_URL = BASE_URL + "/lAccSummary";

    /** POST parameter name for the user's account name */
    private static final String PARAM_USERNAME = "fldUserId";
    /** POST parameter name for the user's password */
    private static final String PARAM_PASSWORD = "fldPassword";

    private static final String PARAM_LOGINTYPE = "LoginType";
    private static final String PARAM_LOGINTYPE_VALUE_LOGIN = "Login";

    /**
     * Configures the httpClient to connect to the URL provided.
     */
    private static DefaultHttpClient getHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        final HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        // HttpClientParams.setRedirecting(params, false);
        httpClient.addRequestInterceptor(new DumpHeadersRequestInterceptor());
        httpClient.addResponseInterceptor(new DumpHeadersResponseInterceptor());
        return httpClient;
    }

    @Override
    public String authenticate(String username, String password) throws IOException,
            ParseException, CaptchaException, AuthenticationException {
        Log.v(TAG, "authenticate: " + username + "/" + password);
        AuthToken authToken = new AuthToken();
        authToken.username = username;
        authToken.password = password;

        performLogin(getHttpClient(), authToken);

        return authToken.toJson();
    }

    @Override
    public List<RawBankAccount> getBankAccounts(String authtoken) throws IOException,
            ParseException, AuthenticationException {
        Log.v(TAG, "getBankAccounts: " + authtoken);
        AuthToken authToken = AuthToken.fromJson(authtoken);
        DefaultHttpClient httpClient = getHttpClient();

        performLogin(httpClient, authToken);

        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("request_type", "open"));
        params.add(new BasicNameValuePair("open_tab", "home"));
        params.add(new BasicNameValuePair("LogSesID", authToken.sessionId));
        final HttpEntity entity;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new IllegalStateException(e);
        }

        HttpPost post = new HttpPost(SUMMARY_URL);
        post.addHeader(entity.getContentType());
        post.setHeader("Accept", "*/*");
        post.setEntity(entity);
        /*
         * curl -b 'ASP.NET_SessionId=afmrm5b0eiesmhha14ml2xml' -d
         * request_type=open -d open_tab=home -d
         * LogSesID=80e46fac-e188-4055-93de-137bac9db9a3
         * https://my.fibank.bg/lAccSummary
         */

        HttpResponse resp = httpClient.execute(post);

        if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new ParseException("getBankAccounts: unhandled http status "
                    + resp.getStatusLine().getStatusCode() + " "
                    + resp.getStatusLine().getReasonPhrase());
        }

        String response = EntityUtils.toString(resp.getEntity());
        Log.v(TAG, "response = " + response);

        Document doc = Jsoup.parse(response, BASE_URL);

        Element table = doc.getElementById("AvailableAmt");
        if (table == null) {
            throw new ParseException("can't find @AvailableAmt");
        }

        List<RawBankAccount> bankAccounts = new LinkedList<RawBankAccount>();
        for (Element row : table.getElementsByTag("tr")) {
            RawBankAccount bankAccount = obtainBankAccountFromHtmlTableRow(row);
            if (bankAccount != null) {
                bankAccounts.add(bankAccount);
            }
        }

        return bankAccounts;
    }

    private RawBankAccount obtainBankAccountFromHtmlTableRow(Element row) {
        Elements cells = row.getElementsByTag("td");
        if (cells == null || cells.size() == 0) {
            return null;
        }

        if (cells.size() == 1) {
            return null;
        }

        String iban = extractIBAN(cells.get(0).text());
        return new RawBankAccount()
                .setServerId(iban)
                .setIBAN(iban)
                .setName(iban)
                .setCurrency(cells.get(1).text())
                .setAvailableBalance(Convert.strToFloat(cells.get(2).text()))
                .setBalance(Convert.strToFloat(cells.get(2).text()));
    }

    private String extractIBAN(String string) {
        Pattern pattern = Pattern.compile("IBAN:([\\w\\d]+)");
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void performLogin(DefaultHttpClient httpClient, AuthToken authToken)
            throws ClientProtocolException, IOException, AuthenticationException {
        obtainAspSessionCookie(httpClient);

        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, authToken.username));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authToken.password));
        params.add(new BasicNameValuePair(PARAM_LOGINTYPE, PARAM_LOGINTYPE_VALUE_LOGIN));
        final HttpEntity entity;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new IllegalStateException(e);
        }

        HttpPost post = new HttpPost(LOGIN_URL);
        post.addHeader(entity.getContentType());
        post.setHeader("Accept", "*/*");
        post.setEntity(entity);

        HttpResponse resp = httpClient.execute(post);
        if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new AuthenticationException("Invalid credentials!");
        }

        String response = EntityUtils.toString(resp.getEntity());

        Document doc = Jsoup.parse(response, BASE_URL);

        Element selectora = doc.getElementById("selectora");
        if (selectora == null) {
            throw new ParseException("can't find #selectora");
        }
        Element form = selectora.getElementById("formPageSelector");
        if (form == null) {
            throw new ParseException("can't find #formPageSelector");
        }
        Element input = form.getElementById("LogSesID");
        if (input == null) {
            throw new ParseException("can't find #LogSesID");
        }

        authToken.sessionId = input.attr("value");
    }

    private void obtainAspSessionCookie(DefaultHttpClient httpClient)
            throws ClientProtocolException, IOException {
        HttpGet get = new HttpGet(OBTAIN_COOKIE_URL);

        HttpResponse response = httpClient.execute(get);

        response.getEntity().consumeContent();
    }

    private static class AuthToken {
        String username;
        String password;
        String sessionId;

        public static AuthToken fromJson(String json) {
            return new Gson().fromJson(json, AuthToken.class);
        }

        public String toJson() {
            return new Gson().toJson(this);
        }
    }
}
