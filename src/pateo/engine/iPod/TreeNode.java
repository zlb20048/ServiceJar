package pateo.engine.iPod;

import java.util.ArrayList;

import pateo.engine.iPod.iPodDataDef.IPOD_CATEGORY_TYPES;
import pateo.engine.iPod.iPodDataDef.IPOD_RET_RESULT_VALUE;

public class TreeNode
{
    private ArrayList<TreeNode> mlist; // 每个节点下都用一个vector来存放各个子节点

    private TreeNode mParent;// 该节点的父节点

    private long mNodeID;// 该节点的MpegID,root节点的m_MpegID为0，文件夹和文件分开依次编号，用m_IsFolder进行区分

    private String mNodeName;// 该节点的名称

    private byte mNodeType;// 该节点的文件类型

    private long mChildCapacity;// 该节点下所有节点的数目

    public TreeNode(TreeNode parent)
    {
        // 每个节点下都用一个vector来存放各个子节点
        mlist = new ArrayList<TreeNode>();
        // 该节点的父节点
        mParent = parent;
        // 该节点的MpegID,root节点的m_MpegID为0，文件夹和文件分开依次编号，用m_IsFolder进行区分
        mNodeID = 0xffffffL;
        // 该节点的名称
        mNodeName = "";
        // 该节点的文件类型
        mNodeType = IPOD_CATEGORY_TYPES.CATEGORY_NOTHING;
        // 该节点下所有节点的数目
        mChildCapacity = 0;
    }

    public TreeNode()
    {
        // 每个节点下都用一个vector来存放各个子节点
        mlist = new ArrayList<TreeNode>();
        // 该节点的父节点
        mParent = null;
        // 该节点的MpegID,root节点的m_MpegID为0，文件夹和文件分开依次编号，用m_IsFolder进行区分
        mNodeID = 0xffffffL;
        // 该节点的名称
        mNodeName = "";
        // 该节点的文件类型
        mNodeType = IPOD_CATEGORY_TYPES.CATEGORY_NOTHING;
        // 该节点下所有节点的数目
        mChildCapacity = 0;
    }

    public void SetParent(TreeNode parent)
    {
        mParent = parent;
    }

    public TreeNode GetParent()
    {
        return mParent;
    }

    public TreeNode AddChild(long id, String name, byte categorytype)
    {
        IPOD_RET_RESULT_VALUE position;
        position = GetChildPosition(id);
        int nRes = position.result;
        if (nRes == 2) // Update node
        {
            TreeNode _node = mlist.get((Integer)position.value);
            if (_node.mNodeID == id)
            {
                _node.mNodeName = name;
                _node.mNodeType = categorytype;
                return mlist.get((Integer)position.value);
            }
            return null;
        }
        else if (nRes == 1) // insert
        {
            TreeNode _node = new TreeNode(this);
            _node.mNodeID = id;
            _node.mNodeName = name;
            _node.mNodeType = categorytype;
            mlist.add((Integer)position.value, _node);
            return _node;
        }
        else
        // push back
        {
            TreeNode _node = new TreeNode(this);
            _node.mNodeID = id;
            _node.mNodeName = name;
            _node.mNodeType = categorytype;
            mlist.add(_node);
            return _node;
        }
    }

    public TreeNode GetChild(long index)
    {
        if (mlist.size() == 0 || mlist == null)
            return null;
        int nPos = mlist.size();
        if (mlist.get(0) != null && index < mlist.get(0).mNodeID
            || mlist.get(nPos - 1) != null
            && index > mlist.get(nPos - 1).mNodeID)
            return null;

        int nFirst = 0;
        int nLast = nPos - 1;
        int nMid;

        while (nFirst <= nLast)
        {
            nMid = (nFirst + nLast) / 2;

            if (mlist.get(nMid).mNodeID == index)
                return mlist.get(nMid);
            else if (mlist.get(nMid).mNodeID > index)
                nLast = nMid - 1;
            else
                nFirst = nMid + 1;
        }

        // 未找到
        return null;
    }

    public void SetNodeID(long id)
    {
        mNodeID = id;// 该节点的MpegID,root节点的m_MpegID为0，文件夹和文件分开依次编号，用m_IsFolder进行区分
    }

    public long GetNodeID()
    {
        return mNodeID;
    }

    public void SetNodeName(String name)
    {
        mNodeName = name;// 该节点的名称
    }

    public String GetNodeName()
    {
        return mNodeName;
    }

    public void SetNodeType(byte type)
    {
        mNodeType = type;// 该节点的文件类型
    }

    public byte GetNodeType()
    {
        return mNodeType;
    }

    public void SetChildCapacity(long count)
    {
        mChildCapacity = count;// 该节点下所有节点的数目
    }

    public long GetChildCapacity()
    {
        return mChildCapacity;
    }

    public void Clear()
    {
        for (int i = 0; i < mlist.size(); i++)
        {
            TreeNode node = mlist.get(i);
            node.Clear();
        }
        mlist.clear();
    }

    // 折半查找定位node位置
    private IPOD_RET_RESULT_VALUE GetChildPosition(long _ID)
    {
        IPOD_RET_RESULT_VALUE res = new IPOD_RET_RESULT_VALUE();
        if (mlist.size() == 0)
        {
            res.result = 0; // push back
            return res;
        }
        int nPos = mlist.size();
        if (_ID < mlist.get(0).mNodeID)
        {
            res.value = 0;
            res.result = 1; // insert
            return res;
        }
        else if (_ID > mlist.get(nPos - 1).mNodeID)
        {
            res.value = nPos - 1;
            res.result = 0; // push back
            return res;
        }

        int nFirst = 0;
        int nLast = nPos - 1;
        int nMid;

        while (nFirst <= nLast)
        {
            nMid = (nFirst + nLast) / 2;

            if (mlist.get(nMid).mNodeID == _ID)
            {
                res.value = nMid;
                res.result = 2; // update node
                return res;
            }
            else if (mlist.get(nMid).mNodeID > _ID)
                nLast = nMid - 1;
            else
                nFirst = nMid + 1;
        }

        res.value = nFirst;
        res.result = 1; // insert
        return res;
    }

    public int GetChildSize()
    {
        return mlist.size();
    }

    public ArrayList<TreeNode> GetAllChildren()
    {
        return mlist;
    }
}
