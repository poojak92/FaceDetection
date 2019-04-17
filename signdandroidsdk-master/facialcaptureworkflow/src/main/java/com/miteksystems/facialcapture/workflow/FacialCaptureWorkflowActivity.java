package com.miteksystems.facialcapture.workflow;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.miteksystems.facialcapture.controller.FacialCaptureFragment;
import com.miteksystems.facialcapture.workflow.accessibility.MiSnapAccessibility;
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowParameters;
import com.miteksystems.facialcapture.workflow.screen.AutoModeTutorialFragment;
import com.miteksystems.facialcapture.workflow.screen.FacialCaptureOverlayFragment;
import com.miteksystems.misnap.events.OnCaptureModeChangedEvent;
import com.miteksystems.misnap.events.OnShutdownEvent;
import com.miteksystems.misnap.events.OnStartedEvent;
import com.miteksystems.misnap.mibidata.MibiData;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.MiSnapApi;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by awood on 3/28/16.
 */
public class FacialCaptureWorkflowActivity extends FragmentActivity {

    private static final String TAG = FacialCaptureWorkflowActivity.class.getSimpleName();
    public static final int PERMISSION_REQUEST_CAMERA = 1;
    private MiSnapAccessibility mAccessibility;
    private Handler mHandler;
    private boolean mHasPermissions;

    private Intent mFacialCaptureIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the application flags/settings here
        // NOTE: You must call these BEFORE calling setContentView!
        // Go full screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Prevent screenshots and viewing on non-secure displays
        if (1 != new CameraParamMgr(getLatestJobSettings()).getAllowScreenshots()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        }

        // Unlock the screen when running unit and regression tests
      //  if (BuildConfig.DEBUG) {
//            Utils.unlockAndTurnOnScreen(this);
     //   }

        setContentView(R.layout.facialcapture_activity_facialcaptureworkflow);

        // GPU optimization - prevents unnecessary frame overdraws
        getWindow().setBackgroundDrawable(null);

        mFacialCaptureIntent = this.getIntent();

        mHasPermissions = false;
    }

    private JSONObject getLatestJobSettings() {
        JSONObject jobSettings;
        try {
            jobSettings = new JSONObject(getIntent().getStringExtra(MiSnapApi.JOB_SETTINGS));
        } catch (JSONException e) {
            Log.e(TAG, "Malformed job settings in FacialCaptureWorkflowActivity's intent:  " + e.toString());
            jobSettings = new JSONObject();
        }

        return jobSettings;
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        // You can optionally pass in a locale to override the user's current locale
        // LocaleHelper.changeLanguage(this, "es"); // e.g. Spanish

        mHandler = new Handler();

        if (MiSnapAccessibility.isTalkbackEnabled(getApplicationContext())) {
            mAccessibility = new MiSnapAccessibility(getApplicationContext()); // You can optionally pass in a locale to override the user's current locale
        }

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (!mHasPermissions) {
            requestRuntimePermissions();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mHandler != null) {
            // Stop listening for the timeout
            mHandler.removeCallbacksAndMessages(null); // Specifying a null token removes ALL callbacks and messages
            mHandler = null;
        }

        if (mAccessibility != null) {
            mAccessibility.shutdown();
            mAccessibility = null;
        }

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        setResult(resultCode, data);

        String miSnapReason = data.getStringExtra(MiSnapApi.RESULT_CODE);
        EventBus.getDefault().post(new OnShutdownEvent(resultCode, miSnapReason)); // Send STOP event to MiSnap
    }

    private void requestRuntimePermissions() {
        // On Android M and above, request necessary permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.facialcapture_camera_permission_title)
                        .setMessage(R.string.facialcapture_camera_permission_rationale)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                ActivityCompat.requestPermissions(FacialCaptureWorkflowActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                                // Now wait for the user to grant or deny permissions. If granted, it will fall through to "Permission granted!" below.
                            }
                        })
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                // Now wait for the user to grant or deny permissions. If granted, it will fall through to "Permission granted!" below.
            }
        } else {
            // Permission granted!
            mHasPermissions = true;
            startFacialCaptureWorkflow();
        }
    }

    private void startFacialCaptureWorkflow() {
        // Please do not remove or uncomment the following UXP statement
        MibiData.getInstance().resetUXP(); // sets up mibi

        if (FacialCaptureWorkflowParameters.getSkipTutorialScreen(getIntent())) {
            FragmentLoader.showScreen(getSupportFragmentManager(), new FacialCaptureFragment());
            // Then, once MiSnap has started, show your custom overlay Fragment.
            // This will happen in the onEvent(OnStartedEvent) callback.
        } else {
            FragmentLoader.showScreen(getSupportFragmentManager(), new AutoModeTutorialFragment());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                // Check if the permission request was granted
                if (grantResults.length > 0
                        && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    // Permission granted! But don't call nextMiSnapState(UX_INITIALIZING) yet!
                    // The Activity hasn't been resumed. When it is, then the permissions
                    // will be rechecked and MiSnap will initialize.
                } else {
                    // Permission denied
                    finish();
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Subscribe
    public void onEvent(OnCaptureModeChangedEvent event) {
        new CameraParamMgr(getLatestJobSettings()).setCaptureMode(CameraApi.PARAMETER_CAPTURE_MODE_MANUAL);
        Log.d(TAG, "capture mode change to manual capture");
    }

    // Received when MiSnap has finished starting up and the camera is showing preview frames.
    @Subscribe
    public void onEvent(OnStartedEvent event) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        FacialCaptureOverlayFragment fragment = new FacialCaptureOverlayFragment();
        FragmentLoader.removeOverlayScreens(getSupportFragmentManager());
        FragmentLoader.overlayScreen(getSupportFragmentManager(), fragment);
    }

    // Received when MiSnap has finished shutting down.
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnShutdownEvent event) {
        Log.e(TAG, "OnShutdown, errorCode=" + event.errorCode + ", reason=" + event.errorReason);
        mHandler.removeCallbacksAndMessages(null); // Prevent capture timeouts from firing

        if (event.errorCode == Activity.RESULT_OK) {
            finish();
        } else if (event.errorReason.startsWith(MiSnapApi.RESULT_ERROR_PREFIX)) {
            // Needed for invalid license key errors. Please see the API JavaDocs for
            // a complete list of MiSnap result errors.
            Toast.makeText(getApplicationContext(), event.errorReason, Toast.LENGTH_LONG).show();
            finish();
        }
        // else the FacialCapture camera was shutdown for a reason such as:
        //   help button pressed; home or power button pressed; etc.
        //   and we should not finish the activity/workflow.
    }
}
