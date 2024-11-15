package com.example.pmavel;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Switch;

import com.google.android.material.textfield.TextInputEditText;

public class StudentInfoActivity extends AppCompatActivity {
private String sIme;
    private String sPrezime;
    private String sDatum;
    private String sPredmet;
    private String sProfesor;
    private String sSatilv;
    private String sSatipr;
    private String sIzbornik;



    private TextInputEditText Predmet;
    private TextInputEditText Profesor;
    private TextInputEditText Satilv;
    private TextInputEditText Satipr;
    private Switch Izbornik;
private Button oButton;

    @Override
    public void onBackPressed()
    {

    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_info);

        final Bundle oExtras = getIntent().getExtras();
        sIme = oExtras.getString("ime");
        sPrezime = oExtras.getString("prezime");
        sDatum = oExtras.getString("datum");

        Predmet = findViewById(R.id.predmet);
        Profesor = findViewById(R.id.profesor);
        Satilv = findViewById(R.id.satilv);
        Satipr = findViewById(R.id.satipr);
        Izbornik = findViewById(R.id.izbornik);
        oButton = findViewById(R.id.dalje);

        Izbornik.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sIzbornik = "da";
            } else {
                sIzbornik = "ne";
            }
        });


        oButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sPredmet = Predmet.getText().toString();
                sProfesor = Profesor.getText().toString();
                sSatilv = Satilv.getText().toString();
                sSatipr = Satipr.getText().toString();
                Intent oSummary = new Intent(getApplicationContext(),SummaryActivity.class);
                oSummary.putExtra("ime",sIme);
                oSummary.putExtra("prezime",sPrezime);
                oSummary.putExtra("datum",sDatum);
                oSummary.putExtra("profesor",sProfesor);
                oSummary.putExtra("satilv",sSatilv);
                oSummary.putExtra("satipr",sSatipr);
                oSummary.putExtra("predmet",sPredmet);
                oSummary.putExtra("izbornik",sIzbornik);
                startActivity(oSummary);
            }
        });
    }









}