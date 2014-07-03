package pateo.engine.iPod;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import pateo.frameworks.api.iPodJNI;
import android.graphics.Point;

public interface iPodDataDef
{
    // 操作事件定义,提供给UI的功能接口参数
    public enum HANDLE_EVENT
    {
        EVENT_IPOD_IDENTIFY, // iPod认证
        EVENT_IPOD_PLAY_CONTROL, // 播放/暂停
        EVENT_IPOD_GET_PLAY_STATUS, // 获取当前播放状态信息
        EVENT_IPOD_SET_SHUFFLE, // 设置Shuffle状态: 随机Off;随机Song;随机Album
        EVENT_IPOD_GET_SHUFFLE, // 获取Shuffle状态
        EVENT_IPOD_SET_REPEAT, // 设置Repeat状态: 重复Off;重复One track;重复All track
        EVENT_IPOD_GET_REPEAT, // 获取Repeat状态
        EVENT_IPOD_SET_SCAN, // 设置Scan状态
        EVENT_IPOD_GET_SCAN, // 获取Scan状态
        EVENT_IPOD_PAGE_INC, // 下一页数据
        EVENT_IPOD_PAGE_DEC, // 上一页数据
        EVENT_IPOD_SCROLL_INC, // 向下滚动数据
        EVENT_IPOD_SCROLL_DEC, // 向上滚动数据
        EVENT_IPOD_REQ_CHILDREN, // 请求列表子孩子
        EVENT_IPOD_REQ_CURLIST, EVENT_IPOD_PLAY_VIDEO, // 进入iPod视频或图片模式
        EVENT_IPOD_PLAY_CURRENT_SONG, // 播放当前选择歌曲（如果没有选项则默认播放第一首）
        EVENT_IPOD_BACK_TOPMENU, // 返回根目录
        EVENT_IPOD_MENU, // 上一目录，相当于iPod的Menu按键
        EVENT_IPOD_GET_CUR_TRACK_INDEX, // 获取当前播放track的索引
        EVENT_IPOD_GET_TOLTAL_RECORDS_NUM, // 获取当前目录的子目录数目
        EVENT_IPOD_REQ_SONG_TITLE, // 获取播放列表中指定歌曲的Title信息
        EVENT_IPOD_REQ_SONG_ARITST, // 获取播放列表中指定歌曲的Artist信息
        EVENT_IPOD_REQ_SONG_ALBUM, // 获取播放列表中指定歌曲的Album信息
        EVENT_IPOD_REQ_SONG_GENRE, // 获取播放列表中指定歌曲的Genre信息
        EVENT_IPOD_REQ_PLAYLIST, // 选择当前目录
        EVENT_IPOD_REQ_CATEGORY_TYPE_LIST, // 请求指定类别的列表
        EVENT_IPOD_REQ_NAME, // 获取iPod的名字
        EVENT_IPOD_GET_TRACK_NUM, // 获取Song数目
        EVENT_IPOD_GET_PLAYLIST_POS, // 获取列表位置
        EVENT_IPOD_GET_LISTPATH_INFOR, // 获取列表路径
        EVENT_IPOD_SYNCLIST, // 同步列表
        EVENT_IPOD_GET_ARTWORK_FORMAT, // 得到Artwork format
        EVENT_IPOD_IMAGE_PLAY_CONTROL, // 图片控制
        EVENT_IPOD_VIDEO_SUBTITLE, // 视频子标题
        EVENT_CHANGE_OSD, // 改变OSD
        EVENT_ABC123_SEARCH, // ABC123Search
        EVENT_LIST_RECORDS, // 请求列表记录
        EVENT_IPOD_ERROR, // iPod返回的错误信息
        EVENT_NOTHING, // 无效事件
    }

    public enum IPOD_MAIN_TASK
    {
        ITASK_idle, // idle
        ITASK_iPod_Check, // iPod Check
        // 认证相关任务Start******************************
        // #ifdef SUPPORT_IP
        ITASK_IP_StartIDPS, ITASK_IP_SetFIDTokenValues, ITASK_IP_EndIDPS, ITASK_IP_IdentifyDeviceLingos, ITASK_IP_GetDevAuthenticationInfo, ITASK_IP_RetDevAuthenticationInfo,
        // #endif
        // 认证相关任务End********************************
        ITASK_Change_AudioType, // 改变音频类型
        ITASK_UI_ChangeType, // 改变播放类型
        ITASK_Connect_ChangeTypeWait, // 调用IPOD_Connect函数时等待音频类型改变完成
        ITASK_DeInit_ChangeTypeWait, // 调用IPOD_DeInit函数时等待音频类型改变完成
        // 协议相关任务Start******************************
        ITASK_GetSupportedEventNotification, ITASK_SetEventNotification, ITASK_RequestRemoteUIMode, ITASK_EnterRemoteUIMode, ITASK_ExitRemoteUIMode, ITASK_RequestiPodName, ITASK_RequestiPodSerialNum, ITASK_RequestiPodModelNum, ITASK_GetiPodOptionsForExtendedLingo, ITASK_GetiPodOptionsForSimpleRemoteLingo, ITASK_RequestExtendedLingoProtocolVersion, ITASK_RequestSimpleRemoteLingoProtocolVersion, ITASK_SetiPodPreferences, ITASK_ContextButtonStatus, ITASK_ImageButtonStatus, ITASK_VideoButtonStatus, ITASK_AudioButtonStatus, ITASK_GetCurrentPlayingTrackCahpterInfo, ITASK_GetIndexedPlayingTrackInfo, ITASK_GetIndexedPlayingTrackReleaseDate, ITASK_GetIndexedPlayingTrackGenre, ITASK_GetIndexedPlayingTrackArtWorkCount, ITASK_GetArtworkFormats, ITASK_GetTrackArtworkData, ITASK_ResetDBSelection, ITASK_SelectDBRecord, ITASK_GetNumberCategorizedDBRecords, ITASK_RetrieveCategorizedDBRecords, ITASK_GetPlayStatus, ITASK_GetCurrentPlayingTrackIndex, ITASK_GetIndexedPlayingTrackTitle, ITASK_GetIndexedPlayingTrackArtistName, ITASK_GetIndexedPlayingTrackAlbumName, ITASK_SetPlayStatusChangeNotification, ITASK_PlayCurrentSelection, ITASK_PlayControl, ITASK_GetTrackArtworkTimes, ITASK_GetShuffle, ITASK_SetShuffle, ITASK_GetRepeat, ITASK_SetRepeat, ITASK_SetDisplayImage, ITASK_GetMonoDisplayImageLimits, ITASK_GetNumPlayingTracks, ITASK_SetCurrentPlayingTrack, ITASK_GetColorDisplayImageLimits, ITASK_ResetDBSelectionHierarchy, ITASK_DigitalAudioLingoAccAck,
        // 协议相关任务End******************************
        // UI处理相关任务Start******************************
        ITASK_UITask_ReturnCategoryList,
        // UI处理相关任务End******************************
        // 初始化处理相关任务Start******************************
        ITASK_InitTask_ReqCurPlayIndex, ITASK_InitTask_ReqCurPlayType, ITASK_InitTask_VideoPlayDefultTrack,
        // 初始化处理相关任务End******************************
        // add by shin
        ITASK_ResetDBSelection_NoFresh,
    }

