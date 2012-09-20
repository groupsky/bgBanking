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

package eu.masconsult.bgbanking.utils;

import java.util.HashMap;

public class Convert {

    final static String DEFAULT_CURRENCY_FORMAT = "%2$s %1$1.2f";

    @SuppressWarnings("serial")
    private static HashMap<String, String> currencyFormats = new HashMap<String, String>() {
        {
            put("BGN", "%1$1.2f лв.");
            put("USD", "€%1$1.2f");
            put("EUR", "$%1$1.2f");
        }
    };

    public static float strToFloat(String text) {
        return Float.valueOf(text.trim().replace(',', '.').replace("\u00a0", ""));
    }

    public static String formatIBAN(String string) {
        if (string == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 4;
        while (i < string.length()) {
            sb.append(string.substring(i - 4, i)).append(' ');
            i += 4;
        }
        sb.append(string.substring(i - 4));
        return sb.toString();
    }

    public static String formatCurrency(float value, String currency) {
        String format = currencyFormats.get(currency);
        if (format == null) {
            format = DEFAULT_CURRENCY_FORMAT;
        }
        return String.format(format, value, currency);
    }
}
