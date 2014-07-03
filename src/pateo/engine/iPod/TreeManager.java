package pateo.engine.iPod;

import java.util.ArrayList;

import android.content.ContentResolver;

import pateo.com.iPod.iPodDefine.SP_IPOD_LIST_FILE_TYPE;
import pateo.com.iPod.iPodType.SongData;
import pateo.engine.iPod.iPodDataDef.IPOD_CATEGORY_TYPES;
import pateo.engine.iPod.iPodDataDef.IPOD_RET_RESULT_VALUE;

public class TreeManager
{
    private TreeNode mRoot;// 根节点

    private TreeNode mListParentFolder;// 当前显示列表的父文件夹节点

    private TreeNode mAudioRoot;// 音频根节点，位于m_Root下

    private TreeNode mVideoRoot;// 视频根节点，位于m_Root下

    private TreeNode mPlayParentFolder; // 当前播放的文件夹节点

    private int mCurrentCategoryCodeID;

    private boolean mIsAudioPlay;

    private long mPlayingIndex;

    // 清除所有列表
    public void ClearAll()
    {
        if (mRoot == null)
        {
            mRoot = new TreeNode();
        }
        mRoot.Clear();
        mRoot.SetChildCapacity(2);
        mAudioRoot =
            mRoot.AddChild(0, "Audio", IPOD_CATEGORY_TYPES.CATEGORY_NOTHING);
        mVideoRoot =
            mRoot.AddChild(1, "Video", IPOD_CATEGORY_TYPES.CATEGORY_GENRE);

        mPlayParentFolder = null;
        mListParentFolder = mAudioRoot;
    }

    // 增加文件
    public TreeNode AddFile(long _ID, String _name, byte _type)
    {
        if (mListParentFolder != null)
        {
            return mListParentFolder.AddChild(_ID, _name, _type);
        }
        return null;
    }

    // 获取父结点类别
    public int GetParentType()
    {
        if (mListParentFolder != null)
        {
            if (mListParentFolder.GetNodeType() >= IPOD_CATEGORY_TYPES.CATEGORY_Movies
                && mListParentFolder.GetNodeType() <= IPOD_CATEGORY_TYPES.CATEGORY_Rentals)
            {
                return IPOD_CATEGORY_TYPES.CATEGORY_GENRE;
            }
            return mListParentFolder.GetNodeType();
        }
        return -1;
    }

    public void SetIsAudioPlay(boolean isAudio)
    {
        mIsAudioPlay = isAudio;
    }

    // 返回根结点
    public void ReturnRootLevel()
    {
        if (mIsAudioPlay)
        {
            mListParentFolder = mAudioRoot;
        }
        else
        {
            mListParentFolder = mVideoRoot;
        }
    }

    // 返回父目录
    public void ReturnParentFolder()
    {
        if (mListParentFolder.GetParent() != null)
        {
            mListParentFolder = mListParentFolder.GetParent();
        }
    }

    // 进入子目录
    public void EnterChildFolder(long nParentIndex)
    {
        if (mListParentFolder != null)
        {
            TreeNode tmpNode = mListParentFolder.GetChild(nParentIndex);
            if (tmpNode != null)
            {
                mListParentFolder = tmpNode;
            }
        }
    }

    // 设置子结点容积
    public void SetChildCapacity(long lCount)
    {
        if (mListParentFolder != null)
        {
            long lTheoryCount = mListParentFolder.GetChildCapacity();
            if (lTheoryCount != lCount)
            {
                mListParentFolder.Clear();
                mListParentFolder.SetChildCapacity(lCount);
            }
        }
    }

    // 获取子结点类型
    public long GetChildCapacity()
    {
        if (mListParentFolder == null)
        {
            return 0;
        }
        long nTheoryCount = mListParentFolder.GetChildCapacity();
        return nTheoryCount;
    }

