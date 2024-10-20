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
        quality = triangleQuality(
                centroid.x - a.x, centroid.y - a.y,
                centroid.x - b.x, centroid.y - b.y,
                centroid.x - c.x, centroid.y - c.y);
        area = GeometryUtils.triangleArea(a.x, a.y, b.x, b.y, c.x, c.y);
    }

    /** Ratio of circumradius to shortest edge as a measure of triangle quality.
     * copy of GeometryUtils.triangleQuality() modified to use provided circumradius instead of recalculating.
     * NOTE: this function expects triangle cords to be relative to the centroid origin (0, 0)!
     */
    public float triangleQuality (float x1, float y1, float x2, float y2, float x3, float y3) {
        float sqLength1 = x1 * x1 + y1 * y1;
        float sqLength2 = x2 * x2 + y2 * y2;
        float sqLength3 = x3 * x3 + y3 * y3;
        return (float)Math.sqrt(Math.min(sqLength1, Math.min(sqLength2, sqLength3))) / circumRadius;
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
