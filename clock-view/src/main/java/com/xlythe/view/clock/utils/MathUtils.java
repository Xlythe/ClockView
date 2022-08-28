package com.xlythe.view.clock.utils;

/**
 * Created by Will on 3/8/2015.
 */
public class MathUtils {
    public static int min(int... ints) {
        if (ints.length == 0)
            throw new IllegalArgumentException("Cannot find the min of 0 elements");
        else if (ints.length == 1) return ints[0];
        else {
            int[] newInts = new int[ints.length - 1];
            newInts[0] = Math.min(ints[0], ints[1]);
            System.arraycopy(ints, 2, newInts, 1, ints.length - 2);
            return min(newInts);
        }
    }

    public static int max(int... ints) {
        if (ints.length == 0)
            throw new IllegalArgumentException("Cannot find the max of 0 elements");
        else if (ints.length == 1) return ints[0];
        else {
            int[] newInts = new int[ints.length - 1];
            newInts[0] = Math.max(ints[0], ints[1]);
            System.arraycopy(ints, 2, newInts, 1, ints.length - 2);
            return max(newInts);
        }
    }
}
