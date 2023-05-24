package com.xlythe.watchface.clock.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.xlythe.view.clock.ClockView;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import kotlin.Result;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.flow.StateFlow;

public class KotlinUtils {
  private static final Set<Observer<?>> sObservers = new HashSet<>();

  private static final CoroutineContext sCoroutineContext = new CoroutineContext() {
    @Nullable
    @Override
    public <E extends Element> E get(@NonNull Key<E> key) {
      return null;
    }

    @Override
    public <R> R fold(R initial, @NonNull Function2<? super R, ? super Element, ? extends R> operation) {
      return null;
    }

    @NonNull
    @Override
    public CoroutineContext plus(@NonNull CoroutineContext coroutineContext) {
      return this;
    }

    @NonNull
    @Override
    public CoroutineContext minusKey(@NonNull Key<?> key) {
      return this;
    }
  };

  private static final Continuation<Object> sContinuation = new Continuation<Object>() {
    @Override
    public void onUpdate(Object o) {
      // ignored
    }
  };

  private KotlinUtils() {}

  public static Continuation<Object> continuation() {
    return sContinuation;
  }

  public static CoroutineContext context() {
    return sCoroutineContext;
  }

  /**
   * Listens to changes in the StateFlow value.
   * Observers must be unregistered with {@link #removeObserver} or they may leak memory.
   */
  public static <T> void addObserver(StateFlow<T> stateFlow, Observer<T> observer) {
    // Hold on to the observer w/ a strong reference.
    sObservers.add(observer);

    // Pass a weak reference into the StateFlow. This way, if the observer references an Activity
    // context, we can safely discard it when unobserved.
    WeakReference<Observer<T>> weakObserver = new WeakReference<>(observer);
    stateFlow.collect((value, continuation) -> {
      Observer<T> possibleObserver = weakObserver.get();
      if (possibleObserver == null) {
        Log.w(ClockView.TAG, "Failed to read value. Observer expired.");
        return null;
      }

      if (sObservers.contains(possibleObserver)) {
        possibleObserver.onChanged(value);
      }
      return null;
    }, new Continuation<T>() {
      @Override
      public void onUpdate(T value) {
        if (value instanceof Result.Failure) {
          Log.e(ClockView.TAG, "Failed to read value", ((Result.Failure) value).exception);
          return;
        }

        Observer<T> possibleObserver = weakObserver.get();
        if (possibleObserver == null) {
          Log.w(ClockView.TAG, "Failed to read value. Observer expired.");
          return;
        }

        if (sObservers.contains(possibleObserver)) {
          possibleObserver.onChanged(value);
        }
      }
    });

    T currentValue = stateFlow.getValue();
    Log.v(ClockView.TAG, "Current value " + currentValue);
    if (currentValue != null) {
      observer.onChanged(currentValue);
    }
  }

  public static <T> void removeObserver(Observer<T> observer) {
    sObservers.remove(observer);
  }

  public static abstract class Continuation<T> implements kotlin.coroutines.Continuation<Object> {
    public abstract void onUpdate(T t);

    @Override
    public void resumeWith(@NonNull Object o) {
      onUpdate((T) o);
    }

    @NonNull
    @Override
    public CoroutineContext getContext() {
      return sCoroutineContext;
    }
  }
}
