package com.spaceproject.screens.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.math.DelaunayCell;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;

import java.util.ArrayList;

//voronoi/polygon stuff
//END GOAL: https://www.youtube.com/watch?v=pe4_Dimk7v0
//https://github.com/libgdx/libgdx/wiki/Circles%2C-planes%2C-rays%2C-etc.
//https://en.wikipedia.org/wiki/Circumscribed_circle
//http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect/565282#565282
//http://stackoverflow.com/questions/31021968/correct-use-of-polygon-triangulators-in-libgdx
//https://github.com/mjholtzem/Unity-2D-Destruction

public class TestVoronoiScreen extends MyScreenAdapter {
    BitmapFont text = FontLoader.createFont(FontLoader.fontBitstreamVMBold, 20);
    
    //all points that define a polygon and any point inside the polygon
    FloatArray points;
    
    //triangulation
    DelaunayTriangulator delaunay = new DelaunayTriangulator();
    ShortArray triangles = new ShortArray();
    ArrayList<DelaunayCell> dCells = new ArrayList<DelaunayCell>();
    
    //convex hull
    float[] hull;
    Polygon hullPoly;
    Vector2 centroid = new Vector2();
    
    //toggles
    boolean drawCircumcircle = false,
            drawCircumcenter = true,
            drawPoints = true,
            drawDelaunay = true,
            drawVoronoi = true,
            drawMidpoints = false,
            drawHull = true,
            drawCentroid = false;
    
    int pSize = 6;
    float dragRadius = 20;
    int focusedPoint = -1;//no index
    
    //todo: [x] fix grabbing points, focus point, don't lose it
    //todo: [x] highlight focused point
    //todo: [ ] highlight when near grab-able point
    //todo: [ ] center points
    //todo: [ ] draw grid
    //todo: [ ] discard points if too close? epsilon check remove duplicate points
    //todo: [ ] fix scaling for window resize
    //todo: [ ] fix voronoi cells for edge cases
    //todo: [ ] extract voronoi cells
    //todo: [ ] display points as array both input and output
    //todo: [x] display centroid delaunay
    //todo: [x] display centroid for sub delaunay
    //todo: [x] display centroid voronoi
    //todo: [x] display centroid for sub voronoi
    
    public TestVoronoiScreen() {
        //center cam
        cam.position.x = Gdx.graphics.getWidth() / 2;
        cam.position.y = Gdx.graphics.getHeight() / 2;
        
        generateNewPoints(10);
    }
    
    
    private void generateNewPoints(int numPoints) {
        //reset / clear previous
        dCells.clear();
        triangles.clear();
        hullPoly = null;
        
        int pad = 200;//distance away from edge of screen
        points = new FloatArray();
        for (int i = 0; i < numPoints * 2; i += 2) {
            float x = MathUtils.random(pad, Gdx.graphics.getWidth() - pad);
            float y = MathUtils.random(pad, Gdx.graphics.getHeight() - pad);
            points.add(x);
            points.add(y);
        }
    
    
        float p = 100;
        Rectangle rectangle = new Rectangle( p, p, Gdx.graphics.getWidth()-p*2, Gdx.graphics.getHeight()-p*2);
        //bottom left
        //points.add(rectangle.x);
        //points.add(rectangle.y);
        //top right
        //points.add(rectangle.x + rectangle.getWidth());
        //points.add(rectangle.y + rectangle.getHeight());
        
        //create cells out of points
        calculateDelaunay();
    }
    
