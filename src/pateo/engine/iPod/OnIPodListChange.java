/*
 * 文 件 名:  OnIPodListChange.java
 * 版    权:  Pateo Co., Ltd. Copyright YYYY-YYYY,  All rights reserved
 * 描    述:  <描述>
 * 修 改 人:  zixiangliu
 * 修改时间:  2014-6-19
 * 跟踪单号:  <跟踪单号>
 * 修改单号:  <修改单号>
 * 修改内容:  <修改内容>
 */
package pateo.engine.iPod;

import java.util.List;

import pateo.com.iPod.iPodType.SongData;

/**
 * 对当前的ipod的数据进行管理，数据更改了，通知界面
 * @author  zixiangliu
 * @version  [版本号, 2014-6-19]
 * @see  [相关类/方法]
 * @since  [产品/模块版本]
 */
public interface OnIPodListChange
{
    /**
     * Ipod 数据更改回调通知
     * @param songDatas 当前的ipod数据更改
     * @see [类、类#方法、类#成员]
     */
    public void onIPodListChange(List<SongData> songDatas);

    /**
     * 歌曲信息更改
     * @param selectSize 当前选中播放的歌曲的index
     * @see [类、类#方法、类#成员]
     */
    public void onIPodSongChange(int selectSize);

    /**
     * 当ipod状态更改的时候提示
     * @param currentState 当前的播放状态
     * @see [类、类#方法、类#成员]
     */
    public void onIPodStateChange(int currentState);

    /**
     * 当IPOD将要播放完成的时候
     * @see [类、类#方法、类#成员]
     */
    public void onIPodSongEnd();
}
