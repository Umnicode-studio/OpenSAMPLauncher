package com.example.samp_launcher.ui.home;

import android.text.InputFilter;
import android.text.Spanned;

public class PortFilter implements InputFilter {
    private int Min, Max;

    public PortFilter(){
        this.Min = 0;
        this.Max = 65535;
    }
    public PortFilter(int min, int max){
        this.Min = min;
        this.Max = max;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dStart, int dEnd) {
        try {
            int Value = Integer.parseInt(dest.toString() + source.toString());
            if (IsInRange(Value, this.Min, this.Max)){
                return null;
            }

        }catch (NumberFormatException nfe) { }

        return "";
    }

    public static boolean IsInRange(int Number, int Min, int Max){
        return Number >= Min && Number <= Max;
    }
}
