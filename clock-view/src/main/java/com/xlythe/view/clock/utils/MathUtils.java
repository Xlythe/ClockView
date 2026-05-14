package com.xlythe.view.clock.utils;

/**
 * Created by Will on 3/8/2015.
 */
public class MathUtils {
    public static int min(int... ints) {
        if (ints.length == 0) {
            throw new IllegalArgumentException("Cannot find the min of 0 elements");
        }
        int min = ints[0];
        for (int i = 1; i < ints.length; i++) {
            if (ints[i] < min) {
                min = ints[i];
            }
        }
        return min;
    }

    public static int max(int... ints) {
        if (ints.length == 0) {
            throw new IllegalArgumentException("Cannot find the max of 0 elements");
        }
        int max = ints[0];
        for (int i = 1; i < ints.length; i++) {
            if (ints[i] > max) {
                max = ints[i];
            }
        }
        return max;
    }
}
