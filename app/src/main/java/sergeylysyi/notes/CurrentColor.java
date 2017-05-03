package sergeylysyi.notes;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class CurrentColor {
    enum Palette {RGB, HSV}

    private int color;
    private Set<View> viewsForChange = new HashSet<>();
    private Map<TextView, StringPalettePair> textViewsForChange = new HashMap<>();

    CurrentColor(int color) {
        this.color = color;
    }

    int getColor() {
        return color;
    }

    void change(int newColor) {
        color = newColor;
        for (View v : viewsForChange) {
            applyToView(v);
        }
        for (TextView tv : textViewsForChange.keySet()) {
            applyToTextView(tv);
        }
    }

    private void applyToView(View v) {
        v.setBackgroundColor(color);
        v.invalidate();
    }

    private void applyToTextView(TextView tv) {
        if (textViewsForChange.get(tv).palette == Palette.RGB) {
            setTextRGB(tv);
        } else {
            setTextHSV(tv);
        }
    }

    private void setTextRGB(TextView tv) {
        tv.setText(String.format(
                Locale.getDefault(),
                textViewsForChange.get(tv).formattedString,
                Color.red(color),
                Color.green(color),
                Color.blue(color)));
        tv.invalidate();
    }

    private void setTextHSV(TextView tv) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        tv.setText(String.format(
                Locale.getDefault(),
                textViewsForChange.get(tv).formattedString,
                hsv[0],
                hsv[1],
                hsv[2]));
        tv.invalidate();
    }

    void addViewForBackgroundChange(View view) {
        viewsForChange.add(view);
        applyToView(view);
    }

    void addTextViewForTextChange(TextView textView, String formattedString, Palette palette) {
        textViewsForChange.put(textView, new StringPalettePair(formattedString, palette));
        applyToTextView(textView);
    }

    static private class StringPalettePair {
        final String formattedString;
        final Palette palette;

        StringPalettePair(String formattedString, Palette palette) {
            this.formattedString = formattedString;
            this.palette = palette;
        }
    }
}
