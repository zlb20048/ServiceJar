package pateo.com.iPod;

import pateo.com.iPod.iPodDefine.SP_IPOD_REF_TRACK_TYPE_ENUM;

public interface iPodType
{
    // OSD
    public class SP_IPOD_OSD
    {
        // Enabled
        public boolean bIsEnabled = true;

        // ROOT
        public String wcRoot = "Root";

        // Audio
        public String wcAudio = "Audio";

        public String wcPlaylists = "Playlists";

        public String wcArtists = "Artists";

        public String wcAlbums = "Albums";

        public String wcGenres = "Genres";

        public String wcSongs = "Songs";

        public String wcComposers = "Composers";

        public String wcAudiobooks = "Audiobooks";

        public String wcPodcasts = "Podcasts";

        // Video
        public String wcVideo = "Video";

        public String wcMovies = "Movies";

        public String wcMusicVideos = "Music Videos";

        public String wcTVShows = "TV Show";

        public String wcVideoPodcasts = "Video Podcasts";

        public String wcRentals = "Rentals";
    }

    public class SP_IPOD_TRACK_INDEX_TIME
    {

        public long uIndex = 0;

        public long uTotalIndex = 0;
    }

    public class SP_IPOD_ID3
    {
        // Track Type
        public SP_IPOD_REF_TRACK_TYPE_ENUM eType =
            SP_IPOD_REF_TRACK_TYPE_ENUM.SP_IPOD_REF_TRACK_TYPE_UNKNOWN;// SP_IPOD_REF_TRACK_TYPE_ENUM

        // Title
        public String wcTitle = "";

        // Artist
        public String wcArtist = "";

        // Album
        public String wcAlbum = "";

        // Genre
        public String wcGenre = "";

        // Release
        public String wcRelease = "";
    }

    public class SP_IPOD_LIST_PARENT_STRUCT
    {

        String wcCaption = null;// Caption

        int uTotalNum = 0;// total num
    }

    public class SP_IPOD_PER_LIST_STRUCT
    {

        int uId = 0;// id

        String wcCaption = null;

        int eABC123 = 0;// ABC123 Type

        boolean bIsChild = false;// If is child

        int eFileType = 0;// SP_IPOD_LIST_FILE_TYPE

        int uIndex = 0;
    }

    public class SP_IPOD_LIST_STRUCT
    {

        SP_IPOD_LIST_PARENT_STRUCT sParent = null;// parent

        int uListNum = 0;// list num

        int uListStartId = 0;// list start id

        int uListEndId = 0;// list end id

        int uListMaxNum = 0;// max of list num

        int uListSelId = 0;// list Sel ID

        boolean bIsOnlyUpdateSel = false;// Only Update Sel num

        boolean bIsSearchList = false;

        SP_IPOD_PER_LIST_STRUCT[] sList;// list

        public SP_IPOD_LIST_STRUCT()
        {
            sList = new SP_IPOD_PER_LIST_STRUCT[1];
            sList[0] = null;
        }

        public SP_IPOD_LIST_STRUCT(int nCount)
        {
            sList = new SP_IPOD_PER_LIST_STRUCT[nCount];
            for (int i = 0; i < nCount; i++)
            {
                sList[i] = null;
            }
        }
    }

    // 该结构体定义了歌曲或文件夹的信息
    public class SongData
    {
        // id
        public long ID;

        // 名称
        public String SongName;

        // 是否是选中状态
        public boolean IsSelected;

        // 文件类别
        public byte fileType;

        // 是否是焦点
        public boolean IsFocused;
    }
}
