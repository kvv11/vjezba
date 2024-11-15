package com.example.pmavel;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

public class SummaryActivity extends AppCompatActivity {
    private String sIme;
    private String sPrezime;
    private String sDatum;
    private String sProfesor;
    private String sPredmet;
    private String sSatilv;
    private String sSatipr;
    private String sIzbornik;


    private TextView T1;
    private TextView T2;
    private TextView T3;
    private TextView T4;
    private TextView T5;
    private TextView T6;
    private TextView T7;
    private TextView T8;


    private Button oButton;

    @Override
    public void onBackPressed()
    {

    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        final Bundle oExtras = getIntent().getExtras();
        sIme = oExtras.getString("ime");
        sPrezime = oExtras.getString("prezime");
        sPredmet = oExtras.getString("predmet");
        sDatum = oExtras.getString("datum");
        sSatilv = oExtras.getString("satilv");
        sSatipr = oExtras.getString("satipr");
        sIzbornik = oExtras.getString("izbornik");
        sProfesor = oExtras.getString("profesor");


        T1 = (TextView) findViewById(R.id.t1);
        T1.setText(sIme);

        T2 = (TextView) findViewById(R.id.t2);
        T2.setText(sPrezime);

        T3 = (TextView) findViewById(R.id.t3);
        T3.setText(sDatum);

        T4 = (TextView) findViewById(R.id.t4);
        T4.setText(sPredmet);

        T5 = (TextView) findViewById(R.id.t5);
        T5.setText(sProfesor);

        T6 = (TextView) findViewById(R.id.t6);
        T6.setText(sSatilv);

        T7 = (TextView) findViewById(R.id.t7);
        T7.setText(sSatipr);

        T8 = (TextView) findViewById(R.id.t8);
        T8.setText(sIzbornik);

        Student student = new Student(sIme, sPrezime, sPredmet);
        StudentSingleton.getInstance().setStudent(student);



        oButton = (Button) findViewById(R.id.button);

        oButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent oHome = new Intent(getApplicationContext(), HomeActivity.class);
                startActivity(oHome);
            }
        });


    }
}