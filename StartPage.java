package com.example.mes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class StartPage extends Activity {
    private OnClickListener OKPressed = new OnClickListener() {
        public void onClick(View v) {
            try {
                StartPage.this.writeToFile("input.txt", StartPage.this.password.getText().toString());
                StartPage.this.writeToFile("name.txt", StartPage.this.name.getText().toString());
                StartPage.this.writeToFile("color.txt", StartPage.this.randHexColor());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(StartPage.this.getApplicationContext(), Client.class);
            intent.addFlags(/*your flag optional*/);
            intent.putExtra("EXIT", true);
            StartPage.this.startActivity(intent);
        }
    };
    EditText name;
    Button ok;
    EditText password;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_page);
        makeUI();
        this.ok.setOnClickListener(this.OKPressed);
    }

    public void onBackPressed() {
        finishAffinity();
    }

    public String randHexColor() {
        return String.format("#%06x", new Object[]{Integer.valueOf(new Random().nextInt(16777216))});
    }

    public void makeUI() {
        this.ok = (Button) findViewById(R.id.button);
        this.password = (EditText) findViewById(R.id.editText2);
        this.name = (EditText) findViewById(R.id.editText3);
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
}
