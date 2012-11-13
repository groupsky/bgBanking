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
            // rowView = inflater.inflate(android.R.layout.simple_list_item_1,
            // null);
            rowView = inflater.inflate(R.layout.choose_account_type_item, null);
        }

        Bank bank = getItem(position);

        TextView labelView = (TextView) rowView.findViewById(android.R.id.text1);
        labelView.setText(getContext().getResources().getString(bank.labelRes));
        labelView.setCompoundDrawablesWithIntrinsicBounds(bank.iconResource, 0, 0, 0);
        labelView.setCompoundDrawablePadding(10);

        return rowView;
    }
}
