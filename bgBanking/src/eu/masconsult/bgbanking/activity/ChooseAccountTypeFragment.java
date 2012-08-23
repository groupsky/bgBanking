
package eu.masconsult.bgbanking.activity;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import eu.masconsult.bgbanking.BankAdapter;
import eu.masconsult.bgbanking.R;
import eu.masconsult.bgbanking.banks.Bank;

public class ChooseAccountTypeFragment extends DialogFragment implements OnItemClickListener {

    BankAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bank[] banks = Bank.values();
        adapter = new BankAdapter(getActivity(), banks);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_account_type, container, false);

        ListView accountsList = (ListView) view.findViewById(R.id.account_types_list);
        accountsList.setAdapter(adapter);
        accountsList.setOnItemClickListener(this);

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.choose_account_type);
        return dialog;
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        AccountManager.get(getActivity()).addAccount(
                adapter.getItem(position).getAccountType(getActivity()), null,
                null, null, getActivity(), null, null);
    }

}
