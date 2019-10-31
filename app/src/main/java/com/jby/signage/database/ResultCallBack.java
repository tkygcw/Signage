package com.jby.signage.database;

public interface ResultCallBack {
    interface OnCreate {
        void createResult(String status);
    }

    interface OnCount {
        void countResult(int count);
    }

    interface OnRead {
        void readResult(String result);
    }

    interface OnUpdate {
        void updateResult(String status);
    }

    interface OnDelete {
        void deleteResult(String status);
    }
}
