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

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

import com.google.analytics.tracking.android.EasyTracker;

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key="
        + BankingApplication.BUGSENSE_KEY, formKey = "")
public class BankingApplication extends Application {

    public static final String BUGSENSE_KEY = "2ce1318e";

    public static final String TAG = "bgB.";

    @Override
    public void onCreate() {
        ACRA.init(this);

        super.onCreate();

        EasyTracker.getInstance().setContext(this);
    }
}