    /**
     * Calculate triangulation on points.
     * Then create cells out of each triangle.
     * Calculate the convex hull based on outermost points.
     * Find and set neighbors for each cell.
     */
    private void calculateDelaunay() {
        //polygons must contain at least 3 points.
        if (points.size < 6) return;
        
        //apply delaunay triangulation to points
        triangles = delaunay.computeTriangles(points, false);
        //triangles = new EarClippingTriangulator().computeTriangles(points);
        
        //create cells for each triangle
        dCells.clear();
        for (int i = 0; i < triangles.size; i += 3) {
            //get points
            int p1 = triangles.get(i) * 2;
            int p2 = triangles.get(i + 1) * 2;
            int p3 = triangles.get(i + 2) * 2;
            Vector2 a = new Vector2(points.get(p1), points.get(p1 + 1));
            Vector2 b = new Vector2(points.get(p2), points.get(p2 + 1));
            Vector2 c = new Vector2(points.get(p3), points.get(p3 + 1));
            
            DelaunayCell d = new DelaunayCell(a, b, c);
            dCells.add(d);
        }
        
        //find surrounding cells for each cell
        DelaunayCell.findNeighbors(dCells);
        
        //calculate the convex hull of all the points
        ConvexHull convex = new ConvexHull();
        hull = convex.computePolygon(points, false).toArray();
        //computePolygon -> Returns convex hull in counter-clockwise order. Note: the last point in the returned list is the same as the first one.
        //Gdx.app.log(getClass().getSimpleName(), "isCCW?" + GeometryUtils.isCCW(hull, 0, hull.length));
        //todo: explore isCCW, start
        hullPoly = new Polygon(hull);
        hullPoly.getCentroid(centroid); //-> GeometryUtils.polygonCentroid(hull, 0, hull.length, centroid);
    }
    
    /**
     * Check if a line from a to b intersects with the convext hull.
     * If so, the point of intersection is stored in the intersect vector.
     *
     * @param a         point one
     * @param b         point two
     * @param intersect point of intersection
     * @return true if intersect. Point of intersection stored in intersect
     */
    public boolean collideWithHull(Vector2 a, Vector2 b, Vector2 intersect) {
        //for each line segment between two vertices
        float[] vertices = hullPoly.getTransformedVertices();
        for (int v = 0; v < vertices.length - 2; v += 2) {
            float xA = vertices[v];
            float yA = vertices[v + 1];
            float xB = vertices[v + 2];
            float yB = vertices[v + 3];
            // convex hull line between A and B
            Vector2 edgeA = new Vector2(xA, yA);
            Vector2 edgeB = new Vector2(xB, yB);
            
            if (Intersector.intersectSegments(edgeA, edgeB, a, b, intersect)) {
                //the two lines intersect. point of intersection is set in variable intersect
                return true;
            }
            
        }
        return false;
    }
    
    private void drawCellEdge(DelaunayCell cellA, DelaunayCell cellB) {
        //check circle is within hull
        if (hullPoly.contains(cellA.circumcenter)) {
            if (cellB != null) {
                shape.setColor(Color.ORANGE);
                if (hullPoly.contains(cellB.circumcenter)) {
                    //if both circumcenters are within the convex hull, draw from circumcenter to circumcenter
                    shape.line(cellA.circumcenter, cellB.circumcenter);
                } else {
                    //if circumcenter is outside of convex hull, draw from circumcenter to intersection
                    Vector2 intersect = new Vector2();
                    if (collideWithHull(cellA.circumcenter, cellB.circumcenter, intersect)) {
                        shape.line(cellA.circumcenter, intersect);
                        shape.circle(intersect.x, intersect.y, 8);//show point of intersection
                    }
                }
                
            } else {
                //if no neighbor, draw from midpoint to circumcenter
                //TODO: don't connect to mid points if already connected to voronoi point
                shape.setColor(Color.CYAN);
                shape.line(cellA.circumcenter, cellA.midAB);
                shape.line(cellA.circumcenter, cellA.midBC);
                shape.line(cellA.circumcenter, cellA.midCA);
                shape.circle(cellA.midAB.x, cellA.midAB.y, 8);
                shape.circle(cellA.midBC.x, cellA.midBC.y, 8);
                shape.circle(cellA.midCA.x, cellA.midCA.y, 8);
            }
        } else {
            // check collision with convex hull, only draw within hull
            float[] vertices = hullPoly.getTransformedVertices();
            for (int v = 0; v < vertices.length - 2; v += 2) {
                float xA = vertices[v];
                float yA = vertices[v + 1];
                float xB = vertices[v + 2];
                float yB = vertices[v + 3];
                // convex hull line
                Vector2 edgeA = new Vector2(xA, yA);
                Vector2 edgeB = new Vector2(xB, yB);
                
                //TODO: only draw where voronoi points are not connected and ignore certain midpoints.
                //ignore where the circumcenter is outside of hull? and some other edge cases...
                //if (Intersector.isPointInTriangle(cellA.circumcenter, cellA.a, cellA.b, cellA.c)?
                //if (Intersector.pointLineSide(edgeA, edgeB, cellA.midXX) == 1)?
                //if circumcenter is same side as midpoint, opposite of obtuse angle, dont draw?
                drawIntersectingLines(cellA, cellA.midAB, edgeA, edgeB);
                drawIntersectingLines(cellA, cellA.midBC, edgeA, edgeB);
                drawIntersectingLines(cellA, cellA.midCA, edgeA, edgeB);
            }
        }
    }
    
