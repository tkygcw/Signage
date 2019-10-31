package com.jby.signage.object;

public class DisplayObject {
    private String linkID, path, timer, priority, galleryID, status, displayType, refreshTime, defaultDisplay;

    public DisplayObject(String linkID, String path, String timer, String priority, String galleryID, String displayType, String status, String defaultDisplay) {
        this.linkID = linkID;
        this.path = path;
        this.timer = timer;
        this.priority = priority;
        this.galleryID = galleryID;
        this.displayType = displayType;
        this.status = status;
        this.defaultDisplay = defaultDisplay;
    }

    public String getLinkID() {
        return linkID;
    }

    public String getPath() {
        return path;
    }

    public String getTimer() {
        return timer;
    }

    public String getPriority() {
        return priority;
    }

    public String getGalleryID() {
        return galleryID;
    }

    public String getStatus() {
        return status;
    }

    public String getDisplayType() {
        return displayType;
    }

    public String getDefaultDisplay() {
        return defaultDisplay;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
