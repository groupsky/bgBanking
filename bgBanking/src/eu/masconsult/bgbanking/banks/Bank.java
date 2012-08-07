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

package eu.masconsult.bgbanking.banks;

import android.content.Context;
import eu.masconsult.bgbanking.R;

public enum Bank {

    // DSK Bank
    DSKBank(R.string.bank_account_type_dskbank, R.drawable.ic_bankicon_dskbank),
    // ProCredit Bank
    ProCreditBank(R.string.bank_account_type_procreditbank, R.drawable.ic_bankicon_procreditbank);

    public final int accountTypeResource;
    public final int iconResource;

    private Bank(int accountTypeResource, int iconResource) {
        this.accountTypeResource = accountTypeResource;
        this.iconResource = iconResource;
    }

    public static Bank fromAccountType(Context context, String accountType) {
        if (accountType == null) {
            return null;
        }
        for (Bank bank : values()) {
            if (accountType.equals(context.getString(bank.accountTypeResource))) {
                return bank;
            }
        }
        return null;
    }
}
