
package eu.masconsult.bgbanking.accounts;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.R;
import eu.masconsult.bgbanking.banks.Bank;

public class LoginActivity extends AccountAuthenticatorActivity {

    private static final String TAG = BankingApplication.TAG + "LoginAct";
    private String accountType;
    private ImageView logo;
    private Bank bank;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.v(TAG, "onCreate(" + icicle + ")");

        super.onCreate(icicle);

        setContentView(R.layout.activity_login);

        accountType = getIntent().getExtras().getString(AccountManager.KEY_ACCOUNT_TYPE);
        bank = Bank.fromAccountType(this, accountType);

        logo = (ImageView) findViewById(R.id.logo);
        logo.setImageResource(bank.iconResource);
    }

}
