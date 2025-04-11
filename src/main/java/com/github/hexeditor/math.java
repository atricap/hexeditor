package com.github.hexeditor;

class math {

    public static double nextUp(double a) {
        if (Double.isNaN(a) || a == Double.POSITIVE_INFINITY) {
            return a;
        }
        a += 0.0D;
        return Double.longBitsToDouble(
            Double.doubleToRawLongBits(a) + (a >= 0.0D ? 1L : -1L));
    }

    public static float nextUp(float a) {
        if (Float.isNaN(a) || a == Float.POSITIVE_INFINITY) {
            return a;
        }
        a += 0.0F;
        return Float.intBitsToFloat(
            Float.floatToRawIntBits(a) + (a >= 0.0F ? 1 : -1));
    }

    public static double nextDown(double a) {
        if (Double.isNaN(a) || a == Double.NEGATIVE_INFINITY) {
            return a;
        }
        if (a == 0.0D) {
            return -4.9E-324D;
        }
        return Double.longBitsToDouble(
            Double.doubleToRawLongBits(a) + (a > 0.0D ? -1L : 1L));
    }

    public static float nextDown(float a) {
        if (Float.isNaN(a) || a == Float.NEGATIVE_INFINITY) {
            return a;
        }
        if (a == 0.0F) {
            return -1.4E-45F;
        }
        return Float.intBitsToFloat(
            Float.floatToRawIntBits(a) + (a > 0.0F ? -1 : 1));
    }
}
