
package eu.masconsult.bgbanking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import eu.masconsult.bgbanking.banks.Bank;

public class BankAdapter extends ArrayAdapter<Bank> {

    LayoutInflater inflater;

    public BankAdapter(Context context, Bank[] banks) {
        super(context, 0, banks);
        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            rowView = inflater.inflate(android.R.layout.simple_list_item_1, null);
        }

        Bank bank = getItem(position);

        TextView labelView = (TextView) rowView.findViewById(android.R.id.text1);
        labelView.setText(getContext().getResources().getString(bank.labelRes));
        labelView.setCompoundDrawablesWithIntrinsicBounds(bank.iconResource, 0, 0, 0);
        labelView.setCompoundDrawablePadding(10);

        return rowView;
    }
}
