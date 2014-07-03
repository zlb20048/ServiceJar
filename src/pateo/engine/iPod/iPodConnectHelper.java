/*
 * 文 件 名:  iPodConnectHelper.java
 * 版    权:  Pateo Co., Ltd. Copyright YYYY-YYYY,  All rights reserved
 * 描    述:  <描述>
 * 修 改 人:  zixiangliu
 * 修改时间:  2014-6-17
 * 跟踪单号:  <跟踪单号>
 * 修改单号:  <修改单号>
 * 修改内容:  <修改内容>
 */
package pateo.engine.iPod;

import java.util.ArrayList;
import java.util.List;

import pateo.com.iPod.iPodDefine.iPodLogDefines;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * 监听当前的ipod的状态的连接
 * @author  zixiangliu
 * @version  [版本号, 2014-6-17]
 * @see  [相关类/方法]
 * @since  [产品/模块版本]
 */
public class iPodConnectHelper
{
    /**
     * TAG
     */
    private final static String TAG = iPodConnectHelper.class.getSimpleName();

    /**
     * ipod 设备连接上了
     */
    private final static String CONNECT_DEVICE = "ASTEROID_IPHONE_CONNECTED";

    /**
     * ipod 设备断开
     */
    private final static String DISCONNECT_DEVICE = "ASTEROID_IPHONE_REMOVED";

    /**
     * 单实例对象
     */
    private static iPodConnectHelper instance = null;

    /**
     * 上下文
     */
    private Context mContext;

    /**
     * 回调给到所有注册的对象
     */
    private List<ConnectStateCallback> callBackList =
        new ArrayList<ConnectStateCallback>();

    /** 构造方法
     */
    private iPodConnectHelper(Context context)
    {
        this.mContext = context;
    }

    /**
     * 获取单实例对象
     * @return 当前的单实例
     * @see [类、类#方法、类#成员]
     */
    public synchronized static iPodConnectHelper getInstance(Context context)
    {
        if (instance == null)
        {
            instance = new iPodConnectHelper(context);
        }
        return instance;
    }

    /**
     * 动态的注册当前的广播
     * @see [类、类#方法、类#成员]
     */
    public void register()
    {
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(CONNECT_DEVICE);
        mIntentFilter.addAction(DISCONNECT_DEVICE);
        mContext.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    /**
     * 注销当前的广播
     * @see [类、类#方法、类#成员]
     */
    public void unRegister()
    {
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    /**
     * 当前注册的广播
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            iPodLogDefines.iPodLog(TAG, "action = " + action);
            if (CONNECT_DEVICE.equals(action))
            {
                for (ConnectStateCallback mConnectStateCallback : callBackList)
                {
                    mConnectStateCallback.connect();
                }
            }
            else if (DISCONNECT_DEVICE.equals(action))
            {
                for (ConnectStateCallback mConnectStateCallback : callBackList)
                {
                    mConnectStateCallback.disconnect();
                }
            }
        }
    };

    /** 注册
     * @param connectStateCallback
     * @see [类、类#方法、类#成员]
     */
    public void addCallback(ConnectStateCallback connectStateCallback)
    {
        if (!callBackList.contains(connectStateCallback))
        {
            callBackList.add(connectStateCallback);
        }
    }

    /**
     * 将当前的状态回调
     * @author  zixiangliu
     * @version  [版本号, 2014-6-17]
     * @see  [相关类/方法]
     * @since  [产品/模块版本]
     */
    public interface ConnectStateCallback
    {
        public void connect();

        public void disconnect();
    }
}
