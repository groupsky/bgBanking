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
