package com.miteksystems.misnap.misnapworkflow.ui.overlay;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.miteksystems.misnap.events.AutoFocusOnceEvent;
import com.miteksystems.misnap.events.ScaledPreviewSizeStickyEvent;
import com.miteksystems.misnap.mibidata.MibiData;
import com.miteksystems.misnap.misnapworkflow.R;
import com.miteksystems.misnap.misnapworkflow.params.UxpConstants;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

/**
 * Created by awood on 9/9/2015.
 */
public class YourCameraOverlayFragment extends Fragment
        implements View.OnClickListener {

    private static final String TAG = YourCameraOverlayFragment.class.getName();
    public static final String KEY_LAYOUT_ID = "KEY_LAYOUT_ID";

    private OnFragmentInteractionListener mListener;
    private CameraOverlay mCameraOverlay;
    private UIManager mUiManager;
    private int mNumTapsToFocus;
    private boolean hasRotated = false;

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onHelpButtonClicked();
        void onCancelButtonClicked();
        void onTorchButtonClicked(boolean shouldTurnOn);
        void onCaptureButtonClicked();
    }

    // Will default to use R.layout.misnap_your_camera_overlay
    public static YourCameraOverlayFragment newInstance() {
        YourCameraOverlayFragment fragment = new YourCameraOverlayFragment();
        return fragment;
    }

    public static YourCameraOverlayFragment newInstance(int layoutId) {
        YourCameraOverlayFragment fragment = new YourCameraOverlayFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_LAYOUT_ID, layoutId);

        fragment.setArguments(args);
        return fragment;
    }

    // Required empty constructor
    public YourCameraOverlayFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int layoutId = R.layout.misnap_your_camera_overlay;
        if (getArguments() != null) {
            layoutId = getArguments().getInt(KEY_LAYOUT_ID, R.layout.misnap_your_camera_overlay);
        }

        JSONObject params;
        try {
            String jobSettings = getActivity().getIntent().getStringExtra(MiSnapApi.JOB_SETTINGS);
            params = new JSONObject(jobSettings);
        } catch (Exception e) {
            params = new JSONObject();
            e.printStackTrace();
        }
        mCameraOverlay = new CameraOverlay(getActivity(), params, this, layoutId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mCameraOverlay = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        mUiManager = new UIManager(getActivity(), mCameraOverlay);
        mUiManager.initialize();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        EventBus.getDefault().unregister(this);

        if (mCameraOverlay != null) {
            mCameraOverlay.getViewTreeObserver().removeGlobalOnLayoutListener(mLayoutListener);
            mCameraOverlay.getViewTreeObserver().removeGlobalOnLayoutListener(mRotationLayoutListener);
        }
        mLayoutListener = null;
        mRotationLayoutListener = null;

        if (mUiManager != null) {
            mUiManager.cleanup();
            mUiManager = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Set a Global Layout Listener to tell us when the Views have been measured.
        // This way we can scale the overlay images properly.
        mCameraOverlay.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
        mCameraOverlay.getViewTreeObserver().addOnGlobalLayoutListener(mRotationLayoutListener);
        mCameraOverlay.setOnClickListener(this);

        return mCameraOverlay;
    }

    ViewTreeObserver.OnGlobalLayoutListener mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (mCameraOverlay != null && mCameraOverlay.getWidth() > 0) {
                mCameraOverlay.getViewTreeObserver().removeGlobalOnLayoutListener(mLayoutListener);
                mCameraOverlay.initialize();
            }
        }
    };

    ViewTreeObserver.OnGlobalLayoutListener mRotationLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        // this will always get called after onConfigurationChanged
        @Override
        public void onGlobalLayout() {
            if (mCameraOverlay != null && mCameraOverlay.getWidth() > 0) {
                if (hasRotated) {
                    if ((Utils.getDeviceBasicOrientation(YourCameraOverlayFragment.this.getContext().getApplicationContext()) == Configuration.ORIENTATION_PORTRAIT
                            && mCameraOverlay.getWidth() < mCameraOverlay.getHeight()) ||
                            (Utils.getDeviceBasicOrientation(YourCameraOverlayFragment.this.getContext().getApplicationContext()) == Configuration.ORIENTATION_LANDSCAPE
                                    && mCameraOverlay.getWidth() > mCameraOverlay.getHeight())) {
                        mCameraOverlay.onRotate();
                        mCameraOverlay.initGhostImage();
                        mCameraOverlay.showGhostImage();
                        mCameraOverlay.setPreviewParameters();
                        hasRotated = false;

                        mCameraOverlay.invalidate();
                        mCameraOverlay.requestLayout();
                    }
                }
            }
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    // Handle overlay button presses
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.misnap_overlay_help_button) {
            // Disable the button so that the user can't press it multiple times
            view.setEnabled(false);
            view.setClickable(false);
            // Alert the state machine that the Help button was pressed.
            mListener.onHelpButtonClicked();
        } else if (id == R.id.overlay_flash_toggle) {
            // Toggle torch button is pressed, so get the Torch state and invert it
            mListener.onTorchButtonClicked(!mCameraOverlay.getTorchStatus());
        } else if (id == R.id.overlay_cancel_button) {
            // Cancel button is pressed
            //disable the button so that user can't press multiple times
            view.setEnabled(false);
            view.setClickable(false);
            // Alert the state machine that the Cancel button was pressed
            mListener.onCancelButtonClicked();
        } else if (id == R.id.misnap_overlay_capture_button) {
            // Disable the button so that the user can't press it multiple times
            view.setEnabled(false);
            view.setClickable(false);
            // Disable rotation so the user does not accidentally change the orientation while capturing
            disableRotation();
            mCameraOverlay.showManualCapturePressedPleaseWait(true);
            // Alert the state machine that the Manual Capture button was pressed
            mListener.onCaptureButtonClicked();
        } else {
            //uxp event
            MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_TOUCH_SCREEN, ++mNumTapsToFocus);

            EventBus.getDefault().post(new AutoFocusOnceEvent());
        }
    }

    @VisibleForTesting
    CameraOverlay getCameraOverlay() {
        return mCameraOverlay;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        hasRotated = true;
    }

    private void disableRotation() {
        int currentOrientation;
        switch (getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
                break;
            default:
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
        getActivity().setRequestedOrientation(currentOrientation);
    }

    @Subscribe(sticky = true)
    public void onEstablishedPreviewSizeStickyEvent(ScaledPreviewSizeStickyEvent event) {
        if (null != mCameraOverlay) { // shouldn't happen since we unregister eventbus in onPause()
            mCameraOverlay.addBlackBarsIfNecessary(event);
        }
    }
}