    // 获取结点信息
    public SongData GetTrackInfo(long nTrackIndex)
    {
        SongData songData = new SongData();
        if (mListParentFolder != null)
        {
            TreeNode tmpNode = mListParentFolder.GetChild(nTrackIndex);
            if (tmpNode != null)
            {
                songData.ID = tmpNode.GetNodeID();
                songData.SongName = tmpNode.GetNodeName();
                if (mListParentFolder == mPlayParentFolder
                    && nTrackIndex == mPlayingIndex)
                {
                    songData.IsSelected = true;
                    // RETAILMSG(1,(L"SelTree=%ld\r\n",_ListSong.ID));
                    songData.IsFocused = true;
                }
                else
                {
                    // UINT nID = m_ListParentFolder->GetFocusID();
                    // if (nID != INVALID_LIST_ID_SUB_ONE && nID ==
                    // _ListSong.ID)
                    // _ListSong.IsFocused = true;
                    // else
                    // _ListSong.IsFocused = false;
                    // _ListSong.IsSelected = false;
                }

                if (mListParentFolder == mAudioRoot)
                {
                    switch (tmpNode.GetNodeType())
                    // nTrackIndex
                    {
                        case IPOD_CATEGORY_TYPES.CATEGORY_PLAYLIST:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_PlayList;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_ARTIST:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Artist;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_ALBUM:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Album;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_GENRE:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Genre;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_TRACK:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Songs;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_COMPOSER:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Composer;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_AUDIOBOOK:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_AudioBook;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_PODCAST:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Podcast;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_Movies:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Movies;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_MusicVideos:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_MusicVideos;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_TVShows:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_TVShows;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_VideoPodcasts:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_VideoPodcasts;
                            break;
                        case IPOD_CATEGORY_TYPES.CATEGORY_Rentals:
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Rentals;
                            break;
                        default:
                            break;
                    }
                }
                else
                {
                    if (tmpNode.GetNodeType() == IPOD_CATEGORY_TYPES.CATEGORY_TRACK
                        && mIsAudioPlay)
                    {
                        songData.fileType =
                            SP_IPOD_LIST_FILE_TYPE.LIST_FILE_AUDIO;
                    }
                    else if (tmpNode.GetNodeType() == IPOD_CATEGORY_TYPES.CATEGORY_TRACK
                        && !mIsAudioPlay)
                    {
                        songData.fileType =
                            SP_IPOD_LIST_FILE_TYPE.LIST_FILE_VIDEO;
                    }
                    else
                    {
                        songData.fileType =
                            SP_IPOD_LIST_FILE_TYPE.LIST_FILE_FOLDER;
                    }
                }
                return songData;
            }
        }
        return null;
    }

    // 获取结点类别
    public int GetNodeType(long nTrackIndex)
    {
        if (mListParentFolder != null)
        {
            TreeNode tmpNode = mListParentFolder.GetChild(nTrackIndex);
            if (tmpNode != null)
            {
                if (tmpNode.GetNodeType() >= IPOD_CATEGORY_TYPES.CATEGORY_Movies
                    && tmpNode.GetNodeType() <= IPOD_CATEGORY_TYPES.CATEGORY_Rentals)
                {
                    return IPOD_CATEGORY_TYPES.CATEGORY_GENRE;
                }
                return tmpNode.GetNodeType();
            }
        }
        return -1;
    }

    // 更新播放文件夹
    public void UpdatePlayFolder()
    {
        if (mListParentFolder != null)
        {
            mPlayParentFolder = mListParentFolder;
            mPlayingIndex = 0xffffffL;
        }
    }

    // 更新播放文件
    public void UpdatePlayingTrack(long nTrackIndex)
    {
        if (mPlayParentFolder != null)
        {
            mPlayingIndex = nTrackIndex;
        }
    }

    // 获取当前播放文件索引
    public long GetCurrentPlayingTrack()
    {
        return mPlayingIndex;
    }

    // 判断当前播放文件是否在当前列表中
    public boolean IsPlayTrackInlist()
    {
        if (mListParentFolder == null)
        {
            return false;
        }
        if (mPlayParentFolder == mListParentFolder)
        {
            return true;
        }
        else
            return false;
    }

    // 判断列表是否处于根目录
    public boolean IsRootLevel()
    {
        if (mListParentFolder == null)
        {
            return true;
        }
        else if (mListParentFolder == mAudioRoot
            || mListParentFolder == mVideoRoot)
        {
            return true;
        }
        else
            return false;
    }

    // 判断父列表是否处于视频根目录
    public boolean ParentIsVideoRootLevel()
    {
        if (mListParentFolder == mVideoRoot)
        {
            return true;
        }
        else
            return false;
    }

    // 判断是否是列表最后一级
    public boolean IsLastLevel()
    {
        if (mListParentFolder == null)
        {
            return false;
        }
        TreeNode tmpNode = mListParentFolder.GetParent();
        if (tmpNode == null)
            return false;
        if (tmpNode == mAudioRoot || tmpNode == mVideoRoot)
            return true;
        else
            return false;
    }

