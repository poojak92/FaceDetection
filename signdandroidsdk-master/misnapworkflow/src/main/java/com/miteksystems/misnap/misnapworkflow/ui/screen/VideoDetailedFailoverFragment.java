package com.miteksystems.misnap.misnapworkflow.ui.screen;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.miteksystems.misnap.analyzer.MiSnapAnalyzerResult;
import com.miteksystems.misnap.events.TextToSpeechEvent;
import com.miteksystems.misnap.misnapworkflow.R;
import com.miteksystems.misnap.misnapworkflow.storage.SessionDiagnostics;
import com.miteksystems.misnap.params.DocType;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Created by awood on 10/17/16.
 */

public class VideoDetailedFailoverFragment extends Fragment {
    private static final String KEY_DOC_TYPE = "KEY_DOC_TYPE";
    private static final String FAILURE_REASONS = "FAILURE_REASONS";
    private static final int TAG_NOT_FOUND = -1;
    private static final boolean DISPLAY_FAILURE_PERCENTAGES = false;
    private VideoDetailedFailoverFragment.OnFragmentInteractionListener mListener;
    private DocType mDocType;
    private boolean mButtonPressed;
    private ArrayList<String> mFailureReasons;
    private String mTextToSpeak;

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
        void onManualCaptureAfterDetailedFailover();
        void onAbortAfterDetailedFailover();
        void onRetryAfterDetailedFailover();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment VideoDetailedFailoverFragment.
     */
    public static VideoDetailedFailoverFragment newInstance(DocType docType, ArrayList<String> failureReasons) {
        VideoDetailedFailoverFragment fragment = new VideoDetailedFailoverFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_DOC_TYPE, docType);
        args.putStringArrayList(FAILURE_REASONS, failureReasons);

        fragment.setArguments(args);
        return fragment;
    }

    public VideoDetailedFailoverFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDocType = (DocType) getArguments().getSerializable(KEY_DOC_TYPE);
        mFailureReasons = getArguments().getStringArrayList(FAILURE_REASONS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.misnap_detailed_failover, container, false);

        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.misnap_detailed_failover_reasons);
        // Add dynamic failover reasons to the view
        for (int i = 0; i < mFailureReasons.size(); i++) {
            TextView reason = (TextView) inflater.inflate(R.layout.misnap_detailed_failover_reason, layout, false);
            String result = getFailure(mFailureReasons.get(i), DISPLAY_FAILURE_PERCENTAGES);
            reason.setText(result);
            reason.setContentDescription(result);
            mTextToSpeak += result;
            layout.addView(reason);
        }

        AppCompatButton bCancelBtn = (AppCompatButton) rootView.findViewById(R.id.misnap_detailed_failover_cancel_btn);
        bCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pass the control to the activity
                if (mListener != null && !mButtonPressed) {
                    mButtonPressed = true;
                    mListener.onAbortAfterDetailedFailover();
                }
            }
        });

        AppCompatButton bContinueBtn = (AppCompatButton) rootView.findViewById(R.id.misnap_detailed_failover_continue_btn);
        bContinueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pass the control to the activity
                if (mListener != null && !mButtonPressed) {
                    mButtonPressed = true;
                    mListener.onManualCaptureAfterDetailedFailover();
                }
            }
        });

        AppCompatButton bRetryBtn = (AppCompatButton) rootView.findViewById(R.id.misnap_detailed_failover_retry_button);
        bRetryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pass the control to the activity
                if (mListener != null && !mButtonPressed) {
                    mButtonPressed = true;
                    mListener.onRetryAfterDetailedFailover();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().post(new TextToSpeechEvent(mTextToSpeak));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (VideoDetailedFailoverFragment.OnFragmentInteractionListener) context;
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

    private String getFailure(String jsonReason, boolean includePercentage){
        String result = "";
        try {
            JSONObject reason = new JSONObject(jsonReason);
            if(includePercentage){
                result += reason.getString(SessionDiagnostics.FAILURE_PERCENT) + "% ";
            }
            result += parseFailureReason(MiSnapAnalyzerResult.FrameChecks.valueOf(reason.getString(SessionDiagnostics.FAILURE_TYPE)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String parseFailureReason(MiSnapAnalyzerResult.FrameChecks failCode){
        String reason;
        Resources res = getActivity().getResources();
        switch (failCode) {
            case FOUR_CORNER_CONFIDENCE:
                reason = res.getString(R.string.id_failure_reason_four_corner_confidence);
                break;
            case HORIZONTAL_MINFILL:
                reason = res.getString(R.string.id_failure_reason_horizontal_min_fill);
                break;
            case MIN_BRIGHTNESS:
                reason = res.getString(R.string.id_failure_reason_min_brightness);
                break;
            case MAX_BRIGHTNESS:
                reason = res.getString(R.string.id_failure_reason_max_brightness);
                break;
            case MAX_SKEW_ANGLE:
                reason = res.getString(R.string.id_failure_reason_skew_angle);
                break;
            case SHARPNESS:
                reason = res.getString(R.string.id_failure_reason_sharpness);
                break;
            case MIN_PADDING:
                reason = res.getString(R.string.id_failure_reason_min_padding);
                break;
            case ROTATION_ANGLE:
                reason = res.getString(R.string.id_failure_reason_rotation_angle);
                break;
            case LOW_CONTRAST:
                reason = res.getString(R.string.id_failure_reason_low_contrast);
                break;
            case BUSY_BACKGROUND:
                reason = res.getString(R.string.id_failure_reason_busy_background);
                break;
            case WRONG_DOCUMENT:
                if (mDocType.isCheckFront()) {
                    reason = res.getString(R.string.id_failure_reason_wrong_doc_check_front);
                } else if (mDocType.isCheckBack()) {
                    reason = res.getString(R.string.id_failure_reason_wrong_doc_check_back);
                } else {
                    reason = res.getString(R.string.id_failure_reason_wrong_doc_generic);
                }
                break;
            case GLARE:
                reason = res.getString(R.string.id_failure_reason_glare);
                break;
            default:
                reason = res.getString(R.string.id_failure_reason_unknown);
                break;
        }
        return reason;
    }
}
