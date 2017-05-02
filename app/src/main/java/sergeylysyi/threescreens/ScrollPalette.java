package sergeylysyi.threescreens;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ScrollPalette extends AppCompatActivity {

    public static final int REQUEST_PALETTE = 0;
    public static final float HUE_OFFSET_DIVIDER = 2f;
    public static final float SATURATION_OFFSET_DIVIDER = 500f;
    public static final int LENGTH_OF_VIBRATION_ON_LONG_PRESS = 50;
    public static final int LENGTH_OF_VIBRATION_ON_BOUND = 10;

    private LinearLayout linLay;
    private SwitchingScrollView sv;
    private CurrentColor currentColor = null;
    private CurrentColor dynamicColor = null;
    private List<ColorButton> buttons = new ArrayList<>();
    private List<Integer> currentColors = new ArrayList<>();
    private boolean colorEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pallete);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        currentColor = new CurrentColor(getIntent().getIntExtra("color to edit", Color.TRANSPARENT));
        dynamicColor = new CurrentColor(Color.TRANSPARENT);

        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        sv = (SwitchingScrollView) findViewById(R.id.horizontalScrollView);
        final ViewTreeObserver vto = sv.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                sv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setButtonsLayout();
                setButtonsCoreColor();
                setButtonsCurrentColors();
                setButtonsBounds();
            }
        });

        linLay = (LinearLayout) findViewById(R.id.linearLayout);

        ImageView imageOfCurrentColor = (ImageView) findViewById(R.id.currentColor);
        ImageView imageOfDynamicColor = (ImageView) findViewById(R.id.dynamicColor);
        TextView textRGB = (TextView) findViewById(R.id.textViewRGB);
        TextView textHSV = (TextView) findViewById(R.id.textViewHSV);

        currentColor.addViewForBackgroundChange(imageOfCurrentColor);
        dynamicColor.addViewForBackgroundChange(imageOfDynamicColor);

        imageOfCurrentColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishWithResult(true);
            }
        });

        currentColor.addTextViewForTextChange(
                textRGB,
                "R: %2h\nG: %2h\nB: %2h",
                CurrentColor.Palette.RGB);

        currentColor.addTextViewForTextChange(
                textHSV,
                "H: %3.2f\nS: %3.2f\nV: %3.2f",
                CurrentColor.Palette.HSV);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        for (int i = 0; i < linLay.getChildCount(); i++) {
            final ColorButton b = (ColorButton) linLay.getChildAt(i);
            b.setLayoutParams(params);

            final GestureDetector gestureDetector = new GestureDetector(
                    this,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onSingleTapConfirmed(MotionEvent e) {
                            currentColor.change(b.getColor());
                            return true;
                        }

                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            b.resetColor();
                            return true;
                        }

                        @Override
                        public void onLongPress(MotionEvent e) {
                            colorEditMode = true;
                            sv.disallowScroll();
                            vibrator.vibrate(LENGTH_OF_VIBRATION_ON_LONG_PRESS);
                        }
                    }
            );
            b.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    if (colorEditMode) {
                        if (e.getAction() == MotionEvent.ACTION_MOVE) {
                            MotionEvent.PointerCoords pointerOffset = new MotionEvent.PointerCoords();
                            e.getPointerCoords(0, pointerOffset);
                            float[] hsvOffset = new float[3];
                            hsvOffset[0] = pointerOffset.x / HUE_OFFSET_DIVIDER;
                            hsvOffset[2] = -pointerOffset.y / SATURATION_OFFSET_DIVIDER;
                            b.offsetHSVColor(hsvOffset);
                            dynamicColor.change(b.getDynamicColor());
                            return true;
                        } else if (e.getAction() == MotionEvent.ACTION_UP) {
                            b.fixCurrentColor();
                            dynamicColor.change(0);
                            colorEditMode = false;
                            sv.allowScroll();
                            return true;
                        }
                        return true;
                    }
                    return gestureDetector.onTouchEvent(e);
                }
            });

            ColorButton.ColorBoundListener colorBoundListener = new ColorButton.ColorBoundListener() {
                @Override
                public void onColorBoundReached() {
                    vibrator.vibrate(LENGTH_OF_VIBRATION_ON_BOUND);
                }
            };
            b.addBoundListener(colorBoundListener, 0);
            b.addBoundListener(colorBoundListener, 2);
            buttons.add(b);
        }
    }

    protected void setButtonsCoreColor() {
        Bitmap bm = Bitmap.createBitmap(
                linLay.getWidth(),
                linLay.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);

        linLay.draw(c);

        for (ColorButton b : buttons) {
            Rect offsetViewBounds = new Rect();
            b.getHitRect(offsetViewBounds);

            b.setCoreColor(bm.getPixel(offsetViewBounds.centerX(), 0));
        }
    }

    protected void setButtonsBounds() {
        float[] hsv = new float[3];
        Iterator<ColorButton> iter = buttons.iterator();
        ColorButton prevButton = iter.next();
        while (iter.hasNext()) {
            ColorButton currentButton = iter.next();
            Color.colorToHSV(currentButton.getColor(), hsv);
            prevButton.setHueMaxValue(hsv[0]);
            Color.colorToHSV(prevButton.getColor(), hsv);
            currentButton.setHueMinValue(hsv[0]);
            prevButton = currentButton;
        }
    }

    protected void setButtonsCurrentColors() {
        Iterator<ColorButton> buttons = this.buttons.iterator();
        Iterator<Integer> colors = currentColors.iterator();
        while (buttons.hasNext() && colors.hasNext()) {
            buttons.next().setColor(colors.next());
        }
    }

    private void setButtonsLayout() {
        Button anyButton = buttons.get(0);

        int sideSize = Math.max(anyButton.getWidth(), anyButton.getHeight());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sideSize / 2, sideSize / 2);

        int margin_width = sideSize / 8;
        int margin_height = sideSize / 4;

        params.setMargins(margin_width, margin_height, margin_width, margin_height);

        for (Button b : buttons) {
            b.setLayoutParams(params);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            finishWithResult(false);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finishWithResult(false);
                return true;
            case R.id.action_done:
                finishWithResult(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void finishWithResult(boolean result) {
        Intent intent = new Intent();
        intent.putExtra("isChanged", result);
        intent.putExtra("color", currentColor.getColor());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("chosen_color", currentColor.getColor());
        ArrayList<Integer> colors = new ArrayList<>(buttons.size());
        for (ColorButton b : buttons) {
            colors.add(b.getColor());
        }
        outState.putIntegerArrayList("palette_current_colors", colors);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int chosenColor = savedInstanceState.getInt("chosen_color");
        currentColor.change(chosenColor);

        ArrayList<Integer> currentColors = savedInstanceState.getIntegerArrayList("palette_current_colors");
        if (currentColors != null) {
            this.currentColors = currentColors;
        }
    }

}
