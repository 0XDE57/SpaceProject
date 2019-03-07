package com.spaceproject.utility;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class PolygonUtil {
    
    static final Vector2 center = new Vector2();
    static final Vector2 vec1 = new Vector2();
    static final Vector2 vec2 = new Vector2();
    
    /***
     * https://stackoverflow.com/a/48542735
     */
    public static boolean overlaps(Polygon polygon, Circle circle) {
        float[] vertices = polygon.getTransformedVertices();
        center.set(circle.x, circle.y);
        float squareRadius = circle.radius * circle.radius;
        for (int i = 0; i < vertices.length; i += 2) {
            if (i == 0) {
                if (Intersector.intersectSegmentCircle(vec1.set(vertices[vertices.length - 2], vertices[vertices.length - 1]),
                        vec2.set(vertices[i], vertices[i + 1]), center, squareRadius))
                    return true;
            } else {
                if (Intersector.intersectSegmentCircle(vec1.set(vertices[i - 2], vertices[i - 1]), vec2.set(vertices[i], vertices[i + 1]), center, squareRadius))
                    return true;
            }
        }
        return polygon.contains(circle.x, circle.y);
    }
    
    
    public static Rectangle getBoundingRectangle(float[] vertices) {
        
        float minX = vertices[0];
        float minY = vertices[1];
        float maxX = vertices[0];
        float maxY = vertices[1];
        
        final int numFloats = vertices.length;
        for (int i = 2; i < numFloats; i += 2) {
            minX = minX > vertices[i] ? vertices[i] : minX;
            minY = minY > vertices[i + 1] ? vertices[i + 1] : minY;
            maxX = maxX < vertices[i] ? vertices[i] : maxX;
            maxY = maxY < vertices[i + 1] ? vertices[i + 1] : maxY;
        }
        
        Rectangle bounds = new Rectangle();
        bounds.x = minX;
        bounds.y = minY;
        bounds.width = maxX - minX;
        bounds.height = maxY - minY;
        
        return bounds;
    }
}
