
package eu.masconsult.bgbanking.activity;

import android.accounts.AccountManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import eu.masconsult.bgbanking.banks.Bank;

public class HomeActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkForLoggedAccounts();
    }

    protected void checkForLoggedAccounts() {
        AccountManager accountManager = AccountManager.get(this);
        Bank[] banks = Bank.values();
        String[] accountTypes = new String[banks.length];

        boolean hasAccounts = false;
        for (int i = 0; i < banks.length; i++) {
            accountTypes[i] = banks[i].getAccountType(this);
            if (accountManager.getAccountsByType(banks[i].getAccountType(this)).length > 0) {
                hasAccounts = true;
            }
        }

        if (!hasAccounts) {
            ChooseAccountTypeFragment accountTypesFragment = new ChooseAccountTypeFragment();
            accountTypesFragment.show(getSupportFragmentManager(), "AccountsDialog");
        }
    }
}
