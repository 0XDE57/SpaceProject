package com.spaceproject.utility;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.spaceproject.SpaceProject;

public class MyMath {
    
    public static long getSeed(float x, float y) {
        return getSeed((int) x, (int) y);
    }
    
    public static long getSeed(int x, int y) {
        //long is 64 bits. store x in upper bits, y in lower bits
        return (x << 32) + y + SpaceProject.SEED;
    }
    
    
    static final Vector2 tmpVec = new Vector2();
    
    public static Vector2 vector(float direction, float magnitude) {
        float dx = MathUtils.cos(direction) * magnitude;
        float dy = MathUtils.sin(direction) * magnitude;
        return tmpVec.set(dx, dy);
        //return new Vector2(magnitude,0).setAngleRad(direction);
    }
    
    
    public static Vector2 logVec(Vector2 vec, float scale) {
        float length = (float) Math.log(vec.len()) * scale;
        float angle = vec.angle() * MathUtils.degreesToRadians;
        float dX = length * MathUtils.cos(angle);
        float dY = length * MathUtils.sin(angle);
        return new Vector2(dX, dY);
    }
    
    
    public static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }
    
    
    /**
     * Get angle from position 1 to position 2.
     */
    public static float angleTo(int x1, int y1, int x2, int y2) {
        return (float) -(Math.atan2(x2 - x1, y2 - y1)) - 1.57f;
    }
    
    public static float angleTo(Vector2 a, Vector2 b) {
        return angleTo((int) a.x, (int) a.y, (int) b.x, (int) b.y);
    }
    
    /**
     * Round value with specified precision.
     *
     * @param value
     * @param precision number of decimal points
     * @return rounded value
     */
    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
    
    /**
     * Returns the percentage of the range max-min that corresponds to value.
     *
     * @param min
     * @param max
     * @param value
     * @return interpolant value within the range [min, max].
     */
    public static float inverseLerp(float min, float max, float value) {
        return (value - min) / (max - min);
    }
    
    /**
     * Convert bytes to a human readable format.
     * Eg: 26673720 -> 25.44MB
     * Credit: icza, stackoverflow.com/questions/3758606/#24805871
     *
     * @param bytes
     * @return bytes with SI postfix
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.2f%sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
    
    public static float getAABB(Body body) {
        //Rectangle aabb = new Rectangle();// = fixture.;
        
        for (Fixture f: body.getFixtureList()) {
            //f.GetAABB()?
            //combine
            //f.getShape().getRadius()?
            return f.getShape().getRadius();
        }
        
        return -1;
        //return aabb;
    }
    
    public static float getFixtureSize(Rectangle aabb) {
        return Math.max(aabb.width, aabb.height);
    }
    
}
