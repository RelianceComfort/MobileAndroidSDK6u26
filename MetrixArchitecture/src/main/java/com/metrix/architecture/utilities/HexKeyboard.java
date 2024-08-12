package com.metrix.architecture.utilities;

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class HexKeyboard {
    private KeyboardView mHexKeyboardView;
    private Activity mActivity;
    private int mKeyboardLayoutId;

    private OnKeyboardActionListener mOnKeyboardActionListener = new OnKeyboardActionListener() {
        public final static int KeyCodeCancel = Keyboard.KEYCODE_CANCEL;
        public final static int KeyCodeClear = 77001;
        public final static int KeyCodeDelete = Keyboard.KEYCODE_DELETE;
        public final static int KeyCodeNext = 77002;

        @Override public void onKey(int code, int[] codeSet) {
            View currFocus = mActivity.getWindow().getCurrentFocus();
            if (currFocus == null || !(currFocus instanceof EditText)) 
            	return;
            
            EditText edittext = (EditText) currFocus;
            Editable editable = edittext.getText();
            int start = edittext.getSelectionStart();

            if (code == KeyCodeCancel) {
                hideHexKeyboard();
            } else if (code == KeyCodeDelete) {
                if (editable != null && start > 0) 
                	editable.delete(start - 1, start);
            } else if (code == KeyCodeClear) {
                if (editable != null) 
                	editable.clear();
            } else if (code == KeyCodeNext) {
                View focusNew = edittext.focusSearch(View.FOCUS_DOWN);
                if (focusNew != null && (focusNew instanceof EditText)) 
                	focusNew.requestFocus();
                else
                	hideHexKeyboard();
            } else {
                editable.insert(start, Character.toString((char) code));
            }
        }

        @Override public void onPress(int arg0) {}

        @Override public void onRelease(int code) {}

        @Override public void onText(CharSequence text) {}

        @Override public void swipeDown() {}

        @Override public void swipeLeft() {}

        @Override public void swipeRight() {}

        @Override public void swipeUp() {}
    };

    public HexKeyboard(Activity activity, int keyboardViewId, int keyboardLayoutId) {
    	mActivity = activity;
    	mKeyboardLayoutId = keyboardLayoutId;
    	mHexKeyboardView = (KeyboardView)mActivity.findViewById(keyboardViewId);
    	mHexKeyboardView.setKeyboard(new Keyboard(mActivity, keyboardLayoutId));
    	mHexKeyboardView.setPreviewEnabled(false);
    	mHexKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void refreshKeyboardLayout() {
    	if (isHexKeyboardVisible()) {
    		hideHexKeyboard();
    	}
    	mHexKeyboardView.setKeyboard(new Keyboard(mActivity, mKeyboardLayoutId));
    }
    
    public boolean isHexKeyboardVisible() {
        return mHexKeyboardView.getVisibility() == View.VISIBLE;
    }

    public void showHexKeyboard(View v) {
    	mHexKeyboardView.setVisibility(View.VISIBLE);
    	mHexKeyboardView.setEnabled(true);
        if (v != null)
        	((InputMethodManager)mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public void hideHexKeyboard() {
    	mHexKeyboardView.setVisibility(View.GONE);
    	mHexKeyboardView.setEnabled(false);
    }

    public void registerEditText(EditText edittext) {        
        edittext.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override 
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) 
                	showHexKeyboard(v); 
                else 
                	hideHexKeyboard();
            }
        });
        
        edittext.setOnClickListener(new OnClickListener() {
            @Override 
            public void onClick(View v) {
                showHexKeyboard(v);
            }
        });
        
        edittext.setOnTouchListener(new OnTouchListener() {
            @Override 
            public boolean onTouch(View v, MotionEvent event) {
                EditText edittext = (EditText) v;
                int inType = edittext.getInputType();
                edittext.setInputType(InputType.TYPE_NULL);
                
                switch (event.getAction()) {
	                case MotionEvent.ACTION_DOWN:
	                    Layout layout = ((EditText) v).getLayout();
	                    View focusedView = mActivity.getWindow().getCurrentFocus();
                        if(focusedView != null) {
                            if (edittext.getId() == focusedView.getId()) {
                                float x = event.getX() + edittext.getScrollX();
                                int offset = layout.getOffsetForHorizontal(0, x);
                                if (offset > 0) {
                                    if (x > layout.getLineMax(0))
                                        edittext.setSelection(offset);     // touch was at end of text
                                    else
                                        edittext.setSelection(offset - 1);
                                }
                            }
                        }
                        else {
                            float x = event.getX() + edittext.getScrollX();
                            int offset = layout.getOffsetForHorizontal(0, x);
                            if (offset > 0) {
                                if (x > layout.getLineMax(0))
                                    edittext.setSelection(offset);     // touch was at end of text
                                else
                                    edittext.setSelection(offset - 1);
                            }
                        }
	                    break;
                }              
                
                edittext.onTouchEvent(event);
                edittext.clearFocus();
                edittext.requestFocus();
                edittext.setInputType(inType);
                return true;
            }
        });
        
        edittext.setInputType(edittext.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }
}
