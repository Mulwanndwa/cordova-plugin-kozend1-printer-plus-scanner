/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package ordev.pos.placeorder;

import android.os.Bundle;

import org.apache.cordova.*;

import static android.print.PrintJobInfo.STATE_STARTED;
import static de.appplant.cordova.plugin.printer.ui.SelectPrinterActivity.ACTION_SELECT_PRINTER;
import static de.appplant.cordova.plugin.printer.ui.SelectPrinterActivity.EXTRA_PRINTER_ID;
import com.imin.library.*;

import android.os.Message;
import android.os.AsyncTask;
import android.os.Handler;
import static com.android.sublcdlibrary.SubLcdConstant.CMD_PROTOCOL_BACKLIGHT;
import static com.android.sublcdlibrary.SubLcdConstant.CMD_PROTOCOL_BMP_DISPLAY;
import static com.android.sublcdlibrary.SubLcdConstant.CMD_PROTOCOL_START_SCAN;
import static com.android.sublcdlibrary.SubLcdConstant.CMD_PROTOCOL_UPDATE;
import static com.android.sublcdlibrary.SubLcdConstant.CMD_PROTOCOL_VERSION;

import com.android.sublcdlibrary.SubLcdException;
import com.android.sublcdlibrary.SubLcdHelper;

import android.widget.Toast;

import android.Manifest;

import android.text.TextUtils;
import android.util.Log;
import android.content.pm.PackageManager;

public class MainActivity extends CordovaActivity implements SubLcdHelper.VuleCalBack {

    private static final int MSG_REFRESH_SHOWRESULT = 0x11;
    private static final int MSG_REFRESH_NO_SHOWRESULT = 0x12;
    private static final int MSG_REFRESH_UPGRADING_SYSTEM = 0x13;

    private static final String TAG = "MainActivity";

    private Toast toast;
    private boolean isShowResult = false;

    private int cmdflag;

    public static String scanResult1 = "12345";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        SubLcdHelper.getInstance().init(getApplicationContext());
    }

}
