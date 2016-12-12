package com.xlythe.view.clock;

public interface Animator {
    interface OnInvalidateListener {
        void onInvalidate();
    }

    void setOnInvalidateListener(OnInvalidateListener l);
}
