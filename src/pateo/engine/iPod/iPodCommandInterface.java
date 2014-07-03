package pateo.engine.iPod;

import pateo.engine.iPod.iPodDataDef.RX_PACKET_PARAM;
import pateo.engine.iPod.iPodDataDef.TX_PACKET_PARAM;

public interface iPodCommandInterface
{
    public interface iPodCmdAnalysedInterface
    {
        public void OnRxAnalysedEnter(int rxCode, RX_PACKET_PARAM rxParam);
    }

    public interface iPodCmdCallBack
    {
        public boolean EnterCallBackFun(byte eventType, int arg1, Object data);
    }

    public interface iPodCmdSendInterface
    {
        public void SendCommand(int tpCode, TX_PACKET_PARAM rxParam);
    }

    public interface iPodWriteInterface
    {
        public void WriteCommand(byte[] buf, int chBufLen);
    }
}
