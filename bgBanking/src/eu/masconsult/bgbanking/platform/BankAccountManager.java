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

import java.util.List;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
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
            long id = lookupRawAccount(resolver, rawAccount.getIBAN());
            if (id != 0) {
                Log.d(TAG, "updating account " + rawAccount.getIBAN());
                updateAccount(context, resolver, rawAccount, true, id, batchOperation);
            } else {
                Log.d(TAG, "adding account " + rawAccount.getIBAN());
                addAccount(context, account, rawAccount, true, batchOperation);
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

    private static long lookupRawAccount(ContentResolver resolver, String iban) {
        long id = 0;
        final Cursor c = resolver.query(
                AccountIdQuery.CONTENT_URI,
                AccountIdQuery.PROJECTION,
                AccountIdQuery.SELECTION,
                new String[] {
                    String.valueOf(iban)
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
    public static void addAccount(Context context, Account account, RawBankAccount rawAccount,
            boolean inSync, BatchOperation batchOperation) {

        // Put the data in the banking provider
        final BankAccountOperations contactOp = BankAccountOperations.createNewAccount(
                context, rawAccount.getIBAN(), account, inSync, batchOperation);

        contactOp.addAccountInfo(rawAccount.getCurrency(), rawAccount.getAvailableBalance(),
                rawAccount.getBalance(), rawAccount.getLastTransaction());
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
     * @param rawAccountId the unique Id for this rawAccount in banking provider
     * @param batchOperation allow us to batch together multiple operations into
     *            a single provider call
     */
    public static void updateAccount(Context context, ContentResolver resolver,
            RawBankAccount rawBankAccount, boolean inSync, long rawAccountId,
            BatchOperation batchOperation) {

        final Cursor c =
                resolver.query(DataQuery.CONTENT_URI, DataQuery.PROJECTION, DataQuery.SELECTION,
                        new String[] {
                            String.valueOf(rawAccountId)
                        }, null);
        final BankAccountOperations contactOp = BankAccountOperations.updateExistingContact(
                context,
                rawAccountId, inSync, batchOperation);
        try {
            // Iterate over the existing rows of data, and update each one
            // with the information we received from the server.
            while (c.moveToNext()) {
                final long id = c.getLong(DataQuery.COLUMN_ID);
                final Uri uri = ContentUris.withAppendedId(
                        BankingContract.BankAccount.CONTENT_URI, id);
                contactOp.updateBalance(
                        uri,
                        c.getFloat(DataQuery.COLUMN_AVAILABLE_BALANCE),
                        c.getFloat(DataQuery.COLUMN_BALANCE),
                        c.getString(DataQuery.COLUMN_LAST_TRANSACTION_DATE),
                        rawBankAccount.getAvailableBalance(),
                        rawBankAccount.getBalance(),
                        rawBankAccount.getLastTransaction());
            } // while
        } finally {
            c.close();
        }
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

        public static final String SELECTION = BankingContract.BankAccount.COLUMN_NAME_IBAN
                + "=?";
    }

    /**
     * Constants for a query to get account data for a given rawAccountId
     */
    final private static class DataQuery {

        private DataQuery() {
        }

        public static final String[] PROJECTION =
                new String[] {
                        BankingContract.BankAccount._ID,
                        BankingContract.BankAccount.COLUMN_NAME_AVAILABLE_BALANCE,
                        BankingContract.BankAccount.COLUMN_NAME_BALANCE,
                        BankingContract.BankAccount.COLUMN_NAME_LAST_TRANSACTION_DATE
                };

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_AVAILABLE_BALANCE = 1;
        public static final int COLUMN_BALANCE = 2;
        public static final int COLUMN_LAST_TRANSACTION_DATE = 3;

        public static final Uri CONTENT_URI = BankingContract.BankAccount.CONTENT_URI;

        public static final String SELECTION = BankingContract.BankAccount._ID + "=?";
    }
}
