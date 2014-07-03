package pateo.com.iPod;

import android.util.Log;

public interface iPodDefine
{

    public final class SP_SUCCESS_ERROR_ENUM
    {

        public final static int SP_SUCCESSFUL = 0;// successful

        public final static int SP_ERR_CONNECT_DEVICE_FAILED = -1;// connect to
                                                                  // device
                                                                  // failed

        public final static int SP_ERR_NO_RESOURCE_TO_FIND = -2;// no resource
                                                                // to find

        public final static int SP_ERR_OUT_OF_RESOURCE = -3;// out of resource

        public final static int SP_ERR_COMMAND_FAILED = -4;// command failed

        public final static int SP_ERR_BAD_PARAMETER = -5;// bad parameter

        public final static int SP_ERR_UNKNOWN_ID = -6;// unknown ID

        public final static int SP_ERR_OPERATION_TIMEED_OUT = -7;// operation
                                                                 // timed out
    };

    // media type
    public enum SP_MEDIA_TYPE_ENUM
    {

        SP_MEDIA_All, // all
        SP_MEDIA_Video, // video
        SP_MEDIA_Audio, // audio
        SP_MEDIA_Image, // image
        SP_MEDIA_DirectControlAudio, // DirectControlAudio
        SP_MEDIA_DirectControlVideo, // DirectControlVideo
        SP_MEDIA_REAR_VIDEO, // rear video
        SP_MEDIA_REAR_DirectControlVideo, // rear video
        SP_MEDIA_Unknow, // unknown
    };

    // 在加载大数据时，用来进行冗错处理
    public class IPOD_LOAD_MEMORY_STATE
    {
        public final static int IPOD_LOAD_END = 0x0;

        public final static int IPOD_LOAD_START = 0x1;

        public final static int IPOD_LOAD_SONG = 0x2;

        public final static int IPOD_LOAD_SERIAL_NUM = 0x3;

        public final static int IPOD_LOAD_ARTIST = 0x4;

        public final static int IPOD_LOAD_ALBUM = 0x5;

        public final static int IPOD_LOAD_GENRE = 0x6;

        public final static int IPOD_LOAD_NONE = 0x7;

        public final static int IPOD_LOAD_VIDEO = 0x8;

        public final static int IPOD_LOAD_VIDEO_ON = 0x9;

        public final static int IPOD_LOAD_NOT_AUTHENTICATED = 0xa;
    }

    public class SP_IPOD_REFRESH_ENUM
    {

        public final static byte SP_IPOD_REF_TRACK_INDEX = 0;

        public final static byte SP_IPOD_REF_TRACK_ID3 = 1;// refresh track ID3
                                                           // of current track

        public final static byte SP_IPOD_REF_TRACK_ARTWORK = 2;

        public final static byte SP_IPOD_REF_PLAY_TIME = 3;// refresh play time
                                                           // of current track

        public final static byte SP_IPOD_REF_PLAY_STATE = 4;// refresh play
                                                            // state

        public final static byte SP_IPOD_REF_REPEAT_STATE = 5;// refresh play
                                                              // mode state

        public final static byte SP_IPOD_REF_SHUFFLE_STATE = 6;

        public final static byte SP_IPOD_REF_LIST = 7;// refresh list

        public final static byte SP_IPOD_REF_LIST_Path = 8;

        public final static byte SP_IPOD_REF_HINT = 9;// refresh hint

        public final static byte SP_IPOD_REF_VOLUME = 10;// refresh volume

        public final static byte SP_IPOD_REF_VIDEO_RENTALS_STATE = 11;// 0
                                                                      // 无,1有Video
                                                                      // Rentals
                                                                      // State

        public final static byte SP_IPOD_REF_DATABASE_STATE = 12;

        public final static byte SP_IPOD_REF_SEARCH_RESULTS = 13;

        public final static byte SP_IPOD_REF_PLAYING_INDEX = 14;

        public final static byte SP_IPOD_REF_PLAY_TYPE = 15;

        public final static byte SP_IPOD_REF_REQUESTCLOSE = 16;

        public final static byte SP_IPOD_REF_PLAYINBACK = 17;

        public final static byte SP_IPOD_REF_PARKING = 18;// obj parking状态true
                                                          // or false

