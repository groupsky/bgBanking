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
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.commonsware.cwac.adapter.AdapterWrapper;
import com.commonsware.cwac.merge.MergeAdapter;

import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.R;
import eu.masconsult.bgbanking.banks.Bank;
import eu.masconsult.bgbanking.provider.BankingContract;
import eu.masconsult.bgbanking.sync.SyncAdapter;
import eu.masconsult.bgbanking.utils.Convert;
import eu.masconsult.bgbanking.utils.SampleCursor;

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
                addAccount(bank, account, false);
            }
        }

        if (mAccounts.size() == 0) {
            // show sample accounts
            for (Bank bank : banks) {
                addAccount(
                        bank,
                        new Account(getString(R.string.sample_account_name), bank
                                .getAccountType(getActivity())),
                        true);
            }

        }

        Log.v(TAG, "found " + mAccounts.size() + " accounts");
        setListAdapter(mAdapter);
    }

    private void addAccount(Bank bank, Account account, boolean sample) {
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
        BankAccountsAdapter adapter = new BankAccountsAdapter(getActivity(), account,
                R.layout.row_bank_account, null, 0);
        EmptyBankAccountsAdapter adapter2 = new EmptyBankAccountsAdapter(getActivity(), account,
                adapter);
        mAdapter.addAdapter(adapter2);

        int idx = mAccounts.size();
        mAccounts.add(idx, account);
        if (sample) {
            Log.d(TAG, "Adding sample account cursor");
            adapter.swapCursor(new SampleCursor(getActivity()));
        } else {
            Log.d(TAG, "Requesting loader");
            getLoaderManager().initLoader(idx, null, this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        Log.v(TAG, "onAttach");

        super.onAttach(activity);

        accountManager = AccountManager.get(getActivity());
        accountManager.addOnAccountsUpdatedListener(this, null, true);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SyncAdapter.START_SYNC);
        intentFilter.addAction(SyncAdapter.STOP_SYNC);
        getActivity().registerReceiver(syncReceiver, intentFilter);
    }

    @Override
    public void onDetach() {
        getActivity().unregisterReceiver(syncReceiver);
        accountManager.removeOnAccountsUpdatedListener(this);
        accountManager = null;

        super.onDetach();
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
        // the old cursor once we return.)
        getCursorAdapter(loader).swapCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    private CursorAdapter getCursorAdapter(Loader<Cursor> loader) {
        ListAdapter adapter = mAdapter.getPiece(loader.getId() * 2 + 1);
        while (adapter instanceof AdapterWrapper) {
            adapter = ((AdapterWrapper) adapter).getWrappedAdapter();
        }
        if (adapter instanceof CursorAdapter) {
            return (CursorAdapter) adapter;
        }
        return null;
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

    void syncStateChanged(boolean syncActive) {
        Log.v(TAG, "syncStateChanged: " + syncActive);
        mAdapter.notifyDataSetChanged();
    }

    private static final class BankAccountsAdapter extends ResourceCursorAdapter {

        private Account account;

        private BankAccountsAdapter(Context context, Account account, int layout, Cursor c,
                int flags) {
            super(context, layout, c, flags);
            this.account = account;

        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {
            Cursor oldCursor = super.swapCursor(newCursor);
            Log.d(TAG, "swaping cursor");
            if (oldCursor != null) {
                Log.d(TAG, String.format("old cursor size=%d", oldCursor.getCount()));
            }
            if (newCursor != null) {
                Log.d(TAG, String.format("new cursor size=%d", newCursor.getCount()));
            }
            return oldCursor;
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

    private static final class EmptyBankAccountsAdapter extends AdapterWrapper {

        private Account account;
        private Context context;

        public EmptyBankAccountsAdapter(Context context, Account account, ListAdapter wrapped) {
            super(wrapped);
            this.account = account;
            this.context = context;
        }

        @Override
        public int getCount() {
            int size = getWrappedAdapter().getCount();
            if (size > 0) {
                return size;
            }
            return 1;
        }

        @Override
        public int getViewTypeCount() {
            return getWrappedAdapter().getViewTypeCount() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (getWrappedAdapter().getCount() > 0) {
                return getWrappedAdapter().getItemViewType(position);
            } else {
                return getWrappedAdapter().getViewTypeCount();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position == 0 && getWrappedAdapter().getCount() == 0) {
                TextView view;
                if (convertView != null && convertView instanceof TextView) {
                    view = (TextView) convertView;
                } else {
                    view = new TextView(context);
                }

                // view.setLayoutParams(new ViewGroup.LayoutParams(
                // ViewGroup.LayoutParams.MATCH_PARENT,
                // ViewGroup.LayoutParams.WRAP_CONTENT));

                if (ContentResolver.isSyncActive(account, BankingContract.AUTHORITY)) {
                    view.setText("syncing...");
                } else {
                    view.setText("no accounts");
                }

                return view;
            }
            return super.getView(position, convertView, parent);
        }

    }

    private static final class MyMergeAdapter extends MergeAdapter {

        ListAdapter getPiece(int index) {
            return pieces.get(index);
        }

    }

    final BroadcastReceiver syncReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (SyncAdapter.START_SYNC.equals(intent.getAction())) {
                syncStateChanged(true);
            } else if (SyncAdapter.STOP_SYNC.equals(intent.getAction())) {
                syncStateChanged(false);
            }
        }
    };
}
