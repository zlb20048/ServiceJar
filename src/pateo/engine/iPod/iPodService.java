package pateo.engine.iPod;

import pateo.com.global.Communication.Server;
import pateo.com.iPod.iPodDefine.iPodLogDefines;
import android.content.Intent;
import android.os.IBinder;

public class iPodService extends Server
{

    private final static String TAG = "iPodService";

    iPodServiceImplement miPodServiceImplement = null;

    @Override
    public IBinder GetServiceImplement(Intent intent)
    {

        return (IBinder)miPodServiceImplement;
    }

    @Override
    public void Create()
    {
        iPodLogDefines.iPodLog(TAG, "Create!");
        miPodServiceImplement = new iPodServiceImplement(this);
    }

    @Override
    public void Close()
    {
        iPodLogDefines.iPodLog(TAG, "Close!");
        miPodServiceImplement.Close();
        miPodServiceImplement = null;
    }

    @Override
    public void Destroy()
    {

        iPodLogDefines.iPodLog(TAG, "Destroy!");
    }

    @Override
    public void OnStartCommand(Intent intent)
    {
        iPodLogDefines.iPodLog(TAG, "OnStartCommand!");
        super.OnStartCommand(intent);

        miPodServiceImplement.OnStartCommand(intent);

    }

}
