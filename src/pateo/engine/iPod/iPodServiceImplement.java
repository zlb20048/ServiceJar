package pateo.engine.iPod;

import java.util.List;

import pateo.com.FrameworkApi.SerialCallback;
import pateo.com.FrameworkApi.SerialInterface;
import pateo.com.ApplicationApi.AppCmdHelper;
import pateo.com.ApplicationApi.IPodApi;
import pateo.com.ApplicationApi.IPodApi.IpodCallback;
import pateo.com.ApplicationApi.TraceManager;
import pateo.com.Config.PateoConfig;
import pateo.com.audio.operate.LauncherCallBack;
import pateo.com.global.InfoBarDefine;
import pateo.com.global.InfoBarDefine.InfoMedia;
import pateo.com.global.InfoBarDefine.PlayState;
import pateo.com.global.Communication.AppCmdCallBack;
import pateo.com.global.Communication.AppDefine.LPARAM_KEYCODE;
import pateo.com.global.Communication.AppDefine.eAppServiceType;
import pateo.com.global.Communication.AppDefine.eSourceDefine;
import pateo.com.global.Communication.ClientCallback;
import pateo.com.global.Communication.RemoteMsgDefine;
import pateo.com.iPod.iPodDefine.IPOD_PLAY_STATE;
import pateo.com.iPod.iPodDefine.SP_IPOD_DATA_ENUM;
import pateo.com.iPod.iPodDefine.SP_IPOD_LIST_CATEGORY_TYPE;
import pateo.com.iPod.iPodDefine.SP_IPOD_REFRESH_ENUM;
import pateo.com.iPod.iPodDefine.SP_MEDIA_TYPE_ENUM;
import pateo.com.iPod.iPodDefine.iPodLogDefines;
import pateo.com.iPod.iPodServiceInterface;
import pateo.com.iPod.iPodType.SP_IPOD_ID3;
import pateo.com.iPod.iPodType.SP_IPOD_OSD;
import pateo.com.iPod.iPodType.SP_IPOD_TRACK_INDEX_TIME;
import pateo.com.iPod.iPodType.SongData;
import pateo.com.provider.IPodMediaStore;
import pateo.com.util.SettingHelper;
import pateo.engine.iPod.iPodCommandInterface.iPodCmdAnalysedInterface;
import pateo.engine.iPod.iPodCommandInterface.iPodCmdCallBack;
import pateo.engine.iPod.iPodCommandInterface.iPodCmdSendInterface;
import pateo.engine.iPod.iPodCommandInterface.iPodWriteInterface;
import pateo.engine.iPod.iPodConnectHelper.ConnectStateCallback;
import pateo.engine.iPod.iPodDataDef.AUDIO_TYPE;
import pateo.engine.iPod.iPodDataDef.HANDLE_EVENT;
import pateo.engine.iPod.iPodDataDef.IPOD_MODEL_IDS;
import pateo.engine.iPod.iPodDataDef.IPOD_PLAY_CONTROL_CMD_CODES;
import pateo.engine.iPod.iPodDataDef.IPOD_UI_EVENT;
import pateo.engine.iPod.iPodDataDef.RX_PACKET_PARAM;
import pateo.engine.iPod.iPodDataDef.TX_PACKET_PARAM;
import pateo.frameworks.api.iPodJNI;
import pateo.frameworks.api.iPodJNI.IPOD_AUDIO_TYPE;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class iPodServiceImplement extends iPodServiceInterface implements
    SerialCallback, iPodCmdSendInterface, iPodCmdAnalysedInterface,
    iPodWriteInterface, iPodCmdCallBack, IpodCallback, ConnectStateCallback
{
    private final static String TAG = "iPodServiceImplement";

    private final int MAX_DATA_LENTH = 4096;

    private Context mContext = null;

    private ClientCallback mClientCallback = null;

    private iPodOperateCode miPodOperate = null;

    private iPodResponse miPodResponse = null;

    private iPodCommand miPodCommand = null;

    private SerialInterface mSerialInterface = null;

    private iPodJNI miPodJni = null;

    private iPodDataLayer mDataLayer = null;

    private boolean mbOpeniPodJNI = false;

    private IPodApi mIPodApi = null;

    private Thread mReadDataThread = null;

    private boolean mbIsRead = false;

    private boolean mNeedPlay = false;

    private iPodConnectHelper mipodConnectHelper = null;

    private boolean mIsExit = false;

    private Handler mSmartHandler = null;

    private Boolean mSmartState = false;

    private SP_IPOD_ID3 trackID3;

    private SettingHelper mSettingHelper = null;

    private LauncherCallBack mLauncherCallBack;

    // 多媒体接口
    private int allSize = 0;

    private int selectSize = 0;

    private int mState = 0;

    private SP_IPOD_TRACK_INDEX_TIME mTrackIndex;

    public iPodServiceImplement(Context context)
    {
        mContext = context;
        mDataLayer = iPodDataLayer.GetInstant();
        mSettingHelper = new SettingHelper(mContext);
        miPodCommand = new iPodCommand(this);
        mipodConnectHelper = iPodConnectHelper.getInstance(mContext);
        mipodConnectHelper.addCallback(this);
        mipodConnectHelper.register();
        // mIPodApi = new IPodApi(mContext, this, mAppCmdCallBack);
        // mIPodApi.Open();
        miPodOperate = new iPodOperateCode(this, this);
        miPodOperate.setOnDataChange(new iPodOperateCode.OnDataChange()
        {

            public void onDataChange(TreeNode node)
            {
                insertIntoData(node);
            }
        });
        miPodResponse = new iPodResponse(this);
        SmartHandle();
    }

    /**
     * 将数据插入到数据库中
     * @param node 当前的数据
     * @see [类、类#方法、类#成员]
     */
    private void insertIntoData(TreeNode node)
    {
        ContentResolver resolver = mContext.getContentResolver();
        Log.d(TAG, "node.GetNodeID() = " + node.GetNodeID());
        ContentValues values = new ContentValues();
        values.put(IPodMediaStore.TITLE_NAME, node.GetNodeName());
        values.put(IPodMediaStore.NODE_ID, node.GetNodeID());
        resolver.insert(IPodMediaStore.CONTENT_URI, values);
    }

    public void Close()
    {

    }

    public boolean RegisterClient(ClientCallback Callback)
    {
        // TODO Auto-generated method stub
        mClientCallback = Callback;
        return true;
    }

    public void ClientState(byte state)
    {
        // TODO Auto-generated method stub

    }

    public boolean UnRegisterClient()
    {
        // TODO Auto-generated method stub
        mClientCallback = null;
        return true;
    }

    @Override
    public int SP_IPOD_Init(SP_IPOD_OSD sOSD)
    {
        // TODO Auto-generated method stub
        iPodLogDefines.iPodLog(TAG, "SP_IPOD_Init mParkingState");
        mIsExit = false;
        if (PateoConfig.SYSTEM_SIMU)
        {
            if (mSerialInterface == null)
            {
                // mSerialInterface = iPodClient.GetSerial();
                // mSerialInterface.SerialRegister(this);
                // mSerialInterface.SerialOpen(FrameworkApi.MCUUart, 0);
            }
        }
        else
        {
            if (miPodJni == null)
            {
                miPodJni = new iPodJNI();
                Log.d(TAG, "miPodJni.ipod_open() = Before");
                if (miPodJni.ipod_open() != -1)
                {
                    Log.d(TAG, "miPodJni.ipod_open() = +++++");
                    // if (mIPodApi != null)
                    // {
                    int[] iAudioType = new int[1];
                    if (miPodJni.ipod_ioctl(iPodJNI.GET_AUDIO_TYPE, iAudioType) != -1)
                    {
                        if (iAudioType[0] == IPOD_AUDIO_TYPE.AUDIO_DIGITAL)
                        {
                            // mIPodApi.AudioType(mIPodApi.LP_IPOD_DLL_AUDIO_DIGITAL);
                            mDataLayer.SetAudioType(AUDIO_TYPE.AUDIO_DIGITAL);
                        }
                        else if (iAudioType[0] == IPOD_AUDIO_TYPE.AUDIO_ANALOG)
                        {
                            // mIPodApi.AudioType(mIPodApi.LP_IPOD_DLL_AUDIO_ANALOG);
                            mDataLayer.SetAudioType(AUDIO_TYPE.AUDIO_ANALOG);
                        }
                    }
                    else
                    {
                        iPodLogDefines.iPodLog(TAG, "GET AUDIO TYPE FAILED!");
                    }
                    // }
                    if (mDataLayer != null)
                    {
                        int[] iIDPSStatus = new int[2];
                        if (miPodJni.ipod_ioctl(iPodJNI.GET_IDPS_STATE,
                            iIDPSStatus) != -1)
                        {
                            if (iIDPSStatus[0] == 0)
                            {
                                mDataLayer.SetUseIDPS(false);
                            }
                            else
                            {
                                mDataLayer.SetUseIDPS(true);
                            }
                            mDataLayer.SetTransID(iIDPSStatus[1]);
                            iPodLogDefines.iPodLog(TAG, "GET IDPSStatus->ID:"
                                + mDataLayer.GetTransID());
                        }
                        else
                        {
                            iPodLogDefines.iPodLog(TAG,
                                "GET IDPSStatus FAILED!");
                        }
                        int[] iPodModelID = new int[1];
                        if (miPodJni.ipod_ioctl(iPodJNI.GET_IPOD_TYPE,
                            iPodModelID) != -1)
                        {
                            mDataLayer.SetiPodModelID(iPodModelID[0]);
                            iPodLogDefines.iPodLog(TAG, "GET IPOD_TYPE->ID:"
                                + mDataLayer.GetiPodModelID());
                        }
                        else
                        {
                            iPodLogDefines.iPodLog(TAG, "GET IPOD_TYPE FAILED!");
                        }
                    }
                    mbOpeniPodJNI = true;
                    ReadThreadStart();
                }
            }
        }
        if (sOSD != null)
        {
            IPOD_UI_EVENT event = new IPOD_UI_EVENT();
            event.eventType = HANDLE_EVENT.EVENT_CHANGE_OSD;
            event.obj = sOSD;
            miPodOperate.iPOD_OperateHandle(event);
        }
        return 0;
    }

    @Override
    public int SP_IPOD_DeInit()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int SP_IPOD_Connect(SP_MEDIA_TYPE_ENUM eMediaType)
    {
        // TODO Auto-generated method stub
        TraceManager.LogE(TAG, "SP_IPOD_Connect+eMediaType" + eMediaType);
        if (miPodJni == null)
        {
            if (mDataLayer != null)
            {
                SP_IPOD_Init(mDataLayer.GetiPodOSD());
            }
        }
        miPodOperate.IPOD_Connect(eMediaType);
        return 0;
    }

    @Override
    public int SP_IPOD_ChangeOSD(SP_IPOD_OSD sOSD)
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_CHANGE_OSD;
        event.obj = sOSD;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_PlayPauseCurrentTrack()
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
        event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
        miPodOperate.iPOD_OperateHandle(event);
        if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PAUSE)
        {
            miPodOperate.iPOD_SetPlayingState(0);
        }
        else if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PLAY)
        {
            miPodOperate.iPOD_SetPlayingState(1);
        }
        else if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START)
        {
            miPodOperate.iPOD_SetPlayingState(2);
        }
        return 0;
    }

    @Override
    public int SP_IPOD_StopCurrentTrack()
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
        event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_STOP;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_ENDFForREW()
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
        if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START
            || mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START)
        {
            event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW;
            miPodOperate.iPOD_OperateHandle(event);
        }
        return 0;
    }

    @Override
    public int SP_IPOD_Shuffle(byte sShuffle)
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_SET_SHUFFLE;
        event.iWParam = sShuffle;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_Repeat(byte sRepeat)
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_SET_REPEAT;
        event.iWParam = sRepeat;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_SelectTrackPlay(long uId)
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CURRENT_SONG;
        event.iWParam = uId;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_ReturnRootList()
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_BACK_TOPMENU;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_ReturnParentList()
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_MENU;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_GoChildrenList(long uId)
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_REQ_PLAYLIST;
        event.iWParam = uId;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_GoCategoryTypeList(
        SP_IPOD_LIST_CATEGORY_TYPE eCategoryType)
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_REQ_CATEGORY_TYPE_LIST;
        event.obj = eCategoryType;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_SetVolume(byte eVolume)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int SP_IPOD_ABC123Search(String wsSearchStr, boolean bNeedInMatching)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int SP_IPOD_ABC123Search(int eABC123)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int SP_IPOD_ReqChildren(long uStart, long uReqNum)
    {
        // TODO Auto-generated method stub
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_REQ_CHILDREN;
        event.iWParam = uStart;
        event.iLParam = uReqNum;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_ReqCurList()
    {
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_REQ_CURLIST;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_SyncList()
    {
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_SYNCLIST;
        miPodOperate.iPOD_OperateHandle(event);
        return 0;
    }

    @Override
    public int SP_IPOD_GetDataType(SP_IPOD_DATA_ENUM eData)
    {

        switch (eData)
        {
            case SP_IPOD_DATA_CURRENT_TRACK:
                EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAYING_INDEX,
                    0,
                    mDataLayer.GetCurTrackIndex());
                break;
            case SP_IPOD_DATA_TRACK_TYPE:
                EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_TYPE,
                    0,
                    mDataLayer.GetCurTrackType());
                break;

            case SP_IPOD_DATA_REPEAT_STATUS:
                EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_REPEAT_STATE,
                    mDataLayer.GetRepeatState(),
                    null);
                break;
            case SP_IPOD_DATA_PLAY_STATUS:
                EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_STATE,
                    mDataLayer.GetPlayState(),
                    null);
                break;
            case SP_IPOD_DATA_SHUFFLE_STATUS:
                EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_SHUFFLE_STATE,
                    mDataLayer.GetShuffleState(),
                    null);
                break;
            case SP_IPOD_DATA_MEDIA_TYPE:
                // EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_,
                // mDataLayer.GetShuffleState(), null);
                break;
            case SP_IPOD_DATA_ID3_INFO:
                SP_IPOD_ID3 trackID3 = new SP_IPOD_ID3();
                trackID3.wcTitle = mDataLayer.GetTrackTitle();
                trackID3.wcArtist = mDataLayer.GetTrackArtist();
                trackID3.wcAlbum = mDataLayer.GetTrackAlbum();
                trackID3.wcGenre = mDataLayer.GetTrackGenre();
                trackID3.wcRelease = mDataLayer.GetTrackRelease();
                trackID3.eType = mDataLayer.GetCurTrackType();
                TraceManager.LogI(TAG, "SP_IPOD_GetDataType->" + eData
                    + " Title:" + trackID3.wcTitle);
                EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_ID3,
                    0,
                    trackID3);
                break;
            case SP_IPOD_DATA_LIST:
                // EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST, 0, 0,
                // mTreeManager.GetCurListSongData());
                break;

            default:
                break;
        }
        return 0;
    }

    public void OnSerialRead(byte[] buf, int bufLen)
    {
        // TODO Auto-generated method stub
        // iPodLogDefines.iPodLog(TAG, "OnSerialRead:"+buf.toString());
        miPodResponse.OnSerialRead(buf, bufLen);
    }

    public void OnRxAnalysedEnter(int rxCode, RX_PACKET_PARAM rxParam)
    {
        // TODO Auto-generated method stub
        // iPodLogDefines.iPodLog(TAG, "OnRxAnalysedEnter->rxCode:" + rxCode);
        miPodOperate.OnRxAnalysedEnter(rxCode, rxParam);
    }

    public void SendCommand(int tpCode, TX_PACKET_PARAM txParam)
    {
        // TODO Auto-generated method stub
        iPodLogDefines.iPodLog(TAG, "SendCommand->tpCode:" + tpCode);
        miPodCommand.SendCommand(tpCode, txParam);
    }

    public void WriteCommand(byte[] buf, int chBufLen)
    {
        // TODO Auto-generated method stub
        if (PateoConfig.SYSTEM_SIMU)
        {
            if (mSerialInterface != null)
            {
                // iPodLogDefines.iPodLog(TAG, "WriteCommand:"+buf.toString());
                mSerialInterface.SerialWrite(buf, (byte)chBufLen);
            }
        }
        else
        {
            if (miPodJni != null && mbOpeniPodJNI)
            {
                // iPodLogDefines.iPodLog(TAG, "WriteCommand!!!!");
                // iPodLogDefines.iPodLog(TAG, "WriteCommand!!!! chBufLen"
                // + chBufLen);
                if (miPodJni.ipod_write(buf, chBufLen) == -1)
                {
                    // iPodLogDefines.iPodLog(TAG, "Write Error, Repeat Write");
                    int temp = 0;
                    do
                    {
                        // iPodLogDefines.iPodLog(TAG,
                        // "Write Error, Repeat Write----------------->");
                        if (miPodJni != null)
                        {
                            temp = miPodJni.ipod_write(buf, chBufLen);
                        }
                    } while (temp == -1 && mIsExit == false);
                }
                // iPodLogDefines.iPodLog(TAG, "WriteCommand!!!!->end");
            }
        }
    }

    synchronized public boolean EnterCallBackFun(byte eventType, int arg1,
        Object data)
    {
        switch (eventType)
        {
            case SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_ID3:
                SendToInfoBar(InfoBarDefine.RF_ID3, data);
                if (mDataLayer.GetIsPlayingVideo())
                {
                    EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_VOIDO_OVER_VIDEOVIEW,
                        100,
                        null);
                }
                // if(allSize==0)
                // {
                // selectSize = 0;
                // }else {
                // selectSize =(int)mTrackIndex.uIndex+1;
                // }
                // mIPodApi.SendToCanInfo(MCU_RESP_CANBUS_HYUNDAI_SUBID.Media_info.Media_Status_Playing,(byte)0,selectSize,allSize,(byte)0,(byte)0);
                break;
            case SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_INDEX:
                mTrackIndex = (SP_IPOD_TRACK_INDEX_TIME)data;
                allSize = (int)mTrackIndex.uTotalIndex;
                selectSize = (int)mTrackIndex.uIndex;

                // TODO add by lzx
                monIPodListChange.onIPodSongChange(selectSize);

                SendToInfoBar(InfoBarDefine.RF_TOTALNUM, data);
                break;
            case SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAYING_INDEX:
                SendToInfoBar(InfoBarDefine.RF_CURRENTNUM, arg1);
                break;
            case SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_STATE:
                mState = arg1;
                // TODO add by lzx
                monIPodListChange.onIPodStateChange(mState);

                SendToInfoBar(InfoBarDefine.RF_PLAYSTATE, arg1);
                break;
            case SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_TIME:
                SP_IPOD_TRACK_INDEX_TIME time = (SP_IPOD_TRACK_INDEX_TIME)data;
                // mIPodApi.SendGeneralToInfoBar(InfoBarDefine.WP_PLAYTIME,
                // Time2Int((int)time.uIndex));
                if (mDataLayer.GetTotalPlayTime() - mDataLayer.GetCurPlayTime() == 2)
                {
                    // 当歌曲时间小于两秒的时候，通知播放即将完成
                    monIPodListChange.onIPodSongEnd();
                }
                if (mState == 1)
                {
                    int time1 = (int)time.uIndex;
                    int second = time1 % 60;
                    int minute = time1 / 60;
                    if (allSize == 0)
                    {
                        selectSize = 0;
                    }
                    else
                    {
                        selectSize = (int)mTrackIndex.uIndex + 1;
                    }
                    // mIPodApi.SendToCanInfo(MCU_RESP_CANBUS_HYUNDAI_SUBID.Media_info.Media_Status_Playing,
                    // (byte)0,
                    // selectSize,
                    // allSize,
                    // (byte)second,
                    // (byte)minute);
                }
                break;
            case SP_IPOD_REFRESH_ENUM.SP_IPOD_AUDIO_TYPE:
                TraceManager.LogE(TAG, "AUDIO_TYPE->" + arg1);
                // mIPodApi.AudioType((byte)arg1);
                return true;
            case SP_IPOD_REFRESH_ENUM.SP_IPOD_SET_AUDIO_TYPE:
                TraceManager.LogE(TAG, "SP_IPOD_SET_AUDIO_TYPE->" + arg1);
                int[] iAudioType = new int[1];
                iAudioType[0] = arg1;
                if (miPodJni != null)
                {
                    TraceManager.LogE(TAG, "miPodJni.ipod_ioctl->" + iAudioType);
                    if (miPodJni.ipod_ioctl(iPodJNI.SET_AUDIO_TYPE, iAudioType) == -1)
                    {
                        TraceManager.LogE(TAG,
                            "miPodJni.ipod_ioctl------------------>FAILED");
                    }
                }
                return true;
            case SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST:
                if (null != monIPodListChange)
                {
                    if (null != data)
                    {
                        monIPodListChange.onIPodListChange((List<SongData>)data);
                    }
                }
                break;
            default:
                break;
        }
        if (mClientCallback != null)
        {
            mClientCallback.LocalCallback(eventType, arg1, data);
            return true;
        }
        else
        {
            return false;
        }
    }

    public void OnPlayInBack(boolean isHide)
    {
        // TODO Auto-generated method stub
        iPodLogDefines.iPodLog(TAG, "OnPlayInBack!");
        // EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAYINBACK, 0,
        // mDataLayer.GetMediaType());
    }

    public void OnParkingState(boolean isOn)
    {
        // TODO Auto-generated method stub
        EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PARKING, 0, isOn);

        TraceManager.LogE(TAG, "OnParkingState" + isOn);
    }

    public void OnRequestClose()
    {
        // if(mDataLayer.GetPlayState() ==
        // IPOD_PLAY_STATE.IPOD_PST_FF_START||mDataLayer.GetPlayState() ==
        // IPOD_PLAY_STATE.IPOD_PST_REW_START)
        // {
        // IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        // event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
        // event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW;
        // miPodOperate.iPOD_OperateHandle(event);
        // }
        // SystemClock.sleep(500);
        ReadThreadStop();
        miPodOperate.iPodDeInit();
        miPodJni.ipod_close();
        miPodJni = null;
        selectSize = 0;
        allSize = 0;
        mbOpeniPodJNI = false;
        miPodOperate.iPOD_SetPlayingState(1);
        EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_REQUESTCLOSE,
            0,
            mDataLayer.GetMediaType());
        iPodLogDefines.iPodLog(TAG, "OnRequestClose!");

    }

    public void OnKeyEvent(byte keycode)
    {

        iPodLogDefines.iPodLog(TAG, "OnKeyEvent:" + keycode);
        switch (keycode)
        {
            case LPARAM_KEYCODE.KEY_PLAY:
                SP_IPOD_PlayPauseCurrentTrack();
                EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_VOIDO_OVER_VIDEOVIEW,
                    100,
                    null);
                break;
            case LPARAM_KEYCODE.KEY_STOPBAND:
                if (mDataLayer.GetIsPlayingVideo())
                {
                    if (mDataLayer.GetPlayState() != IPOD_PLAY_STATE.IPOD_PST_STOP)
                    {
                        SP_IPOD_StopCurrentTrack();
                    }
                }
                else
                {
                    SP_IPOD_StopCurrentTrack();
                }
                break;
            case LPARAM_KEYCODE.KEY_NEXT:
                actionNext();
                break;
            case LPARAM_KEYCODE.KEY_PREVIOUS:
                actionPrev();
                break;
            case LPARAM_KEYCODE.KEY_PANEL_NEXT_LONG:
                if (mDataLayer.GetPlayState() != IPOD_PLAY_STATE.IPOD_PST_STOP)
                {
                    actionForward(true);
                }

                break;
            case LPARAM_KEYCODE.KEY_PANEL_PRE_LONG:
                if (mDataLayer.GetPlayState() != IPOD_PLAY_STATE.IPOD_PST_STOP)
                {
                    actionRewind(true);
                }
                else
                {
                    EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_VOIDO_OVER_VIDEOVIEW,
                        100,
                        null);
                }
                break;
            case LPARAM_KEYCODE.KEY_NEXT_PLUS:
                mSmartHandler.removeMessages(1);
                mSmartState = true;
                EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_NEXT_PLUS,
                    0,
                    null);
                mSmartHandler.sendEmptyMessageDelayed(1, 3000);
                break;
            case LPARAM_KEYCODE.KEY_PREV_PLUS:
                mSmartHandler.removeMessages(1);
                mSmartState = true;
                EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_PREV_PLUS,
                    0,
                    null);
                mSmartHandler.sendEmptyMessageDelayed(1, 3000);
                break;
            case LPARAM_KEYCODE.KEY_SMART_CLICK:
                if (mSmartState == true)
                {
                    EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_SMART_CLICK,
                        0,
                        null);
                    mSmartState = false;
                }
                break;
            default:
                break;
        }
    }

    private void SmartHandle()
    {
        if (mSmartHandler == null)
        {
            mSmartHandler = new Handler()
            {
                @Override
                public void handleMessage(Message msg)
                {
                    super.handleMessage(msg);
                    if (msg.what == 1)
                    {
                        mSmartState = false;
                    }
                }
            };
        }
    }

    private void ReadThreadStart()
    {
        if (!mbIsRead)
        {
            mbIsRead = true;
            mReadDataThread = new Thread("iPodReadData")
            {

                byte[] readBuf = new byte[MAX_DATA_LENTH];

                @Override
                public void run()
                {
                    // TODO Auto-generated method stub
                    while (mbIsRead)
                    {
                        int len;
                        if (miPodJni != null)
                        {
                            // iPodLogDefines.iPodLog(TAG,
                            // "OnSerialRead->Read:");
                            len = miPodJni.ipod_read(readBuf, MAX_DATA_LENTH);
                            // iPodLogDefines.iPodLog(TAG,
                            // "OnSerialRead->Start:"
                            // + len);
                            if (len > 0 && miPodResponse != null)
                            {
                                miPodResponse.OnSerialRead(readBuf, len);
                            }
                            else if (len == -1)
                            {
                                miPodOperate.ReadError();
                            }
                        }
                    }
                }
            };
            mReadDataThread.start();
        }
    }

    private void ReadThreadStop()
    {
        if (mbIsRead)
        {
            mbIsRead = false;
            mReadDataThread.interrupt();
            mReadDataThread = null;
        }
    }

    AppCmdCallBack mAppCmdCallBack = new AppCmdCallBack()
    {
        public void OnSourceEnter(eAppServiceType appEnter, boolean isParking,
            boolean isFrontShow)
        {
            // TODO Auto-generated method stub
            TraceManager.LogE(TAG, "OnSourceEnter" + appEnter);
            SP_IPOD_Init(null);
            if (appEnter == eAppServiceType.mbiPodAudio)
            {
                SP_IPOD_Connect(SP_MEDIA_TYPE_ENUM.SP_MEDIA_Audio);
            }
            else if (appEnter == eAppServiceType.mbiPodVideo)
            {
                SP_IPOD_Connect(SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video);
            }
            else if (appEnter == eAppServiceType.mbReariPod)
            {
                SP_IPOD_Connect(SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO);
            }
        }

        public void OnSourceExit(eAppServiceType appExit,
            eAppServiceType appEnter)
        {
            TraceManager.LogE(TAG, "OnSourceExit***********" + appExit);
            destoryIpod();
        }

        public void OnPageShow(eAppServiceType appShow, boolean isParking)
        {

            TraceManager.LogE(TAG, "OnPageShow" + appShow);
        }

        public void OnPageHide(eAppServiceType appHide, eAppServiceType appShow)
        {

            TraceManager.LogE(TAG, "OnPageHide" + appHide);
        }
    };

    private OnIPodListChange monIPodListChange;

    /**
     * 销毁IPOD的相关值
     * @see [类、类#方法、类#成员]
     */
    public void destoryIpod()
    {
        mIsExit = true;
        ReadThreadStop();
        miPodOperate.iPodDeInit();
        miPodJni.ipod_close();
        miPodJni = null;
        mbOpeniPodJNI = false;
        // delete the data from db
        mContext.getContentResolver().delete(IPodMediaStore.CONTENT_URI,
            null,
            null);
        EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_SOURCEEXIT,
            0,
            mDataLayer.GetMediaType());
        TraceManager.LogE(TAG,
            "destoryIpod***********mDataLayer.GetMediaType()"
                + mDataLayer.GetMediaType());
    }

    public void OnStartCommand(Intent intent)
    {
        TraceManager.LogI(TAG, "OnStartCommand!!!!!");
        if (intent != null)
        {
            AppCmdHelper.ProcessCmd(intent, mAppCmdCallBack);
        }
    }

    public void OnGeneralProc(int wParam, int lParam)
    {
        // 现在只用处理一个消息，wParam参数为RemoteMsgDefine.WP_MAIN_CALL_STATE，
        // lParam参数为RemoteMsgDefine.LP_BT_SOUND_ON 时如果不是暂停状态则暂停播放。
        // lParam参数为RemoteMsgDefine.LP_BT_SOUND_OFF 时如果之前进行了暂停操作则继续播放。
        if (wParam == RemoteMsgDefine.WP_MAIN_CALL_STATE)
        {
            switch (lParam)
            {
                case RemoteMsgDefine.LP_BT_SOUND_ON:
                    if (mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPHONE_3G
                        || mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPHONE
                        || mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPHONE_3GS
                        || mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPHONE_4S
                        || mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPHONE_4)
                    {
                        if (mDataLayer.GetIsPlayingVideo())
                        {
                            if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START
                                || mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START)
                            {
                                mNeedPlay = true;
                                SP_IPOD_ENDFForREW();
                                SP_IPOD_PlayPauseCurrentTrack();
                            }
                            else if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PLAY)
                            {
                                mNeedPlay = true;
                                SP_IPOD_PlayPauseCurrentTrack();
                            }
                            else
                            {
                                mNeedPlay = false;
                            }
                        }
                        return;
                    }
                    if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START
                        || mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START)
                    {
                        mNeedPlay = true;
                        SP_IPOD_ENDFForREW();
                        SP_IPOD_PlayPauseCurrentTrack();
                    }
                    else if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PLAY)
                    {
                        mNeedPlay = true;
                    }
                    else
                    {
                        mNeedPlay = false;
                    }
                    break;
                case RemoteMsgDefine.LP_BT_SOUND_OFF:
                    if (mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPHONE_3G
                        || mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPHONE
                        || mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPHONE_3GS
                        || mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPHONE_4S
                        || mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPHONE_4)
                    {
                        if (mDataLayer.GetIsPlayingVideo())
                        {
                            if (mNeedPlay)
                            {
                                SP_IPOD_PlayPauseCurrentTrack();
                            }
                        }
                        SystemClock.sleep(200);
                        EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_STATE,
                            mDataLayer.GetPlayState(),
                            null);
                        SendToInfoBar(InfoBarDefine.RF_ID3, trackID3);
                        return;
                    }
                    if (mNeedPlay)
                    {
                        actionPlay();
                    }
                    SystemClock.sleep(200);
                    EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_STATE,
                        mDataLayer.GetPlayState(),
                        null);
                    SendToInfoBar(InfoBarDefine.RF_ID3, trackID3);
                    break;
                default:
                    break;
            }
        }
        else if (wParam == RemoteMsgDefine.WP_MAIN_THIRDPART_STATE)
        {
            switch (lParam)
            {
                case RemoteMsgDefine.LP_THIRDPART_ENTER:
                    if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START
                        || mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START)
                    {
                        mNeedPlay = true;
                        SP_IPOD_ENDFForREW();
                        SP_IPOD_PlayPauseCurrentTrack();
                    }
                    else if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PLAY)
                    {
                        actionPause();
                        mNeedPlay = true;
                    }
                    else
                    {
                        mNeedPlay = false;
                    }
                    break;
                case RemoteMsgDefine.LP_THIRDPART_EXIT:
                    if (mNeedPlay)
                    {
                        actionPlay();
                    }
                    SystemClock.sleep(200);
                    EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_STATE,
                        mDataLayer.GetPlayState(),
                        null);
                    SendToInfoBar(InfoBarDefine.RF_ID3, trackID3);
                    break;
                default:
                    break;
            }
        }
    }

    public void OnVolumeChange(int volume)
    {

    }

    private void SendToInfoBar(int rfType, Object obj)
    {
        InfoMedia info = new InfoMedia();
        // 在导航界面下，第一次进入，显示ID信息总是先显示个UnKnown，在这里屏蔽掉
        boolean isSendInfobar = true;
        info.SourceID = eSourceDefine.SRC_IPOD;
        info.RefreshFlag = rfType;
        switch (rfType)
        {
            case InfoBarDefine.RF_ID3:
                trackID3 = (SP_IPOD_ID3)obj;
                if (trackID3 != null)
                {
                    info.Title = ID3Affirm(trackID3.wcTitle);
                    info.Album = ID3Affirm(trackID3.wcAlbum);
                    info.Artist = ID3Affirm(trackID3.wcArtist);
                    if (info.Title.equals("UnKnown"))
                    {
                        isSendInfobar = false;
                    }
                    else
                    {
                        isSendInfobar = true;
                    }
                    Log.d(TAG, "mLauncherCallBack = " + mLauncherCallBack);
                    if (null != mLauncherCallBack)
                    {
                        Log.d(TAG, "info.Title = " + info.Title);
                        mLauncherCallBack.musicInfoCallback(new String[] {
                            info.Title, info.Album, info.Artist});
                    }
                }
                break;
            case InfoBarDefine.RF_CURRENTNUM:
                int index = (Integer)obj;
                info.CurNum = index;
                break;
            case InfoBarDefine.RF_TOTALNUM:
                SP_IPOD_TRACK_INDEX_TIME trackIndex =
                    (SP_IPOD_TRACK_INDEX_TIME)obj;
                info.RefreshFlag |= InfoBarDefine.RF_CURRENTNUM;
                info.CurNum = (int)trackIndex.uIndex;
                info.TotalNum = (int)trackIndex.uTotalIndex;
                break;
            case InfoBarDefine.RF_PLAYSTATE:
                int state = (Integer)obj;
                switch (state)
                {
                    case IPOD_PLAY_STATE.IPOD_PST_STOP:
                    case IPOD_PLAY_STATE.IPOD_PST_PLAY:
                    case IPOD_PLAY_STATE.IPOD_PST_PAUSE:
                        info.PlayState.Set(state);
                        if (state == IPOD_PLAY_STATE.IPOD_PST_PLAY
                            || state == IPOD_PLAY_STATE.IPOD_PST_PAUSE)
                        {
                            info.Title = ID3Affirm(mDataLayer.GetTrackTitle());
                            info.Album = ID3Affirm(mDataLayer.GetTrackAlbum());
                            info.Artist =
                                ID3Affirm(mDataLayer.GetTrackArtist());
                            if (info.Title.equals("UnKnown"))
                            {
                                isSendInfobar = false;
                            }
                            else
                            {
                                isSendInfobar = true;
                            }
                        }
                        break;
                    case IPOD_PLAY_STATE.IPOD_PST_FF_START:
                        info.PlayState.Set(PlayState.FF);
                        break;
                    case IPOD_PLAY_STATE.IPOD_PST_REW_START:
                        info.PlayState.Set(PlayState.Rew);
                        break;

                    default:
                        info.PlayState.Set(PlayState.Play);
                        break;
                }
                info.RefreshFlag |= InfoBarDefine.RF_ID3;
                break;
            default:
                break;
        }
        if (isSendInfobar == true)
        {
            // mIPodApi.SendToInfoBar(info);
        }
    }

    public void OnPowerOff(eAppServiceType appServiceType)
    {
        // TODO Auto-generated method stub

    }

    private int Time2Int(int time)// Bits[31:24]: No Use, Bits[23:16]: Hour,
                                  // Bits[15:8]:Minute, Bits[7:0]:Second
    {
        int strTime = 0;
        byte bHour, bMin, bSec;
        int nTmp;
        nTmp = time / 60;
        bSec = (byte)(time % 60);

        if (nTmp == 0)
        {
            strTime = bSec;
        }
        else if (nTmp > 0 && nTmp < 60)
        {
            bMin = (byte)nTmp;
            strTime = (bMin << 8) | (0xff & bSec);
        }
        else
        {
            bHour = (byte)(nTmp / 60);
            bMin = (byte)(nTmp % 60);
            strTime = (bHour << 16) | (bMin << 8) | (0xff & bSec);
        }
        return strTime;
    }

    private String ID3Affirm(String id3)
    {
        String temp;
        if (id3 == null || id3.isEmpty())
        {
            temp = "UnKnown";
        }
        else
        {
            temp = id3;
        }
        return temp;
    }

    @Override
    public void addConnectStateCallback(
        ConnectStateCallback connectStateCallback)
    {
        mipodConnectHelper.addCallback(connectStateCallback);
    }

    public void connect()
    {
    }

    public void disconnect()
    {
        iPodLogDefines.iPodLog(TAG, "disconnect...");
        destoryIpod();
    }

    @Override
    public void addListChangeListener(OnIPodListChange onIPodListChange)
    {
        this.monIPodListChange = onIPodListChange;
    }

    @Override
    public void actionPlay()
    {
        iPodLogDefines.iPodLog(TAG,
            "************************liu***************mDataLayer.GetPlayState() --->"
                + mDataLayer.GetPlayState());
        if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START
            || mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START)
        {
            SP_IPOD_ENDFForREW();
        }
        else
        {
            IPOD_UI_EVENT event = new IPOD_UI_EVENT();
            event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
            event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
            event.iLParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY;
            miPodOperate.iPOD_OperateHandle(event);
            // SP_IPOD_PlayPauseCurrentTrack();
        }
    }

    @Override
    public void actionPause()
    {
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
        event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
        event.iLParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PAUSE;
        miPodOperate.iPOD_OperateHandle(event);
        // SP_IPOD_PlayPauseCurrentTrack();
    }

    @Override
    public void actionNext()
    {
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
        event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_NEXT_TRACK;
        miPodOperate.iPOD_OperateHandle(event);
    }

    @Override
    public void actionPrev()
    {
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
        event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PREV_TRACK;
        miPodOperate.iPOD_OperateHandle(event);
    }

    @Override
    public void addNotifyLauncher(LauncherCallBack launcherCallBack)
    {
        this.mLauncherCallBack = launcherCallBack;
    }

    @Override
    public void actionForward(boolean isForward)
    {
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
        if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START)
        {
            event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW;
        }
        else
        {
            event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_FF;
        }
        miPodOperate.iPOD_OperateHandle(event);
    }

    @Override
    public void actionRewind(boolean isRewind)
    {
        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
        event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
        if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START)
        {
            event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW;
        }
        else
        {
            event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_REW;
        }
        miPodOperate.iPOD_OperateHandle(event);
    }
}
