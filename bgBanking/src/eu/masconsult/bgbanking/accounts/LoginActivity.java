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

package eu.masconsult.bgbanking.accounts;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
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
import eu.masconsult.bgbanking.ui.LightProgressDialog;

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
            case R.id.next_button:
                signIn();
                break;
        }
    }

    private void signIn() {
        final AlertDialog dialog = LightProgressDialog.create(this,
                R.string.login_activity_authenticating);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
            }
        });
        dialog.show();
    }

    private void validateInputs() {
        nextBtn.setEnabled(!TextUtils.isEmpty(usernameEdit.getText())
                && !TextUtils.isEmpty(passwordEdit.getText()));
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
