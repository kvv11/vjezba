package com.example.pmavel;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PersonalInfoActivity extends AppCompatActivity {
    private Button oBtnUpisi;
    private String sIme;
    private String sPrezime;
    private String sDatum;
    private TextInputEditText oIme;
    private TextInputEditText oPrezime;
    private TextInputEditText oDatum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        oIme = findViewById(R.id.ime);
        oPrezime = findViewById(R.id.prezime);
        oDatum = findViewById(R.id.datum);
        oBtnUpisi = findViewById(R.id.dalje);


        oDatum.setOnLongClickListener(v -> {
            MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Odaberite datum rođenja")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            materialDatePicker.show(getSupportFragmentManager(), "DATE_PICKER");

            materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                String date = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault()).format(new Date(selection));
                oDatum.setText(date);
            });

            return true;
        });

        oBtnUpisi.setOnClickListener(view -> {
            sIme = oIme.getText().toString();
            sPrezime = oPrezime.getText().toString();
            sDatum = oDatum.getText().toString();

            if (sIme.isEmpty()) {
                Toast.makeText(PersonalInfoActivity.this, "String je prazan", Toast.LENGTH_SHORT).show();
            } else {
                Intent oUpisiIme = new Intent(getApplicationContext(), StudentInfoActivity.class);
                oUpisiIme.putExtra("ime", sIme);
                oUpisiIme.putExtra("prezime", sPrezime);
                oUpisiIme.putExtra("datum", sDatum);
                startActivity(oUpisiIme);
            }
        });
    }
}