    /*Lingo:0x00,Cmd:0x000E,iPod model IDS*/
    public static final class IPOD_MODEL_IDS
    {
        public static final int IPOD_MODEL_IDS_NOTHING = 0;// Nothing

        public static final int IPOD_3G_WHITE_WHITE_WHEEL = 0x00030000;// 3G
                                                                       // iPod.This
                                                                       // is the
                                                                       // white
                                                                       // iPod
                                                                       // with 4
                                                                       // buttons
                                                                       // above
                                                                       // a
                                                                       // white
                                                                       // click
                                                                       // wheel.

        public static final int IPOD_MINI_ORIGINAL_4GB = 0x00040000;// iPod
                                                                    // mini:original
                                                                    // 4GB model

        public static final int IPOD_4G_WHITE_GRAY_WHEEL = 0x00050000;// 4G
                                                                      // iPod.This
                                                                      // is the
                                                                      // white
                                                                      // iPod
                                                                      // with a
                                                                      // gray
                                                                      // click
                                                                      // wheel

        public static final int IPOD_PHOTO = 0x00060000;// iPod photo

        public static final int IPOD_2G_4GB_6GB_MINI = 0x00070000;// 2nd
                                                                  // generation
                                                                  // iPod
                                                                  // mini(models
                                                                  // M9800-M9807,4GB
                                                                  // and 6GB)

        public static final int IPOD_5G = 0x000B0000;// 5G iPod

        public static final int IPOD_NANO = 0x000C0000;// iPod nano

        public static final int IPOD_2G_NANO = 0x00100000;// 2G iPod nano

        public static final int IPHONE = 0x00110000;// iPhone

        public static final int IPOD_CLASSIC = 0x00130000;// iPod classic

        public static final int IPOD_CLASSIC_120GB = 0x00130100;// iPod classic
                                                                // 120 GB

        public static final int IPOD_3G_NANO = 0x00140000;// 3G iPod nano

        public static final int IPOD_TOUCH = 0x00150000;// iPod touch

        public static final int IPOD_4G_NANO = 0x00170000;// 4G iPod nano

        public static final int IPHONE_3G = 0x00180000;// iPhone 3G

        public static final int TOUCH_2G = 0x00190000;// 2G touch

        public static final int IPHONE_3GS = 0x001B0000;// iPhone 3GS

        public static final int IPOD_5G_NANO = 0x001C0000;// 5G iPod nano

        public static final int IPOD_TOUCH_2G_2009 = 0x001D0000;// 2G
                                                                // touch(2009)

        public static final int IPOD_6G_NANO = 0x001E0000;// 6G iPod nano

        public static final int IPAD_IPAD_3G = 0x001F0000;// iPad and iPad 3G

        public static final int IPHONE_4 = 0x00200000;// iPhone 4

        public static final int IPHONE_4S = 0x00260000;// iPhone 4s
    };

    // 定义Audio类别
    public enum AUDIO_TYPE
    {
        AUDIO_UNKNOWN, AUDIO_DIGITAL, AUDIO_ANALOG,
    }

    // UI任务结构体
    public static final class IPOD_UI_EVENT
    {
        HANDLE_EVENT eventType;

        long iWParam;

        long iLParam;

        Object obj;

        public IPOD_UI_EVENT()
        {

        }
    }

    // UI事件处理
    public class UIEventQueue<E> extends AbstractQueue<E>
    {

        private static final int DEFAULT_CAPACITY = 10;

        private static final int DEFAULT_CAPACITY_RATIO = 2;

        private int size;

        private transient E[] elements;

        public UIEventQueue()
        {
            this(DEFAULT_CAPACITY);
        }

        public UIEventQueue(int initialCapacity)
        {
            if (initialCapacity < 1)
            {
                throw new IllegalArgumentException();
            }
            elements = newElementArray(initialCapacity);
        }

        @SuppressWarnings("unchecked")
        private E[] newElementArray(int capacity)
        {
            return (E[])new Object[capacity];
        }

        public boolean offer(E o)
        {
            // TODO Auto-generated method stub
            if (o == null)
            {
                throw new NullPointerException();
            }
            growToSize(size + 1);
            elements[size] = o;
            size++;
            return true;
        }

        public E poll()
        {
            // TODO Auto-generated method stub
            if (isEmpty())
            {
                return null;
            }
            E result = elements[0];
            removeAt(0);
            return result;
        }

        public E peek()
        {
            // TODO Auto-generated method stub
            if (isEmpty())
            {
                return null;
            }
            return elements[0];
        }

        @Override
        public Iterator<E> iterator()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int size()
        {
            // TODO Auto-generated method stub
            return size;
        }

        @Override
        public void clear()
        {
            Arrays.fill(elements, null);
            size = 0;
        }

        private void growToSize(int size)
        {
            if (size > elements.length)
            {
                E[] newElements =
                    newElementArray(size * DEFAULT_CAPACITY_RATIO);
                System.arraycopy(elements, 0, newElements, 0, elements.length);
                elements = newElements;
            }
        }

        public void removeAt(int index)
        {
            size--;
            for (int i = index; i < size; i++)
            {
                elements[i] = elements[i + 1];
            }
            elements[size] = null;
        }
    }

    // 接收数据的格式
    public enum RX_STATUS
    {
        RX_SYNC, RX_SOP, RX_PAYLOADLEN, RX_LINGO, RX_CMD, RX_TRANS, RX_DATA, RX_CHECKSUM,
    }

    // 接收包格式
    public class RX_PACKET
    {
        public byte bSync = 0;// 同步字节

        public byte bSOP = 0;// 包头

        public int uPayLoadLen = 0;// Payload长

        // PayLoad Start*********************
        public byte bLingo = 0;// Lingo ID

        public int uCmd = 0;// Command ID

        public int uTrans = 0;// Trans ID

        public IPOD_MemBlob pbData;// Data

        public int uDataLen = 0;// Data长(方便计算，发送包中没有)

        // PayLoad End*********************
        public byte bCheckSum = 0;// 校验和

        public RX_PACKET(int iLen)
        {
            pbData = new IPOD_MemBlob(iLen);
        }
    }

    // 发送包格式
    public class TX_PACKET
    {
        byte bSync = 0x00;// 同步字节

        byte bSOP = 0x55;// 包头

        int uPayLoadLen;// Payload长

        // PayLoad Start*********************
        byte bLingo;// Lingo ID

        int uCmd;// Command ID

        int uTrans;// Trans ID

        byte[] pbData;// Data

        int uDataLen;// Data长(方便计算，发送包中没有)

        // PayLoad End*********************
        byte bCheckSum;// 校验和

        // PacketCode
        long uPacketCode;

        public TX_PACKET(int len)
        {
            pbData = new byte[len];
        }
    }

    // lingo 定义
    public class IPOD_LINGO_ID
    {
        public final static byte GENERAL_LINGO = 0x00;

        public final static byte MICROPHONE_LINGO = 0x01;

