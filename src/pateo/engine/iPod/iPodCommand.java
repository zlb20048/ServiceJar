package pateo.engine.iPod;

import pateo.com.global.TypeConvert;
import pateo.engine.iPod.iPodCommandInterface.iPodWriteInterface;
import pateo.engine.iPod.iPodDataDef.IPOD_LINGO_ID;
import pateo.engine.iPod.iPodDataDef.IPOD_MemBlob;
import pateo.engine.iPod.iPodDataDef.TX_PACKET;
import pateo.engine.iPod.iPodDataDef.TX_PACKET_CODE;
import pateo.engine.iPod.iPodDataDef.TX_PACKET_PARAM;

public class iPodCommand
{

    private static final String TAG = "iPodCommand";

    private static final int TX_DATA_LEN_MAX = 500;// 发送数据最大长度

    private static final int TX_PACKET_LEN_MAX = (TX_DATA_LEN_MAX + 11);// 发送包最大长度

    private iPodWriteInterface mWriteSerial = null;

    private int mTxBufLen;// 发送数据长度

    private TX_PACKET mTxPacket = new TX_PACKET(TX_DATA_LEN_MAX);// 发送包

    private byte[] mTxBuf = new byte[TX_PACKET_LEN_MAX];

    private iPodDataLayer mDataLayer = null;

    private IPOD_MemBlob mProtocolData = new IPOD_MemBlob();

    public iPodCommand(iPodWriteInterface serial)
    {
        mWriteSerial = serial;
        mDataLayer = iPodDataLayer.GetInstant();
    }

