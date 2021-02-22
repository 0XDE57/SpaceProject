package com.spaceproject.math;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;


public class DelaunayCell {
    
    public Vector2 a, b, c;//vertex that define triangle
    public Vector2 midAB, midBC, midCA;//midpoints between vertex
    public DelaunayCell nAB, nBC, nCA;//neighbors (TODO: reference for now, index later)
    public Vector2 circumcenter;//center of circle that intersects each vertex a,b,c
    public float circumradius;//radius of circle that intersects each vertex a,b,c
    
    public DelaunayCell(Vector2 a, Vector2 b, Vector2 c) {
        //set triangle points
        this.a = a;
        this.b = b;
        this.c = c;
        
        //calculate midpoints
        midAB = a.cpy().add(b).scl(0.5f);
        midBC = b.cpy().add(c).scl(0.5f);
        midCA = c.cpy().add(a).scl(0.5f);
        
        //calculate circumscribed circle
        Vector3 circle = PolygonUtil.circumcircle(a, b, c);
        circumcenter = new Vector2(circle.x, circle.y);
        circumradius = circle.z;
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
                if (cellA.circumcenter.epsilonEquals(cellB.circumcenter, 0.01f)) {
                    continue;
                }
                //check and set neighbors
                isNeighbor(cellA, cellB);
            }
        }
    }
    
}