        public final static byte SIMPLE_REMOTE_LINGO = 0x02;

        public final static byte DISPLAY_REMOTE_LINGO = 0x03;

        public final static byte EXTENDED_INTERFACE_LINGO = 0x04;

        public final static byte ACCESSORY_POWER_LINGO = 0x05;

        public final static byte RF_TUNER_LINGO = 0x07;

        public final static byte ACCESSORY_EQUALIZER_LINGO = 0x08;

        public final static byte DIGITAL_AUDIO_LINGO = 0x0A;

        public final static byte STORAGE_LINGO = 0x0C;
    }

    // 接收代码
    public class RX_PACKET_CODE
    {
        public final static int R_Nothing = 0x000000;// Nothing

        public final static int R_Error = 0x000001;// Error

        public final static int R_GeneralLingoACK = 0x000002;// Lingo:0x00,Cmd:0x02

        public final static int R_ReturnRemoteUIMode = 0x0004;// Lingo:0x00,Cmd:0x04

        public final static int R_ReturniPodName = 0x0008;// Lingo:0x00,Cmd:0x08

        public final static int R_ReturniPodSerialNum = 0x00000C;// Lingo:0x00,Cmd:0x0C

        public final static int R_ReturniPodModelNum = 0x00000E;// Lingo:0x00,Cmd:0x0E

        public final static int R_ReturnLingoProtocolVersion = 0x000010;// Lingo:0x00,Cmd:0x10

        public final static int R_RetiPodPreferences = 0x00002A;// Lingo:0x00,Cmd:0x2A

        public final static int R_RetiPodOptionsForLingo = 0x00004C;// Lingo:0x00,Cmd:0x4C,RetiPodOptionsForLingo

        public final static int R_RetSupportedEventNotification = 0x000051;// Lingo:0x00,Cmd:0x51,RetSupportedEventNotification

        // R_ReturnRohmSoftwareVersion=0x0000F2,//Lingo:0x00,Cmd:0xF2,For Rohm
        // Only
        public final static int R_ResetIpodDll = 0x0000F3;// Lingo:0x00,Cmd:0xF3,Reset
                                                          // iPod Dll

        public final static int R_SimpleRemoteLingoACK = 0x020001;// Lingo:0x02,Cmd:0x01

        public final static int R_ExtendInterfaceLingoACK = 0x040001;// Lingo:0x04,Cmd:0x0001

        public final static int R_ReturnIndexedPlayingTrackInfo = 0x04000D;// Lingo:0x04,Cmd:0x000D

        public final static int R_RetArtworkFormats = 0x04000F;// Lingo:0x04,Cmd:0x000F

        public final static int R_RetTrackArtworkData = 0x040011;// Lingo:0x04,Cmd:0x0011

        public final static int R_ReturnNumberCategorizedDBRecords = 0x040019;// Lingo:0x04,Cmd:0x0019

        public final static int R_ReturnCategorizeDBRecord = 0x04001B;// Lingo:0x04,Cmd:0x001B

        public final static int R_ReturnPlayStatus = 0x04001D;// Lingo:0x04,Cmd:0x001D

        public final static int R_ReturnCurrentPlayingTrackIndex = 0x04001F;// Lingo:0x04,Cmd:0x001F

        public final static int R_ReturnIndexPlayingTrackTitle = 0x040021;// Lingo:0x04,Cmd:0x0021

        public final static int R_ReturnIndexedPlayingTrackArtistName =
            0x040023;// Lingo:0x04,Cmd:0x0023

        public final static int R_ReturnIndexedPlayingTrackAlbumName = 0x040025;// Lingo:0x04,Cmd:0x0025

        public final static int R_PlayStatusChangeNotification = 0x040027;// Lingo:0x04,Cmd:0x0027

        public final static int R_RetTrackArtworkTimes = 0x04002B;// Lingo:04,Cmd:0x002B

        public final static int R_ReturnShuffle = 0x04002D;// Lingo:0x04,Cmd:0x002D

        public final static int R_ReturnRepeat = 0x040030;// Lingo:0x04,Cmd:0x0030

        public final static int R_ReturnMonoDisplayImageLimits = 0x040034;// Lingo:0x04,Cmd:0x34

        public final static int R_ReturnNumPlayingTracks = 0x040036;// Lingo:0x04,Cmd:0x0036

        public final static int R_ReturnColorDisplayImageLimits = 0x04003A;// Lingo:0x04,Cmd:0x003A

        public final static int R_RetDBTrackInfo = 0x040041;// Lingo:0x04,Cmd:0x0041

        public final static int R_NewiPodTrackInfo = 0x0A0004;// Lingo:0x0A,Cmd:0x0004
    }

    // 发送代码
    public class TX_PACKET_CODE
    {
        public final static int T_Nothing = 0x000000;// Nothing

        public final static int T_Error = 0x000001;// Error

        public final static int T_RequestRemoteUIMode = 0x000003;// Lingo:0x00,Cmd:0x03

        public final static int T_EnterRemoteUIMode = 0x000005;// Lingo:0x00,Cmd:0x05

        public final static int T_ExitRemoteUIMode = 0x000006;// Lingo:0x00,Cmd:0x06

        public final static int T_RequestiPodName = 0x000007;// Lingo:0x00,Cmd:0x07

        public final static int T_RequestiPodSerialNum = 0x00000B;// Lingo:0x00,Cmd:0x0B

        public final static int T_RequestiPodModelNum = 0x00000D;// Lingo:0x00,Cmd:0x0D

        public final static int T_RequestLingoProtocolVersion = 0x00000F;// Lingo:0x00,Cmd:0x0F

        // T_RequestRohmSoftwareVersion=0x0000F1,//Lingo:0x00,Cmd:0xF1,For Rohm
        // Only
        public final static int T_RetDevAuthenticationInfo = 0x000015;// Lingo:0x00,Cmd:0x15

        public final static int T_SetiPodPreferences = 0x00002B;// Lingo:0x00,Cmd:0x2B

        public final static int T_StartIDPS = 0x000038;// Lingo:0x00,Cmd:0x38

        public final static int T_SetFIDTokenValues = 0x000039;// Lingo:0x00,Cmd:0x39

        public final static int T_EndIDPS = 0x00003B;// Lingo:0x00,Cmd:0x3B

        public final static int T_SetEventNotification = 0x000049;// Lingo:0x00,Cmd:0x49

        public final static int T_GetiPodOptionsForLingo = 0x00004B;// Lingo:0x00,Cmd:0x4B

        public final static int T_GetSupportedEventNotification = 0x00004F;// Lingo:0x00,Cmd:0x4F

        public final static int T_ContextButtonStatus = 0x020000;// Lingo:0x02,Cmd:0x00

        public final static int T_ImageButtonStatus = 0x020002;// Lingo:0x02,Cmd:0x02

        public final static int T_VideoButtonStatus = 0x020003;// Lingo:0x02,Cmd:0x03

        public final static int T_AudioButtonStatus = 0x020004;// Lingo:0x02,Cmd:0x04

        public final static int T_SetRemoteEventNotification = 0x030008;// Lingo:0x03,Cmd:0x08

