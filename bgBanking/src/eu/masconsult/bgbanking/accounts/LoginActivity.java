
package eu.masconsult.bgbanking.accounts;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import eu.masconsult.bgbanking.BankingApplication;
import eu.masconsult.bgbanking.R;
import eu.masconsult.bgbanking.banks.Bank;

public class LoginActivity extends AccountAuthenticatorActivity implements OnClickListener,
        TextWatcher {

    private static final String TAG = BankingApplication.TAG + "LoginAct";
    private String accountType;
    private ImageView logoImage;
    private Bank bank;
    private View backBtn;
    private View nextBtn;
    private TextView messageView;
    private TextView usernameFixedView;
    private EditText usernameEdit;
    private EditText passwordEdit;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.v(TAG, "onCreate(" + icicle + ")");

        super.onCreate(icicle);

        setContentView(R.layout.activity_login);

        accountType = getIntent().getExtras().getString(AccountManager.KEY_ACCOUNT_TYPE);
        bank = Bank.fromAccountType(this, accountType);

        logoImage = (ImageView) findViewById(R.id.logo);
        logoImage.setImageResource(bank.iconResource);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(this);

        nextBtn = findViewById(R.id.next_button);
        nextBtn.setOnClickListener(this);
        nextBtn.setEnabled(false);

        messageView = (TextView) findViewById(R.id.message);
        usernameFixedView = (TextView) findViewById(R.id.username_fixed);
        usernameEdit = (EditText) findViewById(R.id.username_edit);
        passwordEdit = (EditText) findViewById(R.id.password_edit);

        usernameEdit.addTextChangedListener(this);
        passwordEdit.addTextChangedListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_button:
                finish();
                break;
        }
    }

    private void validateInputs() {
        nextBtn.setEnabled(usernameEdit.getText().length() > 0
                && passwordEdit.getText().length() > 0);
    }

    @Override
    public void afterTextChanged(Editable s) {
        validateInputs();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // we don't care about this
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // we don't care about this
    }

}
