package org.mozilla.focus.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.mozilla.focus.R;

public class FloatingMovableButton extends FloatingActionButton {

    public FloatingMovableButton(Context context) {
        super(context);
    }

    public FloatingMovableButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingMovableButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private final String PREF_FILE_ERASE_BUTTON = "floating-movable-button";

    // range of movement
    private int rangeHeight;
    private int rangeWidth;
    // the start position of button
    private int startX;
    private int startY;
    // state of dragging
    private boolean isDrag;
    final static public int EDGEDIS = 50;
    final static public int DURATION = 500;


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if(changed == false) return;
        retrieveAndApplySettings();
    }

    private void retrieveAndApplySettings() {
        Context context = getContext();
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_FILE_ERASE_BUTTON,
                Context.MODE_PRIVATE
        );

        int height = getBottom() - getTop();
        int width = getRight() - getLeft();

        int x = sharedPref.getInt(context.getString(R.string.pref_key_erase_button_x), getLeft());
        int y = sharedPref.getInt(context.getString(R.string.pref_key_erase_button_y), getTop());

        setLeft(x);
        setRight(x + width);
        setTop(y);
        setBottom(y + height);

    }

    private void updateSettings() {
        Context context = getContext();
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_FILE_ERASE_BUTTON,
                Context.MODE_PRIVATE
        );

        SharedPreferences.Editor editor = sharedPref.edit();
        // We use getX and getY because x and y were used during the dragging process
        editor.putInt(context.getString(R.string.pref_key_erase_button_x), (int)getX());
        editor.putInt(context.getString(R.string.pref_key_erase_button_y), (int)getY());
        editor.apply();
    }


    // override onToucheVent so that button can listen the touch inputs (press/unpressed/drag)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // catch the touch position
        int rawX = (int) event.getRawX();
        int rawY = (int) event.getRawY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // press the button
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                isDrag = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                // save the start location of button
                startX = rawX;
                startY = rawY;
                // Gets the parent of this button which is the range of movement.
                ViewGroup parent;
                if (getParent() != null) {
                    parent = (ViewGroup) getParent();
                    // get the range of height and width
                    rangeHeight = parent.getHeight();
                    rangeWidth = parent.getWidth();
                }
                break;
            // dragging the button
            case MotionEvent.ACTION_MOVE:
                // if the range is valid then start drag the button else break
                if (rangeHeight <= 0 || rangeWidth == 0) {
                    isDrag = false;
                    break;
                } else {
                    isDrag = true;
                }
                // calculate the distance of x and y from start location
                int disX = rawX - startX;
                int disY = rawY - startY;
                int distance = (int) Math.sqrt(disX * disX + disY * disY);
                // special case if the distance is 0 end dragging set the state to false
                if (distance == 0) {
                    isDrag = false;
                    break;
                }
                // button size included
                float x = getX() + disX;
                float y = getY() + disY;
                // test if reached the edge: left up right down
                if (x < 0) {
                    x = 0;
                } else if (x > rangeWidth - getWidth()) {
                    x = rangeWidth - getWidth();
                }
                if (getY() < 180) { // we prevent the button to override navBar
                    y = 180;
                } else if (getY() + getHeight() > rangeHeight - EDGEDIS) {
                    y = rangeHeight - getHeight() - EDGEDIS;
                }
                // Set the position of the button after dragging
                setX(x);
                setY(y);
                // update the start position during dragging
                startX = rawX;
                startY = rawY;
                break;

            // unpressed button
            case MotionEvent.ACTION_UP:
                if (!isNotDrag()) {
                    // recovery from press
                    setPressed(false);
                    if (rawX >= rangeWidth / 2) {
                        // attract right
                        animate().setInterpolator(new DecelerateInterpolator())
                                .setDuration(DURATION)
                                // keep 50 pixel away from the edge
                                .xBy(rangeWidth - getWidth() - getX() - EDGEDIS)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateSettings();
                                    }
                                })
                                .start();
                    } else {
                        // attract left
                        animate().setInterpolator(new DecelerateInterpolator())
                                .setDuration(DURATION)
                                // keep 50 pixel away from the edge
                                .xBy(- (getX() - EDGEDIS))
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateSettings();
                                    }
                                })
                                .start();
                    }
                }
                break;

            default:
                break;
        }
        // if drag then update session otherwise pass
        return !isNotDrag() || super.onTouchEvent(event);
    }

    // check is drag or not
    private boolean isNotDrag() {
        return !isDrag && (getX() == EDGEDIS || (getX() == rangeWidth - getWidth() - EDGEDIS));
    }
}