        public final static int T_GetCurrentPlayingTrackCahpterInfo = 0x040002;// Lingo:0x04,Cmd:0x0002

        public final static int T_GetIndexedPlayingTrackInfo = 0x04000C;// Lingo:0x04,Cmd:0x000C

        public final static int T_GetArtworkFormats = 0x04000E;// Lingo:0x04,Cmd:0x000E

        public final static int T_GetTrackArtworkData = 0x040010;// Lingo:0x04,Cmd:0x0010

        public final static int T_ResetDBSelection = 0x040016;// Lingo:0x04,Cmd:0x0016

        public final static int T_SelectDBRecord = 0x040017;// Lingo:0x04,Cmd:0x0017

        public final static int T_GetNumberCategorizedDBRecords = 0x040018;// Lingo:0x04,Cmd:0x0018

        public final static int T_RetrieveCategorizedDatabaseRecords = 0x04001A;// Lingo:0x04,Cmd:0x001A

        public final static int T_GetPlayStatus = 0x04001C;// Lingo:0x04,Cmd:0x001C

        public final static int T_GetCurrentPlayingTrackIndex = 0x04001E;// Lingo:0x04,Cmd:0x001E

        public final static int T_GetIndexedPlayingTrackTitle = 0x040020;// Lingo:0x04,Cmd:0x0020

        public final static int T_GetIndexedPlayingTrackArtistName = 0x040022;// Lingo:0x04,Cmd:0x0022

        public final static int T_GetIndexedPlayingTrackAlbumName = 0x040024;// Lingo:0x04,Cmd:0x0024

        public final static int T_SetPlayStatusChangeNotification = 0x040026;// Lingo:0x04,Cmd:0x0026

        public final static int T_PlayCurrentSelection = 0x040028;// Lingo:0x04,Cmd:0x0028

        public final static int T_PlayControl = 0x040029;// Lingo:0x04,Cmd:0x0029

        public final static int T_GetTrackArtworkTimes = 0x04002A;// Lingo:0x04,Cmd:0x002A

        public final static int T_GetShuffle = 0x04002C;// Lingo:0x04,Cmd:0x002C

        public final static int T_SetShuffle = 0x04002E;// Lingo:0x04,Cmd:0x002E

        public final static int T_GetRepeat = 0x04002F;// Lingo:0x04,Cmd:0x002F

        public final static int T_SetRepeat = 0x040031;// Lingo:0x04,Cmd:0x0031

        public final static int T_SetDisplayImage = 0x040032;// Lingo:0x04,Cmd:0x0032

        public final static int T_GetMonoDisplayImageLimits = 0x040033;// Lingo:0x04,Cmd:0x0033

        public final static int T_GetNumPlayingTracks = 0x040035;// Lingo:0x04,Cmd:0x0035

        public final static int T_SetCurrentPlayingTracks = 0x040037;// Lingo:0x04,Cmd:0x0037

        public final static int T_GetColorDisplayImageLimits = 0x040039;// Lingo:0x04,Cmd:0x0039

        public final static int T_ResetDBSelectionHierarchy = 0x04003B;// Lingo:0x04,Cmd:0x003B

        public final static int T_DigitalAudioLingoAccAck = 0x0A0000;// Lingo:0x0A,Cmd:0x0000
    }

    public class ACK_COMMAND_ERROR_CODES
    {
        public static final byte ACK_SUCCESS = 0x00;

        public static final byte ACK_ERR_UNKNOWN_DB_CATEGORY = 0x01;

        public static final byte ACK_ERR_CMD_FAILED = 0x02;

        public static final byte ACK_ERR_OUT_OF_RESOURCES = 0x03;

        public static final byte ACK_ERR_BAD_PARAMETER = 0x04;

        public static final byte ACK_ERR_UNKNOWN_ID = 0x05;

        public static final byte ACK_PENDING = 0x06;

        public static final byte ACK_ERR_NOT_AUTHENTICATED = 0x07;

        public static final byte ACK_ERR_BAD_AUTH_VERSION = 0x08;

        public static final byte ACK_ERR_ACCESSORY_POW_MODE_REQ = 0x09;

        public static final byte ACK_ERR_CERTIFICATE_INVALID = 0x0A;

        public static final byte ACK_ERR_CERTIFY_PERMISSIONS_INVALID = 0x0B;

        public static final byte ACK_ERR_FILE_IS_IN_USE = 0x0C;

        public static final byte ACK_ERR_INVALI_FILE_HANDLE = 0x0D;

        public static final byte ACK_ERR_DIRECTORY_NOT_EMPTY = 0x0E;

        public static final byte ACK_ERR_OPERA_TIMED_OUT = 0x0F;

        public static final byte ACK_ERR_CMD_UNAVAILABLE_IN_THIS_IPOD_MODE =
            0x10;

        public static final byte ACK_ERR_INVALI_ACCESSARY_RESISTOR_ID_VALUE =
            0x11;

        public static final byte ACK_ERR_MAX_NUM_OF_ACCESSORY_CONN_ALREADY_REACHED =
            0x15;
    }

    public class SP_IPOD_RELEASE_DATE
    {
        byte eWeekday;/*Weekday*/// Sunday 0

        int uYear;/*Year(eg.2010)*/

        byte bMonth;/*Month(1-12)*/

        byte bDay;/*Day of the month(1-31)*/

        byte bHour;/*Hours(0-23)*/

        byte bMinute;/*Minutes(0-59)*/

        byte bSecond;/*Seconds(0-59)*/
    }

    // 接收包参数
    public class RX_PACKET_PARAM
    {
        /*Lingo:0x00,Cmd:0x02,Command result status
        Lingo:0x02,Cmd:0x01,Command result status
        Lingo:0x04,Cmd:0x0001,Command result status*/
        byte bCmdResStatus;

        /*Lingo:0x00,Cmd:0x02,The ID of the command being acknowledged
        Lingo:0x02,Cmd:0x01,The ID of the command being acknowledged
        Lingo:0x04,Cmd:0x0001,The ID of the command being acknowledged*/
        long bTxAckId;

        /*Lingo:0x00,Cmd:0x02,Maximum amount of time to wait for pending response,in milliseconds
        Lingo:0x02,Cmd:0x01,Maximum amount of time to wait for pending response,in milliseconds
        Lingo:0x04,Cmd:0x0001,Maximum amount of time to wait for pending response,in milliseconds*/
        long uMaxWaitPendingRepTime;

        /*Lingo:0x00,Cmd:0x04,Extended Interface Mode byte*/
        boolean bIsInExtendedInterfaceMode;

        /*Lingo:0x00,Cmd:0x08,The name of the iPod,
        as a null-terminated UTF-8 character array*/
        String wsiPodName;

        /*Lingo:0x00,Cmd:0x0C,The iPod serial number,
        as a null-terminated UTF-8 character array*/
        String wsSerialNum;

        /*Lingo:0x00,Cmd:0x0E,iPod Model ID*/
        int iIPodModelID;

        /*Lingo:0x00,Cmd:0x0E,The iPod model number as a null-terminated
        UTF-8 character array.*/
        String wsIpodModelNumber;

