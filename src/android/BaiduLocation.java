package com.zijiu.plugins.baidu;


import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.app.NotificationChannel;
import android.os.Build;
import java.util.Random;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

/**
 * ionic 百度定位插件 for android
 *
 * @author zijiu
 */
public class BaiduLocation extends CordovaPlugin {

    /**
     * LOG TAG
     */
    private static final String LOG_TAG = BaiduLocation.class.getSimpleName();

    /**
     * JS回调接口对象
     */
    public static CallbackContext cbCtx = null;

    /**
     * 百度定位客户端
     */
    public LocationClient mLocationClient = null;

    public boolean stopListen = true;

    // 通知
    public static final String TAG = "NotifysrvPlugin";
    public static final String iconname = "icon"; //icon res name
    public NotificationManager nm;
    public Context m_context;
    // To generate unique request codes
    private final Random random = new Random();


    /**
     * 百度定位监听
     */
    public BDAbstractLocationListener myListener = new BDAbstractLocationListener () {
        @Override
        public void onReceiveLocation(BDLocation location) {
            LOG.d(LOG_TAG, "location received..");
            try {
                JSONObject json = new JSONObject();

                json.put("time", location.getTime());
                json.put("locType", location.getLocType());
                json.put("latitude", location.getLatitude());
                json.put("longitude", location.getLongitude());
                json.put("radius", location.getRadius());
                json.put("address", location.getAddrStr());

                json.put("locationID", location.getLocationID());
                json.put("country", location.getCountry());
                json.put("countryCode", location.getCountryCode());
                json.put("city", location.getCity());
                json.put("cityCode", location.getCityCode());
                json.put("district", location.getDistrict());
                json.put("street", location.getStreet());
                json.put("streetNumber", location.getStreetNumber());
                json.put("locationDescribe", location.getLocationDescribe());
                json.put("poiList", location.getPoiList());

                json.put("buildingID", location.getBuildingID());
                json.put("buildingName", location.getBuildingName());
                json.put("floor", location.getFloor());

                if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                    json.put("speed", location.getSpeed());
                    json.put("satellite", location.getSatelliteNumber());
                    json.put("height", location.getAltitude());
                    json.put("direction", location.getDirection());
                    json.put("describe", "GPS定位成功");
                } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                    json.put("operators", location.getOperators());
                    json.put("describe", "网络定位成功");
                } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                    json.put("describe", "离线定位成功，离线定位结果也是有效的");
                } else if (location.getLocType() == BDLocation.TypeServerError) {
                    json.put("describe", "服务端网络定位失败，可将定位唯一ID、IMEI、定位失败时间反馈至loc-bugs@baidu.com");
                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                    json.put("describe", "网络不同导致定位失败，请检查网络是否通畅");
                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                    json.put("describe", "当前缺少定位依据，可能是用户没有授权，建议弹出提示框让用户开启权限");
                }

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, json);
                pluginResult.setKeepCallback(true);
                cbCtx.sendPluginResult(pluginResult);
            } catch (JSONException e) {
                String errMsg = e.getMessage();
                LOG.e(LOG_TAG, errMsg, e);

                PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, errMsg);
                pluginResult.setKeepCallback(true);
                cbCtx.sendPluginResult(pluginResult);
            } finally {
                if (stopListen)
                    mLocationClient.stop();
            }
        }

    };

    /**
     * 插件主入口
     */
    @Override
    public boolean execute(String action, final JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.d(LOG_TAG, "Baidu Location #execute");

        boolean ret = false;
        cbCtx = callbackContext;

        if ("getCurrentPosition".equalsIgnoreCase(action)) {
            stopListen = true;
            if (mLocationClient == null) {
                mLocationClient = new LocationClient(this.webView.getContext());
                mLocationClient.registerLocationListener(myListener);
            }
            // 配置定位SDK参数
            initLocation(0);
            if (mLocationClient.isStarted())
                mLocationClient.stop();
            mLocationClient.enableLocInForeground(1001, null);// 调起前台定位
            mLocationClient.start();
            ret = true;
        } else if ("watchPosition".equalsIgnoreCase(action)) {
            stopListen = false;
            if (mLocationClient == null) {
                mLocationClient = new LocationClient(this.webView.getContext());
                mLocationClient.registerLocationListener(myListener);
            }

          m_context = this.cordova.getActivity().getApplicationContext();
          //获取状态通知栏管理
          NotificationManager nm = (NotificationManager) m_context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
          NotificationCompat.Builder builder;

          //判断是否是8.0Android.O
          if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel chan1 = new NotificationChannel("static",
              "体育实践通知", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(chan1);
            builder = new NotificationCompat.Builder(m_context, "static");
          } else {
            builder = new NotificationCompat.Builder(m_context);
          }

          String title = "定位中";
          String text = "持续为您定位...";

          int reqCode = random.nextInt();

          PendingIntent contentIntent = PendingIntent.getActivity(
            m_context, reqCode, this.cordova.getActivity().getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);

          builder.setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX) //设置通知优先级
            .setDefaults(Notification.DEFAULT_ALL) //设置默认铃声，震动等
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentIntent(contentIntent)
            .setAutoCancel(true);
//            .setContentIntent(m_PendingIntent)
//            .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间
//            .setOngoing(true);

          Notification notification = null;
            notification = builder.build();

            //nm.notify(1, notification);

            int span = args.getInt(0);

            // 配置定位SDK参数
            initLocation(span * 1000);
            if (mLocationClient.isStarted())
                mLocationClient.stop();
            mLocationClient.start();
            mLocationClient.enableLocInForeground(1001, notification);// 调起前台定位
            ret = true;
        } else if ("clearWatch".equalsIgnoreCase(action)) {
            if (mLocationClient != null && mLocationClient.isStarted())
                mLocationClient.stop();
            mLocationClient.disableLocInForeground(true);// 关闭前台定位，同时移除通知栏
            ret = true;
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(false);
            cbCtx.sendPluginResult(pluginResult);
        }
        return ret;
    }

    /**
     * 配置定位SDK参数
     */
    private void initLocation(int span) {
        LocationClientOption option = new LocationClientOption();
        // 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setLocationMode(LocationMode.Hight_Accuracy);
        // 可选，默认gcj02，设置返回的定位结果坐标系
        option.setCoorType("bd09ll");
        // 可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        if (stopListen)
            option.setScanSpan(0);
        else
            option.setScanSpan(span);
        // 可选，设置是否需要地址信息，默认不需要
        // option.setIsNeedAddress(false);
        // 可选，默认false,设置是否使用gps
        option.setOpenGps(true);
        // 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setLocationNotify(false);
        /* 可选，默认false，设置是否需要位置语义化结果，
         * 可以在BDLocation.getLocationDescribe里得到，
         * 结果类似于“在北京天安门附近”
         */
        // option.setIsNeedLocationDescribe(true);
        // 可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        // option.setIsNeedLocationPoiList(true);
        // 可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
        // option.setIgnoreKillProcess(false);
        // 可选，默认false，设置是否收集CRASH信息，默认收集
        // option.SetIgnoreCacheException(true);
        // 可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        // option.setEnableSimulateGps(false);
        mLocationClient.setLocOption(option);
    }

    @Override
    public void onDestroy() {
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.stop();
            mLocationClient = null;
        }
        super.onDestroy();
    }

}
