
package eu.masconsult.bgbanking.utils;

import static android.provider.BaseColumns._ID;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_AVAILABLE_BALANCE;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_BALANCE;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_CURRENCY;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_IBAN;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_LAST_TRANSACTION_DATE;
import static eu.masconsult.bgbanking.provider.BankingContract.BankAccount.COLUMN_NAME_NAME;
import android.content.Context;
import android.database.CursorWrapper;
import eu.masconsult.bgbanking.R;

public class SampleCursor extends CursorWrapper {

    static String[] currencyCodes = {
            "BGN", "EUR", "USD"
    };

    private int idx = 0;

    private Context context;

    public SampleCursor(Context context) {
        super(null);
        this.context = context;
    }

    @Override
    public int getCount() {
        return currencyCodes.length;
    }

    @Override
    public boolean moveToPosition(int position) {
        if (position < 0 || position >= currencyCodes.length) {
            return false;
        }
        idx = position;
        return true;
    }

    @Override
    public int getColumnIndex(String columnName) {
        int idx = 0;
        if (_ID.equals(columnName)) {
            return idx;
        }
        idx++;
        if (COLUMN_NAME_IBAN.equals(columnName)) {
            return idx;
        }
        idx++;
        if (COLUMN_NAME_NAME.equals(columnName)) {
            return idx;
        }
        idx++;
        if (COLUMN_NAME_CURRENCY.equals(columnName)) {
            return idx;
        }
        idx++;
        if (COLUMN_NAME_BALANCE.equals(columnName)) {
            return idx;
        }
        idx++;
        if (COLUMN_NAME_AVAILABLE_BALANCE.equals(columnName)) {
            return idx;
        }
        idx++;
        if (COLUMN_NAME_LAST_TRANSACTION_DATE.equals(columnName)) {
            return idx;
        }
        idx++;
        return -1;
    }

    @Override
    public boolean isNull(int columnIndex) {
        return columnIndex == 6;
    }

    @Override
    public String getString(int columnIndex) {
        switch (columnIndex) {
        // COLUMN_NAME_IBAN,
            case 1:
                return "BG01ABCD12345678901234";
                // COLUMN_NAME_NAME,
            case 2:
                return context.getString(R.string.sample_account_name);
                // COLUMN_NAME_CURRENCY,
            case 3:
                return currencyCodes[idx];
                // COLUMN_NAME_BALANCE
        }
        return "";
    }

    @Override
    public float getFloat(int columnIndex) {
        switch (columnIndex) {
        // COLUMN_NAME_BALANCE,
            case 4:
                return 1234567.89f;
                // COLUMN_NAME_AVAILABLE_BALANCE,
            case 5:
                return 1234567.89f;
        }
        return -1;
    }

    @Override
    public long getLong(int columnIndex) {
        switch (columnIndex) {
        // _ID,
            case 0:
                return idx;
                // COLUMN_NAME_IBAN,
        }
        return -1;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean requery() {
        return true;
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        int index = getColumnIndex(columnName);
        if (index == -1) {
            throw new IllegalArgumentException("Can't find " + columnName);
        }
        return index;
    }
}
