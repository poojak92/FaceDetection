package com.miteksystems.misnap.misnapworkflow.ui.screen;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.miteksystems.misnap.events.TextToSpeechEvent;
import com.miteksystems.misnap.misnapworkflow.R;
import com.miteksystems.misnap.misnapworkflow.storage.MiSnapPreferencesManager;
import com.miteksystems.misnap.params.DocType;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;


public class FTManualTutorialFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    private static final String KEY_DOC_CHECKER = "KEY_DOC_CHECKER";
    private static final String KEY_ORIENTATION = "KEY_ORIENTATION";
    private static final int TTS_DELAY_MS = 2000;
    private OnFragmentInteractionListener mListener;
    private DocType mDocType;
    private int mRequestedOrientation;
    private boolean mButtonPressed;
    private ImageView mTutorialImage;

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
        // TODO: Update argument type and name
        void onFTManualTutorialDone();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PlaceholderFragment.
     */
    public static FTManualTutorialFragment newInstance(DocType docChecker, int requestedOrientation) {
    	FTManualTutorialFragment fragment = new FTManualTutorialFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_DOC_CHECKER, docChecker);
        args.putInt(KEY_ORIENTATION, requestedOrientation);

        fragment.setArguments(args);
        return fragment;
    }

    public FTManualTutorialFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean dontShowAgain) {
        MiSnapPreferencesManager.setIsFirstTimeUserManual(getContext(), !dontShowAgain, mDocType);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDocType = (DocType) (getArguments().getSerializable(KEY_DOC_CHECKER));
        mRequestedOrientation = getArguments().getInt(KEY_ORIENTATION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment based on doc type
        View rootView;
        if (mDocType.isIdDocument() || mDocType.isBarcode()) {
            rootView = inflater.inflate(R.layout.misnap_manual_first_time_tutorial_ids, container, false);

            TextView message = (TextView) rootView.findViewById(R.id.misnap_manual_ft_message);
            message.setText(Html.fromHtml(getString(R.string.id_manual_ft_message_2)));

            mTutorialImage = (ImageView) rootView.findViewById(R.id.misnap_tutorial_image);
            loadTutorialImage();
        } else {
            rootView = inflater.inflate(R.layout.misnap_manual_first_time_tutorial_non_ids, container, false);
            TextView message1 = (TextView) rootView.findViewById(R.id.misnap_manual_ft_message_1);
            TextView message2 = (TextView) rootView.findViewById(R.id.misnap_manual_ft_message_2);

            message2.setText(Html.fromHtml(getString(R.string.id_manual_ft_message_2)));
            if (mDocType.isCheck()) {
                message1.setText(getString(R.string.id_manual_ft_message_1_check));
            } else {
                message1.setText(getString(R.string.id_manual_ft_message_1_document));
            }
        }
        
        //get the confirmation button handle
        Button bFTConfirmationBtn = (Button)rootView.findViewById(R.id.ft_manual_tut_btn);
        bFTConfirmationBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //pass the control to the activity
                if (mListener != null && !mButtonPressed) {
                    mButtonPressed = true;
                    mListener.onFTManualTutorialDone();
                }
            }
        });

        CheckBox checkBoxDontShowAgain = (CheckBox) rootView.findViewById(R.id.checkbox_dont_show_again);
        if (!(mDocType.isIdDocument() || mDocType.isBarcode())) {
            checkBoxDontShowAgain.setVisibility(View.GONE);
        } else {
            checkBoxDontShowAgain.setOnCheckedChangeListener(this);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        StringBuilder stringBuilder = new StringBuilder();
        if (mDocType.isCheck()){
            stringBuilder.append(getString(R.string.id_manual_ft_message_1_check));
            stringBuilder.append(Html.fromHtml(getString(R.string.id_manual_ft_message_2)));
        } else if (mDocType.isIdDocument()){
            stringBuilder.append(Html.fromHtml(getString(R.string.id_manual_ft_message_2)));
        } else {
            stringBuilder.append(getString(R.string.id_manual_ft_message_1_document));
            stringBuilder.append(Html.fromHtml(getString(R.string.id_manual_ft_message_2)));
        }

        EventBus.getDefault().post(new TextToSpeechEvent(stringBuilder.toString(), TTS_DELAY_MS));
    }

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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDocType.isIdDocument() || mDocType.isBarcode()) {
            loadTutorialImage();
        }
    }

    private void loadTutorialImage() {
        int orientation = Utils.getDeviceBasicOrientation(getContext());
        if (mDocType.isPassport()) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTutorialImage.setImageResource(R.drawable.misnap_help_passport_plain);
            } else {
                if (isVerticalPortrait()) {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_passport_plain_vertical_portrait);
                } else {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_passport_plain_horizontal_portrait);
                }
            }
        } else if (mDocType.isIdCardFront() || mDocType.isLicense()) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTutorialImage.setImageResource(R.drawable.misnap_help_id_plain);
            } else {
                if (isVerticalPortrait()) {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_id_plain_vertical_portrait);
                } else {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_id_plain_horizontal_portrait);
                }
            }
        } else if (mDocType.isIdCardBack()) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTutorialImage.setImageResource(R.drawable.misnap_help_id_back_plain);
            } else {
                if (isVerticalPortrait()) {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_id_back_plain_vertical_portrait);
                } else {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_id_back_plain_horizontal_portrait);
                }
            }
        } else if (mDocType.isBarcode()) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTutorialImage.setImageResource(R.drawable.misnap_help_dl_back_plain);
            } else {
                if (isVerticalPortrait()) {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_dl_back_plain_vertical_portrait);
                } else {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_dl_back_plain_horizontal_portrait);
                }
            }
        }
    }

    private boolean isVerticalPortrait() {
        return (mRequestedOrientation == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_FREE_DOCUMENT_ALIGNED_WITH_DEVICE ||
                mRequestedOrientation == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_PORTRAIT);
    }
}