        public final static byte SP_IPOD_AUDIO_TYPE = 19;

        public final static byte SP_IPOD_SET_AUDIO_TYPE = 20;

        public final static byte SP_IPOD_REF_SOURCEEXIT = 21;

        public final static byte SP_IPOD_REF_TRACK_TYPE = 22;

        public final static byte SP_IPOD_NUM_CATEGORIZED_DBRECORD = 23;

        public final static byte SP_IPOD_REF_PLAYING_NEXT = 24;

        public final static byte SP_IPOD_REF_PLAYING_PRE = 25;

        public final static byte SP_IPOD_VOIDO_OVER_VIDEOVIEW = 26;

        public final static byte SP_IPOD_NEXT_PLUS = 27;

        public final static byte SP_IPOD_PREV_PLUS = 28;

        public final static byte SP_IPOD_SMART_CLICK = 29;
    }

    // 定义iPod播放状态
    public class IPOD_PLAY_STATE
    {
        public final static byte IPOD_PST_STOP = 0;

        public final static byte IPOD_PST_PLAY = 1;

        public final static byte IPOD_PST_PAUSE = 2;

        public final static byte IPOD_PST_FF_START = 3;

        public final static byte IPOD_PST_REW_START = 4;

        public final static byte IPOD_PST_FFREW_END = 5;

        public final static byte IPOD_PST_NONE = 6;
    }

    // 定义Shuffle状态
    public class IPOD_SHUFFLE_STATUS
    {
        public final static byte IPOD_SHUFFLE_OFF = 0x00;

        public final static byte IPOD_SHUFFLE_SONG = 0x01;

        public final static byte IPOD_SHUFFLE_ALBUM = 0x02;
    }

    // 定义Repeat状态
    public class IPOD_REPEAT_STATUS
    {
        public final static byte IPOD_REPEAT_OFF = 0x00;

        public final static byte IPOD_REPEAT_ONE_TRACK = 0x01;

        public final static byte IPOD_REPEAT_ALL_TRACKS = 0x02;
    }

    public enum SP_IPOD_REF_DATABASE_STATE_ENUM
    {

        SP_IPOD_REF_DATABASE_OFF, SP_IPOD_REF_DATABASE_PLAYLISTS, SP_IPOD_REF_DATABASE_ARTISTS, SP_IPOD_REF_DATABASE_ALBUMS, SP_IPOD_REF_DATABASE_GENRES, SP_IPOD_REF_DATABASE_SONGS, SP_IPOD_REF_DATABASE_AUDIOBOOKS, SP_IPOD_REF_DATABASE_ALL,
    };

    public enum SP_IPOD_REF_SHUFFLE_STATE_ENUM
    {

        SP_IPOD_REF_SHUFFLE_STATE_OFF, // shuffle off
        SP_IPOD_REF_SHUFFLE_STATE_TRACKS, // shuffle tracks
    };

    public enum SP_IPOD_REF_REPEAT_STATE_ENUM
    {
        SP_IPOD_REF_REPEAT_STATE_OFF, // repeat off
        SP_IPOD_REF_REPEAT_STATE_ONE_TRACK, // repeat one track
        SP_IPOD_REF_REPEAT_STATE_ALL_TRACKS, // repeat all tracks
    };

    public final class SP_IPOD_REF_PLAY_MODE_STATE_ENUM
    {

        public final static byte SP_IPOD_REF_PLAY_MODE_STATE_SHUFFLE = 1;// shuffle

        public final static byte SP_IPOD_REF_PLAY_MODE_STATE_REPEAT = 2;// repeat

        public final static byte SP_IPOD_REF_PLAY_MODE_STATE_SCAN = 3;// Scan
    };

    public enum SP_IPOD_REF_SCAN_STATE_ENUM
    {

        SP_IPOD_REF_SCAN_STATE_OFF, // scan off
        SP_IPOD_REF_SCAN_STATE_ON, // scan on
    };

    // List file type
    public final class SP_IPOD_LIST_FILE_TYPE
    {

        public final static byte LIST_FILE_UNKNOWN = 0x0;

        public final static byte LIST_FILE_FOLDER = 1;// Folder

        public final static byte LIST_FILE_AUDIO = 2;// audio

