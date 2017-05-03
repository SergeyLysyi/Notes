package sergeylysyi.notes;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;


public class HelperTextView extends android.support.v7.widget.AppCompatTextView {

    static private final String CAPITAL_LETTER = "X";
    private Paint paint = new Paint();
    private Rect rect = new Rect();

    public HelperTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        paint.getTextBounds(CAPITAL_LETTER, 0, 1, rect);
        paint.setTextSize(getTextSize());
        paint.setTypeface(getTypeface());
        int newHeight = getBaseline() - rect.height();
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY));
    }
}
