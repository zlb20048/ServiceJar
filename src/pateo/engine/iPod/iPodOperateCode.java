package pateo.engine.iPod;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import pateo.com.ApplicationApi.IPodApi;
import pateo.com.ApplicationApi.TraceManager;
import pateo.com.global.TypeConvert;
import pateo.com.iPod.iPodDefine.IPOD_LOAD_MEMORY_STATE;
import pateo.com.iPod.iPodDefine.IPOD_PLAY_STATE;
import pateo.com.iPod.iPodDefine.IPOD_REPEAT_STATUS;
import pateo.com.iPod.iPodDefine.IPOD_SHUFFLE_STATUS;
import pateo.com.iPod.iPodDefine.SP_IPOD_LIST_CATEGORY_TYPE;
import pateo.com.iPod.iPodDefine.SP_IPOD_LIST_FILE_TYPE;
import pateo.com.iPod.iPodDefine.SP_IPOD_REFRESH_ENUM;
import pateo.com.iPod.iPodDefine.SP_IPOD_REF_TRACK_TYPE_ENUM;
import pateo.com.iPod.iPodDefine.SP_MEDIA_TYPE_ENUM;
import pateo.com.iPod.iPodDefine.iPodLogDefines;
import pateo.com.iPod.iPodType.SP_IPOD_ID3;
import pateo.com.iPod.iPodType.SP_IPOD_OSD;
import pateo.com.iPod.iPodType.SP_IPOD_TRACK_INDEX_TIME;
import pateo.com.iPod.iPodType.SongData;
import pateo.engine.iPod.iPodCommandInterface.iPodCmdCallBack;
import pateo.engine.iPod.iPodCommandInterface.iPodCmdSendInterface;
import pateo.engine.iPod.iPodDataDef.ACK_COMMAND_ERROR_CODES;
import pateo.engine.iPod.iPodDataDef.HANDLE_EVENT;
import pateo.engine.iPod.iPodDataDef.IPOD_AUDIO_SPECIFIC_BTN_BITMASK;
import pateo.engine.iPod.iPodDataDef.IPOD_CATEGORY_TYPES;
import pateo.engine.iPod.iPodDataDef.IPOD_CONTEXT_BTN_BITMASK;
import pateo.engine.iPod.iPodDataDef.IPOD_IMG_SPECIFIC_BTN_BITMASK;
import pateo.engine.iPod.iPodDataDef.IPOD_LINGO_ID;
import pateo.engine.iPod.iPodDataDef.IPOD_MAIN_TASK;
import pateo.engine.iPod.iPodDataDef.IPOD_MODEL_IDS;
import pateo.engine.iPod.iPodDataDef.IPOD_PLAY_CONTROL_CMD_CODES;
import pateo.engine.iPod.iPodDataDef.IPOD_PLAY_STATE_MASK_BIT;
import pateo.engine.iPod.iPodDataDef.IPOD_RET_RESULT_VALUE;
import pateo.engine.iPod.iPodDataDef.IPOD_UI_EVENT;
import pateo.engine.iPod.iPodDataDef.IPOD_VIDEO_SPECIFIC_BTN_BITMASK;
import pateo.engine.iPod.iPodDataDef.PLAY_STATUS_CHANGE_NOTIFICATION;
import pateo.engine.iPod.iPodDataDef.RX_PACKET_CODE;
import pateo.engine.iPod.iPodDataDef.RX_PACKET_PARAM;
import pateo.engine.iPod.iPodDataDef.STRUCT_ARTWORK;
import pateo.engine.iPod.iPodDataDef.TRACK_CAPABLITIES_BITS;
import pateo.engine.iPod.iPodDataDef.TRACK_INFO_TYPES;
import pateo.engine.iPod.iPodDataDef.TX_PACKET_CODE;
import pateo.engine.iPod.iPodDataDef.TX_PACKET_PARAM;
import pateo.engine.iPod.iPodDataDef.UIEventQueue;
import pateo.engine.iPod.iPodDataDef.iPodArtWorkFormat;
import pateo.engine.iPod.iPodDataDef.iPodPixelForamt;
import pateo.frameworks.api.iPodJNI.IPOD_AUDIO_TYPE;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.SystemClock;
import android.util.Log;

public class iPodOperateCode
{
    private static final String TAG = "iPodOperateCode";

    private static final int TRACK_COUNT_ONE_TIME = 20;

    private static final int IPOD_TASK_MAX = 30;// 任务队列最大个数

    // 任务超时时间
    private static final int TIME_OUT_TIMER = 50;// 50*50=2500ms

    private static final int TIME_OUT_ARTWORK_TIMER = 100;// 100*50=5s

    private static final int TIME_OUT_LONG_TIMER = 1000;// 1000*50=50s

    private iPodDataLayer mDataLayer;

    private UIEventQueue<IPOD_UI_EVENT> miPodUIEvent;

    // 任务队列
    private LinkedList<IPOD_MAIN_TASK> mTask;

    // 临时任务队列
    private LinkedList<IPOD_MAIN_TASK> mTempTask;

    // 临时UI引起的任务队列
    private LinkedList<IPOD_MAIN_TASK> mUITempTask;

    private IPOD_MAIN_TASK mCuriPodTask;

    private long mTaskTimer;

    private int mTaskRetryCount;

    private boolean m_bEnterTaskManager = false;

    private byte[] mSycTask = new byte[0];

    private byte[] mSycUIEvent = new byte[0];

    private TX_PACKET_PARAM mTxPacketPar = new TX_PACKET_PARAM();

    private STRUCT_ARTWORK mstrArtwork = new STRUCT_ARTWORK();// ArtWork图片

    private ArrayList<iPodArtWorkFormat> mArtFormats =
        new ArrayList<iPodDataDef.iPodArtWorkFormat>();// ArtWork格式

    private int mCurrentFormatID;// 当前的ArtWork格式

    private boolean mbIsNeedRefreshList;

    private boolean mbUseFourByteFormNotify;

    private boolean mbInit_GetTrackNum;

    private long mPrepareSetIndex;

    private int mRecordReadCount;

    private long mPreparePlayingSongIndex;

    private long mNeedUpdateIndex;

    private byte mPlayControlType = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_Unknown;

    private boolean mbIsiPodInit;

    private boolean mbSelListTrackToPlay;//

    private boolean mbSyncPath;

    private boolean mbSetShuffle;

    private iPodCmdSendInterface mSendCommand = null;

    private iPodCmdCallBack mCmdCallBack = null;

    private long iPodUIEventTimeCount = 0;

    TreeManager mTreeManager;

    private Timer mTimer = null;

    private TimerTask mTimerTask = null;

    SP_IPOD_LIST_CATEGORY_TYPE eNeedSelCategoryType = null;

    private ArrayList<SongData> mReceiveData;

    private long mNowPlayIndex = -1;

    public iPodOperateCode(iPodCmdSendInterface cmdSendInterface,
        iPodCmdCallBack cmdCallBack)
    {
        mSendCommand = cmdSendInterface;
        mCmdCallBack = cmdCallBack;
        miPodUIEvent = new UIEventQueue<iPodDataDef.IPOD_UI_EVENT>();
        mTreeManager = new TreeManager();
        mDataLayer = iPodDataLayer.GetInstant();
        mTask = new LinkedList<IPOD_MAIN_TASK>();
        mTempTask = new LinkedList<IPOD_MAIN_TASK>();
        mUITempTask = new LinkedList<IPOD_MAIN_TASK>();
        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
        mDataLayer.SetiPodModelID(IPOD_MODEL_IDS.IPOD_MODEL_IDS_NOTHING);
        mDataLayer.SetMediaType(SP_MEDIA_TYPE_ENUM.SP_MEDIA_Unknow);
        mbSelListTrackToPlay = false;
        mbSyncPath = false;
        mReceiveData = new ArrayList<SongData>();
    }

    public void iPodInit()
    {
    }

    public void SeviceAddTask()
    {
        mTask.add(IPOD_MAIN_TASK.ITASK_RequestRemoteUIMode);
    }

