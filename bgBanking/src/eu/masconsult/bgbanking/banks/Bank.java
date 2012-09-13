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
import android.util.Log;
import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.R;
import eu.masconsult.bgbanking.banks.dskbank.DskClient;
import eu.masconsult.bgbanking.banks.procreditbank.ProcreditClient;
import eu.masconsult.bgbanking.banks.sgexpress.SGExpressClient;

public enum Bank {

    // DSK Bank
    DSKBank(
            R.string.bank_account_type_dskbank,
            R.drawable.ic_bankicon_dskbank,
            R.string.bank_name_dskbank,
            DskClient.class),
    // ProCredit Bank
    ProCreditBank(
            R.string.bank_account_type_procreditbank,
            R.drawable.ic_bankicon_procreditbank,
            R.string.bank_name_procreditbank,
            ProcreditClient.class),
    // Societe Generale ExpressBank
    SGExpressBank(
            R.string.bank_account_type_sgexpress,
            R.drawable.ic_bankicon_sgexpress,
            R.string.bank_name_sgexpress,
            SGExpressClient.class);

    private static final String TAG = BankingApplication.TAG + "Bank";

    private final int accountTypeResource;
    public final int iconResource;
    public final int labelRes;
    private final Class<? extends BankClient> clientClass;
    private BankClient client = null;
    private String accountTypeString = null;

    private Bank(int accountTypeResource, int iconResource, int labelRes,
            Class<? extends BankClient> clientClass) {
        this.accountTypeResource = accountTypeResource;
        this.iconResource = iconResource;
        this.labelRes = labelRes;
        this.clientClass = clientClass;
    }

    public static Bank fromAccountType(Context context, String accountType) {
        if (accountType == null) {
            return null;
        }
        // TODO use hashmap to speed things up
        for (Bank bank : values()) {
            if (accountType.equals(context.getString(bank.accountTypeResource))) {
                return bank;
            }
        }
        return null;
    }

    public BankClient getClient() {
        if (client != null) {
            return client;
        }

        try {
            client = clientClass.newInstance();
        } catch (InstantiationException e) {
            Log.e(TAG, "can't create " + name() + " bank client", e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "can't create " + name() + " bank client", e);
            throw new RuntimeException(e);
        }

        return client;
    }

    public String getAccountType(Context context) {
        if (accountTypeString == null) {
            accountTypeString = context.getString(accountTypeResource);
        }
        return accountTypeString;
    }
}
