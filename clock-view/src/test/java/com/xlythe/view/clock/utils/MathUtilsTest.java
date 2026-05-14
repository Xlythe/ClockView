package com.xlythe.view.clock.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathUtilsTest {

    @Test
    public void testMinSingleElement() {
        assertEquals(5, MathUtils.min(5));
        assertEquals(-10, MathUtils.min(-10));
        assertEquals(0, MathUtils.min(0));
    }

    @Test
    public void testMinMultipleElements() {
        assertEquals(1, MathUtils.min(1, 2, 3, 4, 5));
        assertEquals(1, MathUtils.min(5, 4, 3, 2, 1));
        assertEquals(1, MathUtils.min(3, 1, 4, 5, 2));
        assertEquals(-5, MathUtils.min(-1, -2, -3, -4, -5));
        assertEquals(-5, MathUtils.min(0, -5, 10, 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinEmptyElements() {
        MathUtils.min();
    }

    @Test
    public void testMaxSingleElement() {
        assertEquals(5, MathUtils.max(5));
        assertEquals(-10, MathUtils.max(-10));
        assertEquals(0, MathUtils.max(0));
    }

    @Test
    public void testMaxMultipleElements() {
        assertEquals(5, MathUtils.max(1, 2, 3, 4, 5));
        assertEquals(5, MathUtils.max(5, 4, 3, 2, 1));
        assertEquals(5, MathUtils.max(3, 1, 5, 4, 2));
        assertEquals(-1, MathUtils.max(-1, -2, -3, -4, -5));
        assertEquals(10, MathUtils.max(0, -5, 10, 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxEmptyElements() {
        MathUtils.max();
    }
}
