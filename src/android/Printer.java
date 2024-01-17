/*
    Copyright 2013-2016 appPlant GmbH

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

package de.appplant.cordova.plugin.printer;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrinterId;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.appplant.cordova.plugin.printer.ext.PrintManager;
import de.appplant.cordova.plugin.printer.ext.PrintManager.OnPrintJobStateChangeListener;
import de.appplant.cordova.plugin.printer.ext.PrintServiceInfo;
import de.appplant.cordova.plugin.printer.reflect.Meta;
import de.appplant.cordova.plugin.printer.ui.SelectPrinterActivity;
import ordev.pos.placeorder.MainActivity;

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
import android.text.Layout;
import android.Manifest;

import android.text.TextUtils;
import android.util.Log;

import android.app.AlertDialog;

import android.view.LayoutInflater;

import android.app.ProgressDialog;

import android.content.pm.PackageManager;

import java.io.IOException;

import com.elotouch.AP80.sdkhelper.AP80PrintHelper;

//import com.google.zxing.client.android.CaptureActivity;
/**
 * Plugin to print HTML documents. Therefore it creates an invisible web view
 * that loads the markup data. Once the page has been fully rendered it takes
 * the print adapter of that web view and initializes a print job.
 */
public class Printer extends CordovaPlugin{

    /**
     * The web view that loads all the content.
     */
    private WebView view;

    /**
     * Reference is necessary to invoke the callback in the onresume event.
     */
    private CallbackContext command;

    /**
     * Instance of the print manager to listen for job status changes.
     */
    private PrintManager pm;

    /**
     * Invokes the callback once the job has reached a final state.
     */
    private OnPrintJobStateChangeListener listener = new OnPrintJobStateChangeListener() {
        /**
         * Callback notifying that a print job state changed.
         *
         * @param job The print job.
         */
        @Override
        public void onPrintJobStateChanged(PrintJob job) {
            notifyAboutPrintJobResult(job);
        }
    };

    /**
     * Default name of the printed document (PDF-Printer).
     */

    private static final String DEFAULT_DOC_NAME = "unknown";

    /**
     * Executes the request.
     *
     * This method is called from the WebView thread.
     * To do a non-trivial amount of work, use:
     *     cordova.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     *     cordova.getActivity().runOnUiThread(runnable);
     *
     * @param action   The action to execute.
     * @param args     The exec() arguments in JSON form.
     * @param callback The callback context used when calling back into JavaScript.
     * @return         Whether the action was valid.
     */


    private static final int MSG_REFRESH_SHOWRESULT = 0x11;
    private static final int MSG_REFRESH_NO_SHOWRESULT = 0x12;
    private static final int MSG_REFRESH_UPGRADING_SYSTEM = 0x13;

    private static final String TAG = "MainActivity";

    private Toast toast;
    private boolean isShowResult = false;

    private int cmdflag;

    public String scanResult1 = "";
    int count = 0;

    private AP80PrintHelper printHelper;

    @Override
    public boolean execute (String action, JSONArray args,
                            CallbackContext callback) throws JSONException {

        command = callback;

        SubLcdHelper.getInstance().SetCalBack(this::datatrigger);

        AP80PrintHelper.getInstance().initPrint(cordova.getActivity().getApplicationContext());
        printHelper = AP80PrintHelper.getInstance();
        printHelper.initPrint(cordova.getActivity().getApplicationContext());


        if (action.equalsIgnoreCase("check")) {
            check();
            return true;
        }

        if (action.equalsIgnoreCase("pick")) {
            pick();
            return true;
        }

        if (action.equalsIgnoreCase("print")) {
            print(args);
            return true;
        }
        if (action.equalsIgnoreCase("opencashBox")) {
            IminSDKManager.opencashBox();
            return true;
        }
        if (action.equalsIgnoreCase("showScan")) {
            scanResult1 = "";
            showScan(callback);
            return true;
        }


        if (action.equalsIgnoreCase("checkScanResult")) {
            checkScanResult(callback);
            return true;
        }

        if (action.equalsIgnoreCase("showReciept")) {
            showReciept(args);
            return true;
        }

        if (action.equalsIgnoreCase("printKozenData")) {
            String msg = args.getString(0);
            printKozenData(msg);
            return true;
        }

        if (action.equalsIgnoreCase("printKozenTitleData")) {
            String msg = args.getString(0);
            printKozenTitleData(msg);
            return true;
        }
        if (action.equalsIgnoreCase("printKozenSubTitleData")) {
            String msg = args.getString(0);
            printKozenSubTitleData(msg);
            return true;
        }


        return false;
    }

