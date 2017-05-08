package com.brainbrakers.medcalc;

import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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

    /**
     * Weight of parameter name. The less value the more space it takes
     */
    private final float paramNameWeight = 1;

    /**
     * Weight of parameter value. The less value the more space it takes
     */
    private final float paramValueWeight = 1;

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
            sqle.printStackTrace();
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, compList);
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
                                            LinearLayout.LayoutParams.WRAP_CONTENT, paramNameWeight);
                            textView.setLayoutParams(layoutParams);
                            linearLayout.addView(textView);

                            switch (paramType) {
                                case Integer:

                                    EditText editText = new EditText(MedCalcActivity.this);
                                    editText.setId(paramId);
                                    layoutParams =
                                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT, paramValueWeight);
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
                                                    LinearLayout.LayoutParams.WRAP_CONTENT, paramValueWeight);
                                    spinner.setLayoutParams(layoutParams);
                                    linearLayout.addView(spinner);
                                    dynamicContentLayout.addView(linearLayout);

                                    break;
                                case Checkbox:
                                    CheckBox checkBox = new CheckBox(MedCalcActivity.this);
                                    checkBox.setId(paramId);
                                    linearLayout.addView(checkBox);
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
        String compFormula = null;
        if (c.moveToFirst()) {
            compType = ComputationTypes.values()[c.getInt(0)];
            if (!c.isNull(1)) {
                compDesc = "Описание:\n" + c.getString(1);
            }
            if (!c.isNull(2)) {
                compFormula = "Расчет по формуле:\n" + c.getString(2);
            }
            c.moveToNext();
        }
        try {
            if (compType != null) {
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
                    case CPK:
                        calculateCPK();
                        break;
                    case CHA2DS2:
                        calculateCHA2DS2();
                        break;
                    default:
                        Toast.makeText(MedCalcActivity.this, "Расчет не поддерживается", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        } catch (Exception e) {
            return;
        }
        if (compFormula != null && !compFormula.isEmpty()){
            formulaTextView.setText(compFormula);
            formulaTextView.setVisibility(View.VISIBLE);
        }
        if (compDesc != null && !compDesc.isEmpty()) {
            descriptionTextView.setText(compDesc);
            descriptionTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Расчет по шкале CHA2DS2-VASc
     */
    private void calculateCHA2DS2() {
        int chf;
        int hypertension;
        int age75;
        int diabetes;
        int stroke;
        int vascular;
        int age65;
        int sexCategory;
        try {
            CheckBox chfCheckBox = (CheckBox) findViewById(CompParams.CHA2DS2_HEART_FIBRILATION.ordinal());
            chf = chfCheckBox.isChecked() ? 1 : 0;

            CheckBox hypertensionCheckBox = (CheckBox) findViewById(CompParams.CHA2DS2_HYPERTENSION.ordinal());
            hypertension = hypertensionCheckBox.isChecked() ? 1 : 0;

            CheckBox age75CheckBox = (CheckBox) findViewById(CompParams.CHA2DS2_AGE75.ordinal());
            age75 = age75CheckBox.isChecked() ? 1 : 0;

            CheckBox diabetesCheckBox = (CheckBox) findViewById(CompParams.CHA2DS2_DIABETES.ordinal());
            diabetes = diabetesCheckBox.isChecked() ? 1 : 0;

            CheckBox strokeCheckBox = (CheckBox) findViewById(CompParams.CHA2DS2_STROKE.ordinal());
            stroke = strokeCheckBox.isChecked() ? 1 : 0;

            CheckBox vascularCheckBox = (CheckBox) findViewById(CompParams.CHA2DS2_VASCULAR.ordinal());
            vascular = vascularCheckBox.isChecked() ? 1 : 0;

            CheckBox age65CheckBox = (CheckBox) findViewById(CompParams.CHA2DS2_AGE65.ordinal());
            age65 = age65CheckBox.isChecked() ? 1 : 0;

            CheckBox sexCategoryCheckBox = (CheckBox) findViewById(CompParams.CHA2DS2_SEX_CATEGORY.ordinal());
            sexCategory = sexCategoryCheckBox.isChecked() ? 1 : 0;
        } catch (Exception e) {
            Toast.makeText(MedCalcActivity.this, R.string.incorrectInputError, Toast.LENGTH_SHORT).show();
            throw e;
        }

        int result = 1 * chf + 1 * hypertension + 2 * age75 + 1 * diabetes + 2 * stroke + 1 * vascular + 1 * age65 + 1 * sexCategory;

        resultTextView.setText(String.format("Результат = %s", String.valueOf(result)));
        resultTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Расчет ЦПК (Цветовой показатель крови)
     */
    private void calculateCPK() {
        double hemoglobin;
        double erythrocytes;
        try {
            EditText hemoglobinText = (EditText) findViewById(CompParams.CPK_HEMOGLOBIN.ordinal());
            hemoglobin = Double.valueOf(hemoglobinText.getText().toString());

            EditText erythrocytesText = (EditText) findViewById(CompParams.CPK_ERYTHROCYTES.ordinal());
            erythrocytes = Double.valueOf(erythrocytesText.getText().toString());
        } catch (Exception e) {
            Toast.makeText(MedCalcActivity.this, R.string.incorrectInputError, Toast.LENGTH_SHORT).show();
            throw e;
        }

        double result = (hemoglobin * 3) / erythrocytes;

        resultTextView.setText(String.format("ЦПК = %s", String.valueOf(result)));
        resultTextView.setVisibility(View.VISIBLE);
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
        double weight;
        double age;
        double сreatinine;

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
    }

    /**
     * Расчет Индекса Массы Тела
     */
    private void calculateIMT() {
        double weight;
        double height;
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
    }

}
