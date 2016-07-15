package com.spaceproject.screens;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.utility.MyScreenAdapter;

//voronoi/polygon stuff
//END GOAL: https://www.youtube.com/watch?v=pe4_Dimk7v0
//https://github.com/libgdx/libgdx/wiki/Circles%2C-planes%2C-rays%2C-etc.
//https://en.wikipedia.org/wiki/Circumscribed_circle
//http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect/565282#565282
//http://stackoverflow.com/questions/31021968/correct-use-of-polygon-triangulators-in-libgdx
//https://github.com/mjholtzem/Unity-2D-Destruction

class VoronoiCell {
	
	Polygon poly;
	FloatArray verticies = new FloatArray();
	
	public void addVertex(Vector2 point) {
		verticies.add(point.x);
		verticies.add(point.y);
			
	}

	public void setVerticies() {		
		/*
		FloatArray verts = verticies;
		float[] floatArray = new float[verts.length];
		
		int i = 0;
		for (Float f : verticies) {
		    floatArray[i++] = (f != null ? f : Float.NaN);
		}*/
		
		ConvexHull convex = new ConvexHull();
		FloatArray hull = convex.computePolygon(verticies, false);
		poly.setVertices(hull.toArray());
	}
	
}

class DelaunayCell {
	Vector2 a, b, c;
	Vector2 midAB, midBC, midCA;
	DelaunayCell nAB, nBC, nCA;//neighbors (TODO: reference for now, index later)
	Vector2 circumcenter;
	float circumradius;
	
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
		Vector3 circle = circumcircle2(a, b, c);
		circumcenter = new Vector2(circle.x, circle.y);
		circumradius = circle.z;
	}
	
	private static boolean sharesMidpoint (Vector2 midpoint, DelaunayCell other) {
		float epsilon = 0.01f;
		return midpoint.epsilonEquals(other.midAB, epsilon) ||
			   midpoint.epsilonEquals(other.midBC, epsilon) ||
			   midpoint.epsilonEquals(other.midCA, epsilon);
	}
	
	public static boolean isNeighbor(DelaunayCell cellA, DelaunayCell cellB) {
		if (sharesMidpoint(cellA.midAB, cellB)) {
			cellA.nAB = cellB;
			return true;
		}
		
		if (sharesMidpoint(cellA.midBC, cellB)) {;
			cellA.nBC = cellB;
			return true;
		}
		
		if (sharesMidpoint(cellA.midCA, cellB)) {
			cellA.nCA = cellB;
			return true;
		}
		
		return false;
	}
	
	public static void findNeighbors(ArrayList<DelaunayCell> dCells) {		
		for (DelaunayCell cellA : dCells) {
			for (DelaunayCell cellB : dCells) {
				//skip check on self
				if (cellA.circumcenter.epsilonEquals(cellB.circumcenter, 0.01f)) {
					continue;
				}
			
				isNeighbor(cellA, cellB);				
			}
		}		
	}

	
	
	/**
	 * https://gist.github.com/mutoo/5617691
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	private Vector3 circumcircle2(Vector2 a, Vector2 b, Vector2 c) {

	    float EPSILON = 1.0f / 1048576.0f;
	  
	   float fabsy1y2 = Math.abs(a.y - b.y),
	        fabsy2y3 = Math.abs(b.y - c.y),
	        xc, yc, m1, m2, mx1, mx2, my1, my2, dx, dy;

	    /* Check for coincident points */
	    //if(fabsy1y2 < EPSILON && fabsy2y3 < EPSILON) throw new Error("Eek! Coincident points!");

	    if(fabsy1y2 < EPSILON) {
	        m2  = -((c.x - b.x) / (c.y - b.y));
	        mx2 = (b.x + c.x) / 2.0f;
	        my2 = (b.y + c.y) / 2.0f;
	        xc  = (b.x + a.x) / 2.0f;
	        yc  = m2 * (xc - mx2) + my2;
	    }

	    else if(fabsy2y3 < EPSILON) {
	        m1  = -((b.x - a.x) / (b.y - a.y));
	        mx1 = (a.x + b.x) / 2.0f;
	        my1 = (a.y + b.y) / 2.0f;
	        xc  = (c.x + b.x) / 2.0f;
	        yc  = m1 * (xc - mx1) + my1;
	    }

	    else {
	        m1  = -((b.x - a.x) / (b.y - a.y));
	        m2  = -((c.x - b.x) / (c.y - b.y));
	        mx1 = (a.x + b.x) / 2.0f;
	        mx2 = (b.x + c.x) / 2.0f;
	        my1 = (a.y + b.y) / 2.0f;
	        my2 = (b.y + c.y) / 2.0f;
	        xc  = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
	        yc  = (fabsy1y2 > fabsy2y3) ?
	        m1 * (xc - mx1) + my1 :
	        m2 * (xc - mx2) + my2;
	    }

	    dx = b.x - xc;
	    dy = b.y - yc;
	    float radius = (float) Math.sqrt(dx * dx + dy * dy);
	    return new Vector3(xc, yc, radius);
	    
	}
}

public class TestVoronoiScreen extends MyScreenAdapter {
	//points
	FloatArray points;
	
	//triangulation
	DelaunayTriangulator delaunay = new DelaunayTriangulator();
	ShortArray triangles;
	ArrayList<DelaunayCell> dCells = new ArrayList<DelaunayCell>();
	
	//convex hull
	float[] hull;
	Polygon hullPoly;
	
