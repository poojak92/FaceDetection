package com.miteksystems.misnap.misnapworkflow.ui.overlay;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.miteksystems.imaging.JPEGProcessor;
import com.miteksystems.misnap.analyzer.MiSnapAnalyzerResult;
import com.miteksystems.misnap.events.CaptureCurrentFrameEvent;
import com.miteksystems.misnap.events.OnCapturedFrameEvent;
import com.miteksystems.misnap.events.OnStartedEvent;
import com.miteksystems.misnap.events.OnTorchStateEvent;
import com.miteksystems.misnap.events.ScaledPreviewSizeStickyEvent;
import com.miteksystems.misnap.events.TextToSpeechEvent;
import com.miteksystems.misnap.events.TorchStateEvent;
import com.miteksystems.misnap.mibidata.MibiData;
import com.miteksystems.misnap.misnapworkflow.R;
import com.miteksystems.misnap.misnapworkflow.params.UxpConstants;
import com.miteksystems.misnap.misnapworkflow.params.WorkflowConstants;
import com.miteksystems.misnap.misnapworkflow.params.WorkflowParameterReader;
import com.miteksystems.misnap.misnapworkflow.ui.AutoResizeTextView;
import com.miteksystems.misnap.misnapworkflow.ui.animation.FrameSequenceAnimation;
import com.miteksystems.misnap.misnapworkflow.ui.animation.MiSnapAnimation;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.DocType;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.params.ScienceParamMgr;
import com.miteksystems.misnap.storage.CameraInfoCacher;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CameraOverlay extends RelativeLayout {
    private static final String TAG = "CameraOverlay";
    // If true, allow the user to manually capture even in auto-capture mode
    private static final boolean ALWAYS_SHOW_MANUAL_CAPTURE_BUTTON = false;
    // Replaces MiSnapAnimatedBug - If true, animate the bug
    private static final boolean ANIMATED_BUG = true;
    // Replaces MiSnapAnimationRectangleStrokeWidth
    private static final int ANIMATION_STROKE_WIDTH = 20;
    // Replaces MiSnapAnimationRectangleCornerRadius
    private static final int ANIMATION_CORNER_RADIUS = 16;
    // Replaces MiSnapAnimationRectangleColor
    private static final int ANIMATION_COLOR = 0xED1C24; // NOTE: Transparency will be removed.
    // Replaces MiSnapSmartHintEnabled
    private static boolean SMART_HINT_ENABLED = false;
    // Replaces MiSnapSmartHintUpdatePeriod
    private static final int SMART_HINT_UPDATE_PERIOD = 1000;
    // Replaces MiSnapCameraVignetteImageEnabled
    private static final boolean VIGNETTE_IMAGE_ENABLED = false;
    // Replaces MiSnapCameraGuideImageEnabled, which was mis-named and actually refers to the Ghost image
    private static final boolean GHOST_IMAGE_ENABLED = true;
    // Replaces MiSnapGhostImageAlwaysOn
    private static final boolean GHOST_IMAGE_ALWAYS_ON = false;
    private static final double GHOST_IMAGE_ADDITIONAL_SCALE_FACTOR = 1.0d; // e.g. 1.15d means 1.15 - 1 = 15%
    // New workflow param for MiSnapGlareTracking
    private boolean DRAW_REALTIME_GLARE_OUTLINE;
    private static final boolean DRAW_REALTIME_DOC_OUTLINE = false;
    private static final int SMART_HINT_INITIAL_DELAY_MS = 3000;
    Context mActivityContext;
    Context mAppContext;
    LinearLayout errorContainer;
	ImageButton mHelpButton, mCaptureButton, mCancelButton, mFlashToggle;
    ImageButton mPoweredLogo;
	ImageView mGhostImage;
    ImageView mGaugeImage;
    ImageView mBalloonImage;
    ImageView mBugImage;
    ImageView mVignetteImage;
	CameraParamMgr mCameraParamMgr;
    ScienceParamMgr mScienceParamMgr;
    WorkflowParameterReader mWorkflowParamMgr;
    Bitmap mGhostBitmap;
    Bitmap mSnappedDoc;
	Animation mAnimationFadeOut, mAnimationFadeIn, mDocAnimation;
    protected List<HintBubble> mHintBubbles;
    protected HintBubble mCurrentHintBubble;
	boolean mGhostAnimationRunning;
    boolean mRemoveGaugeAnimationRunning;
    boolean drawDetectedRectangle;
    boolean drawDocCenter;
    boolean drawFinalFrame;
    boolean mBubblesDelayInProgress;
    boolean mAllDone;
    boolean mGhostMsgTTSPlayed;
    boolean mTorchStatus;
    private boolean wasGlareFound;
    private boolean mFrameCapturedIgnoreUXP;
    private FrameSequenceAnimation mBugSequence;
    private FrameSequenceAnimation mGaugeOpenSequence;
    private FrameSequenceAnimation mGaugeCloseSequence;
    private FrameSequenceAnimation mGaugeFinishSequence;
    AutoResizeTextView errorText;
	TextView testText;
    TextView instructionText;
    AutoResizeTextView mGhostImageTxt;
    // Setup the asynchronous messaging threads for user help
    Handler mHandler = new Handler();
    private static final int GAUGE_OPEN_ANIMATION_TIME_MS = 1250;
    private static final int GAUGE_FINISH_ANIMATION_TIME_MS = 400;
    private static final int GAUGE_CLOSE_ANIMATION_TIME_MS = 750;
    private static final int RECTANGLE_ANIMATION_INTERVAL_MILLIS = 70;
    private static final int RECTANGLE_ANIMATION_LENGTH_MILLIS = 600;
    private static final float RECTANGLE_ANIMATION_MIN_SCALE = 0.9f;
    private static final int BUG_ANIMATION_TIME_MS = 1600;
    private static final int GHOST_IMAGE_DELAY = 50;
    private float mDisplayWidth, mDisplayHeight;
    private Bitmap targetBitmap;
    Drawable mTargetImage;
    double mGaugeProgressFrame;
    LinkedList<Integer> mLinkedList;
    Object mSyncBlock = new Object();
    List<Point> detectedDocumentPoints = new ArrayList<Point>();
    List<Point> mRectangleAnimationPoints;
    List<Point> debugOutlineCorners = new ArrayList();
    int mPreviewWidth, mPreviewHeight;
    private Point mTarget = new Point();
    Matrix targetMatrix = new Matrix();
    Paint mDetectedRectanglePaint;
    Path mDetectedRectanglePath;
    long mRectangleAnimationStart;
    int mBalloonResID;
    private OrientationEventListener orientationEventListener;
    private Matrix mForward = new Matrix();
    private float[] mTemp = new float[2];
    private int mCurrentRotation;
    final static String TAG_TORCH_ON = "on";
    final static String TAG_TORCH_OFF = "off";
    byte[] mFinalFrameArray;
    int[] mDocumentCorner1;
    int[] mDocumentCorner2;
    int[] mDocumentCorner3;
    int[] mDocumentCorner4;
    Rect glareBox = null;
    private View.OnClickListener mOnClickListener;
    private int[][] mFourCorners;
    private static int mFpsDebugFrameCounter;
    private ProgressDialog mManualCapturePleaseWaitDialog;
    private DocType mDocType;

    public CameraOverlay(Context context, JSONObject params, OnClickListener onClickListener) {
        this(context, null, params, onClickListener, R.layout.misnap_your_camera_overlay);
    }
	
	public CameraOverlay(Context context, JSONObject params, OnClickListener onClickListener, int layoutId) {
		this(context, null, params, onClickListener, layoutId);
	}

    public CameraOverlay(Context context, AttributeSet attrs, JSONObject params, OnClickListener onClickListener, int layoutId) {
        this(context, attrs, 0, params, onClickListener, layoutId);
    }
	
	public CameraOverlay(Context context, AttributeSet attrs, int defStyle, JSONObject params, OnClickListener onClickListener, int layoutId) {
		super(context, attrs, defStyle);
		mActivityContext = context;
        mAppContext = context.getApplicationContext();
		mCameraParamMgr = new CameraParamMgr(params);
        mScienceParamMgr = new ScienceParamMgr(params);
        mWorkflowParamMgr = new WorkflowParameterReader(params);
        loadWorkflowParameters();
        mOnClickListener = onClickListener;
        mFrameCapturedIgnoreUXP = false;
        // Only used in video capture mode, but we init it to prevent NPEs
        mFourCorners = new int[4][2]; // 4 for corners; 2 for X,Y

        mDocType = new DocType(mCameraParamMgr.getRawDocumentType());
        // Read in workflow parameters

		// Setup all the overlay views
        View.inflate(context, layoutId, this);
        //setupButtons
        setupButtons();
        //set the paint object
        setupPaintObj();
        //setup hint bubbles
        setupHintBubbles();
        // initialize mPreviewWidth and mPreviewHeight
        setPreviewParameters();
        //register the event bus
        EventBus.getDefault().register(this);
        // CameraOverlay needs to know if the torch is on or not
        EventBus.getDefault().post(new TorchStateEvent("GET"));
        mFpsDebugFrameCounter = 0; // for debugging
    }

    private void loadWorkflowParameters() {
        DRAW_REALTIME_GLARE_OUTLINE = mWorkflowParamMgr.getGlareTracking() != 0;
    }

    private void setupPaintObj() {
        // setup rectangle paint
        mDetectedRectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDetectedRectanglePaint.setDither(true);
        mDetectedRectanglePaint.setStyle(Paint.Style.STROKE);
        mDetectedRectanglePaint.setStrokeJoin(Paint.Join.ROUND);
        mDetectedRectanglePaint.setStrokeCap(Paint.Cap.ROUND);
        mDetectedRectanglePaint.setPathEffect(new CornerPathEffect(ANIMATION_CORNER_RADIUS));
        mDetectedRectanglePaint.setAntiAlias(true);
        mDetectedRectanglePaint.setStrokeWidth(ANIMATION_STROKE_WIDTH);
        int tempColor = ANIMATION_COLOR | 0xFF000000; //No transparency
        mDetectedRectanglePaint.setColor(tempColor);
        //set the path obj
        mDetectedRectanglePath = new Path();
    }

    private void setupHintBubbles() {
        mHintBubbles = new ArrayList<>();
        mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.GLARE, R.drawable.misnap_error_reduceglare, R.string.id_glare));
        mHintBubbles.add(new CornerConfusionHintBubble(MiSnapAnalyzerResult.FrameChecks.LOW_CONTRAST, R.drawable.misnap_error_usedarkbackground, R.string.id_low_contrast));
        mHintBubbles.add(new CornerConfusionHintBubble(MiSnapAnalyzerResult.FrameChecks.BUSY_BACKGROUND, R.drawable.misnap_error_useplainbackground, R.string.id_busy_background));
        mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.ROTATION_ANGLE, R.drawable.misnap_error_center, R.string.id_hold_center));
        mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MAX_SKEW_ANGLE, R.drawable.misnap_error_center, R.string.id_hold_center));
        mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.HORIZONTAL_MINFILL, R.drawable.misnap_error_getcloser, R.string.id_get_closer));
        mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MIN_PADDING, R.drawable.misnap_error_tooclose, R.string.id_too_close));
        mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MAX_BRIGHTNESS, R.drawable.misnap_error_less_light, R.string.id_less_light));
        mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MIN_BRIGHTNESS, R.drawable.misnap_error_more_light, R.string.id_more_light));
        if(mCameraParamMgr.isCheck()){
            int wrongDocDrawableId = mCameraParamMgr.isCheckBack() ? R.drawable.misnap_error_flipcheck : R.drawable.misnap_error_usefrontcheck;
            int wrongDocSpeechId = mCameraParamMgr.isCheckBack() ? R.string.id_wrong_doc_check_back_expected : R.string.id_wrong_doc_check_front_expected;
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.WRONG_DOCUMENT, wrongDocDrawableId, wrongDocSpeechId));
        }
        mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.SHARPNESS, R.drawable.misnap_error_hold_steady, R.string.id_hold_steady));
        mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE, 0, 0));
    }

    private void setupButtons() {

        // so that onDraw will be called and redraws occur
        setWillNotDraw(false);

        mTargetImage = mActivityContext.getResources().getDrawable(R.drawable.misnap_doc_center);

		//help button
		mHelpButton = (ImageButton) findViewById(R.id.misnap_overlay_help_button);
		if(mHelpButton != null) {
            mHelpButton.setImageResource(R.drawable.misnap_button_help);
			mHelpButton.setOnClickListener(mOnClickListener);
		}
		//flash button
		mFlashToggle = (ImageButton) findViewById(R.id.overlay_flash_toggle);
		try {
			if(mFlashToggle != null) {
				mFlashToggle.setVisibility(View.VISIBLE);
				mFlashToggle.setImageResource(R.drawable.misnap_icon_flash_off);
				mFlashToggle.setOnClickListener(mOnClickListener);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//cancel button
		mCancelButton = (ImageButton) findViewById(R.id.overlay_cancel_button);
		if(mCancelButton != null) {
            mCancelButton.setImageResource(R.drawable.misnap_camera_cancel_icon);
			mCancelButton.setOnClickListener(mOnClickListener);
		}
		//capture button
		mCaptureButton = (ImageButton) findViewById(R.id.misnap_overlay_capture_button);
		if(mCaptureButton != null) {
            mCaptureButton.setImageResource(R.drawable.misnap_camera_shutter_icon);
			mCaptureButton.setOnClickListener(mOnClickListener);
		}

		testText = (TextView) findViewById(R.id.misnap_overlay_test_text);
		testText.setVisibility(View.VISIBLE);

        mVignetteImage = (ImageView) findViewById(R.id.misnap_vignette);
        setVignette();

        //powered by mitek button
        mPoweredLogo = (ImageButton) findViewById(R.id.misnap_overlay_mitek_logo);
        if(mPoweredLogo != null) {
            mPoweredLogo.setImageResource(R.drawable.misnap_powered_by_mitek);
        }
        //if accessibility is enabled, disable it on the following buttons
        disableAccessibilityOnButtons();
        postInvalidate();
    }

    private void disableAccessibilityOnButtons() {

        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            try {
                //Powered by Mitek logo
                ImageButton mitekImage = (ImageButton) findViewById(R.id.misnap_overlay_mitek_logo);
                mitekImage.setAlpha(192);
                mitekImage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            } catch(Exception e) {
                //do nothing
            }
        }
    }

    void updateUI(boolean hasTorchSupport) {
        if (hasTorchSupport) {
            mFlashToggle.setClickable(true);
            mFlashToggle.setVisibility(View.VISIBLE);
        } else {
            mFlashToggle.setClickable(false);
            mFlashToggle.setVisibility(View.INVISIBLE);
        }
    }

    public void initialize() {
        // set dimensions for the first time since the initial global layout listener calls this method
        updateDisplayDimensions();

        //set the right ghost image id
        initGhostImage();
        initGuideImage();

        errorContainer = (LinearLayout) findViewById(R.id.error);
        errorContainer.setVisibility(View.INVISIBLE);

        mBalloonImage = (ImageView) findViewById(R.id.misnap_balloon);

        mBugImage = (ImageView) findViewById(R.id.misnap_bug);
        mBugImage.setVisibility(View.INVISIBLE);

        errorText = (AutoResizeTextView) findViewById(R.id.error_msg);
        errorText.setMinTextSize(1);

        instructionText = (TextView) findViewById(R.id.misnap_check_text);
        //US2582 - configurable check front/back text
        String overlayText = mWorkflowParamMgr.getTextPrompt(
                mAppContext.getString(R.string.id_check_front_text),
                mAppContext.getString(R.string.id_check_front_text),
                mAppContext.getString(R.string.id_check_back_text));
        instructionText.setText(overlayText);
        instructionText.setContentDescription(overlayText);

        setOrientationListener(mAppContext);

        //update UI if device does not support the torch
        CameraInfoCacher cacher = new CameraInfoCacher(mAppContext, mCameraParamMgr.getUseFrontCamera());
		if(mCameraParamMgr != null && !cacher.hasTorch()) {
            updateUI(false);
		}
        postInvalidate();
        //others
        resetVariables();

        // This used to be called by MiSnapFragment:previewStartedState thru the UIManager.
        // Now we should call it ourselves.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showGhostImage();
                }
        }, GHOST_IMAGE_DELAY);

        // Show the gauge (thermometer) in video mode only
        if (mCameraParamMgr.isCurrentModeVideo()) {
            if (mGaugeImage != null && !mGaugeImage.isShown() && !gaugeAnimationRunning()) {
                Log.d(TAG, "start gauge animation()");        // removed by proguard
                mGaugeImage.setVisibility(View.VISIBLE);

                // set its background to our AnimationDrawable XML resource.
                mGaugeOpenSequence = MiSnapAnimation.createGaugeOpenAnim(mGaugeImage, mActivityContext);
                mGaugeOpenSequence.start();

                mHandler.postDelayed(mGaugeOpenFinishedRunner, GAUGE_OPEN_ANIMATION_TIME_MS);
            }
        }

        SMART_HINT_ENABLED = false;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SMART_HINT_ENABLED = true;
            }
        }, SMART_HINT_INITIAL_DELAY_MS);

        setUpManualCapturePleaseWaitDialog();
    }

    private void setUpManualCapturePleaseWaitDialog() {
        mManualCapturePleaseWaitDialog = new ProgressDialog(mActivityContext, R.style.MiSnapProgressDialog);
        mManualCapturePleaseWaitDialog.setMessage(mAppContext.getString(R.string.id_manual_capture_please_wait));
        mManualCapturePleaseWaitDialog.setCancelable(false);
        mManualCapturePleaseWaitDialog.setIndeterminateDrawable(ContextCompat.getDrawable(mActivityContext, R.drawable.misnap_icon));
        Window window = mManualCapturePleaseWaitDialog.getWindow();
        if (null != window) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.gravity = Gravity.CENTER;
            window.setAttributes(layoutParams);
        }
    }

    private void setVignette() {
        if (VIGNETTE_IMAGE_ENABLED) {
            int vignetteResID = 0;

            if (mDocType.isBillPay()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_remittance");
            } else if (mDocType.isCheckBack()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_checkback");
            } else if (mDocType.isCheckFront()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_checkfront");
            } else if (mDocType.isBalanceTransfer()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_balance_transfer");
            } else if (mDocType.isLicense()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_driver_license");
            } else if (mDocType.isBusinessCard()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_business_card");
            } else if (mDocType.isAutoInsurance()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_auto_insurance");
            } else if (mDocType.isVin()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_vin");
            } else if (mDocType.isW2()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_w2");
            } else if (mDocType.isPassport()){
                vignetteResID = getVignetteResourceID("mitek_vignette_passport");
            }

            try {
                // No custom vignette image i Drawable src. Use default Mitek image
                if (0 == vignetteResID) {
                    vignetteResID = getVignetteResourceID("misnap_mitek_vignette");
                }

                if (0 != vignetteResID) {
                    mVignetteImage.setBackgroundResource(vignetteResID);
                    mVignetteImage.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // if going from video mode-show vignette,
            // 	       to manual mode-hide vignette
            // need to hide view
            mVignetteImage.setVisibility(View.INVISIBLE);
        }
    }

    private int getVignetteResourceID(String specificVignetteName) {
        return mActivityContext.getResources().getIdentifier(specificVignetteName,
                "drawable",
                getContext().getPackageName());
    }

    private void setOrientationListener(Context context) {
        orientationEventListener = new OrientationEventListener(
                context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int angle) {
                onOrientationChange(angle);
            }
        };
        orientationEventListener.enable();
    }

    private void onOrientationChange(int angle) {

        if(mActivityContext == null || mAppContext==null || mFrameCapturedIgnoreUXP) {
            return;
        }

            // Check if an orientation switch occurred (i.e. the user rotated the device)
            int rotation = Utils.getDeviceOrientation(mActivityContext);
            if (rotation != mCurrentRotation) {
                Log.i(TAG, "Rotate from " + mCurrentRotation + " to " + rotation);
                mCurrentRotation = rotation;

                postInvalidate();
            }
        }

    private void stopOrientationListener() {
        if (null != orientationEventListener) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }
    }

    private void initGuideImage() {
        mGaugeImage = (ImageView) findViewById(R.id.misnap_overlay_gauge);
        if (mGaugeImage != null) {
            mGaugeImage.setVisibility(View.INVISIBLE);
        }
    }

    public void initGhostImage() {

		if(mGhostImage == null) {
			mGhostImage = (ImageView) findViewById(R.id.misnap_ghost_image);
		}
		if (mGhostImageTxt == null){
            mGhostImageTxt = (AutoResizeTextView) findViewById(R.id.misnap_ghost_text);
        }
		//get the guide image handle
		int lGhostImageId = getGhostImageDrawableId();
//        int lGhostImageTxt = getGhostImageStringId();
		//set it
		if(mGhostImage != null) {
			mGhostImage.setVisibility(View.INVISIBLE); //this call is must otherwise animation won't work as the view had never been rendered
			if(lGhostImageId > 0) {
				//we need to show the guide image
                try {
                    Bitmap lScaledGhostImage = BitmapFactory.decodeResource(mActivityContext.getResources(), lGhostImageId);
                    if(lScaledGhostImage != null) {
                        mGhostBitmap = scaleWithAspectRatio(lScaledGhostImage);
                        if(mGhostBitmap != null) {
                            if (shouldRotateGhostImage()) {
                                mGhostBitmap = JPEGProcessor.rotateBitmap(mGhostBitmap, -90);
                            }

                            mGhostImage.setImageBitmap(mGhostBitmap);

                            //adjust capture button
                            int adjustedHeight = (int)(Math.min(mGhostBitmap.getHeight(), mGhostBitmap.getWidth()) * .3);
                            RelativeLayout.LayoutParams currentParams = (RelativeLayout.LayoutParams)mCaptureButton.getLayoutParams();
                            currentParams.width = adjustedHeight;
                            currentParams.height = adjustedHeight;
                            mCaptureButton.setLayoutParams(currentParams);

                            if (Utils.getDeviceBasicOrientation(mAppContext) == Configuration.ORIENTATION_LANDSCAPE) {
                                LayoutParams params = (RelativeLayout.LayoutParams)mCaptureButton.getLayoutParams();
                                params.setMargins(0, pixelsToDP(mAppContext, 8), pixelsToDP(mAppContext, 8), 0);
                                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
                                mCaptureButton.setLayoutParams(params);
                            } else {
                                LayoutParams params = (RelativeLayout.LayoutParams)mCaptureButton.getLayoutParams();
                                RelativeLayout.LayoutParams logoLayoutParams = (LayoutParams) mPoweredLogo.getLayoutParams();
                                int topOfLogo = logoLayoutParams.height;
                                double bottomAreaHeight = (double)(mDisplayHeight - mGhostBitmap.getHeight()) / 2;
                                double bottomOfButtonOffset = ((bottomAreaHeight - topOfLogo) / 2) - ((double)params.height / 2) + topOfLogo;
                                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
//                                params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE); // TODO KW 2018-06-06:  why doesn't this work?
                                double rightOffset = (double)mDisplayWidth / 2 - ((double)params.width / 2);
                                params.setMargins(0, 0, (int)rightOffset, (int)bottomOfButtonOffset);
                                mCaptureButton.setLayoutParams(params);
                            }

                            //whether to show the MiSnap button
                            if (!mCameraParamMgr.isCurrentModeVideo() || ALWAYS_SHOW_MANUAL_CAPTURE_BUTTON) {
                                mCaptureButton.setVisibility(View.VISIBLE);
                            } else {
                                mCaptureButton.setVisibility(View.INVISIBLE);
                            }
                        }
                        lScaledGhostImage = null;
                    }
                } catch (Exception e) {
                    Log.w(TAG, e.toString());
                    mGhostImage.setImageDrawable(mActivityContext.getResources().getDrawable(lGhostImageId));
                }
                //add the animation effects to it
				mAnimationFadeOut = AnimationUtils.loadAnimation(mActivityContext, R.anim.misnap_fadeout);
				mAnimationFadeIn = AnimationUtils.loadAnimation(mActivityContext, R.anim.misnap_fadein);
                mDocAnimation = AnimationUtils.loadAnimation(mActivityContext, R.anim.misnap_balloon_animation);
			}
		}
		if (mGhostImageTxt != null){
            mGhostImageTxt.setVisibility(View.INVISIBLE); //this call is must otherwise animation won't work as the view had never been rendered
            //set content description for talk back
            Spanned lGhostImageTxt = Html.fromHtml(mActivityContext.getResources().getString(getGhostImageAccessibilityTextId()));
            if (lGhostImageTxt != null) {
                mGhostImage.setContentDescription(lGhostImageTxt.toString());

                if (!mCameraParamMgr.isPassport()) {
                    mGhostImageTxt.setText(lGhostImageTxt);
                }
            }
        }
	}

    private int getGhostImageDrawableId() {
        int rGhostImageId=-1;
        boolean fileFound=true;

        if (mDocType.isBillPay()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_remittance;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_remittance;
            }
        } else if (mDocType.isCheckBack()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_checkback;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_checkback;
            }
        } else if (mDocType.isCheckFront()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_checkfront;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_checkfront;
            }
        } else if (mDocType.isBalanceTransfer()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_balance_transfer;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_balance_transfer;
            }
        } else if (mDocType.isLicense()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_driver_license_landscape;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_driver_license_landscape;
            }
        } else if (mDocType.isIdCardFront()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_id_card;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_id_card;
            }
        } else if (mDocType.isIdCardBack()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_id_card;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_id_card;
            }
        } else if (mDocType.isAutoInsurance()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_auto_insurance_card;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_auto_insurance_card;
            }
        } else if (mDocType.isW2()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_w2;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_w2;
            }
        } else if (mDocType.isPassport()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_passport;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_passport;
            }
        }else if (mDocType.isBusinessCard()) {
            rGhostImageId = R.drawable.misnap_ghost_business_card;
        } else if (mDocType.isVin()) {
            rGhostImageId = R.drawable.misnap_manual_ghost_vin;
        } else {
            rGhostImageId = R.drawable.misnap_ghost_check_blank;
        }

        if(rGhostImageId <= 0){//if the drawable doesn't exists, use the standard one
            rGhostImageId = R.drawable.misnap_ghost_check_blank;
        }

        return rGhostImageId;
    }

    Bitmap scaleWithAspectRatio(Bitmap image){
        if (image != null) {
            // Scale the bitmap width based on the parameter, and maintain the image aspect ratio.
            int minFill;
            int longSide;

            if (shouldRotateGhostImage()) {
                minFill = mScienceParamMgr.getHorizontalFillMin();
                longSide = getHeight();
            } else {
                if (Utils.getDeviceBasicOrientation(mAppContext) == Configuration.ORIENTATION_PORTRAIT) {
                    minFill = mScienceParamMgr.getPortraitHorizontalFillMin();
                    longSide = getWidth();
                }else {
                    minFill = mScienceParamMgr.getHorizontalFillMin();
                    longSide = getWidth();
                }
            }

            double scaledImageWidth = longSide * minFill / 1000;
            scaledImageWidth *= GHOST_IMAGE_ADDITIONAL_SCALE_FACTOR;
            image = Bitmap.createScaledBitmap(
                    image,
                    (int)scaledImageWidth,
                    (int)scaledImageWidth * image.getHeight() / image.getWidth(),
                    true);
        }

        return image;
    }

    void setPreviewParameters() {
        try {
            CameraInfoCacher cacher = new CameraInfoCacher(mAppContext, mCameraParamMgr.getUseFrontCamera());

            mPreviewWidth = Integer.parseInt(cacher.getPreviewWidth());
            mPreviewHeight = Integer.parseInt(cacher.getPreviewHeight());

            // For portrait mode, we need to switch the width and height,
            // since preference does not know orientation
            if (Utils.getDeviceBasicOrientation(mActivityContext) == Configuration.ORIENTATION_PORTRAIT) {
                int temp = mPreviewWidth;
                mPreviewWidth = mPreviewHeight;
                mPreviewHeight = temp;
            }
        } catch (NumberFormatException e) {
            // Swallow NumberFormatExceptions that occur in unit tests because ParameterManager is mocked
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Only draw the glare outline while the hint message is appearing
        if (DRAW_REALTIME_GLARE_OUTLINE && wasGlareFound) {
            boolean isGlareHintMessageShowing = (mBalloonResID == R.drawable.misnap_error_reduceglare)
                    && (errorContainer != null)
                    && (errorContainer.getVisibility() == VISIBLE);
            if (isGlareHintMessageShowing) {
                drawGlareRectangle(canvas);
            }
        }

        //logic to configure and show the doc center image
        if (mTargetImage != null && null == targetBitmap
                && mDisplayWidth > 0 && mDisplayHeight > 0) {
            int side = (int) Math.min(mDisplayWidth / 10, 100); // 100x100 too big on small screens
            mTargetImage.setBounds(0, 0, side, side);
            targetBitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
            Canvas targetBitmapCanvas = new Canvas(targetBitmap);
            mTargetImage.draw(targetBitmapCanvas);
            targetBitmapCanvas = null;
        }

        if(drawFinalFrame) {
            drawFinalFrameOnCanvas(canvas);
        }

        if (drawDetectedRectangle) {
            //draw the rectangle
            drawQuoteRectangleUnquote(canvas);
        }

        if (drawDocCenter) {
            // Draw the target in middle of detected document
            targetMatrix.reset();
//            Log.i("Target","drawDocCenter target.x:" +mTarget.x+ " :TArget.y:" + mTarget.y);
            targetMatrix.postTranslate(mTarget.x - targetBitmap.getWidth() / 2,
                    mTarget.y - targetBitmap.getHeight() / 2);
            canvas.drawBitmap(targetBitmap, targetMatrix, mDetectedRectanglePaint);
        }
    }

    private void drawFinalFrameOnCanvas(Canvas lCanvas) {
        if(mFinalFrameArray != null && mFinalFrameArray.length > 0 && lCanvas != null) {
            try {
                Rect destRect = new Rect(0, 0, lCanvas.getWidth(), lCanvas.getHeight());
                if(mSnappedDoc == null) {
                    mSnappedDoc = BitmapFactory.decodeByteArray(mFinalFrameArray, 0, mFinalFrameArray.length);
                }
                Rect srcRect = new Rect(0, 0, mSnappedDoc.getWidth(), mSnappedDoc.getHeight());
                if(mSnappedDoc != null && srcRect != null && destRect != null) {
                    lCanvas.drawBitmap(mSnappedDoc, srcRect, destRect, mDetectedRectanglePaint);
                }
            } catch (Exception e) {
//                e.printStackTrace();
            }
        } else {
            if (lCanvas != null) {
                drawDetectedRectangle = false;
                // TODO: Why does the final camera preview image stay on screen? Overwrite it w/ black.
                Paint blackPaint = new Paint();
                blackPaint.setColor(0xFF000000);
                lCanvas.drawRect(0, 0, lCanvas.getWidth(), lCanvas.getHeight(), blackPaint);
            }
        }
    }

    private void drawQuoteRectangleUnquote(Canvas canvas) {
        composeRectanglePath(mRectangleAnimationPoints);
        canvas.drawPath(mDetectedRectanglePath, mDetectedRectanglePaint);
    }

    private void composeRectanglePath(List<android.graphics.Point> rect) {
        mDetectedRectanglePath.reset();

        if(rect == null || rect.size() <= 3) {		// Eliminate the rectangle
            mDetectedRectanglePath.moveTo(0f, 0f);
            mDetectedRectanglePath.lineTo(0f, 0f);
            return; // Can't make a polygon with less than 3 points
        }

        mDetectedRectanglePath.moveTo((float) rect.get(0).x, (float) rect.get(0).y);
        for(int i = 1; i < rect.size(); i++) {
            mDetectedRectanglePath.lineTo((float) rect.get(i).x, (float) rect.get(i).y);
        }
        mDetectedRectanglePath.close();
    }

    void hideDocCenterImage() {
        if(mTargetImage != null && mTargetImage.isVisible()) {
            drawDocCenter = false;
            mTargetImage.setVisible(false, false);
        }
    }

    public void showGhostImage() {
        if (mGhostImage == null || mGhostImageTxt == null) {
            return;
        }
        Log.d(TAG, "showGhostImage(): " + mGhostAnimationRunning +"-mGhostImage.isShown():"+mGhostImage.isShown());
        //reset the doc center image
        hideDocCenterImage();
        //check conditions valid to show the ghost image
		if (!mGhostImage.isShown() && !mGhostImageTxt.isShown() && !mGhostAnimationRunning &&
                (GHOST_IMAGE_ENABLED || GHOST_IMAGE_ALWAYS_ON)) {

            Log.d(TAG, "ghost image was not showing;");
			mGhostImage.startAnimation(mAnimationFadeIn);

			// use the drawable's dimensions instead of the view's because the view never updates its width/height on rotation
            mGhostImageTxt.setWidth(mGhostImage.getDrawable().getIntrinsicWidth());
            mGhostImageTxt.setHeight(mGhostImage.getDrawable().getIntrinsicHeight());
            if (shouldRotateGhostImage()) {
                mGhostImageTxt.setMaxLines(3);
            } else {
                mGhostImageTxt.setMaxLines(2);
            }
            mGhostImageTxt.setMaxTextSize((int) getResources().getDimension(R.dimen.misnapworkflow_help_screen_font_size));

            mGhostImageTxt.startAnimation(mAnimationFadeIn);
            Log.d(TAG, "mGhostAnimationRunning set to true");
			mGhostAnimationRunning = true;

            if (mGhostImage != null) {
                mGhostImage.setVisibility(View.VISIBLE);
            }
            if (mGhostImageTxt != null){
                mGhostImageTxt.setVisibility(View.VISIBLE);
            }

            //uxp event
            MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_GHOST_IMAGE_BEGINS);
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mGhostAnimationRunning = false;
                    Log.d(TAG, "mGhostAnimationRunning set to false");

                    //play the Accessbility message after 2 seconds
                    mHandler.postDelayed(mGhostImageAccessMessage, 2000);
                }
            }, mAnimationFadeIn.getDuration());

            postInvalidate();
        }
    }

    public void removeGhostImage(boolean bImmediateHide) {

        if (mGhostImage == null || mGhostImageTxt == null ||
                mAnimationFadeOut == null) {
            return;
        }

        Log.d(TAG, "mGhostImage.isShown(): " + mGhostImage.isShown() + ":mGhostAnimationRunning:" + mGhostAnimationRunning);

        if (mGhostImage.isShown() && mGhostImageTxt.isShown() && !mGhostAnimationRunning &&
                (!GHOST_IMAGE_ALWAYS_ON)) {

            Log.d(TAG, "removeGhostImage():mGhostAnimationRunning:" + mGhostAnimationRunning);        // removed by proguard

            //if visible, only then hide it
            if (bImmediateHide) {
                //hide it
                mGhostImage.setVisibility(View.INVISIBLE);
                mGhostImageTxt.setVisibility(View.INVISIBLE);
            } else {
                if (!mGhostAnimationRunning) {
                    mGhostImage.startAnimation(mAnimationFadeOut);
                    mGhostImageTxt.startAnimation(mAnimationFadeOut);
                    mGhostAnimationRunning = true;
                    Log.d(TAG, "mGhostAnimationRunning set to true2");
                    //uxp event
                    MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_GHOST_IMAGE_ENDS);
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            Log.d(TAG, "mGhostAnimationRunning set to false2");
                            mGhostAnimationRunning = false;
                            if (mGhostImage != null) {
                                mGhostImage.setVisibility(View.INVISIBLE);
                            }
                            if (mGhostImageTxt != null){
                                mGhostImageTxt.setVisibility(View.INVISIBLE);
                            }
                        }
                    }, mAnimationFadeOut.getDuration());
                }
            }

            postInvalidate();
        }
        //start the gauge open animation
        clearGuideBarQueue();
    }

    private boolean isGaugeOpenRunning() {
        return null != mGaugeOpenSequence && mGaugeOpenSequence.isRunning();
    }

    private boolean isGaugeFinishRunning() {
        return null != mGaugeFinishSequence && mGaugeFinishSequence.isRunning();
    }

    private boolean isGaugeCloseRunning() {
        return null != mGaugeCloseSequence && mGaugeCloseSequence.isRunning();
    }

    private boolean gaugeAnimationRunning() {
        return mRemoveGaugeAnimationRunning || isGaugeOpenRunning() || isGaugeCloseRunning() && isGaugeFinishRunning();
    }

    private void removeGaugeImage() {
        drawDocCenter = false;
        if (mGaugeImage.isShown() && !mRemoveGaugeAnimationRunning) {
            Log.d(TAG, "remove guide image: guide image was showing;");
            mGaugeImage.startAnimation(mAnimationFadeOut);
            Log.d(TAG, "mRemoveGaugeAnimationRunning() set to true"); // removed by proguard
            mRemoveGaugeAnimationRunning = true;
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    Log.d(TAG, "removeGaugeImage$onAnimationEnd()"); // removed by proguard
                    if (mGaugeImage != null) {
                        mGaugeImage.setVisibility(View.INVISIBLE);
                    }
                    mRemoveGaugeAnimationRunning = false;
                    clearGuideBarQueue();
                }
            }, mAnimationFadeOut.getDuration());

            postInvalidate();
        }
    }

    void clearGuideBarQueue() {
        //clear the queue
        if(mLinkedList != null) {
            mLinkedList.removeAll(mLinkedList);
        }
    }

    // Timeout for final animation to complete - remove with real animation
    Runnable mGaugeOpenFinishedRunner = new Runnable() {
        @Override
        public void run() {
            if (null != mGaugeOpenSequence && mGaugeOpenSequence.isRunning()) {
                mGaugeOpenSequence.stop();
                Log.d("MiSnapAnim", "gaugeOpenSequence.stop()");	// removed by proguard
            } else {
                Log.d("MiSnapAnim", "gaugeOpenSequence finished");	// removed by proguard
            }
        }
    };

    // Timeout for final animation to complete - remove with real animation
    Runnable mGhostImageAccessMessage = new Runnable() {
        @Override
        public void run() {
            Log.d("MiSnapAnim", "playGuideImgTalkBackMsg");
            // In case Accessbility is turned on
            if(mGhostMsgTTSPlayed == false) {
                playGuideImgTalkBackMsg();
                mGhostMsgTTSPlayed = true;
            }
        }
    };

    public void processGuideBarIncrementEvent(int lProgress) {
        if(!mGaugeImage.isShown() || gaugeAnimationRunning()){
            return;
        }
        synchronized (mSyncBlock) {
            //if the guide image is running wait, else increment the guide bar
            if (isGaugeOpenRunning()) {
                if (mLinkedList == null) {
                    mLinkedList = new LinkedList();
                }
                mLinkedList.add(lProgress);
            } else {
                //increment the guide bar to this much progress
                showGuideBarFrame(lProgress);
            }
        }
    }

    void showGuideBarFrame(int lProgress) {
        synchronized (mSyncBlock) {
            int lGuideBarFrameIndex = getGuideBarFrameIndex(lProgress);
            if (lGuideBarFrameIndex != 0) {
                try {
//                    Log.d("MiSnapAnim", "showGuideBarFrame: " + lProgress + " :time: " + System.currentTimeMillis());
                    mGaugeImage.setImageResource(lGuideBarFrameIndex);
                    postInvalidate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int getGuideBarFrameIndex(final int lProgress) {
        int resID = 0;
        String lUpdatedProgressVal=null;

        if(lProgress == 0 || lProgress == 5) {
            lUpdatedProgressVal = "0"+lProgress;
        } else {
            lUpdatedProgressVal = ""+lProgress;
        }

        mGaugeProgressFrame = (double)lProgress/100;

        try {
            resID = mActivityContext.getResources().getIdentifier(
                    "misnap_gauge_fill_" + lUpdatedProgressVal,
                    "drawable",
                    mAppContext.getPackageName());
//            Log.i("test", "resID:" + resID);
        } catch(Exception e) {
            //do nothing
            resID = 0;
        }
        return resID;
    }

    public void resetVariables() {
        //stop all animations
        if(mBugSequence != null){
            mBugSequence.stop();
        }
        if(mGaugeOpenSequence != null){
            mGaugeOpenSequence.stop();
        }
        if(mGaugeFinishSequence != null){
            mGaugeFinishSequence.stop();
        }

        if(mGaugeCloseSequence != null){
            mGaugeCloseSequence.stop();
        }
        mGhostAnimationRunning=false;
        mRemoveGaugeAnimationRunning=false;
        drawDetectedRectangle = false;
        drawDocCenter = false;
        mBubblesDelayInProgress=false;
        mGhostMsgTTSPlayed = false;
        if (mGaugeImage != null){
            mGaugeImage.setVisibility(View.INVISIBLE);
        }
        if (errorContainer != null) {
            errorContainer.setVisibility(View.INVISIBLE);
        }
    }

    public void cleanup() {
        postInvalidate(); // Prevents a black box from appearing where the bug animation was on the Galaxy S3
		mGhostAnimationRunning = false;
        stopOrientationListener();
        resetVariables();
        if (mCurrentHintBubble != null) {
            mCurrentHintBubble.clearBubbleAnimation();
        }
        if (mManualCapturePleaseWaitDialog != null) {
            mManualCapturePleaseWaitDialog.dismiss();
        }
        mHandler.removeCallbacksAndMessages(null); // Specifying a null token removes ALL callbacks and messages
        if(EventBus.getDefault().isRegistered(this)) {
            //unregister the event bus
            try {
                EventBus.getDefault().unregister(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(mGhostBitmap != null) {
//            mGhostBitmap.recycle();
            mGhostBitmap = null;
        }
        if(mGhostImage != null) {
            mGhostImage.setImageResource(0);
            mGhostImage.setImageDrawable(null);
            mGhostImage.setImageResource(android.R.color.transparent);
            mGhostImage = null;
        }
        if(targetBitmap != null) {
//            targetBitmap.recycle();
            targetBitmap = null;
        }
        if(mSnappedDoc != null) {
            mSnappedDoc = null;
        }
        if(mGaugeImage != null) {
            mGaugeImage.setImageResource(0);
            mGaugeImage.setImageDrawable(null);
            mGaugeImage.setImageResource(android.R.color.transparent);
            mGaugeImage = null;
        }
        if(mVignetteImage != null) {
            mVignetteImage.setImageResource(0);
            mVignetteImage.setImageDrawable(null);
            mVignetteImage.setImageResource(android.R.color.transparent);
            mVignetteImage = null;
        }
        if(errorContainer != null) {
            errorContainer = null;
            mBalloonImage.setImageResource(0);
            mBalloonImage.setImageDrawable(null);
            mBalloonImage.setImageResource(android.R.color.transparent);
        }
        if(mFlashToggle != null) {
            mFlashToggle.setImageResource(0);
            mFlashToggle.setImageDrawable(null);
            mFlashToggle.setImageResource(android.R.color.transparent);
            mFlashToggle = null;
        }

        if(mHelpButton != null) {
            mHelpButton.setImageResource(0);
            mHelpButton.setImageDrawable(null);
            mHelpButton.setImageResource(android.R.color.transparent);
            mHelpButton = null;
        }

        if(mCaptureButton != null) {
            mCaptureButton.setImageResource(0);
            mCaptureButton.setImageDrawable(null);
            mCaptureButton.setImageResource(android.R.color.transparent);
            mCaptureButton = null;
        }

        if(mCancelButton != null) {
            mCancelButton.setImageResource(0);
            mCancelButton.setImageDrawable(null);
            mCancelButton.setImageResource(android.R.color.transparent);
            mCancelButton = null;
        }

        if(mPoweredLogo != null) {
            mPoweredLogo.setImageResource(0);
            mPoweredLogo.setImageDrawable(null);
            mPoweredLogo.setImageResource(android.R.color.transparent);
            mPoweredLogo = null;
        }

        if (mTargetImage != null) {
            mTargetImage = null;
        }

        if(mBugImage != null) {
            mBugImage = null;
        }

        // Moved from seeIfAnimationsDone
        mBugSequence = null;
        mGaugeOpenSequence = null;
        mGaugeCloseSequence = null;
        mGaugeFinishSequence = null;
        mFinalFrameArray = null;
        //clear the Activity handles
        mActivityContext=null;
        mAppContext=null;
        //TODO:clean up instructionText
        System.gc();    //immediately clears the fragmented and unreferenced memory chunks out
	}

	public void toggleTorch(boolean bTorchState) {
		if (bTorchState) {
			mFlashToggle.setImageResource(R.drawable.misnap_icon_flash_on);
            //some customers are requesting that flash state should also be read
            mFlashToggle.setContentDescription(mAppContext.getString(R.string.id_overlay_flash_on));
            mFlashToggle.setTag(TAG_TORCH_ON);
		} else {
			mFlashToggle.setImageResource(R.drawable.misnap_icon_flash_off);
            mFlashToggle.setContentDescription(mAppContext.getString(R.string.id_overlay_flash_off));
            mFlashToggle.setTag(TAG_TORCH_OFF);
		}
	}

	public void showFPSData(String sData) {
		if (sData != null && testText != null) {
			testText.setVisibility(View.VISIBLE);
			testText.setText(sData);
		}
	}

    public void hideButtons() {
        if(mFlashToggle != null) {
            mFlashToggle.setVisibility(View.GONE);
        }
        if(mHelpButton != null) {
            mHelpButton.setVisibility(View.GONE);
        }
        if(mCancelButton != null) {
            mCancelButton.setVisibility(View.GONE);
        }
        if(mCaptureButton != null) {
            mCaptureButton.setVisibility(View.GONE);
        }
        if(mPoweredLogo != null) {
            mPoweredLogo.setVisibility(View.GONE);
        }
    }

    private List<Point> clonePoints(List<Point> points) {
        List<Point> out = new ArrayList<Point>();
        for(int i=0; i<4; i++) {
            Point p = points.get(i);
            out.add(new Point(p.x, p.y));
        }
        return out;
    }

    private Point _mTarget = null;
    private Point docCenter(List<android.graphics.Point> points) {
        if (null == _mTarget)
            _mTarget = new Point();	// Reduce allocations during operation

        _mTarget.x
                = (points.get(0).x + points.get(1).x + points.get(2).x + points.get(3).x) >> 2;
        _mTarget.y
                = (points.get(0).y + points.get(1).y + points.get(2).y + points.get(3).y) >> 2;
        return _mTarget;
    }

    private void startGaugeCloseAnimation() {
        if (isGaugeCloseRunning()) {
            return;
        }
        //reset the doc center image
        drawDocCenter = false;

        if (mGaugeImage != null) {
            mGaugeImage.setVisibility(View.VISIBLE);
            Log.v("MiSnapAnim", "mGaugeCloseSequence.start()");
            mGaugeCloseSequence = MiSnapAnimation.createGaugeCloseAnim(mGaugeImage, mActivityContext);
            //        mGaugeCloseSequence.setOnAnimationStoppedListener(new OnAnimationStoppedListener() {
            //               @Override
            //               public void onAnimationStopped() {
            //                   //hide the gauge image as we are done with it
            //                   mGaugeImage.setVisibility(View.INVISIBLE); //already animated away; no need to fade
            //                   postInvalidate();//refresh the view
            //                   seeIfAnimationsDone();
            //               }
            //           }
            //        );
            mGaugeCloseSequence.start();
            postInvalidate();//refresh the view
            mHandler.postDelayed(mGaugeCloseAnimationRunner, GAUGE_CLOSE_ANIMATION_TIME_MS);
        }
    }

    private void startGaugeFullAnimation() {
        if (isGaugeFinishRunning()) {
            return;
        }
        mGaugeImage.setVisibility(View.VISIBLE);
        mGaugeFinishSequence = MiSnapAnimation.createGaugeFinishAnim(mGaugeImage, mActivityContext);
        mGaugeFinishSequence.start();
        Log.v("MiSnapAnim", "gaugeFullSequence.start()");
        mHandler.postDelayed(mGaugeFinishRunner, GAUGE_FINISH_ANIMATION_TIME_MS);
    }

    // Timeout for final animation to complete - remove with real animation
    Runnable mGaugeFinishRunner = new Runnable() {
        @Override
        public void run() {
            if (null != mGaugeFinishSequence && mGaugeFinishSequence.isRunning()) {
                mGaugeFinishSequence.stop();
                Log.d("MiSnapAnim", "gaugeFinishSequence.stop()");		// removed by proguard
            } else {
                Log.d("MiSnapAnim", "gaugeFinishSequence finished");	// removed by proguard
            }
            startGaugeCloseAnimation();
        }
    };

    Runnable mGaugeCloseAnimationRunner = new Runnable() {
             @Override
        public void run() {
            if (null != mGaugeCloseSequence && mGaugeCloseSequence.isRunning()) {
                mGaugeCloseSequence.stop();
                Log.d("MiSnapAnim", "gaugeCloseSequence.stop()");		// removed by proguard
            } else {
                Log.d("MiSnapAnim", "gaugeCloseSequence finished");	// removed by proguard
            }
                 //hide the gauge image as we are done with it
                 mGaugeImage.setVisibility(View.INVISIBLE); //already animated away; no need to fade
                 postInvalidate();//refresh the view
                 seeIfAnimationsDone();
             }
    };

    private void seeIfAnimationsDone() {
        if (!gaugeAnimationRunning() && !isBugAnimationRunning()&& !mAllDone) {
            mAllDone = true;
        } else {
            Log.d("MiSnapAnim", "waiting to be done");	// removed by proguard
        }
    }

    public void drawReplayFrame(byte[] finalFrame) {
        mFinalFrameArray = finalFrame;
        if (mFinalFrameArray != null) {
            mSnappedDoc = null; // reload the latest frame
            drawFinalFrame = true;
        }

        postInvalidate(); // Redraw needed
    }

    private void drawRectangle(@Nullable List<Point> fourCorners) {
        Log.d("MiSnapAnim", "drawRectangle - start");
        if(!mCameraParamMgr.isCurrentModeVideo()) {
            return;
        }

        if(mHandler != null) {
            if (mCurrentHintBubble != null) {
                mCurrentHintBubble.clearBubbleAnimation();
            }
            //clear the balloon animation callback
            mHandler.removeCallbacks(mBalloonCheckRunner);
        }

        int[][] lCorner = new int[4][2];
        if (null == fourCorners) {
            // If the four corners are null, set them to the outside of the camera preview
            lCorner[0] = new int[]{0, 0};
            lCorner[1] = new int[]{mPreviewWidth, 0};
            lCorner[2] = new int[]{mPreviewWidth, mPreviewHeight};
            lCorner[3] = new int[]{0, mPreviewHeight};
        } else {
            for (int i = 0; i < fourCorners.size(); ++i) {
                lCorner[i] = new int[]{fourCorners.get(i).x, fourCorners.get(i).y};
            }
        }

        double displayToPreviewRatioX = (double) mDisplayWidth / mPreviewWidth;
        double displayToPreviewRatioY = (double) mDisplayHeight / mPreviewHeight;

        detectedDocumentPoints.clear();
        for (int i = 0; i < 4; ++i) {
            detectedDocumentPoints.add(new Point(
                    (int) (lCorner[i][0] * displayToPreviewRatioX),
                    (int) (lCorner[i][1] * displayToPreviewRatioY)));
        }

        // Calculate document center
        mTarget = docCenter(detectedDocumentPoints);
        mRectangleAnimationPoints = clonePoints(detectedDocumentPoints);
        mRectangleAnimationStart = System.currentTimeMillis();
        mHandler.post(mRectangleAnimationRunner);
    }

    // Run the rectangle animation once
    private Runnable mRectangleAnimationRunner = new Runnable() {
        @Override
        public void run() {
            long delta = System.currentTimeMillis() - mRectangleAnimationStart;
            mHandler.postDelayed(mRectangleAnimationRunner, RECTANGLE_ANIMATION_INTERVAL_MILLIS);
                drawDetectedRectangle = true;
            float s = Math.abs(RECTANGLE_ANIMATION_LENGTH_MILLIS/2 - delta);
            s = Math.abs( s / (RECTANGLE_ANIMATION_LENGTH_MILLIS/2));
            if (delta < (RECTANGLE_ANIMATION_LENGTH_MILLIS)) {
                s = 1f - ((1f - s)*(1f - RECTANGLE_ANIMATION_MIN_SCALE));
                // slightly enlarge each call
                mRectangleAnimationPoints = zoomRectangle(detectedDocumentPoints, s);
                postInvalidate();
            }
            else {
                // Stop this animation
                mHandler.removeCallbacks(mRectangleAnimationRunner);
                s = 1f;
                // slightly enlarge each call
                mRectangleAnimationPoints = zoomRectangle(detectedDocumentPoints, s);
                postInvalidate();
            }
        }
    };

    private List<Point> zoomRectangle(List<Point> points, float scale) {
        List<Point> newPoints = clonePoints(points);
        // Find center
        mTarget = docCenter(newPoints);

        // Find 4 lines to corners and scale
        for(Point p: newPoints) {
            int offsetX = p.x - mTarget.x;
            int offsetY = p.y - mTarget.y;
            p.x = (int) (scale*offsetX + mTarget.x);
            p.y = (int) (scale*offsetY + mTarget.y);
        }

        return newPoints;
    }

    public void snapshotGood(byte[] finalFrame, List<Point> fourCorners) {
        mHandler.removeCallbacks(mBalloonCheckRunner);

        //show the actual frame
        drawReplayFrame(finalFrame);

        if (mCameraParamMgr.isCurrentModeVideo()) {
            errorContainer.clearAnimation();
            drawRectangle(fourCorners);
            startGaugeFullAnimation();
        } else {
            showManualCapturePressedPleaseWait(false);
            removeGhostImage(true);
        }

        // Do final bug animation always
        startBugFinalAnimation();
    }

    public void drawDocumentCenter(int[][] l4Corners) {
        if(!mGaugeImage.isShown() || gaugeAnimationRunning()) {
            //required delay to show the next bubble is not timed out yet
            return;
        }

        if(l4Corners != null) {
            mDocumentCorner1 = l4Corners[0];
            mDocumentCorner2 = l4Corners[1];
            mDocumentCorner3 = l4Corners[2];
            mDocumentCorner4 = l4Corners[3];

            double displayToPreviewRatioX = (double) mDisplayWidth / mPreviewWidth;
            double displayToPreviewRatioY = (double) mDisplayHeight / mPreviewHeight;
            if(detectedDocumentPoints != null) {
                detectedDocumentPoints.removeAll(detectedDocumentPoints);
                //first point
                detectedDocumentPoints.add(new Point((int) (mDocumentCorner1[0] * displayToPreviewRatioX),
                        (int) (mDocumentCorner1[1] * displayToPreviewRatioY)));
                detectedDocumentPoints.add(new Point((int) (mDocumentCorner2[0] * displayToPreviewRatioX),
                        (int) (mDocumentCorner2[1] * displayToPreviewRatioY)));
                detectedDocumentPoints.add(new Point((int) (mDocumentCorner3[0] * displayToPreviewRatioX),
                        (int) (mDocumentCorner3[1] * displayToPreviewRatioY)));
                detectedDocumentPoints.add(new Point((int) (mDocumentCorner4[0] * displayToPreviewRatioX),
                        (int) (mDocumentCorner4[1] * displayToPreviewRatioY)));
            }

            drawDocCenter = true;

            // Calculate document center
            mTarget = docCenter(detectedDocumentPoints);
//            Log.i("Target","target.x:" +mTarget.x+ " :TArget.y:" + mTarget.y);
            //update the view
            postInvalidate();
        }
    }

    final Runnable mBalloonCheckRunner = new Runnable() {
        @Override
        public void run() {
            Log.d("MiSnapAnim", "mBalloonCheckRunner - baloon timer over");
            mBubblesDelayInProgress=false;
        }
    };

    private boolean isBugAnimationRunning() {
        return null != mBugSequence && mBugSequence.isRunning();
    }

    Runnable mBugAnimationRunner = new Runnable() {
        @Override
        public void run() {
            if (isBugAnimationRunning()) {
                mBugSequence.stop();
                Log.d("MiSnapAnim", "bugSequence.stop()");		// removed by proguard
            } else {
                Log.d("MiSnapAnim", "bugSequence finished");	// removed by proguard
            }
            seeIfAnimationsDone();
        }
    };

    private void startBugFinalAnimation() {
        drawDocCenter = false;
        if (isBugAnimationRunning()) {
            return;
        }
        mBugImage.setVisibility(View.VISIBLE);
        mHandler.postDelayed(mBugAnimationRunner, BUG_ANIMATION_TIME_MS);	// Trigger the stop

        if (ANIMATED_BUG) {
            mBugSequence = MiSnapAnimation.createBugAnim(mBugImage, mActivityContext);
        } else {
            mBugSequence = MiSnapAnimation.createBugStill(mBugImage, mActivityContext);
        }
        Log.d("MiSnapAnim", "bugSequence.start()");	// removed by proguard
        EventBus.getDefault().post(new TextToSpeechEvent(getResources().getString(R.string.id_overlay_snap_message)));
        mBugSequence.start();
    }

    void playGuideImgTalkBackMsg() {
        int spokenTextId = getGhostImageAccessibilityTextId();

        EventBus.getDefault().post(new TextToSpeechEvent(spokenTextId));
    }

    int getGhostImageAccessibilityTextId() {
        int spokenAccessibilityTextId = 0;

        if (mDocType.isBillPay()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.id_ghost_image_remittance;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_remittance_manual;
            }
        } else if (mDocType.isCheckBack()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.id_ghost_image_check;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_check_manual;
            }
        } else if (mDocType.isCheckFront()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.id_ghost_image_check;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_check_manual;
            }
        } else if (mDocType.isBalanceTransfer()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.id_ghost_image_remittance;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_remittance_manual;
            }
        } else if (mDocType.isLicense()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.id_ghost_image_drivers_license;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_drivers_license_manual;
            }
        } else if (mDocType.isIdCardFront()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.id_ghost_image_id_card;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_id_card_manual;
            }
        } else if (mDocType.isIdCardBack()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.id_ghost_image_id_card;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_id_card_manual;
            }
        } else if (mDocType.isAutoInsurance()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
            spokenAccessibilityTextId = R.string.id_ghost_image_insurance_card;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_insurance_card_manual;
            }
        } else if (mDocType.isVin()) {
            spokenAccessibilityTextId = R.string.id_ghost_image_vin_manual;
        } else if (mDocType.isW2()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.id_ghost_image_w2;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_w2_manual;
            }
        } else if (mDocType.isPassport()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.id_ghost_image_passport;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_passport_manual;
            }
        } else {
            //default case
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.id_ghost_image_document_portrait;
            } else {
                spokenAccessibilityTextId = R.string.id_ghost_image_document_portrait_manual;
            }
        }
        //set content description for talk back
        return spokenAccessibilityTextId;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnTorchStateEvent event){
        if ("SET".equals(event.function) || "GET".equals(event.function)) {
            mTorchStatus = event.currentTorchState == 1 ? true : false;
            // use the correct ghost image
            toggleTorch(mTorchStatus);
        }
    }

    public boolean getTorchStatus() {
        return mTorchStatus;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MiSnapAnalyzerResult event) {
        // Processing is done on the manually captured frame now. Don't show any UI for it tho.
        if (!mCameraParamMgr.isCurrentModeVideo()) {
            return;
        }

        mFourCorners = event.getFourCorners(); // Used to draw the outline around the captured image later

        if (DRAW_REALTIME_DOC_OUTLINE) {
            drawFourCorners(mFourCorners);
        }

        boolean fourCornersFound = event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE);
        boolean isCloseEnough = event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.HORIZONTAL_MINFILL);
        boolean isDocCentered = event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.MIN_PADDING);

        // show glare box only if four corners have been found
        if (fourCornersFound) {
            updateGlareRectangle(event.getGlareRect(), !event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.GLARE));
        }

        // Show the ghost image if the four corners are not reliably found
        if (fourCornersFound && isDocCentered) {
            //MiSnap has detected the 4 corners of the document
            // Hide the guide image and show the thermometer
            //if always-on parameter is set, do not hide the ghost image
            //remove the ghost image
            removeGhostImage(false);
            //show the doc center
            drawDocumentCenter(mFourCorners);
        } else {
            //4 corners not detected
            showGhostImage();
        }

        int numFrameChecksPassed = 0;
        boolean atLeastOneCheckFailed = false;
        for (HintBubble hint : mHintBubbles) {
            // skip glare check if four corners weren't found
            if (MiSnapAnalyzerResult.FrameChecks.GLARE == hint.checkThatNeedsToFail && !fourCornersFound) {
                continue;
            }

            // Don't check the low contrast or busy background hints if the four corners were found (we will still snap if there's a busy background or low contrast)
            // the cases where we don't want to display a hint bubble are:
            // * it's a hint bubble for low contrast or busy background, AND:
            // * 4c confidence is high (since the reason to let the user know about low contrast or busy background is because 4c confidence is low), AND:
            // * the min padding threshold has been met
            if ( !hint.onlyCheckIfFourCornersNotFound || !fourCornersFound || !isDocCentered ) {
                if (!event.getCheckPassed(hint.checkThatNeedsToFail)) {
                    if (!atLeastOneCheckFailed) { // only show hint for the first failed check in the priority-order list of checks
                        atLeastOneCheckFailed = true;
                        mCurrentHintBubble = hint;
                        mCurrentHintBubble.startBalloonOpenAnimation();
                    }
                    continue;
                }
            }
            numFrameChecksPassed++;
        }

        double progress = (double)numFrameChecksPassed / mHintBubbles.size();
        progress = roundToNearestFivePercent(progress);
        processGuideBarIncrementEvent((int)(progress * 100));

