package com.umnicode.samp_launcher.ui.settings;

import android.text.InputFilter;
import android.text.Spanned;

public class PingTimeoutFilter implements InputFilter {
    private int Min;

    public PingTimeoutFilter(){
        this.Min = 1;
    }
    public PingTimeoutFilter(int min){
        this.Min = min;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dStart, int dEnd) {
        try {
            int Value = Integer.parseInt(dest.toString() + source.toString());
            if (Value >= this.Min){
                return null;
            }

        }catch (NumberFormatException ignored) { }

        return "";
    }
}
