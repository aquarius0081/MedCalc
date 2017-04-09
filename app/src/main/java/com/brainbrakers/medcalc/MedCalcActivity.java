package com.brainbrakers.medcalc;

import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MedCalcActivity extends AppCompatActivity {

    public Cursor c = null;

    private String notSelectedCompItem;
    LinearLayout dynamicContentLayout;
    Spinner ddlComputations;
    DatabaseHelper myDbHelper;
    HashMap<String, Integer> paramsMap;
    TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notSelectedCompItem = getResources().getString(R.string.notSelectedCompItem);
        setContentView(R.layout.activity_med_calc);
        dynamicContentLayout = (LinearLayout) findViewById(R.id.DynamicContent);
        final Button btnCalculate = (Button) findViewById(R.id.btnCalculate);
        btnCalculate.setVisibility(View.INVISIBLE);
        resultTextView = (TextView) findViewById(R.id.resultTextView);
        resultTextView.setVisibility(View.INVISIBLE);

        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculate();
            }
        });

        myDbHelper = new DatabaseHelper(MedCalcActivity.this, getResources());
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
        //Toast.makeText(MedCalcActivity.this, "Success", Toast.LENGTH_SHORT).show();
        c = myDbHelper.rawQuery("SELECT Name FROM Computations", null);
        List<String> compList = new ArrayList<>();
        compList.add(notSelectedCompItem);
        if (c.moveToFirst()) {
            do {
                compList.add(c.getString(0)); //Name
//                Toast.makeText(MedCalcActivity.this,
//                        "Id: " + c.getString(0) + "\n" +
//                                "Name: " + c.getString(1) + "\n" +
//                                "TypeId: " + c.getString(2) + "\n",
//                        Toast.LENGTH_LONG).show();
            } while (c.moveToNext());
        }

        ddlComputations = (Spinner) findViewById(R.id.CompSelect);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, compList);
        ddlComputations.setAdapter(adapter);

        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                dynamicContentLayout.removeAllViews();
                btnCalculate.setVisibility(View.INVISIBLE);
                resultTextView.setVisibility(View.INVISIBLE);
                paramsMap = new HashMap<>();
                final String selectedCompItem = (String) parent.getItemAtPosition(pos);
                if (!selectedCompItem.equals(notSelectedCompItem)) {
                    btnCalculate.setVisibility(View.VISIBLE);
//                    Toast.makeText(MedCalcActivity.this, selectedCompItem,
//                            Toast.LENGTH_LONG).show();
                    c = myDbHelper.rawQuery(getString(R.string.getParamsByCompId),
                            new String[]{selectedCompItem});
                    if (c.moveToFirst()) {
                        do {
                            final String paramName = c.getString(0);
                            final String paramType = c.getString(1);
                            final int paramId = c.getInt(2);
                            final String paramDefaultValue = c.getString(3);
                            paramsMap.put(paramName, paramId);
//                            Toast.makeText(MedCalcActivity.this,
//                                    "ParamName: " + paramName + "\n" +
//                                            "TypeName: " + paramType + "\n" +
//                                            "ParamId: " + paramId + "\n",
//                                    Toast.LENGTH_SHORT).show();

                            LinearLayout linearLayout = new LinearLayout(MedCalcActivity.this);
                            TextView textView = new TextView(MedCalcActivity.this);
                            textView.setText(paramName);
                            LinearLayout.LayoutParams layoutParams =
                                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                            textView.setLayoutParams(layoutParams);
                            linearLayout.addView(textView);

                            switch (paramType) {
                                case "Integer":

                                    EditText editText = new EditText(MedCalcActivity.this);
                                    editText.setId(paramId);
                                    layoutParams =
                                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                                    editText.setLayoutParams(layoutParams);
                                    linearLayout.addView(editText);
                                    dynamicContentLayout.addView(linearLayout);

//                                    Toast.makeText(MedCalcActivity.this, paramName, Toast.LENGTH_SHORT).show();
                                    break;
                                case "Select":
                                    Spinner spinner = new Spinner(MedCalcActivity.this);
                                    spinner.setId(paramId);
                                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(MedCalcActivity.this,
                                            android.R.layout.simple_spinner_item, paramDefaultValue.split(";"));
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // Specify the layout to use when the list of choices appears
                                    spinner.setAdapter(adapter); // Apply the adapter to the spinner
                                    layoutParams =
                                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                                    spinner.setLayoutParams(layoutParams);
                                    linearLayout.addView(spinner);
                                    dynamicContentLayout.addView(linearLayout);

//                                    Toast.makeText(MedCalcActivity.this, paramName, Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    Toast.makeText(MedCalcActivity.this, paramName + " " + paramType, Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        } while (c.moveToNext());
                    }
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        };

        ddlComputations.setOnItemSelectedListener(itemSelectedListener);
    }

    private void calculate() {
        String selectedComputation = ddlComputations.getSelectedItem().toString();
        Toast.makeText(MedCalcActivity.this, selectedComputation, Toast.LENGTH_SHORT).show();
        c = myDbHelper.rawQuery(getString(R.string.getComputationTypeByCompName), new String[]{selectedComputation});
        String compType = null;
        if (c.moveToFirst()) {
            compType = c.getString(0);
            c.moveToNext();
        }
        if (compType != null) {
            switch (compType) {
                case "СКФ":
                    calculateSKF();
                    break;
                default:
                    Toast.makeText(MedCalcActivity.this, "Расчет не поддерживается", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void calculateSKF() {
        double sexCoefficient = 1.23; //For man
        Spinner sexSpinner = (Spinner) findViewById(paramsMap.get("Пол"));
        if (((String) sexSpinner.getSelectedItem()).equalsIgnoreCase("женщина")) {
            sexCoefficient = 1.05;
        }

        EditText weightText = (EditText) findViewById(paramsMap.get("Вес"));
        int weight = Integer.valueOf(weightText.getText().toString());

        EditText ageText = (EditText) findViewById(paramsMap.get("Возраст"));
        int age = Integer.valueOf(ageText.getText().toString());

        EditText kreatininText = (EditText) findViewById(paramsMap.get("Креатинин"));
        int сreatinine = Integer.valueOf(kreatininText.getText().toString());

        double result = sexCoefficient * ((140 - age) * weight) / сreatinine;

        resultTextView.setText("СКФ = " + String.valueOf(result));
        resultTextView.setVisibility(View.VISIBLE);

    }
}
