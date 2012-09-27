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

package eu.masconsult.bgbanking.activity.fragment;

import static android.provider.BaseColumns._ID;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.ACCOUNT_NAME;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.ACCOUNT_TYPE;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_AVAILABLE_BALANCE;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_BALANCE;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_CURRENCY;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_IBAN;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_LAST_TRANSACTION_DATE;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_NAME;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.CONTENT_URI;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.commonsware.cwac.merge.MergeAdapter;

import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.R;
import eu.masconsult.bgbanking.banks.Bank;
import eu.masconsult.bgbanking.provider.BankingContract;
import eu.masconsult.bgbanking.utils.Convert;

public class AccountsListFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnAccountsUpdateListener {

    protected static final String TAG = BankingApplication.TAG + "AccountsListFragment";

    // This is the Adapter being used to display the list's data.
    MyMergeAdapter mAdapter;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    final ArrayList<Account> mAccounts = new ArrayList<Account>();

    private AccountManager accountManager;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.v(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText("No bank accounts");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Start out with a progress indicator.
        setListShown(false);
    }

    private void populateList() {
        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new MyMergeAdapter();
        mAccounts.clear();

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        Bank[] banks = Bank.values();
        for (Bank bank : banks) {
            Account[] accounts = accountManager.getAccountsByType(bank
                    .getAccountType(getActivity()));
            for (Account account : accounts) {
                View header = getActivity().getLayoutInflater().inflate(
                        R.layout.row_bank_account_header,
                        null);
                View bank_icon = header.findViewById(R.id.bank_icon);
                if (bank_icon != null && bank_icon instanceof ImageView) {
                    ((ImageView) bank_icon).setImageResource(bank.iconResource);
                }
                View bank_name = header.findViewById(R.id.bank_name);
                if (bank_name != null && bank_name instanceof TextView) {
                    ((TextView) bank_name).setText(bank.labelRes);
                }
                View account_name = header.findViewById(R.id.account_name);
                if (account_name != null && account_name instanceof TextView) {
                    ((TextView) account_name).setText(account.name);
                }
                mAdapter.addView(header);
                mAdapter.addAdapter(new BankAccountsAdapter(getActivity(),
                        R.layout.row_bank_account,
                        null, 0));

                int idx = mAccounts.size();
                mAccounts.add(idx, account);
                getLoaderManager().initLoader(idx, null, this);
            }
        }

        Log.v(TAG, "found " + mAccounts.size() + " accounts");
        setListAdapter(mAdapter);
    }

    @Override
    public void onAttach(Activity activity) {
        Log.v(TAG, "onAttach");

        super.onAttach(activity);

        accountManager = AccountManager.get(getActivity());
        accountManager.addOnAccountsUpdatedListener(this, null, true);
    }

    @Override
    public void onDetach() {
        accountManager.removeOnAccountsUpdatedListener(this);
        accountManager = null;

        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem refreshItem = menu.add("Refresh");
        refreshItem.setIcon(R.drawable.ic_menu_refresh);
        refreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
                | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        refreshItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AccountManager accountManager = AccountManager.get(getActivity());
                if (accountManager == null) {
                    return false;
                }

                Bank[] banks = Bank.values();
                for (Bank bank : banks) {
                    for (Account account : accountManager.getAccountsByType(bank
                            .getAccountType(getActivity()))) {
                        Log.v(TAG,
                                "account: "
                                        + account.name
                                        + ", "
                                        + account.type
                                        + ", "
                                        + ContentResolver.getIsSyncable(account,
                                                BankingContract.AUTHORITY));
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                        ContentResolver.requestSync(account, BankingContract.AUTHORITY, bundle);
                    }
                }
                return true;
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Insert desired behavior here.
        Log.i("FragmentComplexList", "Item clicked: " + id);
    }

    // These are the Contacts rows that we will retrieve.
    static final String[] ACCOUNTS_SUMMARY_PROJECTION = new String[] {
            _ID,
            COLUMN_NAME_IBAN,
            COLUMN_NAME_NAME,
            COLUMN_NAME_CURRENCY,
            COLUMN_NAME_BALANCE,
            COLUMN_NAME_AVAILABLE_BALANCE,
            COLUMN_NAME_LAST_TRANSACTION_DATE,
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        Uri baseUri = CONTENT_URI;

        Account account = mAccounts.get(id);
        Log.v(TAG, "creating cursor loader for account: " + account);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, ACCOUNTS_SUMMARY_PROJECTION,
                ACCOUNT_NAME
                        + "=? AND " + ACCOUNT_TYPE + "=?",
                new String[] {
                        account.name, account.type
                }, COLUMN_NAME_NAME + " COLLATE LOCALIZED ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing
        // the
        // old cursor once we return.)
        getCursorAdapter(loader).swapCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    private CursorAdapter getCursorAdapter(Loader<Cursor> loader) {
        return (CursorAdapter) mAdapter.getPiece(loader.getId() * 2 + 1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        getCursorAdapter(loader).swapCursor(null);
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        populateList();
    }

    private static final class BankAccountsAdapter extends ResourceCursorAdapter {
        private BankAccountsAdapter(Context context, int layout, Cursor c, int flags) {
            super(context, layout, c, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String name = getFromColumn(cursor, COLUMN_NAME_NAME);
            setToView(view, R.id.name, name);

            String iban = Convert.formatIBAN(getFromColumn(cursor, COLUMN_NAME_IBAN));
            if (TextUtils.equals(name, iban)) {
                iban = null;
            }
            setToView(view, R.id.description, iban);
            setToView(view, R.id.last_transaction,
                    getFromColumn(cursor, COLUMN_NAME_LAST_TRANSACTION_DATE));

            setToView(view, R.id.sum,
                    Convert.formatCurrency(
                            getFromFColumn(cursor, COLUMN_NAME_AVAILABLE_BALANCE),
                            getFromColumn(cursor, COLUMN_NAME_CURRENCY)));
        }

        private String getFromColumn(Cursor cursor, String columnName) {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex != -1) {
                return cursor.isNull(columnIndex) ? null : cursor
                        .getString(columnIndex);
            }
            return null;
        }

        private Float getFromFColumn(Cursor cursor, String columnName) {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex != -1) {
                return cursor.isNull(columnIndex) ? null : cursor
                        .getFloat(columnIndex);
            }
            return null;
        }

        private void setToView(View layout, int id, String text) {
            View view = layout.findViewById(id);
            if (view == null) {
                return;
            }
            if (!(view instanceof TextView)) {
                return;
            }
            if (TextUtils.isEmpty(text)) {
                view.setVisibility(View.GONE);
            } else {
                view.setVisibility(View.VISIBLE);
                ((TextView) view).setText(text);
            }
        }

    }

    private static final class MyMergeAdapter extends MergeAdapter {

        ListAdapter getPiece(int index) {
            return pieces.get(index);
        }

    }
}
