package com.spaceproject.screens.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.math.DelaunayCell;
import com.spaceproject.math.MyMath;
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

    //rendering
    final Matrix4 projectionMatrix = new Matrix4();
    BitmapFont text = FontLoader.createFont(FontLoader.fontBitstreamVMBold, 20);
    BitmapFont dataFont = FontLoader.createFont(FontLoader.fontBitstreamVM, 12);
    GlyphLayout layout = new GlyphLayout();

    //all points that define a polygon and any point inside the polygon
    FloatArray points;
    
    //triangulation
    DelaunayTriangulator delaunay = new DelaunayTriangulator();
    final int maxVerticies = 32767;//todo: this is odd number why? there will always be even number of points.
    ShortArray triangles = new ShortArray();
    ArrayList<DelaunayCell> dCells = new ArrayList<>();
    
    //convex hull
    float[] hull;
    Polygon hullPoly;
    Vector2 centroid = new Vector2();
    final ConvexHull convex = new ConvexHull();
    
    //toggles
    boolean drawCircumcircle = false,
            drawCircumcenter = false,
            drawPoints = true,
            drawDelaunay = true,
            drawVoronoi = false,
            drawMidpoints = false,
            drawMidGraph = false,
            drawCenteroidPointGraph = false,
            drawHull = true,
            drawCentroid = false,
            drawTriangleQuality = false,
            drawTriangleInfo = false;
    
    int pSize = 6;
    float dragRadius = 20;
    int focusedPoint = -1;//no index

    boolean isDrag = false;
    Vector2 dragStart = new Vector2();

    //todo:
    // [x] fix grabbing points, focus point, don't lose it
    // [x] highlight focused point
    // [ ] highlight when near grab-able point, or highlighatble info
    // [x] center points
    // [ ] draw grid
    // [x] discard points if too close? epsilon check remove duplicate points
    // [x] fix scaling for window resize
    // [ ] fix voronoi cells for edge cases
    // [ ] extract voronoi cells
    // [ ] display points as array both input and output
    // [x] display centroid delaunay
    // [x] display centroid for sub delaunay
    // [x] display centroid voronoi
    // [x] display centroid for sub voronoi
    // [ ] basic shape loader, centered with reasonable scale
    // [ ] ability to scale shape, maybe [ALT + L-Click]
    //          triangle, rectangle, at least to octagon (or higher since this isn't limited by box2d currently) 12 or allow user to input num sides
    // [ ] ability to delete point. maybe [SHIFT + R-Click]
    // [ ] editable points list in VisUI textbox
    // [...] shatter button
    //      [x] place new point at each circumcenter
    //      [ ] shatter at Voronoi Center
    //      [ ] shatter at Midpoints
    //      [ ] if we think of any more...
    //      [x] limit duplicates...
    // [x] render to file: Pixmap -> PNG. how to render shaperenderer to file?
    //      [ ] transparency
    //      [ ] shape only crop? currently is full screen capture.
    // [ ] render to file: create animation (https://github.com/tommyettinger/anim8-gdx): base shape, shatter iteration 1-10 (or stop when duplicate points = too small to shatter further)
    // [ ] background color options
    // [ ] pool cells
    // [ ] render triangle by area relative to total hull area
    // [ ]

    public TestVoronoiScreen() {
        generateNewPoints(10);
    }

    private void generateNewPoints(int numPoints) {
        //reset / clear previous
        dCells.clear();
        triangles.clear();
        hullPoly = null;

        //todo: shapes!
        int pad = 200;//distance away from edge of screen
        points = new FloatArray();
        for (int i = 0; i < numPoints * 2; i += 2) {
            float x = MathUtils.random(pad, Gdx.graphics.getWidth() - pad);
            float y = MathUtils.random(pad, Gdx.graphics.getHeight() - pad);
            //todo: reject duplicate points
            points.add(x);
            points.add(y);
        }

        //calculate the convex hull of all the points
        //center around centroid in middle of screen
        hull = convex.computePolygon(points, false).toArray();//<--system.arraycopy()
        GeometryUtils.polygonCentroid(hull, 0, hull.length, centroid);
        for (int i = 0; i < points.size; i+= 2) {
            points.set(i, centroid.x - points.get(i) + Gdx.graphics.getWidth()*0.5f);
            points.set(i + 1, centroid.y - points.get(i + 1) + Gdx.graphics.getHeight()*0.5f);
        }
        
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

        //todo: ArrayIndexOutOfBoundsException: Index -32768 out of bounds for length 32766
        // how did index become negative when we do not allow less than 6? see line above
        //
        // Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index -32768 out of bounds for length 32766
        // at com.badlogic.gdx.math.DelaunayTriangulator.computeTriangles(DelaunayTriangulator.java:140) -> i = p2 - end;
        // at com.badlogic.gdx.math.DelaunayTriangulator.computeTriangles(DelaunayTriangulator.java:43)
        // at com.spaceproject.screens.debug.TestVoronoiScreen.calculateDelaunay(TestVoronoiScreen.java:163)

        //computeTriangles() supports only up to 32767 IllegalArgumentException: count must be <= 32767
        if (points.size > maxVerticies-1) {
            Gdx.app.error(getClass().getSimpleName(), points.size + " too big. calculation ignored!");
            return;
        }

        //apply delaunay triangulation to points
        triangles = delaunay.computeTriangles(points, false);
        
        //create cells for each triangle
        dCells.clear();
        int discard = 1;
        for (int i = 0; i < triangles.size; i += 3) {
            //get points
            int p1 = triangles.get(i) * 2;
            int p2 = triangles.get(i + 1) * 2;
            int p3 = triangles.get(i + 2) * 2;
            float[] hull = new float[] {
                    points.get(p1), points.get(p1 + 1), // xy: 0, 1
                    points.get(p2), points.get(p2 + 1), // xy: 2, 3
                    points.get(p3), points.get(p3 + 1)  // xy: 4, 5
            };

            //discard duplicate points (todo: should do this check before new float above, also use equals... exit early)
            if ((hull[0] == hull[2] && hull[1] == hull[3]) || // p1 == p2 or
                    (hull[0] == hull[4] && hull[1] == hull[5]) || // p1 == p3 or
                    (hull[2] == hull[4] && hull[3] == hull[5])) { // p2 == p3
                Gdx.app.error(getClass().getSimpleName(), "Duplicate point!: " + discard++);
                continue;
            }

            Vector2 a = new Vector2(points.get(p1), points.get(p1 + 1));
            Vector2 b = new Vector2(points.get(p2), points.get(p2 + 1));
            Vector2 c = new Vector2(points.get(p3), points.get(p3 + 1));
            DelaunayCell d = new DelaunayCell(a, b, c);
            dCells.add(d);
        }
        
        //find surrounding cells for each cell
        DelaunayCell.findNeighbors(dCells);
        
        //calculate the convex hull of all the points
        //ConvexHull convex = new ConvexHull();
        hull = convex.computePolygon(points, false).toArray();//<system.arraycopy()
        //computePolygon -> Returns convex hull in counter-clockwise order. Note: the last point in the returned list is the same as the first one.
        //Gdx.app.log(getClass().getSimpleName(), "isCCW?" + GeometryUtils.isCCW(hull, 0, hull.length));
        //todo: explore isCCW, start
        hullPoly = new Polygon(hull); //todo: new -> pool
        hullPoly.getCentroid(centroid); //-> GeometryUtils.polygonCentroid(hull, 0, hull.length, centroid);
    }
    
    /**
     * Check if a line from a to b intersects with the convex hull.
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
        if (hullPoly.contains(cellA.circumCenter)) {
            if (cellB != null) {
                shape.setColor(Color.ORANGE);
                if (hullPoly.contains(cellB.circumCenter)) {
                    //if both circumcenters are within the convex hull, draw from circumcenter to circumcenter
                    shape.line(cellA.circumCenter, cellB.circumCenter);
                } else {
                    //if circumcenter is outside convex hull, draw from circumcenter to intersection
                    Vector2 intersect = new Vector2();
                    if (collideWithHull(cellA.circumCenter, cellB.circumCenter, intersect)) {
                        shape.line(cellA.circumCenter, intersect);
                        shape.circle(intersect.x, intersect.y, 8);//show point of intersection
                    }
                }
                
            } else {
                //if no neighbor, draw from midpoint to circumcenter
                //TODO: don't connect to mid points if already connected to voronoi point
                shape.setColor(Color.PINK);
                shape.line(cellA.circumCenter, cellA.midAB);
                shape.line(cellA.circumCenter, cellA.midBC);
                shape.line(cellA.circumCenter, cellA.midCA);
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
        if (Intersector.intersectSegments(edgeA, edgeB, cell.circumCenter, mid, intersect)) {
            shape.setColor(Color.GREEN);
            shape.line(mid, intersect);
            shape.circle(intersect.x, intersect.y, 3);
        }
    }
    
    private void drawStuff() {
        if (focusedPoint >= 0) {
            shape.begin(ShapeType.Filled);
            float focusX = points.get(focusedPoint);
            float focusY = points.get(focusedPoint + 1);
            shape.setColor(Color.GREEN);
            shape.circle(focusX, focusY, dragRadius);
            shape.end();
        }

        /*
        if (drawTriangleQuality) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shape.begin(ShapeType.Filled);
            for (DelaunayCell cell : dCells) {
                shape.setColor(0.3f, 0.3f, 0.3f, 1 - cell.quality);
                shape.triangle(cell.a.x, cell.a.y, cell.b.x, cell.b.y, cell.c.x, cell.c.y);
            }
            shape.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }*/

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
            shape.setColor(Color.RED);
            shape.circle(centroid.x, centroid.y, pSize);
        }
        
        for (DelaunayCell cell : dCells) {
            //draw points
            if (drawPoints) {
                shape.setColor(Color.BLACK);
                shape.circle(cell.a.x, cell.a.y, pSize);
                shape.circle(cell.b.x, cell.b.y, pSize);
                shape.circle(cell.c.x, cell.c.y, pSize);
            }
    
            if (drawCentroid) {
                shape.setColor(Color.CYAN);
                shape.circle(cell.centroid.x, cell.centroid.y, pSize);
            }
            
            //draw delaunay triangles
            if (drawDelaunay) {
                shape.setColor(Color.BLACK);
                shape.triangle(cell.a.x, cell.a.y, // A
                        cell.b.x, cell.b.y, // B
                        cell.c.x, cell.c.y);// C
            }
            
            //draw midpoints
            if (drawMidpoints) {
                shape.setColor(Color.BLUE);
                shape.circle(cell.midAB.x, cell.midAB.y, 1);//Different radius to help determine edge
                shape.circle(cell.midCA.x, cell.midCA.y, 2);
                shape.circle(cell.midBC.x, cell.midBC.y, 3);
            }

            //NOTE: Delaunay and Voronoi are Convex polygons only.
            //a second dual-graph? (third-graph?)
            //connect midpoints to delaunay centroid
            if (drawMidGraph) {
                shape.setColor(Color.BLUE);
                shape.line(cell.centroid, cell.midAB);
                shape.line(cell.centroid, cell.midBC);
                shape.line(cell.centroid, cell.midCA);
                //todo: OBSERVATIONS of MidPointToCentroid Dual Graph
                // cells may be concave and convex!
            }
            //another dual-graph?
            if (drawCenteroidPointGraph) {
                shape.setColor(Color.PURPLE);
                shape.line(cell.centroid, cell.a);
                shape.line(cell.centroid, cell.b);
                shape.line(cell.centroid, cell.c);
                //todo: OBSERVATIONS of VertexToCentroid Dual Graph
                // all internal cells are 4 vertex (quadrilateral) and convex!
                // all external cells are 3 vertex (triangle)
            }
            //are the other dual graphs? I think im beginning to see some shapes and patterns...

            //draw voronoi cells
            if (drawVoronoi) {
				/*
				shape.setColor(Color.ORANGE);		
				shape.line(cell.circumcenter, cell.midAB);
				shape.line(cell.circumcenter, cell.midBC);
				shape.line(cell.circumcenter, cell.midCA);
				*/
				/*
				shape.setColor(Color.ORANGE);
				if (cell.nAB != null) shape.line(cell.circumcenter, cell.nAB.circumcenter);
				if (cell.nBC != null) shape.line(cell.circumcenter, cell.nBC.circumcenter);
				if (cell.nCA != null) shape.line(cell.circumcenter, cell.nCA.circumcenter);
				*/
                
                drawCellEdge(cell, cell.nAB);
                drawCellEdge(cell, cell.nBC);
                drawCellEdge(cell, cell.nBC);
                
				/*
				if (hullPoly.contains(cell.circumcenter)) {
					/*
					shape.setColor(Color.ORANGE);
					if (cell.nAB != null) shape.line(cell.circumcenter, cell.nAB.circumcenter);
					if (cell.nBC != null) shape.line(cell.circumcenter, cell.nBC.circumcenter);
					if (cell.nCA != null) shape.line(cell.circumcenter, cell.nCA.circumcenter);
					*
					/*
					drawCellEdge(cell, cell.nAB);
					drawCellEdge(cell, cell.nBC);
					drawCellEdge(cell, cell.nBC);*

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

						drawIntersectingLines(cell, cell.midAB, edgeA, edgeB);
						drawIntersectingLines(cell, cell.midBC, edgeA, edgeB);
						drawIntersectingLines(cell, cell.midCA, edgeA, edgeB);

					}
				}*/
            }
            
            //draw circumcircle
            if (!hullPoly.contains(cell.circumCenter)) {
                shape.setColor(Color.MAGENTA);
            } else {
                shape.setColor(Color.GREEN);
            }
            if (drawCircumcircle) {
                //todo: failed: ArrayIndexOutOfBoundsException: Index 20003 out of bounds for length 20000
                //      Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 20003 out of bounds for length 20000
                //      at com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20.color(ImmediateModeRenderer20.java:121)
                //      at com.badlogic.gdx.graphics.glutils.ShapeRenderer.circle(ShapeRenderer.java:906)
                //      at com.badlogic.gdx.graphics.glutils.ShapeRenderer.circle(ShapeRenderer.java:892)
                //      at com.spaceproject.screens.debug.TestVoronoiScreen.drawStuff(TestVoronoiScreen.java:438)
                shape.circle(cell.circumCenter.x, cell.circumCenter.y, cell.circumRadius);
            }
            if (drawCircumcenter) shape.circle(cell.circumCenter.x, cell.circumCenter.y, pSize);
        }

        if (drawHull && hullPoly != null) {
            shape.setColor(Color.RED);
            shape.polyline(hullPoly.getVertices());
        }
        shape.end();

        batch.begin();
        if (drawTriangleInfo) {
            dataFont.setColor(Color.BLACK);
            for (DelaunayCell cell : dCells) {
                layout.setText(dataFont, MyMath.round(cell.area, 2) + "", dataFont.getColor(), 0, Align.center, false);
                dataFont.draw(batch, layout, cell.centroid.x, cell.centroid.y);
                layout.setText(dataFont, cell.quality + "", dataFont.getColor(), 0, Align.center, false);
                dataFont.draw(batch, layout, cell.centroid.x, cell.centroid.y - layout.height * 1.3f);
            }
        }
        /*//rendering on screen
        boolean drawCoords = true;
        if (drawCoords) {
            dataFont.setColor(Color.BLACK);
            if (points.size <= 4) {
                for (int i = 0; i < points.size; i += 2) {
                    float x = points.get(i);
                    float y = points.get(i + 1);
                    layout.setText(dataFont, MyMath.formatPos(x, y, 1), dataFont.getColor(), 0, Align.center, false);
                    dataFont.draw(batch, layout, x, y);
                }
            }
        }*/
        batch.end();

        if (drawTriangleQuality) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shape.begin(ShapeType.Filled);
            for (DelaunayCell cell : dCells) {
                shape.setColor(0.3f, 0.3f, 0.3f, 1 - cell.quality);
                shape.triangle(cell.a.x, cell.a.y, cell.b.x, cell.b.y, cell.c.x, cell.c.y);
            }
            shape.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
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

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        batch.setProjectionMatrix(projectionMatrix);
        //todo: recentering may also flip the entire polygon? interesting bug...
        boolean recenter = true;
        if (recenter) {
            //calculate the convex hull of all the points
            //center around centroid in middle of screen
            hull = convex.computePolygon(points, true).toArray(); //sorted seems to have no impact on
            GeometryUtils.polygonCentroid(hull, 0, hull.length, centroid);
            for (int i = 0; i < points.size; i+= 2) {
                points.set(i, centroid.x - points.get(i) + width*0.5f);
                points.set(i + 1, centroid.y - points.get(i + 1) + height*0.5f);
            }
            calculateDelaunay();
        }
    }

    private void drawMenu() {
        batch.begin();
        int y = Gdx.graphics.getHeight() - 10;
        float h = text.getLineHeight();
        int line = 1;
        int maxPoints = maxVerticies / 2;
        layout.setText(text, "Points: " + (int) (points.size * 0.5f) + "/" + maxPoints + ", D-Cells:" + dCells.size() + ", V-Cells: ?", Color.WHITE, 0, Align.center, false);
        text.draw(batch, layout, Gdx.graphics.getWidth() * 0.5f, y);

        text.setColor(Color.BLACK);
        text.draw(batch, "[L-Click] Drag point", 10, y - h * line++);
        text.draw(batch, "[R-Click] Create new point", 10, y- h  * line++);
        text.draw(batch, "[SHIFT + L-Click] Drag points", 10, y - h * line++);
        text.draw(batch, "[S] Shatter: Circumcenter", 10, y - h * line++);
        text.draw(batch, "[CTRL + D] Save PNG", 10, y - h * line++);

        text.setColor(drawCircumcenter ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[1] CircumCenter", 10, y - h  * line++);
        
        text.setColor(drawCircumcircle ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[2] CircumCircle", 10, y - h * line++);
        
        text.setColor(drawPoints ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[3] Points", 10, y - h * line++);
        
        text.setColor(drawMidpoints ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[4] Mid points", 10, y - h * line++);
        
        text.setColor(drawVoronoi ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[5] Voronoi Graph", 10, y - h * line++);
        
        text.setColor(drawDelaunay ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[6] Delaunay Graph", 10, y - h * line++);
        
        text.setColor(drawHull ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[7] Hull", 10, y - h * line++);
        
        text.setColor(drawCentroid ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[8] Delaunay Centroid", 10, y - h * line++);

        text.setColor(drawTriangleQuality ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[9] Triangle Quality", 10, y - h * line++);

        text.setColor(drawTriangleInfo ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[0] Triangle Info", 10, y - h * line++);

        text.setColor(drawCenteroidPointGraph ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[-] Centroid-Vertex Graph", 10, y - h * line++);

        text.setColor(drawMidGraph ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[=] Centroid-Midpoint Graph", 10, y - h * line++);
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

        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight() - Gdx.input.getY();

        //create new point
        if (Gdx.input.justTouched() && Gdx.input.isButtonPressed(Buttons.RIGHT)) {
            //computeTriangles() supports only up to 32767 IllegalArgumentException: count must be <= 32767
            if (points.size > maxVerticies) {
                Gdx.app.error(getClass().getSimpleName(), points.size + " > 32767. ignored!");
                return;
            }

            boolean duplicate = false;
            for (int i = 0; i < points.size && !duplicate; i += 2) {
                if (MathUtils.isEqual(x, points.get(i), 0.1f) && MathUtils.isEqual(y, points.get(i+1), 0.1f)) {
                    duplicate = true;
                }
            }
            if (!duplicate) {
                points.add(x);
                points.add(y);
                calculateDelaunay();
            }
        }

        if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
            if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) {
                //drag all points around
                if (!isDrag) {
                    isDrag = true;
                    dragStart.set(x, y);
                }
                float offsetX = dragStart.x - x;
                float offsetY = dragStart.y - y;

                for (int i = 0; i < points.size; i += 2) {
                    float px = points.get(i);
                    float py = points.get(i + 1);
                    points.set(i, px - offsetX);
                    points.set(i + 1, py - offsetY);
                }
                calculateDelaunay();
                dragStart.set(x, y);
            } else {
                //drag selected point around
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
            }
        } else {
            focusedPoint = -1;
            isDrag = false;
        }

        //sub-shatter!
        if (Gdx.input.isKeyJustPressed(Keys.S)) {
            boolean added = false;
            for (DelaunayCell cell : dCells) {
                //computeTriangles() supports only up to 32767 IllegalArgumentException: count must be <= 32767
                if (points.size > maxVerticies-2) {
                    Gdx.app.error(getClass().getSimpleName(), points.size + " > 32767. ignored!");
                    return;
                }

                boolean duplicate = false;
                float x1 = cell.centroid.x;
                float y1 = cell.centroid.y;
                for (int i = 0; i < points.size && !duplicate; i += 2) {
                    if (MathUtils.isEqual(x1, points.get(i), 0.1f) && MathUtils.isEqual(y1, points.get(i+1), 0.1f)) {
                        duplicate = true;
                    }
                }
                if (!duplicate) {
                    points.add(x1);
                    points.add(y1);
                    added = true;
                }
            }
            if (added) {
                calculateDelaunay();
            }
        }

        //Control + S -> Save to file
        if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Keys.D)) {
            Pixmap image = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            FileHandle handle = Gdx.files.local("capture_" + Gdx.graphics.getFrameId() + ".PNG");
            Gdx.app.log(getClass().getSimpleName(), "writing to: " + handle.path());
            PixmapIO.writePNG(handle, image, -1, true);
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
        if (Gdx.input.isKeyJustPressed(Keys.NUM_9)) {
            drawTriangleQuality = !drawTriangleQuality;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_0)) {
            drawTriangleInfo = !drawTriangleInfo;
        }
        if (Gdx.input.isKeyJustPressed(Keys.MINUS)) {
            drawCenteroidPointGraph = !drawCenteroidPointGraph;
        }
        if (Gdx.input.isKeyJustPressed(Keys.EQUALS)) {
            drawMidGraph = !drawMidGraph;
        }
    }
    
}
