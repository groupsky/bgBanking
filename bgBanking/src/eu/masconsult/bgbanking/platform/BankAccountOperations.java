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

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import eu.masconsult.bgbanking.provider.BankingContract;

/**
 * Helper class for storing data in the platform content providers.
 */
public class BankAccountOperations {

    private final ContentValues mValues;
    private final BatchOperation mBatchOperation;
    private final Context mContext;
    private boolean mIsSyncOperation;
    private long mRawAccountId;
    private int mBackReference;
    private boolean mIsNewContact;

    /**
     * Since we're sending a lot of contact provider operations in a single
     * batched operation, we want to make sure that we "yield" periodically so
     * that the Banking Provider can write changes to the DB, and can open a new
     * transaction. This prevents ANR (application not responding) errors. The
     * recommended time to specify that a yield is permitted is with the first
     * operation on a particular contact. So if we're updating multiple fields
     * for a single account, we make sure that we call withYieldAllowed(true) on
     * the first field that we update. We use mIsYieldAllowed to keep track of
     * what value we should pass to withYieldAllowed().
     */
    private boolean mIsYieldAllowed;

    /**
     * Returns an instance of BankAccountOperations instance for adding new
     * account to the platform banking provider.
     * 
     * @param context the Authenticator Activity context
     * @param userId the userId of the sample SyncAdapter user object
     * @param accountName the username for the SyncAdapter account
     * @param isSyncOperation are we executing this as part of a sync operation?
     * @return instance of BankAccountOperations
     */
    public static BankAccountOperations createNewAccount(Context context, String iban,
            Account account, boolean isSyncOperation, BatchOperation batchOperation) {
        return new BankAccountOperations(context, iban, account, isSyncOperation,
                batchOperation);
    }

    /**
     * Returns an instance of BankAccountOperations for updating existing
     * account in the platform banking provider.
     * 
     * @param context the Authenticator Activity context
     * @param rawAccountId the unique Id of the existing rawBankAccount
     * @param isSyncOperation are we executing this as part of a sync operation?
     * @return instance of ContactOperations
     */
    public static BankAccountOperations updateExistingContact(Context context, long rawAccountId,
            boolean isSyncOperation, BatchOperation batchOperation) {
        return new BankAccountOperations(context, rawAccountId, isSyncOperation, batchOperation);
    }

    public BankAccountOperations(Context context, boolean isSyncOperation,
            BatchOperation batchOperation) {
        mValues = new ContentValues();
        mIsYieldAllowed = true;
        mIsSyncOperation = isSyncOperation;
        mContext = context;
        mBatchOperation = batchOperation;
    }

    public BankAccountOperations(Context context, String iban, Account account,
            boolean isSyncOperation, BatchOperation batchOperation) {
        this(context, isSyncOperation, batchOperation);
        mBackReference = mBatchOperation.size();
        mIsNewContact = true;
        mValues.put(BankingContract.BankAccount.COLUMN_NAME_IBAN, iban);
        mValues.put(BankingContract.BankAccount.ACCOUNT_NAME, account.name);
        mValues.put(BankingContract.BankAccount.ACCOUNT_TYPE, account.type);
        ContentProviderOperation.Builder builder =
                newInsertCpo(BankingContract.BankAccount.CONTENT_URI, mIsSyncOperation, true)
                        .withValues(mValues);
        mBatchOperation.add(builder.build());
    }

    public BankAccountOperations(Context context, long rawAccountId, boolean isSyncOperation,
            BatchOperation batchOperation) {
        this(context, isSyncOperation, batchOperation);
        mIsNewContact = false;
        mRawAccountId = rawAccountId;
    }

    /**
     * Adds a account info.
     * 
     * @param currency The currency type for this account.
     * @param availableBalance The available balance of the account.
     * @param balance The balance of the account.
     * @param lastTransaction The last transaction date of the account.
     * @return instance of BankAccountOperations
     */
    public BankAccountOperations addAccountInfo(String currency, float availableBalance,
            float balance,
            String lastTransaction) {
        if (!TextUtils.isEmpty(currency)) {
            mValues.put(BankingContract.BankAccount.COLUMN_NAME_CURRENCY, currency);
        }

        mValues.put(BankingContract.BankAccount.COLUMN_NAME_AVAILABLE_BALANCE, availableBalance);
        mValues.put(BankingContract.BankAccount.COLUMN_NAME_BALANCE, balance);

        if (!TextUtils.isEmpty(lastTransaction)) {
            mValues.put(BankingContract.BankAccount.COLUMN_NAME_LAST_TRANSACTION_DATE,
                    lastTransaction);
        }

        if (mValues.size() > 0) {
            addInsertOp();
        }
        return this;
    }

    /**
     * Updates account's balance.
     */
    public BankAccountOperations updateBalance(Uri uri,
            float existingAvailableBalance,
            float existingBalance,
            String existingLastTransaction,
            float availableBalance,
            float balance,
            String lastTransaction) {

        mValues.clear();
        if (!TextUtils.equals(existingLastTransaction, lastTransaction)) {
            mValues.put(BankingContract.BankAccount.COLUMN_NAME_LAST_TRANSACTION_DATE,
                    lastTransaction);
        }
        if (existingAvailableBalance != availableBalance) {
            mValues.put(BankingContract.BankAccount.COLUMN_NAME_AVAILABLE_BALANCE,
                    availableBalance);
        }
        if (existingBalance != balance) {
            mValues.put(BankingContract.BankAccount.COLUMN_NAME_BALANCE, balance);
        }
        if (mValues.size() > 0) {
            addUpdateOp(uri);
        }
        return this;
    }

    /**
     * Adds an insert operation into the batch
     */
    private void addInsertOp() {

        ContentProviderOperation.Builder builder =
                newUpdateCpo(BankingContract.BankAccount.CONTENT_URI, mIsSyncOperation,
                        mIsYieldAllowed);
        builder.withSelection(BaseColumns._ID + "=?", new String[] {
                String.valueOf(mRawAccountId)
        });
        builder.withValues(mValues);
        if (mIsNewContact) {
            builder.withSelectionBackReference(0, mBackReference);
        }
        mIsYieldAllowed = false;
        mBatchOperation.add(builder.build());
    }

    /**
     * Adds an update operation into the batch
     */
    private void addUpdateOp(Uri uri) {
        ContentProviderOperation.Builder builder =
                newUpdateCpo(uri, mIsSyncOperation, mIsYieldAllowed).withValues(mValues);
        mIsYieldAllowed = false;
        mBatchOperation.add(builder.build());
    }

    public static ContentProviderOperation.Builder newInsertCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newInsert(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    public static ContentProviderOperation.Builder newUpdateCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newUpdate(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    public static ContentProviderOperation.Builder newDeleteCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newDelete(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
        if (isSyncOperation) {
            // If we're in the middle of a real sync-adapter operation, then go
            // ahead
            // and tell the Banking provider that we're the sync adapter. That
            // gives us some special permissions - like the ability to really
            // delete an account, and the ability to clear the dirty flag.
            //
            // If we're not in the middle of a sync operation (for example, we
            // just
            // locally created/edited a new account), then we don't want to use
            // the special permissions, and the system will automagically mark
            // the account as 'dirty' for us!
            return uri
                    .buildUpon()
                    .appendQueryParameter(BankingContract.BankAccount.CALLER_IS_SYNCADAPTER,
                            "true")
                    .build();
        }
        return uri;
    }
}
