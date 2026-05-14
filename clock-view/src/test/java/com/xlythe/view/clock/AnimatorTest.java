package com.xlythe.view.clock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class AnimatorTest {

    private static class DummyAnimator implements Animator {
        private OnInvalidateListener mListener;

        @Override
        public void setOnInvalidateListener(OnInvalidateListener l) {
            mListener = l;
        }

        public void triggerInvalidate() {
            if (mListener != null) {
                mListener.onInvalidate();
            }
        }
    }

    @Test
    public void testOnInvalidateListener() {
        DummyAnimator animator = new DummyAnimator();
        Animator.OnInvalidateListener listener = mock(Animator.OnInvalidateListener.class);

        animator.setOnInvalidateListener(listener);
        animator.triggerInvalidate();

        verify(listener).onInvalidate();
    }
}