    public void SendCommand(int tpCode, TX_PACKET_PARAM rxParam)
    {
        // TODO Auto-generated method stub
        // mWriteSerial.WriteCommand(buf, chBufLen);
        mProtocolData.Clear();
        switch (tpCode)
        {
            case TX_PACKET_CODE.T_RequestRemoteUIMode:
                // TxRequestRemoteUIMode,//Lingo:0x00,Cmd:0x03
                break;
            case TX_PACKET_CODE.T_EnterRemoteUIMode:
                // TxEnterRemoteUIMode,//Lingo:0x00,Cmd:0x05
                break;
            case TX_PACKET_CODE.T_ExitRemoteUIMode:
                // TxExitRemoteUIMode,//Lingo:0x00,Cmd:0x06
                break;
            case TX_PACKET_CODE.T_RequestiPodName:
                // TxRequestiPodName,//Lingo:0x00,Cmd:0x07
                break;
            case TX_PACKET_CODE.T_RequestiPodSerialNum:
                // TxRequestiPodSerialNum,//Lingo:0x00,Cmd:0x0B
                break;
            case TX_PACKET_CODE.T_RequestiPodModelNum:
                // TxRequestiPodModelNum,//Lingo:0x00,Cmd:0x0D
                break;
            case TX_PACKET_CODE.T_RequestLingoProtocolVersion:
                // TxRequestLingoProtocolVersion,//Lingo:0x00,Cmd:0x0F
                // The lingo for which to request version information
                mProtocolData.AppendByte(rxParam.bLingoReqVerInfo);
                break;
            case TX_PACKET_CODE.T_SetiPodPreferences:
                // TxSetiPodPreferences,//Lingo:0x00,Cmd:0x2B
                // Preference class ID
                mProtocolData.AppendByte(rxParam.bPreferenceClassId);
                // Preference setting ID
                mProtocolData.AppendByte(rxParam.bPreferenceSettingId);
                // Restore on exit
                mProtocolData.AppendByte(rxParam.bRestoreOnExit);
                break;
            case TX_PACKET_CODE.T_SetEventNotification:
                // TxSetEventNotification,//Lingo:0x00,Cmd:0x49
                mTxPacket.pbData[0] = 0x00;
                mTxPacket.pbData[1] = 0x00;
                mTxPacket.pbData[2] = 0x00;
                mTxPacket.pbData[3] = 0x00;
                mTxPacket.pbData[4] = 0x00;
                mTxPacket.pbData[5] = 0x1A;
                mTxPacket.pbData[6] = (byte)0xAE;
                mTxPacket.pbData[7] = 0x3C | 0x04;
                mTxPacket.uDataLen = 8;
                break;
            case TX_PACKET_CODE.T_GetiPodOptionsForLingo:
                // TxGetiPodOptionsForLingo,//Lingo:0x00,Cmd:0x4B
                // The lingo for which to request version information
                mProtocolData.AppendByte(rxParam.bLingoOptionsInfo);
                break;
            case TX_PACKET_CODE.T_GetSupportedEventNotification:
                // TxGetSupportedEventNotification,//Lingo:0x00,Cmd:0x4F
                break;
            case TX_PACKET_CODE.T_ContextButtonStatus:
                // TxContextButtonStatus,//Lingo:0x02,Cmd:0x00
                // mTxPacket.pbData[0] = (byte)(rxParam.uContextBtnState &
                // 0x0ff);
                // mTxPacket.pbData[1] = (byte)(rxParam.uContextBtnState>>8 &
                // 0x0ff);
                // mTxPacket.pbData[2] = (byte)(rxParam.uContextBtnState>>16 &
                // 0x0ff);
                // mTxPacket.pbData[3] = (byte)(rxParam.uContextBtnState>>24 &
                // 0x0ff);
                // mTxPacket.uDataLen = 4;
                break;
            case TX_PACKET_CODE.T_ImageButtonStatus:
                // TxImageButtonStatus,//Lingo:0x02,Cmd:0x02
                if (rxParam.uImgSpeBtnState >= 0x01000000)
                {
                    mTxPacket.pbData[0] =
                        (byte)(rxParam.uImgSpeBtnState & 0x0ff);
                    mTxPacket.pbData[1] =
                        (byte)(rxParam.uImgSpeBtnState >> 8 & 0x0ff);
                    mTxPacket.pbData[2] =
                        (byte)(rxParam.uImgSpeBtnState >> 16 & 0x0ff);
                    mTxPacket.pbData[3] =
                        (byte)(rxParam.uImgSpeBtnState >> 24 & 0x0ff);
                    mTxPacket.uDataLen = 4;
                }
                else if (rxParam.uImgSpeBtnState >= 0x00010000)
                {
                    mTxPacket.pbData[0] =
                        (byte)(rxParam.uImgSpeBtnState & 0x0ff);
                    mTxPacket.pbData[1] =
                        (byte)(rxParam.uImgSpeBtnState >> 8 & 0x0ff);
                    mTxPacket.pbData[2] =
                        (byte)(rxParam.uImgSpeBtnState >> 16 & 0x0ff);
                    mTxPacket.uDataLen = 3;
                }
                else if (rxParam.uImgSpeBtnState >= 0x00000100)
                {
                    mTxPacket.pbData[0] =
                        (byte)(rxParam.uImgSpeBtnState & 0x0ff);
                    mTxPacket.pbData[1] =
                        (byte)(rxParam.uImgSpeBtnState >> 8 & 0x0ff);
                    mTxPacket.uDataLen = 2;
                }
                else
                {
                    mTxPacket.pbData[0] =
                        (byte)(rxParam.uImgSpeBtnState & 0x0ff);
                    mTxPacket.uDataLen = 1;
                }
                break;
            case TX_PACKET_CODE.T_VideoButtonStatus:
                // TxVideoButtonStatus,//Lingo:0x02,Cmd:0x03
                if (rxParam.uVideoSpeBtnState >= 0x01000000)
                {
                    mTxPacket.pbData[0] =
                        (byte)(rxParam.uVideoSpeBtnState & 0x0ff);
                    mTxPacket.pbData[1] =
                        (byte)(rxParam.uVideoSpeBtnState >> 8 & 0x0ff);
                    mTxPacket.pbData[2] =
                        (byte)(rxParam.uVideoSpeBtnState >> 16 & 0x0ff);
                    mTxPacket.pbData[3] =
                        (byte)(rxParam.uVideoSpeBtnState >> 24 & 0x0ff);
                    mTxPacket.uDataLen = 4;
                }
                else if (rxParam.uVideoSpeBtnState >= 0x00010000)
                {
                    mTxPacket.pbData[0] =
                        (byte)(rxParam.uVideoSpeBtnState & 0x0ff);
                    mTxPacket.pbData[1] =
                        (byte)(rxParam.uVideoSpeBtnState >> 8 & 0x0ff);
                    mTxPacket.pbData[2] =
                        (byte)(rxParam.uVideoSpeBtnState >> 16 & 0x0ff);
                    mTxPacket.uDataLen = 3;
                }
                else if (rxParam.uVideoSpeBtnState >= 0x00000100)
                {
                    mTxPacket.pbData[0] =
                        (byte)(rxParam.uVideoSpeBtnState & 0x0ff);
                    mTxPacket.pbData[1] =
                        (byte)(rxParam.uVideoSpeBtnState >> 8 & 0x0ff);
                    mTxPacket.uDataLen = 2;
                }
                else
                {
                    mTxPacket.pbData[0] =
                        (byte)(rxParam.uVideoSpeBtnState & 0x0ff);
                    mTxPacket.uDataLen = 1;
                }
                break;
            case TX_PACKET_CODE.T_AudioButtonStatus:
                // TxAudioButtonStatus,//Lingo:0x02,Cmd:0x04
                mTxPacket.pbData[0] = (byte)(rxParam.uAudioSpeBtnState & 0x0ff);
                mTxPacket.pbData[1] =
                    (byte)(rxParam.uAudioSpeBtnState >> 8 & 0x0ff);
                mTxPacket.pbData[2] =
                    (byte)(rxParam.uAudioSpeBtnState >> 16 & 0x0ff);
                mTxPacket.pbData[3] =
                    (byte)(rxParam.uAudioSpeBtnState >> 24 & 0x0ff);
                mTxPacket.uDataLen = 4;
                break;
            case TX_PACKET_CODE.T_GetCurrentPlayingTrackCahpterInfo:
                // TxGetCurrentPlayingTrackCahpterInfo,//Lingo:0x04,Cmd:0x0002
                mTxPacket.uDataLen = 0;
                break;
            case TX_PACKET_CODE.T_GetIndexedPlayingTrackInfo:
                // TxGetIndexedPlayingTrackInfo,//Lingo:0x04,Cmd:0x000C

                // Track info type
                // 0x00:Track Capabilities and Information
                // 0x01:Podcast Name
                // 0x02:Track Release Date
                // 0x03:Track Description
                // 0x04:Track Song Lyrics
                // 0x05:Track Genere
                // 0x06:Track Composer
                // 0x07:Track Artwork Count
                // mTxPacket.pbData[0] = rxParam.bTrackInfoType;
                mProtocolData.AppendByte(rxParam.bTrackInfoType);
                // Track index(bits31:24)
                mProtocolData.AppendInt(rxParam.iTrackIndex);
                mProtocolData.AppendByte((byte)0x00);
                mProtocolData.AppendByte((byte)0x00);
                break;
            case TX_PACKET_CODE.T_GetArtworkFormats:
                // TxGetArtworkFormats,//Lingo:0x04,Cmd:0x000E
                break;
            case TX_PACKET_CODE.T_GetTrackArtworkData:
                // TxGetTrackArtworkData,//Lingo:0x04,Cmd:0x0010
                mTxPacket.bLingo = 0x04;
                mTxPacket.uCmd = 0x0010;
                // trackIndex(bits31:24)
                mProtocolData.AppendInt(rxParam.iTrackIndex);
                // formatID
                mProtocolData.AppendShort(rxParam.uFormatID);
                // time offset from track start,in ms
                mProtocolData.AppendInt(rxParam.uTimeOffset);
                break;
            case TX_PACKET_CODE.T_ResetDBSelection:
                // TxResetDBSelection,//Lingo:0x04,Cmd:0x0016
                break;
            case TX_PACKET_CODE.T_SelectDBRecord:
                // TxSelectDBRecord,//Lingo:0x04,Cmd:0x0017
                // Database category type
                // Category,Code,Protocol Verson
                // Reserved,0x00,N/A
                // Playlist,0x01,1.00
                // Artist,0x02,1.00
                // Album,0x03,1.00
                // Genre,0x04,1.00
                // Track,0x05,1.00
                // Composer,0x06,1.00
                // Audiobook,0x07,1.06
                // Podcast,0x08,1.08
                // Nested playlist,0x09,1.13
                // Reserved,0x0A-0xFF,N/A
                mProtocolData.AppendByte(rxParam.bDbCateType);
                // Database record index
                mProtocolData.AppendInt(rxParam.iDbRecordIndex);
                break;
            case TX_PACKET_CODE.T_GetNumberCategorizedDBRecords:
                // TxGetNumberCategorizedDBRecords,//Lingo:0x04,Cmd:0x0018
                // Database category type
                // Category,Code,Protocol Verson
                // Reserved,0x00,N/A
                // Playlist,0x01,1.00
                // Artist,0x02,1.00
                // Album,0x03,1.00
                // Genre,0x04,1.00
                // Track,0x05,1.00
                // Composer,0x06,1.00
                // Audiobook,0x07,1.06
                // Podcast,0x08,1.08
                // Nested playlist,0x09,1.13
                // Reserved,0x0A-0xFF,N/A
                mProtocolData.AppendByte(rxParam.bDbCateType);
                break;
            case TX_PACKET_CODE.T_RetrieveCategorizedDatabaseRecords:
                // TxRetrieveCategorizedDatabaseRecords,//Lingo:0x04,Cmd:0x001A
                // Database category type
                // Category,Code,Protocol Verson
                // Reserved,0x00,N/A
                // Playlist,0x01,1.00
                // Artist,0x02,1.00
                // Album,0x03,1.00
                // Genre,0x04,1.00
                // Track,0x05,1.00
                // Composer,0x06,1.00
                // Audiobook,0x07,1.06
                // Podcast,0x08,1.08
                // Nested playlist,0x09,1.13
                // Reserved,0x0A-0xFF,N/A
                mProtocolData.AppendByte(rxParam.bDbCateType);
                // Database record start index
                mProtocolData.AppendInt(rxParam.iDbRecordStartIndex);
                // Database record read count
                mProtocolData.AppendInt(rxParam.iDbRecordReadCount);
                break;
            case TX_PACKET_CODE.T_GetPlayStatus:
                // TxGetPlayStatus,//Lingo:0x04,Cmd:0x001C
                break;
            case TX_PACKET_CODE.T_GetCurrentPlayingTrackIndex:
                // TxGetCurrentPlayingTrackIndex,//Lingo:0x04,Cmd:0x001E
                break;
            case TX_PACKET_CODE.T_GetIndexedPlayingTrackTitle:
                // TxGetIndexedPlayingTrackTitle,//Lingo:0x04,Cmd:0x0020
                // Playback track index
                mProtocolData.AppendInt(rxParam.iTrackIndex);
                break;
            case TX_PACKET_CODE.T_GetIndexedPlayingTrackArtistName:
                // TxGetIndexedPlayingTrackArtistName,//Lingo:0x04,Cmd:0x0022
                // Playback track index
                mProtocolData.AppendInt(rxParam.iTrackIndex);
                break;
            case TX_PACKET_CODE.T_GetIndexedPlayingTrackAlbumName:
                // TxGetIndexedPlayingTrackAlbumName,//Lingo:0x04,Cmd:0x0024
                // Playback track index
                mProtocolData.AppendInt(rxParam.iTrackIndex);
                break;
            case TX_PACKET_CODE.T_SetPlayStatusChangeNotification:
                // TxSetPlayStatusChangeNotification,//Lingo:0x04,Cmd:0x0026
                if (rxParam.bPlayStateOneByte != 0)// 是否支持1个字节的播放状态
                {
                    // g_pProtocol->m_TxPacket.uPayLoadLen= 0x04;
                    /*Enable/disable notifications
                    0x00:Disable all status event notifications
                    0x01:Enable play status notifications for basic play state,
                    track index,track time position,FFW/REW seek stop,
                    and chapter index changes(StatusChangeNotification types
                    0x00-0x05)
                    0x02-0xFF:Reserved
                    */
                    mProtocolData.AppendByte(rxParam.bEnableNotify);
                }
                else
                {
                    // g_pProtocol->m_TxPacket.uPayLoadLen= 0x07;
                    /*
                    Notification event mask
                    bit00:Basic play state change(stop,FFW seek stop,using
                    status notification types 0x00,0x02,0x03)
                    bit01:Extended play state change(playback stop,FFW seek
                    start,REW seek start,playback started,FFW/REW seek
                    stop,or playback pause using status notification type 0x06).
                    Uses PlayControl command control codes as the play
                    status codes
                    bit02:Track index
                    bit03:Track time offset(ms)
                    bit04:Track tiem offset(sec)
                    bit05:Chapter index
                    bit06:Chapter time offset(ms)
                    bit07:Chapter time offset(sec)
                    bit08:Track unique identifier
                    bit09:Track media type(audio/video)
                    bit10:Track lyrics ready(if the track has lyrics)
                    bit11-31:Reserved
                    */
                    // Notification event mask
                    mProtocolData.AppendInt(rxParam.iNotifyEventMask);
                }
                break;
            case TX_PACKET_CODE.T_PlayCurrentSelection:
                // TxPlayCurrentSelection,//Lingo:0x04,Cmd:0x0028
                // Selection track record index
                mProtocolData.AppendInt(rxParam.iTrackIndex);
                break;
            case TX_PACKET_CODE.T_PlayControl:
                // TxPlayControl,//Lingo:0x04,Cmd:0x0029
                // Play control command codes
                mProtocolData.AppendByte(rxParam.bPlayControlCmdCode);
                break;
            case TX_PACKET_CODE.T_GetTrackArtworkTimes:
                // TxGetTrackArtworkTimes,//Lingo:0x04,Cmd:0x002A
                // trackIndex
                mProtocolData.AppendInt(rxParam.iTrackIndex);
                // formatID
                mProtocolData.AppendShort(rxParam.uFormatID);
                // artworkIndex
                mProtocolData.AppendShort(rxParam.uArtworkIndex);
                // artworkCount
                mProtocolData.AppendShort(rxParam.uArtworkCount);
                break;
            case TX_PACKET_CODE.T_GetShuffle:
                // TxGetShuffle,//Lingo:0x04,Cmd:0x002C
                break;
            case TX_PACKET_CODE.T_SetShuffle:
                // TxSetShuffle,//Lingo:0x04,Cmd:0x002E
                /*New shuffle mode
                0x00:Shuffle off
                0x01:Shuffle tracks
                0x02:Shuffle albums
                0x03-0xFF:Reserved
                */
                mProtocolData.AppendByte(rxParam.bNewShuffleMode);
                break;
            case TX_PACKET_CODE.T_GetRepeat:
                // TxGetRepeat,//Lingo:0x04,Cmd:0x002F
                break;
            case TX_PACKET_CODE.T_SetRepeat:
                // TxSetRepeat,//Lingo:0x04,Cmd:0x0031
                /*New repeat status
                0x00:Repeat off
                0x01:Repeat one track
                0x02:Repeat all tracks
                0x03-0xFF:Reserved
                */
                mProtocolData.AppendByte(rxParam.bNewRepeatStatus);
                break;
            case TX_PACKET_CODE.T_SetDisplayImage:
                // TxSetDisplayImage,//Lingo:0x04,Cmd:0x0032
                if (rxParam.uDescriptionTelegramIndex == 0x0000)
                {
                    // 发送0x0000包
                    /*Description telegram index(2 bytes)*/
                    mProtocolData.AppendShort(rxParam.uDescriptionTelegramIndex);
                    /*Display pixel format*/
                    mProtocolData.AppendByte(rxParam.bDispPixelFormatCode);
                    /*Image width in pixels(2 bytes)*/
                    mProtocolData.AppendShort(rxParam.uImgWidth);
                    /*Lingo:0x04,Cmd:0x0032,Image height in pixels*/
                    mProtocolData.AppendShort(rxParam.uImgHeight);
                    /*Lingo:0x04,Cmd:0x0032,Row size(stride) in bytes*/
                    mProtocolData.AppendInt(rxParam.uRowSize);
                    /*Lingo:0x04,Cmd:0x0032,Display image pixel data*/
                    mProtocolData.AppendByteArray(rxParam.pbImgData,
                        0,
                        rxParam.uImgDataLen);
                    // g_pProtocol->m_TxPacket.uPayLoadLen=g_pProtocol->m_TxPacket.uDataLen+3;
                }
                else
                {
                    // 发送其它包
                    /*Description telegram index(2 bytes)*/
                    mProtocolData.AppendShort(rxParam.uDescriptionTelegramIndex);
                    /*Lingo:0x04,Cmd:0x0032,Display image pixel data*/
                    mProtocolData.AppendByteArray(rxParam.pbImgData,
                        0,
                        rxParam.uImgDataLen);
                    // g_pProtocol->m_TxPacket.uPayLoadLen=g_pProtocol->m_TxPacket.uDataLen+3;
                }
                break;
            case TX_PACKET_CODE.T_GetMonoDisplayImageLimits:
                // TxGetMonoDisplayImageLimits,//Lingo:0x04,Cmd:0x0033
                break;
            case TX_PACKET_CODE.T_GetNumPlayingTracks:
                // TxGetNumPlayingTracks,//Lingo:0x04,Cmd:0x0035
                break;
            case TX_PACKET_CODE.T_SetCurrentPlayingTracks:
                // T_SetCurrentPlayingTracks,//Lingo:0x04,Cmd:0x0037
                mProtocolData.AppendInt(rxParam.uSelectIndex);
                break;
            case TX_PACKET_CODE.T_GetColorDisplayImageLimits:
                // TxGetColorDisplayImageLimits,//Lingo:0x04,Cmd:0x0039
                break;
            case TX_PACKET_CODE.T_ResetDBSelectionHierarchy:
                // TxResetDBSelectionHierarchy,//Lingo:0x04,Cmd:0x003B
                /*Hierarchy selection
                0x01:the accessory wants to navigate
                the audio hierarchy
                0x02:the accessory wants
                to navigate the video hierarchy
                */
                mProtocolData.AppendByte((byte)0x02);
                break;
            case TX_PACKET_CODE.T_DigitalAudioLingoAccAck:
                // TxDigitalAudioLingoAccAck,//Lingo:0x0A,Cmd:0x0000
                // Command status
                mProtocolData.AppendByte(rxParam.bCmdStatus);
                // The ID of the command being acknowledged
                mProtocolData.AppendByte((byte)rxParam.bAckId);
                break;
            default:
                break;
        }
        mTxPacket.uPacketCode = tpCode;
        SendQuicklyCommand(mTxPacket.uPacketCode,
            mProtocolData.GetData(),
            mProtocolData.GetDataLength(),
            false);
    }