    private void drawIntersectingLines(DelaunayCell cell, Vector2 mid, Vector2 edgeA, Vector2 edgeB) {
        Vector2 intersect = new Vector2();
        if (Intersector.intersectSegments(edgeA, edgeB, cell.circumcenter, mid, intersect)) {
            shape.setColor(Color.GREEN);
            shape.line(mid, intersect);
            shape.circle(intersect.x, intersect.y, 3);
        }
    }
    
    private void drawStuff() {
        
        shape.begin(ShapeType.Line);
        
        shape.setColor(Color.BLACK);
        if (points.size <= 4) {
            for (int i = 0; i < points.size; i += 2) {
                float x = points.get(i);
                float y = points.get(i + 1);
                shape.circle(x, y, pSize);
            }
        }
        
        if (drawCentroid) {
            shape.setColor(Color.CYAN);
            shape.circle(centroid.x, centroid.y, pSize);
        }
        
        for (DelaunayCell d : dCells) {
            //draw points
            if (drawPoints) {
                shape.setColor(Color.BLACK);
                shape.circle(d.a.x, d.a.y, pSize);
                shape.circle(d.b.x, d.b.y, pSize);
                shape.circle(d.c.x, d.c.y, pSize);
            }
    
            if (drawCentroid) {
                shape.setColor(Color.CYAN);
                shape.circle(d.centroid.x, d.centroid.y, pSize);
            }
            
            //draw delaunay triangles
            if (drawDelaunay) {
                shape.setColor(Color.BLACK);
                shape.triangle(d.a.x, d.a.y, // A
                        d.b.x, d.b.y, // B
                        d.c.x, d.c.y);// C
            }
            
            //draw midpoints
            if (drawMidpoints) {
                shape.setColor(Color.BLUE);
                shape.circle(d.midAB.x, d.midAB.y, 1);//Different radius to help determine edge
                shape.circle(d.midCA.x, d.midCA.y, 2);
                shape.circle(d.midBC.x, d.midBC.y, 3);
            }
            
            //draw voronoi cells
            if (drawVoronoi) {
				/*
				shape.setColor(Color.ORANGE);		
				shape.line(d.circumcenter, d.midAB);
				shape.line(d.circumcenter, d.midBC);
				shape.line(d.circumcenter, d.midCA);
				*/
				/*
				shape.setColor(Color.ORANGE);
				if (d.nAB != null) shape.line(d.circumcenter, d.nAB.circumcenter);
				if (d.nBC != null) shape.line(d.circumcenter, d.nBC.circumcenter);
				if (d.nCA != null) shape.line(d.circumcenter, d.nCA.circumcenter);
				*/
                
                drawCellEdge(d, d.nAB);
                drawCellEdge(d, d.nBC);
                drawCellEdge(d, d.nBC);
                
				/*
				if (hullPoly.contains(d.circumcenter)) {
					/*
					shape.setColor(Color.ORANGE);
					if (d.nAB != null) shape.line(d.circumcenter, d.nAB.circumcenter);
					if (d.nBC != null) shape.line(d.circumcenter, d.nBC.circumcenter);
					if (d.nCA != null) shape.line(d.circumcenter, d.nCA.circumcenter);
					*
					/*
					drawCellEdge(d, d.nAB);
					drawCellEdge(d, d.nBC);
					drawCellEdge(d, d.nBC);*

				} else {
					//check collision with convex hull, only draw within hull
					float[] vertices = hullPoly.getTransformedVertices();
					for (int velocity = 0; velocity < vertices.length - 2; velocity += 2) {
						float x1 = vertices[velocity];
						float y1 = vertices[velocity + 1];
						float x2 = vertices[velocity + 2];
						float y2 = vertices[velocity + 3];
						// convex hull line
						Vector2 edgeA = new Vector2(x1, y1);
						Vector2 edgeB = new Vector2(x2, y2);

						drawIntersectingLines(d, d.midAB, edgeA, edgeB);
						drawIntersectingLines(d, d.midBC, edgeA, edgeB);
						drawIntersectingLines(d, d.midCA, edgeA, edgeB);

					}
				}*/
            }
            
            //draw circumcircle
            if (!hullPoly.contains(d.circumcenter)) {
                shape.setColor(Color.MAGENTA);
            } else {
                shape.setColor(Color.GREEN);
            }
            if (drawCircumcircle) shape.circle(d.circumcenter.x, d.circumcenter.y, d.circumradius);
            if (drawCircumcenter) shape.circle(d.circumcenter.x, d.circumcenter.y, pSize);
        }
        
        if (focusedPoint >= 0) {
            float focusX = points.get(focusedPoint);
            float focusY = points.get(focusedPoint + 1);
            shape.setColor(Color.GREEN);
            shape.circle(focusX, focusY, dragRadius);
        }
        
        if (drawHull && hullPoly != null) {
            shape.setColor(Color.RED);
            shape.polyline(hullPoly.getVertices());
        }
        
        shape.end();
    }
    
