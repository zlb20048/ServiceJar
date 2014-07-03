package pateo.engine.iPod;

import pateo.com.iPod.iPodDefine.IPOD_PLAY_STATE;
import pateo.com.iPod.iPodDefine.IPOD_REPEAT_STATUS;
import pateo.com.iPod.iPodDefine.IPOD_SHUFFLE_STATUS;
import pateo.com.iPod.iPodDefine.SP_IPOD_REF_TRACK_TYPE_ENUM;
import pateo.com.iPod.iPodDefine.SP_MEDIA_TYPE_ENUM;
import pateo.com.iPod.iPodType.SP_IPOD_OSD;
import pateo.engine.iPod.iPodDataDef.AUDIO_TYPE;
import android.graphics.Bitmap;

public class iPodDataLayer
{
    private static iPodDataLayer mThis = null;

    private SP_MEDIA_TYPE_ENUM mMediaType;

    private boolean mbIsSupportVideo;

    private boolean mbIsPlayingVideo;

    private int miPodModelID;

    private AUDIO_TYPE mAudioType;

    private String miPodName;

    private String miPodSerialNum;// iPod的序列号

    private byte mPlayState = IPOD_PLAY_STATE.IPOD_PST_NONE;

    private byte mShuffleState = IPOD_SHUFFLE_STATUS.IPOD_SHUFFLE_OFF;

    private byte mRepeatState = IPOD_REPEAT_STATUS.IPOD_REPEAT_OFF;

    private String mTrackTitle;

    private String mTrackArtist;

    private String mTrackAlbum;

    private String mTrackGenre;

    private String mTrackRelease;

    private Bitmap mArtwork = null;// ArtWork图片句柄

    private SP_IPOD_OSD miPodOSD = new SP_IPOD_OSD();

    // 当前播放曲目类别
    private SP_IPOD_REF_TRACK_TYPE_ENUM mCurTrackType =
        SP_IPOD_REF_TRACK_TYPE_ENUM.SP_IPOD_REF_TRACK_TYPE_UNKNOWN;

    private long mCurTrackIndex;// 正在播放的歌曲

    private long mTotalTrack;

    private long mCurPlayTime;

    private long mTotalPlayTime;

    private boolean mbUseIDPS = false;

    private int muTransID = 0;

    private iPodDataLayer()
    {

    }

    public static iPodDataLayer GetInstant()
    {
        if (mThis == null)
        {
            mThis = new iPodDataLayer();
        }
        return mThis;
    }

    public void SetMediaType(SP_MEDIA_TYPE_ENUM type)
    {
        mMediaType = type;
    }

    public SP_MEDIA_TYPE_ENUM GetMediaType()
    {
        return mMediaType;
    }

    public void SetIsPlayingVideo(boolean isViedo)
    {
        mbIsPlayingVideo = isViedo;
    }

    public boolean GetIsPlayingVideo()
    {
        return mbIsPlayingVideo;
    }

    public void SetIsSupportVideo(boolean isSupport)
    {
        mbIsSupportVideo = isSupport;
    }

    public boolean GetIsSupportVideo()
    {
        return mbIsSupportVideo;
    }

    public void SetiPodModelID(int id)
    {
        miPodModelID = id;
    }

    public int GetiPodModelID()
    {
        return miPodModelID;
    }

    public void SetAudioType(AUDIO_TYPE type)
    {
        mAudioType = type;
    }

    public AUDIO_TYPE GetAudioType()
    {
        return mAudioType;
    }

    public void SetiPodName(String name)
    {
        miPodName = name;
    }

    public String GetiPodName()
    {
        return miPodName;
    }

    public void SetPlayState(byte state)
    {
        mPlayState = state;
    }

    public byte GetPlayState()
    {
        return mPlayState;
    }

    public void SetiPodSerialNum(String num)
    {
        miPodSerialNum = num;
    }

    public String GetiPodSerialNum()
    {
        return miPodSerialNum;
    }

    public void SetArtwork(Bitmap pic)
    {
        mArtwork = pic;
    }

    public Bitmap GetArtwork()
    {
        return mArtwork;
    }

    public void SetCurTrackType(SP_IPOD_REF_TRACK_TYPE_ENUM type)
    { // 当前播放曲目类别
        mCurTrackType = type;
    }

    public SP_IPOD_REF_TRACK_TYPE_ENUM GetCurTrackType()
    {
        return mCurTrackType;
    }

    public void SetCurTrackIndex(long index)
    {
        mCurTrackIndex = index;
    }

    public long GetCurTrackIndex()
    {
        return mCurTrackIndex;
    }

    public void SetiPodOSD(SP_IPOD_OSD iOSD)
    {
        if (null != iOSD)
        {
            miPodOSD = iOSD;
        }
    }

    public SP_IPOD_OSD GetiPodOSD()
    {
        return miPodOSD;
    }

    public void SetShuffleState(byte state)
    {
        mShuffleState = state;
    }

    public byte GetShuffleState()
    {
        return mShuffleState;
    }

    public void SetRepeatState(byte state)
    {
        mRepeatState = state;
    }

    public byte GetRepeatState()
    {
        return mRepeatState;
    }

    public void SetTotalTrack(long count)
    {
        mTotalTrack = count;
    }

    public long GetTotalTrack()
    {
        return mTotalTrack;
    }

    public void SetTrackTitle(String title)
    {
        mTrackTitle = title;
    }

    public String GetTrackTitle()
    {
        return mTrackTitle;
    }

    public void SetTrackArtist(String artist)
    {
        mTrackArtist = artist;
    }

    public String GetTrackArtist()
    {
        return mTrackArtist;
    }

    public void SetTrackAlbum(String album)
    {
        mTrackAlbum = album;
    }

    public String GetTrackAlbum()
    {
        return mTrackAlbum;
    }

    public void SetTrackGenre(String genre)
    {
        mTrackGenre = genre;
    }

    public String GetTrackGenre()
    {
        return mTrackGenre;
    }

    public void SetTrackRelease(String date)
    {
        mTrackRelease = date;
    }

    public String GetTrackRelease()
    {
        return mTrackRelease;
    }

    public void SetCurPlayTime(long time)
    {
        mCurPlayTime = time;
    }

    public long GetCurPlayTime()
    {
        return mCurPlayTime;
    }

    public void SetTotalPlayTime(long time)
    {
        mTotalPlayTime = time;
    }

    public long GetTotalPlayTime()
    {
        return mTotalPlayTime;
    }

    public void SetUseIDPS(boolean isIDPS)
    {
        mbUseIDPS = isIDPS;
    }

    public boolean GetUseIDPS()
    {
        return mbUseIDPS;
    }

    public void SetTransID(int transID)
    {
        muTransID = transID;
    }

    public int GetTransID()
    {
        return muTransID;
    }
}
