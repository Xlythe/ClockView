package com.xlythe.watchface.clock.utils;

import androidx.lifecycle.Observer;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.flow.FlowCollector;
import kotlinx.coroutines.flow.StateFlow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class KotlinUtilsTest {

    @Test
    public void testContinuation() {
        KotlinUtils.Continuation<Object> continuation = KotlinUtils.continuation();
        assertNotNull(continuation);
        assertNotNull(continuation.getContext());
        continuation.resumeWith("test");
    }

    @Test
    public void testCoroutineContext() {
        CoroutineContext context = KotlinUtils.context();
        assertNotNull(context);
        assertNull(context.get(mock(CoroutineContext.Key.class)));
        assertNull(context.fold("initial", (acc, element) -> acc));
        assertEquals(context, context.plus(mock(CoroutineContext.class)));
        assertEquals(context, context.minusKey(mock(CoroutineContext.Key.class)));
    }

    @Test
    public void testAddAndRemoveObserver() throws Exception {
        StateFlow<String> mockStateFlow = mock(StateFlow.class);
        when(mockStateFlow.getValue()).thenReturn("initial_value");

        FlowCollector[] collectorHolder = new FlowCollector[1];

        doAnswer(invocation -> {
            collectorHolder[0] = invocation.getArgument(0);
            return null;
        }).when(mockStateFlow).collect(any(), null);

        Observer<String> mockObserver = mock(Observer.class);

        KotlinUtils.addObserver(mockStateFlow, mockObserver);
        verify(mockObserver).onChanged("initial_value");

        FlowCollector<String> collector = collectorHolder[0];
        assertNotNull(collector);

        // Test collector emit
        collector.emit("new_value", mock(kotlin.coroutines.Continuation.class));
        verify(mockObserver).onChanged("new_value");

        // Test removeObserver works
        KotlinUtils.removeObserver(mockObserver);

        // After removing observer, emit should not call onChanged
        collector.emit("post_remove_value", mock(kotlin.coroutines.Continuation.class));
        verify(mockObserver, never()).onChanged("post_remove_value");
    }
}
