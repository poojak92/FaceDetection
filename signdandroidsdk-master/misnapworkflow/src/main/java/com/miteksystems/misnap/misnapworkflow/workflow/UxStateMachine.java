package com.miteksystems.misnap.misnapworkflow.workflow;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.miteksystems.misnap.barcode.BarcodeFragment;
import com.miteksystems.misnap.barcode.events.OnCapturedBarcodeEvent;
import com.miteksystems.misnap.events.CaptureCurrentFrameEvent;
import com.miteksystems.misnap.events.OnCaptureModeChangedEvent;
import com.miteksystems.misnap.events.OnCapturedFrameEvent;
import com.miteksystems.misnap.events.OnShutdownEvent;
import com.miteksystems.misnap.events.OnStartedEvent;
import com.miteksystems.misnap.events.OnTorchStateEvent;
import com.miteksystems.misnap.events.SetCaptureModeEvent;
import com.miteksystems.misnap.events.ShutdownEvent;
import com.miteksystems.misnap.events.TorchStateEvent;
import com.miteksystems.misnap.mibidata.MibiData;
import com.miteksystems.misnap.misnapworkflow.R;
import com.miteksystems.misnap.misnapworkflow.accessibility.MiSnapAccessibility;
import com.miteksystems.misnap.misnapworkflow.device.MiSnapBenchMark;
import com.miteksystems.misnap.misnapworkflow.params.UxpConstants;
import com.miteksystems.misnap.misnapworkflow.params.WorkflowApi;
import com.miteksystems.misnap.misnapworkflow.params.WorkflowParameterReader;
import com.miteksystems.misnap.misnapworkflow.storage.MiSnapPreferencesManager;
import com.miteksystems.misnap.misnapworkflow.storage.SessionDiagnostics;
import com.miteksystems.misnap.misnapworkflow.ui.FragmentLoader;
import com.miteksystems.misnap.misnapworkflow.ui.overlay.BarcodeOverlayFragment;
import com.miteksystems.misnap.misnapworkflow.ui.overlay.YourCameraOverlayFragment;
import com.miteksystems.misnap.misnapworkflow.ui.screen.FTManualTutorialFragment;
import com.miteksystems.misnap.misnapworkflow.ui.screen.FTVideoTutorialFragment;
import com.miteksystems.misnap.misnapworkflow.ui.screen.ManualHelpFragment;
import com.miteksystems.misnap.misnapworkflow.ui.screen.VideoDetailedFailoverFragment;
import com.miteksystems.misnap.misnapworkflow.ui.screen.VideoHelpFragment;
import com.miteksystems.misnap.misnapworkflow.ui.screen.VideoTimeoutFragment;
import com.miteksystems.misnap.params.BarcodeApi;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.DocType;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.params.MiSnapIntentCheck;
import com.miteksystems.misnapcontroller.MiSnapFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by awood on 8/20/2015.
 * This sample state machine contains the standard MiSnap experience.
 * Customize it however you want. Some easy changes are available in the constants below.
 * To change the workflow, modify nextMiSnapState().
 */
public class UxStateMachine {
    private static final String TAG = UxStateMachine.class.getName();
    public static final int PERMISSION_REQUEST_CAMERA = 1;
    private static final int SEC_TO_MS = 1000;

    // Available states
    private static final int UX_REQUEST_PERMISSIONS = 1;
    private static final int UX_INITIALIZING = 2;
    private static final int UX_FIRST_TIME_VIDEO_TUTORIAL = 3;
    private static final int UX_FIRST_TIME_MANUAL_TUTORIAL = 4;
    private static final int UX_START_MISNAP_CAPTURE = 5;
    private static final int UX_MISNAP_IS_ACTIVE = 6;
    private static final int UX_VIDEO_TIMEOUT = 7;
    private static final int UX_VIDEO_HELP = 8;
    private static final int UX_MANUAL_HELP = 9;
    private static final int UX_FINISH_MISNAP_WORKFLOW = 11;
    private static final int UX_START_BARCODE_CAPTURE = 12;
    private static final int UX_START_CREDIT_CARD_CAPTURE = 13;
    int mCurrentState = UX_REQUEST_PERMISSIONS;

