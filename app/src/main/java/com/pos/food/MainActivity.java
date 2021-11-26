package com.pos.food;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.amazonaws.amplify.generated.graphql.ListOrdersQuery;
import com.amazonaws.amplify.generated.graphql.ListRestaurantsQuery;
import com.amazonaws.amplify.generated.graphql.OnCreateOrderSubscription;
import com.amazonaws.amplify.generated.graphql.OnUpdateOrderSubscription;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.navigation.NavigationView;
import com.pos.food.Adapters.AcceptedOrdersAdapter;
import com.pos.food.Adapters.NewOrdersAdapter;
import com.zcs.sdk.ConnectTypeEnum;
import com.zcs.sdk.DriverManager;
import com.zcs.sdk.Printer;
import com.zcs.sdk.SdkResult;
import com.zcs.sdk.Sys;
import com.zcs.sdk.print.PrnStrFormat;
import com.zcs.sdk.print.PrnTextFont;
import com.zcs.sdk.print.PrnTextStyle;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

public class
MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private AWSAppSyncClient mAWSAppSyncClient;
    private List<ListOrdersQuery.Item> orderslist = new ArrayList<>();
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;

    private static final int REQ_CODE_READ_PHONE = 0x01;
    private static final int REQ_CODE_PERMISSIONS = 0x02;
    private static final String TAG = "SettingsFragment";

    private ProgressDialog mDialogInit;
    private PermissionsManager mPermissionsManager;
    private final String[] mPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    NewOrdersAdapter adapter;
    ListView orderslistview;
    private AppSyncSubscriptionCall<OnCreateOrderSubscription.Data> subscriptionWatcher;

    private DriverManager mDriverManager;

    private Sys mSys;

    private Printer mPrinter;

    NavigationView navigationView;
    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        drawerLayout = findViewById(R.id.my_drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();
        orderslistview = findViewById(R.id.orderslistview);
        queryOrders();
        subscribe();

        adapter = new NewOrdersAdapter(MainActivity.this, orderslist);
        orderslistview.setAdapter(adapter);


        mActivity = MainActivity.this;

        mDriverManager = DriverManager.getInstance();

        mSys = mDriverManager.getBaseSysDevice();

        mPrinter = mDriverManager.getPrinter();


        initSdk();


    }


    private void checkPermission() {
        mPermissionsManager = new PermissionsManager(mActivity) {
            @Override
            public void authorized(int requestCode) {
                if (requestCode == REQ_CODE_READ_PHONE) {
                    initSdk();
                }
            }

            @Override
            public void noAuthorization(int requestCode, String[] lacksPermissions) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setTitle("Warning");
                builder.setMessage("Please open permission");
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionsManager.startAppSettings(mActivity.getApplicationContext());
                    }
                });
                builder.create().show();
            }

            @Override
            public void ignore(int requestCode) {
                if (requestCode == REQ_CODE_READ_PHONE) {
                    initSdk();
                }
            }
        };
        mPermissionsManager.checkPermissions(MainActivity.this, REQ_CODE_READ_PHONE, Manifest.permission.READ_PHONE_STATE);
        mPermissionsManager.checkPermissions(MainActivity.this, REQ_CODE_PERMISSIONS, mPermissions);
    }
    void initSdk() {
        initSdk(true);
    }
    int speed = 460800;
    int count = 0;
    private void initSdk(final boolean reset) {
        // Config the SDK base info
      //  mSys.showLog(getPreferenceManager().getSharedPreferences().getBoolean(getString(R.string.key_show_log), true));
        if (mDialogInit == null) {
            mDialogInit = (ProgressDialog) DialogUtils.showProgress(MainActivity.this, "waiting", "Initializing");
        } else if (!mDialogInit.isShowing()) {
            mDialogInit.show();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int statue = mSys.getFirmwareVer(new String[1]);
                if (statue != SdkResult.SDK_OK) {
                    int sysPowerOn = mSys.sysPowerOn();
                    Log.i(TAG, "sysPowerOn: " + sysPowerOn);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                mSys.setUartSpeed(speed);
                final int i = mSys.sdkInit();
                if (i == SdkResult.SDK_OK) {
                    setDeviceInfo();
                }
                if (reset && ++count < 2 && i == SdkResult.SDK_OK && mSys.getCurSpeed() != 460800) {
                    Log.d(TAG, "switch baud rate, cur speed = " + mSys.getCurSpeed());
                    int ret = mSys.setDeviceBaudRate();
                    if (ret != SdkResult.SDK_OK) {
                        DialogUtils.show(MainActivity.this, "SwitchBaudRate error: " + ret);
                    }
                    mSys.sysPowerOff();
                    initSdk();
                    return;
                }
                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mDialogInit != null)
                                mDialogInit.dismiss();
                            Log.d(TAG, "Cur speed: " + mSys.getCurSpeed());
                            if (BuildConfig.DEBUG && mSys.getConnectType() == ConnectTypeEnum.COM) {
                                DialogUtils.show(MainActivity.this, "Cur speed: " + mSys.getCurSpeed());
                            }
                           // String initRes = (i == SdkResult.SDK_OK) ? getString(R.string.init_success) : SDK_Result.obtainMsg(mActivity, i);

                           // Toast.makeText(MainActivity.this, initRes, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void subscribe() {
        OnCreateOrderSubscription subscription1 =OnCreateOrderSubscription.builder().build();
        subscriptionWatcher = mAWSAppSyncClient.subscribe(subscription1);
        subscriptionWatcher.execute(subCallback);
    }


    public void printMatrixText(final int fontsStyle,String resName, String note, String createdAt, String foodItems, String total, String customerName, String customerID, String address) {
        Log.e("cominghere","here");
        new Thread(new Runnable() {
            @Override
            public void run() {
                AssetManager asm = mActivity.getAssets();
                InputStream inputStream = null;
                try {
                    inputStream = asm.open("food_by_home.bmp");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Drawable d = Drawable.createFromStream(inputStream, null);
                Bitmap bitmap = ((BitmapDrawable) d).getBitmap();

                int printStatus = mPrinter.getPrinterStatus();
                if (printStatus == SdkResult.SDK_PRN_STATUS_PAPEROUT) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DialogUtils.show(mActivity, "Out of paper");

                        }
                    });
                } else {

                    mPrinter.setPrintAppendBitmap(bitmap, Layout.Alignment.ALIGN_CENTER);
                    PrnStrFormat format = new PrnStrFormat();
                    mPrinter.setPrintAppendString("", format);
                    mPrinter.setPrintAppendString("", format);
                    format.setTextSize(20);
                    format.setAli(Layout.Alignment.ALIGN_CENTER);
                    //format.setStyle(PrnTextStyle.BOLD);
                    if (fontsStyle == 0) {
                        format.setFont(PrnTextFont.DEFAULT);
                    } else {
                        format.setFont(PrnTextFont.CUSTOM);
                        format.setPath(Environment.getExternalStorageDirectory() + "/fonts/fangzhengyouyuan.ttf");
                    }
                    mPrinter.setPrintAppendString(resName, format);
                    mPrinter.setPrintAppendString("--------------------------------------------------------------", format);

                    format.setTextSize(40);
                    format.setAli(Layout.Alignment.ALIGN_CENTER);
                    format.setStyle(PrnTextStyle.BOLD);
                    mPrinter.setPrintAppendString("Planlagt levering", format);


                    format.setTextSize(36);
                    format.setAli(Layout.Alignment.ALIGN_CENTER);
                    format.setStyle(PrnTextStyle.NORMAL);

                    /*String date = createdAt.substring(0, createdAt.length()-14);
                    String time = createdAt.substring(11, createdAt.length()-5);

                    String inputPattern = "yyyy-MM-dd HH:mm:ss";
                    String outputPattern = "dd-MMM-yyyy h:mm a";
                    SimpleDateFormat inputFormat = new SimpleDateFormat(inputPattern);
                    SimpleDateFormat outputFormat = new SimpleDateFormat(outputPattern);

                    Date dateFinal = null;
                    String cAt = null;

                    try {
                        dateFinal = inputFormat.parse(date+" "+time);
                        cAt = outputFormat.format(dateFinal);
                        //Toast.makeText(mContext, ""+str, Toast.LENGTH_SHORT).show();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }*/

                    String datee = createdAt.substring(0, createdAt.length()-14);
                    String timee = createdAt.substring(11, createdAt.length()-5);

                    String inputPatternn = "yyyy-MM-dd";
                    String outputPatternn = "dd-MMM";
                    SimpleDateFormat inputFormatt = new SimpleDateFormat(inputPatternn);
                    SimpleDateFormat outputFormatt = new SimpleDateFormat(outputPatternn);

                    Date dateFinall = null;
                    String cADate = null;

                    try {
                        dateFinall = inputFormatt.parse(datee);
                        cADate = outputFormatt.format(dateFinall);
                        //Toast.makeText(mContext, ""+str, Toast.LENGTH_SHORT).show();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    String inputPatternnn = "HH:mm:ss";
                    String outputPatternnn = "HH-mm";
                    SimpleDateFormat inputFormattt = new SimpleDateFormat(inputPatternnn);
                    SimpleDateFormat outputFormattt = new SimpleDateFormat(outputPatternnn);

                    Date timeFinal = null;
                    String cATime = null;

                    try {
                        timeFinal = inputFormattt.parse(timee);
                        cATime = outputFormattt.format(timeFinal);
                        //Toast.makeText(mContext, ""+str, Toast.LENGTH_SHORT).show();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }



                    mPrinter.setPrintAppendString(cADate+", At "+cATime, format);


                    format.setTextSize(35);
                    format.setAli(Layout.Alignment.ALIGN_CENTER);
                    format.setStyle(PrnTextStyle.NORMAL);
                    mPrinter.setPrintAppendString("Ordrenummer: 74681776", format);


                    format.setTextSize(20);
                    mPrinter.setPrintAppendString("--------------------------------------------------------------", format);

                    format.setAli(Layout.Alignment.ALIGN_NORMAL);
                    format.setStyle(PrnTextStyle.NORMAL);
                    mPrinter.setPrintAppendString("Restaurant Noter", format);

                    format.setAli(Layout.Alignment.ALIGN_NORMAL);
                    format.setTextSize(34);
                    format.setStyle(PrnTextStyle.NORMAL);
                    mPrinter.setPrintAppendString(note, format);
                   // mPrinter.setPrintAppendString("Kontaktfri levering! Ma jeg bede om ekstra karryketchup og ra log pa polsemix. Ma jeg oksa bede om hvidlogsdip til crispy chicken i stedet for chilimayo, Tak for altid gosd service hos jer!", format);

                    format.setTextSize(20);
                    mPrinter.setPrintAppendString("--------------------------------------------------------------", format);

                    format.setTextSize(23);
                    format.setStyle(PrnTextStyle.BOLD);

                    StringBuilder stringBuilder0=new StringBuilder();
                    Formatter formatter0=new Formatter(stringBuilder0);
                    formatter0.format("%1$-7s %2$-27s %3$9s", "       ", "                           ", "      DKK");


                    format.setTextSize(20);
                    format.setAli(Layout.Alignment.ALIGN_NORMAL);
                    format.setStyle(PrnTextStyle.NORMAL);
                    try {
                        JSONArray valarray = new JSONArray(foodItems);
                        for (int i = 0; i < valarray.length(); i++) {

                            String qty = valarray.getJSONObject(i).getString("qty");
                           // mPrinter.setPrintAppendString("Quantity: "+qty, format);
                            String name = valarray.getJSONObject(i).getString("name");
                            String itemPrice = valarray.getJSONObject(i).getString("itemPrice");

                            int l = name.length();
                            int ll = l-15;

                            mPrinter.setPrintAppendString(qty+" x "+""+name+""+ "                                 "+itemPrice, format);

                            String bprice = valarray.getJSONObject(i).getString("BasketPrice");
                           // mPrinter.setPrintAppendString("Basket price: "+bprice, format);


                            //mPrinter.setPrintAppendString("Price: "+itemPrice+"kr", format);

                            //String des = valarray.getJSONObject(i).getString("discription");
                            //mPrinter.setPrintAppendString("Description: "+des, format);

                            // Toast.makeText(MainActivity.this, ""+str, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON", "There was an error parsing the JSON", e);
                    }



                    StringBuilder stringBuilder8=new StringBuilder();
                    Formatter formatter8=new Formatter(stringBuilder8);
                    formatter8.format("%1$-27s %2$-7s %3$9s", "Total                                 ","                             ", total+" kr");
                    mPrinter.setPrintAppendString(stringBuilder8.toString(), format);

                    //StringBuilder stringBuilder9=new StringBuilder();
                   // Formatter formatter9=new Formatter(stringBuilder9);
                   // formatter9.format("%1$-27s %2$-7s %3$9s", "Brtalt med:                ","       ", "         ");
                   // mPrinter.setPrintAppendString(stringBuilder9.toString(), format);

                    //StringBuilder stringBuilder10=new StringBuilder();
                   // Formatter formatter10=new Formatter(stringBuilder10);
                   // formatter10.format("%1$-27s %2$-7s %3$9s", "Kort ************4732     ","       ", "   375,89");
                 //   mPrinter.setPrintAppendString(stringBuilder10.toString(), format);

                    format.setTextSize(20);
                    mPrinter.setPrintAppendString("--------------------------------------------------------------", format);


                    format.setTextSize(38);
                    format.setAli(Layout.Alignment.ALIGN_CENTER);
                    format.setStyle(PrnTextStyle.BOLD);
                    mPrinter.setPrintAppendString("ORDREN ER BETALT", format);


                    format.setTextSize(20);
                    mPrinter.setPrintAppendString("--------------------------------------------------------------", format);


                    format.setTextSize(40);
                    format.setAli(Layout.Alignment.ALIGN_CENTER);
                    format.setStyle(PrnTextStyle.BOLD);
                    mPrinter.setPrintAppendString("KONTAKTFRI LEVERING", format);
                    format.setTextSize(17);
                    mPrinter.setPrintAppendString("Husk at lase Kommentarfeltet", format);

                    format.setTextSize(20);
                    mPrinter.setPrintAppendString("--------------------------------------------------------------", format);


                    format.setTextSize(17);
                    format.setAli(Layout.Alignment.ALIGN_NORMAL);
                   // mPrinter.setPrintAppendString("Kundenummer   1119608483", format);
                    mPrinter.setPrintAppendString(" ", format);
                    mPrinter.setPrintAppendString("Kundedetaljer:", format);

                    format.setTextSize(40);
                    format.setAli(Layout.Alignment.ALIGN_NORMAL);
                    format.setStyle(PrnTextStyle.BOLD);

                    mPrinter.setPrintAppendString( customerName, format);

                    //mPrinter.setPrintAppendString( customerID, format);

                    String[] add = address.split(",");

                    for(String addList : add){
                        mPrinter.setPrintAppendString(addList, format);
                    }

                    format.setTextSize(17);
                    mPrinter.setPrintAppendString("For at Kontakte Kunden ring", format);
                    format.setTextSize(40);
                    format.setAli(Layout.Alignment.ALIGN_NORMAL);
                    format.setStyle(PrnTextStyle.BOLD);
                    mPrinter.setPrintAppendString("78 76 89 33", format);


                  //  format.setTextSize(17);
                   // mPrinter.setPrintAppendString("VerificeringsKode", format);
                    /*format.setTextSize(40);
                    format.setAli(Layout.Alignment.ALIGN_NORMAL);
                    format.setStyle(PrnTextStyle.BOLD);
                    mPrinter.setPrintAppendString("794 217 214", format);*/

                    format.setTextSize(20);
                    mPrinter.setPrintAppendString(" ", format);
                    mPrinter.setPrintAppendString(" ", format);
                    mPrinter.setPrintAppendString("--------------------------------------------------------------", format);

                   // format.setTextSize(17);
                    //mPrinter.setPrintAppendString("Ordrer fra denne kunde:  10", format);
                  //  mPrinter.setPrintAppendString("Just Eat-ordrer fra kunden:  151", format);


                    format.setTextSize(20);
                    mPrinter.setPrintAppendString(" ", format);

                    /*String datee = createdAt.substring(0, createdAt.length()-14);
                    String timee = createdAt.substring(11, createdAt.length()-5);

                    String inputPatternn = "yyyy-MM-dd";
                    String outputPatternn = "dd-MMM";
                    SimpleDateFormat inputFormatt = new SimpleDateFormat(inputPatternn);
                    SimpleDateFormat outputFormatt = new SimpleDateFormat(outputPatternn);

                    Date dateFinall = null;
                    String cADate = null;

                    try {
                        dateFinall = inputFormatt.parse(datee);
                        cADate = outputFormatt.format(dateFinall);
                        //Toast.makeText(mContext, ""+str, Toast.LENGTH_SHORT).show();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    String inputPatternnn = "HH:mm:ss";
                    String outputPatternnn = "HH-mm";
                    SimpleDateFormat inputFormattt = new SimpleDateFormat(inputPatternnn);
                    SimpleDateFormat outputFormattt = new SimpleDateFormat(outputPatternnn);

                    Date timeFinal = null;
                    String cATime = null;

                    try {
                        timeFinal = inputFormattt.parse(timee);
                        cATime = outputFormattt.format(timeFinal);
                        //Toast.makeText(mContext, ""+str, Toast.LENGTH_SHORT).show();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
*/


                    StringBuilder stringBuilder11=new StringBuilder();
                    Formatter formatter11=new Formatter(stringBuilder11);
                    formatter11.format("%1$-24s %2$-2s %3$17s", "Ordre bestilt Klokken:  ", "  ", "     "+ cATime+"  "+cADate);
                    mPrinter.setPrintAppendString(stringBuilder11.toString(), format);



                    StringBuilder stringBuilder12=new StringBuilder();
                    Formatter formatter12=new Formatter(stringBuilder12);
                    String currentDate = new SimpleDateFormat("dd-MMM", Locale.getDefault()).format(new Date());
                    String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

                    formatter12.format("%1$-24s %2$-2s %3$17s", "Ordre accepteret til    ", "  ", "    "+currentTime+"  "+currentDate);
                    mPrinter.setPrintAppendString(stringBuilder12.toString(), format);;


                    mPrinter.setPrintAppendString(" ", format);
                    mPrinter.setPrintAppendString(" ", format);
                    mPrinter.setPrintAppendString(" ", format);
                    mPrinter.setPrintAppendString(" ", format);
                    mPrinter.setPrintAppendString(" ", format);
                    printStatus = mPrinter.setPrintStart();
                    if (printStatus == SdkResult.SDK_PRN_STATUS_PAPEROUT) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DialogUtils.show(mActivity, "Out of paper");
                            }
                        });
                    }
                    else
                    {

                        Log.e("printStatus",printStatus+"");
                    }
                }
            }
        }).start();
    }

    private void setDeviceInfo() {
        // 读取并判断, 不存在则存入
        byte[] info = new byte[1000];
        byte[] infoLen = new byte[2];
        int getInfo = mSys.getDeviceInfo(info, infoLen);
        if (getInfo == SdkResult.SDK_OK) {
            int len = infoLen[0] * 256 + infoLen[1];
            byte[] newInfo = new byte[len];
            System.arraycopy(info, 0, newInfo, 0, len);
            String infoStr = new String(newInfo);
            Log.i(TAG, "getDeviceInfo: " + getInfo + "\t" + len + "\t" + infoStr);
            if (!TextUtils.isEmpty(infoStr)) {
                String[] split = infoStr.split("\t");
                // 已存则返回
                try {
                    // 确保imei1和mac 存值正确
                    if (split.length >= 4) {
                        String val1 = split[0].split(":")[1];
                        String val4 = split[3].split(":")[1];
                        if (!TextUtils.isEmpty(val1) && !val1.equals("null") && val1.length() >= 15
                                && !TextUtils.isEmpty(val4) && !val4.equals("null") && val4.length() >= 12 && val4.contains(":")) {
                            Log.i(TAG, "Have saved, return");
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Map<String, String> map = SystemInfoUtils.getImeiAndMeid(mActivity.getApplicationContext());
        String imei1 = map.get("imei1");
        String imei2 = map.get("imei2");
        String meid = map.get("meid");
        String mac = SystemInfoUtils.getMac();
        WifiManager wifi = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        long start = System.currentTimeMillis();
        while (TextUtils.isEmpty(mac) && System.currentTimeMillis() - start < 5000) {
            if (!wifi.isWifiEnabled()) {
                wifi.setWifiEnabled(true);
            }
            mac = SystemInfoUtils.getMac();
        }
        Log.i(TAG, "mac = " + mac);
        if (TextUtils.isEmpty(mac)) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DialogUtils.show(mActivity, "Warning! No mac!!!");
                }
            });
            return;
        }
        String msg = "IMEI1:" + (imei1 == null ? "" : imei1) + "\t" + "IMEI2:" + (imei2 == null ? "" : imei2) + "\t" + "MEID:" + (meid == null ? "" : meid) + "\t" + "MAC:" + mac;
        Log.i(TAG, "readDeviceInfo: " + msg);
        byte[] bytes = msg.getBytes();
        int setInfo;
        int count = 0;
        do {
            setInfo = mSys.setDeviceInfo(bytes, bytes.length);
            Log.i(TAG, "setDeviceInfo: " + setInfo);
            if (setInfo == SdkResult.SDK_OK) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (count++ < 5);
    }

    @Override
    public boolean onNavigationItemSelected(@androidx.annotation.NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.neworders: {
                break;
            }
            case R.id.acceptedorder: {
                Intent intent = new Intent(MainActivity.this, AcceptedOrdersActivity.class);
                intent.putExtra("screentitle","Acceptede Ordre");
                startActivity(intent);

                break;
            }

            case R.id.annulorder: {
                Intent intent = new Intent(MainActivity.this, AcceptedOrdersActivity.class);
                intent.putExtra("screentitle","Annulleret Ordre");

                startActivity(intent);

                break;
            }

            case R.id.faerorders: {
                Intent intent = new Intent(MainActivity.this, AcceptedOrdersActivity.class);
                intent.putExtra("screentitle","Faerdige Ordre");

                startActivity(intent);

                break;
            }

            case R.id.nav_logout: {
                break;
            }


        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.my_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void queryOrders() {

        mAWSAppSyncClient.query(ListOrdersQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(todosCallback);


       /* mAWSAppSyncClient.query(ListRestaurantsQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(todosCallback1);*/
    }

    private AppSyncSubscriptionCall.Callback<OnCreateOrderSubscription.Data> subCallback = new AppSyncSubscriptionCall.Callback<OnCreateOrderSubscription.Data>() {
        @Override
        public void onResponse(@Nonnull Response<OnCreateOrderSubscription.Data> response) {
            Log.e("Subscription", response.data().toString());

            /*orderslist = response.data().listOrders().items();
            if (response.data() != null) {
                orderslist = response.data().listOrders().items();
            } else {
                orderslist = new ArrayList<>();
            }
            adapter.setOrders(orderslist);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("changednotify", "Notifying data set changed");
                    adapter.notifyDataSetChanged();

                }
            });*/
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("Subscription", e.toString());
        }

        @Override
        public void onCompleted() {

            Log.e("Subscription", "Subscription completed");


        }
    };


    private GraphQLCall.Callback<ListOrdersQuery.Data> todosCallback = new GraphQLCall.Callback<ListOrdersQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListOrdersQuery.Data> response) {
            orderslist = response.data().listOrders().items();
            if (response.data() != null) {
                orderslist = response.data().listOrders().items();
            } else {
                orderslist = new ArrayList<>();
            }
            adapter.setOrders(orderslist);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("changednotify", "Notifying data set changed");
                    adapter.notifyDataSetChanged();

                }
            });

            /*int i;
            for (i = 0; i < orderslist.size(); i++) {
                Log.e("customername", orderslist.get(i).id());
            }


            adapter.notifyDataSetChanged();
            Log.e("orderslistsize", orderslist.size() + "");*/


        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("ERRORofresponse", e.toString());
        }
    };



}