    private boolean SendQuicklyCommand(long Code, byte[] buf, long bufLen,
        boolean bUseRxTransID)
    {
        if (bufLen > TX_DATA_LEN_MAX)
        {
            return false;
        }
        // 初始化发送数据
        // ZeroMemory(m_TxBuf,TX_PACKET_LEN_MAX);
        // 计算Lingo及Cmd
        // Lingo
        mTxPacket.bLingo = (byte)(Code >> 16 & 0x0ff);
        // Cmd
        mTxPacket.uCmd = TypeConvert.ToUnsignedShort((short)Code);
        // 拷贝PayLoad中数据部分
        TypeConvert.memcpy(mTxPacket.pbData, buf, 0, 0, (int)bufLen);
        mTxPacket.uDataLen = (int)bufLen;
        // 计算PayLoad长
        if (mTxPacket.bLingo != IPOD_LINGO_ID.EXTENDED_INTERFACE_LINGO)
        {
            mTxPacket.uPayLoadLen = mTxPacket.uDataLen + 2;
        }
        else
        {
            mTxPacket.uPayLoadLen = mTxPacket.uDataLen + 3;
        }
        if (mDataLayer.GetUseIDPS() == true)
        {// 是否使用IDPS
         // iPodLogDefines.iPodLog(TAG, "mDataLayer.GetUseIDPS() == true");
            mTxPacket.uPayLoadLen += 2;
        }
        // 初始化变量
        mTxBufLen = 0;
        // *******************开始写包****************************
        // 同步字节
        if (mTxPacket.bSync != 0x00)
        {
            mTxBuf[mTxBufLen++] = mTxPacket.bSync;// 同步字节
        }
        // 包头
        mTxBuf[mTxBufLen++] = mTxPacket.bSOP;
        // Payload长
        if (mTxPacket.uPayLoadLen < 0xFF)
        {
            // 小包
            mTxBuf[mTxBufLen++] = (byte)(mTxPacket.uPayLoadLen & 0x0ff);
        }
        else
        {
            // 大包
            mTxBuf[mTxBufLen++] = 0x00;
            mTxBuf[mTxBufLen++] = (byte)(mTxPacket.uPayLoadLen >> 8 & 0x0ff);
            mTxBuf[mTxBufLen++] = (byte)(mTxPacket.uPayLoadLen & 0x0ff);
        }

        // PayLoad Start*********************
        // Lingo ID
        mTxBuf[mTxBufLen++] = mTxPacket.bLingo;
        // Command ID
        if (mTxPacket.bLingo == IPOD_LINGO_ID.EXTENDED_INTERFACE_LINGO)
        {
            mTxBuf[mTxBufLen++] = (byte)(mTxPacket.uCmd >> 8 & 0x0ff);
            mTxBuf[mTxBufLen++] = (byte)(mTxPacket.uCmd & 0x0ff);
        }
        else
        {
            mTxBuf[mTxBufLen++] = (byte)(mTxPacket.uCmd & 0x0ff);
        }
        // TransID
        if (mDataLayer.GetUseIDPS() == true)
        {// 是否使用IDPS
         // iPodLogDefines.iPodLog(TAG, "mDataLayer.GetUseIDPS() == true");
            if (bUseRxTransID == true)
            {
                // m_TxBuf[m_TxBufLen++]=HIBYTE(mRxPacket.uTrans);
                // m_TxBuf[m_TxBufLen++]=LOBYTE(mRxPacket.uTrans);
                // RETAILMSG(1,(L">>>>iPod:bUseRxTransID=0x%4x\r\n",m_RxPacket.uTrans));
            }
            else
            {
                int transID = mDataLayer.GetTransID();
                transID++;
                // iPodLogDefines.iPodLog(TAG, "TransID:"+transID);
                mTxBuf[mTxBufLen++] = (byte)(transID >> 8 & 0x0ff);
                mTxBuf[mTxBufLen++] = (byte)(transID & 0x0ff);
                mDataLayer.SetTransID(transID);
                // RETAILMSG(1,(L">>>>iPod:TxTransID=0x%4x\r\n",m_TxPacket.uTrans));
            }
        }
        // Data
        if (mTxPacket.uDataLen > 0)
        {
            TypeConvert.memcpy(mTxBuf,
                mTxPacket.pbData,
                mTxBufLen,
                0,
                mTxPacket.uDataLen);
            mTxBufLen += mTxPacket.uDataLen;
        }
        // PayLoad End*********************
        // 校验和
        mTxPacket.bCheckSum = CalCheckParity(mTxBuf, 1, mTxBufLen - 1);
        mTxBuf[mTxBufLen++] = mTxPacket.bCheckSum;
        // *******************结束写包****************************
        // 发送
        mWriteSerial.WriteCommand(mTxBuf, mTxBufLen);
        return true;
    }

    private byte CalCheckParity(byte[] buf, int index, int bufLen)
    {
        byte check_parity = 0;

        if (bufLen > 0)
        {
            for (int i = 0; i < bufLen; i++)
            {
                check_parity += buf[i + index];
            }
            check_parity = (byte)(-check_parity);
        }

        return check_parity;
    }
}
