package com.jby.signage.shareObject;

/**
 * Created by wypan on 2/21/2017.
 */

public class ApiModelObject {

    private String modelName;
    private ApiDataObject apiDataObject;

    public ApiModelObject(String modelName, ApiDataObject apiDataObject) {
        this.modelName = modelName;
        this.apiDataObject = apiDataObject;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public ApiDataObject getApiDataObject() {
        return apiDataObject;
    }

    public void setApiDataObject(ApiDataObject apiDataObject) {
        this.apiDataObject = apiDataObject;
    }
}
