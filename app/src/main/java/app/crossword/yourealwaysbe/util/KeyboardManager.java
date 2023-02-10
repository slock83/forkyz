/**
 * Manage on screen keyboard for Play/Notes/ClueList activity (and others)
 */

package app.crossword.yourealwaysbe.util;

import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;

public class KeyboardManager {
    private static final Logger LOG = Logger.getLogger(KeyboardManager.class.getCanonicalName());

    private static final String PREF_KEYBOARD_MODE = "keyboardShowHide";

    private Activity activity;
    private SharedPreferences prefs;
    private ForkyzKeyboard keyboardView;
    private int blockHideDepth = 0;

    private enum KeyboardMode {
        ALWAYS_SHOW, HIDE_MANUAL, SHOW_SPARINGLY, NEVER_SHOW
    }

    /**
     * A view that can be set to take native input
     *
     * If false, assumed to just handle keypresses from ForkyzKeyboard.
     * Needs getView method to get access to the actual view.
     */
    public interface ManageableView {
        void setNativeInput(boolean nativeInput);
        View getView();
        InputConnection onCreateForkyzInputConnection(EditorInfo ei);
    }

    /**
     * Create a new manager to handle the keyboard
     *
     * To use, pass on calls to the implemented methods below.
     *
     * @param activity the activity the keyboard is for
     * @param keyboardView the keyboard view of the activity
     * @param initialView the view that should have focus immediately if
     * keyboard always show
     */
    public KeyboardManager(
        Activity activity,
        ForkyzKeyboard keyboardView,
        ManageableView initialView
    ) {
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.keyboardView = keyboardView;

        if (getKeyboardMode() == KeyboardMode.ALWAYS_SHOW) {
            showKeyboard(initialView);
        } else {
            hideKeyboard();
        }
    }

    /**
     * Call this from the activities onResume method
     *
     * @param currentView is the view the keyboard should be showing
     * for if it's always show
     */
    public void onResume() {
        setHideRowVisibility();

        if (isNativeKeyboard())
            keyboardView.setVisibility(View.GONE);

        setSoftInputLayout();
    }

    /**
     * Call this when the activity receives an onPause
     */
    public void onPause() {
        keyboardView.onPause();
    }

    /**
     * Call this when the activity receives an onStop
     */
    public void onStop() { }

    /**
     * Call this when the activity receives an onDestroy
     */
    public void onDestroy() { }

    /**
     * Show the keyboard -- must be called after UI drawn
     *
     * @param view the view the keyboard should work for, will request
     * focus
     */
    public void showKeyboard(ManageableView manageableView) {
        if (manageableView == null)
            return;

        View view = manageableView.getView();
        if (view == null)
            return;

        boolean isNativeKeyboard = isNativeKeyboard();
        manageableView.setNativeInput(isNativeKeyboard);

        if (
            getKeyboardMode() != KeyboardMode.NEVER_SHOW
            && view.requestFocus()
        ) {
            if (isNativeKeyboard) {
                InputMethodManager imm = getIntputMethodManager();
                imm.showSoftInput(view, 0);
                keyboardView.setVisibility(View.GONE);
            } else {
                keyboardView.setVisibility(View.VISIBLE);
                attachForkyzKeyboardToView(manageableView);
            }
        }
    }

    /**
     * Attach the keyboard to a view without changing visibilty
     */
    public void attachKeyboardToView(ManageableView view) {
        if (!isNativeKeyboard())
            attachForkyzKeyboardToView(view);
    }

    public boolean hideKeyboard() { return hideKeyboard(false); }