    /**
     * Customize the standard MiSnap workflow.
     */
    // Replaces MiSnapInitialTimeout
    private static final int INITIAL_VIDEO_TIMEOUT_MS = 20 * SEC_TO_MS; // 20 seconds
    // Replaces MiSnapTimeout - only used if MAX_TIMEOUTS_BEFORE_FAILOVER is greater than 0
    private static final int VIDEO_TIMEOUT_MS = 30 * SEC_TO_MS; // 30 seconds
    // Add a timeout for barcode scans
    private static final int BARCODE_TIMEOUT = 20 * SEC_TO_MS; // 20 seconds
    // Replaces MiSnapMaxTimeouts
    private static final int MAX_TIMEOUTS_BEFORE_FAILOVER = 0; // No auto-capture retries
    // Replaces MiSnapAutoCaptureFailoverToStillCapture
    private static final int AUTO_CAPTURE_FAILOVER_TO_STILL_CAPTURE = 1; // yes, failover to manual
    // How long to wait for bug animation to finish
    private static final int BUG_ANIMATION_TIMEOUT_MS = 2 * SEC_TO_MS; // 2 seconds


    /**
     * Experimental workflow features.
     */
    // If true, if auto-capture fails MAX_TIMEOUTS_BEFORE_FAILOVER times, restart immediately in manual capture mode.
    private static final boolean SKIP_FAILOVER_SCREEN = false;
    // If true, and SKIP_FAILOVER_SCREEN is true, then seamlessly restart in manual capture mode.
    private static final boolean SEAMLESS_FAILOVER = false;
    // MiSnap will loop, capturing a set number of images. Normally this is 1.
    private static final int NUM_IMAGES_TO_CAPTURE = 1;

    private static final boolean ENABLE_SESSION_DIAGNOSTICS = true;

    private WeakReference<FragmentActivity> mMiWorkflowActivity;
    private Context mAppContext;
    private Intent mMiSnapIntent;
    private JSONObject mParams;
    private CameraParamMgr mCameraParamMgr;
    private int mStartingCaptureModeForMultipleImageCapture;
    private Handler mHandler;
    private VideoTimeoutRunnable mVideoTimeoutRunnable; // Keep a reference so we can cancel it on help screens
    private MiSnapAccessibility mAccessibility;
    private SessionDiagnostics mSessionDiagnostics;
    private boolean mIsCapturingCheck;
    private boolean mHasCapturedAFrame;
    private int mNumTimeouts;
    private int mNumImagesCaptured;
    private String barcodeResult;
    private DocType mDocType;

