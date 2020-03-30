package com.vsb.kru13.osmzhttpserver;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;


public class SocketServer extends Thread {

    private ServerSocket serverSocket;
    public final int port = 12345;
    private boolean bRunning;
    private Handler mHandler;
    private Semaphore sp;

    public SocketServer(Handler mHandler, int maxThreads) {
        this.mHandler = mHandler;
        this.sp = new Semaphore(maxThreads);
    }

    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }

    public void run() {
        try {
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;

            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                if(sp.tryAcquire()){
                    Thread client = new ClientThread(s, mHandler, sp);
                    client.start();
                    Log.d("semaphore", "Number of available threads: " + sp.availablePermits());
                }
                else{
                    Log.d("semaphore", "All threads used -> Server too busy.");
                }
            }
        }
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
                Log.d("SERVER", "Normal exit");
            else {
                Log.d("SERVER", "Error");

                e.printStackTrace();
            }
        }
        finally {
            serverSocket = null;
            bRunning = false;
        }
    }

}