    @Override
    public void render(float delta) {
        //clear screen
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        //render voronoi stuff
        drawStuff();
        
        //toggles, add/move points, reset
        updateControls();
        
        drawMenu();
    }
    
    private void drawMenu() {
        batch.begin();
        int y = Gdx.graphics.getHeight() - 10;
        float h = text.getLineHeight();
        text.setColor(drawCircumcenter ? Color.GREEN : Color.BLACK);
        text.draw(batch, "1: Circumcenter", 10, y);
        
        text.setColor(drawCircumcircle ? Color.GREEN : Color.BLACK);
        text.draw(batch, "2: Circumcircle", 10, y - h * 1);
        
        text.setColor(drawPoints ? Color.GREEN : Color.BLACK);
        text.draw(batch, "3: Points", 10, y - h * 2);
        
        text.setColor(drawMidpoints ? Color.GREEN : Color.BLACK);
        text.draw(batch, "4: Mid points", 10, y - h * 3);
        
        text.setColor(drawVoronoi ? Color.GREEN : Color.BLACK);
        text.draw(batch, "5: Voronoi", 10, y - h * 4);
        
        text.setColor(drawDelaunay ? Color.GREEN : Color.BLACK);
        text.draw(batch, "6: Delaunay", 10, y - h * 5);
        
        text.setColor(drawHull ? Color.GREEN : Color.BLACK);
        text.draw(batch, "7: Hull", 10, y - h * 6);
        
        text.setColor(drawCentroid ? Color.GREEN : Color.BLACK);
        text.draw(batch, "8: Delaunay Centroid", 10, y - h * 7);
        batch.end();
    }
    
    private void updateControls() {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            MyScreenAdapter.game.setScreen(new TitleScreen(MyScreenAdapter.game));
        }
        
        //reset. new test points
        if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            generateNewPoints(3);
        }
        
        //create new point
        if (Gdx.input.justTouched() && Gdx.input.isButtonPressed(Buttons.RIGHT)) {
            points.add(Gdx.input.getX());
            points.add(Gdx.graphics.getHeight() - Gdx.input.getY());
            calculateDelaunay();
        }
        
        //drag points around
        if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY();
            
            if (focusedPoint >= 0) {
                points.set(focusedPoint, x);
                points.set(focusedPoint + 1, y);
            }
            
            boolean mod = false;
            for (int i = 0; i < points.size && !mod; i += 2) {
                float px = points.get(i);
                float py = points.get(i + 1);
                if (Vector2.dst(x, y, px, py) < dragRadius) {
                    focusedPoint = i;
                    mod = true;
                }
            }
            
            if (mod) {
                calculateDelaunay();
            }
        } else {
            focusedPoint = -1;
        }
        
        //toggle drawings
        if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) {
            drawCircumcenter = !drawCircumcenter;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) {
            drawCircumcircle = !drawCircumcircle;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_3)) {
            drawPoints = !drawPoints;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_4)) {
            drawMidpoints = !drawMidpoints;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_5)) {
            drawVoronoi = !drawVoronoi;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_6)) {
            drawDelaunay = !drawDelaunay;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_7)) {
            drawHull = !drawHull;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_8)) {
            drawCentroid = !drawCentroid;
        }
    }
    
}
