package com.jby.signage.shareObject;

/**
 * Created by wypan on 2/20/2017.
 */

public class ApiDataObject {

    private String dataKey;
    private String dataContent;

    public ApiDataObject(String dataKey, String dataContent) {
        this.dataKey = dataKey;
        this.dataContent = dataContent;
    }

    public String getDataKey() {
        return dataKey;
    }

    public String getDataContent() {
        return dataContent;
    }

}
