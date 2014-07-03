package pateo.engine.iPod;

import pateo.com.global.TypeConvert;
import pateo.com.iPod.iPodDefine.iPodLogDefines;
import pateo.engine.iPod.iPodCommandInterface.iPodCmdAnalysedInterface;
import pateo.engine.iPod.iPodDataDef.ACK_COMMAND_ERROR_CODES;
import pateo.engine.iPod.iPodDataDef.IPOD_LINGO_ID;
import pateo.engine.iPod.iPodDataDef.RX_PACKET;
import pateo.engine.iPod.iPodDataDef.RX_PACKET_CODE;
import pateo.engine.iPod.iPodDataDef.RX_PACKET_PARAM;
import pateo.engine.iPod.iPodDataDef.RX_STATUS;
import pateo.engine.iPod.iPodDataDef.TRACK_INFO_TYPES;
import pateo.engine.iPod.iPodDataDef.iPodArtWorkFormat;
import pateo.engine.iPod.iPodDataDef.iPodLogoFormat;

public class iPodResponse
{
    private final static String TAG = "iPodResponse";

    private final static int READ_MEM_BUF_MAX = 512000;// 最大的最大接收数据字节数

    private final static int RX_DATA_LEN_MAX = 1056;// 接收包Data最大长度

    private int mRxMemBufLen = 0;

    private byte[] mSycRead = new byte[0];

    private byte[] mReadSignal = new byte[0];

    private byte[] mRxMemBuf = new byte[READ_MEM_BUF_MAX];

    private RX_PACKET mRxPacket = new RX_PACKET(RX_DATA_LEN_MAX);

    private RX_PACKET_PARAM mRxPacketPar = new RX_PACKET_PARAM(RX_DATA_LEN_MAX);// 接收包参数

    private iPodCmdAnalysedInterface miPodCmdAnalysed = null;

    private iPodDataLayer mDataLayer = null;

    public iPodResponse(iPodCmdAnalysedInterface cmdAnalysedInterface)
    {
        miPodCmdAnalysed = cmdAnalysedInterface;
        mDataAnalyseThread.start();
        mDataLayer = mDataLayer.GetInstant();
    }

