package com.spaceproject.math;

import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;


public class DelaunayCell {

    public Vector2 a, b, c;//vertex that define triangle todo: make this an index, don't create 3 new vectors for each triangle, that's silly!
    public Vector2 midAB, midBC, midCA;//semiperimeter: midpoints between vertex
    public DelaunayCell nAB, nBC, nCA;//neighbors (TODO: reference for now, index later)
    public Vector2 circumCenter = new Vector2();//center of circle that intersects each vertex a,b,c
    public float circumRadius;//radius of circle that intersects each vertex a,b,c
    public Vector2 centroid = new Vector2();
    public Vector2 incircle = new Vector2();
    public Vector2 orthocenter = new Vector2();
    public float inRadius;
    public float area;
    public float quality;

    /*
    private static final Vector2 cacheVec = new Vector2();
    public Vector2 getA(FloatArray points) {
        return cacheVec.set(points.get(p1), points.get(p1 + 1));
    }
    public Vector2 getB(FloatArray points) {
        return cacheVec.set(points.get(p2), points.get(p2 + 1));
    }
    public Vector2 getC(FloatArray points) {
        return cacheVec.set(points.get(p3), points.get(p3 + 1));
    }
    public int p1, p2, p3;
    public DelaunayCell(FloatArray points, int p1, int p2, int p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;

        float ax = points.get(p1), ay = points.get(p1 + 1); // xy: 0, 1
        float bx = points.get(p2), by = points.get(p2 + 1); // xy: 2, 3
        float cx = points.get(p3), cy = points.get(p3 + 1); // xy: 4, 5
        //or
        /*
        float ax = points.get(p1), ay = points.get(p1 + 1), // xy: 0, 1
            bx = points.get(p1 + 2), by = points.get(p1 + 3), // xy: 2, 3
            cx = points.get(p1 + 4), cy = points.get(p1 + 5); // xy: 4, 5
*
        Vector2 a = getA(points);
        Vector2 b = getB(points);
        Vector2 c = getC(points);
    }
    */


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
        circumCenter.set(circle.x, circle.y);
        circumRadius = circle.z;

        //calculate centroid
        GeometryUtils.triangleCentroid(a.x, a.y, b.x, b.y, c.x, c.y, centroid);
        quality = PolygonUtil.triangleQuality(
                centroid.x - a.x, centroid.y - a.y,
                centroid.x - b.x, centroid.y - b.y,
                centroid.x - c.x, centroid.y - c.y,
                circumRadius);
        area = GeometryUtils.triangleArea(a.x, a.y, b.x, b.y, c.x, c.y);

        //calculate inscribed circle
        Vector3 inscribedCircle = PolygonUtil.incircle(a, b, c, area);
        incircle.set(inscribedCircle.x, inscribedCircle.y);
        inRadius = inscribedCircle.z;

        //calculate orthocenter
        Vector2 ortho = PolygonUtil.orthocenter(a, b, c);
        orthocenter.set(ortho);
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