        /*Lingo:0x00,Cmd:0x10,The lingo for which verison information
        is being returned*/
        byte bLingoRetVer;

        /*Lingo:0x00,Cmd:0x10,protocol version for the given lingo*/
        byte bLingoVerMajor;

        byte bLingoVerMinor;

        /*Lingo:0x00,Cmd:0x4B*/
        byte bLingoRetOptionsInfo;

        /*Lingo:0x00,Cmd:0x4F,EventNotification*/
        byte[] bEventNotificationMask = new byte[8];

        /*Lingo:0x00,Cmd:0xF2,Rohm Version*/
        String wsRohmVer;

        /*Lingo:0x04,Cmd:0x000D,Track info type*/
        byte bTrackInfoType;

        /*Lingo:0x04,Cmd:0x000D,Track Capability bits*/
        int iTrackCapaBits;

        /*Lingo:0x04,Cmd:0x000D,Total track length(ms)*/
        long uTrackLen;

        /*Lingo:0x04,Cmd:0x000D,Chapter count*/
        int uChapterCount;

        /*Lingo:0x04,Cmd:0x000D,TrackReleaseData*/
        SP_IPOD_RELEASE_DATE sTrackReleaseDate = new SP_IPOD_RELEASE_DATE();

        /*Lingo:0x04,Cmd:0x000D,TrackGenre*/
        String wsTrackGenre;

        /*Lingo:0x04,Cmd:0x000D,formatID
        Lingo:0x04,Cmd:0x0011,formatID*/
        int uFormatID;

        /*Lingo:0x04,Cmd:0x000D,images count*/
        int uImgCount;

        /*Lingo:0x04,Cmd:0x000F,ArtworkFormats*/
        ArrayList<iPodArtWorkFormat> vArtWorkFormats =
            new ArrayList<iPodDataDef.iPodArtWorkFormat>();

        /*Lingo:0x04,Cmd:0x11,Desriptor telegram index*/
        int uDesrTelegramIndex;

        /*Lingo:0x04,Cmd:0x11,Image width in pixel*/
        int uImgWidth;

        /*Lingo:0x04,Cmd:0x11,Image height in pixel*/
        int uImgHeight;

        /*Lingo:0x04,Cmd:0x11,Inset rectangle,top-left point*/
        Point pointTopLeft = new Point();

        /*Lingo:0x04,Cmd:0x11,Inset rectangle,bottom-right point*/
        Point pointBottomRight = new Point();

        /*Lingo:0x04,Cmd:0x11,Row size*/
        long uRowSize;

        /*Lingo:0x04,Cmd:0x11,Image pixel data*/
        byte[] pbImgData;

        /*Lingo:0x04,Cmd:0x11,Image pixel data length*/
        int uImgDataLen;

        /*Lingo:0x04,Cmd:0x0019,Database record count*/
        long iRecordCount;

        /*Lingo:0x04,Cmd:0x001B,Database record category index*/
        int iRecordCategoryIndex;

        /*Lingo:0x04,Cmd:0x001B,Database record*/
        String wsRecord;

        /*Lingo:0x04,Cmd:0x001D,Track length in milliseconds*/
        long iTrackLen;

        /*Lingo:0x04,Cmd:0x001D,Track position in milliseconds*/
        long iTrackPos;

        /*Lingo:0x04,Cmd:0x001D,Player state*/
        byte bPlayerState;

        /*Lingo:0x04,Cmd:0x001F,Playback track index*/
        long iTrackIndex;

        /*Lingo:0x04,Cmd:0x0021,Track title*/
        String wsTrackTitle;

        /*Lingo:0x04,Cmd:0x0023,Artist name*/
        String wsArtistName;

        /*Lingo:0x04,Cmd:0x0025,Album name*/
        String wsAlbumName;

        /*Lingo:0x04,Cmd:0x0027,New play status*/
        byte bNewPlayStatus;

        /*Lingo:0x04,Cmd:0x0027,Parameters*/
        byte[] pbParams = new byte[8];

        /*Lingo:0x04,Cmd:0x002B,time offset from track start in ms*/
        long uTimeOffset;

        /*Lingo:0x04,Cmd:0x002D,Shuffle mode*/
        byte bShuffleMode;

        /*Lingo:0x04,Cmd:0x0030,Repeat state*/
        byte bRepeatState;

        /*Lingo:0x04,Cmd:0x0034,Logo Formats
        Lingo:0x04,Cmd:0x003A,Logo Formats*/
        ArrayList<iPodLogoFormat> vLogoFormats =
            new ArrayList<iPodDataDef.iPodLogoFormat>();

        /*Lingo:0x04,Cmd:0x0036,Number of tracks playing*/
        long iTrackPlayingNum;

        /*Lingo:0x0A,Cmd:0x0004,New sample rate*/
        long uNewSampleRate;

        /*Lingo:0x0A,Cmd:0x0004,New Sound Check value*/
        long uNewSoundCheckValue;

        /*Lingo:0x0A,Cmd:0x0004,New track volume adjustment*/
        long uTrackVolumeAdjustment;

        public RX_PACKET_PARAM(int nlen)
        {
            pbImgData = new byte[nlen];
        }
    }

    // 发送包参数
    public class TX_PACKET_PARAM
    {
        // 是否是立即发送
        boolean bIsQuicklySend;

        /*Lingo:0x00,Cmd:0x0F,The lingo for which 
        to request version information*/
        byte bLingoReqVerInfo;

        /*Lingo:0x00,Cmd:0x2B,Preference class ID*/
        byte bPreferenceClassId;

        /*Lingo:0x00,Cmd:0x2B,Preference setting ID*/
        byte bPreferenceSettingId;

        /*Lingo:0x00,Cmd:0x2B,Restore on exit*/
        byte bRestoreOnExit;

        /*Lingo:0x00,Cmd:0x4B,Audio-specific button values*/
        byte bLingoOptionsInfo;

        /*Lingo:0x02,Cmd:0x00,Context button values*/
        long uContextBtnState;

        /*Lingo:0x02,Cmd:0x02,Image-specific button values*/
        long uImgSpeBtnState;

        /*Lingo:0x02,Cmd:0x03,video-specific button values*/
        int uVideoSpeBtnState;

        /*Lingo:0x02,Cmd:0x04,Audio-specific button values*/
        int uAudioSpeBtnState;

        /*Lingo:0x04,Cmd:0x0010,formatID
        Lingo:0x04,Cmd:0x002A,formatID*/
        int uFormatID;

        /*Lingo:0x04,Cmd:0x0010,time offset from track start,in ms*/
        long uTimeOffset;

        /*Lingo:0x04,Cmd:0x000C:track info type*/
        byte bTrackInfoType;

        /*Lingo:0x04,Cmd:0x0017,Database category type
        Lingo:0x04,Cmd:0x0018,Database category type
        Lingo:0x04,Cmd:0x001A,Database category type*/
        byte bDbCateType;

        /*Lingo:0x04,Cmd:0x0017,Database record index*/
        long iDbRecordIndex;

        /*Lingo:0x04,Cmd:0x001A,Database record start index*/
        long iDbRecordStartIndex;

        /*Lingo:0x04,Cmd:0x001A,Database record read count*/
        long iDbRecordReadCount;

