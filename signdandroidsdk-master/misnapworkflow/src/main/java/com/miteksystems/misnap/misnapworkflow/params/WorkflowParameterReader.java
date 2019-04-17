package com.miteksystems.misnap.misnapworkflow.params;

import com.miteksystems.misnap.params.ApiParameterBuilder;
import com.miteksystems.misnap.params.BaseParamMgr;
import com.miteksystems.misnap.params.DocType;

import org.json.JSONObject;

/**
 * Created by awood on 10/13/16.
 */

public class WorkflowParameterReader extends BaseParamMgr {
    private String param;
    private int minValue;
    private int maxValue;
    private int defaultIntValue;
    private String defaultStringValue;
    private int maxStringLen;
    private boolean isHex;
    private static final int MAX_LEN_DOC_TYPE_OVERLAY_LABEL = 40;

    public WorkflowParameterReader(JSONObject params) {
        super(params);
    }

    public static JSONObject getDefaultParameters(String docType) {
        ApiParameterBuilder params = new ApiParameterBuilder();

        params.addParam(WorkflowApi.MiSnapTrackGlare, WorkflowApi.TRACK_GLARE_DEFAULT);
        params.addParam(WorkflowApi.MiSnapTextOverlay, getDocSpecificTextOverlay(docType));
        params.addParam(WorkflowApi.MiSnapShortDescription, WorkflowApi.SHORT_DESCRIPTION_DEFAULT);

        return params.build();
    }

    static String getDocSpecificTextOverlay(String docType) {
        String overlay = WorkflowConstants.TEXT_OVERLAY_CHECK_FRONT;
        if (new DocType(docType).isCheckBack()) {
            overlay = WorkflowConstants.TEXT_OVERLAY_CHECK_BACK;
        }
        return overlay;
    }

    public int getGlareTracking() {
        param = WorkflowApi.MiSnapTrackGlare;
        minValue = WorkflowApi.TRACK_GLARE_LOWER_BOUND;
        maxValue = WorkflowApi.TRACK_GLARE_UPPER_BOUND;
        defaultIntValue = WorkflowApi.TRACK_GLARE_DEFAULT;
        return getIntParameterValueInRange(param, minValue, maxValue, defaultIntValue);
    }

    public String getTextPrompt(String defaultText, String defaultCheckFrontText, String defaultCheckBackText) {
        param = WorkflowApi.MiSnapTextOverlay;
        maxStringLen = MAX_LEN_DOC_TYPE_OVERLAY_LABEL;
        if (docType.isCheckBack()) {
            defaultStringValue = defaultCheckBackText;
        } else if (docType.isCheckFront()) {
            defaultStringValue = defaultCheckFrontText;
        } else {
            defaultStringValue = defaultText;
        }

        return getCroppedStringParameter(param, maxStringLen, defaultStringValue);
    }

    public String getShortDescription() {
        param = WorkflowApi.MiSnapShortDescription;
        maxStringLen = MAX_LEN_DOC_TYPE_OVERLAY_LABEL;
        defaultStringValue = "";
        return getCroppedStringParameter(param, maxStringLen, defaultStringValue);
    }
}
