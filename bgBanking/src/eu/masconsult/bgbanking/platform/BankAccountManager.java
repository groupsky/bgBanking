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

package eu.masconsult.bgbanking.platform;

import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.ACCOUNT_NAME;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.ACCOUNT_TYPE;

import java.util.List;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.banks.RawBankAccount;
import eu.masconsult.bgbanking.provider.BankingContract;

public final class BankAccountManager {

    private static final String TAG = BankingApplication.TAG + "BankAccountManager";

    public static void updateAccounts(Context context, Account account,
            List<RawBankAccount> bankAccounts) {

        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);

        Log.d(TAG, "In SyncContacts");
        for (final RawBankAccount rawAccount : bankAccounts) {
            // we know that banks don't delete accounts, so we only need to
            // check if we already have the account at our side
            long id = lookupRawAccount(resolver, account, rawAccount.getServerId());
            if (id != 0) {
                Log.d(TAG, "updating account " + rawAccount.getServerId());
                updateAccount(resolver, rawAccount, true, id, batchOperation);
            } else {
                Log.d(TAG, "adding account " + rawAccount.getServerId());
                addAccount(account, rawAccount, true, batchOperation);
            }

            // A sync adapter should batch operations on multiple accounts,
            // because it will make a dramatic performance difference.
            // (UI updates, etc)
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }
        batchOperation.execute();
    }

    private static long lookupRawAccount(ContentResolver resolver, Account account, String serverId) {
        long id = 0;
        final Cursor c = resolver.query(
                AccountIdQuery.CONTENT_URI,
                AccountIdQuery.PROJECTION,
                AccountIdQuery.SELECTION,
                new String[] {
                        String.valueOf(serverId),
                        account.name,
                        account.type
                },
                null);
        try {
            if (c != null && c.moveToFirst()) {
                id = c.getLong(AccountIdQuery.COLUMN_RAW_ID);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return id;
    }

    /**
     * Adds a single account to the platform banking provider. This can be used
     * to respond to a new account found as part of sync information returned
     * from the server.
     * 
     * @param context the Authenticator Activity context
     * @param account the account the record belongs to
     * @param rawAccount the sample SyncAdapter User object
     * @param inSync is the add part of a client-server sync?
     * @param batchOperation allow us to batch together multiple operations into
     *            a single provider call
     */
    public static void addAccount(Account account, RawBankAccount rawAccount, boolean inSync,
            BatchOperation batchOperation) {

        // Put the data in the banking provider
        BankAccountOperations
                .createNewAccount(account, inSync, batchOperation)
                .setAvailableBalance(rawAccount.getAvailableBalance())
                .setBalance(rawAccount.getBalance())
                .setCurrency(rawAccount.getCurrency())
                .setIBAN(rawAccount.getIBAN())
                .setName(rawAccount.getName())
                .setServerId(rawAccount.getServerId())
                .done();
    }

    /**
     * Updates a single account to the platform banking provider. This method
     * can be used to update a account from a sync operation. This operation is
     * actually relatively complex. We query the database to find all the rows
     * of info that already exist for this Account. For rows that exist (and
     * thus we're modifying existing fields), we create an update operation to
     * change that field. But for fields we're adding, we create "add"
     * operations to create new rows for those fields.
     * 
     * @param context the Authenticator Activity context
     * @param resolver the ContentResolver to use
     * @param rawBankAccount the sample SyncAdapter account object
     * @param inSync is the update part of a client-server sync?
     * @param internalId the unique Id for this rawAccount in banking provider
     * @param batchOperation allow us to batch together multiple operations into
     *            a single provider call
     */
    public static void updateAccount(ContentResolver resolver,
            RawBankAccount rawBankAccount, boolean inSync, long internalId,
            BatchOperation batchOperation) {

        final Cursor c =
                resolver.query(DataQuery.CONTENT_URI, DataQuery.PROJECTION, DataQuery.SELECTION,
                        new String[] {
                            String.valueOf(internalId)
                        }, null);
        final BankAccountOperations contactOp = BankAccountOperations.updateExistingContact(
                internalId, inSync, batchOperation);
        try {
            // Iterate over the existing rows of data, and update each one
            // with the information we received from the server.
            if (c.moveToNext()) {
                contactOp
                        .updateAvailableBalance(c.getFloat(DataQuery.COLUMN_AVAILABLE_BALANCE),
                                rawBankAccount.getAvailableBalance())
                        .updateBalance(c.getFloat(DataQuery.COLUMN_BALANCE),
                                rawBankAccount.getBalance())
                        .updateName(c.getString(DataQuery.COLUMN_NAME), rawBankAccount.getName())
                        .done();
            }
        } finally {
            c.close();
        }
    }

    public static void removeAccount(Context context, Account account) {
        final ContentResolver resolver = context.getContentResolver();
        resolver.delete(AccountIdQuery.CONTENT_URI, ACCOUNT_NAME
                + "=? AND " + ACCOUNT_TYPE + "=?", new String[] {
                account.name, account.type
        });
    }

    /**
     * Constants for a query to find an account given an IBAN.
     */
    final private static class AccountIdQuery {

        private AccountIdQuery() {
        }

        public final static String[] PROJECTION = new String[] {
                BankingContract.BankAccount._ID,
        };

        public final static int COLUMN_RAW_ID = 0;

        public final static Uri CONTENT_URI = BankingContract.BankAccount.CONTENT_URI;

        public static final String SELECTION = BankingContract.BankAccount.COLUMN_NAME_SERVER_ID
                + "=? AND " + BankingContract.BankAccount.ACCOUNT_NAME + "=? AND "
                + BankingContract.BankAccount.ACCOUNT_TYPE + "=?";
    }

    /**
     * Constants for a query to get account data for a given rawAccountId
     */
    final private static class DataQuery {

        private DataQuery() {
        }

        public static final String[] PROJECTION =
                new String[] {
                        BankingContract.BankAccount.COLUMN_NAME_AVAILABLE_BALANCE,
                        BankingContract.BankAccount.COLUMN_NAME_BALANCE,
                        BankingContract.BankAccount.COLUMN_NAME_LAST_TRANSACTION_DATE,
                        BankingContract.BankAccount.COLUMN_NAME_NAME,
                };

        public static final int COLUMN_AVAILABLE_BALANCE = 0;
        public static final int COLUMN_BALANCE = 1;
        public static final int COLUMN_LAST_TRANSACTION_DATE = 2;
        public static final int COLUMN_NAME = 3;

        public static final Uri CONTENT_URI = BankingContract.BankAccount.CONTENT_URI;

        public static final String SELECTION = BankingContract.BankAccount._ID + "=?";
    }

}
