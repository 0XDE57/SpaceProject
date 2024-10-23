package com.spaceproject.math;

import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;


public class DelaunayCell {
    
    public Vector2 a, b, c;//vertex that define triangle
    public Vector2 midAB, midBC, midCA;//semiperimeter: midpoints between vertex
    public DelaunayCell nAB, nBC, nCA;//neighbors (TODO: reference for now, index later)
    public Vector2 circumCenter;//center of circle that intersects each vertex a,b,c
    public float circumRadius;//radius of circle that intersects each vertex a,b,c
    public Vector2 centroid = new Vector2();
    public Vector2 incircle = new Vector2();
    public float inRadius;
    public float area;
    public float quality;
    
    public DelaunayCell(Vector2 a, Vector2 b, Vector2 c) {
        //set triangle points
        this.a = a;
        this.b = b;
        this.c = c;
        
        //calculate semiperimeter / midpoints
        midAB = a.cpy().add(b).scl(0.5f);
        midBC = b.cpy().add(c).scl(0.5f);
        midCA = c.cpy().add(a).scl(0.5f);
        
        //calculate circumscribed circle
        Vector3 circle = PolygonUtil.circumcircle(a, b, c);
        circumCenter = new Vector2(circle.x, circle.y);
        circumRadius = circle.z;

        //calculate centroid
        /*
        float[] poly = new float[]{
                a.x, a.y,
                b.x, b.y,
                c.x, c.y
        };
        GeometryUtils.polygonCentroid(poly, 0 , poly.length, centroid);
        */
        GeometryUtils.triangleCentroid(a.x, a.y, b.x, b.y, c.x, c.y, centroid);
        quality = PolygonUtil.triangleQuality(
                centroid.x - a.x, centroid.y - a.y,
                centroid.x - b.x, centroid.y - b.y,
                centroid.x - c.x, centroid.y - c.y,
                circumRadius);
        area = GeometryUtils.triangleArea(a.x, a.y, b.x, b.y, c.x, c.y);

        //calculate incircle and radius
        inCircle();
    }

    /** Incircle: compute "inner circle" of a triangle. incenter and inradius
     * NOTE: expects area to be calculated before calling
     * todo: make static polygonutil version
     */
    private void inCircle() {
        //delta a, b and c are the side lengths OPPOSITE vertex A, B and C
        float da = b.dst(c);
        float db = c.dst(a);
        float dc = a.dst(b);
        float dt = da + db + dc; //delta total
        float x = (da * a.x + db * b.x + dc * c.x) / dt;
        float y = (da * a.y + db * b.y + dc * c.y) / dt;
        incircle.set(x, y);

        //calculate inRadius
        float p = dt / 2;// p = semi-perimeter of the circle
        float r = area / p;
        inRadius = r;
    }
    
    /**
     * Check if points are close enough together.
     *
     * @param midpoint
     * @param other    cell containing midpoints
     * @return true if midpoints overlap
     */
    private static boolean sharesMidpoint(Vector2 midpoint, DelaunayCell other) {
        float epsilon = 0.01f;//error margin
        return midpoint.epsilonEquals(other.midAB, epsilon) ||
                midpoint.epsilonEquals(other.midBC, epsilon) ||
                midpoint.epsilonEquals(other.midCA, epsilon);
    }
    
    /**
     * Check if two cells are neighbors, and sets cell reference to neighbors.
     *
     * @param cellA
     * @param cellB
     * @return true if cells are touching
     */
    public static boolean isNeighbor(DelaunayCell cellA, DelaunayCell cellB) {
        if (sharesMidpoint(cellA.midAB, cellB)) {
            cellA.nAB = cellB;
            return true;
        }
        
        if (sharesMidpoint(cellA.midBC, cellB)) {
            cellA.nBC = cellB;
            return true;
        }
        
        if (sharesMidpoint(cellA.midCA, cellB)) {
            cellA.nCA = cellB;
            return true;
        }
        
        return false;
    }
    
    /**
     * Check and set neighbor references for all cells in list of cells.
     *
     * @param dCells
     */
    public static void findNeighbors(ArrayList<DelaunayCell> dCells) {
        //check each cell against each other
        for (DelaunayCell cellA : dCells) {
            for (DelaunayCell cellB : dCells) {
                //skip check on self
                if (cellA.circumCenter.epsilonEquals(cellB.circumCenter, 0.01f)) {
                    continue;
                }
                //check and set neighbors
                isNeighbor(cellA, cellB);
            }
        }
    }
    
}
