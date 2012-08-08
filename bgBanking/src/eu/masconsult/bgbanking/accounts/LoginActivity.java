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

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
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
import eu.masconsult.bgbanking.Constants;
import eu.masconsult.bgbanking.R;
import eu.masconsult.bgbanking.banks.Bank;
import eu.masconsult.bgbanking.ui.LightProgressDialog;

public class LoginActivity extends AccountAuthenticatorActivity implements Constants,
        OnClickListener, TextWatcher {

    private static final String TAG = BankingApplication.TAG + "LoginAct";

    /**
     * Sync period in seconds, currently every 8 hours
     */
    private static final long SYNC_PERIOD = 8L * 60L * 60L;

    private String accountType;
    private ImageView logoImage;
    private Bank bank;
    private View backBtn;
    private View nextBtn;
    private TextView messageView;
    private TextView usernameFixedView;
    EditText usernameEdit;
    EditText passwordEdit;
    AsyncTask<String, Void, String> authenticationTask;
    private String username;
    private String password;

    /**
     * Was the original caller asking for an entirely new account?
     */
    protected boolean requestNewAccount = false;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.v(TAG, "onCreate(" + icicle + ")");

        super.onCreate(icicle);

        setContentView(R.layout.activity_login);

        Intent intent = getIntent();
        username = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);

        bank = Bank.fromAccountType(this, accountType);
        requestNewAccount = username == null;

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

        if (!requestNewAccount) {
            usernameFixedView.setVisibility(View.VISIBLE);
            usernameEdit.setVisibility(View.GONE);
            usernameFixedView.setText(username);
        }
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
        messageView.setVisibility(View.GONE);
        if (requestNewAccount) {
            username = usernameEdit.getText().toString();
        }
        password = passwordEdit.getText().toString();

        authenticationTask = new AsyncTask<String, Void, String>() {

            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                dialog = LightProgressDialog.create(LoginActivity.this,
                        R.string.login_activity_authenticating);
                dialog.setCancelable(true);
                dialog.setOnCancelListener(new OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (authenticationTask != null) {
                            authenticationTask.cancel(true);
                        }
                    }
                });
                dialog.show();
            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    return bank.getClient().authenticate(params[0], params[1]);
                } catch (Exception e) {
                    Log.e(TAG, "authentication failed", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String authToken) {
                dialog.dismiss();
                // On a successful authentication, call back into the Activity
                // to communicate the authToken (or null for an error).
                onAuthenticationResult(authToken);
            }

            @Override
            protected void onCancelled() {
                dialog.dismiss();
                // If the action was canceled (by the user clicking the cancel
                // button in the progress dialog), then call back into the
                // activity to let it know.
                onAuthenticationCancel();
            }

        };
        authenticationTask.execute(username, password);
    }

    private void validateInputs() {
        nextBtn.setEnabled((!requestNewAccount || !TextUtils.isEmpty(usernameEdit.getText()))
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

    /**
     * Called when the authentication process completes (see attemptLogin()).
     * 
     * @param authToken the authentication token returned by the server, or NULL
     *            if authentication failed.
     */
    public void onAuthenticationResult(String authToken) {

        boolean success = authToken != null && authToken.length() > 0;
        Log.i(TAG, "onAuthenticationResult(" + success + ")");

        // Our task is complete, so clear it out
        authenticationTask = null;

        if (success) {
            finishLogin(authToken);
        } else {
            Log.e(TAG, "onAuthenticationResult: failed to authenticate");
            if (requestNewAccount) {
                // "Please enter a valid username/password.
                messageView.setText(R.string.login_activity_loginfail_text_both);
            } else {
                // "Please enter a valid password." (Used when the
                // account is already in the database but the password
                // doesn't work.)
                messageView.setText(R.string.login_activity_loginfail_text_pwonly);
            }
            messageView.setVisibility(View.VISIBLE);
        }
    }

    public void onAuthenticationCancel() {
        Log.i(TAG, "onAuthenticationCancel()");

        // Our task is complete, so clear it out
        authenticationTask = null;
    }

    /**
     * Called when response is received from the server for authentication
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller. Also sets
     * the authToken in AccountManager for this account.
     */
    protected void finishLogin(String authToken) {
        AccountManager ac = AccountManager.get(this);
        final Account account = new Account(username, accountType);
        if (requestNewAccount) {
            ac.addAccountExplicitly(account, password, null);
            configureSyncFor(account);
        } else {
            ac.setPassword(account, password);
        }
        ac.setAuthToken(account, accountType, authToken);

        final Intent intent = new Intent();
        intent.putExtra(KEY_ACCOUNT_NAME, username);
        intent.putExtra(KEY_ACCOUNT_TYPE, accountType);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void configureSyncFor(Account account) {
        Log.d(TAG, "Configuring account sync");

        String authorityType = getString(AUTHORITY_RESOURCE);
        ContentResolver.setIsSyncable(account, authorityType, 1);
        ContentResolver.setSyncAutomatically(account, authorityType, true);
        ContentResolver.addPeriodicSync(account, authorityType, new Bundle(), SYNC_PERIOD);
    }
}
