
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