	//toggles
	boolean drawCircumcircle = false,
		drawCircumcenter = true,
		drawPoints = true,
		drawDelaunay = true,
		drawVoronoi = true,
		drawMidpoints = false,
		drawHull = true;
		
	public TestVoronoiScreen() {
		//center cam
		cam.position.x = Gdx.graphics.getWidth()/2;
		cam.position.y = Gdx.graphics.getHeight()/2;
	
		generateNewPoints(10);
	}


	private void generateNewPoints(int numPoints) {
		points = new FloatArray();
		for (int i = 0; i < numPoints*2; i+=2) {
			float x = MathUtils.random(0, Gdx.graphics.getWidth());
			float y = MathUtils.random(0, Gdx.graphics.getHeight());
			points.add(x);
			points.add(y);
		}
		
		calculateDelaunay();
	}


	private void calculateDelaunay() {
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
		
		//calculate the convex hull of all the points
		ConvexHull convex = new ConvexHull();
		hull = convex.computePolygon(points, false).toArray();
		hullPoly = new Polygon(hull);
		
		//find surrounding cells for each cell
		DelaunayCell.findNeighbors(dCells);
	}
	
	/**
	 * Check if a line from a to b intersects with the convext hull. 
	 * If so, the point of intersection is stored in the intersect vector.
	 * @param a point one
	 * @param b point two
	 * @param intersect point of intersection
	 * @return true if intersect. Point of intersection stored in intersect
	 */
	public boolean collideWithHull(Vector2 a, Vector2 b, Vector2 intersect) {
		float[] verticies = hullPoly.getTransformedVertices();
		for (int v = 0; v < verticies.length - 2; v += 2) {
			float x1 = verticies[v];
			float y1 = verticies[v + 1];
			float x2 = verticies[v + 2];
			float y2 = verticies[v + 3];
			// convex hull edge
			Vector2 pA = new Vector2(x1, y1);
			Vector2 pB = new Vector2(x2, y2);
			
			if (Intersector.intersectSegments(pA, pB, a, b, intersect)) {
				return true;
			}

		}
		return false;
	}
	
	private void drawCellEdge(DelaunayCell cellA, DelaunayCell cellB) {
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
			float[] verticies = hullPoly.getTransformedVertices();
			for (int v = 0; v < verticies.length - 2; v += 2) {
				float x1 = verticies[v];
				float y1 = verticies[v + 1];
				float x2 = verticies[v + 2];
				float y2 = verticies[v + 3];
				// convex hull line
				Vector2 edgeA = new Vector2(x1, y1);
				Vector2 edgeB = new Vector2(x2, y2);
				
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
		
		
		int pSize = 6;
		for (DelaunayCell d : dCells) {
			//draw points
			if (drawPoints) {
				shape.setColor(Color.BLACK);
				shape.circle(d.a.x, d.a.y, pSize);
				shape.circle(d.b.x, d.b.y, pSize);
				shape.circle(d.c.x, d.c.y, pSize);
			}
			
			//draw delaunay triangles
			if (drawDelaunay) {
				shape.setColor(Color.GRAY);
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
					float[] verticies = hullPoly.getTransformedVertices();
					for (int v = 0; v < verticies.length - 2; v += 2) {
						float x1 = verticies[v];
						float y1 = verticies[v + 1];
						float x2 = verticies[v + 2];
						float y2 = verticies[v + 3];
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

	
		if (drawHull) {
			shape.setColor(Color.RED);
			shape.polyline(hullPoly.getVertices());
		}
		
		shape.end();
	}
	
	@Override
	public void render(float delta) {
		super.render(delta);
		
		//clear screen
		Gdx.gl20.glClearColor(1,1,1,1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		//render voronoi stuff
		drawStuff();
		
		//toggles, add/move points, reset
		updateControls();
	}


	private void updateControls() {
		//reset. new test points
		if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
			generateNewPoints(3);
		}
		
		
		//create new point
		if (Gdx.input.justTouched() && Gdx.input.isButtonPressed(Buttons.LEFT)) {
			points.add(Gdx.input.getX());
			points.add(Gdx.graphics.getHeight() - Gdx.input.getY());
			calculateDelaunay();
		}
		
		//drag points around
		if (Gdx.input.isButtonPressed(Buttons.RIGHT)) {
			int x = Gdx.input.getX();
			int y = Gdx.graphics.getHeight() - Gdx.input.getY();
			boolean mod = false;
			
			for (int i = 0; i < points.size && !mod; i += 2) {
				float px = points.get(i);
				float py = points.get(i+1);
				
				if (Vector2.dst(x, y, px, py) < 20) {
					points.set(i, x);
					points.set(i+1, y);
					mod = true;
				}
			}
	
			if (mod) {
				calculateDelaunay();
			}
		}
		
		
		//toggle drawings
		if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) {
			drawCircumcenter =! drawCircumcenter;
		}
		if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) {
			drawCircumcircle =! drawCircumcircle;
		}
		if (Gdx.input.isKeyJustPressed(Keys.NUM_3)) {
			drawPoints =! drawPoints;
		}
		if (Gdx.input.isKeyJustPressed(Keys.NUM_4)) {
			drawMidpoints =! drawMidpoints;
		}
		if (Gdx.input.isKeyJustPressed(Keys.NUM_5)) {
			drawVoronoi =! drawVoronoi;
		}
		if (Gdx.input.isKeyJustPressed(Keys.NUM_6)) {
			drawDelaunay =! drawDelaunay;
		}
		if (Gdx.input.isKeyJustPressed(Keys.NUM_7)) {
			drawHull =! drawHull;
		}
	}

}