    /**
     * Hide the keyboard unless the user always wants it
     *
     * Will not hide if the user is currently pressing a key
     *
     * @param force force hide the keyboard, even if user has set always
     * show
     * @return true if the hide request was not blocked by settings or
     * pushBlockHide
     */
    public boolean hideKeyboard(boolean force) {
        KeyboardMode mode = getKeyboardMode();
        boolean prefHide =
            mode != KeyboardMode.ALWAYS_SHOW
                && mode != KeyboardMode.HIDE_MANUAL;
        boolean softHide =
            prefHide && !keyboardView.hasKeysDown() && !isBlockHide();
        boolean doHide = force || softHide;

        if (doHide) {
            if (isNativeKeyboard()) {
                View focus = activity.getCurrentFocus();
                if (focus != null) {
                    // turn off native input if can
                    if (focus instanceof ManageableView)
                        ((ManageableView) focus).setNativeInput(false);
                    InputMethodManager imm = getIntputMethodManager();
                    imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
            } else {
                keyboardView.setVisibility(View.GONE);
            }
        }

        return doHide;
    }

    /**
     * Call when a native view (e.g. TextEdit) gets focus
     *
     * Will hide the inapp/native keyboard if needed
     */
    public void onFocusNativeView(View view, boolean gainFocus) {
        if (!isNativeKeyboard()) {
            if (gainFocus) {
                hideKeyboard(true);
            } else {
                InputMethodManager imm = getIntputMethodManager();
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
    }

    /**
     * Handle back key
     *
     * Hides keyboard if mode allows it.
     *
     * @return true if key press was consumed, false if it should be
     * passed on
     */
    public boolean handleBackKey() {
        boolean force = getKeyboardMode() != KeyboardMode.ALWAYS_SHOW;
        boolean toHide =
            keyboardView.getVisibility() == View.VISIBLE
                && !isKeyboardHideButton();
        return toHide && hideKeyboard(force);
    }

    /**
     * Add a block hide request
     *
     * hideKeyboard will only have an effect if there are no block hide
     * requests (or force was passed to hideKeyboard)
     */
    public void pushBlockHide() { blockHideDepth++; }

    /**
     * Remove a block hide request
     */
    public void popBlockHide() { blockHideDepth--; }

    private boolean isBlockHide() { return blockHideDepth > 0; }

    private KeyboardMode getKeyboardMode() {
        String never = activity.getString(R.string.keyboard_never_show);
        String back = activity.getString(R.string.keyboard_hide_manual);
        String spare = activity.getString(R.string.keyboard_show_sparingly);
        String always = activity.getString(R.string.keyboard_always_show);

        String modePref = prefs.getString(PREF_KEYBOARD_MODE, back);

        if (never.equals(modePref))
            return KeyboardMode.NEVER_SHOW;
        else if (back.equals(modePref))
            return KeyboardMode.HIDE_MANUAL;
        else if (always.equals(modePref))
            return KeyboardMode.ALWAYS_SHOW;
        else
            return KeyboardMode.SHOW_SPARINGLY;
    }

    private void setHideRowVisibility() {
        if (isKeyboardHideButton()) {
            KeyboardMode mode = getKeyboardMode();
            keyboardView.setShowHideButton(
                mode == KeyboardMode.HIDE_MANUAL
                    || mode == KeyboardMode.SHOW_SPARINGLY
            );
        } else {
            keyboardView.setShowHideButton(false);
        }
    }

    private boolean isKeyboardHideButton() {
        return prefs.getBoolean("keyboardHideButton", false);
    }

    private boolean isNativeKeyboard() {
        return prefs.getBoolean("useNativeKeyboard", false);
    }

    /**
     * Sets window-level soft input mode
     *
     * E.g. always show or always hide when native
     */
    private void setSoftInputLayout() {
        if (isNativeKeyboard()) {
            KeyboardMode mode = getKeyboardMode();
            if (mode == KeyboardMode.ALWAYS_SHOW) {
                activity.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                );
            } else if (mode == KeyboardMode.NEVER_SHOW) {
                activity.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                );
            }
        }
    }

    private void attachForkyzKeyboardToView(ManageableView view) {
        keyboardView.setInputConnection(
            view.onCreateForkyzInputConnection(
                keyboardView.getEditorInfo()
            )
        );
    }

    private InputMethodManager getIntputMethodManager() {
        return (InputMethodManager)
            activity.getSystemService(Context.INPUT_METHOD_SERVICE);
    }
}
