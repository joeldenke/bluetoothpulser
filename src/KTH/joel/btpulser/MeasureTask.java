package KTH.joel.btpulser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Environment;

public class MeasureTask extends AsyncTask<Void, String, String>
{
    // The byte sequence to set sensor to a specific (and obsolete) format
    private static final byte[] FORMAT = { 0x02, 0x70, 0x04, 0x02, 0x02, 0x00, 0x78, 0x03};
    private static final byte ACK = 0x06; // ACK from Nonin sensor
    private static final UUID STANDARD_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BTClient client;
    private BluetoothDevice btDevice;
    private boolean run = true;
    BluetoothSocket socket = null;
    BluetoothAdapter adapter;

    public MeasureTask(BTClient client, BluetoothDevice btDevice)
    {
        this.client = client;
        this.btDevice = btDevice;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
    }


    public File getFileResource()
    {
        String path;

        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            path = Environment.getExternalStorageDirectory().getPath();
        } else {
            path = client.getFilesDir().getPath();
        }

        return new File(path + '/' + client.getPreference(AVAILABLE_PREFERENCE.FILENAME));
    }

    @Override
    protected String doInBackground(Void... params)
    {
        String result = "";
        // Cancel all bluetooth discovery battery draining.
        adapter.cancelDiscovery();
        FileWriter writer = null;
        InputStream is = null;
        OutputStream os = null;

        try {
            socket = btDevice.createInsecureRfcommSocketToServiceRecord(STANDARD_SPP_UUID);
            socket.connect();
            is = socket.getInputStream();
            os = socket.getOutputStream();

            writer =  new FileWriter(getFileResource(), true);
            os.write(FORMAT); // change the output stream format, to use same byte sequence as the nonin device
            os.flush();
            byte[] status = new byte[1];
            // read first byte
            is.read(status);
            long startTime = System.currentTimeMillis();
            long measureTime = Long.parseLong(client.getPreference(AVAILABLE_PREFERENCE.MEASURETIME));

            if (status[0] == ACK) {
                while (System.currentTimeMillis() - startTime < measureTime && run) {
                    // read 5 byte
                    byte[] frame = new byte[5];
                    is.read(frame);
                    // Convert raw byte data to java int
                    int value = unsignedByteToInt(frame[2]);
                    result = String.format("PLETH DATA: %d \r\n", value);
                    publishProgress(result);

                    writer.write(result);
                    writer.flush();
                }
            }

            is.close();
        } catch (Exception e) {
            publishProgress("Lost connection");
        } finally {
            try {
                 if (socket != null) socket.close();
                 if (writer != null) writer.close();
                 if (is != null)  is.close();
                 if (os != null) os.close();

            } catch (IOException e) {}
        }
        return result;
    }

    @Override
    protected void onCancelled()
    {
        super.onCancelled();
        run = false;
    }

    @Override
    protected void onProgressUpdate(String... result)
    {
        super.onProgressUpdate(result);
        client.setResponse(result[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

    }

    /**
     * Convert unsigned byte to int
     * @param b
     * @return
     */
    private int unsignedByteToInt(byte b)
    {
        return (int) b & 0xFF;
    }
}