//        displayPassFailResultsForDebugging(event);
    }

    private void displayPassFailResultsForDebugging(MiSnapAnalyzerResult event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Frame: ").append(++mFpsDebugFrameCounter);
        sb.append("Glare: ").append(getFpsResultMessage(event, MiSnapAnalyzerResult.FrameChecks.GLARE)).append("\n");
        sb.append("Corners: ").append(getFpsResultMessage(event, MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE)).append("\n");
        sb.append("Contrast: ").append(getFpsResultMessage(event, MiSnapAnalyzerResult.FrameChecks.LOW_CONTRAST)).append("\n");
        sb.append("Busy: ").append(getFpsResultMessage(event, MiSnapAnalyzerResult.FrameChecks.BUSY_BACKGROUND)).append("\n");
        boolean anglePassed = event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.ROTATION_ANGLE) && event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.MAX_SKEW_ANGLE);
        sb.append("Angle: ").append(anglePassed ? "OK" : "FAIL").append("\n");
        sb.append("Fill: ").append(getFpsResultMessage(event, MiSnapAnalyzerResult.FrameChecks.HORIZONTAL_MINFILL)).append("\n");
        sb.append("Pad: ").append(getFpsResultMessage(event, MiSnapAnalyzerResult.FrameChecks.MIN_PADDING)).append("\n");
        sb.append("MinBright: ").append(getFpsResultMessage(event, MiSnapAnalyzerResult.FrameChecks.MIN_BRIGHTNESS)).append("\n");
        sb.append("MaxBright: ").append(getFpsResultMessage(event, MiSnapAnalyzerResult.FrameChecks.MAX_BRIGHTNESS)).append("\n");
        sb.append("MICR: ").append(getFpsResultMessage(event, MiSnapAnalyzerResult.FrameChecks.WRONG_DOCUMENT)).append("\n");
        sb.append("Sharp: ").append(getFpsResultMessage(event, MiSnapAnalyzerResult.FrameChecks.SHARPNESS)).append("\n");

        showFPSData(sb.toString());
    }

    private String getFpsResultMessage(MiSnapAnalyzerResult event, MiSnapAnalyzerResult.FrameChecks check) {
        if (event.getCheckPassed(check)) {
            return "OK";
        } else {
            return "FAIL";
        }
    }

    private double roundToNearestFivePercent(double percent) {
        return Math.round(percent * 20) / 20.0d;
    }

    final boolean SCALE = true;
    private void drawFourCorners(int[][] fourCorners) {
        debugOutlineCorners.clear();

        double displayToPreviewRatioX;
        double displayToPreviewRatioY;
        if (SCALE) {
            displayToPreviewRatioX = (double) mDisplayWidth / mPreviewWidth;
            displayToPreviewRatioY = (double) mDisplayHeight / mPreviewHeight;
        } else {
            displayToPreviewRatioX = 1.0d;
            displayToPreviewRatioY = 1.0d;
        }

        for (int i = 0; i < 4; i++) {
            debugOutlineCorners.add(new Point(
                    (int)(fourCorners[i][0] * displayToPreviewRatioX),
                    (int)(fourCorners[i][1] * displayToPreviewRatioY)));
        }

        Log.w("Scale", "X="+displayToPreviewRatioX + ", Y="+displayToPreviewRatioY);
        Log.w("Points", debugOutlineCorners.get(0).x + "," + debugOutlineCorners.get(0).y + "-" + debugOutlineCorners.get(2).x + "," + debugOutlineCorners.get(2).y);

        mHandler.post(mPerFrameFourCornersRunner);
    }

    private int[][] scaleGlareCoordinatesToScreen(int[][] unscaled){

        int[][] result = new int[2][2];
        double displayToPreviewRatioX = (double) mDisplayWidth / mPreviewWidth;
        double displayToPreviewRatioY = (double) mDisplayHeight / mPreviewHeight;

        for (int i = 0; i < 2; i++) {
            result[i][0] = (int)(unscaled[i][0] * displayToPreviewRatioX);
            result[i][1] = (int)(unscaled[i][1] * displayToPreviewRatioY);
        }
        return result;
    }

    private Runnable mPerFrameFourCornersRunner = new Runnable() {
        @Override
        public void run() {
            drawDetectedRectangle = true;
            mRectangleAnimationPoints = zoomRectangle(debugOutlineCorners, 1f);
            postInvalidate();
        }
    };

    // Received when MiSnap has captured a document image
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnCapturedFrameEvent event) {
        //Once frame has been captured, do not write any UI related UXP data in the frame
        mFrameCapturedIgnoreUXP = true;

        hideButtons();

        // Pick apart the event result to find the captured image and potentially the four corners
        Intent returnIntent = event.returnIntent;
        byte[] capturedImage = returnIntent.getByteArrayExtra(MiSnapApi.RESULT_PICTURE_DATA);
        ArrayList<Point> points = returnIntent.getParcelableArrayListExtra(MiSnapApi.RESULT_FOUR_CORNERS);
        snapshotGood(capturedImage, points);

        // tell UI manager to do vibrate
        // TODO KW 2018-06-01:  need to remove sendMsgToUIFragment entirely; this is in camera, too.
        Utils.sendMsgToUIFragment(mAppContext,
                WorkflowConstants.UI_DO_VIBRATE, null, null, null, null, null);
    }

    // Received when starting to manually snap a frame
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CaptureCurrentFrameEvent event) {
        Utils.sendMsgToUIFragment(mAppContext, WorkflowConstants.UI_FRAGMENT_SNAP_BUTTON_CLICKED);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnStartedEvent event) {
        showGhostImage();
    }

    private void updateGlareRectangle(int[][] rect, boolean wasGlareFound){
        int[][] scaledRect = scaleGlareCoordinatesToScreen(rect);
        glareBox = new Rect(scaledRect[0][0], scaledRect[0][1], scaledRect[1][0], scaledRect[1][1]);
        glareBox.sort();
        this.wasGlareFound = wasGlareFound;
        postInvalidate();
    }

    private void drawGlareRectangle(Canvas canvas){
        if(glareBox != null){
            canvas.drawRect(glareBox, mDetectedRectanglePaint);
        }
    }

    @Deprecated
    protected void startBalloonOpenAnimation(int id) {
    }

    protected class HintBubble {
        MiSnapAnalyzerResult.FrameChecks checkThatNeedsToFail;
        int balloonDrawableResId;
        int speechResId;
        boolean onlyCheckIfFourCornersNotFound; // this hint is relevant only if four corners weren't found

        public HintBubble(MiSnapAnalyzerResult.FrameChecks checkThatNeedsToFail, int balloonDrawableResId, int speechResId) {
            this.checkThatNeedsToFail = checkThatNeedsToFail;
            this.balloonDrawableResId = balloonDrawableResId;
            this.speechResId = speechResId;
            this.onlyCheckIfFourCornersNotFound = false;
        }

        public void startBalloonOpenAnimation() {
            // Check if bubbles are disabled or if the required delay to show the next bubble
            // has not timed out yet
            if ( !SMART_HINT_ENABLED
                    || mBubblesDelayInProgress
                    || mGaugeImage.getVisibility() != VISIBLE
                    || gaugeAnimationRunning()
                    || mGhostAnimationRunning) {
                return;
            }
            Log.i("Target","startBalloonOpenAnimation start");
            //get the right bubble to show
            mBalloonResID = balloonDrawableResId;

            float xOffset = mDisplayWidth/2- errorContainer.getWidth()/2;
            if (drawDocCenter) {
                if(mGaugeProgressFrame > 0) {
                    xOffset = (float) (mGaugeProgressFrame * (mGaugeImage.getRight() - mGaugeImage.getLeft())
                            + mGaugeImage.getLeft()
                            - errorContainer.getWidth()); // Changed from getWidth/2 to getWidth so that the balloon isn't ever squished at the right edge of the screen
                } else {
                    xOffset = (float) ((mGaugeImage.getRight() - mGaugeImage.getLeft())*0.5
                            + mGaugeImage.getLeft()
                            - errorContainer.getWidth()); // Changed from getWidth/2 to getWidth so that the balloon isn't ever squished at the right edge of the screen
                }
                mTargetImage.setVisible(false, false);
            }

            mBalloonImage.setImageResource(mBalloonResID);
            mBalloonImage.setTag(mBalloonResID);
            errorText.setText(speechResId);
            //make sure to adjust text box to match new size
            errorText.reAdjust();

            MarginLayoutParams params=(MarginLayoutParams )errorContainer.getLayoutParams();
            params.leftMargin=(int) xOffset;
            //here 100 means 100px,not 80% of the width of the parent view
            //you may need a calculation to convert the percentage to pixels.
            errorContainer.setLayoutParams(params);
            // setup help animation
            mDocAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    errorContainer.setVisibility(View.VISIBLE);
                    //when the animation starts, if talkback is enabled, make the required sound
                    EventBus.getDefault().post(new TextToSpeechEvent(speechResId));
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (drawDocCenter) {
                        mTargetImage.setVisible(true, true);
                    }
                    if (errorContainer != null) {
                        errorContainer.setVisibility(View.INVISIBLE);
                    }
                    mHandler.postDelayed(mBalloonCheckRunner, SMART_HINT_UPDATE_PERIOD);
                }

                @Override
                public void onAnimationRepeat(Animation arg0) {
                }
            });
            errorContainer.setAnimation(mDocAnimation);
            errorContainer.startAnimation(mDocAnimation);


            //refresh the view