    // 判断是否是显示音频
    public boolean IsAudioDisplay()
    {
        return mIsAudioPlay;
    }

    // 设置音视频显示类别
    public void DisplayMediaType(boolean isAudio)
    {
        mIsAudioPlay = isAudio;
        ReturnRootLevel();
    }

    // 获取类别路径
    public String GetListPath()
    {
        if (mListParentFolder == null)
            return "";
        if (mListParentFolder == mVideoRoot)
        {
            // path name is iPod device name when in Video root
            return mListParentFolder.GetNodeName();
        }
        if (mListParentFolder == mAudioRoot)
        {
            return mListParentFolder.GetNodeName();
        }

        TreeNode tmpNode = mListParentFolder.GetParent();
        if (tmpNode == null)
            return "";
        if (tmpNode == mAudioRoot || tmpNode == mVideoRoot)
        {
            return mListParentFolder.GetNodeName();
        }
        else
        {
            TreeNode tmpParentNode = tmpNode.GetParent();
            while (tmpParentNode != null && tmpParentNode != mAudioRoot
                && tmpParentNode != mVideoRoot)
            {
                tmpNode = tmpParentNode;
                tmpParentNode = tmpNode.GetParent();
                // RETAILMSG(1,(L"Parent3=%s\r\n",_name.c_str()));
            }
            return mListParentFolder.GetNodeName();
        }
    }

    // 设置根目录名称
    public void SetRootName(String str)
    {
        mRoot.SetNodeName(str);
    }

    // 设置音频根目录名称
    public void SetAudioRootName(String str)
    {
        if (mAudioRoot != null)
        {
            mAudioRoot.SetNodeName(str);
        }
    }

    // 设置视频根目录名称
    public void SetVideoRootName(String str)
    {
        if (mVideoRoot != null)
        {
            mVideoRoot.SetNodeName(str);
        }
    }

    // 设置音频根目录子孩子名称
    public void SetAudioChildName(long _ID, String str)
    {
        if (mAudioRoot != null)
        {
            TreeNode tmpNode = mAudioRoot.GetChild(_ID);
            if (tmpNode != null)
            {
                tmpNode.SetNodeName(str);
            }
        }
    }

    // 设置视频根目录子孩子名称
    public void SetVideoChildName(long _ID, String str)
    {
        if (mVideoRoot != null)
        {
            TreeNode tmpNode = mVideoRoot.GetChild(_ID);
            if (tmpNode != null)
            {
                tmpNode.SetNodeName(str);
            }
        }
    }

    // 在目录同步时，得到下一次将要进入的子目录
    public IPOD_RET_RESULT_VALUE PathBack_GetNextIndex()
    {
        IPOD_RET_RESULT_VALUE res = new IPOD_RET_RESULT_VALUE();
        if (mListParentFolder == null || mPlayParentFolder == null)
        {
            res.result = -1;
            return res;
        }
        if (mPlayParentFolder == mListParentFolder)
        {
            res.value = mPlayingIndex;
            res.result = 1;
            return res;
        }

        TreeNode tmpCurrent = mPlayParentFolder;
        TreeNode tmpParent = mPlayParentFolder.GetParent();
        while (tmpParent != null)
        {
            if (tmpParent == mListParentFolder)
            {
                res.value = tmpCurrent.GetNodeID();
                res.result = 0;
                return res;
            }
            tmpCurrent = tmpParent;
            tmpParent = tmpParent.GetParent();
            if (tmpParent == null)
            {
                res.result = -1;
                return res;
            }
        }
        res.result = -1;
        return res;
    }

    // 获取音频根目录下一级结点的id
    public long GetAudioCatalog()
    {
        long nID;
        if (mListParentFolder == null || mPlayParentFolder == null)
            return -1;

        if (mPlayParentFolder == mAudioRoot)
        {
            return -1;
        }

        TreeNode tmpCurrent = mPlayParentFolder;
        TreeNode tmpParent = mPlayParentFolder.GetParent();
        while (tmpParent != null)
        {
            if (tmpParent == mAudioRoot)
            {
                nID = tmpCurrent.GetNodeID();
                return nID;
            }
            tmpCurrent = tmpParent;
            tmpParent = tmpParent.GetParent();
            if (tmpParent == null)
                return -1;
        }
        return -1;
    }

    // 判断是否从列表中播放
    public boolean IsPlayingFromList()
    {
        if (mPlayParentFolder == null)
            return false;
        else
            return true;
    }

    public int GetChildSize()
    {
        if (mListParentFolder == null)
        {
            return 0;
        }
        return mListParentFolder.GetChildSize();
    }

