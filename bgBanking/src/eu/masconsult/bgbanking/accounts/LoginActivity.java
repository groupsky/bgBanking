
package eu.masconsult.bgbanking.accounts;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.R;

public class LoginActivity extends AccountAuthenticatorActivity {

    private static final String TAG = BankingApplication.TAG + "LoginAct";
    private String accountType;
    private ImageView logo;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.v(TAG, "onCreate(" + icicle + ")");

        super.onCreate(icicle);

        setContentView(R.layout.activity_login);

        accountType = getIntent().getExtras().getString(AccountManager.KEY_ACCOUNT_TYPE);

        logo = (ImageView) findViewById(R.id.logo);
        if (getString(R.string.bank_account_type_dskbank).equals(accountType)) {
            logo.setImageResource(R.drawable.ic_bankicon_dskbank);
        } else if (getString(R.string.bank_account_type_procreditbank).equals(accountType)) {
            logo.setImageResource(R.drawable.ic_bankicon_procreditbank);
        }
    }

}