        /*Lingo:0x04,Cmd:0x000C:track index
        Lingo:0x04,Cmd:0x0020,Playback track index
        Lingo:0x04,Cmd:0x0022,Playback track index
        Lingo:0x04,Cmd:0x0024,Playback track index
        Lingo:0x04,Cmd:0x0028,Selection track index
        Lingo:0x04,Cmd:0x002A,track index
        Lingo:0x04,Cmd:0x0010,track index*/
        long iTrackIndex;

        /*Lingo:0x04,Cmd:0x026,是否支持1个字节的播放状态*/
        byte bPlayStateOneByte;

        /*Lingo:0x04,Cmd:0x0026,是否能够通告,Enable/disable notifications*/
        byte bEnableNotify;

        /*Lingo:0x04,Cmd:0x0026,Notification event mask*/
        long iNotifyEventMask;

        /*Lingo:0x04,Cmd:0x0029,Play control command code*/
        byte bPlayControlCmdCode;

        /*Lingo:0x04,Cmd:0x002A,artworkIndex*/
        int uArtworkIndex;

        /*Lingo:0x04,Cmd:0x002A,artworkCount*/
        int uArtworkCount;

        /*Lingo:0x04,Cmd:0x002E,New Shuffle Mode*/
        byte bNewShuffleMode;

        /*Lingo:0x04,Cmd:0x0031,New Repeat Status*/
        byte bNewRepeatStatus;

        /*Lingo:0x04,Cmd:0x0032,Description telegram index*/
        int uDescriptionTelegramIndex;

        /*Lingo:0x04,Cmd:0x0032,Display pixel format*/
        byte bDispPixelFormatCode;

        /*Lingo:0x04,Cmd:0x0032,Image width in pixels*/
        int uImgWidth;

        /*Lingo:0x04,Cmd:0x0032,Image height in pixels*/
        int uImgHeight;

        /*Lingo:0x04,Cmd:0x0032,Row size(stride) in bytes*/
        long uRowSize;

        /*Lingo:0x04,Cmd:0x0032,Display image pixel data*/
        byte[] pbImgData;

        /*Lingo:0x04,Cmd:0x0032,Display image pixel data len*/
        int uImgDataLen;

        /*Lingo:0x04,Cmd:0x0037,SetCurrentPlayingTrack*/
        long uSelectIndex;

        /*Lingo:0x0A,Cmd:0x0000,Command status*/
        byte bCmdStatus;

        /*Lingo:0x0A,Cmd:0x0000,The ID of the command being acknowledged*/
        long bAckId;
    }

    /*Lingo:0x04,Cmd:0x000F,ArtworkFormats*/
    public class iPodArtWorkFormat
    {
        int formatID;

        byte pixelFormat;

        int width;

        int height;
    }

    public class STRUCT_ARTWORK
    {
        byte pixelFormat;

        int widthMax;

        int heightMax;

        byte[] image_data = null;// 图像数据

        int pImage = 0;// 图像数据偏移指针

        int width;// 图像宽度

        int height;// 图像高度

        long row_byte;// 每行所需要的字节数

        int telegram_index;// Descriptor telegram index

        int total_byte;// 图像总的字节数

        long left_len;// 图像剩下未传的字节数

        long this_write_len;// 图像这次需要传的字节数

        boolean bIsOmitData = false;// 是否丢失数据包
    }

    /*Lingo:0x04,Cmd:0x0034,0x003A,LogoFormats*/
    public class iPodLogoFormat
    {
        byte dispPixelFormat;

        int maxWidth;

        int maxHeight;
    }

    /*Lingo:0x04,Cmd:0x0032,logo data*/
    public class iPodPixelForamt
    {
        public static final byte IPF_MONOCHRONE = 0x01;//

        public static final byte IPF_RGB565_LITTLE_ENDIAN = 0x02;//

        public static final byte IPF_RGB565_BIG_ENDIAN = 0x03;
    }

    /*Lingo:0x02,Cmd:0x02,Image-specific button values*/
    public class IPOD_IMG_SPECIFIC_BTN_BITMASK
    {
        public static final int IISBB_PLAY_PAUSE = 0x00000001;

        public static final int IISBB_NEXT_IMG = 0x00000002;

        public static final int IISBB_PREV_IMG = 0x00000004;

        public static final int IISBB_STOP = 0x00000008;

        public static final int IISBB_PLAY_RESUME = 0x00000010;

        public static final int IISBB_PAUSE = 0x00000020;

        public static final int IISBB_SHUFFLE_ADVANCE = 0x00000040;

        public static final int IISBB_REPEAT_ADVANCE = 0x00000080;
    }

    /*Lingo:0x02,Cmd:0x00,Context button values*/
    public class IPOD_CONTEXT_BTN_BITMASK
    {
        public static final int ICBB_BTN_UP = 0x00000000;

        public static final int ICBB_PLAY_PAUSE = 0x00000001;

        public static final int ICBB_VOL_UP = 0x00000002;

        public static final int ICBB_VOL_DOWN = 0x00000004;

        public static final int ICBB_NEXT_TRACK = 0x00000008;

        public static final int ICBB_PREV_TRACK = 0x00000010;

        public static final int ICBB_NEXT_ALBUM = 0x00000020;

        public static final int ICBB_PREV_ALBUM = 0x00000040;

        public static final int ICBB_STOP = 0x00000080;

        public static final int ICBB_PLAY_RESUME = 0x00000100;

        public static final int ICBB_PAUSE = 0x00000200;

        public static final int ICBB_MUTE_TOGGLE = 0x00000400;

        public static final int ICBB_NEXT_CHAPTER = 0x00000800;

        public static final int ICBB_PREV_CHAPTER = 0x00001000;

        public static final int ICBB_NEXT_PLAYLIST = 0x00002000;

        public static final int ICBB_PREV_PLAYLIST = 0x00004000;

        public static final int ICBB_SHUFFLE_SETTING_ADVANCE = 0x00008000;

        public static final int ICBB_REPEAT_SETTING_ADVANCE = 0x00010000;

        public static final int ICBB_POWER_ON = 0x00020000;

        public static final int ICBB_POWER_OFF = 0x00040000;

        public static final int ICBB_BACKLIGHT_FOR_30S = 0x00080000;

        public static final int ICBB_BEGIN_FF = 0x00100000;

        public static final int ICBB_BEGIN_REW = 0x00200000;

        public static final int ICBB_MENU = 0x00400000;

        public static final int ICBB_SELECT = 0x00800000;

        public static final int ICBB_UP_ARROW = 0x01000000;

        public static final int ICBB_DOWN_ARROW = 0x02000000;

        public static final int ICBB_BACKLIGHT_OFF = 0x04000000;
    }

    /*Lingo:0x02,Cmd:0x03,Video-specific button values*/
    public class IPOD_VIDEO_SPECIFIC_BTN_BITMASK
    {
        public static final int IVSBB_PLAY_PAUSE = 0x00000001;

        public static final int IVSBB_NEXT_VIDEO = 0x00000002;

        public static final int IVSBB_PREV_VIDEO = 0x00000004;

