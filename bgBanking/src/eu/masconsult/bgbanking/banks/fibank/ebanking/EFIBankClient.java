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

package eu.masconsult.bgbanking.banks.fibank.ebanking;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

import com.google.gson.Gson;

import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.banks.BankClient;
import eu.masconsult.bgbanking.banks.CaptchaException;
import eu.masconsult.bgbanking.banks.RawBankAccount;
import eu.masconsult.bgbanking.utils.Convert;
import eu.masconsult.bgbanking.utils.DumpHeadersRequestInterceptor;
import eu.masconsult.bgbanking.utils.DumpHeadersResponseInterceptor;

public class EFIBankClient implements BankClient {

    private static final String TAG = BankingApplication.TAG + "EFIB";

    /** Timeout (in ms) we specify for each http request */
    private static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

    /** Domain for DSK Direct website */
    private static final String DOMAIN = "e-fibank.bg";
    /** Base URL for DSK Direct website */
    private static final String BASE_URL = "https://" + DOMAIN;
    /** URL to login */
    private static final String LOGIN_URL = BASE_URL + "/EBank/Login";

    private static final String ENQUIRY_URL = BASE_URL + "/EBank/Enquiry";

    /**
     * Configures the httpClient to connect to the URL provided.
     */
    private static DefaultHttpClient getHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        final HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        httpClient.addRequestInterceptor(new DumpHeadersRequestInterceptor());
        httpClient.addResponseInterceptor(new DumpHeadersResponseInterceptor());
        return httpClient;
    }

    @Override
    public String authenticate(String username, String password) throws IOException,
            ParseException, CaptchaException, AuthenticationException {

        AuthToken authToken = new AuthToken();
        authToken.username = username;
        authToken.password = password;

        obtainSession(getHttpClient(), authToken);

        return authToken.toJson();
    }

    private void obtainSession(DefaultHttpClient httpClient, AuthToken authToken)
            throws IOException, AuthenticationException {
        HttpEntity entity;
        try {
            entity = new StringEntity("<?xml version=\"1.0\"?><LOGIN><USER_NAME>"
                    + authToken.username + "</USER_NAME><USER_PASS>" + authToken.password
                    + "</USER_PASS><USER_LANG>BG</USER_LANG><PATH>Login</PATH></LOGIN>", "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        HttpPost post = new HttpPost(LOGIN_URL);
        post.addHeader("Content-Type", "text/xml");
        post.setHeader("Accept", "*/*");
        post.setEntity(entity);

        HttpResponse resp = httpClient.execute(post);
        if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new AuthenticationException("Invalid credentials!");
        }

        String response = EntityUtils.toString(resp.getEntity());

        Log.v(TAG, "response = " + response);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ParseException(e.getMessage());
        }

        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(response));

        Document doc;
        try {
            doc = db.parse(is);
        } catch (SAXException e) {
            throw new ParseException(e.getMessage());
        }

        NodeList session = doc.getElementsByTagName("SESSION");
        if (session == null || session.getLength() != 1) {
            throw new ParseException("can't find SESSION ");
        }

        NamedNodeMap attributes = session.item(0).getAttributes();
        authToken.sessionId = attributes.getNamedItem("id").getTextContent();
    }

    @Override
    public List<RawBankAccount> getBankAccounts(String authtoken) throws IOException,
            ParseException, AuthenticationException {
        AuthToken authToken = AuthToken.fromJson(authtoken);
        DefaultHttpClient httpClient = getHttpClient();

        obtainSession(httpClient, authToken);

        HttpEntity entity;
        try {
            entity = new StringEntity(
                    "<?xml version=\"1.0\"?><?xml-stylesheet type=\"text/xsl\" href=\"xslt/enquiries/expositions.xslt\"?><ENQUIRY sessid=\""
                            + authToken.sessionId
                            + "\" lang=\"BG\" funcid=\"14\"><EXPOSITIONS/></ENQUIRY>", "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        HttpPost post = new HttpPost(ENQUIRY_URL);
        post.addHeader("Content-Type", "text/xml");
        post.setHeader("Accept", "*/*");
        post.setEntity(entity);

        HttpResponse resp = httpClient.execute(post);
        if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new AuthenticationException("Invalid credentials!");
        }

        String response = EntityUtils.toString(resp.getEntity());

        Log.v(TAG, "response = " + response);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ParseException(e.getMessage());
        }

        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(response));

        Document doc;
        try {
            doc = db.parse(is);
        } catch (SAXException e) {
            throw new ParseException(e.getMessage());
        }

        NodeList expositions = doc.getElementsByTagName("EXPOSITION");
        List<RawBankAccount> bankAccounts = new LinkedList<RawBankAccount>();
        for (int i = 0; i < expositions.getLength(); i++) {
            Node exposition = expositions.item(i);

            RawBankAccount bankAccount = obtainBankAccountFromExposition(exposition);
            if (bankAccount != null) {
                bankAccounts.add(bankAccount);
            }

        }

        return bankAccounts;
    }

    private RawBankAccount obtainBankAccountFromExposition(Node exposition) {
        NamedNodeMap attributes = exposition.getAttributes();
        return new RawBankAccount()
                .setServerId(attributes.getNamedItem("accountid").getTextContent())
                .setIBAN(attributes.getNamedItem("iban").getTextContent())
                .setCurrency(attributes.getNamedItem("curr").getTextContent())
                .setName(attributes.getNamedItem("alias").getTextContent())
                .setLastTransaction(attributes.getNamedItem("lastmovement").getTextContent())
                .setBalance(Convert.strToFloat(attributes.getNamedItem("balance").getTextContent()))
                .setAvailableBalance(
                        Convert.strToFloat(attributes.getNamedItem("available").getTextContent()));
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