//        postInvalidate();
            mBubblesDelayInProgress = true;
        }

        private void clearBubbleAnimation() {
            Log.d("clearBubbleAnimation", "clearBubbleAnimation - start");
            try {
                removeBubbleAnimation();

                //clear the animation listeners as well
                if(mDocAnimation != null) {
                    mDocAnimation.setAnimationListener(null);
                }
                CameraOverlay.this.postInvalidate();
            } catch (Exception e) {
                //log
            }
        }

        private void removeBubbleAnimation() {
            try {
                //remove any bubbles
                if(errorContainer != null) {
                    errorContainer.clearAnimation();
                    errorContainer.setVisibility(View.INVISIBLE);
                }
            } catch (Exception e) {
                //log
            }
        }
    }

    private class CornerConfusionHintBubble extends HintBubble {

        CornerConfusionHintBubble(MiSnapAnalyzerResult.FrameChecks checkThatNeedsToFail, int balloonDrawableResId, int speechResId) {
            super(checkThatNeedsToFail, balloonDrawableResId, speechResId);
            this.onlyCheckIfFourCornersNotFound = true;
        }
    }

    void showManualCapturePressedPleaseWait(boolean show) {
        if (show) {
            mManualCapturePleaseWaitDialog.show();
        } else {
            mManualCapturePleaseWaitDialog.dismiss();
        }
    }

    public void onRotate(){
        if (mCurrentHintBubble != null) {
            mCurrentHintBubble.clearBubbleAnimation();
        }
        mBubblesDelayInProgress = false;
        mGhostAnimationRunning = false;
        if(mTargetImage != null) {
            drawDocCenter = false;
            mTargetImage.setVisible(false, false);
        }

        updateDisplayDimensions();
    }

    private void updateDisplayDimensions() {
        mDisplayWidth = getWidth();
        mDisplayHeight = getHeight();
    }

    public void addBlackBarsIfNecessary(ScaledPreviewSizeStickyEvent event) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)getLayoutParams();
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.width = event.getWidth();
        layoutParams.height = event.getHeight();

        setLayoutParams(layoutParams);
        requestLayout();

        mDisplayWidth = event.getWidth();
        mDisplayHeight = event.getHeight();
    }

    private int pixelsToDP(Context context, int pixels) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixels, context.getResources().getDisplayMetrics());
    }

    private boolean shouldRotateGhostImage() {
        return ((mCameraParamMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_PORTRAIT
                || mCameraParamMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_FREE_DOCUMENT_ALIGNED_WITH_DEVICE)
                && Utils.getDeviceBasicOrientation(mAppContext) == Configuration.ORIENTATION_PORTRAIT);
    }
}
