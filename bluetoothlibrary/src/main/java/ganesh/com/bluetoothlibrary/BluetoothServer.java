package ganesh.com.bluetoothlibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import ganesh.com.bluetoothlibrary.bus.BluetoothCommunicator;
import ganesh.com.bluetoothlibrary.bus.ServeurConnectionFail;
import ganesh.com.bluetoothlibrary.bus.ServeurConnectionSuccess;

/**
 * Created by Rami MARTIN on 13/04/2014.
 */
public class BluetoothServer implements Runnable {

    private boolean CONTINUE_READ_WRITE = true;

    private UUID mUUID;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServerSocket mServerSocket;
    private BluetoothSocket mSocket;
    private InputStream mInputStream;
    private OutputStreamWriter mOutputStreamWriter;

    public BluetoothServer(BluetoothAdapter bluetoothAdapter, UUID uuid){
        mBluetoothAdapter = bluetoothAdapter;
        mUUID = uuid;
    }

    @Override
    public void run() {
        try {
            mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BLTServer", mUUID);
            mSocket = mServerSocket.accept();
            mInputStream = mSocket.getInputStream();
            mOutputStreamWriter = new OutputStreamWriter(mSocket.getOutputStream());

            int bufferSize = 1024;
            int bytesRead = -1;
            byte[] buffer = new byte[bufferSize];

            EventBus.getDefault().post(new ServeurConnectionSuccess());

            while(CONTINUE_READ_WRITE) {
                final StringBuilder sb = new StringBuilder();
                bytesRead = mInputStream.read(buffer);
                if (bytesRead != -1) {
                    String result = "";
                    while ((bytesRead == bufferSize) && (buffer[bufferSize] != 0)) {
                        result = result + new String(buffer, 0, bytesRead);
                        bytesRead = mInputStream.read(buffer);
                    }
                    result = result + new String(buffer, 0, bytesRead);
                    sb.append(result);
                }
                EventBus.getDefault().post(new BluetoothCommunicator(sb.toString()));

            }
        } catch (IOException e) {
            Log.e("", "ERROR : " + e.getMessage());
            EventBus.getDefault().post(new ServeurConnectionFail());
        }
    }

    public void write(String message) {
        try {
            mOutputStreamWriter.write(message);
            mOutputStreamWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnexion(){
        if(mSocket != null){
            try{
                mInputStream.close();
                mInputStream = null;
                mOutputStreamWriter.close();
                mOutputStreamWriter = null;
                mSocket.close();
                mSocket = null;
                mServerSocket.close();
            }catch(Exception e){}
            CONTINUE_READ_WRITE = false;
        }
    }
}
