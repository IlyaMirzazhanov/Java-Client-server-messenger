package com.example.mes;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.app.Notification.Builder;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;

//@author Ilya Mirzazhanov
public class Listener extends Service {
    final String LOG_TAG = "myLogs";
    String STATE = "android.net.conn.CONNECTIVITY_CHANGE";
    Socket clientSocket;
    DataInputStream inFromServer;
    String line;
    BroadcastReceiver networkStateReceiver;
    public String nm;
    DataOutputStream outToServer;
    public String ps;

    class ConnectToServer extends AsyncTask<Void, Void, Void> {
        ConnectToServer() {
        }

        /* Access modifiers changed, original: protected */
        public void onPreExecute() {
        }

        /* Access modifiers changed, original: protected|varargs */
        public Void doInBackground(Void... v) {
            try {
                String address = InetAddress.getByName("YOUR_IP").getHostAddress(); // ip in string
                clientSocket = new Socket(address, 1700); // choose port
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                inFromServer = new DataInputStream(clientSocket.getInputStream());
                logIn();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public void onPostExecute(Void result) {
        }

        public void onProgressUpdate(Void... values) {
        }
    }

    class ListenFromServer extends AsyncTask<String, String, String> {

        class ReceiveMessage extends Thread {
            DataInputStream in;

            ReceiveMessage(DataInputStream in) {
                in = in;
            }

            public void run() {
                while (true) {
                    try {
                        line = inFromServer.readUTF();
                        appendToFile(line, "history.txt");
                        String let = BuildConfig.FLAVOR;
                        if (!line.equals("\n Welcome!\n")) {
                            String[] lineprt = line.split(" ");
                            for (int i = 3; i < lineprt.length; i++) {
                                let = let + lineprt[i] + " ";
                            }
                            if (!(lineprt[1].equals(nm + ":") || lineprt[1].equals("\n Welcome!\n"))) {
                                notif("Сообщение от " + lineprt[1], let);
                            }
                            System.out.println(lineprt[1]);
                        }
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        }

        ListenFromServer() {
        }

        public void onPreExecute() {
        }

        public String doInBackground(String... v) {
            new ReceiveMessage(inFromServer).start();
            return line;
        }

 
        public void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        ps = readFromFile(this, "input.txt");
        nm = readFromFile(this, "name.txt");
        networkStateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (isNet()) {
                    new ConnectToServer().execute(new Void[0]);
                    new ListenFromServer().execute(new String[0]);
                }
            }
        };
        registerReceiver(networkStateReceiver, new IntentFilter(STATE));
        return 1;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d("myLogs", "onDestroy");
    }

    public IBinder onBind(Intent intent) {
        Log.d("myLogs", "onBind");
        return null;
    }

    public void logIn() throws IOException {
        outToServer.writeUTF(ps + " " + nm);
    }

    public static void appendColoredText(TextView tv, String text, String color) {
        int start = tv.getText().length();
        tv.append(text);
        ((Spannable) tv.getText()).setSpan(new ForegroundColorSpan(Color.parseColor(color)), start, tv.getText().length(), 0);
    }

    public void notif(String title, String text) {
        Intent resultIntent = new Intent(this, Client.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        //resultIntent.addFlags(268435456);
        Builder builder = new Builder(this).setContentTitle(title).setContentText(text).setContentIntent(PendingIntent.getActivity(this, 0, resultIntent, 0));
        builder.setColor(Color.WHITE); // see gradle
        builder.setSmallIcon(R.drawable.micon); // your icon
        if (VERSION.SDK_INT >= 21) {
            builder.setSmallIcon(R.drawable.micon);
            builder.setColor(Color.WHITE); //see gradle
        } else {
            builder.setSmallIcon(R.drawable.micon);
        }
        ((NotificationManager) getSystemService("notification")).notify(1, builder.build());
        try {
            RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(2)).play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readFromFile(Context context, String name) {
        String ret = BuildConfig.FLAVOR;
        try {
            InputStream inputStream = context.openFileInput(name);
            if (inputStream == null) {
                return ret;
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String receiveString = BuildConfig.FLAVOR;
            StringBuilder stringBuilder = new StringBuilder();
            while (true) {
                receiveString = bufferedReader.readLine();
                if (receiveString != null) {
                    stringBuilder.append(receiveString);
                } else {
                    inputStream.close();
                    return stringBuilder.toString();
                }
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
            return ret;
        } catch (IOException e2) {
            Log.e("login activity", "Can not read file: " + e2.toString());
            return ret;
        }
    }

    public boolean isNet() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService("connectivity");
        NetworkInfo mWifi = connManager.getNetworkInfo(1); // permission need
        NetworkInfo mMobile = connManager.getNetworkInfo(0); // permission need
        if (mWifi.isConnected() || mMobile.isConnected()) {
            return true;
        }
        return false;
    }

    public void appendToFile(String str, String filename) {
        try {
            FileOutputStream fos = openFileOutput(filename, MODE_APPEND);
            fos.write(str.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
