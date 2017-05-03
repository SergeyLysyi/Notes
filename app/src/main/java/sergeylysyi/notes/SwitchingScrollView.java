package sergeylysyi.notes;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;


public class SwitchingScrollView extends HorizontalScrollView {

    private boolean scrollAllowed = true;

    public SwitchingScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return scrollAllowed && super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return scrollAllowed && super.onInterceptTouchEvent(event);
    }

    public void allowScroll(){
        scrollAllowed = true;
    }

    public void disallowScroll(){
        scrollAllowed = false;
    }
}
