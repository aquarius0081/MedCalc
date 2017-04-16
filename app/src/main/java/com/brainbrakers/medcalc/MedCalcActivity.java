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
import java.util.ArrayList;
import java.util.List;

public class MedCalcActivity extends AppCompatActivity {

    public Cursor c = null;

    private String notSelectedCompItem;
    LinearLayout dynamicContentLayout;
    Spinner ddlComputations;
    DatabaseHelper myDbHelper;
    TextView resultTextView;
    TextView formulaTextView;
    TextView descriptionTextView;

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
        formulaTextView = (TextView) findViewById(R.id.formulaTextView);
        formulaTextView.setVisibility(View.INVISIBLE);
        descriptionTextView = (TextView) findViewById(R.id.descriptiontextView);
        descriptionTextView.setVisibility(View.INVISIBLE);

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
        c = myDbHelper.rawQuery("SELECT Name FROM Computations", null);
        List<String> compList = new ArrayList<>();
        compList.add(notSelectedCompItem);
        if (c.moveToFirst()) {
            do {
                compList.add(c.getString(0)); //Name
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
                formulaTextView.setVisibility(View.INVISIBLE);
                descriptionTextView.setVisibility(View.INVISIBLE);
                final String selectedCompItem = (String) parent.getItemAtPosition(pos);
                if (!selectedCompItem.equals(notSelectedCompItem)) {
                    btnCalculate.setVisibility(View.VISIBLE);
                    c = myDbHelper.rawQuery(getString(R.string.getParamsByCompId),
                            new String[]{selectedCompItem});
                    if (c.moveToFirst()) {
                        do {
                            final String paramName = c.getString(0);
                            final CompParamTypes paramType = CompParamTypes.values()[c.getInt(1)];
                            final int paramId = c.getInt(2);
                            final String paramDefaultValue = c.getString(3);

                            LinearLayout linearLayout = new LinearLayout(MedCalcActivity.this);
                            TextView textView = new TextView(MedCalcActivity.this);
                            textView.setText(paramName);
                            LinearLayout.LayoutParams layoutParams =
                                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT, 2);
                            textView.setLayoutParams(layoutParams);
                            linearLayout.addView(textView);

                            switch (paramType) {
                                case Integer:

                                    EditText editText = new EditText(MedCalcActivity.this);
                                    editText.setId(paramId);
                                    layoutParams =
                                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                                    editText.setLayoutParams(layoutParams);
                                    linearLayout.addView(editText);
                                    dynamicContentLayout.addView(linearLayout);

                                    break;
                                case Select:
                                    Spinner spinner = new Spinner(MedCalcActivity.this);
                                    spinner.setId(paramId);
                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MedCalcActivity.this,
                                            android.R.layout.simple_spinner_dropdown_item, paramDefaultValue.split(";"));
                                    spinner.setAdapter(adapter); // Apply the adapter to the spinner
                                    layoutParams =
                                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                                    spinner.setLayoutParams(layoutParams);
                                    linearLayout.addView(spinner);
                                    dynamicContentLayout.addView(linearLayout);

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
        c = myDbHelper.rawQuery(getString(R.string.getComputationDetailsByCompName), new String[]{selectedComputation});
        String compDesc = null;
        ComputationTypes compType = null;
        if (c.moveToFirst()) {
            compType = ComputationTypes.values()[c.getInt(0)];
            if (!c.isNull(1)) {
                compDesc = "Описание:\n" + c.getString(1);
            }
            c.moveToNext();
        }
        try {
            switch (compType) {
                case SKF:
                    calculateSKF();
                    break;
                case IMT:
                    calculateIMT();
                    break;
                case BSA:
                    calculateBSA();
                    break;
                default:
                    Toast.makeText(MedCalcActivity.this, "Расчет не поддерживается", Toast.LENGTH_SHORT).show();
                    break;
            }
        } catch (Exception e) {
            return;
        }
        if (compDesc != null && !compDesc.isEmpty()) {
            descriptionTextView.setText(compDesc);
            descriptionTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Расчет площади тела по формуле Дюбуа
     */
    private void calculateBSA() {
        double weight;
        double height;
        try {
            EditText weightText = (EditText) findViewById(CompParams.BSA_WEIGHT.ordinal());
            weight = Double.valueOf(weightText.getText().toString());

            EditText heightText = (EditText) findViewById(CompParams.BSA_HEIGHT.ordinal());
            height = Double.valueOf(heightText.getText().toString());
        } catch (Exception e) {
            Toast.makeText(MedCalcActivity.this, R.string.incorrectInputError, Toast.LENGTH_SHORT).show();
            throw e;
        }

        double result = 0.007184 * Math.pow(height, 0.725) * Math.pow(weight, 0.425);

        resultTextView.setText(String.format("BSA = %s", String.valueOf(result)));
        resultTextView.setVisibility(View.VISIBLE);

        formulaTextView.setText("Расчет по формуле: 0.007184 x Рост(см)^0.725 x Вес(кг)^0.425");
        formulaTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Расчет Скорости Клубочковой Фильтрации
     */
    private void calculateSKF() {
        double sexCoefficient = 1.23; //For men
        Spinner sexSpinner = (Spinner) findViewById(CompParams.SKF_SEX.ordinal());
        if (((String) sexSpinner.getSelectedItem()).equalsIgnoreCase("женский")) {
            sexCoefficient = 1.05;
        }
        double weight = 0.0;
        double age = 0.0;
        double сreatinine = 0.0;

        try {
            EditText weightText = (EditText) findViewById(CompParams.SKF_WEIGHT.ordinal());
            weight = Double.valueOf(weightText.getText().toString());

            EditText ageText = (EditText) findViewById(CompParams.SKF_AGE.ordinal());
            age = Double.valueOf(ageText.getText().toString());

            EditText kreatininText = (EditText) findViewById(CompParams.SKF_CREATININE.ordinal());
            сreatinine = Double.valueOf(kreatininText.getText().toString());
        } catch (Exception e) {
            Toast.makeText(MedCalcActivity.this, R.string.incorrectInputError, Toast.LENGTH_SHORT).show();
            throw e;
        }

        double result = sexCoefficient * ((140.0 - age) * weight) / сreatinine;

        resultTextView.setText(String.format("СКФ = %s", String.valueOf(result)));
        resultTextView.setVisibility(View.VISIBLE);

        formulaTextView.setText(String.format("Расчет по формуле: %s x ((140 - Возраст(годы)) х вес(кг)) / Креатинин(мкмоль/л)", sexCoefficient));
        formulaTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Расчет Индекса Массы Тела
     */
    private void calculateIMT() {
        double weight = 0.0;
        double height = 0.0;
        try {
            EditText weightText = (EditText) findViewById(CompParams.IMT_WEIGHT.ordinal());
            weight = Double.valueOf(weightText.getText().toString());

            EditText heightText = (EditText) findViewById(CompParams.IMT_HEIGHT.ordinal());
            height = Double.valueOf(heightText.getText().toString());
        } catch (Exception e) {
            Toast.makeText(MedCalcActivity.this, R.string.incorrectInputError, Toast.LENGTH_SHORT).show();
            throw e;
        }

        double result = (weight * 100.0 * 100.0) / (height * height);

        resultTextView.setText(String.format("ИМТ = %s", String.valueOf(result)));
        resultTextView.setVisibility(View.VISIBLE);

        formulaTextView.setText("Расчет по формуле: Вес(кг) / Рост(см)^2");
        formulaTextView.setVisibility(View.VISIBLE);
    }

}
