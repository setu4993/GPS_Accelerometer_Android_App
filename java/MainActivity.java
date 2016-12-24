package com.example.setu4.gps_accel_v2;

//importing all classes
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.StrictMode;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends Activity implements SensorEventListener
{
    //initializing sensor classes
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    //initializing TextView variables for output
    private TextView outputX;
    private TextView outputY;
    private TextView outputZ;
    private TextView gps_lat;
    private TextView gps_lon;
    private TextView gps_alt;

    //initializing EditText variables for processing input variables
    EditText gps_sr;
    EditText acc_sr;
    Button start;

    //initializing sampling rates for processing
    double accel_sampling_rate;
    long gps_sampling_rate;

    //initializing handler for running the GPS task
    Handler handler = new Handler();

    //initializing lat, long, alt values for storing and displaying within the main activity
    double latitude;
    double longitude;
    double altitude;

    //initializing the gpstrack class variable
    gpstrack gps;

    //initializing x, y, z values for storing and displaying within the main activity
    float last_x, last_y, last_z;
    long lastUpdate = 0;

    //initializing toggles for runtime
    boolean toggle = false;
    boolean gps_running = false;

    //initializing BufferedWriter for storing values into a file
    BufferedWriter writer;

    //initializing filepath for internal storage
    private String filepathd = "/storage/emulated/0/data";
    private String filepath;

    int i = 0, j = 0;

    //initializing max value of buffer
    int maxBufferValue = 90;

    //initalizing timestamp values for gps and accelerometer
    String gpsts, accts;

    //initializing server url which will store data
    private String SERVER_URL = "YOUR_SERVER_URL/upload.php";

    //initialize server response code
    int serverResponseCode = 0;

    //intializing the spinner class for input of max buffer value
    private Spinner spinner;

    //initializing boolean values for storing if data is to be transferred to the server,
    //if mobile data is allowed by by user, wifi data usage is allowed by user,
    //if wifi network is connected or not, mobile data is connected or not
    boolean data = false, mddata = false, wfdata = false, wfavl = false, mdavl = false;

    //initialize checkbox values
    private CheckBox mddata_tmp, wfdata_tmp;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //relating all input variables to layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        outputX = (TextView) findViewById(R.id.textViewAccX);
        outputY = (TextView) findViewById(R.id.textViewAccY);
        outputZ = (TextView) findViewById(R.id.textViewAccZ);

        gps_lat = (TextView) findViewById(R.id.textViewGPSLat);
        gps_lon = (TextView) findViewById(R.id.textViewGPSLong);
        gps_alt = (TextView) findViewById(R.id.textViewGPSAlt);

        gps_sr = (EditText)findViewById(R.id.editTextGPS_SR);
        acc_sr = (EditText)findViewById(R.id.editTextAccel_SR);

        start = (Button) findViewById(R.id.buttonStart);

        spinner = (Spinner) findViewById(R.id.spinner);

        mddata_tmp = (CheckBox) findViewById(R.id.checkBoxMD);
        wfdata_tmp = (CheckBox) findViewById(R.id.checkBoxWF);

        //initializing sensor manager
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //initializing connectivity manager and getting the network connection information
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
        {
            wfavl = true;
        }
        else if(networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
        {
            mdavl = true;
        }

        //to use internet connection within the main class
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //waiting for button click
        start.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        //storing input values as strings for comparison
                        start.setText("Stop Data Collection");
                        String gps_sr_tmp = gps_sr.getText().toString();
                        String acc_sr_tmp = acc_sr.getText().toString();


                        //setting max value of buffer based on selection of spinner
                        String buf_tmp = String.valueOf(spinner.getSelectedItem());
                        switch (buf_tmp)
                        {
                            case "75%":
                                maxBufferValue = 75;
                                break;
                            case "80%":
                                maxBufferValue = 80;
                                break;
                            case "85%":
                                maxBufferValue = 85;
                                break;
                            case "90%":
                                maxBufferValue = 90;
                                break;
                            case "95%":
                                maxBufferValue = 95;
                                break;
                            case "100%":
                                maxBufferValue = 100;
                                break;
                            default:
                                maxBufferValue = 90;
                                break;
                        }

                        //setting gps sampling rate
                        if (gps_sr_tmp == null || gps_sr_tmp.isEmpty())
                        {
                            gps_sampling_rate = 1000;
                        }
                        else
                        {
                            gps_sampling_rate = Long.parseLong(gps_sr_tmp);
                        }

                        //setting accelerometer sampling rate
                        if (acc_sr_tmp == null || acc_sr_tmp.isEmpty())
                        {
                            accel_sampling_rate = 1000;

                        }
                        else
                        {
                            accel_sampling_rate = Double.parseDouble(acc_sr_tmp);
                        }

                        //setting data transfer to server via mobile data / wifi
                        if (mddata_tmp.isChecked() && mdavl)
                        {
                            data = true;
                            mddata = true;
                        }
                        else if(wfdata_tmp.isChecked() && wfavl)
                        {
                                data = true;
                                wfdata = true;
                        }
                        else if(wfdata_tmp.isChecked() && mddata_tmp.isChecked() && mdavl && wfavl)
                        {
                            data = true;
                            mddata = true;
                            wfdata = true;
                        }

                        //runnable for getting gps values repeatedly
                        Runnable runnableCode = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                gps = new gpstrack(MainActivity.this);
                                latitude = gps.getLatitude();
                                longitude = gps.getLongitude();
                                altitude = gps.getAltitude();
                                setCurTime("G");
                                writeFile("G", gpsts);
                                gps_lat.setText("Lati: " + Double.toString(latitude));
                                gps_lon.setText("Long: " + Double.toString(longitude));
                                gps_alt.setText("Alti: " + Double.toString(altitude));
                                Log.d("Handlers", "Called on main thread");
                                if(gps_running)
                                {
                                    handler.postDelayed(this, gps_sampling_rate); //calling the handler again after a delay of gps_sampling_rate (in ms)
                                }
                                else
                                {
                                    handler.removeCallbacks(this); //stop the handler
                                    clearValues();
                                }
                            }
                        };

                        //stop the data collection
                        if(toggle == false)
                        {

                            start.setText("Start Data Collection");
                            gps_running = false;
                            onPause();
                            toggle = true;
                            j = 0;
                        }

                        //start the data collection
                        else
                        {
                            senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                            onResume();
                            toggle = false;
                            gps_running = true;
                            handler.post(runnableCode);

                        }
                    }
                });


    }
    @Override

    //get accelerometer values
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        Sensor mySensor = sensorEvent.sensor;

        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        long curTime = 0;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            z = sensorEvent.values[2];
            x = sensorEvent.values[0];
            y = sensorEvent.values[1];
            curTime = System.currentTimeMillis();
        }

        if ((curTime - lastUpdate) > (accel_sampling_rate))
        {

            outputX.setText("x: " + Float.toString(x));
            outputY.setText("y: " + Float.toString(y));
            outputZ.setText("z: " + Float.toString(z));
            lastUpdate = curTime;
            last_x = x;
            last_y = y;
            last_z = z;
            setCurTime("A");
            writeFile("A", accts);
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //to pause the sensor readings
    protected void onPause()
    {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    //to resume the sensor readings
    protected void onResume()
    {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //clear all the values of display
    public void clearValues()
    {
        outputX.setText("0.0");
        outputY.setText("0.0");
        outputZ.setText("0.0");
        gps_lat.setText("0.0");
        gps_lon.setText("0.0");
        gps_alt.setText("0.0");
    }

    //write the values to internal storage
    private void writeFile(String var, String time_stamp)
    {
        filepath = filepathd + "-" + j + ".txt";
        try
        {
            if(i == 0)
            {
                writer = new BufferedWriter(new FileWriter(filepath)); //create new file
            }
            else
            {
                writer = new BufferedWriter(new FileWriter(filepath, true)); //append to existing file
            }

            //add to file
            if(var == "A")
            {
                writer.write(time_stamp + " " + var +  " [" + last_x + " " + last_y + " " + last_z + "]");
                i++;
            }
            else if (var == "G")
            {
                writer.write(time_stamp + " " + var + " [" + latitude + " " + longitude + " " + altitude + "]");
                i++;
            }
            writer.newLine();
            writer.flush();

            //transfer data to the server if max value is reached
            if(i == maxBufferValue)
            {
                if(data)
                {
                    if(wfdata && mddata) //use mobile data or wifi
                    {
                        uploadFile(filepath);
                        i = 0;
                    }
                    else if(mddata) //use only mobile data
                    {
                        uploadFile(filepath);
                        i = 0;
                    }
                    else if(wfdata) //use only wifi
                    {
                        uploadFile(filepath);
                        i = 0;
                    }
                }
                else
                {
                    i = 0;
                    j = 0;
                }
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //get current time
    private void setCurTime(String var)
    {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd z HH:mm:ss.SSS");

        if (var == "A")
        {
            accts = df1.format(c.getTime());
        }
        else if(var == "G")
        {
            gpsts = df1.format(c.getTime());
        }
    }

    //connect to a server and upload the file to it using an HTTP connection
    public int uploadFile(String sourceFileUri)
    {
        String fileName = sourceFileUri;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile())
        {
            return 0;
        }
        else
        {
            try
            {
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(SERVER_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                j++;

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0)
                {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
                Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200) //successful transmission
                {
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            String msg = "File upload to server completed.";
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
                //close the streams
                fileInputStream.close();
                dos.flush();
                dos.close();
            }
            catch (MalformedURLException ex)
            {
                ex.printStackTrace();
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.e("Upload file to server Exception", "Exception : " + e.getMessage(), e);
            }
            return serverResponseCode;
        }
    }
}