    public void SetCurrentCategory(int category)
    {
        mCurrentCategoryCodeID = category;
    }

    public int GetCurrentCategory()
    {
        return mCurrentCategoryCodeID;
    }

    public ArrayList<SongData> GetCurListSongData()
    {
        ArrayList<SongData> listDatas = new ArrayList<SongData>();
        if (mListParentFolder != null)
        {
            ArrayList<TreeNode> tmpNodes = mListParentFolder.GetAllChildren();
            if (tmpNodes != null)
            {
                for (int i = 0; i < tmpNodes.size(); i++)
                {
                    SongData songData = new SongData();
                    songData.ID = tmpNodes.get(i).GetNodeID();
                    songData.SongName = tmpNodes.get(i).GetNodeName();
                    songData.IsSelected = false;
                    if (mListParentFolder == mAudioRoot)
                    {
                        switch (tmpNodes.get(i).GetNodeType())
                        // nTrackIndex
                        {
                            case IPOD_CATEGORY_TYPES.CATEGORY_PLAYLIST:
                                songData.fileType =
                                    SP_IPOD_LIST_FILE_TYPE.LIST_FILE_PlayList;
                                break;
                            case IPOD_CATEGORY_TYPES.CATEGORY_ARTIST:
                                songData.fileType =
                                    SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Artist;
                                break;
                            case IPOD_CATEGORY_TYPES.CATEGORY_ALBUM:
                                songData.fileType =
                                    SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Album;
                                break;
                            case IPOD_CATEGORY_TYPES.CATEGORY_GENRE:
                                songData.fileType =
                                    SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Genre;
                                break;
                            case IPOD_CATEGORY_TYPES.CATEGORY_TRACK:
                                songData.fileType =
                                    SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Songs;
                                break;
                            case IPOD_CATEGORY_TYPES.CATEGORY_COMPOSER:
                                songData.fileType =
                                    SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Composer;
                                break;
                            case IPOD_CATEGORY_TYPES.CATEGORY_AUDIOBOOK:
                                songData.fileType =
                                    SP_IPOD_LIST_FILE_TYPE.LIST_FILE_AudioBook;
                                break;
                            case IPOD_CATEGORY_TYPES.CATEGORY_PODCAST:
                                songData.fileType =
                                    SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Podcast;
                                break;
                            default:
                                break;
                        }
                    }
                    else
                    {
                        if (tmpNodes.get(i).GetNodeType() == IPOD_CATEGORY_TYPES.CATEGORY_TRACK
                            && mIsAudioPlay)
                        {
                            if (mListParentFolder == mPlayParentFolder
                                && mPlayingIndex == i)
                            {
                                songData.IsSelected = true;
                            }
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_AUDIO;
                        }
                        else if (tmpNodes.get(i).GetNodeType() == IPOD_CATEGORY_TYPES.CATEGORY_TRACK
                            && !mIsAudioPlay)
                        {
                            if (mListParentFolder == mPlayParentFolder
                                && mPlayingIndex == i)
                            {
                                songData.IsSelected = true;
                            }
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_VIDEO;
                        }
                        else if (tmpNodes.get(i).GetNodeType() == IPOD_CATEGORY_TYPES.CATEGORY_Movies
                            && !mIsAudioPlay)
                        {
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Movies;
                        }
                        else if (tmpNodes.get(i).GetNodeType() == IPOD_CATEGORY_TYPES.CATEGORY_MusicVideos
                            && !mIsAudioPlay)
                        {
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_MusicVideos;
                        }
                        else if (tmpNodes.get(i).GetNodeType() == IPOD_CATEGORY_TYPES.CATEGORY_TVShows
                            && !mIsAudioPlay)
                        {
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_TVShows;
                        }
                        else if (tmpNodes.get(i).GetNodeType() == IPOD_CATEGORY_TYPES.CATEGORY_VideoPodcasts
                            && !mIsAudioPlay)
                        {
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_VideoPodcasts;
                        }
                        else if (tmpNodes.get(i).GetNodeType() == IPOD_CATEGORY_TYPES.CATEGORY_Rentals
                            && !mIsAudioPlay)
                        {
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_Rentals;
                        }
                        else
                        {
                            songData.fileType =
                                SP_IPOD_LIST_FILE_TYPE.LIST_FILE_FOLDER;
                        }
                    }
                    listDatas.add(songData);
                }
            }
        }
        return listDatas;
    }
}
