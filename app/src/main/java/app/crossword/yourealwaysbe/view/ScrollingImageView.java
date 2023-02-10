package app.crossword.yourealwaysbe.view;

import java.util.logging.Logger;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.core.view.GestureDetectorCompat;

public class ScrollingImageView extends FrameLayout implements OnGestureListener {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    private static final float BOARD_STICKY_BORDER_MM = 1.5F;

    private AuxTouchHandler aux = null;
    private ClickListener ctxListener;
    private GestureDetectorCompat gestureDetector;
    private ImageView imageView;
    private ScaleListener scaleListener = null;
    private ScrollLocation scaleScrollLocation;
    private boolean longTouched;
    private float maxScale = 1.5f;
    private float minScale = 0.20f;
    private float runningScale = 1.0f;
    private boolean haveAdded = false;
    private boolean allowOverScroll = true;
    private boolean allowZoom = true;
    private int boardStickyBorder;
    private int gestureStartX;
    private int gestureStartY;

    public ScrollingImageView(Context context, AttributeSet as) {
        super(context, as);
        gestureDetector = new GestureDetectorCompat(context, this);
        gestureDetector.setIsLongpressEnabled(true);
        imageView = new ImageView(context);

        boardStickyBorder
            = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_MM,
                BOARD_STICKY_BORDER_MM,
                context.getResources().getDisplayMetrics()
            );

        // have some initial presence so that view can have focus from
        // the get-go
        setImageViewParams(1, 1);

