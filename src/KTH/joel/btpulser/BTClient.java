package KTH.joel.btpulser;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.util.Set;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

enum AVAILABLE_PREFERENCE
{
    FILENAME, SERVERIP, SERVERPORT, MEASURETIME
}

enum TASKS
{
    SEND, RECEIVE
}

/**
 * @description Main activity, the bluetooth client
 * @author Joel Denke, Mathias Westman
 *
 */
public class BTClient extends Activity  implements View.OnClickListener
{
    private static final int RESULT_SETTINGS = 10;
    private static final int REQUEST_ENABLE_BT = 20;
    private static final int REQUEST_PAIR_NONIN = 30;

    private BluetoothDevice btDevice = null;
    private BluetoothAdapter btAdapter;
    private MeasureTask receiveTask;
    private SendToServerTask sendTask;

    private TextView textView;

    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeries pulseSeries;
    private XYSeriesRenderer mpulseRenderer;

    private double currentX = 0;

    /**
     * @description Initiates the graph chart
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    private void initChart()
    {
        pulseSeries = new XYSeries("Pulse");
        mDataset.addSeries(pulseSeries);
        mpulseRenderer = new XYSeriesRenderer();
        mpulseRenderer.setColor(Color.GREEN);
        mRenderer.addSeriesRenderer(mpulseRenderer);
        mRenderer.setBackgroundColor(Color.BLACK);
        mRenderer.setApplyBackgroundColor(true);
    }

    /**
     * @description Publish a new pleth (blood flow) value and view on graph
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    public void publishPleth(int data, double deltaT)
    {
        pulseSeries.add(currentX, data);
        currentX += deltaT;
        mChart.repaint();
    }

    /**
     * @description Create UI and init bluetooth
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
	    super.onCreate(savedInstanceState);
        initUI();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        initBluetooth();
    }

    /**
     * @description Initiate asynchronous background task.
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    public void startTask(TASKS type)
    {
        switch (type) {
            case RECEIVE:
                if (receiveTask != null) receiveTask.cancel(true);
                receiveTask = new MeasureTask(this, btDevice);
                receiveTask.execute();
                break;
            case SEND:
                if (sendTask != null) sendTask.cancel(true);
                sendTask = new SendToServerTask(this);
                sendTask.execute();
                break;
        }
    }

    /**
     * @description Get evailable shared preferenc
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    public synchronized String getPreference(AVAILABLE_PREFERENCE pref)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        switch (pref) {
            case FILENAME:
                return sharedPrefs.getString("prefFilename", "data.txt");
            case SERVERIP:
                return sharedPrefs.getString("prefServerIp", "127.0.0.1");
            case SERVERPORT:
                return sharedPrefs.getString("prefServerPort", "6667");
            case MEASURETIME:
                return sharedPrefs.getString("prefMeasureTime", "30000");
            default:
                return "";
        }
    }

    public void viewMessage(int resId, boolean flash)
    {
        viewMessage(getString(resId), flash);
    }

    /**
     * @description view message on the screen
     * @author Joel Denke, Mathias Westman
     *
     */
    public synchronized void viewMessage(String message, boolean flash)
    {
        textView.setText(message);
        if (flash)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * @description Set response to text field
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    public synchronized void setResponse(String response)
    {
        textView.setText(response);
    }

    /**
     * @description Try to initiate bluetooth
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    private void initBluetooth()
    {
        if (btAdapter == null) {
            viewMessage("This application requires that your device has bluetooth", true);
            this.finish();
            return;
        }

        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            getBTDevice();
        }
    }

    /**
     * @description Fetch first available Nonon Bluetooth device, if any paired yet
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    private void getBTDevice()
    {
        btDevice = null;
        Set<BluetoothDevice> pairedBTDevices = btAdapter.getBondedDevices();

        if (pairedBTDevices.size() > 0) {
            // Scan paired bluetooth devices for Nonin
            for (BluetoothDevice device : pairedBTDevices) {
                String name = device.getName();
                if (name.contains("Nonin")) {
                    btDevice = device;
                    viewMessage(String.format("Paired device: %s", name), true);
                    return;
                }
            }
        }
        if (btDevice == null) {
            viewMessage("No paired Nonin devices found!\nPlease pair a Nonin BT device with this device.", true);
            Intent pairDevice = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivityForResult(pairDevice, REQUEST_PAIR_NONIN);
        }
    }

    /**
     * @description Initiaties GUI components
     * @author Joel Denke, Mathias Westman
     *
     */
    private void initUI()
    {
        // init the GUI
        setTitle(R.string.mainTitle);
        setContentView(R.layout.main);
        LinearLayout context = (LinearLayout)findViewById(R.id.context);
        context.setBackgroundColor(Color.BLACK);

        textView = (TextView) findViewById(R.id.textUserSettings);
        textView.setTextColor(Color.WHITE);

        Button start = (Button)findViewById(R.id.startMeasure);
        start.setOnClickListener(this);

        Button stopMeasure = (Button)findViewById(R.id.stopMeasure);
        stopMeasure.setOnClickListener(this);

        Button clearChart = (Button)findViewById(R.id.clearChart);
        clearChart.setOnClickListener(this);

        Button sendData = (Button)findViewById(R.id.sendData);
        sendData.setOnClickListener(this);
    }

    /**
     * @description Handle button click events
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.startMeasure:
                if (btDevice != null) {
                    viewMessage("Starting measure task ...", true);
                    startTask(TASKS.RECEIVE);
                } else {
                    initBluetooth();
                }
                break;
            case R.id.stopMeasure:
                viewMessage(".. stops measuring", true);
                if (receiveTask != null) receiveTask.cancel(true);
                break;
            case R.id.clearChart:
                clearChart();
                break;
            case R.id.sendData:
                startTask(TASKS.SEND);
                break;
        }
    }

    /**
     * @description When preference activity is finished
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_PAIR_NONIN :
                getBTDevice();
                break;
            case REQUEST_ENABLE_BT :
                if (btAdapter.isEnabled()) {
                    initBluetooth();
                } else {
                    viewMessage("Bluetooth is turned off.", true);
                }
                initBluetooth();
                break;
            case RESULT_SETTINGS:
                // Maybe do something here later, when you get back from settings
                break;

        }

    }

    /**
     * @description Create menu from xml
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    /**
     * @description Menu actions
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;
        }

        return true;
    }

    /**
     * @description Clear everything in the chart
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    public void clearChart()
    {
        pulseSeries.clear();
        mChart.repaint();
    }

    /**
     * @description Setup the layout of the chart
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    private void setupChartSize()
    {
        LinearLayout surface = (LinearLayout)findViewById(R.id.chart);
        surface.setBackgroundColor(Color.BLACK);
        ViewGroup.LayoutParams params = surface.getLayoutParams();

        params.width = 500;
        params.height = 300;
    }

    /**
     * @description Initializes chart on resume
     *
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    public void onResume()
    {
        Log.i("SaveActivity", "onStart called");
    	super.onResume();

        setupChartSize();

        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        if (mChart == null) {
            initChart();
            mChart = ChartFactory.getCubeLineChartView(this, mDataset, mRenderer, 0.3f);
            layout.addView(mChart);
        } else {
            mChart.repaint();
        }
    }

    /**
     * @description Listen for configuration changes and reload config as well as the gui
     * @author Joel Denke
     *
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        //viewMessage(getString(R.string.changedLang) + ": " + newConfig.locale.getLanguage(), true);

        getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
        initUI();
    }

    /**
     * @description Write game data when app is pausing, stop or gets killed
     * @author Joel Denke
     *
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        Log.i("SaveActivity", "onPause called");
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.i("SaveActivity", "onStop called");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.i("SaveActivity", "onDestroy called");
    }
}
