package com.vsb.kru13.osmzhttpserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SocketServer s;
    private static final int READ_EXTERNAL_STORAGE = 1;
    private Handler mHandler;
    private long totalSum = 0;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String uri = bundle.getString("uri", "");
                String httpMethod = bundle.getString("http", "");
                String timestamp = bundle.getString("timestamp", "");
                String size = bundle.getString("total", "");
                totalSum += Integer.parseInt(size);
                String totalDataTransfer = "Total data transfer: " + totalSum + " B";
                String resultRow = String.format("[%s]\t%s\t%s\t%s", timestamp, httpMethod, uri, size);
                TextView total = (TextView)findViewById(R.id.total);
                LinearLayout ll = (LinearLayout) findViewById(R.id.linear);
                TextView tv = new TextView(getApplicationContext());
                tv.setTextSize(12);
                tv.setText(resultRow);
                ll.addView(tv);
                total.setText(totalDataTransfer);
            }
        };

        Button btn1 = (Button)findViewById(R.id.button1);
        Button btn2 = (Button)findViewById(R.id.button2);
        Button cameraActivBtn = (Button)findViewById(R.id.cam);
        cameraActivBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), CamActivity.class));
            }
        });
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);




    }


    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.button1) {

            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
            } else {
                EditText thread = (EditText) findViewById(R.id.thread);
                int maxThreads = Integer.valueOf(thread.getText().toString());
                if(thread.getText().toString().isEmpty()){
                    Toast.makeText(this,"Number of threads: ", Toast.LENGTH_LONG).show();
                }
                if(maxThreads < 1){
                    Toast.makeText(this, "Bad threads value", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(this, "Server started with " + maxThreads + " threads.", Toast.LENGTH_LONG).show();
                    s = new SocketServer(mHandler, maxThreads);
                    s.start();
                }
            }
        }
        if (v.getId() == R.id.button2) {
            s.close();
            try {
                s.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case READ_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    EditText thread = (EditText) findViewById(R.id.thread);
                    int maxThreads = Integer.valueOf(thread.getText().toString());
                    if(maxThreads < 1){
                        Toast.makeText(this, "Bad threads value or not specified", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(this, "Server started with " + maxThreads + " threads.", Toast.LENGTH_LONG).show();
                        s = new SocketServer(mHandler, maxThreads);
                        s.start();
                    }
                }
                break;

            default:
                break;
        }
    }
}
