package com.brainbrakers.medcalc;

import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MedCalcActivity extends AppCompatActivity {

    public Cursor c = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_med_calc);

        DatabaseHelper myDbHelper = new DatabaseHelper(MedCalcActivity.this, getResources());
        try {
            myDbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }
        try {
            myDbHelper.openDataBase();
        } catch (SQLException sqle) {
            throw sqle;
        }
        Toast.makeText(MedCalcActivity.this, "Success", Toast.LENGTH_SHORT).show();
        c = myDbHelper.query("Computations", null, null, null, null, null, null);
        List<String> compList = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                compList.add(c.getString(1));
//                Toast.makeText(MedCalcActivity.this,
//                        "Id: " + c.getString(0) + "\n" +
//                                "Name: " + c.getString(1) + "\n" +
//                                "TypeId: " + c.getString(2) + "\n",
//                        Toast.LENGTH_LONG).show();
            } while (c.moveToNext());
        }

        Spinner ddlComputations = (Spinner)findViewById(R.id.CompSelect);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, compList);
        ddlComputations.setAdapter(adapter);

        ddlComputations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                Toast.makeText(MedCalcActivity.this, (String) parent.getItemAtPosition(pos),
                        Toast.LENGTH_LONG).show();
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });
    }
}