        try {
            aux = (AuxTouchHandler) Class.forName(
                "app.crossword.yourealwaysbe.view.MultitouchHandler"
            ).newInstance();
            aux.init(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setBitmap(Bitmap bitmap) {
        this.setBitmap(bitmap, true);
    }

    public void setBitmap(Bitmap bitmap, boolean rescale) {
        if (bitmap == null) {
            imageView.setImageBitmap(null);
            setImageViewParams(1, 1);
        } else if (rescale) {
            imageView.setImageBitmap(bitmap);
            setImageViewParams(bitmap.getWidth(), bitmap.getHeight());
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    public void setContextMenuListener(ClickListener l) {
        this.ctxListener = l;
    }

    public ImageView getImageView() {
        return this.imageView;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public float getMinScale() {
        return minScale;
    }

    public void setScaleListener(ScaleListener scaleListener) {
        this.scaleListener = scaleListener;
    }

    private float currentScale = 1.0f;

    /**
     * Sets current scale
     *
     * May adjust if necessary, returns final scale
     */
    public float setCurrentScale(float scale){
        this.currentScale = scale;
        return this.currentScale;
    }

    public float getCurrentScale() {
        return currentScale;
    }

    public boolean isVisible(Point p) {
        int currentMinX = this.getScrollX();
        int currentMaxX = this.getWidth() + this.getScrollX();
        int currentMinY = this.getScrollY();
        int currentMaxY = this.getHeight() + this.getScrollY();

        return (p.x >= currentMinX) && (p.x <= currentMaxX)
            && (p.y >= currentMinY) && (p.y <= currentMaxY);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            onTouchStart();
            break;
        case MotionEvent.ACTION_UP:
            onTouchEnd();
            break;
        case MotionEvent.ACTION_CANCEL:
            onTouchCancel();
            break;
        }

        if (aux != null)
            aux.onTouchEvent(ev);

        gestureDetector.onTouchEvent(ev);

        return true;
    }

    public void ensureVisible(Point p) {
        // do not scroll before we have dimensions
        if (this.getWidth() == 0 || this.getHeight() == 0)
            return;

        int x = p.x;
        int y = p.y;

        int currentX = this.getScrollX();
        int currentMaxX = this.getWidth() + this.getScrollX();
        int currentY = this.getScrollY();
        int currentMaxY = this.getHeight() + this.getScrollY();

        if (x < currentX) {
            this.scrollTo(x, currentY);
        } else if (x > currentMaxX) {
            this.scrollTo(x - this.getWidth(), currentY);
        }

        if (y < currentY) {
            this.scrollTo(currentX, y);
        } else if (y > currentMaxY) {
            this.scrollTo(currentX, y - this.getHeight());
        }
    }

    public boolean onDown(MotionEvent e) {
        // this should always return true
        return true;
    }

    public boolean onFling(
        MotionEvent e1, MotionEvent e2, float velocityX, float velocityY
    ) {
        return false;
    }

    public void onLongPress(MotionEvent e) {
        if ((aux != null) && aux.inProgress())
            return;

        final Point p = this.resolveToImagePoint(e.getX(), e.getY());

        onContextMenu(p);
    }

    public boolean onScroll(
        MotionEvent e1, MotionEvent e2, float distanceX, float distanceY
    ) {
        if ((aux != null) && aux.inProgress())
            return true;

        this.longTouched = false;

        this.scrollBy((int) distanceX, (int) distanceY);

        return true;
    }

    public void onShowPress(MotionEvent e) {
    }

    public boolean onSingleTapUp(MotionEvent e) {
        if ((aux != null) && aux.inProgress())
            return true;

        Point p = this.resolveToImagePoint(e.getX(), e.getY());

        if (this.longTouched) {
            this.longTouched = false;
        } else {
            onTap(p);
        }

        return true;
    }

    public Point resolveToImagePoint(float x, float y) {
        return this.resolveToImagePoint((int) x, (int) y);
    }

    public Point resolveToImagePoint(int x, int y) {
        return new Point(x + this.getScrollX(), y + this.getScrollY());
    }

    /**
     * Set whether overscroll is allowed
     *
     * Overscroll is when the board is dragged so that an edge of the
     * board pulls away from the corresponding edge of the view. E.g. a
     * gap between the left view edge and the left board edge. Default
     * is yes.
     */
    public void setAllowOverScroll(boolean allowOverScroll) {
        this.allowOverScroll = allowOverScroll;
    }

    public void setAllowZoom(boolean allowZoom) {
        this.allowZoom = allowZoom;
    }

    public void scrollBy(int x, int y) {
        int curX = this.getScrollX();
        int curY = this.getScrollY();
        int newX = curX + x;
        int newY = curY + y;

        int screenWidth = this.getWidth();
        int screenHeight = this.getHeight();
        int boardWidth = this.imageView.getWidth();
        int boardHeight= this.imageView.getHeight();

        // don't allow space between right/bot edge of screen and
        // board (careful of negatives, since co-ords are neg)
        // only adjust if we're scrolling up and just stay put if there
        // was already a gap
        int newRight = newX - boardWidth;
        if (x > 0 &&
            -newRight < screenWidth &&
            (!allowOverScroll || -newRight > screenWidth - boardStickyBorder))
            newX = Math.max(-(screenWidth - boardWidth), curX);

        if (x < 0 &&
            -newRight > screenWidth &&
            (!allowOverScroll || -newRight < screenWidth + boardStickyBorder))
            newX = Math.max(-(screenWidth - boardWidth), curX);

        int newBot = newY - boardHeight;
        if (y > 0 &&
            -newBot < screenHeight &&
            (!allowOverScroll || -newBot > screenHeight - boardStickyBorder))
            newY = Math.max(-(screenHeight - boardHeight), curY);

        if (y < 0 &&
            -newBot > screenHeight &&
            (!allowOverScroll || -newBot < screenHeight + boardStickyBorder))
            newY = Math.max(-(screenHeight - boardHeight), curY);

        // don't allow space between left/top edge of screen and board
        // by doing second this is prioritised over bot/right
        // fix even if scrolling down to stop flipping from one edge to
        // the other (i.e. never allow a gap top/left, but sometime
        // allow bot/right if needed)
        if (newX < 0 &&
            (!allowOverScroll || newX > -boardStickyBorder))
            newX = 0;
        if (newY < 0 &&
            (!allowOverScroll || newY > -boardStickyBorder))
            newY = 0;

        // as above but for scrolling top/left off screen
        if (newX > 0 &&
            (!allowOverScroll || newX < boardStickyBorder))
            newX = 0;
        if (newY > 0 &&
            (!allowOverScroll || newY < boardStickyBorder))
            newY = 0;

        super.scrollTo(newX, newY);
    }

    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
    }

    public void zoom(float scale, int x, int y) {
        if (!allowZoom)
            return;

        if (this.scaleScrollLocation == null) {
            this.scaleScrollLocation = new ScrollLocation(
                this.resolveToImagePoint(x, y), this.imageView
            );
        }

        if ((runningScale * scale) < minScale) {
            scale = 1.0F;
        }

        if ((runningScale * scale) > maxScale) {
            scale = 1.0F;
        }

        if(scale * this.currentScale > maxScale ){
            return;
        }
        if(scale * this.currentScale < minScale){
            return;
        }
        int h = imageView.getHeight();
        int w = imageView.getWidth();
        h *= scale;
        w *= scale;
        runningScale *= scale;
        currentScale *= scale;

        setImageViewParams(w, h);
        this.scaleScrollLocation.fixScroll(w, h);
    }

    public void zoomEnd() {
        if (this.scaleScrollLocation != null) {
            onScale(
                runningScale,
                this.scaleScrollLocation.findNewPoint(
                    imageView.getWidth(), imageView.getHeight()
                )
            );
            this.scaleScrollLocation.fixScroll(
                imageView.getWidth(), imageView.getHeight()
            );
        }

        this.scaleScrollLocation = null;
        runningScale = 1.0f;
    }

    /**
     * Bookkeeping for gesture start
     *
     * Stores scroll location so it can be restored on cancel
     */
    private void onTouchStart() {
        gestureStartX = getScrollX();
        gestureStartY = getScrollY();
    }

    /**
     * Bookkeeping for gesture end
     *
     * Currently does nothing
     */
    private void onTouchEnd() {
        // pass
    }

    /**
     * Bookkeeping for gesture cancel
     *
     * Undoes scroll actions if the scroll was ultimately cancelled
     */
    private void onTouchCancel() {
        scrollTo(gestureStartX, gestureStartY);
    }

    public interface AuxTouchHandler {
        boolean inProgress();
        void init(ScrollingImageView view);
        boolean onTouchEvent(MotionEvent ev);
    }

    public interface ClickListener {
        default void onContextMenu(Point e) { };
        default void onTap(Point e) { };
    }

    public interface ScaleListener {
        void onScale(float scale);
    }

    /**
     * Called when scale changes
     *
     * Always call super.onScale, so that listeners can also be
     * notified.
     */
    protected void onScale(float scale, Point center) {
        notifyScaleChange(scale);
    }

    protected void notifyScaleChange(float scale) {
        if (scaleListener != null)
            scaleListener.onScale(scale);
    }

    /**
     * Called on long press on point p
     *
     * Always call super.onContextMenu so that listeners can be
     * notified.
     */
    protected void onContextMenu(Point p) {
        if (ScrollingImageView.this.ctxListener != null) {
            ScrollingImageView.this.ctxListener.onContextMenu(p);
            ScrollingImageView.this.longTouched = true;
        }
    }

    /**
     * Called on tap on point p
     *
     * Always call super.onTap so that listeners can be notified.
     */
    protected void onTap(Point p) {
        if (this.ctxListener != null) {
            this.ctxListener.onTap(p);
        }
    }

    /**
     * Change image size, add to view if needed
     */
    private void setImageViewParams(int width, int height) {
        FrameLayout.LayoutParams params
            = new FrameLayout.LayoutParams(width, height);
        if(!haveAdded){
            this.addView(imageView, params);
            haveAdded = true;
        } else {
            imageView.setLayoutParams(params);
        }
    }

    public static class Point {
        int x;
        int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Point() {
        }

        public int distance(Point p) {
            double d = Math.sqrt(
                ((double) this.x - (double) p.x)
                    + ((double) this.y - (double) p.y)
            );

            return (int) Math.round(d);
        }

        @Override
        public String toString(){
            return "["+x+", "+y+"]";
        }
    }

    private class ScrollLocation {
        private final double percentAcrossImage;
        private final double percentDownImage;
        private final int absoluteX;
        private final int absoluteY;

        public ScrollLocation(Point p, ImageView imageView) {
            this.percentAcrossImage
                = (double) p.x / (double) imageView.getWidth();
            this.percentDownImage
                = (double) p.y / (double) imageView.getHeight();
            this.absoluteX = p.x - ScrollingImageView.this.getScrollX();
            this.absoluteY = p.y - ScrollingImageView.this.getScrollY();
        }

        public Point findNewPoint(int newWidth, int newHeight) {
            int newX = (int) Math.round(
                (double) newWidth * this.percentAcrossImage
            );
            int newY = (int) Math.round(
                (double) newHeight * this.percentDownImage
            );
            return new Point(newX, newY);
        }

        public void fixScroll(int newWidth, int newHeight) {
            Point newPoint = this.findNewPoint(newWidth, newHeight);

            int newScrollX = newPoint.x - this.absoluteX;
            int newScrollY = newPoint.y - this.absoluteY;

            ScrollingImageView.this.scrollTo(newScrollX, newScrollY);
        }
    }
}
