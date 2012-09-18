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
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;
import eu.masconsult.bgbanking.provider.BankingContract;

/**
 * Helper class for storing data in the platform content providers.
 */
public class BankAccountOperations {

    private final ContentValues mValues;
    private final BatchOperation mBatchOperation;
    private boolean mIsSyncOperation;
    private long mInternalId;
    private boolean mIsNewContact;

    /**
     * Returns an instance of BankAccountOperations instance for adding new
     * account to the platform banking provider.
     * 
     * @param context the Authenticator Activity context
     * @param account the {@link Account} for the SyncAdapter account
     * @param isSyncOperation are we executing this as part of a sync operation?
     * @return instance of {@link BatchOperation}
     */
    public static BankAccountOperations createNewAccount(Account account, boolean isSyncOperation,
            BatchOperation batchOperation) {
        return new BankAccountOperations(account, isSyncOperation, batchOperation);
    }

    /**
     * Returns an instance of BankAccountOperations for updating existing
     * account in the platform banking provider.
     * 
     * @param context the Authenticator Activity context
     * @param internalId the unique Id of the existing account
     * @param isSyncOperation are we executing this as part of a sync operation?
     * @return instance of {@link BatchOperation}
     */
    public static BankAccountOperations updateExistingContact(long internalId,
            boolean isSyncOperation, BatchOperation batchOperation) {
        return new BankAccountOperations(internalId, isSyncOperation, batchOperation);
    }

    /**
     * initialize common stuff
     * 
     * @param context
     * @param isSyncOperation
     * @param batchOperation
     */
    private BankAccountOperations(boolean isSyncOperation, BatchOperation batchOperation) {
        mValues = new ContentValues();
        mIsSyncOperation = isSyncOperation;
        mBatchOperation = batchOperation;
    }

    /**
     * for new accounts
     * 
     * @param context
     * @param serverId
     * @param account
     * @param isSyncOperation
     * @param batchOperation
     */
    private BankAccountOperations(Account account, boolean isSyncOperation,
            BatchOperation batchOperation) {
        this(isSyncOperation, batchOperation);
        mIsNewContact = true;
        mValues.put(BankingContract.BankAccount.ACCOUNT_NAME, account.name);
        mValues.put(BankingContract.BankAccount.ACCOUNT_TYPE, account.type);
    }

    /**
     * for updating
     * 
     * @param context
     * @param internalId
     * @param isSyncOperation
     * @param batchOperation
     */
    private BankAccountOperations(long internalId, boolean isSyncOperation,
            BatchOperation batchOperation) {
        this(isSyncOperation, batchOperation);
        mIsNewContact = false;
        mInternalId = internalId;
    }

    public void done() {
        if (mValues.size() == 0) {
            return;
        }

        ContentProviderOperation.Builder builder;
        if (mIsNewContact) {
            builder = newInsertCpo(BankingContract.BankAccount.CONTENT_URI, mIsSyncOperation, true);
        } else {
            builder = newUpdateCpo(
                    ContentUris
                            .withAppendedId(BankingContract.BankAccount.CONTENT_URI, mInternalId),
                    mIsSyncOperation, true);
        }
        mBatchOperation.add(builder.withValues(mValues).build());
    }

    public BankAccountOperations setAvailableBalance(float balance) {
        mValues.put(BankingContract.BankAccount.COLUMN_NAME_AVAILABLE_BALANCE, balance);
        return this;
    }

    public BankAccountOperations setBalance(float balance) {
        mValues.put(BankingContract.BankAccount.COLUMN_NAME_BALANCE, balance);
        return this;
    }

    public BankAccountOperations setCurrency(String currency) {
        if (!TextUtils.isEmpty(currency)) {
            mValues.put(BankingContract.BankAccount.COLUMN_NAME_CURRENCY, currency);
        }
        return this;
    }

    public BankAccountOperations setIBAN(String iban) {
        if (!TextUtils.isEmpty(iban)) {
            mValues.put(BankingContract.BankAccount.COLUMN_NAME_IBAN, iban);
        }
        return this;
    }

    public BankAccountOperations setName(String name) {
        if (!TextUtils.isEmpty(name)) {
            mValues.put(BankingContract.BankAccount.COLUMN_NAME_NAME, name);
        }
        return this;
    }

    public BankAccountOperations setServerId(String serverId) {
        if (!TextUtils.isEmpty(serverId)) {
            mValues.put(BankingContract.BankAccount.COLUMN_NAME_SERVER_ID, serverId);
        }
        return this;
    }

    private static ContentProviderOperation.Builder newInsertCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newInsert(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    private static ContentProviderOperation.Builder newUpdateCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newUpdate(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    private static ContentProviderOperation.Builder newDeleteCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newDelete(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
        if (isSyncOperation) {
            // If we're in the middle of a real sync-adapter operation, then go
            // ahead and tell the Banking provider that we're the sync adapter.
            // That gives us some special permissions - like the ability to
            // really delete an account, and the ability to clear the dirty
            // flag.
            //
            // If we're not in the middle of a sync operation (for example, we
            // just locally created/edited a new account), then we don't want to
            // use the special permissions, and the system will automagically
            // mark the account as 'dirty' for us!
            return uri
                    .buildUpon()
                    .appendQueryParameter(BankingContract.BankAccount.CALLER_IS_SYNCADAPTER, "true")
                    .build();
        }
        return uri;
    }

    public BankAccountOperations updateAvailableBalance(float existingBalance, float newBalance) {
        if (existingBalance != newBalance) {
            setAvailableBalance(newBalance);
        }
        return this;
    }

    public BankAccountOperations updateBalance(float existingBalance, float newBalance) {
        if (existingBalance != newBalance) {
            setBalance(newBalance);
        }
        return this;
    }

    public BankAccountOperations updateName(String existingName, String newName) {
        if (!TextUtils.isEmpty(newName)) {
            if (!newName.equals(existingName)) {
                setName(newName);
            }
        }
        return this;
    }
}