    public UxStateMachine(FragmentActivity miWorkflowActivity) {
        mAppContext = miWorkflowActivity.getApplicationContext();
        mMiSnapIntent = miWorkflowActivity.getIntent();
        mMiWorkflowActivity = new WeakReference<>(miWorkflowActivity);
        //Please do not remove or uncomment following UXP statements
        MibiData.getInstance().resetUXP(); //this will start the time for the UXP events

        try {
            if (!MiSnapIntentCheck.isDangerous(mMiSnapIntent)) {
                String jobSettings = mMiSnapIntent.getStringExtra(MiSnapApi.JOB_SETTINGS);
                mParams = new JSONObject(jobSettings);
                mCameraParamMgr = new CameraParamMgr(mParams);
                mDocType = new DocType(mCameraParamMgr.getRawDocumentType());
                mStartingCaptureModeForMultipleImageCapture = mCameraParamMgr.getCaptureMode();
                mSessionDiagnostics = new SessionDiagnostics(mDocType);
                mIsCapturingCheck = mCameraParamMgr.isCheck();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void resume(int resumedState) {
        Log.d(TAG, "resume");

        // Start TTS support
        if (MiSnapAccessibility.isTalkbackEnabled(mAppContext)) {
            mAccessibility = new MiSnapAccessibility(mAppContext); // You can optionally pass in a locale to override the user's current locale
        }

        // Register the event bus
        EventBus.getDefault().register(this);

        mHandler = new Handler();

        nextMiSnapState(resumedState);
    }

    public void pause() {
        Log.d(TAG, "pause");

        if(mHandler != null) {
            // Stop listening for the timeout
            mHandler.removeCallbacksAndMessages(null); // Specifying a null token removes ALL callbacks and messages
            mHandler = null;
        }
        if(EventBus.getDefault().isRegistered(this)) {
            // Unregister the event bus
            EventBus.getDefault().unregister(this);
        }

        FragmentLoader.removeOverlayScreens(mMiWorkflowActivity.get().getSupportFragmentManager());

        //Stop TTS if running
        if (mAccessibility != null) {
            mAccessibility.shutdown();
            mAccessibility = null;
        }
    }

    public void destroy() {
        mMiWorkflowActivity.clear();
        mSessionDiagnostics.deInit();
        mMiWorkflowActivity = null;
        mMiSnapIntent = null;
        mAppContext = null;
    }

    /**
     * Handles logic for transitioning to a new workflow state.
     * This is the heart of the state machine.
     * @param nextState: ID of the next state to transition to
     */
    void nextMiSnapState(int nextState) {
        Log.d(TAG, "State changed from " + mCurrentState + " to " + nextState);
        mCurrentState = nextState;
        switch (nextState) {
            case UX_REQUEST_PERMISSIONS:
                // The Card.io library handles permissions itself.
                if (mCameraParamMgr.isCreditCard()) {
                    nextMiSnapState(UX_INITIALIZING);
                    return;
                }

                // On Android M and above, request necessary permissions
                if (ContextCompat.checkSelfPermission(mMiWorkflowActivity.get(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(mMiWorkflowActivity.get(), Manifest.permission.CAMERA)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(mMiWorkflowActivity.get());
                        builder.setTitle(R.string.id_camera_permission_title)
                                .setMessage(R.string.id_camera_permission_rationale)
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        ActivityCompat.requestPermissions(mMiWorkflowActivity.get(), new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                                        // Now wait for the user to grant or deny permissions. If granted, it will fall through to "Permission granted!" below.
                                    }
                                })
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    } else {
                        ActivityCompat.requestPermissions(mMiWorkflowActivity.get(), new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                        // Now wait for the user to grant or deny permissions. If granted, it will fall through to "Permission granted!" below.
                    }
                } else {
                    // Permission granted!
                    nextMiSnapState(UX_INITIALIZING);
                }
                break;

            case UX_INITIALIZING:
                // For credit card and PDF417 barcodes, don't show a tutorial screen.
                if (mCameraParamMgr.isCreditCard()) {
                    nextMiSnapState(UX_START_CREDIT_CARD_CAPTURE);
                } else {
                    if (!MiSnapBenchMark.isCameraSufficientForAutoCapture(mAppContext)) {
                        // If auto-focus is unsupported, auto-capture is unlikely. Save some time and start in manual mode.
                        useManualCaptureModeNextTime();
                    }

                    // For document types handled by MiSnap, show a tutorial screen the first time.
                    if (mCameraParamMgr.isCurrentModeVideo() && MiSnapPreferencesManager.isFirstTimeUser(mAppContext, mDocType)) {
                        nextMiSnapState(UX_FIRST_TIME_VIDEO_TUTORIAL);
                    } else if (!mCameraParamMgr.isCurrentModeVideo() && MiSnapPreferencesManager.isFirstTimeUserManual(mAppContext, mDocType)) {
                        nextMiSnapState(UX_FIRST_TIME_MANUAL_TUTORIAL);
                    } else if(mCameraParamMgr.isBarcode()){
                        nextMiSnapState(UX_START_BARCODE_CAPTURE);
                    } else {
                        nextMiSnapState(UX_START_MISNAP_CAPTURE);
                    }
                }
                break;

            case UX_FIRST_TIME_VIDEO_TUTORIAL:
                FragmentLoader.showScreen(mMiWorkflowActivity.get().getSupportFragmentManager(), FTVideoTutorialFragment.newInstance(mDocType, mCameraParamMgr.getRequestedOrientation()));
                if (!(mCameraParamMgr.isIdDocument() || mCameraParamMgr.isBarcode())) {
                    MiSnapPreferencesManager.setIsFirstTimeUser(mAppContext, false, mDocType);
                }
                break;

            case UX_FIRST_TIME_MANUAL_TUTORIAL:
                FragmentLoader.showScreen(mMiWorkflowActivity.get().getSupportFragmentManager(), FTManualTutorialFragment.newInstance(mDocType, mCameraParamMgr.getRequestedOrientation()));
                if (!(mCameraParamMgr.isIdDocument() || mCameraParamMgr.isBarcode())) {
                    MiSnapPreferencesManager.setIsFirstTimeUserManual(mAppContext, false, mDocType);
                }
                break;

            case UX_START_MISNAP_CAPTURE:
                FragmentLoader.showScreen(mMiWorkflowActivity.get().getSupportFragmentManager(), new MiSnapFragment());
                //TODO: EventBus.getDefault().post(new StartEvent(ParamsHelper.getJobSettings(mMiSnapIntent)));
                break;

            case UX_MISNAP_IS_ACTIVE:
                mHasCapturedAFrame = false;

                // In video mode, fail over to manual mode after X seconds
                if (mCameraParamMgr.isCurrentModeVideo()) {
                    final int timeoutMs = mNumTimeouts == 0 ? INITIAL_VIDEO_TIMEOUT_MS : VIDEO_TIMEOUT_MS;
                    mVideoTimeoutRunnable = new VideoTimeoutRunnable();
                    mHandler.postDelayed(mVideoTimeoutRunnable, timeoutMs);
                }

                // Also display your camera overlay now
                FragmentLoader.overlayScreen(mMiWorkflowActivity.get().getSupportFragmentManager(), new YourCameraOverlayFragment());
                break;

            case UX_VIDEO_HELP:
                mHandler.removeCallbacks(mVideoTimeoutRunnable); // Don't timeout on a help screen
                FragmentLoader.showScreen(mMiWorkflowActivity.get().getSupportFragmentManager(), VideoHelpFragment.newInstance(mIsCapturingCheck));
                break;

            case UX_MANUAL_HELP:
                FragmentLoader.showScreen(mMiWorkflowActivity.get().getSupportFragmentManager(), ManualHelpFragment.newInstance(mIsCapturingCheck));
                break;

            case UX_VIDEO_TIMEOUT:
                if (++mNumTimeouts <= MAX_TIMEOUTS_BEFORE_FAILOVER) {
                    // Let the user try to auto-capture again
                    EventBus.getDefault().post(new ShutdownEvent(ShutdownEvent.TIMEOUT)); // Send STOP event to MiSnap
                    FragmentLoader.showScreen(mMiWorkflowActivity.get().getSupportFragmentManager(), VideoTimeoutFragment.newInstance(mIsCapturingCheck));
                } else {
                    if (AUTO_CAPTURE_FAILOVER_TO_STILL_CAPTURE == 1) {
                        // Failover to manual capture mode
                        if (SKIP_FAILOVER_SCREEN) {
                            // Go straight to manual capture mode...
                            if (SEAMLESS_FAILOVER) {
                                // ...seamlessly
                                // When MiSnap is done entering manual capture mode, this state machine will receive
                                // an Event called OnCaptureModeChangedEvent.
                                EventBus.getDefault().post(new SetCaptureModeEvent(CameraApi.PARAMETER_CAPTURE_MODE_MANUAL));
                            } else {
                                // ...quickly restart MiSnap in between
                                EventBus.getDefault().post(new ShutdownEvent(ShutdownEvent.FAILOVER)); // Send STOP event to MiSnap
                                useManualCaptureModeNextTime();
                                nextMiSnapState(UX_INITIALIZING);
                            }
                        } else {
                            // Give the user control with manual capture mode.
                            EventBus.getDefault().post(new ShutdownEvent(ShutdownEvent.FAILOVER)); // Send STOP event to MiSnap
                            useManualCaptureModeNextTime();
                            // Show the Failover screen so they know what is happening.
                            FragmentLoader.showScreen(mMiWorkflowActivity.get().getSupportFragmentManager(),
                                    VideoDetailedFailoverFragment.newInstance(mDocType, ENABLE_SESSION_DIAGNOSTICS ? mSessionDiagnostics.rankFailures() : new ArrayList<String>()));
                        }
                    } else {
                        // If the workflow doesn't want to failover to manual
                        nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
                    }
                }
                break;

            case UX_START_BARCODE_CAPTURE:
                FragmentLoader.showScreen(mMiWorkflowActivity.get().getSupportFragmentManager(), new BarcodeFragment());
                break;

            case UX_START_CREDIT_CARD_CAPTURE:
                try {
                    Class creditCardFragment = Class.forName("com.miteksystems.creditcardcontroller.CreditCardFragment");
                    FragmentLoader.showScreen(mMiWorkflowActivity.get().getSupportFragmentManager(), (Fragment) creditCardFragment.newInstance());
                } catch (Exception e) {
                    nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
                }
                break;

            case UX_FINISH_MISNAP_WORKFLOW:
                mMiWorkflowActivity.get().finish();
                break;
        }
    }

    // After permissions granted or denied
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case UxStateMachine.PERMISSION_REQUEST_CAMERA:
                // Check if the permission request was granted
                if (grantResults.length > 0
                        && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    // Permission granted! But don't call nextMiSnapState(UX_INITIALIZING) yet!
                    // The Activity hasn't been resumed. When it is, then the permissions
                    // will be rechecked and MiSnap will initialize.
                } else {
                    // Permission denied
                    nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
                }
                return true;
        }

        return false;
    }


    // After FTVideoTutorialFragment
    public void onFirstTimeVideoTutorialFragmentDone() {
        if (mCameraParamMgr.isBarcode()) {
            nextMiSnapState(UX_START_BARCODE_CAPTURE);
        } else {
            nextMiSnapState(UX_START_MISNAP_CAPTURE);
        }
    }

    // After FTManualTutorialFragment
    public void onFirstTimeManualTutorialFragmentDone() {
        if (mCameraParamMgr.isBarcode()) {
            nextMiSnapState(UX_START_BARCODE_CAPTURE);
        } else {
            nextMiSnapState(UX_START_MISNAP_CAPTURE);
        }
    }

    // After VideoTimeoutFragment - continue
    public void onRetryAfterTimeout() {
        nextMiSnapState(UX_START_MISNAP_CAPTURE);
    }
    // After VideoTimeoutFragment - abort
    public void onAbortAfterTimeout() {
        nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
    }

    // After VideoFailoverFragment - continue
    public void onContinueToManualCapture() {
        nextMiSnapState(UX_START_MISNAP_CAPTURE);
    }
    // After VideoFailoverFragment - abort
    public void onAbortCapture() {
        nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
    }

    // After VideoDetailedFailoverFragment - continue
    public void onManualCaptureAfterDetailedFailover() {
        nextMiSnapState(UX_START_MISNAP_CAPTURE);
    }
    // After VideoDetailedFailoverFragment - restart capture session in Auto Mode
    public void onRetryAfterDetailedFailover() {
        EventBus.getDefault().post(new ShutdownEvent(ShutdownEvent.TIMEOUT)); // Send STOP event to MiSnap
        useAutoCaptureModeNextTime();
        nextMiSnapState(UX_START_MISNAP_CAPTURE);
    }
    // After VideoDetailedFailoverFragment - abort
    public void onAbortAfterDetailedFailover() {
        nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
    }

    // After VideoHelpFragment - restart capture session
    public void onVideoHelpRestartMiSnapSession() {
        //uxp event
        MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_HELP_END);
        nextMiSnapState(UX_START_MISNAP_CAPTURE);
    }
    // After VideoHelpFragment - abort
    public void onVideoHelpAbortMiSnap() {
        nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
    }

