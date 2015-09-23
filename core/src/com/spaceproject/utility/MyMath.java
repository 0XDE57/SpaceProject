package com.spaceproject.utility;


public class MyMath {

	/**
	 * Get distance from position 1 to position 2.
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return distance
	 */
	public static float distance(float x1, float y1, float x2, float y2) {
		return (float) Math.hypot(x2 - x1, y2 - y1);
	}
	
	/**
	 * Get angle from position 1 to position 2.
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return angle
	 */
	public static float angleTo(int x1, int y1, int x2, int y2) {
		return (float) -(Math.atan2(x2 - x1, y2 - y1)) - 1.57f;
	}
	
	/**
	 * Round value with specified precision.
	 * @param value
	 * @param precision number of decimal points
	 * @return rounded value
	 */
	public static double round(double value, int precision) {
	    int scale = (int) Math.pow(10, precision);
	    return (double) Math.round(value * scale) / scale;
	}
}
