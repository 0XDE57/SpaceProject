package com.spaceproject.math;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class PolygonUtil {
    
    static final Vector2 tempcenter = new Vector2();
    static final Vector2 tempvec1 = new Vector2();
    static final Vector2 tempvec2 = new Vector2();
    
    public static BoundingBox calculateBoundingBox(Body body) {
        BoundingBox boundingBox = null;
        
        for (Fixture fixture : body.getFixtureList()) {
            if (boundingBox == null) {
                boundingBox = calculateBoundingBox(fixture);
            } else {
                boundingBox.ext(calculateBoundingBox(fixture));
            }
        }
        
        return boundingBox;
    }
    
    /**
     * Calculates a {@link BoundingBox} for the given {@link Fixture}. It will
     * be in physics/world coordinates.
     * credit: gist.github.com/nooone/8363982
     */
    public static BoundingBox calculateBoundingBox(Fixture fixture) {
        BoundingBox boundingBox = new BoundingBox();
        switch (fixture.getShape().getType()) {
            case Polygon: {
                PolygonShape shape = (PolygonShape) fixture.getShape();
                
                Vector2 tmp = new Vector2();
                shape.getVertex(0, tmp);
                tmp = fixture.getBody().getWorldPoint(tmp);
                boundingBox = new BoundingBox(new Vector3(tmp, 0), new Vector3(tmp, 0));
                for (int v = 1; v < shape.getVertexCount(); v++) {
                    shape.getVertex(v, tmp);
                    boundingBox.ext(new Vector3(fixture.getBody().getWorldPoint(tmp), 0));
                }
                
                break;
            }
            case Circle:
                // TODO implement
                //fixture.getShape().getRadius()
                break;
            case Chain:
                // TODO implement
                break;
            case Edge:
                // TODO implement
                break;
        }
        
        return boundingBox;
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
    
    /***
     * https://stackoverflow.com/a/48542735
     */
    public static boolean overlaps(Polygon polygon, Circle circle) {
        float[] vertices = polygon.getTransformedVertices();
        tempcenter.set(circle.x, circle.y);
        float squareRadius = circle.radius * circle.radius;
        for (int i = 0; i < vertices.length; i += 2) {
            if (i == 0) {
                if (Intersector.intersectSegmentCircle(tempvec1.set(vertices[vertices.length - 2], vertices[vertices.length - 1]),
                        tempvec2.set(vertices[i], vertices[i + 1]), tempcenter, squareRadius))
                    return true;
            } else {
                if (Intersector.intersectSegmentCircle(tempvec1.set(vertices[i - 2], vertices[i - 1]), tempvec2.set(vertices[i], vertices[i + 1]), tempcenter, squareRadius))
                    return true;
            }
        }
        return polygon.contains(circle.x, circle.y);
    }
    
    /**
     * Calculate smallest possible circle that intersects
     * each vertex of a triangle defined by vertex a,b,c.
     * https://gist.github.com/mutoo/5617691
     *
     * @param a
     * @param b
     * @param c
     * @return circumcirle of triangle in a Vector3 with position in x,y and radius in z.
     */
    public static Vector3 circumcircle(Vector2 a, Vector2 b, Vector2 c) {
        float EPSILON = 1.0f / 1048576.0f;
        
        float fabsy1y2 = Math.abs(a.y - b.y),
                fabsy2y3 = Math.abs(b.y - c.y),
                xc, yc, m1, m2, mx1, mx2, my1, my2, dx, dy;
        
        /* Check for coincident points */
        if (fabsy1y2 < EPSILON && fabsy2y3 < EPSILON) {
            Gdx.app.log("cirumcircle", "Eek! Coincident points!");
        }
        
        if (fabsy1y2 < EPSILON) {
            m2 = -((c.x - b.x) / (c.y - b.y));
            mx2 = (b.x + c.x) / 2.0f;
            my2 = (b.y + c.y) / 2.0f;
            xc = (b.x + a.x) / 2.0f;
            yc = m2 * (xc - mx2) + my2;
        } else if (fabsy2y3 < EPSILON) {
            m1 = -((b.x - a.x) / (b.y - a.y));
            mx1 = (a.x + b.x) / 2.0f;
            my1 = (a.y + b.y) / 2.0f;
            xc = (c.x + b.x) / 2.0f;
            yc = m1 * (xc - mx1) + my1;
        } else {
            m1 = -((b.x - a.x) / (b.y - a.y));
            m2 = -((c.x - b.x) / (c.y - b.y));
            mx1 = (a.x + b.x) / 2.0f;
            mx2 = (b.x + c.x) / 2.0f;
            my1 = (a.y + b.y) / 2.0f;
            my2 = (b.y + c.y) / 2.0f;
            xc = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
            yc = (fabsy1y2 > fabsy2y3) ? m1 * (xc - mx1) + my1 : m2 * (xc - mx2) + my2;
        }
        
        dx = b.x - xc;
        dy = b.y - yc;
        float radius = (float) Math.sqrt(dx * dx + dy * dy);
        return new Vector3(xc, yc, radius);
    }
    
}
