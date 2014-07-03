package pateo.com.iPod;

import pateo.com.audio.operate.BaseMediaInterface;
import pateo.com.global.Communication.ClientCallback;
import pateo.com.global.Communication.ServiceInterface;
import pateo.com.iPod.iPodDefine.SP_IPOD_DATA_ENUM;
import pateo.com.iPod.iPodDefine.SP_IPOD_LIST_CATEGORY_TYPE;
import pateo.com.iPod.iPodDefine.SP_MEDIA_TYPE_ENUM;
import pateo.com.iPod.iPodType.SP_IPOD_OSD;
import pateo.engine.iPod.OnIPodListChange;
import pateo.engine.iPod.iPodConnectHelper.ConnectStateCallback;

public abstract class iPodServiceInterface extends BaseMediaInterface implements
    ServiceInterface
{
    /*-------------------------------------------------------*
    * Function		: SP_IPOD_Init                
    * Description	: initial SP_IPOD.dll
    * Parameters	: 
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_Init(SP_IPOD_OSD sOSD);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_DeInit                
    * Description	: uninstall SP_IPOD.dll
    * Parameters	: BOOL bExist
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_DeInit();

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_Connect                
    * Description	: connect and set media type
    * Parameters	: SP_MEDIA_TYPE_ENUM eMediaType:media type
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_Connect(SP_MEDIA_TYPE_ENUM eMediaType);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_ChangeOSD               
    * Description	: change OSD
    * Parameters	: SP_IPOD_OSD sListOSD:Specifies OSD
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_ChangeOSD(SP_IPOD_OSD sOSD);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_PlayPauseCurrentTrack                
    * Description	: play/pause current track
    * Parameters	: void
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_PlayPauseCurrentTrack();

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_StopCurrentTrack                
    * Description	: stop current track
    * Parameters	: void
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_StopCurrentTrack();

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_REW                
    * Description	: start REW
    * Parameters	: void
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_ENDFForREW();

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_Shuffle                
    * Description	: switch shuffle mode between shuffle off
    *				  and shuffle tracks
    * Parameters	: SP_IPOD_REF_SHUFFLE_STATE_ENUM sShuffle:shuffle
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_Shuffle(byte sShuffle);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_Repeat                
    * Description	: switch repeat mode between repeat off,
    *				  repeat on track and repeat all tracks
    * Parameters	: SP_IPOD_REF_REPEAT_STATE_ENUM sRepeat:repeat
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_Repeat(byte sRepeat);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_SelectTrackPlay               
    * Description	: select one track to play
    * Parameters	: UINT uId:Specifies the index which will
    *						play
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_SelectTrackPlay(long uId);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_ReturnRootList               
    * Description	: return root list
    * Parameters	: void
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_ReturnRootList();

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_ReturnParentList               
    * Description	: return parent list
    * Parameters	: BOOL bIsFromListStart:If from list start
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_ReturnParentList();

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_GoChildrenList               
    * Description	: go children list
    * Parameters	: UINT uId
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_GoChildrenList(long uId);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_GoCategoryTypeList               
    * Description	: go category type list
    * Parameters	: SP_IPOD_LIST_CATEGORY_TYPE eCategoryType:
    *				  Category Type
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_GoCategoryTypeList(
        SP_IPOD_LIST_CATEGORY_TYPE eCategoryType);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_SetVolume              
    * Description	: Set Volume
    * Parameters	: SP_IPOD_VOLUME_ENUM eVolume:volume
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_SetVolume(byte eVolume);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_ABC123Search             
    * Description	: Search list
    * Parameters	:
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_ABC123Search(String wsSearchStr,
        boolean bNeedInMatching);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_ABC123Search             
    * Description	: Search list
    * Parameters	: SP_IPOD_ABC123_SEARCH_ENUM eABC123:ABC123
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_ABC123Search(int eABC123);

    /*-------------------------------------------------------*
    * Function		: SP_IPOD_ReqChildren
    * Description	: Request Children
    * Parameters	: void
    * Return		: int:the type is SP_SUCCESS_ERROR_ENUM
    *--------------------------------------------------------*/
    public abstract int SP_IPOD_ReqChildren(long uStart, long uReqNum);

    public abstract int SP_IPOD_ReqCurList();

    public abstract int SP_IPOD_SyncList();

    public abstract int SP_IPOD_GetDataType(SP_IPOD_DATA_ENUM eData);

    /** {@inheritDoc} */

    public abstract boolean RegisterClient(ClientCallback Callback);

    /** 注册当前的状态回调方法
     * @param connectStateCallback 状态更改回调
     * @see [类、类#方法、类#成员]
     */
    public abstract void addConnectStateCallback(
        ConnectStateCallback connectStateCallback);

    /**
     * Ipod状态更改的消息
     * @param onIPodListChange 监听Ipod相关状态
     * @see [类、类#方法、类#成员]
     */
    public abstract void addListChangeListener(OnIPodListChange onIPodListChange);

    /**
     * 销毁当前的ipod
     * @see [类、类#方法、类#成员]
     */
    public abstract void destoryIpod();
}
