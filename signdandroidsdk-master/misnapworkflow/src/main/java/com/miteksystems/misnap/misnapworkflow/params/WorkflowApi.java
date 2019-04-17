package com.miteksystems.misnap.misnapworkflow.params;

import com.miteksystems.misnap.params.ApiParameterBuilder;
import com.miteksystems.misnap.params.MiSnapApi;

import org.json.JSONObject;

/**
 * Created by awood on 10/13/16.
 */

public class WorkflowApi {

    /**
     * @deprecated Use {@link #MiSnapTrackGlare}
     */
    @Deprecated public static final String MISNAP_WORKFLOW_TRACK_GLARE = "MiSnapTrackGlare";

    /**
     * If enabled, a red outline will be drawn around any glare that is detected on the document.
     * The outline's lifecycle is tied to the glare hint bubble.
     * <p/>
     * <b>Values:</b><br>
     * Range: {@link #TRACK_GLARE_LOWER_BOUND} - {@link #TRACK_GLARE_UPPER_BOUND}
     * Default: {@link #TRACK_GLARE_DEFAULT}
     */
    public static final String MiSnapTrackGlare = "MiSnapTrackGlare";
    static final int TRACK_GLARE_LOWER_BOUND = 0;
    static final int TRACK_GLARE_UPPER_BOUND = 1;
    static final int TRACK_GLARE_DEFAULT = 1;

    /**
     * @deprecated Modify misnapworkflow_strings.xml (id_check_front_text, id_check_back_text, misnap_id_card_back_text)
     * This value represents the text that will be displayed at the top of the image capture screen.
     * <p>
     * If the parameter is not found, a localized version of "Back Image" will be
     * displayed for check backs, or a localized version of "Front Image" will be displayed for
     * any other document type.
     * <p>
     * If the parameter is found and is "" (empty string), no prompt string will be
     * displayed.
     * <p>
     * If the value is anything else and less than 40 characters, that exact value will be displayed.
     * Otherwise, it will be truncated to show first 40 characters only.
     * <p>
     * (Therefore, if the app is designed to use its own localized value for this purpose, the
     * the call to retrieve the localized resource should occur prior to establishing the
     * parameter to be passed to MiSnapFragment.)
     * <p>
     * <b>Value:</b><br>
     * Default: Either "Front Image" or "Back Image", in the current device language, or in English if
     * the language is not supported.<br>
     */
    @Deprecated public static final String MiSnapTextOverlay = "MiSnapTextOverlay";

    /**
     * @deprecated use {@link #MiSnapTextOverlay} instead
     */
    @Deprecated public static final String MiSnapTextCheckBackPrompt = "MiSnapTextCheckBackPrompt";

    /**
     * @deprecated use {@link #MiSnapTextOverlay} instead
     */
    @Deprecated public static final String MiSnapTextCheckFrontPrompt = "MiSnapTextCheckFrontPrompt";

    /**
     * A human readable description of the document type referenced in {@link MiSnapApi#MiSnapDocumentType} .
     */
    @Deprecated public static final String MiSnapShortDescription = "MiSnapShortDescription";
    static String SHORT_DESCRIPTION_DEFAULT = "";


    private static String[] docSpecificParameters = new String[] {
            MiSnapTextOverlay
    };
    private static String[] commonParameters = new String[] {
            MiSnapTrackGlare
    };

    public static JSONObject getDefaultParameters(String docType) {
        ApiParameterBuilder params = new ApiParameterBuilder();
        params.addParam(MiSnapApi.MiSnapDocumentType, docType);

        // Thresholds that are constant across all document types
        for (String param : commonParameters) {
            params.addParam(param, getDefaultIntThreshold(param, docType));
        }

        // Doc-specific thresholds
//        for (String param : docSpecificParameters) {
//            params.addParam(param, getDefaultIntThreshold(param, docType));
//        }

        return params.build();
    }

    public static int getDefaultIntThreshold(String parameter, String docType) {
        switch (parameter) {
            // Parameters that are constant across all document types
            case MiSnapTrackGlare:
                return TRACK_GLARE_DEFAULT;
            default:
                return -1;
        }
    }
}
