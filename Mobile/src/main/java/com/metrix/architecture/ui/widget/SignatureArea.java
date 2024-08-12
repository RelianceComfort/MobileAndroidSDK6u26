package com.metrix.architecture.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.metrix.architecture.utilities.LogManager;

public class SignatureArea extends SurfaceView implements
		SurfaceHolder.Callback {

	private float lastPointX;
	private float lastPointY;
    private int mWidth;
    private int mHeight;
	private Paint paint;	
	private Bitmap signatureAreaContent = null;
	private SurfaceHolder mSurfaceHolder = null;
	public boolean mSigned = false;
	private boolean mKeyboardWasShown;
	private boolean mKeyboardWasHidden;

	public SignatureArea(Context context) {
		this(context, null);
	}

	public SignatureArea(Context context, AttributeSet attrs) {
		super(context, attrs);

		//SurfaceHolder holder = getHolder();
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);   	    
	    	
		setFocusable(true);

		paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(6);
		setWillNotDraw(false);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled())
			return false;

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			lastPointX = event.getX();
			lastPointY = event.getY();

		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			Canvas c = new Canvas(signatureAreaContent);
			c.drawLine(lastPointX, lastPointY, event.getX(), event.getY(),
					paint);
			lastPointX = event.getX();
			lastPointY = event.getY();
			postInvalidate();			

		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			Canvas c = new Canvas(signatureAreaContent);
			c.drawLine(lastPointX, lastPointY, event.getX(), event.getY(),
					paint);
			postInvalidate();
		}
		
		mSigned = true;
		return true;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height) {
		Paint lightGray = new Paint();
		lightGray.setColor(Color.LTGRAY);
		
//		Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();
//	    DisplayMetrics outMetrics = new DisplayMetrics ();
//	    display.getMetrics(outMetrics);
//
//	    float density  = getResources().getDisplayMetrics().density;
//	    float dpHeight = outMetrics.heightPixels / density;
//	    float dpWidth  = outMetrics.widthPixels / density;		
		
		mWidth = width;
		mHeight =height;		
		
		if (signatureAreaContent == null) {
			signatureAreaContent = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			
			Canvas c = new Canvas(signatureAreaContent);
			c.drawRect(0, 0, width, height, lightGray);
			invalidate();
			
			mSigned = false;
		}		
	}
	
	public void drawSurface(Bitmap image) {
		if(image != null)
			signatureAreaContent = image; 		
		
		if (signatureAreaContent != null) {						
			Canvas c = new Canvas(signatureAreaContent);
			onDraw(c);
			invalidate();			
		}		
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if(!mKeyboardWasHidden)
			mSigned = false;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if(!mKeyboardWasShown)
			mSigned = false;
	}
	
	public void clearSurface(){
		Paint lightGray = new Paint();
		lightGray.setColor(Color.LTGRAY);
		
		//SurfaceHolder mSurfaceHolder = getHolder();
		Canvas c = null;
        try {
            c = new Canvas(signatureAreaContent);
            synchronized (mSurfaceHolder) {                
            	c = new Canvas(signatureAreaContent);
        		c.drawRect(0, 0, mWidth, mHeight, lightGray);   
        		postInvalidate();
            }           
        }
        catch(Exception ex){
        	LogManager.getInstance().error(ex);
        }
        finally {
        	mSigned = false;
//            if (c != null) {
//                mSurfaceHolder.unlockCanvasAndPost(c);  
//            }            
        }  	       
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (signatureAreaContent != null) {
			canvas.drawBitmap(signatureAreaContent, 0, 0, null);
		}
		super.onDraw(canvas);
	}

	public Bitmap getBitmap() {
		return signatureAreaContent;
	}

	public void onKeyboardShown() {
		mKeyboardWasShown = true;
		mKeyboardWasHidden = false;
	}

	public void onKeyboardHidden() {
		mKeyboardWasShown = false;
		mKeyboardWasHidden = true;
	}
}
