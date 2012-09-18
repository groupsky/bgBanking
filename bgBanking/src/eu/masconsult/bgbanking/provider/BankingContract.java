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

package eu.masconsult.bgbanking.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class BankingContract {

    /** The authority for the Banking provider */
    public static final String AUTHORITY = "eu.masconsult.bgbanking";

    private BankingContract() {
        // this class cannot be initialized ever
    }

    /**
     * Accounts table contract
     */
    public static final class BankAccount implements BaseColumns {

        // This class cannot be instantiated
        private BankAccount() {
        }

        /**
         * The table name offered by this provider
         */
        public static final String TABLE_NAME = "accounts";

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path parts for the URIs
         */

        /**
         * Path part for the Accounts URI
         */
        private static final String PATH_ACCOUNTS = "/accounts";

        /**
         * Path part for the Account ID URI
         */
        private static final String PATH_ACCOUNT_ID = "/accounts/";

        /**
         * 0-relative position of an account ID segment in the path part of a
         * account ID URI
         */
        public static final int ACCOUNT_ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_ACCOUNTS);

        /**
         * The content URI base for a single account. Callers must append a
         * account id to this Uri to retrieve an account
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY
                + PATH_ACCOUNT_ID);

        /**
         * The content URI match pattern for a single account, specified by its
         * ID. Use this to match incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME + AUTHORITY
                + PATH_ACCOUNT_ID + "/#");

        /*
         * MIME type definitions
         */
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * accounts.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.eu.masconsult.bgbanking.account";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * account.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.eu.masconsult.bgbanking.account";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "name";

        /*
         * Column definitions
         */

        /**
         * Column name for the name of the bank account
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_NAME = "name";

        /**
         * Column name for the server id of the bank account
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_SERVER_ID = "server_id";

        /**
         * Column name for the iban of the bank account
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_IBAN = "iban";

        /**
         * Column name of the account currency
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_CURRENCY = "currency";

        /**
         * Column name of the account balance
         * <P>
         * Type: FLOAT
         * </P>
         */
        public static final String COLUMN_NAME_BALANCE = "balance";

        /**
         * Column name of the account available balance
         * <P>
         * Type: FLOAT
         * </P>
         */
        public static final String COLUMN_NAME_AVAILABLE_BALANCE = "available_balance";

        /**
         * Column name for the last transaction date
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_LAST_TRANSACTION_DATE = "last_transaction";

        /**
         * Column name for the account type that owns this record
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ACCOUNT_TYPE = "account_type";

        /**
         * Column name for the account name that owns this record
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ACCOUNT_NAME = "account_name";

        public static final String CALLER_IS_SYNCADAPTER = "__caller_is_sync_adapter";
    }
}
