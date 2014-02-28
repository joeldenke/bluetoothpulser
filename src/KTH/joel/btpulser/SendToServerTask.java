package KTH.joel.btpulser;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 * @description Async task to send data from file to a server over TCP socket
 * @author      Joel Denke, Mathias Westman
 */
public class SendToServerTask extends AsyncTask<Void, String, String>
{
    private BTClient client;
    private Socket socket;
    private DataOutputStream dataOs;

    public SendToServerTask(BTClient client)
    {
        this.client = client;
    }

    /**
     * @description Get file from SD card if availabe, else use internal storage.
     * @author      Joel Denke, Mathias Westman
     */
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

    /**
     * @description Send data to server with TCP socket, in background
     * @author      Joel Denke, Mathias Westman
     */
    @Override
    protected String doInBackground(Void... params)
    {
        String result = "";
        File f = getFileResource();
        byte[] data = new byte[(int) f.length()];

        try {
            new FileInputStream(f).read(data);
        } catch (Exception e) {
            result = e.toString();
        }

        String serverIP = client.getPreference(AVAILABLE_PREFERENCE.SERVERIP);
        int serverPort  = Integer.parseInt(client.getPreference(AVAILABLE_PREFERENCE.SERVERPORT));

        try {
            socket = new Socket(InetAddress.getByName(serverIP), serverPort);

            publishProgress("Start sending data to server...");

            dataOs = new DataOutputStream(socket.getOutputStream());
            dataOs.write(data, 0, data.length);
            dataOs.flush();
            result = String.format("Successfully sent %d bytes to server", data.length);
        } catch (Exception e) {
            result = e.getMessage();
        } finally {
            try {
                if (dataOs != null) dataOs.close();
                if (socket != null) socket.close();
            } catch (IOException e) {}
        }

        return result;
    }

    /**
     * @description When new progress is made, set response
     * @author      Joel Denke, Mathias Westman
     */
    @Override
    protected void onProgressUpdate(String... result) {
        super.onProgressUpdate(result);
        // set response text field while the thread is running
        client.setResponse(result[0]);
    }

    /**
     * @description Set end result after background task is executed
     * @author      Joel Denke, Mathias Westman
     */
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        client.setResponse(result);
    }
}
