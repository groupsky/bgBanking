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

package eu.masconsult.bgbanking.banks;

import java.io.IOException;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;

public interface BankClient {

    /**
     * Authenticates with the banking service and returns authToken/cookie for
     * use in following operations.
     * 
     * @param username to use for authentication
     * @param password to use for authentication
     * @return authToken that represents successful authentication. A
     *         <code>null</code> value means authentication failed.
     * @throws IOException in case service communication is temporarily
     *             unavailable, and could succeed if tried again
     * @throws ParseException in case some or all received data was not
     *             understandable. This may indicate server-side change and thus
     *             a new version of the client should be implemented.
     * @throws CaptchaException in case a captcha is required to verify human
     *             being
     */
    String authenticate(String username, String password) throws IOException, ParseException,
            CaptchaException;

    /**
     * Retrieves all accounts with their sums
     * 
     * @param authtoken authToken obtained from a previous call to
     *            {@link #authenticate(String, String)}
     * @return list of all bank accounts
     * @throws IOException some low level communication problem
     * @throws ParseException when the server response is unexpected and cannot
     *             be handled
     * @throws AuthenticationException if the authToken has expired or is
     *             invalid. Should try to obtain new one using
     *             {@link #authenticate(String, String)} and if it doesn't work
     *             then user need to provide new credentials
     */
    List<RawBankAccount> getBankAccounts(String authtoken) throws IOException, ParseException,
            AuthenticationException;

}