        public static final int IVSBB_STOP = 0x00000008;

        public static final int IVSBB_PLAY_RESUME = 0x00000010;

        public static final int IVSBB_PAUSE = 0x00000020;

        public static final int IVSBB_BEGIN_FF = 0x00000040;

        public static final int IVSBB_BEGIN_REW = 0x00000080;

        public static final int IVSBB_NEXT_CHAPTER = 0x00000100;

        public static final int IVSBB_PREV_CHAPTER = 0x00000200;

        public static final int IVSBB_NEXT_FRAME = 0x00000400;

        public static final int IVSBB_PREV_FRAME = 0x00000800;
    }

    /*Lingo:0x02,Cmd:0x04,Audio-specific button values*/
    public class IPOD_AUDIO_SPECIFIC_BTN_BITMASK
    {
        public static final int IASBB_PLAY_PAUSE = 0x00000001;

        public static final int IASBB_VOL_UP = 0x00000002;

        public static final int IASBB_VOL_DOWN = 0x00000004;

        public static final int IASBB_NEXT_TRACK = 0x00000008;

        public static final int IASBB_PREV_TRACK = 0x00000010;

        public static final int IASBB_NEXT_ALBUM = 0x00000020;

        public static final int IASBB_PREV_ALBUM = 0x00000040;

        public static final int IASBB_STOP = 0x00000080;

        public static final int IASBB_PLAY_RESUME = 0x00000100;

        public static final int IASBB_PAUSE = 0x00000200;

        public static final int IASBB_MUTE_TOGGLE = 0x00000400;

        public static final int IASBB_NEXT_CHAPTER = 0x00000800;

        public static final int IASBB_PREV_CHAPTER = 0x00001000;

        public static final int IASBB_NEXT_PLAYLIST = 0x00002000;

        public static final int IASBB_PREV_PLAYLIST = 0x00004000;

        public static final int IASBB_SHUFFLE_SETTING_ADVANCE = 0x00008000;

        public static final int IASBB_REPEAT_SETTING_ADVANCE = 0x00010000;

        public static final int IASBB_BEGIN_FF = 0x00020000;

        public static final int IASBB_BEGIN_REW = 0x00040000;

        public static final int IASBB_RECORD = 0x00080000;
    }

    /*Lingo:0x04,Cmd:0x0027,play state change notification*/
    public class PLAY_STATUS_CHANGE_NOTIFICATION
    {
        public final static byte PLAYBACK_STOPPED = 0x00;

        public final static byte TRACK_INDEX_ = 0x01;

        public final static byte PLAYBACK_FFW_SEEK_STOP = 0x02;

        public final static byte PLAYBACK_REW_SEEK_STOP = 0x03;

        public final static byte TRACK_TIME_OFFSET_MS_ = 0x04;

        public final static byte CHAPTER_INDEX_ = 0x05;

        public final static byte PLAYBACK_STATUS_EXTERNED = 0X06;

        public final static byte TRACK_TIME_OFFSET_SEC_ = 0x07;

        public final static byte CHAPTER_TIME_OFFSET_MS_ = 0x08;

        public final static byte CHAPTER_TIME_OFFSET_SEC_ = 0x09;

        public final static byte TRACK_UNIQUE_IDENTIFIER_ = 0x0A;

        public final static byte TRACK_PLAYBACK_MODE = 0x0B;

        public final static byte TRACK_LYRICS_READY_ = 0x0C;
    }

    /*Lingo:0x04,Cmd:0x0029,Play control command codes*/
    public class IPOD_PLAY_CONTROL_CMD_CODES
    {
        public final static byte PARAM_Unknown = 0x0;

        public final static byte PARAM_PLAY_PAUSE = 0x1;

        public final static byte PARAM_STOP = 0x02;

        public final static byte PARAM_NEXT_TRACK = 0x03;

        public final static byte PARAM_PREV_TRACK = 0x04;

        public final static byte PARAM_START_FF = 0x05;

        public final static byte PARAM_START_REW = 0x06;

        public final static byte PARAM_END_FF_REW = 0x07;

        public final static byte PARAM_NEXT_CHAPTER = 0x08;

        public final static byte PARAM_PREV_CHAPTER = 0x09;

        public final static byte PARAM_PLAY = 0x0A;

        public final static byte PARAM_PAUSE = 0x0B;
    }

    /*Lingo:0x04,Cmd:0x000D,track info types*/
    public class TRACK_INFO_TYPES
    {
        public final static byte TRACK_CAPABILITIES_AND_INFOMATION = 0x00;// A
                                                                          // 10-byte
                                                                          // data

        public final static byte PODCAST_NAME = 0x01;// UTF-8 string

        public final static byte TRACK_RELEASE_DATE = 0x02;// An 8-byte data

        public final static byte TRACK_DESCRIPTION = 0x03;// UTF-8 string

        public final static byte TRACK_SONG_LYRICS = 0x04;// UTF-8 string

        public final static byte TRACK_GENRE = 0x05;// UTF-8 string

        public final static byte TRACK_COMPOSER = 0x06;// UTF-8 string

        public final static byte TRACK_ARTWORK_COUNT = 0x07;// Artwork count
                                                            // data
    }

    /*Lingo:0x04,Cmd:0x000D,Track Capabilties and Information encoding*/
    public class TRACK_CAPABLITIES_BITS
    {
        public final static int TRACK_IS_AUDIOBOOK = (1);

        public final static int TRACK_HAS_CHAPTERS = (1 << 1);

        public final static int TRACK_HAS_ALBUM_ARTWORK = (1 << 2);

        public final static int TRACK_HAS_SONG_LYRICS = (1 << 3);

        public final static int TRACK_HAS_A_PODCAST_EPISODE = (1 << 4);

        public final static int TRACK_HAS_RELEASE_DATE = (1 << 5);

        public final static int TRACK_HAS_DESCRIPTION = (1 << 6);

        public final static int TRACK_CONTAINS_VIDEO = (1 << 7);

        public final static int TRACK_IS_PLAY_AS_A_VIDEO = (1 << 8);
    }

    /*Lingo:0x04,Cmd:0x0018,Database category types*/
    public class IPOD_CATEGORY_TYPES
    {
        public final static byte CATEGORY_NOTHING = 0;

        public final static byte CATEGORY_PLAYLIST = 0x01;

        public final static byte CATEGORY_ARTIST = 0x02;

        public final static byte CATEGORY_ALBUM = 0x03;

        public final static byte CATEGORY_GENRE = 0x04;

        public final static byte CATEGORY_TRACK = 0x05;

        public final static byte CATEGORY_COMPOSER = 0x06;

        public final static byte CATEGORY_AUDIOBOOK = 0x07;

        public final static byte CATEGORY_PODCAST = 0x08;

        public final static byte CATEGORY_NESTED_PLAYLIST = 0x09;// Nested
                                                                 // playlist

        // 只在代码中用，不在协议中发
        public final static byte CATEGORY_Movies = 0x20;

        public final static byte CATEGORY_MusicVideos = 0x21;

        public final static byte CATEGORY_TVShows = 0x22;

        public final static byte CATEGORY_VideoPodcasts = 0x23;

