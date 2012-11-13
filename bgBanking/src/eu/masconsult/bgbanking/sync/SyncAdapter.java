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

package eu.masconsult.bgbanking.sync;

import java.io.IOException;
import java.util.List;

import org.acra.ACRA;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.Constants;
import eu.masconsult.bgbanking.banks.Bank;
import eu.masconsult.bgbanking.banks.RawBankAccount;
import eu.masconsult.bgbanking.platform.BankAccountManager;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = BankingApplication.TAG + "SyncAdapter";

    private static final boolean NOTIFY_AUTH_FAILURE = true;

    public static final String START_SYNC = "eu.masconsult.bgbanking.sync.start";

    public static final String STOP_SYNC = "eu.masconsult.bgbanking.sync.stop";

    private Context context;

    private AccountManager accountManager;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.context = context;
        accountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        Log.v(TAG, "onPerformSync(): " + account + ", " + authority);
        // we track only the account type (i.e - procredit, dsk, etc...) not the
        // username
        EasyTracker.getTracker().trackEvent("SyncAdapter", "sync", account.type, 1l);
        long _start = System.currentTimeMillis();

        // notify we are starting to perform sync
        context.sendBroadcast(new Intent().setAction(START_SYNC));

        Bank bank = Bank.fromAccountType(context, account.type);
        if (bank == null) {
            throw new IllegalArgumentException(String.format("unsupported account type: %s",
                    account.type));
        }
        if (!Constants.getAuthorityType(context).equals(authority)) {
            throw new IllegalArgumentException(
                    String.format("unsupported authority: %s", authority));
        }

        String authToken = null;
        try {
            // Use the account manager to request the AuthToken we'll need
            // to talk to our sample server. If we don't have an AuthToken
            // yet, this could involve a round-trip to the server to request
            // and AuthToken.
            authToken = accountManager.blockingGetAuthToken(account,
                    Constants.getAuthorityType(context), NOTIFY_AUTH_FAILURE);

            // get the bank accounts from server
            List<RawBankAccount> bankAccounts = bank.getClient().getBankAccounts(authToken);

            Log.d(TAG, "Received " + bankAccounts.size() + " bank accounts");
            BankAccountManager.updateAccounts(context, account, bankAccounts);

            syncResult.stats.numEntries += bankAccounts.size();

            // TODO Auto-generated method stub
        } catch (AuthenticatorException e) {
            Log.e(TAG, "AuthenticatorException", e);
            syncResult.stats.numAuthExceptions++;
            ACRA.getErrorReporter().handleException(e);
            EasyTracker.getTracker().trackEvent("SyncAdapter", "error-auth", account.type, 1l);
        } catch (OperationCanceledException e) {
            Log.e(TAG, "OperationCanceledExcetpion", e);
            EasyTracker.getTracker().trackEvent("SyncAdapter", "error-canceled", account.type, 1l);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            syncResult.stats.numIoExceptions++;
            ACRA.getErrorReporter().handleException(e);
            EasyTracker.getTracker().trackEvent("SyncAdapter", "error-io", account.type, 1l);
        } catch (AuthenticationException e) {
            Log.e(TAG, "AuthenticationException", e);
            if (authToken != null) {
                Log.d(TAG, "invalidating token: " + account.type + ", " + authToken);
                accountManager.invalidateAuthToken(account.type, authToken);
            }
            syncResult.stats.numAuthExceptions++;
            EasyTracker.getTracker().trackEvent("SyncAdapter", "error-session", account.type, 1l);
        } catch (ParseException e) {
            Log.e(TAG, "ParseException", e);
            syncResult.stats.numParseExceptions++;
            ACRA.getErrorReporter().handleException(e);
            EasyTracker.getTracker().trackEvent("SyncAdapter", "error-parse", account.type, 1l);
        }

        // notify we finished performing sync
        context.sendBroadcast(new Intent().setAction(STOP_SYNC));

        EasyTracker.getTracker().trackTiming("SyncAdapter", System.currentTimeMillis() - _start,
                bank.name(), null);
    }

}
