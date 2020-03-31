package com.vsb.kru13.osmzhttpserver;

import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;


public class ClientThread extends Thread {
    private Socket s;
    private Handler mHandler;
    private Semaphore sp;


    public ClientThread(Socket socket, Handler mHandler, Semaphore sp) {
        this.s = socket;
        this.mHandler = mHandler;
        this.sp = sp;

    }

    public static String checkIndex(File[] files) {
        if(files != null){
            for (File file : files) {
                if (file.getAbsolutePath().endsWith("index.html")) {
                    return file.getAbsolutePath();
                }
            }
        }
        return "";
    }
    private String getContentType(String path){
        if(path.endsWith(".png")){
            return "image/png";
        }
        else if(path.endsWith(".jpg")){
            return "image/jpeg";
        }
        else{
            return "text/html";
        }
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        }
        catch (IOException e) {
            Log.d("SERVER", "File Not Found");
            e.printStackTrace();
        }
        finally {
            if (fileIn != null)
                fileIn.close();
        }
        return fileData;
    }

    private void sendInfo(String method, String locator, long total){
        Timestamp timestamp = new Timestamp(new Date().getTime());
        Message msg = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("total", String.valueOf(total));
        bundle.putString("uri", locator);
        bundle.putString("http", method);
        bundle.putString("timestamp", timestamp.toString());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    public void run() {
        try {

            Log.d("ClientThread", "Client Socket");
            OutputStream o = s.getOutputStream();
            BufferedOutputStream dataOut = new BufferedOutputStream(o);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String[] sp;
            String methodType = "";
            String pathFile = "";
            String cgiURI = "/cgi-bin";
            String cameraURI = "/camera/snapshot";


            String tmp = in.readLine();
            if(tmp != null){
            if(!tmp.isEmpty()) {
                sp = tmp.split(" ");
                methodType = sp[0];
                pathFile = sp[1] == null ? "/" : sp[1];
                sp = tmp.split(" ");
                Log.d("FilePath", pathFile);
                while (!tmp.isEmpty()) {
                    Log.d("HTTP", "REQUEST : " + tmp);
                    tmp = in.readLine();
                }
            }
                //HTTP response pro CGI scripting
                if(pathFile.contains("/cgi-bin")) {
                    String command = pathFile.substring(9);
                    String[] commands = command.split("%20");
                    if (commands.length > 0) {
                        out.flush();
                        out.write("HTTP/1.0 200 OK\n" +
                                "Content-Type: text/plain\n"
                                + "\n");
                        out.flush();
                        ArrayList<String> arguments = new ArrayList<String>();
                        arguments.addAll(Arrays.asList(commands));
                        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
                        Process process = processBuilder.start();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1);

                        int part = 0;
                        while ((part = reader.read()) != -1) {
                            o.write(part);
                        }
                        o.flush();
                        process.destroy();

                    }
                }

                //HTTP response pro /camera/snapshot
                if(pathFile.equals("/camera/snapshot")){

                    if(CamActivity.getImageData() != null) {

                        out.flush();
                        out.write("HTTP/1.0 200 OK\n" +
                                "Content-Type: image/jpeg\n"
                                + "\n");
                        out.flush();
                        dataOut.write(CamActivity.getImageData());
                        dataOut.flush();
                    }
                }
                //HTTP response pro /camera/stream
                if (pathFile.equals("/camera/stream")) {

                    if (CamActivity.getImageData() != null) {
                        out.flush();
                        out.write("HTTP/1.0 200 OK\n" +
                                "Content-Type: multipart/x-mixed-replace; boundary=\"OSMZ_boundary\"\n\n");
                        while (CamActivity.isStreaming()) {
                            out.flush();
                            out.write("--OSMZ_boundary\n" +
                                    "Content-Type: image/jpeg\n\n");
                            out.flush();
                            dataOut.write(CamActivity.getImageData());
                            dataOut.flush();
                        }
                        dataOut.flush();
                        out.write("--OSMZ_boundary");
                        out.flush();
                    }
                }



            if(!pathFile.contains(cameraURI) || !pathFile.contains(cgiURI)){
            String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            File f = new File(sdPath + pathFile);
            String type = getContentType(f.getAbsolutePath());
            int fileLength;
            out.flush();
            if(!f.exists() && !pathFile.contains(cgiURI)){
                out.write("HTTP/1.0 404 Not found\n" +
                            "Content-Type: text/html\n" +
                            "Content-Length: " + f.length() + "\n" +
                            "\n" +
                            "<html>\n" +
                            "<body>\n" +
                            "<h1>404</h1></body></html>");
                out.flush();
                sendInfo(methodType, pathFile, f.length());

            }
            else{
                if(f.isFile()){
                    fileLength = (int) f.length();
                    byte[] data = readFileData(f, fileLength);
                    int i;
                    out.write("HTTP/1.0 200 OK\n" +
                                "Content-Type: " + type + "\n" +
                                "Content-Length: " + fileLength + "\n" +
                                "\n");
                    out.flush();
                    dataOut.write(data);
                    dataOut.flush();
                    sendInfo(methodType, pathFile, fileLength);
                }
                else{
                    File[] folderContent = new File(f.getAbsolutePath()).listFiles();
                    String path = checkIndex(folderContent);
                    File index = new File(path);
                    if(path.equals("") && folderContent != null){
                        fileLength = (int) f.length();
                        out.write("HTTP/1.0 200 OK\n" +
                                    "Content-Type: text/html\n" +
                                    "Content-Length: " + fileLength + "\n" +
                                    "\n");

                        out.flush();
                        out.write("<html>\n" + "<body><h1>Files in folder</h1\n<ul>");
                        for (File file: folderContent) {
                            out.write("<li><a href="+ '"' + file.getAbsolutePath() + '"' + ">" + file.getName()+"</a></li>");
                        }
                        out.write("</ul>\n</body>\n" + "</html>\n");
                        out.flush();
                        sendInfo(methodType, path, fileLength);
                    }
                    else{
                        fileLength = (int) index.length();
                        byte[] data = readFileData(index, fileLength);
                        out.write("HTTP/1.0 200 OK\n" +
                                    "Content-Type: " + type + "\n" +
                                    "Content-Length: " + fileLength + "\n" +
                                    "\n");
                        out.flush();
                        dataOut.write(data);
                        dataOut.flush();
                        sendInfo(methodType, pathFile, fileLength);
                    }
                }
            }
                out.flush();
            }

            }

                s.close();
                Log.d("ClientThread", "Socket Closed");
            }

        catch (IOException e) {
            if (s != null && s.isClosed())
                Log.d("ClientThread", "Normal exit");
            else {
                Log.d("ClientThread", "Error");
                e.printStackTrace();
            }
        }
        finally {
            sp.release();
            Log.d("semaphore", "Number of available threads: " + sp.availablePermits());
            s = null;
        }
    }


}