    private void printKozenTitleData(String msg) {
        printHelper.printData(msg, 62, 1, false, 1, 80, 0);
    }
    private void printKozenSubTitleData(String msg) {
        printHelper.printData(msg, 42, 0, false, 1, 80, 0);
    }

    private void printKozenData(String msg) {
        command.success("Done printing");
        printHelper.printData(msg, 32, 0, false, 0, 80, 0);
        printSpace(5);
        printHelper.printStart();
        printHelper.cutPaper(1);

    }

    private void printSpace(int n) {
        if (n <0) {
            return;
        }
        StringBuilder str_space = new StringBuilder();
        for (int i = 0; i < n; i++) {
            str_space.append("\n");
        }
        printHelper.printData(str_space.toString(), 32, 0, false, 1, 80, 0);
    }

      private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH_SHOWRESULT:
                    isShowResult = true;
                    SubLcdHelper.getInstance().readData();
                    mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_SHOWRESULT, 100);
                    break;
                case MSG_REFRESH_NO_SHOWRESULT:
                    isShowResult = false;
                    SubLcdHelper.getInstance().readData();
                    mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_NO_SHOWRESULT, 100);
                    break;
                case MSG_REFRESH_UPGRADING_SYSTEM:
                    //showLoading();
                    mHandler.sendEmptyMessage(MSG_REFRESH_SHOWRESULT);
                    break;
            }
            return false;
        }
    });

     public void showScan(CallbackContext callback) {

        cordova.getThreadPool().execute(new Runnable() {

            @Override
             public void run() {

                //callback.error("Testing");
                 try {
                    SubLcdHelper.getInstance().sendScan();
                    cmdflag = CMD_PROTOCOL_START_SCAN;
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_SHOWRESULT, 300);
                    callback.success("Scanner opened!");

                 } catch (SubLcdException e) {
                     String errMsg = e.getMessage();
                     e.printStackTrace();
                     callback.error(errMsg);
                 }
            }
        });
    }

    public void checkScanResult(CallbackContext callback) {

        if(scanResult1 != "" && scanResult1 != null){
            callback.success(scanResult1);
        }
    }
    public void datatrigger(String s, int cmd) {
        //command.success("Testing");
        cordova.getActivity().runOnUiThread(() -> {
            if (!TextUtils.isEmpty(s)) {

                if (cmd == cmdflag) {
                    if (cmd == CMD_PROTOCOL_UPDATE && s.equals(" data is incorrect")) {
                        // closeLoading();
                        mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                        mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                        Log.i(TAG, "datatrigger result=" + s);
                        Log.i(TAG, "datatrigger cmd=" + cmd);
                        if (isShowResult) {
                            //showtoast("update successed");
                        }
                    } else if (cmd == CMD_PROTOCOL_UPDATE && (s.equals("updatalogo") || s.equals("updatafilenameok") || s.equals("updatauImage") || s.equals("updataok"))) {
                        Log.i(TAG, "neglect");
                    } else if (cmd == CMD_PROTOCOL_UPDATE && (s.equals("Same_version"))) {
                        // closeLoading();
                        mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                        mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                        Log.i(TAG, "datatrigger result=" + s);
                        Log.i(TAG, "datatrigger cmd=" + cmd);
                        if (isShowResult) {
                            //showtoast("Same version");
                        }
                    } else {
                        mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                        mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                        Log.i(TAG, "datatrigger result=" + s);
                        Log.i(TAG, "datatrigger cmd=" + cmd);
                        scanResult1 = s;

                        if (isShowResult) {
                            command.success(scanResult1);
                        }

                    }
                }
            }
        });
    }


    public void showReciept(final JSONArray args) throws JSONException {
        String first_name =  args.getString(0);
        String last_name =  args.getString(1);
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                String text = "First Name:  "+first_name+"\n"+
                        "Last Name:  "+last_name+"\n";
                try {
                    SubLcdHelper.getInstance().sendText(text, Layout.Alignment.ALIGN_CENTER, 36);
                    cmdflag = CMD_PROTOCOL_BMP_DISPLAY;
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_NO_SHOWRESULT, 300);
                } catch (SubLcdException e) {
                    e.printStackTrace();
                }
            }
        });

    }
  
    /**
     * Informs if the device is able to print documents.
     * A Internet connection is required to load the cloud print dialog.
     */
    private void check () {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                List<PrintServiceInfo> services  = pm.getEnabledPrintServices();
                Boolean available = services.size() > 0;

                PluginResult res1 = new PluginResult(
                        PluginResult.Status.OK, available);
                PluginResult res2 = new PluginResult(
                        PluginResult.Status.OK, services.size());
                PluginResult res  = new PluginResult(
                        PluginResult.Status.OK, Arrays.asList(res1, res2));

                command.sendPluginResult(res);
            }
        });
    }

    /**
     * Loads the HTML content into the web view and invokes the print manager.
     *
     * @param args
     *      The exec arguments as JSON
     */
    private void print (final JSONArray args) {
        final String content   = args.optString(0, "<html></html>");
        final JSONObject props = args.optJSONObject(1);

        cordova.getActivity().runOnUiThread( new Runnable() {
            @Override
            public void run() {
                initWebView(props);
                loadContent(content);
            }
        });
    }

    /**
     * Presents a list with all enabled print services and invokes the
     * callback with the selected one.
     */
    private void pick () {
        Intent intent = new Intent(
            cordova.getActivity(), SelectPrinterActivity.class);

        cordova.startActivityForResult(this, intent, 0);
    }

    /**
     * Loads the content into the web view.
     *
     * @param content
     *      Either an HTML string or URI
     */
    private void loadContent(String content) {
        if (content.startsWith("http") || content.startsWith("file:")) {
            view.loadUrl(content);
        } else {
            String baseURL = webView.getUrl();
            baseURL        = baseURL.substring(0, baseURL.lastIndexOf('/') + 1);

            // Set base URI to the assets/www folder
            view.loadDataWithBaseURL(
                    baseURL, content, "text/html", "UTF-8", null);
        }
    }

    /**
     * Configures the WebView components which will call the Google Cloud Print
     * Service.
     *
     * @param props
     *      The JSON object with the containing page properties
     */
    private void initWebView (JSONObject props) {
        Activity ctx         = cordova.getActivity();
        view                 = new WebView(ctx);
        WebSettings settings = view.getSettings();
        final boolean jsEnabled = props.optBoolean("javascript", false);

        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setSaveFormData(true);
        settings.setUseWideViewPort(true);
        if (jsEnabled) {
            settings.setJavaScriptEnabled(jsEnabled);
        }
        view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        if (Build.VERSION.SDK_INT >= 21) {
            Method setMixedContentModeMethod = Meta.getMethod(
                    settings.getClass(), "setMixedContentMode", true, int.class);

            Meta.invokeMethod(settings, setMixedContentModeMethod, 2);
        }

        setWebViewClient(props);
    }

    /**
     * Creates the web view client which sets the print document.
     *
     * @param props
     *      The JSON object with the containing page properties
     */
    private void setWebViewClient (JSONObject props) {
        final String docName    = props.optString("name", DEFAULT_DOC_NAME);
        final boolean landscape = props.optBoolean("landscape", false);
        final boolean graystyle = props.optBoolean("graystyle", false);
        final String  duplex    = props.optString("duplex", "none");

        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading (WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished (WebView webView, String url) {
                PrintAttributes.Builder builder = new PrintAttributes.Builder();
                PrintDocumentAdapter adapter    = getAdapter(webView, docName);

                builder.setMinMargins(PrintAttributes.Margins.NO_MARGINS);

                builder.setColorMode(graystyle
                        ? PrintAttributes.COLOR_MODE_MONOCHROME
                        : PrintAttributes.COLOR_MODE_COLOR);

                builder.setMediaSize(landscape
                        ? PrintAttributes.MediaSize.UNKNOWN_LANDSCAPE
                        : PrintAttributes.MediaSize.UNKNOWN_PORTRAIT);

                if (!duplex.equals("none") && Build.VERSION.SDK_INT >= 23) {
                    boolean longEdge = duplex.equals("long");
                    Method setDuplexModeMethod = Meta.getMethod(
                            builder.getClass(), "setDuplexMode", int.class);

                    Meta.invokeMethod(builder, setDuplexModeMethod,
                            longEdge ? 2 : 4);
                }

                pm.getInstance().print(docName, adapter, builder.build());
                view = null;
            }
        });
    }

    /**
     * Invoke the callback send with `print` to inform about the result.
     *
     * @param job The print job.
     */
    @SuppressWarnings("ConstantConditions")
    private void notifyAboutPrintJobResult(PrintJob job) {

        if (job == null || command == null ||
                job.getInfo().getState() <= STATE_STARTED) {
            return;
        }

        PluginResult res = new PluginResult(
                PluginResult.Status.OK, job.isCompleted());

        command.sendPluginResult(res);
    }

    /**
     * Create the print document adapter for the web view component. On
     * devices older then SDK 21 it will use the deprecated method
     * `createPrintDocumentAdapter` without arguments and on newer devices
     * the recommended way.
     *
     * @param webView
     *      The web view which content to print out.
     * @param docName
     *      The name of the printed document.
     * @return
     *      The created adapter.
     */
    private PrintDocumentAdapter getAdapter (WebView webView, String docName) {
        if (Build.VERSION.SDK_INT >= 21) {
            Method createPrintDocumentAdapterMethod = Meta.getMethod(
                    WebView.class, "createPrintDocumentAdapter", String.class);

            return (PrintDocumentAdapter) Meta.invokeMethod(
                    webView, createPrintDocumentAdapterMethod, docName);
        } else {
            return (PrintDocumentAdapter) Meta.invokeMethod(webView,
                    "createPrintDocumentAdapter");
        }
    }

    /**
     * Called after plugin construction and fields have been initialized.
     */
    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        pm = new PrintManager(cordova.getActivity());
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    @Override
    public void onDestroy() {
    if(pm != null && listener != null && command != null && view != null) {
           pm       = null;
           listener = null;
           command  = null;
           view     = null;

           super.onDestroy();
    }
    }

    /**
     * Invoke the callback from `pick` method.
     *
     * @param requestCode   The request code originally supplied to
     *                      startActivityForResult(), allowing you to
     *                      identify who this result came from.
     * @param resultCode    The integer result code returned by the child
     *                      activity through its setResult().
     * @param intent        An Intent, which can return result data to the
     *                      caller (various data can be
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (command == null || intent == null) {
            return;
        }

        if (!intent.getAction().equals(ACTION_SELECT_PRINTER)) {
            return;
        }

        PrinterId printer = intent.getParcelableExtra(EXTRA_PRINTER_ID);

        PluginResult res  = new PluginResult(PluginResult.Status.OK,
                printer != null ? printer.getLocalId() : null);

        command.sendPluginResult(res);
    }
}