    private Thread mDataAnalyseThread = new Thread("iPodAnalyse")
    {

        private int iPacketLen = 0;// 该包长度

        private byte[] bReadBuf = new byte[READ_MEM_BUF_MAX + 1];// 读数据

        private int iReadLen = 0;// 读数据长度

        private static final int iBuffGuard = READ_MEM_BUF_MAX;

        private int iBuffHead = 0;

        private int iBuffTail = iBuffHead;

        private int dwEmptyLen = READ_MEM_BUF_MAX;// 空的buffer长度

        private int dwValidLen = 0;// 有效的数据长度

        private byte bCheckSum = 0;

        private byte bData = 0;

        private boolean bIsLargePackage = false;

        private RX_STATUS RxState = RX_STATUS.RX_SOP;

        private int RxPacketCode = 0;

        private byte bNeedAnalyseTransIDLen = 0;

        @Override
        public void run()
        {
            // TODO Auto-generated method stub
            while (true)
            {
                synchronized (mReadSignal)
                {
                    try
                    {
                        mReadSignal.wait();
                    }
                    catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                while (true)
                {
                    synchronized (mSycRead)
                    {
                        // iPodLogDefines.iPodLog(TAG, "mRxMemBufLen = "
                        // + mRxMemBufLen + " dwEmptyLen = " + dwEmptyLen);
                        if (mRxMemBufLen > 0 && mRxMemBufLen < dwEmptyLen)
                        {
                            // 拷贝到临时的buf中
                            // 进入互斥体
                            iReadLen = mRxMemBufLen;

                            if ((iBuffGuard - iBuffTail) > iReadLen)
                            {
                                TypeConvert.memcpy(bReadBuf,
                                    mRxMemBuf,
                                    iBuffTail,
                                    0,
                                    iReadLen);
                                iBuffTail += iReadLen;
                            }
                            else
                            {
                                iReadLen = iBuffGuard - iBuffTail;
                                TypeConvert.memcpy(bReadBuf,
                                    mRxMemBuf,
                                    iBuffTail,
                                    0,
                                    iReadLen);
                                iBuffTail = 0;
                                TypeConvert.memcpy(bReadBuf,
                                    mRxMemBuf,
                                    0,
                                    iReadLen,
                                    mRxMemBufLen - iReadLen);
                                iBuffTail += (mRxMemBufLen - iReadLen);
                            }

                            if (iBuffTail > iBuffGuard - 1)
                            {
                                iBuffTail = 0;
                            }

                            dwEmptyLen -= mRxMemBufLen;
                            dwValidLen += mRxMemBufLen;

                            mRxMemBufLen = 0;
                        }// 获取所有权
                        else
                        {
                            break;
                        }
                    }
                    boolean bOutOfRange = false;
                    while (!bOutOfRange && dwValidLen > 0)
                    {
                        if (RxState == RX_STATUS.RX_LINGO
                            && iPacketLen > dwValidLen)
                        {
                            break;
                        }
                        bData = bReadBuf[iBuffHead];
                        // if (RxState != RX_STATUS.RX_DATA)
                        // {
                        // iPodLogDefines.iPodLog(TAG, "RxState:" + RxState
                        // + " bData:" + bData);
                        // }

                        iBuffHead++;
                        dwValidLen--;

                        if (iBuffHead > iBuffGuard - 1)
                        {
                            iBuffHead = 0;
                        }

                        switch (RxState)
                        {
                            case RX_SOP:
                                if (bData == 0x55)
                                {
                                    mRxPacket.uPayLoadLen = 0;
                                    mRxPacket.uDataLen = 0;
                                    mRxPacket.pbData.Clear();
                                    mRxPacket.bSOP = bData;
                                    RxState = RX_STATUS.RX_PAYLOADLEN;
                                }
                                break;
                            case RX_PAYLOADLEN:
                                bCheckSum += bData;
                                if (!bIsLargePackage)
                                {
                                    if (bData != 0) // small package
                                    {
                                        iPacketLen = bData & 0xff;
                                        RxState = RX_STATUS.RX_LINGO;
                                    }
                                    else
                                    // 属于大包
                                    {
                                        iPacketLen = 0;
                                        bIsLargePackage = true;
                                    }
                                }
                                else
                                {
                                    if (iPacketLen == 0)// Rx first byte
                                    {
                                        iPacketLen = (bData & 0xff) << 8;
                                    }
                                    else
                                    // Rx second byte
                                    {
                                        iPacketLen |= bData & 0xff;
                                        RxState = RX_STATUS.RX_LINGO;
                                    }
                                }
                                if (iPacketLen > dwValidLen)
                                {
                                    bOutOfRange = true;
                                }
                                break;
                            case RX_LINGO:
                                bCheckSum += bData;
                                mRxPacket.uPayLoadLen = iPacketLen;
                                mRxPacket.bLingo = bData;
                                RxState = RX_STATUS.RX_CMD;
                                iPacketLen--;
                                break;
                            case RX_CMD:
                                bCheckSum += bData;
                                iPacketLen--;
                                if (mRxPacket.bLingo != IPOD_LINGO_ID.EXTENDED_INTERFACE_LINGO)
                                {
                                    mRxPacket.uCmd = bData;
                                    if (mDataLayer.GetUseIDPS())
                                    {
                                        if ((mRxPacket.uCmd == RX_PACKET_CODE.R_GeneralLingoACK && mRxPacket.uPayLoadLen == 0x04)
                                            || (mRxPacket.uCmd == RX_PACKET_CODE.R_ExtendInterfaceLingoACK && mRxPacket.uPayLoadLen == 0x06))// 是否使用IDPS
                                        {
                                            mDataLayer.SetUseIDPS(false);// 是否使用IDPS
                                            RxState = RX_STATUS.RX_DATA;
                                        }
                                        else
                                        {
                                            bNeedAnalyseTransIDLen = 2;// 还需分析的TransID长度
                                            RxState = RX_STATUS.RX_TRANS;
                                        }
                                    }
                                    else
                                    {
                                        RxState = RX_STATUS.RX_DATA;
                                    }
                                }
                                else
                                {
                                    if (bData == 0x00) // 根据iPod的协议，高字节的Command
                                                       // 都为0。
                                    {
                                        // Do nothing
                                    }
                                    else
                                    {
                                        mRxPacket.uCmd = bData;
                                        if (mDataLayer.GetUseIDPS())
                                        {
                                            if ((mRxPacket.uCmd == RX_PACKET_CODE.R_GeneralLingoACK && mRxPacket.uPayLoadLen == 0x04)
                                                || (mRxPacket.uCmd == RX_PACKET_CODE.R_ExtendInterfaceLingoACK && mRxPacket.uPayLoadLen == 0x06))// 是否使用IDPS
                                            {
                                                mDataLayer.SetUseIDPS(false);// 是否使用IDPS
                                                RxState = RX_STATUS.RX_DATA;
                                            }
                                            else
                                            {
                                                bNeedAnalyseTransIDLen = 2;// 还需分析的TransID长度
                                                RxState = RX_STATUS.RX_TRANS;
                                            }
                                        }
                                        else
                                        {
                                            RxState = RX_STATUS.RX_DATA;
                                        }
                                    }
                                }
                                break;
                            case RX_TRANS:
                            {
                                bCheckSum += bData;
                                iPacketLen--;
                                bNeedAnalyseTransIDLen--;// 还需分析的TransID长度
                                if (bNeedAnalyseTransIDLen == 0)// 还需分析的TransID长度
                                {
                                    // 低位
                                    mRxPacket.uTrans |= bData;
                                    RxState = RX_STATUS.RX_DATA;
                                }
                                else
                                {
                                    // 高位
                                    mRxPacket.uTrans = ((int)bData) << 8;
                                }
                            }
                                break;
                            case RX_DATA:
                                if (iPacketLen > 0)
                                {
                                    int nPos = iBuffHead - 1;
                                    // 接收命令
                                    RxPacketCode =
                                        ((int)mRxPacket.bLingo) << 16;
                                    RxPacketCode |= mRxPacket.uCmd;
                                    if (nPos + iPacketLen > iBuffGuard)
                                    {
                                        int nSize = iBuffGuard - nPos;
                                        mRxPacket.pbData.AppendByteArray(bReadBuf,
                                            nPos,
                                            nSize);
                                        if (RxPacketCode != RX_PACKET_CODE.R_RetTrackArtworkData)
                                        {
                                            for (int i = 0; i < nSize; ++i)
                                            {
                                                bCheckSum += bReadBuf[nPos + i];
                                            }
                                        }
                                        nSize = iPacketLen - nSize;
                                        mRxPacket.pbData.AppendByteArray(bReadBuf,
                                            0,
                                            nSize);
                                        if (RxPacketCode != RX_PACKET_CODE.R_RetTrackArtworkData)
                                        {
                                            for (int i = 0; i < nSize; ++i)
                                            {
                                                bCheckSum += bReadBuf[i];
                                            }
                                        }
                                        iBuffHead = nSize;
                                        RxState = RX_STATUS.RX_CHECKSUM;
                                    }
                                    else
                                    {
                                        mRxPacket.pbData.AppendByteArray(bReadBuf,
                                            nPos,
                                            iPacketLen);
                                        if (RxPacketCode != RX_PACKET_CODE.R_RetTrackArtworkData)
                                        {
                                            for (int i = 0; i < iPacketLen; ++i)
                                            {
                                                bCheckSum += bReadBuf[nPos + i];
                                            }
                                        }
                                        iBuffHead = nPos + iPacketLen;
                                        RxState = RX_STATUS.RX_CHECKSUM;
                                    }
                                    if (iBuffHead > iBuffGuard - 1)
                                    {
                                        iBuffHead = 0;
                                    }
                                    dwValidLen -= (iPacketLen - 1);
                                }
                                else
                                {
                                    bCheckSum += bData;
                                    if (bCheckSum == 0
                                        || RxPacketCode == RX_PACKET_CODE.R_RetTrackArtworkData)
                                    {
                                        // receive a message success, the
                                        // recieve next.
                                        // mRxPacket.uDataLen =
                                        // TypeConvert.min(mReadData.GetDataLength(),RX_DATA_LEN_MAX);
                                        // mRxPacket.pbData[mRxPacket.uDataLen]
                                        // = 0;
                                        RxState = RX_STATUS.RX_SOP;
                                        iPacketLen = 0;
                                        bCheckSum = 0;
                                        bIsLargePackage = false;
                                        // 接收命令
                                        RxPacketCode =
                                            ((int)mRxPacket.bLingo) << 16;
                                        RxPacketCode |= mRxPacket.uCmd;
                                        // iPodLogDefines.iPodLog(TAG,
                                        // "RX_DATA->>>>RxCommand");
                                        RxCommand(RxPacketCode);
                                    }
                                    else
                                    {
                                        // iPodLogDefines.iPodLog(TAG,
                                        // "RX_DATA->bCheckSum:"+bCheckSum);
                                        RxState = RX_STATUS.RX_CHECKSUM;
                                    }
                                }
                                break;
                            case RX_CHECKSUM:
                            {
                                bCheckSum += bData;
                                if (bCheckSum == 0
                                    || RxPacketCode == RX_PACKET_CODE.R_RetTrackArtworkData)
                                {
                                    // receive a message success!
                                    RxState = RX_STATUS.RX_SOP;

                                    iPacketLen = 0;
                                    bCheckSum = 0;
                                    bIsLargePackage = false;

                                    // iPodLogDefines.iPodLog(TAG,
                                    // "RX_CHECKSUM->>>>RxCommand");
                                    RxCommand(RxPacketCode);
                                }
                                else
                                {
                                    iPodLogDefines.iPodLog(TAG,
                                        "RX_CHECKSUM->bCheckSum:" + bCheckSum);
                                    // 数据包出错，开始接收下一包，获取的的接收BUFFER没有有效数据，继续使用。
                                    iPacketLen = 0;
                                    bCheckSum = 0;
                                    bIsLargePackage = false;
                                }
                                RxState = RX_STATUS.RX_SOP;
                            }
                                break;
                            default:
                            {
                                RxState = RX_STATUS.RX_SOP;
                            }
                                break;
                        }
                    }
                    dwEmptyLen = READ_MEM_BUF_MAX - dwValidLen;
                }
            }
        }

    };

    public void OnSerialRead(byte[] buf, int bufLen)
    {
        // 如果BUFF已不够装载新数据，则直接返回
        if (READ_MEM_BUF_MAX - mRxMemBufLen < bufLen)
        {
            return;
        }

        synchronized (mSycRead)
        {
            TypeConvert.memcpy(mRxMemBuf, buf, mRxMemBufLen, 0, bufLen);
            if (mRxMemBufLen == 0)
            {
                mRxMemBufLen += bufLen;
            }
            else
            {
                mRxMemBufLen += bufLen;
            }

            synchronized (mReadSignal)
            {
                mReadSignal.notifyAll();
            }
        }
    }

    private void RxCommand(int rpCode)
    {
        // TraceManager.LogE(TAG, "RxCommand rpCode "+rpCode);
        // iPodLogDefines.iPodLog(TAG, "RxCommand:" + rpCode);
        mRxPacket.pbData.SetDataPosition(0);
        mRxPacket.uDataLen = mRxPacket.pbData.GetDataLength();
        switch (rpCode)
        {
            case RX_PACKET_CODE.R_GeneralLingoACK:
                // RxGeneralLingoACK,//Lingo:0x00,Cmd:0x02
                /*Lingo:0x00,Cmd:0x02,Command result status*/
                mRxPacketPar.bCmdResStatus = mRxPacket.pbData.ReadByte();
                /*Lingo:0x00,Cmd:0x02,The ID of the command being acknowledged*/
                mRxPacketPar.bTxAckId = 0x000000;
                mRxPacketPar.bTxAckId |= mRxPacket.pbData.ReadByte();
                if (mRxPacketPar.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING)
                {
                    // Maximum amount of time to wait for pending response,in
                    // milliseconds
                    mRxPacketPar.uMaxWaitPendingRepTime =
                        mRxPacket.pbData.ReadInt();
                    /*TypeConvert.ByteToInt(
                                                   mRxPacket.pbData[5],
                                                   mRxPacket.pbData[4],
                                                   mRxPacket.pbData[3],
                                                   mRxPacket.pbData[2]);*/
                }
                break;
            case RX_PACKET_CODE.R_ReturnRemoteUIMode:
                // RxReturnRemoteUIMode,//Lingo:0x00,Cmd:0x04
                if (mRxPacket.pbData.ReadByte() != 0)
                {
                    // true
                    mRxPacketPar.bIsInExtendedInterfaceMode = true;
                    break;
                }
                else
                {
                    // false
                    mRxPacketPar.bIsInExtendedInterfaceMode = false;
                }
                break;
            case RX_PACKET_CODE.R_ReturniPodName:
                // RxReturniPodName,//Lingo:0x00,Cmd:0x08
                mRxPacketPar.wsiPodName = mRxPacket.pbData.ReadString();
                break;
            case RX_PACKET_CODE.R_ReturniPodSerialNum:
                // RxReturniPodSerialNum,//Lingo:0x00,Cmd:0x0C
                mRxPacketPar.wsSerialNum = mRxPacket.pbData.ReadString();
                break;
            case RX_PACKET_CODE.R_ReturniPodModelNum:
                // RxReturniPodModelNum,//Lingo:0x00,Cmd:0x0E
                // iPod Model ID
                mRxPacketPar.iIPodModelID = (int)mRxPacket.pbData.ReadInt();
                /*The iPod model number as a null-terminated
                UTF-8 character array.*/
                mRxPacketPar.wsIpodModelNumber = mRxPacket.pbData.ReadString();
                break;
            case RX_PACKET_CODE.R_ReturnLingoProtocolVersion:
                // RxReturnLingoProtocolVersion,//Lingo:0x00,Cmd:0x1
                /*The lingo for which verison information
                is being returned*/
                mRxPacketPar.bLingoRetVer = mRxPacket.pbData.ReadByte();
                /*protocol version for the given lingo*/
                mRxPacketPar.bLingoVerMajor = mRxPacket.pbData.ReadByte();
                mRxPacketPar.bLingoVerMinor = mRxPacket.pbData.ReadByte();
                break;
            case RX_PACKET_CODE.R_RetiPodPreferences:
                // RxRetiPodPreferences,//Lingo:0x00,Cmd:0x2A
                break;
            case RX_PACKET_CODE.R_RetiPodOptionsForLingo:
                // RxRetiPodOptionsForLingo,//Lingo:0x00,Cmd:0x4C
                mRxPacketPar.bLingoRetOptionsInfo = mRxPacket.pbData.ReadByte();
                break;
            case RX_PACKET_CODE.R_RetSupportedEventNotification:
                // RxRetSupportedEventNotification,//Lingo:0x00,Cmd:0x51
                for (int i = 0; i < 8; i++)
                {
                    mRxPacketPar.bEventNotificationMask[i] =
                        mRxPacket.pbData.ReadByte();
                }
                break;
            case RX_PACKET_CODE.R_ResetIpodDll:
                // RxResetIpodDll,//Lingo:0x00,Cmd:0xF3,Reset iPod Dll
                break;
            case RX_PACKET_CODE.R_SimpleRemoteLingoACK:
                // RxSimpleRemoteLingoACK,//Lingo:0x02,Cmd:0x01
                /*Lingo:0x02,Cmd:0x01,Command result status*/
                mRxPacketPar.bCmdResStatus = mRxPacket.pbData.ReadByte();
                /*Lingo:0x02,Cmd:0x01,The ID of the command being acknowledged*/
                mRxPacketPar.bTxAckId = 0x020000;
                mRxPacketPar.bTxAckId |= mRxPacket.pbData.ReadByte();
                if (mRxPacketPar.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING)
                {
                    // Maximum amount of time to wait for pending response,in
                    // milliseconds
                    mRxPacketPar.uMaxWaitPendingRepTime =
                        mRxPacket.pbData.ReadInt();
                }
                break;
            case RX_PACKET_CODE.R_ExtendInterfaceLingoACK:
                // RxExtendInterfaceLingoACK,//Lingo:0x04,Cmd:0x0001
                /*Lingo:0x04,Cmd:0x01,Command result status*/
                mRxPacketPar.bCmdResStatus = mRxPacket.pbData.ReadByte();
                /*Lingo:0x04,Cmd:0x01,The ID of the command being acknowledged*/
                mRxPacketPar.bTxAckId = 0x040000;
                mRxPacketPar.bTxAckId |= mRxPacket.pbData.ReadShort();
                if (mRxPacketPar.bCmdResStatus == ACK_COMMAND_ERROR_CODES.ACK_PENDING)
                {
                    // Maximum amount of time to wait for pending response,in
                    // milliseconds
                    mRxPacketPar.uMaxWaitPendingRepTime =
                        mRxPacket.pbData.ReadInt();
                }
                break;
            case RX_PACKET_CODE.R_ReturnIndexedPlayingTrackInfo:
                // RxReturnIndexedPlayingTrackInfo,//Lingo:0x04,Cmd:0x000D
                // Track info type
                mRxPacketPar.bTrackInfoType = mRxPacket.pbData.ReadByte();
                switch (mRxPacketPar.bTrackInfoType)
                {
                    case TRACK_INFO_TYPES.TRACK_CAPABILITIES_AND_INFOMATION:// A
                                                                            // 10-byte
                                                                            // data
                    {
                        /*Track Capability bits
                        bit31:9:Reserved
                        bit8:Track is currently queued to play as a video
                        bit7:Track contains video(a video podcast,music video,movie,
                        or TV show
                        bit6:Track has description
                        bit5:Track has release date
                        bit4:Track is a podcast episode
                        bit3:Track has song lyrics
                        bit2:Track has album artwork
                        bit1:Track has chapters
                        bit0:Track is audiobook*/
                        mRxPacketPar.iTrackCapaBits =
                            (int)mRxPacket.pbData.ReadInt();
                        // Total track length,in milliseconds
                        mRxPacketPar.uTrackLen =
                            (int)mRxPacket.pbData.ReadInt();
                        // Chapter count
                        mRxPacketPar.uChapterCount =
                            mRxPacket.pbData.ReadShort();
                    }
                        break;
                    case TRACK_INFO_TYPES.PODCAST_NAME:// UTF-8 string
                        break;
                    case TRACK_INFO_TYPES.TRACK_RELEASE_DATE:// An 8-byte data
                    {
                        if ((mRxPacket.uDataLen - 1) != 8)
                        {
                            break;
                        }
                        /*Seconds(0-59)*/
                        mRxPacketPar.sTrackReleaseDate.bSecond =
                            mRxPacket.pbData.ReadByte();
                        /*Minutes(0-59)*/
                        mRxPacketPar.sTrackReleaseDate.bMinute =
                            mRxPacket.pbData.ReadByte();
                        /*Hours(0-23)*/
                        mRxPacketPar.sTrackReleaseDate.bHour =
                            mRxPacket.pbData.ReadByte();
                        /*Day of the month(1-31)*/
                        mRxPacketPar.sTrackReleaseDate.bDay =
                            mRxPacket.pbData.ReadByte();
                        /*Month(1-12)*/
                        mRxPacketPar.sTrackReleaseDate.bMonth =
                            mRxPacket.pbData.ReadByte();
                        /*Year(eg.2010)*/
                        mRxPacketPar.sTrackReleaseDate.uYear =
                            mRxPacket.pbData.ReadShort();
                        /*Weekday(0-6,where 0=Sunday and 6=Saturday)*/
                        mRxPacketPar.sTrackReleaseDate.eWeekday =
                            mRxPacket.pbData.ReadByte();
                    }
                        break;
                    case TRACK_INFO_TYPES.TRACK_GENRE:// UTF-8 string
                    {
                        mRxPacketPar.wsTrackGenre =
                            mRxPacket.pbData.ReadString();
                    }
                        break;
                    case TRACK_INFO_TYPES.TRACK_COMPOSER:// UTF-8 string
                        break;
                    case TRACK_INFO_TYPES.TRACK_ARTWORK_COUNT:// Artwork count
                                                              // data
                    {
                        /*Lingo:0x04,Cmd:0x000D,formatID*/
                        mRxPacketPar.uFormatID = mRxPacket.pbData.ReadShort();
                        /*Lingo:0x04,Cmd:0x000D,images count*/
                        mRxPacketPar.uImgCount = mRxPacket.pbData.ReadShort();
                    }
                        break;
                    case TRACK_INFO_TYPES.TRACK_DESCRIPTION:// UTF-8 string
                    case TRACK_INFO_TYPES.TRACK_SONG_LYRICS:// UTF-8 string
                    {

                    }
                        break;
                    default:
                        break;
                }
                break;
            case RX_PACKET_CODE.R_RetArtworkFormats:
                // RxRetArtworkFormats,//Lingo:0x04,Cmd:0x000F
                /*Lingo:0x04,Cmd:0x0F,format*/
                mRxPacketPar.vArtWorkFormats.clear();
                int uPos = 0;
                while (uPos < mRxPacket.uDataLen)
                {
                    iPodArtWorkFormat tmpFormat = new iPodArtWorkFormat();

                    tmpFormat.formatID = mRxPacket.pbData.ReadShort();
                    tmpFormat.pixelFormat = mRxPacket.pbData.ReadByte();
                    tmpFormat.width = mRxPacket.pbData.ReadShort();
                    tmpFormat.height = mRxPacket.pbData.ReadShort();

                    /*	RETAILMSG(RETAIL_MSG, (L"FormartID=%04x,pixelformat=%04x,width=%d,height=%d\r\n", 
                    tmpFormat.formatID, tmpFormat.pixelFormat, tmpFormat.width, tmpFormat.height));*/

                    mRxPacketPar.vArtWorkFormats.add(tmpFormat);

                    uPos += 7;
                }
                break;
            case RX_PACKET_CODE.R_RetTrackArtworkData:
                // RxRetTrackArtworkData,//Lingo:0x04,Cmd:0x0011
                /*Lingo:0x04,Cmd:0x11,Desriptor telegram index*/
                // iPodLogDefines.iPodLog(TAG, "R_RetTrackArtworkData");
                mRxPacketPar.uDesrTelegramIndex = mRxPacket.pbData.ReadShort();
                // RETAILMSG(1,(L"RxRetTrackArtworkData=0x%x,0x%x\r\n",g_pProtocol->m_RxPacket.pbData[1],
                // g_pProtocol->m_RxPacket.pbData[0]));
                if (mRxPacketPar.uDesrTelegramIndex == 0)
                {
                    /*Lingo:0x04,Cmd:0x11,Discriptor pixel format code*/
                    mRxPacketPar.uFormatID = mRxPacket.pbData.ReadByte();
                    /*Lingo:0x04,Cmd:0x11,Image width in pixel*/
                    mRxPacketPar.uImgWidth = mRxPacket.pbData.ReadShort();
                    /*Lingo:0x04,Cmd:0x11,Image height in pixel*/
                    mRxPacketPar.uImgHeight = mRxPacket.pbData.ReadShort();
                    /*Lingo:0x04,Cmd:0x11,Inset rectangle,top-left point*/
                    mRxPacketPar.pointTopLeft.x = mRxPacket.pbData.ReadShort();
                    mRxPacketPar.pointTopLeft.y = mRxPacket.pbData.ReadShort();
                    /*Lingo:0x04,Cmd:0x11,Inset rectangle,bottom-right point*/
                    mRxPacketPar.pointBottomRight.x =
                        mRxPacket.pbData.ReadShort();
                    mRxPacketPar.pointBottomRight.y =
                        mRxPacket.pbData.ReadShort();
                    /*Lingo:0x04,Cmd:0x11,Row size*/
                    mRxPacketPar.uRowSize = mRxPacket.pbData.ReadInt();
                    /*Lingo:0x04,Cmd:0x11,Image pixel data*/
                    if (mRxPacket.pbData.GetDataLength()
                        - mRxPacket.pbData.GetDataOffset() < RX_DATA_LEN_MAX)
                    {
                        TypeConvert.memset(mRxPacketPar.pbImgData,
                            0,
                            mRxPacketPar.pbImgData.length);
                        TypeConvert.memcpy(mRxPacketPar.pbImgData,
                            mRxPacket.pbData.GetData(),
                            0,
                            mRxPacket.pbData.GetDataOffset(),
                            mRxPacket.pbData.GetDataLength()
                                - mRxPacket.pbData.GetDataOffset());
                        mRxPacketPar.uImgDataLen =
                            mRxPacket.pbData.GetDataLength()
                                - mRxPacket.pbData.GetDataOffset();
                    }
                }
                else
                {
                    /*Lingo:0x04,Cmd:0x11,Image pixel data*/
                    if (mRxPacket.pbData.GetDataLength()
                        - mRxPacket.pbData.GetDataOffset() < RX_DATA_LEN_MAX)
                    {
                        TypeConvert.memset(mRxPacketPar.pbImgData,
                            0,
                            mRxPacketPar.pbImgData.length);
                        TypeConvert.memcpy(mRxPacketPar.pbImgData,
                            mRxPacket.pbData.GetData(),
                            0,
                            mRxPacket.pbData.GetDataOffset(),
                            mRxPacket.pbData.GetDataLength()
                                - mRxPacket.pbData.GetDataOffset());
                        mRxPacketPar.uImgDataLen =
                            mRxPacket.pbData.GetDataLength()
                                - mRxPacket.pbData.GetDataOffset();
                    }
                }
                break;
            case RX_PACKET_CODE.R_ReturnNumberCategorizedDBRecords:
                // RxReturnNumberCategorizedDBRecords,//Lingo:0x04,Cmd:0x0019
                mRxPacketPar.iRecordCount = mRxPacket.pbData.ReadInt();
                break;
            case RX_PACKET_CODE.R_ReturnCategorizeDBRecord:
                // RxReturnCategorizeDBRecord,//Lingo:0x04,Cmd:0x001B
                // Database record category index
                mRxPacketPar.iRecordCategoryIndex =
                    (int)mRxPacket.pbData.ReadInt();
                // Database record as a UTF-8 character array
                mRxPacketPar.wsRecord = mRxPacket.pbData.ReadString();
                break;
            case RX_PACKET_CODE.R_ReturnPlayStatus:
                // RxReturnPlayStatus,//Lingo:0x04,Cmd:0x001D
                // Track length in milliseconds
                mRxPacketPar.iTrackLen = mRxPacket.pbData.ReadInt();
                // Track position in milliseconds
                mRxPacketPar.iTrackPos = mRxPacket.pbData.ReadInt();
                /*Player state
                0x00:Stop
                0x01:Playing
                0x02:Paused
                0x03-0xFE:Reserved
                0xFF:Error*/
                mRxPacketPar.bPlayerState = mRxPacket.pbData.ReadByte();
                break;
            case RX_PACKET_CODE.R_ReturnCurrentPlayingTrackIndex:
                // RxReturnCurrentPlayingTrackIndex,//Lingo:0x04,Cmd:0x001F
                // Playback track index
                mRxPacketPar.iTrackIndex = mRxPacket.pbData.ReadInt();
                break;
            case RX_PACKET_CODE.R_ReturnIndexPlayingTrackTitle:
                // RxReturnIndexPlayingTrackTitle,//Lingo:0x04,Cmd:0x0021
                // Track title as a UTF-8 character array
                mRxPacketPar.wsTrackTitle = mRxPacket.pbData.ReadString();
                break;
            case RX_PACKET_CODE.R_ReturnIndexedPlayingTrackArtistName:
                // RxReturnIndexedPlayingTrackArtistName,//Lingo:0x04,Cmd:0x0023
                // Artist name as a UTF-8 character array
                mRxPacketPar.wsArtistName = mRxPacket.pbData.ReadString();
                break;
            case RX_PACKET_CODE.R_ReturnIndexedPlayingTrackAlbumName:
                // RxReturnIndexedPlayingTrackAlbumName,//Lingo:0x04,Cmd:0x0025
                // Album name as a UTF-8 character array
                mRxPacketPar.wsAlbumName = mRxPacket.pbData.ReadString();
                break;
            case RX_PACKET_CODE.R_PlayStatusChangeNotification:
                // RxPlayStatusChangeNotification,//Lingo:0x04,Cmd:0x0027
                // New play status
                mRxPacketPar.bNewPlayStatus = mRxPacket.pbData.ReadByte();
                // Paramters
                if (mRxPacket.uDataLen > 9)
                {
                    return;
                }
                for (int i = 0; i < (mRxPacket.uDataLen - 1); i++)
                {
                    mRxPacketPar.pbParams[i] = mRxPacket.pbData.ReadByte();
                }
                break;
            case RX_PACKET_CODE.R_RetTrackArtworkTimes:
                // RxRetTrackArtworkTimes,//Lingo:04,Cmd:0x002B
                // time offset from track start in ms
                mRxPacketPar.uTimeOffset = mRxPacket.pbData.ReadInt();
                break;
            case RX_PACKET_CODE.R_ReturnShuffle:
                // RxReturnShuffle,//Lingo:0x04,Cmd:0x002D
                // Shuffle mode
                mRxPacketPar.bShuffleMode = mRxPacket.pbData.ReadByte();
                break;
            case RX_PACKET_CODE.R_ReturnRepeat:
                // RxReturnRepeat,//Lingo:0x04,Cmd:0x0030
                // Repeat state
                mRxPacketPar.bRepeatState = mRxPacket.pbData.ReadByte();
                break;
            case RX_PACKET_CODE.R_ReturnMonoDisplayImageLimits:
                // RxReturnMonoDisplayImageLimits,//Lingo:0x04,Cmd:0x34
                mRxPacketPar.vLogoFormats.clear();

                iPodLogoFormat tmpFormat = new iPodLogoFormat();
                tmpFormat.maxWidth = mRxPacket.pbData.ReadShort();
                tmpFormat.maxHeight = mRxPacket.pbData.ReadShort();
                tmpFormat.dispPixelFormat = mRxPacket.pbData.ReadByte();
                /*RETAILMSG(RETAIL_MSG, (L"DispFormat=0x%x,Maxwidth=%d,Maxheight=%d\r\n", 
                tmpFormat.dispPixelFormat, tmpFormat.maxWidth, tmpFormat.maxHeight));*/

                mRxPacketPar.vLogoFormats.add(tmpFormat);
                break;
            case RX_PACKET_CODE.R_ReturnNumPlayingTracks:
                // RxReturnNumPlayingTracks,//Lingo:0x04,Cmd:0x0036
                // Number of tracks playing
                mRxPacketPar.iTrackPlayingNum = mRxPacket.pbData.ReadInt();
                break;
            case RX_PACKET_CODE.R_ReturnColorDisplayImageLimits:
                // RxReturnColorDisplayImageLimits,//Lingo:0x04,Cmd:0x003A
                mRxPacketPar.vLogoFormats.clear();
                int iPos = 0;
                while (iPos < mRxPacket.uDataLen)
                {
                    iPodLogoFormat tmpFormat2 = new iPodLogoFormat();
                    tmpFormat2.maxWidth = mRxPacket.pbData.ReadShort();
                    tmpFormat2.maxHeight = mRxPacket.pbData.ReadShort();
                    tmpFormat2.dispPixelFormat = mRxPacket.pbData.ReadByte();
                    /*RETAILMSG(RETAIL_MSG, (L"DispFormat=0x%x,Maxwidth=%d,Maxheight=%d\r\n", 
                    tmpFormat.dispPixelFormat, tmpFormat.maxWidth, tmpFormat.maxHeight));*/

                    mRxPacketPar.vLogoFormats.add(tmpFormat2);
                    iPos += 5;
                }
                break;
            case RX_PACKET_CODE.R_RetDBTrackInfo:
                // RxRetDBTrackInfo,//Lingo:0x04,Cmd:0x0041
                break;
            case RX_PACKET_CODE.R_NewiPodTrackInfo:
                // RxNewiPodTrackInfo,//Lingo:0x0A,Cmd:0x0004
                /*Lingo:0x0A,Cmd:0x0004,New sample rate*/
                mRxPacketPar.uNewSampleRate = mRxPacket.pbData.ReadInt();
                /*Lingo:0x0A,Cmd:0x0004,New Sound Check value*/
                mRxPacketPar.uNewSoundCheckValue = mRxPacket.pbData.ReadInt();
                /*Lingo:0x0A,Cmd:0x0004,New track volume adjustment*/
                mRxPacketPar.uTrackVolumeAdjustment =
                    mRxPacket.pbData.ReadInt();
                break;
            default:
                break;
        }
        if (miPodCmdAnalysed != null)
        {
            miPodCmdAnalysed.OnRxAnalysedEnter(rpCode, mRxPacketPar);
        }
    }
}
