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

public final class Constants {

    private static final int AUTHORITY_RESOURCE = R.string.authority_type;
    private static String authorityType = null;

    public static String getAuthorityType(Context context) {
        if (authorityType != null) {
            return authorityType;
        }
        return authorityType = context.getString(AUTHORITY_RESOURCE);
    }
}