    // After ManualHelpFragment - restart capture session
    public void onManualHelpRestartMiSnapSession() {
        //uxp event
        MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_HELP_END);
        nextMiSnapState(UX_START_MISNAP_CAPTURE);
    }
    // After ManualHelpFragment - abort
    public void onManualHelpAbortMiSnap() {
        nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
    }


    // After YourCameraOverlayFragment - help button
    public void onHelpButtonClicked() {
        if (!mHasCapturedAFrame) {
            EventBus.getDefault().post(new ShutdownEvent(ShutdownEvent.HELP, ShutdownEvent.EXT_HELP_BUTTON)); // Send STOP event to MiSnap
            nextMiSnapState(mCameraParamMgr.isCurrentModeVideo() ? UX_VIDEO_HELP : UX_MANUAL_HELP);
        }
    }
    // After YourCameraOverlayFragment - torch button
    public void onTorchButtonClicked(boolean shouldTurnOn) {
        EventBus.getDefault().post(new TorchStateEvent("SET", shouldTurnOn));

        // The user didn't like the torch on or off. Remember what they chose for the next capture session.
        // Also remember it if auto-capture times out or fails over.
        setTorchStartingState(shouldTurnOn);
    }
    // After YourCameraOverlayFragment - manual capture button
    public void onCaptureButtonClicked() {
        EventBus.getDefault().post(new CaptureCurrentFrameEvent());
    }
    // After YourCameraOverlayFragment - cancel button
    public void onCancelButtonClicked() {
        EventBus.getDefault().post(new ShutdownEvent(ShutdownEvent.CANCELLED)); // Send STOP event to MiSnap
        nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
    }



    // Received when MiSnap has finished starting up
    @Subscribe
    public void onEvent(OnStartedEvent event) {
        Log.d(TAG, "OnStarted");

        // If we requested auto-capture mode, but MiSnap determines that it is unsupported
        // for this device (i.e. due to no autofocus, no torch, etc.), then it will
        // override your request and start in manual capture mode.
        if (mCameraParamMgr.getCaptureMode() != event.captureMode) {
            setCaptureModeNextTime(event.captureMode);
            // TODO: It would be nice to provide a method to query if MiSnap supports video capture mode on this device BEFORE starting MiSnap.
            if(!mCameraParamMgr.isBarcode()){
                Toast.makeText(mAppContext, mAppContext.getResources().getText(R.string.id_auto_capture_not_supported), Toast.LENGTH_LONG)
                        .show();
            }
        }

        if (mCurrentState == UX_START_BARCODE_CAPTURE) {
            FragmentLoader.overlayScreen(mMiWorkflowActivity.get().getSupportFragmentManager(), new BarcodeOverlayFragment());
        } else {
            nextMiSnapState(UX_MISNAP_IS_ACTIVE);
        }
    }

    // Received when MiSnap has finished shutting down
    @Subscribe
    public void onEvent(OnShutdownEvent event) {
        Log.d(TAG, "OnShutdown");
        mHandler.removeCallbacksAndMessages(null); // Prevent the timeout from firing
        if (event.errorCode == Activity.RESULT_OK || mHasCapturedAFrame) {
            mNumImagesCaptured++;
            if (mNumImagesCaptured >= NUM_IMAGES_TO_CAPTURE) {
                // This is the usual workflow for MiSnap - capture one image, and exit.
                nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
            } else {
                // If you would like to capture more than one document image, then restart MiSnap.
                setCaptureModeNextTime(mStartingCaptureModeForMultipleImageCapture);
                nextMiSnapState(UX_INITIALIZING);
            }
        } else if (event.errorReason.startsWith(MiSnapApi.RESULT_ERROR_PREFIX)) {
            // Needed for invalid license key errors. Please see the API JavaDocs for
            // a complete list of MiSnap result errors.
            Intent returnIntent = new Intent();
            returnIntent.putExtra(MiSnapApi.RESULT_CODE, event.errorReason);
            mMiWorkflowActivity.get().setResult(Activity.RESULT_CANCELED, returnIntent);
            nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
        }
    }

    // Received when MiSnap has captured a document image
    @Subscribe
    public void onEvent(OnCapturedFrameEvent event) {
        Log.d(TAG, "OnCapturedFrame");
        if (mCurrentState != UX_MISNAP_IS_ACTIVE && !mCameraParamMgr.isBarcode()) {
            Log.d(TAG, "Frame arrived too late, and we're already on a new screen. Ignore it.");
            return;
        }

        mHasCapturedAFrame = true;
        //if returning a barcode image, add in barcode data and change result code
        if(mCameraParamMgr.isBarcode()){
            event.returnIntent.putExtra(BarcodeApi.RESULT_PDF417_DATA, barcodeResult);
            event.returnIntent.putExtra(MiSnapApi.RESULT_CODE, MiSnapApi.RESULT_SUCCESS_PDF417);
        }
        // Add any workflow parameter usage to the MIBI data for now
        addWorkflowParametersToMibi(event);

        mMiWorkflowActivity.get().setResult(Activity.RESULT_OK, event.returnIntent);

        //no snap animation for barcode, just return
        if(mCameraParamMgr.isBarcode()){
            nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
        }else{
            // Let the bug animation play for a bit
            mHandler.postDelayed(new BugAnimationTimeoutRunnable(), BUG_ANIMATION_TIMEOUT_MS);
        }
    }

    private void addWorkflowParametersToMibi(OnCapturedFrameEvent event) {
        try {
            String jobSettings = mMiSnapIntent.getStringExtra(MiSnapApi.JOB_SETTINGS);
            JSONObject params = new JSONObject(jobSettings);
            WorkflowParameterReader reader = new WorkflowParameterReader(params);
            int glareTracking = reader.getGlareTracking();

            MibiData mibiData = MibiData.getInstance();
            mibiData.addWorkflowParameter(WorkflowApi.MISNAP_WORKFLOW_TRACK_GLARE, String.valueOf(glareTracking));

            Intent intent = event.returnIntent;
            intent.putExtra(MiSnapApi.RESULT_MIBI_DATA, mibiData.getMibiData());
        } catch (Exception e) {
            Log.e(TAG, "Unable to write workflow parameters to MIBI data");
            Log.e(TAG, e.getMessage());
        }
    }

    // Received when the barcode has been read
    @Subscribe
    public void onEvent(OnCapturedBarcodeEvent event) {
        Log.d(TAG, "OnCapturedBarcodeEvent");

        //save the barcode result to hijack the normal return intent
        barcodeResult = event.returnIntent.getStringExtra(BarcodeApi.RESULT_PDF417_DATA);
        //mMiWorkflowActivity.get().setResult(Activity.RESULT_OK, event.returnIntent);

        //nextMiSnapState(UX_FINISH_MISNAP_WORKFLOW);
    }

    // Received when MiSnap has turned on or turned off the torch
    @Subscribe
    public void onEvent(OnTorchStateEvent event){
        String lMessage=null;
        if(event.function.equals("GET")) {
            lMessage = "Torch is " + (event.currentTorchState == 1 ? "ON" : "OFF");
        } else if(event.function.equals("SET")){
            lMessage = "Torch state has been set to " + (event.currentTorchState == 1 ? "ON" : "OFF");
        }
        Log.d(TAG, "OnTorchState: " + lMessage);
    }

    // Received when MiSnap has changed the capture mode on-the-fly
    @Subscribe
    public void onEvent(OnCaptureModeChangedEvent event) {
        Log.d(TAG, "OnCaptureModeChanged");

        // This workflow supports failover to manual capture
        if (event.mode == CameraApi.PARAMETER_CAPTURE_MODE_MANUAL) {
            useManualCaptureModeNextTime();
            CharSequence message = mAppContext.getResources().getText(R.string.id_seamless_failover);
            Toast.makeText(mAppContext, message, Toast.LENGTH_SHORT)
                    .show();
            FragmentLoader.removeOverlayScreens(mMiWorkflowActivity.get().getSupportFragmentManager()); // Remove it because it will get added in the UX_MISNAP_IS_ACTIVE state
            nextMiSnapState(UX_MISNAP_IS_ACTIVE);
        }
    }



    // Timeout methods
    // Triggers if Auto-Capture mode is unsuccessful for X seconds
    class VideoTimeoutRunnable implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "Session timed out");
            // If we still haven't found a good frame, then show the timeout fragment.
            if (!mHasCapturedAFrame) {
                nextMiSnapState(UX_VIDEO_TIMEOUT);
            } else {
                Log.d(TAG, "...but MiSnap already captured an image.");
            }
        }
    }

    // Triggers when the "successful capture animation" has finished
    class BugAnimationTimeoutRunnable implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "Bug animation finished");
            EventBus.getDefault().post(new ShutdownEvent(ShutdownEvent.CAPTURED)); // Send STOP event to MiSnap
        }
    }


    /**
     * Side effects: The starting Intent is modified, so that the next time MiSnap starts,
     * it will be in manual capture mode.
     * Also, mCameraParamMgr is recreated so that subsequent checks to see which capture mode
     * is active will be correct.
     *
     * @return true if the starting Intent was successfully modified
     */
    boolean useManualCaptureModeNextTime() {
        return setCaptureModeNextTime(CameraApi.PARAMETER_CAPTURE_MODE_MANUAL);
    }

    /**
     * Side effects: The starting Intent is modified, so that the next time MiSnap starts,
     * it will be in auto capture mode.
     * Also, mCameraParamMgr is recreated so that subsequent checks to see which capture mode
     * is active will be correct.
     *
     * @return true if the starting Intent was successfully modified
     */
    boolean useAutoCaptureModeNextTime() {
        return setCaptureModeNextTime(CameraApi.PARAMETER_CAPTURE_MODE_AUTO);
    }

    private boolean setCaptureModeNextTime(int captureMode) {
        try {
            mParams.put(CameraApi.MiSnapCaptureMode, String.valueOf(captureMode));
        } catch (JSONException e) {
            return false;
        }
        mMiSnapIntent.putExtra(MiSnapApi.JOB_SETTINGS, mParams.toString());
        mCameraParamMgr = new CameraParamMgr(mParams);
        return true;
    }

    boolean setTorchStartingState(boolean isOn) {
        try {
            mParams.put(CameraApi.MiSnapTorchMode, isOn ? "2" : "0");
        } catch (JSONException e) {
            return false;
        }
        mMiSnapIntent.putExtra(MiSnapApi.JOB_SETTINGS, mParams.toString());
        mCameraParamMgr = new CameraParamMgr(mParams);
        return true;
    }

    public int getCurrentState() {
        return mCurrentState;
    }
}