        public final static byte LIST_FILE_VIDEO = 3;// video

        public final static byte LIST_FILE_PICTURE = 4;// picture

        public final static byte LIST_FILE_PlayList = 0x20;// playlist

        public final static byte LIST_FILE_Songs = 0x21;// songs

        public final static byte LIST_FILE_Artist = 0x22;// artist

        public final static byte LIST_FILE_Album = 0x23;// album

        public final static byte LIST_FILE_Genre = 0x24;// genre

        public final static byte LIST_FILE_Composer = 0x25;// composer

        public final static byte LIST_FILE_AudioBook = 0x26;// audiobook

        public final static byte LIST_FILE_Podcast = 0x27;// podcast

        public final static byte LIST_FILE_Movies = 0x30;

        public final static byte LIST_FILE_MusicVideos = 0x31;

        public final static byte LIST_FILE_TVShows = 0x32;

        public final static byte LIST_FILE_VideoPodcasts = 0x33;

        public final static byte LIST_FILE_Rentals = 0x34;
    };

    // List category type
    public enum SP_IPOD_LIST_CATEGORY_TYPE
    {
        LIST_CATEGORY_UNKNOWN, //
        LIST_CATEGORY_PLAYLISTS, // Playlists
        LIST_CATEGORY_ARTISTS, // Artists
        LIST_CATEGORY_SONGS, // Songs
        LIST_CATEGORY_ALBUMS, // Albums
        LIST_CATEGORY_COMPOSERS, // Composers
        LIST_CATEGORY_AUDIOBOOKS, // AudioBooks
        LIST_CATEGORY_GENRES, // Genres
        LIST_CATEGORY_PODCASTS, // Podcasts
        LIST_CATEGORY_MOVIES, // Movies
        LIST_CATEGORY_MUSIC_VIDEOS, // Music Videos
        LIST_CATEGORY_TV_SHOWS, // TV Shows
        LIST_CATEGORY_VIDEO_PODCASTS, // Video Podcasts
        LIST_CATEGORY_RENTALS, // Rentals
    };

    // Volume
    public enum SP_IPOD_VOLUME_ENUM
    {
        SP_IPOD_VOLUME_UNKNOWN, SP_IPOD_VOLUME_MIN, SP_IPOD_VOLUME_MIDDLE_0, SP_IPOD_VOLUME_MIDDLE_1, SP_IPOD_VOLUME_MIDDLE_2, SP_IPOD_VOLUME_MIDDLE_3, SP_IPOD_VOLUME_MAX,
    };

    // track type
    public enum SP_IPOD_REF_TRACK_TYPE_ENUM
    {

        SP_IPOD_REF_TRACK_TYPE_UNKNOWN, SP_IPOD_REF_TRACK_TYPE_AUDIO, // audio
        SP_IPOD_REF_TRACK_TYPE_VIDEO, // video
    };

    // hint
    public enum SP_IPOD_REF_HINT_ENUM
    {

        SP_IPOD_REF_HINT_UNKNOWN, SP_IPOD_REF_HINT_NO_MEDIA_FILE, // no media
                                                                  // file
        SP_IPOD_REF_HINT_COMMUNICATION_ERR, // communication error
    };

    // ABC123Search
    public enum SP_IPOD_ABC123_SEARCH_ENUM
    {

        SP_IPOD_ABC123_UNKNOWN, SP_IPOD_ABC123_A, SP_IPOD_ABC123_Z, SP_IPOD_ABC123_123,
    };

    // DataType Define
    public enum SP_IPOD_DATA_ENUM
    {
        SP_IPOD_DATA_REPEAT_STATUS, SP_IPOD_DATA_PLAY_STATUS, SP_IPOD_DATA_SHUFFLE_STATUS, SP_IPOD_DATA_MEDIA_TYPE, SP_IPOD_DATA_ID3_INFO, SP_IPOD_DATA_LIST, SP_IPOD_DATA_PARKING, SP_IPOD_DATA_TRACK_TYPE, SP_IPOD_DATA_CURRENT_TRACK,
    }

    // log
    public final static class iPodLogDefines
    {
        public static final void iPodLog(String tag, String msg)
        {
            Log.i(tag, msg);
        }
    }
}