    public boolean IPOD_Connect(SP_MEDIA_TYPE_ENUM nMediaType)
    {
        iPodLogDefines.iPodLog(TAG, "IPOD_Connect->Type:" + nMediaType);
        iPodLogDefines.iPodLog(TAG,
            "mDataLayer.GetMediaType() = " + mDataLayer.GetMediaType());
        if (nMediaType == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Unknow)
        {
            mDataLayer.SetMediaType(nMediaType);
            return true;
        }
        else if (mDataLayer.GetMediaType() == nMediaType)
        {
            return true;
        }
        else
        {
            iPodLogDefines.iPodLog(TAG, "IPOD_Connect->begin" + nMediaType);
            mPlayControlType = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_Unknown;
            mbIsiPodInit = true;
            mbSetShuffle = false;
            mRecordReadCount = 0;
            mDataLayer.SetMediaType(nMediaType);
            // Meidatype赋值后才能初始化列表
            InitRootList();
            synchronized (mSycTask)
            {
                mTask.clear();
                mTempTask.clear();
                mUITempTask.clear();
                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                mTask.add(IPOD_MAIN_TASK.ITASK_RequestRemoteUIMode);
            }

            if (mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPOD_5G
                && (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO))
            {
                TraceManager.LogE(TAG, "IPOD_MODEL_IDS.IPOD_5G！");
                TraceManager.LogE(TAG,
                    "*********************1*********************！");
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_AUDIO_TYPE,
                    IPodApi.LP_IPOD_DLL_AUDIO_ANALOG,
                    0);
            }
            else if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
            {
                TraceManager.LogE(TAG,
                    "*********************2*********************！");
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_SET_AUDIO_TYPE,
                    IPOD_AUDIO_TYPE.AUDIO_ANALOG,
                    0);
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_AUDIO_TYPE,
                    IPodApi.LP_IPOD_DLL_AUDIO_ANALOG,
                    0);
            }
            else
            {
                TraceManager.LogE(TAG,
                    "*********************3*********************！");
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_SET_AUDIO_TYPE,
                    IPOD_AUDIO_TYPE.AUDIO_DIGITAL,
                    0);
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_AUDIO_TYPE,
                    IPodApi.LP_IPOD_DLL_AUDIO_DIGITAL,
                    0);
            }
            InitTimer();
            iPodLogDefines.iPodLog(TAG, "IPOD_Connect->end");
            return true;
        }
    }

    private void iPodTaskManager(boolean bIsTimerOut)
    {
        // iPodLogDefines.iPodLog(TAG, "iPodTaskManager");
        if (m_bEnterTaskManager)
        {
            return;
        }
        m_bEnterTaskManager = true;
        if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_idle)
        {
            mTaskTimer = 0;
            synchronized (mSycTask)
            {
                if (mTask.size() > 0)
                {
                    TraceManager.LogE(TAG, "iPodTaskManager mTask.size()"
                        + mTask.size());
                    iPodProcessTask(mTask.pollFirst(), bIsTimerOut);
                }
                else
                {
                    iPodProcessTask(IPOD_MAIN_TASK.ITASK_idle, bIsTimerOut);
                }
            }
        }
        else
        {
            if (mTaskTimer != 0)
            {
                mTaskTimer--;

                if (mTaskTimer == 0)
                {
                    if (++mTaskRetryCount > 3)
                    {
                        // give up current task
                        if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_EnterRemoteUIMode)
                        {
                            // 第一次发命令给iPod失败
                            // 关闭定时器
                            mTimer.cancel();
                            // 清除所有任务
                            // 清除任务
                            mTask.clear();
                            // 清除临时任务队列
                            mTempTask.clear();
                            // 清除UI临时任务队列
                            mUITempTask.clear();
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            mTaskRetryCount = 0;
                            // 关闭
                        }// m_iPodMainTask==ITASK_EnterRemoteUIMode
                        else
                        {
                            // 发命令给iPod失败
                            // 清除任务
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            mTaskRetryCount = 0;
                        }
                    }// ++m_uTaskRetryCnt > 3
                    else
                    {
                        if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_GetIndexedPlayingTrackArtWorkCount)
                        {
                            // 超时
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            mTaskRetryCount = 0;
                        }
                        else if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_GetTrackArtworkData)
                        {
                            // 超时
                            // RETAILMSG(1,(L"Del ITASK_GetTrackArtworkData\r\n"));
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            mTaskRetryCount = 0;
                        }
                        else if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_PlayControl)
                        {
                            // 超时
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            mTaskRetryCount = 0;
                        }
                        else if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_GetPlayStatus)
                        {
                            // 超时
                            // RETAILMSG(1,(L"Del ITASK_GetPlayStatus\r\n"));
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            mTaskRetryCount = 0;
                        }
                        else if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_RetrieveCategorizedDBRecords)
                        {
                            mRecordReadCount =
                                (int)mTxPacketPar.iDbRecordReadCount;
                            iPodProcessTask(mCuriPodTask, bIsTimerOut);
                        }
                        else
                        {
                            // process the task again!!!
                            iPodProcessTask(mCuriPodTask, bIsTimerOut);
                        }
                    }
                }
            }
        }
        m_bEnterTaskManager = false;
    }

    private void iPodProcessTask(IPOD_MAIN_TASK eTask, boolean bIsTimerOut)
    {
        if (eTask != IPOD_MAIN_TASK.ITASK_idle)
        {
            iPodLogDefines.iPodLog(TAG, "iPodProcessTask->eTask:" + eTask);
        }
        if (eTask == IPOD_MAIN_TASK.ITASK_Change_AudioType)
        {
            mCuriPodTask = IPOD_MAIN_TASK.ITASK_Change_AudioType;
        }
        if ((mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
            && mDataLayer.GetIsSupportVideo() == false
            && IPOD_MAIN_TASK.ITASK_ResetDBSelection == eTask)
        {
            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
            return;
        }
        switch (eTask)
        {
            case ITASK_idle:
            {
                synchronized (mSycUIEvent)
                {
                    // 处理UI任务
                    UiEventResp();
                }
            }
                break;
            // 认证相关任务End********************************
            case ITASK_Change_AudioType:
            {
            }
                break;
            case ITASK_UI_ChangeType:// 改变播放类型
            {
            }
                break;
            case ITASK_Connect_ChangeTypeWait:// 调用IPOD_Connect函数时等待音频类型改变完成
            {
            }
                break;
            case ITASK_DeInit_ChangeTypeWait:// 调用IPOD_DeInit函数时等待音频类型改变完成
            {
            }
                break;
            case ITASK_RequestRemoteUIMode:
            {
                // iPodLogDefines.iPodLog(TAG,
                // "iPodProcessTask->ITASK_RequestRemoteUIMode");
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_RequestRemoteUIMode,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetSupportedEventNotification:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetSupportedEventNotification,
                    mTxPacketPar);
            }
                break;
            case ITASK_SetEventNotification:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_SetEventNotification,
                    mTxPacketPar);
            }
                break;
            case ITASK_EnterRemoteUIMode:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_EnterRemoteUIMode,
                    mTxPacketPar);
            }
                break;
            case ITASK_ExitRemoteUIMode:
            {
            }
                break;
            case ITASK_RequestiPodName:
            {
            }
                break;
            case ITASK_RequestiPodSerialNum:
            {
            }
                break;
            case ITASK_GetiPodOptionsForExtendedLingo:
            {
            }
                break;
            case ITASK_GetiPodOptionsForSimpleRemoteLingo:
            {
            }
                break;
            case ITASK_RequestExtendedLingoProtocolVersion:
            {
            }
                break;
            case ITASK_RequestSimpleRemoteLingoProtocolVersion:
            {
            }
                break;
            case ITASK_SetiPodPreferences:
            {
            }
                break;
            case ITASK_ContextButtonStatus:
            {
            }
                break;
            case ITASK_ImageButtonStatus:
            {
            }
                break;
            case ITASK_VideoButtonStatus:
            {
            }
                break;
            case ITASK_AudioButtonStatus:
            {
            }
                break;
            case ITASK_GetCurrentPlayingTrackCahpterInfo:
            {
            }
                break;
            case ITASK_GetIndexedPlayingTrackInfo:
            {
                mCuriPodTask = eTask;
                mTxPacketPar.bTrackInfoType =
                    TRACK_INFO_TYPES.TRACK_CAPABILITIES_AND_INFOMATION;
                mTxPacketPar.iTrackIndex = mNeedUpdateIndex;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetIndexedPlayingTrackInfo,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetIndexedPlayingTrackReleaseDate:
            {
                mCuriPodTask = eTask;
                mTxPacketPar.bTrackInfoType =
                    TRACK_INFO_TYPES.TRACK_RELEASE_DATE;
                mTxPacketPar.iTrackIndex = mNeedUpdateIndex;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetIndexedPlayingTrackInfo,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetIndexedPlayingTrackGenre:
            {
                mCuriPodTask = eTask;
                mTxPacketPar.bTrackInfoType = TRACK_INFO_TYPES.TRACK_GENRE;
                mTxPacketPar.iTrackIndex = mNeedUpdateIndex;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetIndexedPlayingTrackInfo,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetIndexedPlayingTrackArtWorkCount:
            {
                mCuriPodTask = eTask;
                mTxPacketPar.bTrackInfoType =
                    TRACK_INFO_TYPES.TRACK_ARTWORK_COUNT;
                mTxPacketPar.iTrackIndex = mNeedUpdateIndex;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetIndexedPlayingTrackInfo,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetArtworkFormats:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetArtworkFormats,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetTrackArtworkData:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetTrackArtworkData,
                    mTxPacketPar);
            }
                break;
            case ITASK_ResetDBSelection:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_ResetDBSelection,
                    mTxPacketPar);
            }
                break;
            case ITASK_ResetDBSelection_NoFresh:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_ResetDBSelection,
                    mTxPacketPar);
            }
                break;
            case ITASK_SelectDBRecord:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_SelectDBRecord,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetNumberCategorizedDBRecords:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetNumberCategorizedDBRecords,
                    mTxPacketPar);
            }
                break;
            case ITASK_RetrieveCategorizedDBRecords:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_RetrieveCategorizedDatabaseRecords,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetPlayStatus:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetPlayStatus,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetCurrentPlayingTrackIndex:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetCurrentPlayingTrackIndex,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetIndexedPlayingTrackTitle:
            {
                mCuriPodTask = eTask;
                mTxPacketPar.iTrackIndex = mNeedUpdateIndex;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetIndexedPlayingTrackTitle,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetIndexedPlayingTrackArtistName:
            {
                mCuriPodTask = eTask;
                mTxPacketPar.iTrackIndex = mNeedUpdateIndex;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetIndexedPlayingTrackArtistName,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetIndexedPlayingTrackAlbumName:
            {
                mCuriPodTask = eTask;
                mTxPacketPar.iTrackIndex = mNeedUpdateIndex;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetIndexedPlayingTrackAlbumName,
                    mTxPacketPar);
            }
                break;
            case ITASK_SetPlayStatusChangeNotification:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_SetPlayStatusChangeNotification,
                    mTxPacketPar);
            }
                break;
            case ITASK_PlayCurrentSelection:
            {
                TraceManager.LogE(TAG, "iPodProcessTask" + eTask
                    + "mTxPacketPar" + mTxPacketPar);
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_PlayCurrentSelection,
                    mTxPacketPar);
            }
                break;
            case ITASK_PlayControl:
            {
                mCuriPodTask = eTask;
                mPlayControlType = mTxPacketPar.bPlayControlCmdCode;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_PlayControl,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetTrackArtworkTimes:
            {
            }
                break;
            case ITASK_GetShuffle:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetShuffle,
                    mTxPacketPar);
            }
                break;
            case ITASK_SetShuffle:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_SetShuffle,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetRepeat:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetRepeat,
                    mTxPacketPar);
            }
                break;
            case ITASK_SetRepeat:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_SetRepeat,
                    mTxPacketPar);
            }
                break;
            case ITASK_SetDisplayImage:
            {
            }
                break;
            case ITASK_GetMonoDisplayImageLimits:
            {
            }
                break;
            case ITASK_GetNumPlayingTracks:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetNumPlayingTracks,
                    mTxPacketPar);
            }
                break;
            case ITASK_SetCurrentPlayingTrack:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_SetCurrentPlayingTracks,
                    mTxPacketPar);
            }
                break;
            case ITASK_GetColorDisplayImageLimits:
            {
            }
                break;
            case ITASK_ResetDBSelectionHierarchy:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_ResetDBSelectionHierarchy,
                    mTxPacketPar);
            }
                break;
            case ITASK_DigitalAudioLingoAccAck:
            {
            }
                break;
            case ITASK_UITask_ReturnCategoryList:
            {
                TraceManager.LogE(TAG,
                    "ITASK_UITask_ReturnCategoryList!!!!!!!!!!!!!!!!!!");
                long iIndex = 0;// 树中的索引
                SongData _Song;
                // 判断树的孩子节点类型
                byte needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_UNKNOWN;

                switch (eNeedSelCategoryType)
                {
                    case LIST_CATEGORY_UNKNOWN:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_UNKNOWN;
                        break;
                    case LIST_CATEGORY_PLAYLISTS:
                        needlistType =
                            SP_IPOD_LIST_FILE_TYPE.LIST_FILE_PlayList;
                        break;
                    case LIST_CATEGORY_ARTISTS:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Artist;
                        break;
                    case LIST_CATEGORY_SONGS:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Songs;
                        break;
                    case LIST_CATEGORY_ALBUMS:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Album;
                        break;
                    case LIST_CATEGORY_COMPOSERS:
                        needlistType =
                            SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Composer;
                        break;
                    case LIST_CATEGORY_AUDIOBOOKS:
                        needlistType =
                            SP_IPOD_LIST_FILE_TYPE.LIST_FILE_AudioBook;
                        break;
                    case LIST_CATEGORY_GENRES:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Genre;
                        break;
                    case LIST_CATEGORY_PODCASTS:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Podcast;
                        break;
                    case LIST_CATEGORY_MOVIES:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_FOLDER;
                        break;
                    case LIST_CATEGORY_MUSIC_VIDEOS:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_FOLDER;
                        break;
                    case LIST_CATEGORY_TV_SHOWS:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_FOLDER;
                        break;
                    case LIST_CATEGORY_VIDEO_PODCASTS:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_FOLDER;
                        break;
                    case LIST_CATEGORY_RENTALS:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_FOLDER;
                        break;
                    default:
                        needlistType = SP_IPOD_LIST_FILE_TYPE.LIST_FILE_UNKNOWN;
                        break;
                }
                // 没有符合要求的树的孩子节点
                if (needlistType == SP_IPOD_LIST_FILE_TYPE.LIST_FILE_UNKNOWN)
                {
                    break;
                }
                // 查找符合要求的树的孩子节点
                boolean bIsFindOk = false;
                long uMax = mTreeManager.GetChildCapacity();
                // RETAILMSG(1,(L"needlistType=%ld,uMax=%ld\r\n",needlistType,uMax));
                for (long i = 0; i < uMax; i++)
                {
                    _Song = mTreeManager.GetTrackInfo(i);
                    if (_Song != null)
                    {
                        // RETAILMSG(1,(L"GetTrackInfo ok i=%ld,fileType=%ld,name=%s\r\n",i,_Song.fileType,_Song.SongName));
                        if (needlistType != SP_IPOD_LIST_FILE_TYPE.LIST_FILE_FOLDER)
                        {
                            // audio
                            // RETAILMSG(1,(L"i=%ld,fileType=%ld\r\n",i,_Song.fileType));
                            if (_Song.fileType == needlistType)
                            {
                                iIndex = _Song.ID;
                                // RETAILMSG(1,(L"i=%ld,iIndex=%ld\r\n",i,iIndex));
                                bIsFindOk = true;
                            }
                        }
                        else
                        {
                            // movie
                            switch (eNeedSelCategoryType)
                            {
                                case LIST_CATEGORY_MOVIES:
                                {
                                    /*if (wcscmp(_Song.SongName,L"Movies")==0)
                                    {*/
                                    iIndex = 0;// _Song.ID;
                                    bIsFindOk = true;
                                    // }
                                }
                                    break;
                                case LIST_CATEGORY_MUSIC_VIDEOS:
                                {
                                    /*if (wcscmp(_Song.SongName,L"Music Videos")==0)
                                    {*/
                                    iIndex = 1;// _Song.ID;
                                    bIsFindOk = true;
                                    // }
                                }
                                    break;
                                case LIST_CATEGORY_TV_SHOWS:
                                {
                                    /*if (wcscmp(_Song.SongName,L"TV Shows")==0)
                                    {*/
                                    iIndex = 2;// _Song.ID;
                                    bIsFindOk = true;
                                    // }
                                }
                                    break;
                                case LIST_CATEGORY_VIDEO_PODCASTS:
                                {
                                    /*if (wcscmp(_Song.SongName,L"Video Podcasts")==0)
                                    {*/
                                    iIndex = 3;// _Song.ID;
                                    bIsFindOk = true;
                                    // }
                                }
                                    break;
                                case LIST_CATEGORY_RENTALS:
                                {
                                    /*if (wcscmp(_Song.SongName,L"Rentals")==0)
                                    {*/
                                    iIndex = 4;// _Song.ID;
                                    bIsFindOk = true;
                                    // }
                                }
                                    break;
                                default:
                                    break;
                            }
                            // RETAILMSG(1,(L"video iIndex=%ld\r\n",iIndex));
                        }
                    }
                    else
                    {
                        // RETAILMSG(1,(L"GetTrackInfo ng i=%ld,fileType=%ld\r\n",i,_Song.fileType));
                    }
                    if (bIsFindOk == true)
                    {
                        // 已找到
                        break;
                    }
                }

                if (bIsFindOk == true)
                {
                    // RETAILMSG(1,(L"Op_EnterChildFolder iIndex=%ld\r\n",iIndex));
                    Op_EnterChildFolder(iIndex, false);
                }

            }
                break;
            // 初始化处理相关任务Start******************************
            case ITASK_InitTask_ReqCurPlayIndex:
            {
                mCuriPodTask = eTask;
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetCurrentPlayingTrackIndex,
                    mTxPacketPar);
            }
                break;
            case ITASK_InitTask_ReqCurPlayType:
            {
                mCuriPodTask = eTask;
                mTxPacketPar.bTrackInfoType =
                    TRACK_INFO_TYPES.TRACK_CAPABILITIES_AND_INFOMATION;
                mTxPacketPar.iTrackIndex = mDataLayer.GetCurTrackIndex();
                mSendCommand.SendCommand(TX_PACKET_CODE.T_GetIndexedPlayingTrackInfo,
                    mTxPacketPar);
            }
                break;
            case ITASK_InitTask_VideoPlayDefultTrack:
            {
                // mCuriPodTask = eTask;
                mPreparePlayingSongIndex = 0;
                mTxPacketPar.iTrackIndex = mPreparePlayingSongIndex;
                mTask.addLast(IPOD_MAIN_TASK.ITASK_PlayCurrentSelection);
                return;
            }
            // 初始化处理相关任务End******************************
            default:
                return;
        }
        if (eTask != IPOD_MAIN_TASK.ITASK_ContextButtonStatus)
        {
            // mCuriPodTask = eTask;
            // 重置任务处理时间(相对定时器)
            if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_Change_AudioType)
            {
                mTaskTimer = TIME_OUT_LONG_TIMER;
            }
            else if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_GetCurrentPlayingTrackIndex)
            {
                mTaskTimer = TIME_OUT_LONG_TIMER;
            }
            else if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_PlayCurrentSelection)
            {
                // if(mCuriPodTask == ITASK_PlayCurrentSelection
                // &&m_bIsVideoPlayDefultTrackTask==TRUE
                // &&treeManager.GetChildCapacity()==0)
                // {
                // //是否在处理视频默认播放任务
                // // m_bIsVideoPlayDefultTrackTask=FALSE;
                // mTaskTimer = ITASK_idle;
                // }
                // else
                // {
                // mTaskTimer = TIME_OUT_LONG_TIMER;
                // }
            }
            else if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_GetTrackArtworkData)
            {
                mTaskTimer = TIME_OUT_ARTWORK_TIMER;
            }
            else if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_GetPlayStatus)
            {
                mTaskTimer = TIME_OUT_LONG_TIMER;
            }
            else
            {
                mTaskTimer = TIME_OUT_TIMER;
            }
        }
        else
        {
        }
    }

    private void UiEventResp()
    {
        IPOD_UI_EVENT sEvent;
        if (miPodUIEvent.size() == 0)
        {
            // iPodLogDefines.iPodLog(TAG,
            // "UiEventResp->CurCapacity: "+mTreeManager.GetChildCapacity()+" CurSize: "+mTreeManager.GetChildSize());
            if (mTreeManager.GetChildCapacity() != mTreeManager.GetChildSize())
            {
                TraceManager.LogE(TAG, "UiEventResp->ChildCapacity()"
                    + mTreeManager.GetChildCapacity() + "->ChildSize()"
                    + mTreeManager.GetChildSize());
                RefreshList(false, true);
            }
            return;
        }
        sEvent = miPodUIEvent.poll();
        // RETAILMSG(1,(TEXT("Ge=%ld,iW=%ld,iL=%ld\r\n"),sEvent.eventType,sEvent.iWParam,sEvent.iLParam));
        long iWParam = sEvent.iWParam;
        long iLParam = sEvent.iLParam;
        Object obj = sEvent.obj;

        // 清除UI临时任务队列
        mUITempTask.clear();
        /*下面所有新加的任务先全放入临时任务队列，
        最后再从临时任务队列中移入任务队列中*/
        switch (sEvent.eventType)
        {
            case EVENT_CHANGE_OSD:
            {
                // 前面已执行
            }
                break;
            case EVENT_IPOD_PLAY_CONTROL:
            {
                ManageUIPlayControl(iWParam, iLParam);
            }
                break;
            case EVENT_IPOD_SET_SHUFFLE:
            {
                boolean bIsRomote = true;
                switch (mDataLayer.GetMediaType())
                {
                    case SP_MEDIA_Image:
                    {
                        bIsRomote = false;
                        // m_Protocol.m_TxPacketPar.uImgSpeBtnState=IISBB_SHUFFLE_ADVANCE;
                        // SetUITempTask(ITASK_ImageButtonStatus);
                        mTxPacketPar.uContextBtnState =
                            IPOD_CONTEXT_BTN_BITMASK.ICBB_SHUFFLE_SETTING_ADVANCE;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                    }
                        break;
                    case SP_MEDIA_DirectControlAudio:// DirectControlAudio
                        bIsRomote = false;
                        /*m_Protocol.m_TxPacketPar.uAudioSpeBtnState=IASBB_SHUFFLE_SETTING_ADVANCE;
                        SetUITempTask(ITASK_AudioButtonStatus);*/
                        mTxPacketPar.uContextBtnState =
                            IPOD_CONTEXT_BTN_BITMASK.ICBB_SHUFFLE_SETTING_ADVANCE;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                        break;
                    case SP_MEDIA_DirectControlVideo:// DirectControlVideo
                    case SP_MEDIA_REAR_DirectControlVideo:
                        bIsRomote = false;
                        break;
                    default:
                        break;
                }
                if (bIsRomote == true)
                {
                    if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_STOP
                        || mDataLayer.GetIsPlayingVideo())
                        break;
                    switch ((byte)iWParam)
                    {
                        case IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_OFF:// shuffle off
                        {
                            if (mDataLayer.GetShuffleState() != IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_OFF)
                            {
                                mTxPacketPar.bNewShuffleMode =
                                    IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_OFF;
                                mbSetShuffle = true;
                                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SetShuffle);
                                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetShuffle);
                                // mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumPlayingTracks);
                                // mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetCurrentPlayingTrackIndex);
                            }
                        }
                            break;
                        case IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_SONG:// shuffle
                                                                   // tracks
                        {
                            if (mDataLayer.GetShuffleState() != IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_SONG)
                            {
                                mTxPacketPar.bNewShuffleMode =
                                    IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_SONG;
                                mbSetShuffle = true;
                                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SetShuffle);
                                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetShuffle);
                                // mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumPlayingTracks);
                                // mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetCurrentPlayingTrackIndex);
                            }
                        }
                            break;
                        default:
                            break;
                    }
                }
            }
                break;
            case EVENT_IPOD_SET_REPEAT:
            {
                boolean bIsRomote = true;
                switch (mDataLayer.GetMediaType())
                {
                    case SP_MEDIA_Image:
                    {
                        bIsRomote = false;
                        /*m_Protocol.m_TxPacketPar.uImgSpeBtnState=IISBB_REPEAT_ADVANCE;
                        SetUITempTask(ITASK_ImageButtonStatus);*/
                        mTxPacketPar.uContextBtnState =
                            IPOD_CONTEXT_BTN_BITMASK.ICBB_REPEAT_SETTING_ADVANCE;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                    }
                        break;
                    case SP_MEDIA_DirectControlAudio:// DirectControlAudio
                        bIsRomote = false;
                        /*m_Protocol.m_TxPacketPar.uAudioSpeBtnState=IASBB_REPEAT_SETTING_ADVANCE;
                        SetUITempTask(ITASK_AudioButtonStatus);*/
                        mTxPacketPar.uContextBtnState =
                            IPOD_CONTEXT_BTN_BITMASK.ICBB_REPEAT_SETTING_ADVANCE;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                        break;
                    case SP_MEDIA_DirectControlVideo:// DirectControlVideo
                    case SP_MEDIA_REAR_DirectControlVideo:
                        bIsRomote = false;
                        break;
                    default:
                        break;
                }
                if (bIsRomote == true)
                {
                    if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_STOP)
                        break;
                    switch ((byte)iWParam)
                    {
                        case IPOD_REPEAT_STATUS.IPOD_REPEAT_OFF:// repeat off
                        {
                            // off
                            if (mDataLayer.GetPlayState() != IPOD_REPEAT_STATUS.IPOD_REPEAT_OFF)
                            {
                                mTxPacketPar.bNewRepeatStatus =
                                    IPOD_REPEAT_STATUS.IPOD_REPEAT_OFF;
                                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SetRepeat);
                                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetRepeat);
                            }
                        }
                            break;
                        case IPOD_REPEAT_STATUS.IPOD_REPEAT_ONE_TRACK:// repeat
                                                                      // one
                                                                      // track
                        {
                            // One track
                            if (mDataLayer.GetRepeatState() != IPOD_REPEAT_STATUS.IPOD_REPEAT_ONE_TRACK)
                            {
                                mTxPacketPar.bNewRepeatStatus =
                                    IPOD_REPEAT_STATUS.IPOD_REPEAT_ONE_TRACK;
                                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SetRepeat);
                                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetRepeat);
                            }
                        }
                            break;
                        case IPOD_REPEAT_STATUS.IPOD_REPEAT_ALL_TRACKS:// repeat
                                                                       // all
                                                                       // tracks
                        {
                            // all track
                            if (mDataLayer.GetRepeatState() != IPOD_REPEAT_STATUS.IPOD_REPEAT_ALL_TRACKS)
                            {
                                mTxPacketPar.bNewRepeatStatus =
                                    IPOD_REPEAT_STATUS.IPOD_REPEAT_ALL_TRACKS;
                                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SetRepeat);
                                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetRepeat);
                            }
                        }
                            break;
                        default:
                            break;
                    }
                }
            }
                break;
            case EVENT_IPOD_REQ_CHILDREN:// 请求列表子孩子
            {
            }
                break;
            case EVENT_IPOD_REQ_NAME:
                break;
            case EVENT_IPOD_MENU:
            {
                Op_ReturnParentFolder();
            }
                break;
            case EVENT_IPOD_REQ_PLAYLIST:
            {
                Op_EnterChildFolder(iWParam, true);
            }
                break;
            case EVENT_LIST_RECORDS:// 请求列表记录
            {
            }
                break;
            case EVENT_IPOD_REQ_CATEGORY_TYPE_LIST:// 请求指定类别的列表
            {
                eNeedSelCategoryType = (SP_IPOD_LIST_CATEGORY_TYPE)obj;
                // 是否有Rentals列表
                TraceManager.LogE(TAG, "EVENT_IPOD_REQ_CATEGORY_TYPE_LIST");
                if ((mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
                    && mDataLayer.GetIsSupportVideo() == false
                    && (eNeedSelCategoryType == SP_IPOD_LIST_CATEGORY_TYPE.LIST_CATEGORY_MOVIES
                        || eNeedSelCategoryType == SP_IPOD_LIST_CATEGORY_TYPE.LIST_CATEGORY_MUSIC_VIDEOS
                        || eNeedSelCategoryType == SP_IPOD_LIST_CATEGORY_TYPE.LIST_CATEGORY_TV_SHOWS
                        || eNeedSelCategoryType == SP_IPOD_LIST_CATEGORY_TYPE.LIST_CATEGORY_VIDEO_PODCASTS || eNeedSelCategoryType == SP_IPOD_LIST_CATEGORY_TYPE.LIST_CATEGORY_RENTALS))
                {
                    // 不支持视频
                    // m_bSyncPath = false;
                    // 刷新空列表给UI
                    Log.e(TAG, "UiEventResp%%%%%%%%%%%%%%%%%%");
                    mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST,
                        IPOD_LOAD_MEMORY_STATE.IPOD_LOAD_START,
                        null);
                    mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST,
                        IPOD_LOAD_MEMORY_STATE.IPOD_LOAD_END,
                        null);
                    if (eNeedSelCategoryType == SP_IPOD_LIST_CATEGORY_TYPE.LIST_CATEGORY_MOVIES)
                    {
                        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
                            0,
                            mDataLayer.GetiPodOSD().wcMovies);
                    }
                    else if (eNeedSelCategoryType == SP_IPOD_LIST_CATEGORY_TYPE.LIST_CATEGORY_MUSIC_VIDEOS)
                    {
                        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
                            0,
                            mDataLayer.GetiPodOSD().wcMusicVideos);
                    }
                    else if (eNeedSelCategoryType == SP_IPOD_LIST_CATEGORY_TYPE.LIST_CATEGORY_TV_SHOWS)
                    {
                        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
                            0,
                            mDataLayer.GetiPodOSD().wcTVShows);
                    }
                    else if (eNeedSelCategoryType == SP_IPOD_LIST_CATEGORY_TYPE.LIST_CATEGORY_VIDEO_PODCASTS)
                    {
                        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
                            0,
                            mDataLayer.GetiPodOSD().wcVideoPodcasts);
                    }
                    else if (eNeedSelCategoryType == SP_IPOD_LIST_CATEGORY_TYPE.LIST_CATEGORY_RENTALS)
                    {
                        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
                            0,
                            mDataLayer.GetiPodOSD().wcRentals);
                    }
                }
                else
                {
                    // m_bSyncPath = false;
                    mTreeManager.ReturnRootLevel();
                    if (!mTreeManager.IsAudioDisplay())
                    {
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ResetDBSelectionHierarchy);
                    }
                    else
                    {
                        // Reset DB,且刷新根目录列表
                        // mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ResetDBSelection);
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ResetDBSelection_NoFresh);

                    }
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_UITask_ReturnCategoryList);
                }
            }
                break;
            case EVENT_IPOD_PLAY_CURRENT_SONG:
            {
                mTxPacketPar.uSelectIndex = iWParam;
                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SetCurrentPlayingTrack);
                // iPod从videolist界面选曲后过来，play的状态不对，重新获取一下
                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
            }
                break;
            case EVENT_IPOD_BACK_TOPMENU:
            {
            }
                break;
            case EVENT_IPOD_PLAY_VIDEO:
            {
            }
                break;
            case EVENT_IPOD_VIDEO_SUBTITLE:
            {
            }
                break;
            case EVENT_IPOD_ERROR:
                break;
            case EVENT_IPOD_SYNCLIST:
            {
                if (!mbSyncPath)
                {
                    iPodLogDefines.iPodLog(TAG, "EVENT_IPOD_SYNCLIST->SyncPath");
                    mbSyncPath = true;
                    SyncPath();
                }
            }
                break;
            case EVENT_ABC123_SEARCH:// ABC123Search
            {
            }
                break;
            case EVENT_IPOD_REQ_CURLIST:
            {
                RefreshList(true, false);
            }
                break;
            default:
                break;

        }
        // 将临时任务队列中任务移到到任务队列尾部
        SetUITempTaskToTask(false);
        return;
    }

    private void SetUITempTaskToTask(boolean bFront)
    {
        if (mUITempTask.size() <= 0)
        {
            return;
        }

        if ((mUITempTask.size() > IPOD_TASK_MAX)
            || (mTask.size() > (IPOD_TASK_MAX - mUITempTask.size())))
        {
            return;
        }
        // 进入临界区域
        synchronized (mSycTask)
        {
            if (bFront == true)
            {
                mTask.addAll(0, mUITempTask);
            }
            else
            {
                mTask.addAll(mTask.size(), mUITempTask);
            }
        }
    }

    public void OnRxAnalysedEnter(int rxCode, RX_PACKET_PARAM rxParam)
    {
        // TraceManager.LogE(TAG, "OnRxAnalysedEnte rxCoder"+rxCode);
        // TraceManager.LogE(TAG,
        // "OnRxAnalysedEnte rxParam"+rxParam.bCmdResStatus);
        // if (m_bStartOK == true)//退出开始
        // {
        // if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
        // {
        // if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
        // && rxParam.bTxAckId == TX_PACKET_CODE.T_PlayControl)
        // {
        // //退出DeInit里的pause
        // }
        // }
        // else if (mDataLayer.GetMediaType() ==
        // SP_MEDIA_TYPE_ENUM.SP_MEDIA_Image)
        // {
        // if (rxCode == RX_PACKET_CODE.R_SimpleRemoteLingoACK)
        // {
        // if (rxParam.bTxAckId == TX_PACKET_CODE.T_ImageButtonStatus)
        // {
        // //退出DeInit里的pause
        // }
        // }
        // }
        // return;
        // }
        // iPod发送数据失败
        // iPodLogDefines.iPodLog(TAG, "OnRxAnalysedEnter->rxCode:"+rxCode);
        if (rxCode == RX_PACKET_CODE.R_Error)
        {
            // 发命令给iPod失败
            // RETAILMSG(1,(L"Send Command NG\r\n"));
            // 关闭定时器
            if (mTimer != null)
            {
                // RETAILMSG(1,(L"Kill Timer1\r\n"));
                mTimer.cancel();
            }
            // 清除所有任务
            // 清除任务
            synchronized (mSycTask)
            {
                mTask.clear();
                // 清除临时任务队列
                mTempTask.clear();
                // 清除UI临时任务队列
                mUITempTask.clear();
            }
            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;

            // 第一次发命令给iPod失败
            // 关闭
            // IPOD_DeInit(FALSE);

            // 退出DeInit里的pause
            return;
        }
        if (rxCode == RX_PACKET_CODE.R_Nothing
            && (mCuriPodTask != IPOD_MAIN_TASK.ITASK_Change_AudioType))
        {
            // RETAILMSG(1,(L"iPod R_Nothing Start\r\n"));

            // 清除所有任务
            // 清除任务
            synchronized (mSycTask)
            {
                mTask.clear();
                // 清除临时任务队列
                mTempTask.clear();
                // 清除UI临时任务队列
                mUITempTask.clear();
            }
            // 初始化UI任务
            miPodUIEvent.clear();
            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
            // RETAILMSG(1,(L"iPod R_Nothing End\r\n"));
            return;
        }

        if (rxCode == RX_PACKET_CODE.R_PlayStatusChangeNotification)
        {

            if (RxPlayStatusChNotification(rxCode, rxParam))
            {
                return;
            }
        }
        else if (rxCode == RX_PACKET_CODE.R_NewiPodTrackInfo)
        {
            // RETAILMSG(OUT_MSG,(TEXT("R_NewiPodTrackInfo\r\n")));
            if (RxNewIpodTrackInfo(rxCode, rxParam) == true)
                return;
        }
        boolean bRes = false;

        // 判断iPod是否认证成功
        if ((rxCode == RX_PACKET_CODE.R_GeneralLingoACK && rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_ERR_NOT_AUTHENTICATED)
            || (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK && rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_ERR_NOT_AUTHENTICATED))
        {
            // 通知UI，iPod没有认证成功
            // RETAILMSG(OUT_MSG,(L"iPod Auth Err\r\n"));
            return;
        }
        if ((rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK
            || rxCode == RX_PACKET_CODE.R_GeneralLingoACK || rxCode == RX_PACKET_CODE.R_SimpleRemoteLingoACK)
            && rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_ERR_BAD_PARAMETER)
        {
            // 参数错误
            if (mCuriPodTask != IPOD_MAIN_TASK.ITASK_SetPlayStatusChangeNotification
                && mCuriPodTask != IPOD_MAIN_TASK.ITASK_GetSupportedEventNotification
                && mCuriPodTask != IPOD_MAIN_TASK.ITASK_SetEventNotification)
            {
                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
            }
        }
        // 清除临时任务队列
        mTempTask.clear();
        /*下面所有新加的任务先全放入临时任务队列，
        最后再从临时任务队列中移入任务队列中*/
        switch (mCuriPodTask)
        // CmdID
        {
            case ITASK_idle:
                break;
            case ITASK_Change_AudioType:
            {
                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
            }
                break;
            case ITASK_UI_ChangeType:// 改变播放类型
            {
                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
            }
                break;
            case ITASK_Connect_ChangeTypeWait:// 调用IPOD_Connect函数时等待音频类型改变完成
            {
                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
            }
                break;
            case ITASK_DeInit_ChangeTypeWait:// 调用IPOD_DeInit函数时等待音频类型改变完成
            {
                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
            }
                break;
            case ITASK_RequestRemoteUIMode:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnRemoteUIMode)
                {
                    if (rxParam.bIsInExtendedInterfaceMode)
                    {
                        iPodLogDefines.iPodLog(TAG,
                            "iPod In ExtendedInterfaceMode");
                        // 当前iPod处于Extended Interface Mode
                        switch (mDataLayer.GetMediaType())
                        {
                            case SP_MEDIA_All:
                                break;
                            case SP_MEDIA_Video:
                            {
                                // 5G iPod视频认证是数字音频通道，但通知UI需要模拟通道
                                UpdateFirstInfor();
                            }
                                break;
                            case SP_MEDIA_REAR_VIDEO:
                            {
                                UpdateFirstInfor();
                            }
                                break;
                            case SP_MEDIA_Audio:
                            {
                                UpdateFirstInfor();
                            }
                                break;
                            case SP_MEDIA_Image:
                                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_ExitRemoteUIMode);
                                mTxPacketPar.uImgSpeBtnState =
                                    IPOD_IMG_SPECIFIC_BTN_BITMASK.IISBB_PLAY_RESUME;
                                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_ImageButtonStatus);
                                break;
                            case SP_MEDIA_DirectControlAudio:// DirectControlAudio
                                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_ExitRemoteUIMode);
                                // 启动
                                if (mDataLayer.GetiPodModelID() != IPOD_MODEL_IDS.IPOD_5G
                                    && mDataLayer.GetiPodModelID() != IPOD_MODEL_IDS.IPOD_NANO)
                                {
                                    // 5G iPod Simple Mode是模拟音频通道
                                    // 1G iPod nano
                                    // RETAILMSG(1,(L"<>Simple Mode HIDAUDIO_Start\r\n"));
                                }
                                /*if (m_eiPodModelID!=IPOD_CLASSIC
                                	&&m_eiPodModelID!=IPOD_CLASSIC_120GB
                                	&&m_eiPodModelID!=IPOD_3G_NANO)
                                {*/
                                mTxPacketPar.uContextBtnState =
                                    IPOD_CONTEXT_BTN_BITMASK.ICBB_PLAY_PAUSE;// ICBB_PLAY_RESUME;
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                                // }
                                break;
                            case SP_MEDIA_DirectControlVideo:// DirectControlVideo
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_ExitRemoteUIMode);
                                // 5G iPod Simple Mode是模拟音频通道
                                if (mDataLayer.GetiPodModelID() != IPOD_MODEL_IDS.IPOD_5G
                                    && mDataLayer.GetiPodModelID() != IPOD_MODEL_IDS.IPOD_NANO)
                                {
                                    // RETAILMSG(1,(L"<>Simple Mode HIDAUDIO_Start\r\n"));
                                }
                                /*if (m_eiPodModelID!=IPOD_CLASSIC
                                	&&m_eiPodModelID!=IPOD_CLASSIC_120GB
                                	&&m_eiPodModelID!=IPOD_3G_NANO)
                                {*/
                                mTxPacketPar.uContextBtnState =
                                    IPOD_CONTEXT_BTN_BITMASK.ICBB_PLAY_PAUSE;// ICBB_PLAY_RESUME;
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                                // }
                                break;
                            case SP_MEDIA_REAR_DirectControlVideo:
                            {
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_ExitRemoteUIMode);
                                /*if (m_eiPodModelID!=IPOD_CLASSIC
                                	&&m_eiPodModelID!=IPOD_CLASSIC_120GB
                                	&&m_eiPodModelID!=IPOD_3G_NANO)
                                {*/
                                mTxPacketPar.uContextBtnState =
                                    IPOD_CONTEXT_BTN_BITMASK.ICBB_PLAY_PAUSE;// ICBB_PLAY_RESUME;
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                                // }
                            }
                                break;
                            case SP_MEDIA_Unknow:
                                break;
                            default:
                                break;
                        }
                    }
                    else
                    {
                        // 当前iPod处于Standard UI Mode
                        switch (mDataLayer.GetMediaType())
                        {
                            case SP_MEDIA_All:
                                break;
                            case SP_MEDIA_Video:
                            {
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_GetSupportedEventNotification);
                                // 5G iPod视频认证是数字音频通道，但通知UI需要模拟通道
                                if (mDataLayer.GetiPodModelID() != IPOD_MODEL_IDS.IPOD_5G)
                                {
                                }
                                UpdateFirstInfor();
                            }
                                break;
                            case SP_MEDIA_REAR_VIDEO:
                            {
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_GetSupportedEventNotification);
                                UpdateFirstInfor();
                            }
                                break;
                            case SP_MEDIA_Audio:
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_GetSupportedEventNotification);
                                UpdateFirstInfor();
                                break;
                            case SP_MEDIA_Image:
                                mTxPacketPar.uImgSpeBtnState =
                                    IPOD_IMG_SPECIFIC_BTN_BITMASK.IISBB_PLAY_RESUME;
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_ImageButtonStatus);
                                break;
                            case SP_MEDIA_DirectControlAudio:// DirectControlAudio
                                // 启动
                                if (mDataLayer.GetiPodModelID() != IPOD_MODEL_IDS.IPOD_5G
                                    && mDataLayer.GetiPodModelID() != IPOD_MODEL_IDS.IPOD_NANO)
                                {
                                    // 5G iPod Simple Mode是模拟音频通道
                                    // 1G iPod nano
                                    // RETAILMSG(1,(L"<>Simple Mode HIDAUDIO_Start\r\n"));
                                }
                                /*if (m_eiPodModelID!=IPOD_CLASSIC
                                	&&m_eiPodModelID!=IPOD_CLASSIC_120GB
                                	&&m_eiPodModelID!=IPOD_3G_NANO)
                                {*/
                                mTxPacketPar.uContextBtnState =
                                    IPOD_CONTEXT_BTN_BITMASK.ICBB_PLAY_PAUSE;// ICBB_PLAY_RESUME;
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                                // }
                                break;
                            case SP_MEDIA_DirectControlVideo:// DirectControlVideo
                                // 5G iPod Simple Mode是模拟音频通道
                                if (mDataLayer.GetiPodModelID() != IPOD_MODEL_IDS.IPOD_5G
                                    && mDataLayer.GetiPodModelID() != IPOD_MODEL_IDS.IPOD_NANO)
                                {
                                    // RETAILMSG(1,(L"<>Simple Mode HIDAUDIO_Start\r\n"));
                                }
                                /*if (m_eiPodModelID!=IPOD_CLASSIC
                                	&&m_eiPodModelID!=IPOD_CLASSIC_120GB
                                	&&m_eiPodModelID!=IPOD_3G_NANO)
                                {*/
                                mTxPacketPar.uContextBtnState =
                                    IPOD_CONTEXT_BTN_BITMASK.ICBB_PLAY_PAUSE;// ICBB_PLAY_RESUME;
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                                // }
                                break;
                            case SP_MEDIA_REAR_DirectControlVideo:
                            {
                                // 5G iPod Simple Mode是模拟音频通道
                                /*if (m_eiPodModelID!=IPOD_CLASSIC
                                	&&m_eiPodModelID!=IPOD_CLASSIC_120GB
                                	&&m_eiPodModelID!=IPOD_3G_NANO)
                                {*/
                                mTxPacketPar.uContextBtnState =
                                    IPOD_CONTEXT_BTN_BITMASK.ICBB_PLAY_PAUSE;// ICBB_PLAY_RESUME;
                                mTempTask.add(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                                // }
                            }
                                break;
                            case SP_MEDIA_Unknow:
                                break;
                            default:
                                break;
                        }
                    }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetSupportedEventNotification:
            {
                if (rxCode == RX_PACKET_CODE.R_RetSupportedEventNotification
                    || rxCode == RX_PACKET_CODE.R_GeneralLingoACK)
                {
                    mTempTask.add(IPOD_MAIN_TASK.ITASK_SetEventNotification);
                }
            }
                break;
            case ITASK_SetEventNotification:
            {
                if (rxCode == RX_PACKET_CODE.R_GeneralLingoACK)
                {
                    mTempTask.add(IPOD_MAIN_TASK.ITASK_EnterRemoteUIMode);
                }
                if (bRes == false)
                {
                    // 取消重发
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    // 通知UI，进入远程UI失败
                    // RETAILMSG(OUT_MSG,(L"iPod EnterRemoteUI Err\r\n"));
                    // UpdateUIInfor(IPOD_DSC_Error_Status,IPOD_ERR_QUERY_PROTOCOL);
                }
            }
                break;
            case ITASK_EnterRemoteUIMode:
            {
                if (rxCode == RX_PACKET_CODE.R_GeneralLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_EnterRemoteUIMode)
                    {
                        // RETAILMSG(1,(L"If logo:data=0x%x,ModelID=0x%x\r\n",m_LogoImg.image_data,m_piPodCtlInfo->chiPodModelID));
                        // 判断是否需要传Logo
                        // if (m_LogoImg.image_data != null
                        // && mDataLayer.GetiPodModelID() !=
                        // IPOD_MODEL_IDS.IPHONE//iPhone
                        // && mDataLayer.GetiPodModelID() !=
                        // IPOD_MODEL_IDS.IPOD_TOUCH//iPod touch
                        // && mDataLayer.GetiPodModelID() !=
                        // IPOD_MODEL_IDS.IPHONE_3G//iPhone 3G
                        // && mDataLayer.GetiPodModelID() !=
                        // IPOD_MODEL_IDS.TOUCH_2G//2G iPod touch
                        // && mDataLayer.GetiPodModelID() !=
                        // IPOD_MODEL_IDS.IPHONE_3GS//iPhone 3GS
                        // && mDataLayer.GetiPodModelID() !=
                        // IPOD_MODEL_IDS.IPOD_TOUCH_2G_2009)//2G touch(2009)
                        // {
                        // if (m_LogoImg.pixelFormat == IPF_MONOCHRONE)
                        // {
                        // mTempTask.add(IPOD_MAIN_TASK.ITASK_GetMonoDisplayImageLimits);
                        // }
                        // else
                        // {
                        // mTempTask.add(IPOD_MAIN_TASK.ITASK_GetColorDisplayImageLimits);
                        // }
                        // }
                        bRes = true;
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_EnterRemoteUIMode)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                        bRes = true;
                    }
                }
                if (bRes == false)
                {
                    // 取消重发
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;

                    // 通知UI，进入远程UI失败
                    // RETAILMSG(OUT_MSG,(L"iPod EnterRemoteUI Err\r\n"));
                    // UpdateUIInfor(IPOD_DSC_Error_Status,IPOD_ERR_QUERY_PROTOCOL);
                }
            }
                break;
            case ITASK_ExitRemoteUIMode:
            {
                if (rxCode == RX_PACKET_CODE.R_GeneralLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_ExitRemoteUIMode)
                    {
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_ExitRemoteUIMode)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                }
            }
                break;
            case ITASK_RequestiPodName:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturniPodName)
                {
                    mDataLayer.SetiPodName(rxParam.wsiPodName);
                    // RETAILMSG(1,(L"ModelID=0x%08x\r\n",m_eiPodModelID));
                    // 检查是否支持视频
                    switch (mDataLayer.GetiPodModelID())
                    {
                        case IPOD_MODEL_IDS.IPOD_3G_WHITE_WHITE_WHEEL:
                        case IPOD_MODEL_IDS.IPOD_MINI_ORIGINAL_4GB:
                        case IPOD_MODEL_IDS.IPOD_4G_WHITE_GRAY_WHEEL:
                        case IPOD_MODEL_IDS.IPOD_PHOTO:
                        case IPOD_MODEL_IDS.IPOD_2G_4GB_6GB_MINI:
                        case IPOD_MODEL_IDS.IPOD_NANO:
                        case IPOD_MODEL_IDS.IPOD_2G_NANO:
                        case IPOD_MODEL_IDS.IPOD_6G_NANO:
                        {
                            mDataLayer.SetIsSupportVideo(false); // 是否支持视频
                            if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video
                                || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
                            {
                                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                                // 通知UI不支持视频文件
                                // UpdateUIInfor(IPOD_DSC_NoMediaFiles,0);
                                // 发送播放状态给UI,解除ClarionUI的死锁
                                mDataLayer.SetPlayState(IPOD_PLAY_STATE.IPOD_PST_STOP);
                                // UpdateUIInfor(IPOD_DSC_Play_Status,IPOD_PST_STOP);
                                break;
                            }
                        }
                            break;
                        case IPOD_MODEL_IDS.IPOD_5G: // 5G iPod
                        case IPOD_MODEL_IDS.IPOD_CLASSIC: // iPod classic
                        case IPOD_MODEL_IDS.IPOD_CLASSIC_120GB:// iPod classic
                                                               // 120GB
                        case IPOD_MODEL_IDS.IPOD_3G_NANO: // 3G iPod nano
                        case IPOD_MODEL_IDS.IPOD_4G_NANO: // 4G iPod nano
                        case IPOD_MODEL_IDS.IPHONE_3G: // iPhone 3G
                        case IPOD_MODEL_IDS.TOUCH_2G: // 2G iPod touch
                        case IPOD_MODEL_IDS.IPHONE_3GS: // iPhone 3GS
                        case IPOD_MODEL_IDS.IPOD_5G_NANO:// 5G iPod nano
                        case IPOD_MODEL_IDS.IPOD_TOUCH_2G_2009:// 2G touch(2009)
                        {
                            mDataLayer.SetIsSupportVideo(true);
                        }
                            break;
                        default:
                            mDataLayer.SetIsSupportVideo(true);// 此IPOD是否支持视频
                            break;
                    }
                    // 更新iPod音频通道
                    // UpdateiPodAudioChannel();
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_RequestiPodSerialNum:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturniPodSerialNum)
                {
                    // 保存当前iPod的序列号
                    mDataLayer.SetiPodSerialNum(rxParam.wsSerialNum);

                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetiPodOptionsForExtendedLingo:
            {
                if (rxCode == RX_PACKET_CODE.R_RetiPodOptionsForLingo)
                {
                    if (rxParam.bLingoRetVer == IPOD_LINGO_ID.EXTENDED_INTERFACE_LINGO)
                    {
                        mTask.add(IPOD_MAIN_TASK.ITASK_RequestExtendedLingoProtocolVersion);
                    }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetiPodOptionsForSimpleRemoteLingo:
            {
                if (rxCode == RX_PACKET_CODE.R_RetiPodOptionsForLingo)
                {
                    if (rxParam.bLingoRetVer == IPOD_LINGO_ID.SIMPLE_REMOTE_LINGO)
                    {
                        mTask.add(IPOD_MAIN_TASK.ITASK_RequestSimpleRemoteLingoProtocolVersion);
                    }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_RequestExtendedLingoProtocolVersion:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnLingoProtocolVersion)
                {
                    if (rxParam.bLingoRetVer == IPOD_LINGO_ID.EXTENDED_INTERFACE_LINGO)
                    {
                        // mDataLayer.SetExtendedLingoVer(rxParam.bLingoVerMajor,
                        // rxParam.bLingoVerMinor);
                    }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_RequestSimpleRemoteLingoProtocolVersion:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnLingoProtocolVersion)
                {
                    if (rxParam.bLingoRetVer == IPOD_LINGO_ID.SIMPLE_REMOTE_LINGO)
                    {
                        // mDataLayer.SetSimpleRemoteLingoVer(rxParam.bLingoVerMajor,
                        // rxParam.bLingoVerMinor);
                    }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_SetiPodPreferences:
                break;
            case ITASK_ContextButtonStatus:
                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                break;
            case ITASK_ImageButtonStatus:
            {
                if (rxCode == RX_PACKET_CODE.R_SimpleRemoteLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_ImageButtonStatus)
                    {
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_ImageButtonStatus)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                }
            }
                break;
            case ITASK_VideoButtonStatus:
            {
                if (rxCode == RX_PACKET_CODE.R_SimpleRemoteLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_VideoButtonStatus)
                    {
                        // 初始化
                        byte tmpState = mDataLayer.GetPlayState();
                        // 更新状态
                        switch (mTxPacketPar.uVideoSpeBtnState)
                        {
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_PLAY_PAUSE:
                            {
                                if (tmpState != IPOD_PLAY_STATE.IPOD_PST_PLAY)
                                {
                                    // 之前不是Play
                                    tmpState = IPOD_PLAY_STATE.IPOD_PST_PLAY;
                                }
                                else
                                {
                                    // 之前是Play
                                    tmpState = IPOD_PLAY_STATE.IPOD_PST_PAUSE;
                                }
                            }
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_NEXT_VIDEO:
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_PREV_VIDEO:
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_STOP:
                            {
                                tmpState = IPOD_PLAY_STATE.IPOD_PST_STOP;
                            }
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_PLAY_RESUME:
                            {
                                tmpState = IPOD_PLAY_STATE.IPOD_PST_PLAY;
                            }
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_PAUSE:
                            {
                                tmpState = IPOD_PLAY_STATE.IPOD_PST_PAUSE;
                            }
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_BEGIN_FF:
                            {
                                tmpState = IPOD_PLAY_STATE.IPOD_PST_FF_START;
                            }
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_BEGIN_REW:
                            {
                                tmpState = IPOD_PLAY_STATE.IPOD_PST_REW_START;
                            }
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_NEXT_CHAPTER:
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_PREV_CHAPTER:
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_NEXT_FRAME:
                                break;
                            case IPOD_VIDEO_SPECIFIC_BTN_BITMASK.IVSBB_PREV_FRAME:
                                break;
                            default:
                                break;
                        }

                        if (mDataLayer.GetPlayState() != tmpState)
                        {
                            mDataLayer.SetPlayState(tmpState);
                            // 通知UI更新播放状态
                            // UpdateUIInfor(IPOD_DSC_Play_Status,tmpState);
                        }
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_VideoButtonStatus)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                }
            }
                break;
            case ITASK_AudioButtonStatus:
            {
                if (rxCode == RX_PACKET_CODE.R_SimpleRemoteLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_AudioButtonStatus)
                    {
                        // 初始化
                        byte tmpState = mDataLayer.GetPlayState();
                        // 更新状态
                        switch (mTxPacketPar.uAudioSpeBtnState)
                        {
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_PLAY_PAUSE:
                            {
                                if (tmpState != IPOD_PLAY_STATE.IPOD_PST_PLAY)
                                {
                                    // 之前不是Play
                                    tmpState = IPOD_PLAY_STATE.IPOD_PST_PLAY;
                                }
                                else
                                {
                                    // 之前是Play
                                    tmpState = IPOD_PLAY_STATE.IPOD_PST_PAUSE;
                                }
                            }
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_VOL_UP:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_VOL_DOWN:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_NEXT_TRACK:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_PREV_TRACK:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_NEXT_ALBUM:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_PREV_ALBUM:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_STOP:
                            {
                                tmpState = IPOD_PLAY_STATE.IPOD_PST_STOP;
                            }
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_PLAY_RESUME:
                            {
                                tmpState = IPOD_PLAY_STATE.IPOD_PST_PLAY;
                            }
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_PAUSE:
                            {
                                tmpState = IPOD_PLAY_STATE.IPOD_PST_PAUSE;
                            }
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_MUTE_TOGGLE:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_NEXT_CHAPTER:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_PREV_CHAPTER:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_NEXT_PLAYLIST:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_PREV_PLAYLIST:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_SHUFFLE_SETTING_ADVANCE:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_REPEAT_SETTING_ADVANCE:
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_BEGIN_FF:
                            {
                                tmpState = IPOD_PLAY_STATE.IPOD_PST_FF_START;
                            }
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_BEGIN_REW:
                            {
                                tmpState = IPOD_PLAY_STATE.IPOD_PST_REW_START;
                            }
                                break;
                            case IPOD_AUDIO_SPECIFIC_BTN_BITMASK.IASBB_RECORD:
                                break;
                            default:
                                break;
                        }
                        // //RETAILMSG(1,(L"UpdatePlayStatus0:Old=%ld,New=%ld\r\n",m_iPodInfo->pstate,tmpState));
                        if (mDataLayer.GetPlayState() != tmpState)
                        {
                            mDataLayer.SetPlayState(tmpState);
                            // 通知UI更新播放状态
                            // UpdateUIInfor(IPOD_DSC_Play_Status,tmpState);
                        }
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_AudioButtonStatus)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                }
            }
                break;
            case ITASK_GetCurrentPlayingTrackCahpterInfo:
                break;
            case ITASK_GetIndexedPlayingTrackInfo:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnIndexedPlayingTrackInfo)
                {
                    // 清空ArtWork结构
                    // UpdateUIInfor(IPOD_DSC_Artwork_Status,0);
                    // ArtWork图片

                    // 图像数据偏移指针
                    if (mstrArtwork.pImage != 0)
                    {
                        // delete []m_strArtwork.pImage;
                        mstrArtwork.pImage = 0;
                        mstrArtwork.telegram_index = 0;
                    }
                    if (mDataLayer.GetArtwork() != null)
                    {
                        mDataLayer.SetArtwork(null);
                    }
                    // 当前播放曲目类别
                    SP_IPOD_REF_TRACK_TYPE_ENUM curTypeTmp =
                        mDataLayer.GetCurTrackType();
                    if (rxParam.bTrackInfoType == TRACK_INFO_TYPES.TRACK_CAPABILITIES_AND_INFOMATION) // 第6为0，得到Track
                                                                                                      // Capabilities
                                                                                                      // and
                                                                                                      // Information
                    {
                        // RETAILMSG(1,(L"TrackInfoType=0x%x\r\n",rxParam.iTrackCapaBits));
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_IS_AUDIOBOOK) != 0)
                        {

                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_HAS_CHAPTERS) != 0)
                        {

                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_HAS_ALBUM_ARTWORK) != 0)// artwork
                        {
                            iPodLogDefines.iPodLog(TAG,
                                "This Song has a Artwork!");
                            // 获取当前ArtWork格式
                            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetIndexedPlayingTrackArtWorkCount);
                        }
                        else
                        {
                            mDataLayer.SetArtwork(null);
                            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_ARTWORK,
                                0,
                                null);
                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_HAS_SONG_LYRICS) != 0)
                        {

                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_HAS_A_PODCAST_EPISODE) != 0)
                        {

                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_HAS_RELEASE_DATE) != 0)// has
                                                                                                          // release
                                                                                                          // date
                        {
                            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetIndexedPlayingTrackReleaseDate);
                        }
                        else
                        {
                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_HAS_DESCRIPTION) != 0)
                        {

                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_CONTAINS_VIDEO) != 0)
                        {

                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_IS_PLAY_AS_A_VIDEO) != 0) // has
                                                                                                             // chapter
                                                                                                             // //POSCAST
                                                                                                             // 既可以当作音乐播，也可以当作视频播
                        {
                            // RETAILMSG(1,(L"SP_IPOD>>Play Video\r\n"));
                            // 视频
                            // if ((mDataLayer.GetMediaType() ==
                            // SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video //||
                            // mDataLayer.GetMediaType() ==
                            // SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO||
                            // mDataLayer.GetMediaType() ==
                            // SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_DirectControlVideo||
                            // mDataLayer.GetMediaType() ==
                            // SP_MEDIA_TYPE_ENUM.SP_MEDIA_DirectControlVideo
                            // ))
                            // {
                            // //过滤掉博客
                            // curTypeTmp =
                            // SP_IPOD_REF_TRACK_TYPE_ENUM.SP_IPOD_REF_TRACK_TYPE_VIDEO;
                            // if (curTypeTmp != mDataLayer.GetCurTrackType())
                            // {
                            // mDataLayer.SetCurTrackType(curTypeTmp);
                            // }
                            // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_TYPE,
                            // 0, mDataLayer.GetCurTrackType());
                            // }
                            if (mDataLayer.GetIsPlayingVideo() == false)
                            {
                                mDataLayer.SetIsPlayingVideo(true);
                            }
                            // 当前播放曲目类别
                            curTypeTmp =
                                SP_IPOD_REF_TRACK_TYPE_ENUM.SP_IPOD_REF_TRACK_TYPE_VIDEO;
                            if (curTypeTmp != mDataLayer.GetCurTrackType())
                            {
                                mDataLayer.SetCurTrackType(curTypeTmp);
                            }
                            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_TYPE,
                                0,
                                mDataLayer.GetCurTrackType());
                        }
                        else
                        {
                            // 音频
                            // RETAILMSG(1,(L"SP_IPOD>>Play Audio\r\n"));
                            if (mDataLayer.GetIsPlayingVideo() == true)
                            {
                                mDataLayer.SetIsPlayingVideo(false);
                            }
                            // 当前播放曲目类别
                            curTypeTmp =
                                SP_IPOD_REF_TRACK_TYPE_ENUM.SP_IPOD_REF_TRACK_TYPE_AUDIO;
                            if (curTypeTmp != mDataLayer.GetCurTrackType())
                            {
                                mDataLayer.SetCurTrackType(curTypeTmp);
                                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_TYPE,
                                    0,
                                    mDataLayer.GetCurTrackType());
                            }
                        }
                        // Total track length,in milliseconds
                        // rxParam.uTrackLen;
                        // Chapter count
                        // rxParam.uChapterCount;
                    }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetIndexedPlayingTrackReleaseDate:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnIndexedPlayingTrackInfo)
                {
                    if (rxParam.bTrackInfoType == TRACK_INFO_TYPES.TRACK_RELEASE_DATE) // 得到Release
                                                                                       // Data
                    {
                        mDataLayer.SetTrackRelease(""
                            + rxParam.sTrackReleaseDate.uYear);
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;

                    }
                }
            }
                break;
            case ITASK_GetIndexedPlayingTrackGenre:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnIndexedPlayingTrackInfo)
                {
                    // RETAILMSG(1,(L"ITASK_GetIndexedPlayingTrackGenre=0x%x",rxParam.bTrackInfoType));
                    if (rxParam.bTrackInfoType == TRACK_INFO_TYPES.TRACK_GENRE) // 得到Track
                                                                                // Genre
                    {
                        mDataLayer.SetTrackGenre(rxParam.wsTrackGenre);
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;

                        SP_IPOD_ID3 trackID3 = new SP_IPOD_ID3();
                        trackID3.wcTitle = mDataLayer.GetTrackTitle();
                        trackID3.wcArtist = mDataLayer.GetTrackArtist();
                        trackID3.wcAlbum = mDataLayer.GetTrackAlbum();
                        trackID3.wcGenre = mDataLayer.GetTrackGenre();
                        trackID3.wcRelease = mDataLayer.GetTrackRelease();
                        trackID3.eType = mDataLayer.GetCurTrackType();
                        TraceManager.LogI(TAG, "SP_IPOD_REF_TRACK_ID3->"
                            + trackID3.wcTitle);
                        TraceManager.LogI(TAG, "SP_IPOD_REF_TRACK_ID3->"
                            + trackID3.wcArtist);
                        TraceManager.LogI(TAG, "SP_IPOD_REF_TRACK_ID3->"
                            + trackID3.wcAlbum);
                        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_ID3,
                            0,
                            trackID3);
                    }
                }
            }
                break;
            case ITASK_GetIndexedPlayingTrackArtWorkCount:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnIndexedPlayingTrackInfo)
                {
                    if (rxParam.bTrackInfoType == TRACK_INFO_TYPES.TRACK_ARTWORK_COUNT) // 得到Track
                                                                                        // artwork
                                                                                        // count
                    {
                        int nFormatID = rxParam.uFormatID;
                        int nCount = rxParam.uImgCount;
                        // RETAILMSG(RETAIL_MSG,(TEXT("Return ACount:FormatID=%04x,nCount=%04x\r\n"),
                        // nFormatID, nCount));
                        if (nCount == 0)
                        {
                            mDataLayer.SetArtwork(null);
                            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_ARTWORK,
                                0,
                                null);
                        }
                        else
                        // if (m_CurrentFormatID != nFormatID)
                        {
                            int nPos = GetArtWorkFormatPos(nFormatID);
                            if (nPos == -1)
                            {
                                mDataLayer.SetArtwork(null);
                                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_ARTWORK,
                                    0,
                                    null);
                                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                                return;
                            }
                            mCurrentFormatID = nFormatID;

                            if (mstrArtwork.image_data != null)
                            {
                                mstrArtwork.image_data = null;
                            }

                            mTxPacketPar.iTrackIndex =
                                mDataLayer.GetCurTrackIndex();
                            mTxPacketPar.uFormatID = mCurrentFormatID;
                            mTxPacketPar.uTimeOffset = 0;
                            mTempTask.add(IPOD_MAIN_TASK.ITASK_GetTrackArtworkData);
                        }
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                }
            }
                break;
            case ITASK_GetArtworkFormats:
            {
                if (rxCode == RX_PACKET_CODE.R_RetArtworkFormats)
                {
                    mArtFormats.clear();
                    mArtFormats.addAll(rxParam.vArtWorkFormats);
                    // 打印Format
                    // for (int i = 0; i< mArtFormats.size(); i++ )
                    // {
                    // }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetTrackArtworkData:
            {
                if (rxCode == RX_PACKET_CODE.R_RetTrackArtworkData)
                {
                    // RETAILMSG(RETAIL_MSG,(L"ITASK_GetTrackArtworkData0\r\n"));
                    if (ReceiveArtworkData(rxParam) == true)
                    {
                        // 获取一次数据成功
                        // 后面的数据返回的定时器
                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = TIME_OUT_ARTWORK_TIMER;
                    }
                    else
                    {
                        // RETAILMSG(RETAIL_MSG,(L"RxArtworkData NG0\r\n"));
                        // 获取Artwork数据失败
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                }
                else
                {
                    // RETAILMSG(1,(L"RxArtworkData NG1\r\n"));//RETAIL_MSG
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_ResetDBSelection_NoFresh:
            {
                Log.e(TAG,
                    "ITASK_ResetDBSelection_NoFresh------------------------------------->rxCode"
                        + rxCode);
                if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_ResetDBSelection)
                    {

                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                        break;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_ResetDBSelection)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                }
            }
                break;
            case ITASK_ResetDBSelection:
            {
                if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_ResetDBSelection)
                    {
                        // 如果不需要刷新列表直接返回
                        if (!mbIsNeedRefreshList)
                        {
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            break;
                        }

                        if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Audio)
                        {
                            mTreeManager.DisplayMediaType(true);
                            if (mbSyncPath)
                                PathBack_Changing(false);
                            else
                                RefreshRootList();
                        }
                        else if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video
                            || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
                        {
                            // treeManager.DisplayMediaType(false);
                        }
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_ResetDBSelection)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                    }
                }
            }
                break;
            case ITASK_SelectDBRecord:
            {
                if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_SelectDBRecord)
                    {
                        // 如果不需要刷新列表直接返回
                        if (!mbIsNeedRefreshList)
                        {
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            break;
                        }

                        // UpdateTreeFolderLevel();
                        // 获取列表父目录
                        int nCategoryCodeID = mTreeManager.GetParentType();
                        if (!mbInit_GetTrackNum
                            && nCategoryCodeID == IPOD_CATEGORY_TYPES.CATEGORY_AUDIOBOOK)
                        {
                            // 是否已经从列表选择过歌曲播放
                            if (mbSelListTrackToPlay)
                            {
                                // 记录播放状态，已经浏览过目录
                                mTreeManager.UpdatePlayFolder();
                            }
                            // 请求当前曲目数和总曲目数，Bosch项目不要求此功能
                            mTempTask.add(IPOD_MAIN_TASK.ITASK_GetCurrentPlayingTrackIndex);
                        }
                        if (!mbInit_GetTrackNum
                            && nCategoryCodeID != IPOD_CATEGORY_TYPES.CATEGORY_AUDIOBOOK)
                        {
                            // 列表父目录不是AudioBook
                            mTxPacketPar.bDbCateType =
                                (byte)mTreeManager.GetCurrentCategory();
                            mTempTask.add(IPOD_MAIN_TASK.ITASK_GetNumberCategorizedDBRecords);
                        }
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_SelectDBRecord)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                }
            }
                break;
            case ITASK_GetNumberCategorizedDBRecords:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnNumberCategorizedDBRecords)
                {
                    long nCount = rxParam.iRecordCount;
                    mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_NUM_CATEGORIZED_DBRECORD,
                        0,
                        nCount);
                    Log.e(TAG,
                        "ITASK_GetNumberCategorizedDBRecords ------------------nCount"
                            + nCount);
                    // RETAILMSG(1,(L"ITASK_GetNumberCategorizedDBRecords nCount=%ld\r\n",nCount));
                    long MAXTRACK = 0xffffff00L;
                    if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video
                        || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
                    {
                        // RETAILMSG(1,(L"ITASK_GetNumberCategorizedDBRecords nCount0=%ld,m_bInit_GetTrackNum=%ld\r\n",nCount,m_bInit_GetTrackNum));
                        if (mbInit_GetTrackNum == true)
                        {
                            mbInit_GetTrackNum = false;
                            // 更新Video下的Rentals列表状态
                            if (nCount < 5)
                            {
                                // 没有Rentals列表
                                // 是否有Rentals列表
                                // m_bIsHaveRentalsList=FALSE;
                                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_VIDEO_RENTALS_STATE,
                                    0,
                                    null);
                            }
                            else
                            {
                                // 是否有Rentals列表
                                // m_bIsHaveRentalsList=TRUE;
                                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_VIDEO_RENTALS_STATE,
                                    1,
                                    null);
                            }
                        }
                    }
                    else if (mbInit_GetTrackNum)
                    {
                        if (nCount < 1 || nCount >= MAXTRACK)
                        {
                            // 清空当前所有任务
                            // 清除所有任务
                            // 清除任务
                            mTask.clear();
                            // 清除临时任务队列
                            mTempTask.clear();
                            // 清除UI临时任务队列
                            mUITempTask.clear();
                            // 初始化UI任务
                            miPodUIEvent.clear();
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            // 通知UI没有音频文件
                            // UpdateUIInfor(IPOD_DSC_NoMediaFiles,0);
                            // 发送播放状态给UI,解除ClarionUI的死锁
                            mDataLayer.SetPlayState(IPOD_PLAY_STATE.IPOD_PST_STOP);
                            // UpdateUIInfor(IPOD_DSC_Play_Status,IPOD_PST_STOP);
                        }
                        mbInit_GetTrackNum = false;
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                        break;
                    }

                    // 如果不需要刷新列表直接返回
                    if (!mbIsNeedRefreshList)
                    {
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                        break;
                    }

                    if (nCount < MAXTRACK) // 下面目录没有数据 , 有效数据
                    {
                        UpdateFolderNumber(nCount);
                    }
                    else if (nCount >= MAXTRACK && nCount <= 0xffffffffL) // 下面目录没有数据/有效数据
                    {
                        UpdateFolderNumber(0);
                    }
                    else if (mTreeManager.GetCurrentCategory() == IPOD_CATEGORY_TYPES.CATEGORY_PODCAST) // 可能是为
                                                                                                        // -1
                    {
                        mTreeManager.SetCurrentCategory(IPOD_CATEGORY_TYPES.CATEGORY_TRACK);
                        mTxPacketPar.bDbCateType =
                            (byte)mTreeManager.GetCurrentCategory();
                        mTxPacketPar.iDbRecordIndex = 0;
                        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_SelectDBRecord);
                    }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_RetrieveCategorizedDBRecords:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnCategorizeDBRecord)
                {
                    // 设置本次接收数据任务是否结束
                    boolean bIsThisReceEnd = false;
                    TraceManager.LogE(TAG, "DBRecords->Index:"
                        + rxParam.iRecordCategoryIndex + "start:"
                        + mTxPacketPar.iDbRecordStartIndex + "count:"
                        + mTxPacketPar.iDbRecordReadCount);
                    mRecordReadCount--;
                    // 判断本次接收数据任务是否结束
                    if (mRecordReadCount != 0)
                    {
                        mTaskTimer = TIME_OUT_TIMER;
                    }
                    else
                    {
                        bIsThisReceEnd = true;
                    }
                    // 只判断最后一个索引值不能确定列表数据刷完成
                    // if(rxParam.iRecordCategoryIndex <
                    // (mTxPacketPar.iDbRecordStartIndex
                    // + mTxPacketPar.iDbRecordReadCount-1))
                    // {
                    // //后面的数据返回的定时器
                    // //重置任务处理时间(相对定时器)
                    // mTaskTimer = TIME_OUT_TIMER;
                    //
                    // }
                    // else
                    // {
                    // //本次接收任务结束
                    // bIsThisReceEnd = true;
                    // }

                    // 接收一条数据
                    ReceivedOneTrack(rxParam);
                    // 判断本次接收数据任务是否结束

                    if (bIsThisReceEnd)
                    {
                        // 设置本次接收数据任务结束
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                        Log.e(TAG,
                            "ITASK_RetrieveCategorizedDBRecords%%%%%%%%%%%%%%%");
                        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST,
                            0,
                            mTreeManager.GetCurListSongData());
                    }
                }
            }
                break;
            case ITASK_GetPlayStatus:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnPlayStatus)
                {
                    // 发送当前歌曲播放时间长度及总长度
                    // 初始化iPod播放记忆,只有在iPod初始化的时候才用到

                    UpdatePlayStatus(rxParam.iTrackLen,
                        rxParam.iTrackPos,
                        rxParam.bPlayerState);

                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetCurrentPlayingTrackIndex:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnCurrentPlayingTrackIndex)
                {
                    // 更新当前曲目序号
                    // RETAILMSG(1,(L"ITASK_GetCurrentPlayingTrackIndex=%ld\r\n",rxParam.iTrackIndex));
                    TraceManager.LogE(TAG,
                        "ITASK_GetCurrentPlayingTrackIndex rxParam.iTrackIndex"
                            + rxParam.iTrackIndex);
                    if (rxParam.iTrackIndex != 0xFFFFFFFFL)
                    {
                        // have track currently playing or pause
                        UpdateCurrentPlayIndex(rxParam.iTrackIndex, false);
                    }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetIndexedPlayingTrackTitle:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnIndexPlayingTrackTitle)
                {
                    mDataLayer.SetTrackTitle(rxParam.wsTrackTitle);
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetIndexedPlayingTrackArtistName:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnIndexedPlayingTrackArtistName)
                {
                    mDataLayer.SetTrackArtist(rxParam.wsArtistName);
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetIndexedPlayingTrackAlbumName:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnIndexedPlayingTrackAlbumName)
                {
                    mDataLayer.SetTrackAlbum(rxParam.wsAlbumName);
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_SetPlayStatusChangeNotification:
            {
                if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_SetPlayStatusChangeNotification)
                    {
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_SetPlayStatusChangeNotification)
                    {
                        // 等待时间
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // 重置任务处理时间(相对定时器)
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                    else
                    {
                        mbUseFourByteFormNotify = false;
                        // one-byte form
                        mTxPacketPar.bPlayStateOneByte = 1;
                        mTxPacketPar.bEnableNotify = 0x01;
                        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_SetPlayStatusChangeNotification);
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                }
            }
                break;
            case ITASK_PlayCurrentSelection:
            {
                if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_PlayCurrentSelection)
                    {
                        // 是否在处理视频默认播放任务
                        // m_bIsVideoPlayDefultTrackTask=FALSE;
                        // 是否已经从列表选择过歌曲播放
                        if (mbSelListTrackToPlay)
                        {
                            // 记录播放状态，已经浏览过目录
                            mTreeManager.UpdatePlayFolder();
                        }

                        // 请求当前曲目数和总曲目数，Bosch项目不要求此功能
                        // 师兄修改artwork刷两次时加的，2012-10-15我在修改bug796艺术家专辑选择歌曲播放后歌曲总数显示不正确。
                        // if(mDataLayer.GetMediaType() !=
                        // SP_MEDIA_TYPE_ENUM.SP_MEDIA_Audio)
                        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetCurrentPlayingTrackIndex);
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_ERR_CMD_FAILED
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_PlayCurrentSelection)
                    {
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_PlayCurrentSelection)
                    {
                        // 等待时间
                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                }
            }
                break;
            case ITASK_PlayControl:
            {
                if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_PlayControl)
                    {
                        // 播放状态
                        if (mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE
                            || mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY
                            || mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PAUSE)
                        {
                            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            break;
                        }
                        else if (mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_FF)
                            mDataLayer.SetPlayState(IPOD_PLAY_STATE.IPOD_PST_FF_START);
                        else if (mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_REW)
                            mDataLayer.SetPlayState(IPOD_PLAY_STATE.IPOD_PST_REW_START);
                        else if (mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW)
                        {
                            mDataLayer.SetPlayState(IPOD_PLAY_STATE.IPOD_PST_FFREW_END);
                            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
                        }
                        else if (mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_NEXT_TRACK
                            || mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PREV_TRACK
                            || mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_NEXT_CHAPTER
                            || mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PREV_CHAPTER)
                        {
                            // add xuxiuchen show title
                            if (mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_NEXT_TRACK)
                            {
                                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAYING_NEXT,
                                    0,
                                    null);
                            }
                            if (mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PREV_TRACK)
                            {
                                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAYING_PRE,
                                    0,
                                    null);
                            }
                            if (mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPOD_5G
                                && (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO))
                            {
                                // 5G视频暂停状态下Next/Play会自行处理,不需要再进行播放处理
                                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
                                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                                break;
                            }
                            else if (mDataLayer.GetPlayState() != IPOD_PLAY_STATE.IPOD_PST_PLAY)
                            {
                                // TODO add by lzx, delete audio play
                                Log.d(TAG,
                                    "come here ************************************************");
                                mTxPacketPar.bPlayControlCmdCode =
                                    IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
                                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                                break;
                            }
                        }
                        else
                        {
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            break;
                        }
                        mPlayControlType =
                            IPOD_PLAY_CONTROL_CMD_CODES.PARAM_Unknown;
                        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_STATE,
                            mDataLayer.GetPlayState(),
                            null);
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_PlayControl)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                }
            }
                break;
            case ITASK_GetTrackArtworkTimes:
            {
                if (rxCode == RX_PACKET_CODE.R_RetTrackArtworkTimes)
                {
                    mTxPacketPar.iTrackIndex = mDataLayer.GetCurTrackIndex();
                    mTxPacketPar.uFormatID = mCurrentFormatID;
                    mTxPacketPar.uTimeOffset = rxParam.uTimeOffset;
                    // RETAILMSG(1,(L"GetArt=%ld,%ld,%ldms\r\n",
                    // m_Protocol.m_TxPacketPar.iTrackIndex,m_Protocol.m_TxPacketPar.uFormatID,
                    // m_Protocol.m_TxPacketPar.uTimeOffset));
                    mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetTrackArtworkData);
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetShuffle:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnShuffle)
                {

                    mDataLayer.SetShuffleState(rxParam.bShuffleMode);
                    mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_SHUFFLE_STATE,
                        mDataLayer.GetShuffleState(),
                        null);
                    if (mDataLayer.GetShuffleState() != IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_OFF)
                    {
                        // Shuffle On时更新列表
                        // if (treeManager.IsPlayingFromList()==true)
                        // {
                        // treeManager.UpdatePlayingTrack(INVALID_LIST_ID_SUB_ONE);
                        // //RETAILMSG(1,(L"IPOD_DSC_Track_Selection0=%ld\r\n",INVALID_LIST_ID_SUB_ONE));
                        // if (treeManager.IsPlayTrackInlist()==true)
                        // {
                        // //RETAILMSG(1,(L"IPOD_DSC_Track_Selection10=%ld\r\n",m_iPodInfo->CurrentTrackIndex));
                        // UpdateUIInfor(IPOD_DSC_Track_Selection,INVALID_LIST_ID_SUB_ONE);
                        // }
                        // }
                    }
                    else
                    {
                        // Shuffle Off时当前列表的选中项
                        // if (treeManager.IsPlayingFromList()==true)
                        // {
                        // treeManager.UpdatePlayingTrack(m_iPodInfo->CurrentTrackIndex);
                        // if (treeManager.IsPlayTrackInlist()==true)
                        // {
                        // //RETAILMSG(1,(L"IPOD_DSC_Track_Selection11=%ld\r\n",m_iPodInfo->CurrentTrackIndex));
                        // UpdateUIInfor(IPOD_DSC_Track_Selection,m_iPodInfo->CurrentTrackIndex);
                        // }
                        // }
                    }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_SetShuffle:
            {
                if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_SetShuffle)
                    {
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_SetShuffle)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                }
            }
                break;
            case ITASK_GetRepeat:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnRepeat)
                {
                    mDataLayer.SetRepeatState(rxParam.bRepeatState);
                    mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_REPEAT_STATE,
                        mDataLayer.GetRepeatState(),
                        null);
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_SetRepeat:
            {
                if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_SetRepeat)
                    {
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_SetRepeat)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                        // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                    }
                }
            }
                break;
            case ITASK_SetDisplayImage:
            {
            }
                break;
            case ITASK_GetMonoDisplayImageLimits:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnMonoDisplayImageLimits)
                {
                    // m_LogoFormats.clear();
                    // m_LogoFormats.swap(rxParam.vLogoFormats);
                    // m_LogoImg.maxWidth=m_LogoFormats[0].maxWidth;
                    // m_LogoImg.maxHeight=m_LogoFormats[0].maxHeight;
                    // //RETAILMSG(1,(L"SetDisplayImage(maxW=%ld,maxH=%ld)\r\n",
                    // // m_LogoImg.maxWidth,m_LogoImg.maxHeight));
                    // //初始化变量
                    // //Descriptor telegram index
                    // m_LogoImg.telegram_index=0x0000;
                    // //发送图片数据
                    // ZeroMemory(m_Protocol.m_TxPacketPar.pbImgData,
                    // sizeof(m_Protocol.m_TxPacketPar.pbImgData));
                    // //设置图像数据偏移指针
                    // m_LogoImg.pImage=m_LogoImg.image_data;
                    // if (SetDisplayImage()==FALSE)
                    // {
                    //
                    // }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_GetNumPlayingTracks:// ID35，ID36用于请求歌曲当前Playback
                                           // engine中的总曲目
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnNumPlayingTracks)
                {
                    // 计算播放引擎中的歌曲总数
                    mDataLayer.SetTotalTrack(rxParam.iTrackPlayingNum);
                    // 通知歌曲总数目
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    SP_IPOD_TRACK_INDEX_TIME trackIndex =
                        new SP_IPOD_TRACK_INDEX_TIME();
                    trackIndex.uIndex = mDataLayer.GetCurTrackIndex();
                    trackIndex.uTotalIndex = mDataLayer.GetTotalTrack();
                    mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_INDEX,
                        0,
                        trackIndex);
                }
            }
                break;
            case ITASK_SetCurrentPlayingTrack:
            {
                if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
                {
                    if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_SetCurrentPlayingTracks)
                    {
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                    else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING
                        && rxParam.bTxAckId == TX_PACKET_CODE.T_SetCurrentPlayingTracks)
                    {
                        // 等待时间

                        // 重置任务处理时间(相对定时器)
                        mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                    }
                }
            }
                break;
            case ITASK_GetColorDisplayImageLimits:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnColorDisplayImageLimits)
                {
                    // m_LogoFormats.clear();
                    // m_LogoFormats.swap(rxParam.vLogoFormats);
                    //
                    // boolean bRes = false;
                    // for (int i=0;i<(int)m_LogoFormats.size();i++)
                    // {
                    // if
                    // (m_LogoFormats[i].dispPixelFormat==m_LogoImg.pixelFormat)
                    // {
                    // m_LogoImg.maxWidth=m_LogoFormats[i].maxWidth;
                    // m_LogoImg.maxHeight=m_LogoFormats[i].maxHeight;
                    // bRes=TRUE;
                    // }
                    // }
                    // if (bRes==TRUE)
                    // {
                    // /*RETAILMSG(OUT_MSG,(L"SetDisplayImage(maxW=%ld,maxH=%ld)\r\n",
                    // m_LogoImg.maxWidth,m_LogoImg.maxHeight));*/
                    // //初始化变量
                    // //Descriptor telegram index
                    // m_LogoImg.telegram_index=0x0000;
                    // //发送图片数据
                    // ZeroMemory(m_Protocol.m_TxPacketPar.pbImgData,
                    // sizeof(m_Protocol.m_TxPacketPar.pbImgData));
                    // //设置图像数据偏移指针
                    // m_LogoImg.pImage=m_LogoImg.image_data;
                    // if (SetDisplayImage()==FALSE)
                    // {
                    //
                    // }
                    // }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_ResetDBSelectionHierarchy:
            {
                if (rxCode == RX_PACKET_CODE.R_ExtendInterfaceLingoACK)
                {
                    if (rxParam.bTxAckId == TX_PACKET_CODE.T_ResetDBSelectionHierarchy)
                    {
                        if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_SUCCESS)
                        {
                            ResetVideoDB();
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                        }
                        else if (rxParam.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING)
                        {
                            // 等待时间

                            // 重置任务处理时间(相对定时器)
                            mTaskTimer = rxParam.uMaxWaitPendingRepTime / 50;
                            // RETAILMSG(1,(L"Wait Time=%ld.TaskTime=%ld\r\n",rxParam.uMaxWaitPendingRepTime,m_uTaskTimer));
                        }
                        else
                        {
                            // 通知UI，不支持视频浏览
                            // RETAILMSG(OUT_MSG,(L"iPod No Video\r\n"));
                            // UpdateUIInfor(IPOD_DSC_Error_Status,IPOD_ERR_NOT_SUPPORT_VIDEO);
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                        }
                    }
                }
            }
                break;
            case ITASK_UITask_ReturnCategoryList:
            {
                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
            }
                break;
            // 初始化处理相关任务Start******************************
            case ITASK_InitTask_ReqCurPlayIndex:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnCurrentPlayingTrackIndex)
                {
                    // 更新当前曲目序号
                    iPodLogDefines.iPodLog(TAG,
                        "ITASK_InitTask_ReqCurPlayIndex->Index:"
                            + rxParam.iTrackIndex);
                    if (rxParam.iTrackIndex != 0xffffffffL)
                    {
                        // 需要更新的曲目索引
                        mDataLayer.SetCurTrackIndex(rxParam.iTrackIndex);
                        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_InitTask_ReqCurPlayType);
                    }
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                }
            }
                break;
            case ITASK_InitTask_ReqCurPlayType:
            {
                if (rxCode == RX_PACKET_CODE.R_ReturnIndexedPlayingTrackInfo)
                {
                    if (rxParam.bTrackInfoType == TRACK_INFO_TYPES.TRACK_CAPABILITIES_AND_INFOMATION) // 第6为0，得到Track
                                                                                                      // Capabilities
                                                                                                      // and
                                                                                                      // Information
                    {
                        // RETAILMSG(1,(L"ITASK_InitTask_ReqCurPlayType=0x%x\r\n",rxParam.iTrackCapaBits));
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_IS_AUDIOBOOK) != 0)
                        {

                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_HAS_CHAPTERS) != 0)
                        {

                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_CONTAINS_VIDEO) != 0)
                        {

                        }
                        if ((rxParam.iTrackCapaBits & TRACK_CAPABLITIES_BITS.TRACK_IS_PLAY_AS_A_VIDEO) != 0) // has
                                                                                                             // chapter
                                                                                                             // //POSCAST
                                                                                                             // 既可以当作音乐播，也可以当作视频播
                        {
                            if (mDataLayer.GetIsPlayingVideo() == false)
                            {
                                mDataLayer.SetIsPlayingVideo(true);
                            }
                        }
                        else
                        {
                            if (mDataLayer.GetIsPlayingVideo() == true)
                            {
                                mDataLayer.SetIsPlayingVideo(false);
                            }
                        }
                        mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    }
                }
            }
                break;
            case ITASK_InitTask_VideoPlayDefultTrack:
            {
                mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
            }
                break;
            // 初始化处理相关任务End******************************
            default:
                break;
        }
        // 将临时任务队列中任务移到到任务队列头部
        synchronized (mSycTask)
        {
            mTask.addAll(0, mTempTask);
        }

        // 处理下一条任务
        if (mCuriPodTask == IPOD_MAIN_TASK.ITASK_idle)// 处理任务
        {
            // 处理任务
            iPodTaskManager(false);
        }
        // RETAILMSG(1,(L"Recv Temp Task\r\n"));
    }

    private boolean RxNewIpodTrackInfo(int rxCode, RX_PACKET_PARAM rxParam)
    {
        // TODO Auto-generated method stub
        mTxPacketPar.bCmdStatus = ACK_COMMAND_ERROR_CODES.ACK_SUCCESS;
        mTxPacketPar.bAckId = 0x04;
        return true;
    }

    private boolean RxPlayStatusChNotification(int rxCode,
        RX_PACKET_PARAM rxParam)
    {
        // 清除临时任务队列
        mTempTask.clear();
        /*下面所有新加的任务先全放入临时任务队列，
        最后再从临时任务队列中移入任务队列中*/
        // 该命令由iPod直接返回
        // iPodLogDefines.iPodLog(TAG, "rxParam.bNewPlayStatus = "
        // + rxParam.bNewPlayStatus);
        switch (rxParam.bNewPlayStatus)
        {
            case PLAY_STATUS_CHANGE_NOTIFICATION.PLAYBACK_STOPPED:// 播放已停止
            {
                // m_piPodCtlInfo->bIsMemoryPlay = TRUE;
                mDataLayer.SetPlayState(IPOD_PLAY_STATE.IPOD_PST_STOP);
                mDataLayer.SetTotalPlayTime(0);
                mDataLayer.SetCurPlayTime(0);
                mPlayControlType = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_Unknown;
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_STATE,
                    mDataLayer.GetPlayState(),
                    null);
                // if (m_iPodInfo->ScanState == IPOD_SCAN_ON)
                // {
                // m_iPodInfo->ScanState = IPOD_SCAN_OFF;
                // chTimerMode =3;
                // }
                SP_IPOD_TRACK_INDEX_TIME trackTime =
                    new SP_IPOD_TRACK_INDEX_TIME();
                trackTime.uIndex = mDataLayer.GetCurPlayTime();
                trackTime.uTotalIndex = mDataLayer.GetTotalPlayTime();

                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_TIME,
                    0,
                    trackTime);

            }
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.TRACK_INDEX_:// 换歌播放
            {
                // 计算当前歌曲的索引值
                long iNewIndex = 0;
                iNewIndex =
                    TypeConvert.ToUnsignedInt(TypeConvert.ByteToInt(rxParam.pbParams[3],
                        rxParam.pbParams[2],
                        rxParam.pbParams[1],
                        rxParam.pbParams[0]));
                UpdateCurrentPlayIndex(iNewIndex, true);
                // Scan计时
                // m_iScanStartTime = 0;
            }
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.PLAYBACK_FFW_SEEK_STOP:// 快进结束
            {
                // 在快退的过程中收到快进结束，则不再请求状态
                if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START)
                    break;
                // UpdateUIInfor(IPOD_DSC_EndRev,0);
                // UpdateUIInfor(IPOD_DSC_Play_Status,IPOD_PST_FFREW_END);
                if ((mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPOD_CLASSIC || mDataLayer.GetiPodModelID() == IPOD_MODEL_IDS.IPOD_CLASSIC_120GB)
                    && (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
                    && mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PAUSE)
                {
                    // 如果是视频下暂停再快进到下一曲，iPod Classic已经播放，但它回复的状态不对。
                    switch (mIpodPlayingStateBoolean)
                    {
                        case 0:
                            mTxPacketPar.bPlayControlCmdCode =
                                IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY;
                            mTempTask.add(IPOD_MAIN_TASK.ITASK_PlayControl);
                            break;
                        case 1:
                            break;
                        case 2:
                            mTxPacketPar.bPlayControlCmdCode =
                                IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PAUSE;
                            mTempTask.add(IPOD_MAIN_TASK.ITASK_PlayControl);
                            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                            break;
                        default:
                            break;
                    }

                    mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    // mTxPacketPar.bPlayControlCmdCode =
                    // IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY;
                    // mTempTask.add(IPOD_MAIN_TASK.ITASK_PlayControl);
                }
                else
                {
                    mTempTask.add(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
                }
            }
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.PLAYBACK_REW_SEEK_STOP:// 快退结束
            {
                // 在快进的过程中收到快退结束，则不再请求状态
                if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START)
                    break;
                // UpdateUIInfor(IPOD_DSC_EndRev,1);
                // UpdateUIInfor(IPOD_DSC_Play_Status,IPOD_PST_FFREW_END);
                mTempTask.add(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
            }
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.TRACK_TIME_OFFSET_MS_:// 播放时间改变
            {
                // 计算当前歌曲的播放时间
                long iNewPlayTime = 0;
                iNewPlayTime =
                    TypeConvert.ToUnsignedInt(TypeConvert.ByteToInt(rxParam.pbParams[3],
                        rxParam.pbParams[2],
                        rxParam.pbParams[1],
                        rxParam.pbParams[0]));
                mDataLayer.SetCurPlayTime(iNewPlayTime / 1000);
                SP_IPOD_TRACK_INDEX_TIME trackTime =
                    new SP_IPOD_TRACK_INDEX_TIME();
                trackTime.uIndex = mDataLayer.GetCurPlayTime();
                trackTime.uTotalIndex = mDataLayer.GetTotalPlayTime();
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_TIME,
                    0,
                    trackTime);

                // 对策bug Start:播放歌曲时，信息显示不对，当快退时，不能立刻显示Play or Pause
                if (iNewPlayTime < 800)// 800ms
                {
                    if ((mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START || mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START))
                    {
                        mTempTask.add(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
                    }
                }
                // 对策bug End:播放歌曲时，信息显示不对，当快退时，不能立刻显示Play or Pause
            }
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.CHAPTER_INDEX_:
            {
                if ((mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
                    && (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START || mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START))
                {
                    mTxPacketPar.bPlayControlCmdCode =
                        IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW;
                    mTempTask.add(IPOD_MAIN_TASK.ITASK_PlayControl);
                }
            }
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.PLAYBACK_STATUS_EXTERNED:
                // TODO add by lzx 当状态更改的时候，将当前的播放状态返回
                byte playState = rxParam.pbParams[0];
                iPodLogDefines.iPodLog(TAG, "playState = " + playState);
                if (playState == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY)
                {
                    mDataLayer.SetPlayState(IPOD_PLAY_STATE.IPOD_PST_PLAY);
                }
                else if (playState == IPOD_PLAY_STATE.IPOD_PST_PAUSE)
                {
                    mDataLayer.SetPlayState(IPOD_PLAY_STATE.IPOD_PST_PAUSE);
                }
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_STATE,
                    mDataLayer.GetPlayState(),
                    null);
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.TRACK_TIME_OFFSET_SEC_:
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.CHAPTER_TIME_OFFSET_MS_:
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.CHAPTER_TIME_OFFSET_SEC_:
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.TRACK_UNIQUE_IDENTIFIER_:
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.TRACK_PLAYBACK_MODE:
            {
                // 计算当前歌曲的模式
                byte bNewPlayMode = 0;
                bNewPlayMode = rxParam.pbParams[0];
                switch (bNewPlayMode)
                {
                    case 0x00:// Audio track
                    {
                        // RETAILMSG(1,(L"Cur Audio track\r\n"));
                    }
                        break;
                    case 0x01:// Video track
                    {
                        // RETAILMSG(1,(L"Cur Video track\r\n"));
                    }
                        break;
                    default:
                        break;
                }
            }
                break;
            case PLAY_STATUS_CHANGE_NOTIFICATION.TRACK_LYRICS_READY_:
                break;
            default:
                break;
        }
        // 将临时任务队列中任务移到到任务队列尾部
        synchronized (mSycTask)
        {
            mTask.addAll(mTask.size(), mTempTask);
        }
        // RETAILMSG(1,(L"Notification Temp Task\r\n"));
        return true;
    }

    private int mIpodPlayingStateBoolean = 0; // 0是播放 1是暂停 2 是快进结束

    private void UpdateFirstInfor()
    {
        if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Image)
        {
            // 不进行任何处理
            return;
        }
        // 设置iPod的状态改变通知
        mbUseFourByteFormNotify = true;
        mTxPacketPar.bPlayStateOneByte = 0;
        mTxPacketPar.iNotifyEventMask =
            IPOD_PLAY_STATE_MASK_BIT.BASIC_PLAY_STATE_CHANGES
                | IPOD_PLAY_STATE_MASK_BIT.TRACK_CAPABILITIES_CHANGED
                | IPOD_PLAY_STATE_MASK_BIT.TRACK_INDEX;

        mTempTask.add(IPOD_MAIN_TASK.ITASK_SetPlayStatusChangeNotification);
        // 设置iPod的状态改变通知
        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetArtworkFormats);
        // PackageData_Lingo00(IPOD_REQ_RohmVersion);//请求软件版本
        if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Audio)
        { // 在请求iPod名称前请求歌曲总数目
          // 请求当前播放曲目索引，成功请求曲目类型
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_InitTask_ReqCurPlayIndex);

            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_ResetDBSelection);
            mbInit_GetTrackNum = true;
            mTxPacketPar.bDbCateType = IPOD_CATEGORY_TYPES.CATEGORY_TRACK;
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumberCategorizedDBRecords);
            // 获取repeat和shuffle状态
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetShuffle);
            // 当音视频切换的话，是否需要将Repeat状态重设为off
            // if (m_bIsSetRepeatOff == TRUE)
            // {
            // mTxPacketPar.bNewRepeatStatus = IPOD_REPEAT_OFF;
            // mTempTask.addLast(IPOD_MAIN_TASK.ITASK_SetRepeat);
            // }
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetRepeat);
            // RETAILMSG(1,(L"ITASK_GetPlayStatus3\r\n"));
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
        }
        else if ((mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)) // Video
        {
            // IPOD_OperateHandle(EVENT_IPOD_PLAY_VIDEO);
            // piPodCtlInfo->chCurrentCategoryCodeID = CATEGORY_NOTHING;
            // treeManager.ReturnRootLevel();
            mbInit_GetTrackNum = true;
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_InitTask_ReqCurPlayIndex);
            mDataLayer.SetIsSupportVideo(true);
            if (mDataLayer.GetIsSupportVideo() == true)
            {
                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_ResetDBSelection);
                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_ResetDBSelectionHierarchy);
                // 取消shuffle状态
                mTxPacketPar.bNewShuffleMode =
                    IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_OFF;
                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_SetShuffle);
            }
            mDataLayer.SetPlayState(IPOD_PLAY_STATE.IPOD_PST_STOP);
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetShuffle);
            // 当音视频切换的话，是否需要将Repeat状态重设为off
            // if (m_bIsSetRepeatOff == true)
            // {
            // mTxPacketPar.bNewRepeatStatus = IPOD_REPEAT_OFF;
            // mTempTask.addLast(IPOD_MAIN_TASK.ITASK_SetRepeat);
            // }

            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetRepeat);
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
            // RETAILMSG(1,(L"ITASK_GetPlayStatus2\r\n"));
        }
    }

    private int GetArtWorkFormatPos(int nID)
    {
        int nPos = -1;
        for (int i = 0; i < mArtFormats.size(); i++)
        {
            if (mArtFormats.get(i).formatID == nID)
            {
                nPos = i;
                break;
            }
        }
        return nPos;
    }

    // 接收到Artwork数据
    private boolean ReceiveArtworkData(RX_PACKET_PARAM rxParam)
    {
        int index = 0;
        index = rxParam.uDesrTelegramIndex;
        // iPodLogDefines.iPodLog(TAG, "ReceiveArtworkDataIndex:"+index);
        if (index == 0)
        {
            mstrArtwork.telegram_index = 0;// 第一个数据包
        }
        else
        {
            if (index != (mstrArtwork.telegram_index + 1))
            {
                // 数据包有丢失
                // RETAILMSG(1,(L"ReceiveArtworkData0=%ld,%ld\r\n",index,m_strArtwork.telegram_index));
                if (index > (mstrArtwork.telegram_index + 1))
                {
                    mstrArtwork.bIsOmitData = true;// 是否丢失数据包
                    // 加入的偏移
                    int uTmpAddLen = 0;
                    uTmpAddLen =
                        (rxParam.uImgDataLen * (index
                            - mstrArtwork.telegram_index - 1));
                    if (mstrArtwork.left_len >= (uTmpAddLen + rxParam.uImgDataLen))
                    {
                        // 设置图像剩下未传的字节数
                        mstrArtwork.left_len -= uTmpAddLen;
                        // 图片偏移
                        mstrArtwork.pImage += uTmpAddLen;

                        // 拷贝图像数据
                        TypeConvert.memcpy(mstrArtwork.image_data,
                            rxParam.pbImgData,
                            mstrArtwork.pImage,
                            0,
                            rxParam.uImgDataLen);
                        // 图像这次刚传的字节数
                        mstrArtwork.this_write_len = rxParam.uImgDataLen;
                        // 设置图像剩下未传的字节数
                        mstrArtwork.left_len -= mstrArtwork.this_write_len;
                        // 图片偏移
                        mstrArtwork.pImage += mstrArtwork.this_write_len;

                        mstrArtwork.telegram_index = index;
                        return true;
                    }
                    else
                    {
                        // RETAILMSG(1,(L"ReceiveArtworkData02=%ld,%ld\r\n",index,m_strArtwork.telegram_index));
                        return false;
                    }
                }
                else
                {
                    // RETAILMSG(1,(L"ReceiveArtworkData01=%ld,%ld\r\n",index,m_strArtwork.telegram_index));
                    return false;
                }
            }
            else
            {
                mstrArtwork.telegram_index = index;
            }
        }
        if (mstrArtwork.telegram_index == 0) // 第一个数据包
        {
            // ArtWork图片
            // 图像数据偏移指针
            if (mstrArtwork.pImage != 0)
            {
                mstrArtwork.pImage = 0;
            }
            // 获得Format
            for (int i = 0; i < mArtFormats.size(); i++)
            {
                if (mArtFormats.get(i).formatID == mCurrentFormatID)
                {
                    // pixelformat
                    mstrArtwork.pixelFormat = mArtFormats.get(i).pixelFormat;
                    mstrArtwork.widthMax = mArtFormats.get(i).width;
                    mstrArtwork.heightMax = mArtFormats.get(i).height;
                    break;
                }
            }
            // 获得ArtWork大小
            mstrArtwork.width = rxParam.uImgWidth;
            mstrArtwork.height = rxParam.uImgHeight;
            if (mstrArtwork.width > mstrArtwork.widthMax
                || mstrArtwork.height > mstrArtwork.heightMax)
            {
                return false;
            }

            // 获得图像每行的大小
            mstrArtwork.row_byte = rxParam.uRowSize;
            // 计算图像总的字节数
            mstrArtwork.total_byte =
                (int)(mstrArtwork.height * mstrArtwork.row_byte);
            iPodLogDefines.iPodLog(TAG, "mstrArtwork.total_byte:"
                + mstrArtwork.total_byte);
            if (mstrArtwork.total_byte <= 0)
            {
                return false;
            }
            // 创建数据buf
            if (mstrArtwork.image_data == null
                || mstrArtwork.image_data.length != mstrArtwork.total_byte)
            {
                mstrArtwork.image_data = new byte[mstrArtwork.total_byte];
            }
            if (mstrArtwork.image_data == null)
            {
                // RETAILMSG(1,(L"ReceiveArtworkData3=0x%x\r\n",
                // m_strArtwork.image_data));
                return false;
            }
            // 图像数据偏移指针
            mstrArtwork.pImage = 0;
            // 拷贝图像数据
            TypeConvert.memcpy(mstrArtwork.image_data,
                rxParam.pbImgData,
                mstrArtwork.pImage,
                0,
                rxParam.uImgDataLen);
            // 图像这次刚传的字节数
            mstrArtwork.this_write_len = rxParam.uImgDataLen;
            // 设置图像剩下未传的字节数
            mstrArtwork.left_len =
                mstrArtwork.total_byte - mstrArtwork.this_write_len;
            // 图片偏移
            mstrArtwork.pImage = (int)mstrArtwork.this_write_len;
            iPodLogDefines.iPodLog(TAG, "mstrArtwork.pImage:"
                + mstrArtwork.pImage);
        }
        else
        {
            // 创建数据buf
            if (mstrArtwork.image_data == null)
            {
                // RETAILMSG(1,(L"ReceiveArtworkData3=0x%x\r\n",
                // m_strArtwork.image_data));
                return false;
            }
            // 拷贝图像数据
            TypeConvert.memcpy(mstrArtwork.image_data,
                rxParam.pbImgData,
                mstrArtwork.pImage,
                0,
                rxParam.uImgDataLen);
            // 图像这次刚传的字节数
            mstrArtwork.this_write_len = rxParam.uImgDataLen;
            // 设置图像剩下未传的字节数
            mstrArtwork.left_len -= mstrArtwork.this_write_len;
            // 图片偏移
            mstrArtwork.pImage += mstrArtwork.this_write_len;
        }
        // iPodLogDefines.iPodLog(TAG,
        // "ReceiveArtworkDataleft_len:"+mstrArtwork.left_len);
        if (mstrArtwork.left_len <= 0)
        {
            if (mstrArtwork.bIsOmitData == false)// 是否丢失数据包
            {
                // 图像已全部传完
                if (CreateArtWorkBITMAP(mstrArtwork) == false)
                {
                    iPodLogDefines.iPodLog(TAG, "CreateArtWorkBITMAP->FAIL");
                    mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_ARTWORK,
                        0,
                        null);
                    return false;
                }
                iPodLogDefines.iPodLog(TAG, "CreateArtWorkBITMAP->OK");
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_ARTWORK,
                    0,
                    mDataLayer.GetArtwork());
            }
            else
            {
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_ARTWORK,
                    0,
                    null);
                iPodLogDefines.iPodLog(TAG, "Artwork 丢包");
                mstrArtwork.bIsOmitData = false;
            }

            // 图像数据
            if (mstrArtwork.image_data != null)
            {
                mstrArtwork.image_data = null;
            }
            // 图像数据偏移指针
            if (mstrArtwork.pImage != 0)
            {
                mstrArtwork.pImage = 0;
            }

            // 主任务
            mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
        }
        // iPodLogDefines.iPodLog(TAG, "ReceiveArtworkData->OK");
        return true;
    }

    private HANDLE_EVENT mTpyeEvent = null;

    public void iPOD_SetPlayingState(int type)
    {
        mIpodPlayingStateBoolean = type;
    }

    public void iPOD_OperateHandle(IPOD_UI_EVENT event)
    {
        synchronized (mSycUIEvent)
        {
            // if((SystemClock.elapsedRealtime() - iPodUIEventTimeCount) < 500){
            // return;
            // }
            iPodUIEventTimeCount = SystemClock.elapsedRealtime();
            iPodLogDefines.iPodLog(TAG, "iPOD_OperateHandle->eventType:"
                + event.eventType);
            // 当前正在切换媒体类型
            if (event.eventType == HANDLE_EVENT.EVENT_CHANGE_OSD)
            {
                mDataLayer.SetiPodOSD((SP_IPOD_OSD)event.obj);
                // ChangeOSD((SP_IPOD_OSD)event.obj);
                // mDataLayer.SetiPodOSD((SP_IPOD_OSD)event.obj);
                // Long index = mTreeManager.GetCurrentPlayingTrack();
                // InitRootList();
                // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
                // 0, mTreeManager.GetListPath());
            }
            else if (event.eventType == HANDLE_EVENT.EVENT_ABC123_SEARCH)
            {
                // 当前正处于ABC123Search中，不处理
            }
            else if (event.eventType == HANDLE_EVENT.EVENT_IPOD_REQ_PLAYLIST
                || event.eventType == HANDLE_EVENT.EVENT_IPOD_REQ_CATEGORY_TYPE_LIST
                || event.eventType == HANDLE_EVENT.EVENT_IPOD_REQ_CHILDREN
                || event.eventType == HANDLE_EVENT.EVENT_IPOD_MENU
                || event.eventType == HANDLE_EVENT.EVENT_IPOD_BACK_TOPMENU)
            {
                // 当Shuffle On时列表操作需要先取消Shuffle
                if (mTpyeEvent == event.eventType
                    && miPodUIEvent.size() > 0
                    && event.eventType == HANDLE_EVENT.EVENT_IPOD_REQ_CATEGORY_TYPE_LIST)
                {
                    miPodUIEvent.removeAt(miPodUIEvent.size() - 1);
                }
                mTpyeEvent = event.eventType;
                miPodUIEvent.offer(event);
            }
            else if (event.eventType == HANDLE_EVENT.EVENT_IPOD_PLAY_CURRENT_SONG)
            {
                // 放入UI队列中,不立即执行
                miPodUIEvent.offer(event);
            }
            else
            {
                // 放入UI队列中,不立即执行
                miPodUIEvent.offer(event);
            }
        }
    }

    private void UpdatePlayStatus(long iTotalTime, long iCurrentTime,
        byte nState)
    {
        byte tmpState = nState;
        iPodLogDefines.iPodLog(TAG, "UpdatePlayStatus:" + tmpState);
        if (mDataLayer.GetPlayState() != tmpState)
        {
            // 如果当前处于快进/快退状态，取消快进/快退
            if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START
                || mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START)
            {
                mTxPacketPar.bPlayControlCmdCode =
                    IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW;
                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
            }
            iPodLogDefines.iPodLog(TAG, "UpdatePlayStatus Refresh:" + tmpState);
            mDataLayer.SetPlayState(tmpState);
            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_STATE,
                tmpState,
                null);
        }
        else if (mDataLayer.GetPlayState() == tmpState
            && tmpState == IPOD_PLAY_STATE.IPOD_PST_STOP)
        {
            // 第一次没有发状态给UI
            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_STATE,
                tmpState,
                null);
        }

        // 初始化iPod播放记忆,只有在iPod初始化的时候才用到
        if (mbIsiPodInit)
        {
            // 停止，则从SONG目录第一曲开始播放
            if ((mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_STOP
                || mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_NONE || mDataLayer.GetIsPlayingVideo())
                && mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Audio)
            {
                // Op_RootFolder(FALSE);
                // 是否已经从列表选择过歌曲播放
                mbSelListTrackToPlay = true;

                // 选择TRACK目录
                mPrepareSetIndex =
                    IPOD_CATEGORY_TYPES.CATEGORY_TRACK
                        - IPOD_CATEGORY_TYPES.CATEGORY_PLAYLIST;
                mTreeManager.SetCurrentCategory(IPOD_CATEGORY_TYPES.CATEGORY_TRACK);

                mTxPacketPar.bDbCateType = IPOD_CATEGORY_TYPES.CATEGORY_TRACK;
                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumberCategorizedDBRecords);

                // 播放第一曲
                mPreparePlayingSongIndex = 0;
                mTxPacketPar.iTrackIndex = mPreparePlayingSongIndex;
                // mTempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayCurrentSelection);
                // mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetCurrentPlayingTrackIndex);
            }
            else if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Audio)// 从当前曲播放
            {
                if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PAUSE)
                {
                    // TODO add by lzx
                    // mTxPacketPar.bPlayControlCmdCode =
                    // IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
                    // mTempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                }
                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumPlayingTracks);
                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetCurrentPlayingTrackIndex);
            }
            else if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video
                || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
            {
                if (!mDataLayer.GetIsSupportVideo())
                {
                    // 是否需要屏蔽从切到视频到从列表中选择视频中间的播放命令
                    // m_bIsOmitPlayCmdBeforVideoList=TRUE;
                    if (mDataLayer.GetPlayState() != IPOD_PLAY_STATE.IPOD_PST_STOP)
                    {
                        IPOD_UI_EVENT event = new IPOD_UI_EVENT();
                        event.eventType = HANDLE_EVENT.EVENT_IPOD_PLAY_CONTROL;
                        event.iLParam =
                            IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
                        event.iWParam = IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PAUSE;
                        iPOD_OperateHandle(event);
                    }
                }
                else if (mDataLayer.GetIsPlayingVideo())
                {
                    // 如果当前正在播放视频并且UI为视频模式,播放当前曲目
                    if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PAUSE)
                    {
                        mTxPacketPar.bPlayControlCmdCode =
                            IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
                        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                    }
                    mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumPlayingTracks);
                    mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetCurrentPlayingTrackIndex);
                }
                else
                {
                    // 如果当前不在播放视频但UI为视频模式,播放默认曲目
                    if (mTreeManager.IsRootLevel())
                    { // 在根目录下
                      // 进入第一个子列表
                        Op_EnterChildFolder(0, false);
                        // 播放第一曲
                        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_InitTask_VideoPlayDefultTrack);
                    }
                }
            }
            if (mbUseFourByteFormNotify == true)
            {
                mTxPacketPar.bPlayStateOneByte = 0;
                mTxPacketPar.iNotifyEventMask =
                    IPOD_PLAY_STATE_MASK_BIT.BASIC_PLAY_STATE_CHANGES
                        | IPOD_PLAY_STATE_MASK_BIT.TRACK_INDEX
                        | IPOD_PLAY_STATE_MASK_BIT.CHAPTER_INDEX
                        | IPOD_PLAY_STATE_MASK_BIT.TRACK_CAPABILITIES_CHANGED
                        | IPOD_PLAY_STATE_MASK_BIT.EXTERN_PLAY_STATE_CHANGES
                        | IPOD_PLAY_STATE_MASK_BIT.TRACK_TIME_OFFSET_MS
                        | IPOD_PLAY_STATE_MASK_BIT.TRACK_MEDIA_TYPE;

                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_SetPlayStatusChangeNotification);
            }
            // 初始化完成
            mbIsiPodInit = false;
        }
        else
        {
            if (mDataLayer.GetPlayState() != IPOD_PLAY_STATE.IPOD_PST_PLAY
                && mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY)
            {
                mTxPacketPar.bPlayControlCmdCode =
                    IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
            }
            else if (mDataLayer.GetPlayState() != IPOD_PLAY_STATE.IPOD_PST_PAUSE
                && mPlayControlType == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PAUSE)
            {
                mTxPacketPar.bPlayControlCmdCode =
                    IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
                mTempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
            }

            SP_IPOD_TRACK_INDEX_TIME trackTime = new SP_IPOD_TRACK_INDEX_TIME();

            mDataLayer.SetTotalPlayTime(iTotalTime / 1000);
            mDataLayer.SetCurPlayTime(iCurrentTime / 1000);
            trackTime.uIndex = mDataLayer.GetCurPlayTime();
            trackTime.uTotalIndex = mDataLayer.GetTotalPlayTime();
            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAY_TIME,
                0,
                trackTime);

        }
    }

    private void UpdateCurrentPlayIndex(long nPlayIndex, boolean isPathUpdate)
    {
        // 需要更新的曲目索引
        // if(mNowPlayIndex==nPlayIndex){
        // return;
        // }
        mNeedUpdateIndex = nPlayIndex;
        // mNowPlayIndex = nPlayIndex;
        mDataLayer.SetCurTrackIndex(nPlayIndex);
        mTreeManager.UpdatePlayingTrack(mNeedUpdateIndex);
        SP_IPOD_TRACK_INDEX_TIME trackIndex = new SP_IPOD_TRACK_INDEX_TIME();
        trackIndex.uIndex = mDataLayer.GetCurTrackIndex();
        trackIndex.uTotalIndex = mDataLayer.GetTotalTrack();
        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_TRACK_INDEX,
            0,
            trackIndex);
        TraceManager.LogE(TAG, "UpdateCurrentPlayIndex trackIndex.uTotalIndex"
            + trackIndex.uTotalIndex);
        TraceManager.LogE(TAG, "UpdateCurrentPlayIndex trackIndex.uIndex"
            + trackIndex.uIndex);

        if (mTreeManager.IsPlayTrackInlist())
        {
            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAYING_INDEX,
                (int)mNeedUpdateIndex,
                null);
            TraceManager.LogE(TAG, "UpdateCurrentPlayIndexmNeedUpdateIndex"
                + mNeedUpdateIndex);
        }
        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumPlayingTracks);
        if (!mbSetShuffle)
        {
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetIndexedPlayingTrackInfo);

            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetPlayStatus);
            // 如果当前曲目数>总曲目数
            // mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumPlayingTracks);
            // 请求ID3信息
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetIndexedPlayingTrackTitle);
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetIndexedPlayingTrackArtistName);
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetIndexedPlayingTrackAlbumName);
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetIndexedPlayingTrackGenre);
        }
        else
        {
            mbSetShuffle = false;
        }
        // xuxiuchen
        if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START)
        {
            mIpodPlayingStateBoolean = 2;
            return;
        }
        mIpodPlayingStateBoolean = 1;
    }

    private void ManageUIPlayControl(long iWParam, long iLParam)
    {
        switch (mDataLayer.GetMediaType())
        {
            case SP_MEDIA_Video:
            case SP_MEDIA_Audio:
            case SP_MEDIA_REAR_VIDEO:
            {
                if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_STOP
                    && iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_STOP)
                {
                    // do nothing
                }
                else if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_STOP)
                {
                    if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Audio)
                    {
                        // Shuffle Off
                        if (mDataLayer.GetShuffleState() != IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_OFF)
                        {
                            mTxPacketPar.bNewShuffleMode =
                                IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_OFF;

                            mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SetShuffle);
                            mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetShuffle);
                        }
                        // play current track,
                        mPreparePlayingSongIndex =
                            mDataLayer.GetCurTrackIndex();
                        mTxPacketPar.iTrackIndex = mPreparePlayingSongIndex;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayCurrentSelection);
                    }
                    else
                    {
                        mPreparePlayingSongIndex =
                            mDataLayer.GetCurTrackIndex();
                        mTxPacketPar.iTrackIndex = mPreparePlayingSongIndex;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayCurrentSelection);
                    }
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE
                    && iLParam != 0)
                {
                    // 指定是play还是pause
                    if (iLParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY
                        && mDataLayer.GetPlayState() != IPOD_PLAY_STATE.IPOD_PST_PLAY)
                    {
                        // play
                        mTxPacketPar.bPlayControlCmdCode =
                            IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                        mIpodPlayingStateBoolean = 0;
                    }
                    else if (iLParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PAUSE
                        && mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PLAY)
                    {
                        // pause
                        mTxPacketPar.bPlayControlCmdCode =
                            IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);

                        mIpodPlayingStateBoolean = 1;
                    }
                }
                else if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_REW_START
                    || mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_FF_START)
                {
                    if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_STOP)
                    {
                        mTxPacketPar.bPlayControlCmdCode =
                            IPOD_PLAY_CONTROL_CMD_CODES.PARAM_STOP;
                        // RETAILMSG(1,(L"PLay Control0=PARAM_STOP\r\n"));
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                    }
                    else if (iWParam != IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_FF
                        && iWParam != IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_REW)
                    {
                        // 如果当前处于快进快退状态，则在收到任何除快进快退的其它状态后均取消快进快退
                        // RETAILMSG(1,(L"End FFREW3\r\n"));
                        mTxPacketPar.bPlayControlCmdCode =
                            IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW;
                        // RETAILMSG(1,(L"PLay Control1=PARAM_END_FF_REW\r\n"));
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);

                        if (mIpodPlayingStateBoolean == 2)
                        {
                            mTxPacketPar.bPlayControlCmdCode =
                                IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY;
                            mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                        }
                        // chTimerMode =6;
                    }
                    else
                    {
                        mTxPacketPar.bPlayControlCmdCode = (byte)iWParam;
                        // RETAILMSG(1,(L"PLay Control3=%ld\r\n",iWParam));
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                    }
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW)
                {
                    // 快进/快退状态已终止,不进行任何操作
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_NEXT_TRACK
                    || iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PREV_TRACK)
                {
                    // 如果状态是上一曲／下一曲
                    if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Audio)
                    {
                        // 对于单曲的情况，如果直接发上一曲或下一曲命令，iPhone/iTouch将会Stop，
                        // 如果状态是音频下的上一曲／下一曲
                        // if(mDataLayer.GetTotalTrack() > 1)
                        // {
                        mTxPacketPar.bPlayControlCmdCode = (byte)iWParam;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                        // }
                    }
                    else if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video
                        || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
                    {
                        // 如果状态是视频下的上一曲／下一曲
                        if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_NEXT_TRACK)
                        {
                            iWParam =
                                IPOD_PLAY_CONTROL_CMD_CODES.PARAM_NEXT_CHAPTER;
                            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_VOIDO_OVER_VIDEOVIEW,
                                2,
                                null);
                        }
                        else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PREV_TRACK)
                        {
                            iWParam =
                                IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PREV_CHAPTER;
                            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_VOIDO_OVER_VIDEOVIEW,
                                1,
                                null);
                        }
                        mTxPacketPar.bPlayControlCmdCode = (byte)iWParam;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);

                    }
                }
                // else if (iWParam ==
                // IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_REW
                // && mDataLayer.GetPlayState() ==
                // IPOD_PLAY_STATE.IPOD_PST_PAUSE
                // /*&&m_iPodInfo->ptime.LastElapseTime==0*/)
                // {
                // //对策暂停在歌曲开始状态下快退的bug
                // //不进行任何操作
                // }
                else
                {
                    mTxPacketPar.bPlayControlCmdCode = (byte)iWParam;
                    // RETAILMSG(1,(L"PLay Control4=%d\r\n",iWParam));
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                    // 跳转到播放界面
                    mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_VOIDO_OVER_VIDEOVIEW,
                        0,
                        null);
                }
            }
                break;
            case SP_MEDIA_Image:
            {
                if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE
                    && iLParam != 0)
                {
                    // 指定是play还是pause
                    if (iLParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY
                        && mDataLayer.GetPlayState() != IPOD_PLAY_STATE.IPOD_PST_PLAY)
                    {
                        // play
                        mTxPacketPar.uImgSpeBtnState =
                            IPOD_IMG_SPECIFIC_BTN_BITMASK.IISBB_PLAY_RESUME;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ImageButtonStatus);
                    }
                    else if (iLParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PAUSE
                        && mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PLAY)
                    {
                        // pause
                        mTxPacketPar.uImgSpeBtnState =
                            IPOD_IMG_SPECIFIC_BTN_BITMASK.IISBB_PAUSE;
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ImageButtonStatus);
                    }
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE)
                {
                    mTxPacketPar.uImgSpeBtnState =
                        IPOD_IMG_SPECIFIC_BTN_BITMASK.IISBB_PLAY_PAUSE;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ImageButtonStatus);
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_NEXT_TRACK)
                {
                    mTxPacketPar.uImgSpeBtnState =
                        IPOD_IMG_SPECIFIC_BTN_BITMASK.IISBB_NEXT_IMG;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ImageButtonStatus);
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PREV_TRACK)
                {
                    mTxPacketPar.uImgSpeBtnState =
                        IPOD_IMG_SPECIFIC_BTN_BITMASK.IISBB_PREV_IMG;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ImageButtonStatus);
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_STOP)
                {
                    mTxPacketPar.uImgSpeBtnState =
                        IPOD_IMG_SPECIFIC_BTN_BITMASK.IISBB_STOP;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ImageButtonStatus);
                }
            }
                break;
            case SP_MEDIA_DirectControlAudio:
            {
                if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE)
                {
                    mTxPacketPar.uContextBtnState =
                        IPOD_CONTEXT_BTN_BITMASK.ICBB_PLAY_PAUSE;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_NEXT_TRACK)
                {
                    mTxPacketPar.uContextBtnState =
                        IPOD_CONTEXT_BTN_BITMASK.ICBB_NEXT_TRACK;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PREV_TRACK)
                {
                    mTxPacketPar.uContextBtnState =
                        IPOD_CONTEXT_BTN_BITMASK.ICBB_PREV_TRACK;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_FF)
                {
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_REW)
                {
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW)
                {
                }
            }
                break;
            case SP_MEDIA_DirectControlVideo:
            case SP_MEDIA_REAR_DirectControlVideo:
            {
                if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE)
                {
                    mTxPacketPar.uContextBtnState =
                        IPOD_CONTEXT_BTN_BITMASK.ICBB_PLAY_PAUSE;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_NEXT_TRACK)
                {
                    mTxPacketPar.uContextBtnState =
                        IPOD_CONTEXT_BTN_BITMASK.ICBB_NEXT_TRACK;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PREV_TRACK)
                {
                    mTxPacketPar.uContextBtnState =
                        IPOD_CONTEXT_BTN_BITMASK.ICBB_PREV_TRACK;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_FF)
                {
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_START_REW)
                {
                }
                else if (iWParam == IPOD_PLAY_CONTROL_CMD_CODES.PARAM_END_FF_REW)
                {
                }
            }
                break;
            default:
                break;
        }
    }

    private void Op_ReturnParentFolder()
    {
        if (mTreeManager.IsRootLevel())
            return;

        // 设置列表刷新类别为: 上一层目录
        // m_piPodCtlInfo->chPlayListFlag =0;
        if (!mTreeManager.IsAudioDisplay())
        {
            int nCodeID = mTreeManager.GetParentType();
            if (nCodeID == -1)
                return;
            mTreeManager.SetCurrentCategory(nCodeID);
            mPrepareSetIndex = -1;
            // 返回上一层DB目录
            mTxPacketPar.bDbCateType = (byte)mTreeManager.GetCurrentCategory();
            mTxPacketPar.iDbRecordIndex = -1;
            mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SelectDBRecord);
            return;
        }

        // 如果判断是目录树的最上层,则使用ResetDB来刷新根目录
        if (mTreeManager.IsLastLevel())
        {
            return;
            // mTreeManager.SetCurrentCategory(IPOD_CATEGORY_TYPES.CATEGORY_NOTHING);
            // mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ResetDBSelection);
        }
        else
        {
            int nCodeID = 0;
            nCodeID = mTreeManager.GetParentType();
            if (nCodeID == -1)
                return;
            // RETAILMSG(1,(L"Op_ReturnParentFolder GetParentType=%ld\r\n",nCodeID));
            if (nCodeID != IPOD_CATEGORY_TYPES.CATEGORY_AUDIOBOOK)
            {
                mTreeManager.SetCurrentCategory(nCodeID);

                mPrepareSetIndex = -1;
                mbIsNeedRefreshList = true;
                // 返回上一层DB目录
                mTxPacketPar.bDbCateType =
                    (byte)mTreeManager.GetCurrentCategory();
                mTxPacketPar.iDbRecordIndex = -1;
                mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SelectDBRecord);
            }
        }
    }

    private void Op_EnterChildFolder(long nTrackIndex, boolean bIsUITask)
    {
        TraceManager.LogE(TAG, "Op_EnterChildFolder nTrackIndex:" + nTrackIndex);
        // 设置列表刷新列表为: 下一层目录
        SongData _ListSong = mTreeManager.GetTrackInfo(nTrackIndex);

        if (_ListSong == null)
            return;
        if (_ListSong.fileType == SP_IPOD_LIST_FILE_TYPE.LIST_FILE_AUDIO
            || _ListSong.fileType == SP_IPOD_LIST_FILE_TYPE.LIST_FILE_VIDEO) // 当前为歌曲，需要播放歌曲
        {
            TraceManager.LogE(TAG, "Op_EnterChildFolder _ListSong" + _ListSong);
            TraceManager.LogE(TAG, "Op_EnterChildFoldern TrackIndex "
                + nTrackIndex);
            TraceManager.LogE(TAG,
                "Op_EnterChildFolderm TreeManager.IsPlayTrackInlist()"
                    + mTreeManager.IsPlayTrackInlist());
            TraceManager.LogE(TAG,
                "Op_EnterChildFolder mTreeManager.GetCurrentPlayingTrack()"
                    + mTreeManager.GetCurrentPlayingTrack());
            if (mTreeManager.IsPlayTrackInlist()
                && mTreeManager.GetCurrentPlayingTrack() == nTrackIndex)
            {
                // RETAILMSG(1,(L"Op_EnterChildFolder=%ld\r\n",m_iPodInfo->pstate));
                // 当前选择的正是当前播放的曲目
                if (mDataLayer.GetPlayState() != IPOD_PLAY_STATE.IPOD_PST_PLAY)
                {
                    // play
                    mTxPacketPar.bPlayControlCmdCode =
                        IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY;

                    if (!bIsUITask)
                    {
                        synchronized (mSycTask)
                        {
                            mTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                        }
                    }
                    else
                    {
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayControl);
                    }
                }
            }
            else
            {
                int nCategoryCodeID = mTreeManager.GetParentType();
                if (nCategoryCodeID == -1)
                    return;
                if (nCategoryCodeID == IPOD_CATEGORY_TYPES.CATEGORY_AUDIOBOOK)
                {

                    // 记忆当前需要播放的歌曲索引
                    mPreparePlayingSongIndex = nTrackIndex;
                    mTxPacketPar.iTrackIndex = mPreparePlayingSongIndex;
                    // 是否已经从列表选则过歌曲播放
                    mbSelListTrackToPlay = true;

                    mPrepareSetIndex = nTrackIndex;
                    TraceManager.LogE(TAG,
                        "Op_EnterChildFolder CATEGORY_AUDIOBOOK mPreparePlayingSongIndex"
                            + mPreparePlayingSongIndex);
                    // 记忆当前目录的索引
                    mTxPacketPar.bDbCateType =
                        IPOD_CATEGORY_TYPES.CATEGORY_AUDIOBOOK;
                    mTxPacketPar.iDbRecordIndex = nTrackIndex;
                    if (!bIsUITask)
                    {
                        mTask.addLast(IPOD_MAIN_TASK.ITASK_SelectDBRecord);
                    }
                    else
                    {
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SelectDBRecord);
                    }
                }
                else
                {
                    // 记忆当前需要播放的歌曲索引
                    mPreparePlayingSongIndex = nTrackIndex;// +
                                                           // (piPodCtlInfo->iCurrentPageIndex-1)*PAGE_SIZE;
                    mTxPacketPar.iTrackIndex = mPreparePlayingSongIndex;
                    TraceManager.LogE(TAG,
                        "Op_EnterChildFolder mPreparePlayingSongIndex"
                            + mPreparePlayingSongIndex);
                    // 是否已经从列表选则过歌曲播放
                    mbSelListTrackToPlay = true;

                    if (!bIsUITask)
                    {
                        mTask.addLast(IPOD_MAIN_TASK.ITASK_PlayCurrentSelection);
                    }
                    else
                    {
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_PlayCurrentSelection);
                    }
                }
            }
        }
        else
        {
            int nCategoryCodeID = mTreeManager.GetParentType();
            if (nCategoryCodeID == -1)
                return;

            if (nCategoryCodeID == IPOD_CATEGORY_TYPES.CATEGORY_NOTHING) // 从第一级目录进入指定库,音频
            {

                mPrepareSetIndex = nTrackIndex;
                // 获取当前记录类型
                int nCategory = mTreeManager.GetNodeType(nTrackIndex);
                if (nCategory == -1)
                    return;
                // 记录第一个目录类型
                mTreeManager.SetCurrentCategory(nCategory);// nTrackIndex;
                mTxPacketPar.bDbCateType =
                    (byte)mTreeManager.GetCurrentCategory();// nTrackIndex;
                TraceManager.LogE(TAG,
                    "Op_EnterChildFolder CATEGORY_NOTHING mPreparePlayingSongIndex"
                        + mPreparePlayingSongIndex);
                // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_PLAYING_INDEX,
                // (int)mPreparePlayingSongIndex, null);
                mbIsNeedRefreshList = true;
                if (!bIsUITask)
                {
                    mTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumberCategorizedDBRecords);
                }
                else
                {
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumberCategorizedDBRecords);
                }
            }
            else
            {
                mPrepareSetIndex = nTrackIndex;
                // 记忆当前目录的索引
                mTxPacketPar.bDbCateType =
                    (byte)mTreeManager.GetCurrentCategory();
                mTxPacketPar.iDbRecordIndex = nTrackIndex;
                if (!bIsUITask)
                {
                    mTask.addLast(IPOD_MAIN_TASK.ITASK_SelectDBRecord);
                }
                else
                {
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SelectDBRecord);
                }
                mTreeManager.SetCurrentCategory(GetChildFolderCategory(nTrackIndex));
            }
        }
    }

    private byte GetChildFolderCategory(long nTrackIndex)
    {
        int nCategory = mTreeManager.GetNodeType(nTrackIndex);
        if (nCategory == -1)
            return IPOD_CATEGORY_TYPES.CATEGORY_NOTHING;

        if (mTreeManager.IsAudioDisplay())
        {
            switch (nCategory)
            {
                case IPOD_CATEGORY_TYPES.CATEGORY_ARTIST:
                    return IPOD_CATEGORY_TYPES.CATEGORY_ALBUM;
                case IPOD_CATEGORY_TYPES.CATEGORY_ALBUM:
                    return IPOD_CATEGORY_TYPES.CATEGORY_TRACK;
                case IPOD_CATEGORY_TYPES.CATEGORY_GENRE:
                    return IPOD_CATEGORY_TYPES.CATEGORY_ARTIST;
                case IPOD_CATEGORY_TYPES.CATEGORY_PLAYLIST:
                    return IPOD_CATEGORY_TYPES.CATEGORY_TRACK;
                case IPOD_CATEGORY_TYPES.CATEGORY_COMPOSER:
                    return IPOD_CATEGORY_TYPES.CATEGORY_ALBUM;
                case IPOD_CATEGORY_TYPES.CATEGORY_AUDIOBOOK:
                    return IPOD_CATEGORY_TYPES.CATEGORY_AUDIOBOOK;
                case IPOD_CATEGORY_TYPES.CATEGORY_PODCAST:
                    return IPOD_CATEGORY_TYPES.CATEGORY_TRACK;
                default:
                    return IPOD_CATEGORY_TYPES.CATEGORY_TRACK;
            }
        }
        else
        {
            if (nTrackIndex == 0x03 && mTreeManager.ParentIsVideoRootLevel()) // 在视频目录中，第一级目录第四条记录是Video
                                                                              // podcast
            {
                return IPOD_CATEGORY_TYPES.CATEGORY_PODCAST;
            }
            else if (nTrackIndex == 0x01
                && mTreeManager.ParentIsVideoRootLevel())// 在视频目录中，第一级目录第二条记录是Music
                                                         // Video
            {
                return IPOD_CATEGORY_TYPES.CATEGORY_ARTIST;
            }
            else
            {
                return IPOD_CATEGORY_TYPES.CATEGORY_TRACK;
            }
        }
    }

    private boolean CreateArtWorkBITMAP(STRUCT_ARTWORK sArtwork)
    {
        byte[] artwork_data = new byte[sArtwork.total_byte];
        if (mDataLayer.GetArtwork() != null)
        {
            mDataLayer.SetArtwork(null);
        }
        // 拷贝数据
        switch (sArtwork.pixelFormat)
        {
            case iPodPixelForamt.IPF_MONOCHRONE:// Mono Image
            {
            }
                break;
            case iPodPixelForamt.IPF_RGB565_BIG_ENDIAN:// RGB565,big-endian//5red-3lowgreen-3greenupper-5blue
            {
                for (int i = 0; i < artwork_data.length; i = i + 2)
                {
                    artwork_data[i] = sArtwork.image_data[i + 1];
                    artwork_data[i + 1] = sArtwork.image_data[i];
                }
            }
                break;
            case iPodPixelForamt.IPF_RGB565_LITTLE_ENDIAN:// RGB565,little-endian//3lowgreen-5blue-5red-3greenupper
            {
                TypeConvert.memcpy(artwork_data,
                    sArtwork.image_data,
                    0,
                    0,
                    sArtwork.total_byte);
            }
                break;
            default:
                return false;
        }
        ByteBuffer artwork_bufferBuffer = ByteBuffer.wrap(artwork_data);
        Bitmap temp =
            Bitmap.createBitmap(sArtwork.width, sArtwork.height, Config.RGB_565);
        temp.copyPixelsFromBuffer(artwork_bufferBuffer);
        mDataLayer.SetArtwork(temp);
        if (mDataLayer.GetArtwork() == null)
        {
            return false;
        }
        return true;
    }

    private void ResetVideoDB()
    {
        // 记录第一个目录类型
        mTreeManager.SetCurrentCategory(IPOD_CATEGORY_TYPES.CATEGORY_GENRE);
        mTreeManager.DisplayMediaType(false);
        mbIsNeedRefreshList = true;
        mTxPacketPar.bDbCateType = (byte)mTreeManager.GetCurrentCategory();
        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumberCategorizedDBRecords);
    }

    private void UpdateFolderNumber(long nCount)
    {
        UpdateTreeFolderLevel();
        mTreeManager.SetChildCapacity(nCount);
        if (mbSyncPath)
            PathBack_Changing(false);
        else
            RefreshList(false, false);
    }

    private void UpdateTreeFolderLevel()
    {
        if (mPrepareSetIndex != -2)
        {
            if (mPrepareSetIndex == -1) // 回到上一级目录
            {
                if ((mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
                    && mTreeManager.GetCurrentCategory() == IPOD_CATEGORY_TYPES.CATEGORY_GENRE)
                    mTreeManager.ReturnRootLevel();
                else
                    mTreeManager.ReturnParentFolder();
            }
            else
            // 进入下一级目录
            {
                mTreeManager.EnterChildFolder(mPrepareSetIndex);
            }
            mPrepareSetIndex = -2;
        }
    }

    private void RefreshList(boolean bIsUITask, boolean bIsAuto)
    {
        long lTrackNum = mTreeManager.GetChildCapacity();
        long lTrackSize = mTreeManager.GetChildSize();
        TraceManager.LogE(TAG, "RefreshList->ChildCapacity()" + lTrackNum
            + "->ChildSize()" + lTrackSize);
        if (!bIsAuto)
        {
            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
                0,
                mTreeManager.GetListPath());
            Log.e(TAG, "RefreshList%%%%%%%%");
            mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST,
                IPOD_LOAD_MEMORY_STATE.IPOD_LOAD_START,
                mTreeManager.GetCurListSongData());
            if (lTrackNum == lTrackSize)
            {
                mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST,
                    0,
                    null);
                return;
            }
        }
        else
        {
            if (lTrackNum == lTrackSize)
                return;
        }
        int nCount =
            TypeConvert.min((int)(lTrackNum - lTrackSize), TRACK_COUNT_ONE_TIME);
        mTxPacketPar.iDbRecordStartIndex = lTrackSize;
        mTxPacketPar.iDbRecordReadCount = nCount;
        mRecordReadCount = nCount;
        if (!bIsUITask && !bIsAuto)
        {
            mTempTask.addLast(IPOD_MAIN_TASK.ITASK_RetrieveCategorizedDBRecords);
        }
        else if (!bIsUITask && bIsAuto)
        {
            synchronized (mSycTask)
            {
                mTask.addLast(IPOD_MAIN_TASK.ITASK_RetrieveCategorizedDBRecords);
            }
        }
        else
        {
            mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_RetrieveCategorizedDBRecords);
        }
    }

    private void ReceivedOneTrack(RX_PACKET_PARAM rxParam)
    {
        byte trackType = IPOD_CATEGORY_TYPES.CATEGORY_NOTHING;
        TraceManager.LogE(TAG,
            "ReceivedOneTrack->" + mTreeManager.IsRootLevel() + "   "
                + mDataLayer.GetiPodOSD().bIsEnabled + "  index:"
                + rxParam.iRecordCategoryIndex + ":" + rxParam.wsRecord);
        if (mTreeManager.IsRootLevel() && mDataLayer.GetiPodOSD().bIsEnabled)
        {
            // 视频的根目录
            switch (rxParam.iRecordCategoryIndex)
            {
                case 0:
                    rxParam.wsRecord = mDataLayer.GetiPodOSD().wcMovies;
                    trackType = IPOD_CATEGORY_TYPES.CATEGORY_Movies;
                    break;
                case 1:
                    rxParam.wsRecord = mDataLayer.GetiPodOSD().wcMusicVideos;
                    trackType = IPOD_CATEGORY_TYPES.CATEGORY_MusicVideos;
                    break;
                case 2:
                    rxParam.wsRecord = mDataLayer.GetiPodOSD().wcTVShows;
                    trackType = IPOD_CATEGORY_TYPES.CATEGORY_TVShows;
                    break;
                case 3:
                    rxParam.wsRecord = mDataLayer.GetiPodOSD().wcVideoPodcasts;
                    trackType = IPOD_CATEGORY_TYPES.CATEGORY_VideoPodcasts;
                    break;
                case 4:
                    rxParam.wsRecord = mDataLayer.GetiPodOSD().wcRentals;
                    trackType = IPOD_CATEGORY_TYPES.CATEGORY_Rentals;
                    break;
                default:
                    break;
            }
        }
        else if (mTreeManager.IsRootLevel())
        {
            // 视频的根目录
            switch (rxParam.iRecordCategoryIndex)
            {
                case 0:
                    trackType = IPOD_CATEGORY_TYPES.CATEGORY_Movies;
                    break;
                case 1:
                    trackType = IPOD_CATEGORY_TYPES.CATEGORY_MusicVideos;
                    break;
                case 2:
                    trackType = IPOD_CATEGORY_TYPES.CATEGORY_TVShows;
                    break;
                case 3:
                    trackType = IPOD_CATEGORY_TYPES.CATEGORY_VideoPodcasts;
                    break;
                case 4:
                    trackType = IPOD_CATEGORY_TYPES.CATEGORY_Rentals;
                    break;
                default:
                    break;
            }
        }
        else
        {
            trackType = (byte)mTreeManager.GetCurrentCategory();
        }
        long iTrackIndex = rxParam.iRecordCategoryIndex;
        String stTrackName = rxParam.wsRecord;
        TreeNode node =
            mTreeManager.AddFile(iTrackIndex, stTrackName, trackType);
        if (null != onDataChange)
        {
            onDataChange.onDataChange(node);
        }
    }

    public OnDataChange onDataChange = null;

    public void setOnDataChange(OnDataChange onDataChange)
    {
        this.onDataChange = onDataChange;
    }

    public interface OnDataChange
    {
        public void onDataChange(TreeNode node);
    }

    private void InitRootList()
    {

        // 每次切换media类型全部清掉列表树
        mTreeManager.ClearAll();
        mbSelListTrackToPlay = false;
        // 将列表更新到根目录
        if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Audio)
        {
            // 音频
            mTreeManager.DisplayMediaType(true);
            mTreeManager.ReturnRootLevel();
            mTreeManager.SetAudioRootName(mDataLayer.GetiPodOSD().wcAudio);
            mTreeManager.SetChildCapacity(8);
            mTreeManager.AddFile(0,
                mDataLayer.GetiPodOSD().wcPlaylists,
                IPOD_CATEGORY_TYPES.CATEGORY_PLAYLIST);
            mTreeManager.AddFile(1,
                mDataLayer.GetiPodOSD().wcArtists,
                IPOD_CATEGORY_TYPES.CATEGORY_ARTIST);
            mTreeManager.AddFile(2,
                mDataLayer.GetiPodOSD().wcAlbums,
                IPOD_CATEGORY_TYPES.CATEGORY_ALBUM);
            mTreeManager.AddFile(3,
                mDataLayer.GetiPodOSD().wcGenres,
                IPOD_CATEGORY_TYPES.CATEGORY_GENRE);
            mTreeManager.AddFile(4,
                mDataLayer.GetiPodOSD().wcSongs,
                IPOD_CATEGORY_TYPES.CATEGORY_TRACK);
            mTreeManager.AddFile(5,
                mDataLayer.GetiPodOSD().wcComposers,
                IPOD_CATEGORY_TYPES.CATEGORY_COMPOSER);
            mTreeManager.AddFile(6,
                mDataLayer.GetiPodOSD().wcAudiobooks,
                IPOD_CATEGORY_TYPES.CATEGORY_AUDIOBOOK);
            mTreeManager.AddFile(7,
                mDataLayer.GetiPodOSD().wcPodcasts,
                IPOD_CATEGORY_TYPES.CATEGORY_PODCAST);
        }
        else if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video
            || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
        {
            // 视频
            mTreeManager.DisplayMediaType(false);
            mTreeManager.ReturnRootLevel();
            mTreeManager.SetVideoRootName(mDataLayer.GetiPodOSD().wcVideo);
        }
    }

    private void RefreshRootList()
    {
        mTreeManager.ReturnRootLevel();
        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
            0,
            mTreeManager.GetListPath());
        Log.e(TAG, "RefreshRootList%%%%%%%%%%%%%%%%%");
        mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST,
            0,
            mTreeManager.GetCurListSongData());
    }

    private void SyncPath()
    {
        // 每次同步列表证明，即进入列表模式，此时需要刷新列表
        mbIsNeedRefreshList = true;
        if (mTreeManager.IsPlayTrackInlist()) // 播放曲目，在当前目录下，则只需要滚动指定位置即可。
        {
            mbSyncPath = false;
            RefreshList(true, false);
        }
        else
        // 播放曲不在当前目录下
        {
            if (mTreeManager.IsRootLevel()) // 在根目录下
            {
                PathBack_Changing(true);
            }
            else
            // 不在根目录下, 先要回到根目录
            {
                if (!mTreeManager.IsAudioDisplay())
                {
                    mPrepareSetIndex = -1;
                    mTreeManager.SetCurrentCategory(IPOD_CATEGORY_TYPES.CATEGORY_GENRE);
                    mTxPacketPar.bDbCateType =
                        (byte)mTreeManager.GetCurrentCategory();
                    mTxPacketPar.iDbRecordIndex = -1;
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SelectDBRecord);
                }
                else
                {
                    mTreeManager.SetCurrentCategory(IPOD_CATEGORY_TYPES.CATEGORY_NOTHING);
                    mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_ResetDBSelection);
                }
            }
        }
    }

    public void PathBack_Changing(boolean bIsUITask)
    {
        iPodLogDefines.iPodLog(TAG, "PathBack_Changing");
        IPOD_RET_RESULT_VALUE res = mTreeManager.PathBack_GetNextIndex();
        long nID = 0;
        if (res.value != null)
        {
            nID = (Long)res.value;
        }
        int nRes = res.result;
        if (nRes == 1) // 就在当前目录下
        {
            mbSyncPath = false;
            RefreshList(bIsUITask, false);
        }
        else if (nRes == 0) // 进入子目录
        {
            SongData _ListSong = mTreeManager.GetTrackInfo(nID);
            if (_ListSong == null)
            {
                mbSyncPath = false;
                RefreshList(bIsUITask, false);
                return;
            }

            if (_ListSong.fileType == SP_IPOD_LIST_FILE_TYPE.LIST_FILE_AUDIO
                || _ListSong.fileType == SP_IPOD_LIST_FILE_TYPE.LIST_FILE_VIDEO) // 当前为歌曲，需要播放歌曲
            {
                mbSyncPath = false;
                RefreshList(bIsUITask, false);
                return;
            }
            else
            {
                int nCategoryCodeID = mTreeManager.GetParentType();
                if (nCategoryCodeID == -1)
                {
                    mbSyncPath = false;
                    RefreshList(bIsUITask, false);
                    return;
                }

                if (nCategoryCodeID == IPOD_CATEGORY_TYPES.CATEGORY_NOTHING) // 从第一级目录进入指定库
                {
                    mPrepareSetIndex = nID;

                    // 获取当前记录类型
                    int nCategory = mTreeManager.GetNodeType(nID);
                    if (nCategory == -1)
                        return;
                    // 记录第一个目录类型
                    mTreeManager.SetCurrentCategory(nCategory);// nID;
                    mTxPacketPar.bDbCateType = (byte)nCategory;// nID;
                    if (!bIsUITask)
                    {
                        iPodLogDefines.iPodLog(TAG, "PathBack_Changing->NOUI");
                        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumberCategorizedDBRecords);
                    }
                    else
                    {
                        iPodLogDefines.iPodLog(TAG, "PathBack_Changing->UI");
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_GetNumberCategorizedDBRecords);
                    }
                }
                else
                {
                    mPrepareSetIndex = nID;
                    // 记忆当前目录的索引
                    mTxPacketPar.bDbCateType =
                        (byte)mTreeManager.GetCurrentCategory();
                    mTxPacketPar.iDbRecordIndex = nID;
                    if (!bIsUITask)
                    {
                        mTempTask.addLast(IPOD_MAIN_TASK.ITASK_SelectDBRecord);
                    }
                    else
                    {
                        mUITempTask.addLast(IPOD_MAIN_TASK.ITASK_SelectDBRecord);
                    }
                    mTreeManager.SetCurrentCategory(GetChildFolderCategory(nID));
                }
            }
        }
        else
        {
            mbSyncPath = false;
            RefreshList(bIsUITask, false);
        }
    }

    private void InitTimer()
    {
        if (mTimerTask != null)
        {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if (mTimer != null)
        {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimer == null)
        {
            mTimer = new Timer();
        }
        if (mTimerTask == null)
        {
            mTimerTask = new TimerTask()
            {

                @Override
                public void run()
                {
                    // TODO Auto-generated method stub
                    iPodTaskManager(true);
                }
            };
        }
        mTimer.schedule(mTimerTask, 0, 50);
    }

    public void iPodDeInit()
    {
        if (mTimerTask != null)
        {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if (mTimer != null)
        {
            // mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
        mTask.clear();
        mTempTask.clear();
        mUITempTask.clear();
        if (mDataLayer.GetPlayState() == IPOD_PLAY_STATE.IPOD_PST_PLAY
            || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_DirectControlAudio
            || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_DirectControlVideo
            || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_DirectControlVideo)
        {

            switch (mDataLayer.GetMediaType())
            {
                case SP_MEDIA_Video:
                case SP_MEDIA_Audio:
                case SP_MEDIA_REAR_VIDEO:
                    mTxPacketPar.bPlayControlCmdCode =
                        IPOD_PLAY_CONTROL_CMD_CODES.PARAM_PLAY_PAUSE;
                    mTask.add(IPOD_MAIN_TASK.ITASK_PlayControl);
                    mCuriPodTask = IPOD_MAIN_TASK.ITASK_idle;
                    break;
                case SP_MEDIA_DirectControlAudio:
                case SP_MEDIA_DirectControlVideo:
                case SP_MEDIA_REAR_DirectControlVideo:
                    // mTxPacketPar.uContextBtnState = ICBB_PAUSE;
                    mTask.add(IPOD_MAIN_TASK.ITASK_ContextButtonStatus);
                    break;
                default:
                    break;
            }
            iPodTaskManager(false);
        }
        ClearAllInfo();
        mDataLayer.SetMediaType(SP_MEDIA_TYPE_ENUM.SP_MEDIA_Unknow);
        mDataLayer.SetIsPlayingVideo(false);
        mbSetShuffle = false;
    }

    private void ClearAllInfo()
    {
        mDataLayer.SetTrackTitle("");
        mDataLayer.SetTrackAlbum("");
        mDataLayer.SetTrackArtist("");
        mDataLayer.SetTrackGenre("");
        mDataLayer.SetArtwork(null);
        mDataLayer.SetCurPlayTime(0);
        mDataLayer.SetTotalPlayTime(0);
        mDataLayer.SetCurTrackIndex(0);
        mDataLayer.SetTotalTrack(0);
        mDataLayer.SetPlayState(IPOD_PLAY_STATE.IPOD_PST_NONE);
        mDataLayer.SetRepeatState(IPOD_REPEAT_STATUS.IPOD_REPEAT_OFF);
        mDataLayer.SetShuffleState(IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_OFF);
    }

    public void ReadError()
    {
        iPodProcessTask(mCuriPodTask, true);
    }
    // public void ChangeOSD(SP_IPOD_OSD sOSD){
    // Log.e("OSD", "ChangeOSD"+sOSD.wcPlaylists);
    // if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Audio)
    // {
    // //音频
    // mTreeManager.DisplayMediaType(true);
    // mTreeManager.SetAudioChildName(0, sOSD.wcPlaylists);
    // mTreeManager.SetAudioChildName(1, sOSD.wcArtists);
    // mTreeManager.SetAudioChildName(2, sOSD.wcAlbums);
    // mTreeManager.SetAudioChildName(3, sOSD.wcGenres);
    // mTreeManager.SetAudioChildName(4, sOSD.wcSongs);
    // mTreeManager.SetAudioChildName(5, sOSD.wcComposers);
    // mTreeManager.SetAudioChildName(6, sOSD.wcAudiobooks);
    // mTreeManager.SetAudioChildName(7, sOSD.wcPodcasts);
    // mDataLayer.SetiPodOSD(sOSD);
    // Log.e("OSD", "sOSD.wcPlaylists"+sOSD.wcPlaylists);
    // }
    // else if (mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_Video
    // || mDataLayer.GetMediaType() == SP_MEDIA_TYPE_ENUM.SP_MEDIA_REAR_VIDEO)
    // {
    // //视频
    // mTreeManager.DisplayMediaType(false);
    // mTreeManager.ReturnRootLevel();
    // mTreeManager.SetVideoRootName(mDataLayer.GetiPodOSD().wcVideo);
    // }
    // if (eNeedSelCategoryType==null)
    // return;
    // switch(eNeedSelCategoryType)
    // {
    // case LIST_CATEGORY_UNKNOWN:
    //
    // break;
    // case LIST_CATEGORY_PLAYLISTS:
    //
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcPlaylists);
    // break;
    // case LIST_CATEGORY_ARTISTS:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcArtists);
    // break;
    // case LIST_CATEGORY_SONGS:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcSongs);
    // break;
    // case LIST_CATEGORY_ALBUMS:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcAlbums);
    // break;
    // case LIST_CATEGORY_COMPOSERS:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcComposers);
    // break;
    // case LIST_CATEGORY_AUDIOBOOKS:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcAudiobooks);
    // break;
    // case LIST_CATEGORY_GENRES:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcGenres);
    // break;
    // case LIST_CATEGORY_PODCASTS:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcPodcasts);
    // break;
    // case LIST_CATEGORY_MOVIES:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcPlaylists);
    // break;
    // case LIST_CATEGORY_MUSIC_VIDEOS:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcPlaylists);
    // break;
    // case LIST_CATEGORY_TV_SHOWS:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcPlaylists);
    // break;
    // case LIST_CATEGORY_VIDEO_PODCASTS:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcPlaylists);
    // break;
    // case LIST_CATEGORY_RENTALS:
    // mCmdCallBack.EnterCallBackFun(SP_IPOD_REFRESH_ENUM.SP_IPOD_REF_LIST_Path,
    // 0,sOSD.wcPlaylists);
    // break;
    // default:
    //
    // break;
    // }
    // }
}
