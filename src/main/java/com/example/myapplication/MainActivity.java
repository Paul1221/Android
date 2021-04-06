package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public String metrica1 = "Km",metrica2 = "Km";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Spinner spinner = (Spinner) findViewById(R.id.spinner2);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.metrici, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner = (Spinner) findViewById(R.id.spinner3);
        spinner.setAdapter(adapter);

    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        if(parent.getSelectedItemId() == R.id.spinner2)
            metrica1 = parent.getItemAtPosition(pos).toString();
        else
            metrica2 = parent.getItemAtPosition(pos).toString();
    }

    public void Convert(View view) {
        if(metrica1.equals(metrica2)){
            Toast.makeText(this,"Vezi ca metricile sunt la fel", Toast.LENGTH_SHORT).show();
        }
    }
}