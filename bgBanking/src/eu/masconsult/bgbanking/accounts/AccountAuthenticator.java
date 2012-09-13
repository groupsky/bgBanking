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

package eu.masconsult.bgbanking.accounts;

import static android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE;
import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_ERROR_CODE;
import static android.accounts.AccountManager.KEY_ERROR_MESSAGE;
import static android.accounts.AccountManager.KEY_INTENT;
import static eu.masconsult.bgbanking.accounts.LoginActivity.KEY_CAPTCHA_URI;

import java.io.IOException;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.Constants;
import eu.masconsult.bgbanking.banks.Bank;
import eu.masconsult.bgbanking.banks.CaptchaException;

/**
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = BankingApplication.TAG + "AcAuth";
    private Context context;
    private AccountManager accountManager;

    public AccountAuthenticator(Context context) {
        super(context);
        this.context = context;
        accountManager = AccountManager.get(context);
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        Log.v(TAG, "addAccount(type: " + accountType + ", authTokenType: " + authTokenType + ")");

        final Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(KEY_ACCOUNT_TYPE, accountType);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
            Bundle options) throws NetworkErrorException {
        Log.v(TAG, "confirmCredentials(account: " + account + ")");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        Log.v(TAG, "editProperties(accountType: " + accountType + ")");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        Log.v(TAG, "getAuthToken(account: " + account + ", authTokenType: " + authTokenType + ")");

        Bank bank = Bank.fromAccountType(context, account.type);
        if (bank == null) {
            throw new IllegalArgumentException("unsupported account type " + account.type);
        }
        if (!Constants.getAuthorityType(context).equals(authTokenType)) {
            throw new IllegalArgumentException("unsupported authTOkenType " + authTokenType);
        }

        final Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(KEY_ACCOUNT_NAME, account.name);
        intent.putExtra(KEY_ACCOUNT_TYPE, account.type);

        String password = accountManager.getPassword(account);
        try {
            String authToken = bank.getClient().authenticate(account.name, password);
            Log.v(TAG, "obtained auth token " + authToken);

            if (authToken == null) {
                throw new AuthenticationException("no authToken");
            }

            // store the new auth token and return it
            accountManager.setAuthToken(account, authTokenType, authToken);
            intent.putExtra(KEY_AUTHTOKEN, authToken);
            return intent.getExtras();
        } catch (ParseException e) {
            Log.w(TAG, "ParseException", e);
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_ERROR_CODE, 1);
            bundle.putString(KEY_ERROR_MESSAGE, e.getMessage());
            return bundle;
        } catch (IOException e) {
            Log.w(TAG, "IOException", e);
            throw new NetworkErrorException(e);
        } catch (CaptchaException e) {
            Log.w(TAG, "CaptchaException", e);
            // We need human to verify captcha
            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            intent.putExtra(KEY_CAPTCHA_URI, e.getCaptchaUri());
            return bundle;
        } catch (AuthenticationException e) {
            Log.w(TAG, "AuthenticationException", e);
            // we need new credentials
            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        Log.v(TAG, "getAuthTokenLabel(authTokenType: " + authTokenType + ")");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) throws NetworkErrorException {
        Log.v(TAG, "hasFeatures(account: " + account + ")");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        Log.v(TAG, "hasFeatures(account: " + account + ", authTokenType: " + authTokenType + ")");
        // TODO Auto-generated method stub
        return null;
    }

}
