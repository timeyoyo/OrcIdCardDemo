
package com.exidcard.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.exidcard.camera.CameraManager;
import com.exidcard.mycard.R;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 100L;
    private static final int OPAQUE = 0xFF;

    private final Paint paint;
    private final int maskColor;
    private final int resultColor;
    private final int frameColor;
    private final int laserColor;
    private final int resultPointColor;
    private final int boxColor;
    private int scannerAlpha;
    private int Round;
    private final int tipColor;
    private final String tipText;
    private final float tipTextSize;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint();
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        frameColor = resources.getColor(R.color.viewfinder_frame);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        boxColor = resources.getColor(R.color.viewfinder_box);
        scannerAlpha = 0;
        Round = 48;
        tipColor = 0xFFEFEFEF;
        tipText = new String("请将身份证放平，尽量充满屏幕；识别成功后点击屏幕再次识别。");
        tipTextSize = dip2px(context, 12);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = CameraManager.get().getFramingRect();
        if (frame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int lw = 16;

        canvas.save();
        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        paint.setColor(boxColor);
        canvas.drawRect(frame.left, frame.top, frame.right, frame.top + 1, paint);
        canvas.drawRect(frame.right - 1, frame.top, frame.right, frame.bottom, paint);
        canvas.drawLine(frame.left, frame.top, frame.left + 1, frame.bottom, paint);
        canvas.drawLine(frame.left, frame.bottom - 1, frame.right, frame.bottom, paint);

        // Draw a two pixel solid black border inside the framing rect
        paint.setColor(frameColor);
        //canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
        //canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
        //canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
        //canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);

        canvas.drawRect(frame.left, frame.top, frame.left + Round, frame.top + 6, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + 6, frame.top + Round, paint);

        canvas.drawRect(frame.right - Round, frame.top, frame.right, frame.top + 6, paint);
        canvas.drawRect(frame.right - 6, frame.top, frame.right, frame.top + Round, paint);

        canvas.drawRect(frame.left, frame.bottom - 6, frame.left + Round, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - Round, frame.left + 6, frame.bottom, paint);

        canvas.drawRect(frame.right - Round, frame.bottom - 6, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.right - 6, frame.bottom - Round, frame.right, frame.bottom, paint);

        //+
        int midx = (frame.left + frame.right) / 2;
        int midy = (frame.top + frame.bottom) / 2;
        // Draw a red "laser scanner" line through the middle to show decoding is active
        paint.setColor(laserColor);
        paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        int middle = frame.height() / 2 + frame.top;
        //canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 1, paint);
        canvas.drawRect(midx - Round - 3, midy - 3, midx + Round + 3, midy + 3, paint);
        canvas.drawRect(midx - 3, midy - Round - 3, midx + 3, midy + Round + 3, paint);
        //int half = (Round+1)/2;
        //canvas.drawLine(midx-half, midy-half, midx+half, midy-half, paint);
        //canvas.drawLine(midx-half, midy+half, midx+half, midy+half, paint);
        //canvas.drawLine(midx-half, midy-half, midx-half, midy+half, paint);
        //canvas.drawLine(midx+half, midy-half, midx+half, midy+half, paint);

        //if(logo != null){
        //paint.setAlpha(OPAQUE);
        //canvas.drawBitmap(logo, frame.right - logo.getWidth() - lw/2, frame.top+lw/2, paint);
        //}

        if (tipText != null) {
            paint.setTextAlign(Align.CENTER);
            paint.setColor(tipColor);
            paint.setTextSize(tipTextSize);
            canvas.translate(frame.left + frame.width() / 2, frame.top + frame.height() * 7 / 8);
            canvas.drawText(tipText, 0, 0, paint);
        }

        canvas.restore();

        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
    }

    public void drawViewfinder() {
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        invalidate();
    }

}
