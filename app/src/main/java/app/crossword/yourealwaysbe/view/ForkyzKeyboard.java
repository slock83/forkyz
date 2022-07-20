/**
 * Deliberately not using Keyboard or KeyboardView as they are deprecated.
 * First principles approach preferred instead.
 */

package app.crossword.yourealwaysbe.view;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.IntDef;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.LayoutInflaterCompat;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.R;
import java.util.logging.Logger;

public class ForkyzKeyboard
    extends LinearLayout
    implements View.OnTouchListener, View.OnClickListener {

    private static final Logger LOG
        = Logger.getLogger(ForkyzKeyboard.class.getCanonicalName());

    private static final String FORKYZ_TEXT_KEY = "ForkyzTextKey";
    private static final String FORKYZ_IMAGE_KEY = "ForkyzImageKey";
    private static final String PREF_KEYBOARD_LAYOUT = "keyboardLayout";
    private static final String PREF_KEYBOARD_HAPTIC = "keyboardHaptic";
    private static final String DEFAULT_KEYBOARD_LAYOUT = "0";
    private static final int[] KEYBOARD_LAYOUTS = {
        R.layout.forkyz_keyboard_qwerty,
        R.layout.forkyz_keyboard_qwertz,
        R.layout.forkyz_keyboard_dvorak,
        R.layout.forkyz_keyboard_colemak
    };

    private static final int KEY_REPEAT_DELAY = 300;
    private static final int KEY_REPEAT_INTERVAL = 75;

    public static final int KEY_CHANGE_CLUE_DIRECTION = 0;
    public static final int KEY_NEXT_CLUE = 1;
    public static final int KEY_PREVIOUS_CLUE = 2;

    @IntDef({
        KEY_CHANGE_CLUE_DIRECTION,
        KEY_NEXT_CLUE,
        KEY_PREVIOUS_CLUE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SpecialKey { }

    public interface SpecialKeyListener {
        public void onKeyUp(@SpecialKey int key);
        public void onKeyDown(@SpecialKey int key);
    }

    /**
     * Encodes a particular key's action on up/down
     *
     * Different implementations for normal keys and special keys
     */
    private interface KeyActor {
        public void fireKeyUp();
        public void fireKeyDown();
    }

    private Handler handler = new Handler(Looper.getMainLooper());

    private SparseArray<KeyActor> keyActors;
    private SparseArray<Timer> keyTimers;
    private InputConnection inputConnection;
    private int countKeysDown = 0;
    private List<Integer> specialKeyViewIds;
    private SpecialKeyListener specialKeyListener = null;

    private OnSharedPreferenceChangeListener prefChangeListener
        = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(
                SharedPreferences prefs, String key
            ) {
                if (PREF_KEYBOARD_LAYOUT.equals(key))
                    createView(getContext());
            }
        };

    public ForkyzKeyboard(Context context) {
        this(context, null, 0);
    }

    public ForkyzKeyboard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ForkyzKeyboard(
        Context context, AttributeSet attrs, int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        setFocusable(false);
        createView(context);

        PreferenceManager.getDefaultSharedPreferences(context)
            .registerOnSharedPreferenceChangeListener(prefChangeListener);
    }

    /**
     * Call when activity paused to cancel unfinished key repeats
     */
    public synchronized void onPause() {
        cancelKeyTimers();
    }

    @Override
    public synchronized boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            onKeyDown(view.getId());
            view.setPressed(true);
            if (performHaptic()) {
                view.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP
                );
            }
            return true;
        case MotionEvent.ACTION_UP:
            view.setPressed(false);
            onKeyUp(view.getId());
            return true;
        case MotionEvent.ACTION_CANCEL:
            view.setPressed(false);
            onKeyCancel(view.getId());
            return true;
        case MotionEvent.ACTION_MOVE:
        case MotionEvent.ACTION_POINTER_UP:
        case MotionEvent.ACTION_POINTER_DOWN:
            // ignore these mid-gesture movements, but consume them as
            // they are part of the gesture we're tracking
            return true;
        }
        return false;
    }

    @Override
    public synchronized void onClick(View view) {
        int id = view.getId();
        onKeyDown(id);
        onKeyUp(id);
    }

    /**
     * Attach the keyboard to send events to the view
     */
    public synchronized void setInputConnection(
        InputConnection inputConnection
    ) {
        this.inputConnection = inputConnection;
    }

    public EditorInfo getEditorInfo() {
        return new EditorInfo();
    }

    /**
     * Check if at least one key is currently pressed
     */
    public synchronized boolean hasKeysDown() { return countKeysDown > 0; }

    /**
     * Set a listener for and display special keys
     *
     * Special keys are hidden if they are not listened for. Set to null
     * to hide keys.
     */
    public synchronized void setSpecialKeyListener(
        SpecialKeyListener specialKeyListener
    ) {
        this.specialKeyListener = specialKeyListener;
        setupSpecialKeys();
    }

    /**
     * Whether to show or hide the keyboard hide button row
     */
    public synchronized void setShowHideButton(boolean show) {
        View hideRow = findViewById(R.id.hide_row);
        hideRow.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Make special keys visible, reactive if listener available
     */
    private synchronized void setupSpecialKeys() {
        if (this.specialKeyListener == null) {
            setSpecialKeyVisibilty(View.INVISIBLE);
            for (int viewId : specialKeyViewIds) {
                View keyView = findViewById(viewId);
                keyView.setOnTouchListener(null);
                keyView.setOnClickListener(null);
            }
        } else {
            setSpecialKeyVisibilty(View.VISIBLE);
            for (int viewId : specialKeyViewIds) {
                View keyView = findViewById(viewId);
                keyView.setOnTouchListener(this);
                keyView.setOnClickListener(this);
            }
        }
    }

    private synchronized void countKeyDown() { countKeysDown++; }
    private synchronized void countKeyUp() { countKeysDown--; }

    private synchronized void createView(Context context) {
        // clear any old state (if config change)
        cancelKeyTimers();
        removeAllViews();
        keyActors = new SparseArray<>();
        keyTimers = new SparseArray<>();
        specialKeyViewIds = new ArrayList<>();;

        // inflate new state
        LayoutInflater inflater
            = LayoutInflater.from(context).cloneInContext(context);
        LayoutInflaterCompat.setFactory2(inflater, new FKFactory());
        inflater.inflate(getKeyboardLayout(context), this, true);

        // initially hide unless special key listener is set
        setupSpecialKeys();

        ImageButton hideButton = (ImageButton) findViewById(R.id.key_hide);
        hideButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ForkyzKeyboard.this.setVisibility(View.GONE);
            }
        });
    }

    private int getKeyboardLayout(Context context) {
        // Determines keyboard layout based on shared preferences
        // I'm not fully keen on this -- it would be nice to keep app settings
        // out of here. But the alternative of inflating with a default layout
        // then having the KeyboardManager reinflating if needed seems like
        // overkill.
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);

        String keyboardLayoutString = prefs.getString(
            PREF_KEYBOARD_LAYOUT, DEFAULT_KEYBOARD_LAYOUT
        );

        try {
            int keyboardLayout = Integer.valueOf(keyboardLayoutString);

            if (keyboardLayout < 0 || keyboardLayout >= KEYBOARD_LAYOUTS.length)
                keyboardLayout = Integer.valueOf(DEFAULT_KEYBOARD_LAYOUT);

            return KEYBOARD_LAYOUTS[keyboardLayout];
        } catch (NumberFormatException e) {
            return KEYBOARD_LAYOUTS[Integer.valueOf(DEFAULT_KEYBOARD_LAYOUT)];
        }
    }

    private synchronized void setSpecialKeyVisibilty(int visibility) {
        for (int viewId : specialKeyViewIds) {
            findViewById(viewId).setVisibility(visibility);
        }
    }

    private synchronized void addKeyCode(int keyId, int keyCode) {
        keyActors.put(keyId, new KeyActor() {
            @Override
            public synchronized void fireKeyUp() {
                if (inputConnection != null)
                    fireKey(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
            }

            @Override
            public synchronized void fireKeyDown() {
                if (inputConnection != null)
                     fireKey(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            }

            private void fireKey(KeyEvent event) {
                int flags = KeyEvent.FLAG_KEEP_TOUCH_MODE
                    | KeyEvent.FLAG_SOFT_KEYBOARD;
                KeyEvent flagged = KeyEvent.changeFlags(event, flags);
                inputConnection.sendKeyEvent(flagged);
            }
        });
    }

    private void addSpecialKeyCode(int keyId, @SpecialKey int keyCode) {
        specialKeyViewIds.add(keyId);
        keyActors.put(keyId, new KeyActor() {
            @Override
            public synchronized void fireKeyUp() {
                if (specialKeyListener != null) {
                    specialKeyListener.onKeyUp(keyCode);
                }
            }

            @Override
            public synchronized void fireKeyDown() {
                if (specialKeyListener != null) {
                    specialKeyListener.onKeyDown(keyCode);
                }
            }
        });
    }

    private synchronized void onKeyUp(int keyId) {
        countKeyUp();
        sendKeyUp(keyId);
        cancelKeyTimer(keyId);
    }

    private synchronized void onKeyCancel(int keyId) {
        countKeyUp();
        cancelKeyTimer(keyId);
    }

    private synchronized void onKeyDown(final int keyId) {
        countKeyDown();
        sendKeyDown(keyId);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // post key events on main thread
                handler.post(() -> {
                    sendKeyUp(keyId);
                    sendKeyDown(keyId);
                });
            }
        }, KEY_REPEAT_DELAY, KEY_REPEAT_INTERVAL);
        setKeyTimer(keyId, timer);
    }

    private synchronized void setKeyTimer(int keyId, Timer timer) {
        cancelKeyTimer(keyId);
        keyTimers.put(keyId, timer);
    }

    private synchronized void cancelKeyTimer(int keyId) {
        if (keyTimers == null)
            return;

        Timer timer = keyTimers.get(keyId);
        if (timer != null) {
            timer.cancel();
            // no point keeping references to expired timers
            keyTimers.put(keyId, null);
        }
    }

    private synchronized void cancelKeyTimers() {
        if (keyTimers == null)
            return;

        for (int i = 0; i < keyTimers.size(); i++) {
            Timer timer = keyTimers.valueAt(i);
            if (timer != null)
                timer.cancel();
        }
    }

    /**
     * Send a key up event for the button with the view id keyId
     *
     * Only if an actor was initialised
     */
    private synchronized void sendKeyUp(int keyId) {
        KeyActor actor = keyActors.get(keyId);
        if (actor != null)
            actor.fireKeyUp();
    }

    /**
     * Send a key down event for the button with the view id keyId
     *
     * Only if an actor was initialised
     */
    private synchronized void sendKeyDown(int keyId) {
        KeyActor actor = keyActors.get(keyId);
        if (actor != null)
            actor.fireKeyDown();
    }

    private boolean performHaptic() {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean(PREF_KEYBOARD_HAPTIC, true);
    }

    private class FKFactory implements LayoutInflater.Factory2 {
        @Override
        public View onCreateView(
            View parent, String tag, Context context, AttributeSet attrs
        ) {
            if (FORKYZ_TEXT_KEY.equals(tag)) {
                TextView view = new AppCompatButton(context, attrs);
                setButtonPadding(view, context, attrs);
                setupButton(view, context, attrs);
                return view;
            } else if (FORKYZ_IMAGE_KEY.equals(tag)) {
                View view = new AppCompatImageButton(context, attrs);
                setButtonPadding(view, context, attrs);
                setupButton(view, context, attrs);
                return view;
            } else {
                return null;
            }
        }

        public View onCreateView(
            String tag, Context context, AttributeSet attrs
        ) {
            return onCreateView(null, tag, context, attrs);
        }

        private void setupButton(
            View view, Context context, AttributeSet attrs
        ) {
            view.setFocusable(false);
            TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.ForkyzKey, 0, 0
            );
            try {
                int keyCode = ta.getInt(R.styleable.ForkyzKey_keyCode, -1);
                int specialKeyCode = ta.getInt(
                    R.styleable.ForkyzKey_specialKeyCode, -1
                );

                if (keyCode > -1) {
                    ForkyzKeyboard.this.addKeyCode(view.getId(), keyCode);
                }

                if (specialKeyCode > -1) {
                    ForkyzKeyboard.this.addSpecialKeyCode(
                        view.getId(), specialKeyCode
                    );
                }

                if (keyCode > -1 || specialKeyCode > -1) {
                    view.setOnTouchListener(ForkyzKeyboard.this);
                    view.setOnClickListener(ForkyzKeyboard.this);
                }
            } finally {
                ta.recycle();
            }
        }

        private void setButtonPadding(
            View view, Context context, AttributeSet attrs
        ) {
            int paddingTop = view.getPaddingTop();
            int paddingBottom = view.getPaddingBottom();
            int paddingLeft = view.getPaddingLeft();
            int paddingRight = view.getPaddingRight();

            if (!hasAttribute(android.R.attr.padding, context, attrs) &&
                !hasAttribute(android.R.attr.paddingTop, context, attrs) &&
                !hasAttribute(android.R.attr.paddingBottom, context, attrs)) {

                int btnPaddingPcnt
                    = context
                        .getResources()
                        .getInteger(R.integer.keyboardButtonPaddingPcnt);
                int dispHght
                    = context.getResources().getDisplayMetrics().heightPixels;
                int paddingTopBot = (int) ((btnPaddingPcnt / 100.0) * dispHght);

                paddingTop = paddingTopBot;
                paddingBottom = paddingTopBot;
            }

            view.setPadding(
                paddingLeft, paddingTop, paddingRight, paddingBottom
            );
        }

        private boolean hasAttribute(
            int id, Context context, AttributeSet attrs
        ) {
            boolean hasAttribute = false;
            TypedArray ta = context.obtainStyledAttributes(
                attrs, new int[] { id }
            );
            try {
                hasAttribute = ta.getString(0) != null;
            } finally {
                ta.recycle();
            }
            return hasAttribute;
        }

        private int dpToPx(Context context, int dp) {
            final float scale
                = context.getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }
    }
}