        public final static byte CATEGORY_Rentals = 0x24;
    }

    /*Lingo:0x04,Cmd:0x0026,Notification event mask*/
    public class IPOD_PLAY_STATE_MASK_BIT
    {
        public final static short BASIC_PLAY_STATE_CHANGES = 0x01;

        public final static short EXTERN_PLAY_STATE_CHANGES = 0x02;

        public final static short TRACK_INDEX = 0x04;

        public final static short TRACK_TIME_OFFSET_MS = 0x08;

        public final static short TRACK_TIME_OFFSET_SEC = 0x10;

        public final static short CHAPTER_INDEX = 0x20;

        public final static short CHAPTER_TIME_OFFSET_MS = 0x40;

        public final static short CHAPTER_TIME_OFFSET_SEC = 0x80;

        public final static short TRACK_UNIQUE_IDENTIFIER = 0x100;

        public final static short TRACK_MEDIA_TYPE = 0x200;

        public final static short TRACK_LYRICS_READY = 0x400;

        public final static short TRACK_CAPABILITIES_CHANGED = 0x800;
    }

    public class IPOD_MemBlob
    {
        private static final int DEFAULT_SIZE = 10;

        int m_nDataLength; // 当前数据的长度

        byte[] m_pData; // 数据指针

        int m_nDataOffset; // 用于读取数据

        public IPOD_MemBlob()
        {
            this(DEFAULT_SIZE);
        }

        public IPOD_MemBlob(int nArrayLength)
        {
            this(null, 0, nArrayLength);
        }

        public IPOD_MemBlob(byte[] data, int nOffset, int nLength)
        {
            m_nDataLength = 0;
            m_nDataOffset = 0;
            m_pData = new byte[nLength];
            if (null == data)
            {
            }
            else
            {
                if (data.length < nOffset + nLength)
                {
                    throw new IllegalArgumentException();
                }
                for (int i = 0; i < nLength; ++i)
                {
                    m_pData[m_nDataLength + i] = data[nOffset + i];
                }
                m_nDataLength += nLength;
            }
        }

        // 根据增长的长度重新申请内存块
        private void AllocArray(int length)
        {
            if (length > m_pData.length)
            {
                byte[] newElements = new byte[length * 2];
                System.arraycopy(m_pData, 0, newElements, 0, m_pData.length);
                m_pData = newElements;
            }
        }

        public void AppendByte(byte data)
        {
            AllocArray(m_nDataLength + 1);
            m_pData[m_nDataLength] = data;
            m_nDataLength += 1;
        }

        public void AppendByteArray(byte[] data, int startIndex, int nLength)
        {
            if (nLength == 0)
            {
                return;
            }
            if (data.length < startIndex + nLength)
            {
                throw new IllegalArgumentException();
            }
            AllocArray(m_nDataLength + nLength);
            for (int i = 0; i < nLength; ++i)
            {
                m_pData[m_nDataLength + i] = data[startIndex + i];
            }
            m_nDataLength += nLength;
        }

        public void AppendShort(int data)
        {
            AllocArray(m_nDataLength + 2);
            m_pData[m_nDataLength] = (byte)((data & 0xff00) >> 8);
            m_pData[m_nDataLength + 1] = (byte)(data & 0xff);
            m_nDataLength += 2;
        }

        public void AppendInt(long data)
        {
            AllocArray(m_nDataLength + 4);
            m_pData[m_nDataLength] = (byte)((data & 0xff000000L) >> 24);
            m_pData[m_nDataLength + 1] = (byte)((data & 0xff0000L) >> 16);
            m_pData[m_nDataLength + 2] = (byte)((data & 0xff00L) >> 8);
            m_pData[m_nDataLength + 3] = (byte)(data & 0xffL);
            m_nDataLength += 4;
        }

        public void AppendString(String data)
        {
            byte[] tempByte = data.getBytes();
            AllocArray(m_nDataLength + tempByte.length);
            for (int i = 0; i < tempByte.length; ++i)
            {
                m_pData[m_nDataLength + i] = tempByte[i];
            }
            m_pData[m_nDataLength + tempByte.length] = 0;
            m_nDataLength += tempByte.length + 1;
        }

        public void AppendMemBlob(IPOD_MemBlob data)
        {
            AllocArray(m_nDataLength + data.GetDataLength());
            byte[] pData = data.GetData();
            for (int i = 0; i < data.GetDataLength(); ++i)
            {
                m_pData[m_nDataLength + i] = pData[i];
            }
            m_nDataLength += data.GetDataLength();
        }

        public void SetDataPosition(int offSet)
        {
            if (offSet >= m_pData.length)
            {
                m_nDataOffset = 0;
            }
            else
            {
                m_nDataOffset = offSet;
            }
        }

        public byte ReadByte()
        {
            byte data = m_pData[m_nDataOffset++];
            return data;
        }

        public byte[] ReadByteArray(int nLength)
        {
            if (m_nDataOffset + nLength >= m_nDataLength)
            {
                throw new IllegalArgumentException();
            }
            byte[] data = new byte[nLength];
            for (int i = 0; i < nLength; ++i)
            {
                data[i] = m_pData[m_nDataOffset++];
            }
            return data;
        }

        public int ReadShort()
        {
            if (m_nDataOffset + 2 > m_nDataLength)
            {
                // xuxiuchen ipad3不能识别设备是否连接，扑货异常后调JNI断开设备连接
                iPodJNI miPodJni = new iPodJNI();
                miPodJni.ipod_ioctl(iPodJNI.SET_IPOD_DISCONNECT, null);
                // throw new IllegalArgumentException();
            }
            int data =
                ((m_pData[m_nDataOffset++] << 8 & 0xff00) | (m_pData[m_nDataOffset++] & 0xff));
            return data;
        }

        public long ReadInt()
        {
            if (m_nDataOffset + 4 > m_nDataLength)
            {
                throw new IllegalArgumentException();
            }
            long data =
                ((m_pData[m_nDataOffset++] << 24 & 0xff000000L)
                    | (m_pData[m_nDataOffset++] << 16 & 0xff0000L)
                    | (m_pData[m_nDataOffset++] << 8 & 0xff00L) | (m_pData[m_nDataOffset++] & 0xffL));
            return data;
        }

        public String ReadString()
        {
            int nLength = 0;
            for (int i = 0; i < m_nDataLength - m_nDataOffset; ++i)
            {
                if (m_pData[m_nDataOffset + i] == 0)
                {
                    break;
                }
                else
                {
                    nLength++;
                }
            }
            String data = new String(m_pData, m_nDataOffset, nLength);
            m_nDataOffset += (nLength + 1);
            return data;
        }

        public byte[] GetData()
        {
            return m_pData;
        }

        public int GetDataLength()
        {
            return m_nDataLength;
        }

        public int GetDataOffset()
        {
            return m_nDataOffset;
        }

        public void Clear()
        {
            for (int i = 0; i < m_pData.length; i++)
            {
                m_pData[i] = 0;
            }
            m_nDataLength = 0;
        }
    }

    public class IPOD_RET_RESULT_VALUE
    {
        public Object value = null;

        public int result = 0;
    }
}
