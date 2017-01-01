package antl.pataky.sk.familyshopping.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import antl.pataky.sk.familyshopping.utils.LoginResponse;
import antl.pataky.sk.familyshopping.R;
import antl.pataky.sk.familyshopping.model.User;

import static antl.pataky.sk.familyshopping.utils.Constants.*;
import static antl.pataky.sk.familyshopping.utils.SignUtils.*;

/**
 * A login screen that offers login via email/password.
 */
public class RegisterActivity extends AppCompatActivity {

    private FirebaseDatabase database;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private SignUpTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptSignUp();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignUpButton = (Button) findViewById(R.id.email_sign_up_button);
        mEmailSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignUp();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        database = FirebaseDatabase.getInstance();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptSignUp() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (!isNetworkAvailable(this)) {
            showNetworkIsNotAvailableDialog(this);
            return;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new SignUpTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        // The ViewPropertyAnimator APIs are not available, so simply show
        // and hide the relevant UI components.
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class SignUpTask extends AsyncTask<Void, Void, LoginResponse> {

        private final String mEmail;
        private final String mPassword;

        SignUpTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected LoginResponse doInBackground(Void... params) {

            final int NUMBER_OF_RETRIES = 5;
            final List<String> logins = new ArrayList<>();
            // Read from the database
            database.getReference("users/").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        logins.add(ds.getValue(User.class).getLogin());
                    }
                    Log.d("Firebase logins", "Value is: " + logins);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("Firebase logins", "Failed to read value.", error.toException());
                }
            });
            int counter = 0;
            while (logins.isEmpty() || counter < NUMBER_OF_RETRIES) {
                try {
                    counter++;
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Log.w("Waiting sleep", e.getMessage());
                }
            }
            if (!logins.contains(mEmail)) {
                DatabaseReference usersRef = database.getReference("users");
                final User newUser = new User(mEmail, mPassword);
                usersRef.push().setValue(newUser);
                try {
                    Thread.sleep(750);
                } catch (InterruptedException e) {
                    Log.w("Waiting sleep", e.getMessage());
                }
                return LoginResponse.SUCCESS;
            } else {
                return LoginResponse.USER_ALREADY_EXISTS;
            }
        }

        @Override
        protected void onPostExecute(final LoginResponse response) {
            mAuthTask = null;
            showProgress(false);

            if (response == LoginResponse.SUCCESS) {
                final Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.putExtra(USER_EMAIL, mEmail);
                intent.putExtra(USER_PASSWORD, mPassword);
                startActivity(intent);
                finish();
            } else if (response == LoginResponse.USER_ALREADY_EXISTS) {
                mEmailView.setError(getString(R.string.error_user_already_exists));
                mEmailView.requestFocus();
                mPasswordView.setText("");
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

