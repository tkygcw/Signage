package com.jby.signage.object;

public class DisplayObject {
    private String path, timer, priority, galleryID, status, displayType, refreshTime;

    public DisplayObject(String path, String timer, String priority, String galleryID, String displayType, String refreshTime, String status) {
        this.path = path;
        this.timer = timer;
        this.priority = priority;
        this.galleryID = galleryID;
        this.displayType = displayType;
        this.refreshTime = refreshTime;
        this.status = status;
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

    public String getRefreshTime() {
        return refreshTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
