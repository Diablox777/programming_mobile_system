package com.example.calculator_tetyakoff;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.calculator_tetyakoff.utils.CalculatorParser;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView mathOperation;

    private TextView result;

    private Button[] buttons;
    private final int[] buttonIds = {
            R.id.backscape, R.id.comma, R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six,
            R.id.seven, R.id.eight, R.id.nine, R.id.res, R.id.plus, R.id.minus, R.id.multiply, R.id.divide,
            R.id.rightBrk, R.id.leftBrk, R.id.clear
    };
    private final String[] buttonSymbols = {
            "<-", ".", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "=", "+", "-", "*", "/", ")", "(", "C"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mathOperation = findViewById(R.id.math_operation);
        result = findViewById(R.id.result);
        buttons = new Button[buttonIds.length];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = findViewById(buttonIds[i]);
            buttons[i].setText(buttonSymbols[i]);
            buttons[i].setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        if (id == R.id.clear) {
            mathOperation.setText("");
            result.setText("");
            return;
        }

        if (id != R.id.res && id != R.id.backscape)
        {
            for (int i = 0; i < buttons.length; i++) {
                if (id == buttonIds[i]) {
                    mathOperation.append(buttonSymbols[i]);
                    break;
                }
            }
        }


        if (id == R.id.backscape){
            StringBuilder str = new StringBuilder(mathOperation.getText().toString());
            try {
                str.delete(str.length() - 1, str.length());
                mathOperation.setText(str.toString());
            }
            catch (Exception e){
                result.setText("Invalid input");
            }
        }

        if (id == R.id.res)
        {
            CalculatorParser calculatorParser = new CalculatorParser(mathOperation.getText().toString());
            try{
                Double num = calculatorParser.calculate();
                result.setText(String.valueOf(num));
            }
            catch (Exception e){
                result.setText("Invalid input");
            }

        }


    }
}