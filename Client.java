package com.example.mes;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

//@author Ilya Mirzazhanov

public class Client extends Activity {
    String address;
    Socket clientSocket;
    public AsyncTask cts;
    InetAddress giriAddress;
    String history = BuildConfig.FLAVOR;
    String hn = BuildConfig.FLAVOR;
    DataInputStream inFromServer;
    EditText inp;
    //InetSocketAddress insa;
    //public boolean isNetwork = false;
    //public AsyncTask lfs;
    String line;
    TextView mes;
    String nameColor;
    BroadcastReceiver networkStateReceiver;
    public String nm;
    DataOutputStream outToServer;
    public String ps;
    //SharedPreferences sPref;
    Button sendBtn;
   // String ssb;
    String ln;
    String STATE = "android.net.conn.CONNECTIVITY_CHANGE";
    private OnClickListener SendPressed = new OnClickListener() {
        public void onClick(View v) {
            ln = inp.getText().toString();
            new SendMessage().execute(new String[]{ln});
            inp.setText(BuildConfig.FLAVOR);
        }
    };


    class ConnectToServer extends AsyncTask<Void, Void, Void> {
        ConnectToServer() {
        }

        
        public void onPreExecute() {
        }

        public Void doInBackground(Void... v) {
            try {
                giriAddress = InetAddress.getByName("192.168.1.xx"); // some ip
                address = giriAddress.getHostAddress();
                clientSocket = new Socket(address, 1700);
                clientSocket.setReuseAddress(true);
                clientSocket.setTcpNoDelay(true);
                clientSocket.setReceiveBufferSize(100);
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                inFromServer = new DataInputStream(clientSocket.getInputStream());
                logIn();
                System.out.println("*********************");
                System.out.println("                     ");
                System.out.println("   " + address + "   ");
                System.out.println("                     ");
                System.out.println("*********************");
            } catch (Exception e) {
            }
            return null;
        }

        
        public void onPostExecute(Void result) {
        }

        public void onProgressUpdate(Void... values) {
            if (!isNet()) {
                cancel(true);
            }
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
                        System.out.println(line);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                try {
                                    appendRegularMessage(line, mes);
                                } catch (Exception e) {
                                    mes.append("Error listen from server!\n");
                                }
                            }
                        });
                    } catch (IOException e) {
                        line = "Waiting for network...\n";
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
            if (!isNet()) {
                cancel(true);
            }
        }
    }

    class SendMessage extends AsyncTask<String, String, String> {
        String mes2;

        SendMessage() {
        }
        
        public void onPreExecute() {
            if (!isNet()) {
                cancel(true);
            }
        }
        
        public String doInBackground(String... v) {
            mes2 = nameColor + " " + v[0];
            try {
                outToServer.writeUTF(mes2 + "\n");
                outToServer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        
        public void onProgressUpdate(String... values) {
            if (!isNet()) {
                cancel(true);
            }
        }
    }
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // your layout
        startService(new Intent(getApplicationContext(), Listener.class));
        makeUI();
        appendToFile(BuildConfig.FLAVOR, "history.txt");
        getHistory();
        ps = readFromFile(this, "input.txt");
        nm = readFromFile(this, "name.txt");
        nameColor = readFromFile(this, "color.txt");
        sendBtn.setOnClickListener(SendPressed);
        networkStateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (isNet()) {
                    new ConnectToServer().execute(new Void[0]); // best way
                    new ListenFromServer().execute(new String[0]);
                    return;
                }
                line = "Waiting for network...\n";
                mes.append(line);
            }
        };
        registerReceiver(networkStateReceiver, new IntentFilter(STATE));
    }

    public void getHistory() {
        try {
            InputStream inputStream = openFileInput("history.txt");
            if (inputStream != null) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String str = BuildConfig.FLAVOR;
                while (true) {
                    str = bufferedReader.readLine();
                    if (str == null) {
                        return;
                    }
                    if (str.length() > 1) {
                        System.out.println("(" + str + ")");
                        appendRegularMessage(str, mes);
                        mes.append("\n");
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Integer.parseInt(VERSION.SDK) <= 5 || keyCode != 4 || event.getRepeatCount() != 0) {
            return super.onKeyDown(keyCode, event);
        }
        Log.d("CDA", "onKeyDown Called");
        onBackPressed();
        return true;
    }

    public void onBackPressed() {
        Log.d("CDA", "onBackPressed Called");
        Intent setIntent = new Intent("android.intent.action.MAIN");
        setIntent.addCategory("android.intent.category.HOME");
        //setIntent.setFlags(268435456);
        startActivity(setIntent);
    }

    
    public void onDestroy() {
        super.onDestroy();
    }

    public void makeUI() {
        sendBtn = (Button) findViewById(R.id.button1);
        mes = (TextView) findViewById(R.id.textView1);
        inp = (EditText) findViewById(R.id.editText1);
        mes.setMovementMethod(new ScrollingMovementMethod());
    }

    public void logIn() throws IOException {
        outToServer.writeUTF(ps + " " + nm);
    }

    
    public void onPause() {
        super.onPause();
    }

    public static void appendColoredText(TextView tv, String text, String color) {
        int start = tv.getText().length();
        tv.append(text);
        ((Spannable) tv.getText()).setSpan(new ForegroundColorSpan(Color.parseColor(color)), start, tv.getText().length(), 0);
    }

    public void appendRegularMessage(String line, TextView mes) {
        String[] lineprt = line.split(" ");
        String let = BuildConfig.FLAVOR;
        try {
            String Col = lineprt[2];
            appendColoredText(mes, lineprt[0] + " ", "#033b96"); // check out server part
            appendColoredText(mes, lineprt[1] + " ", Col);
            for (int i = 3; i < lineprt.length; i++) {
                mes.append(lineprt[i] + " ");
                let = let + lineprt[i] + " ";
            }
            mes.append("\n");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void appendToFile(String str, String filename) {
        try {
            FileOutputStream fos = openFileOutput(filename, 32768);
            fos.write(str.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isNet() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService("connectivity");
        NetworkInfo mWifi = connManager.getNetworkInfo(1);
        NetworkInfo mMobile = connManager.getNetworkInfo(0);
        if (mWifi.isConnected() || mMobile.isConnected()) {
            return true;
        }
        return false;
    }

    public void toHistory(String str) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("history.txt", true));
            bw.append(str);
            bw.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void writeToFile(String fileName, String str) {
        try {
            FileOutputStream fos = openFileOutput(fileName, 0);
            fos.write(str.getBytes());
            fos.close();
        } catch (IOException e) {
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
}
