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
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.utility.MyScreenAdapter;

//https://en.wikipedia.org/wiki/Circumscribed_circle
//http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect/565282#565282
//http://stackoverflow.com/questions/31021968/correct-use-of-polygon-triangulators-in-libgdx
class DelaunayCell {
	Vector2 a, b, c;
	Vector2 midAB, midBC, midCA;
	boolean ab, bc, ca;
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

	FloatArray points;
	DelaunayTriangulator tri;
	ShortArray triangles;
	//ConvexHull convex;	
	ArrayList<DelaunayCell> dCells = new ArrayList<DelaunayCell>();
	float[] hull;// = convex.computePolygon(points, false).items;
	
	boolean drawCircumcircle = false,
		drawCircumcenter = true,
		drawPoints = true,
		drawDelaunay = true,
		drawVoronoi = true,
		drawMidpoints = true,
		drawHull = true;
	
	
	int pX = 0;
	int pY = Gdx.graphics.getHeight();
	
	
	
	public TestVoronoiScreen() {
		cam.position.x = Gdx.graphics.getWidth()/2;
		cam.position.y = Gdx.graphics.getHeight()/2;
		
		tri = new DelaunayTriangulator();
		//convex = new ConvexHull();
		
		generateNewPoints(10);

	}


	private void generateNewPoints(int numPoints) {
		points = new FloatArray();
		for (int i = 0; i < numPoints*2; i+=2) {
			float x = MathUtils.random(0, Gdx.graphics.getWidth());
			float y = MathUtils.random(0, Gdx.graphics.getHeight());
			points.add(x);
			points.add(y);
			//System.out.println(x + "," + y);
		}
		
		
		calculateDelaunay();
		
	}


	private void calculateDelaunay() {
		triangles = tri.computeTriangles(points, false);
		//triangles = new EarClippingTriangulator().computeTriangles(points);
		
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
		
		ConvexHull convex = new ConvexHull();
		hull = convex.computePolygon(points, false).toArray();
		hullPoly = new Polygon(hull);
	}
	Polygon hullPoly;
	
	@Override
	public void render(float delta) {
		super.render(delta);
		
		Gdx.gl20.glClearColor(1,1,1,1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		shape.begin(ShapeType.Line);
		
		
		int dist = 6;
		for (DelaunayCell d : dCells) {
			//draw points
			if (drawPoints) {
				shape.setColor(Color.BLACK);
				shape.circle(d.a.x, d.a.y, dist);
				shape.circle(d.b.x, d.b.y, dist);
				shape.circle(d.c.x, d.c.y, dist);
			}
			
			//draw delaunay triangulation
			if (drawDelaunay) {
				shape.setColor(Color.GRAY);
				shape.triangle(d.a.x, d.a.y, // A
						d.b.x, d.b.y, // B
						d.c.x, d.c.y);// C
			}
			
			//draw midpoints
			if (drawMidpoints) {
				shape.setColor(Color.BLUE);
				shape.circle(d.midAB.x, d.midAB.y, 1);
				shape.circle(d.midCA.x, d.midCA.y, 2);
				shape.circle(d.midBC.x, d.midBC.y, 3);
			}
			
			
			
			//draw voronoi cells
			if (drawVoronoi) {
				//shape.setColor(Color.ORANGE);		
				//shape.line(d.circumcenter, d.midAB);
				//shape.line(d.circumcenter, d.midBC);
				//shape.line(d.circumcenter, d.midCA);
				
				float[] verticies = hullPoly.getTransformedVertices();
				for (int v = 0; v < verticies.length - 2; v += 2) {
					float x1 = verticies[v];
					float y1 = verticies[v + 1];
					float x2 = verticies[v + 2];
					float y2 = verticies[v + 3];
					// convex hull line
					Vector2 pA1 = new Vector2(x1, y1);
					Vector2 pA2 = new Vector2(x2, y2);
					shape.setColor(Color.RED);
					shape.line(pA1, pA2);

					Vector2 intersect = new Vector2();

					// AB
					if (Intersector.intersectSegments(pA1, pA2, d.circumcenter, d.midAB, intersect)) {
						shape.setColor(Color.LIME);
						shape.line(d.midAB, intersect);
						shape.circle(intersect.x, intersect.y, 3);
					} else {
						shape.setColor(Color.ORANGE);
						shape.line(d.circumcenter, d.midAB);
					}
					
					// BC
					if (Intersector.intersectSegments(pA1, pA2, d.circumcenter, d.midBC, intersect)) {
						shape.setColor(Color.LIME);
						shape.line(d.midBC, intersect);
						shape.circle(intersect.x, intersect.y, 3);
					} else {
						shape.setColor(Color.ORANGE);
						shape.line(d.circumcenter, d.midBC);
					}

					// CA
					if (Intersector.intersectSegments(pA1, pA2, d.circumcenter, d.midCA, intersect)) {
						shape.setColor(Color.LIME);
						shape.line(d.midCA, intersect);
						shape.circle(intersect.x, intersect.y, 3);
					} else {
						shape.setColor(Color.ORANGE);
						shape.line(d.circumcenter, d.midCA);
					}
					
				}
		
				
				/*
				shape.setColor(Color.PINK);
				for (DelaunayCell other : dCells) {
					//if line from center to midpoint hits another voronoi point, draw to that voronoi point
					float ab = Intersector.distanceLinePoint(d.circumcenter.x, d.circumcenter.y, d.midAB.x, d.midAB.y, other.circumcenter.x, other.circumcenter.y);
					float bc = Intersector.distanceLinePoint(d.circumcenter.x, d.circumcenter.y, d.midBC.x, d.midBC.y, other.circumcenter.x, other.circumcenter.y);
					float ca = Intersector.distanceLinePoint(d.circumcenter.x, d.circumcenter.y, d.midCA.x, d.midCA.y, other.circumcenter.x, other.circumcenter.y);

					float e = 0.01f;
					if (ab < e || bc < e || ca < e) {				
						//shape.line(d.circumcenter, other.circumcenter);
					}
					
					
					/*
					if (ab < e) {
						d.ab = true;
					}
					if (bc < e) {
						d.bc = true;
					}
					if (ca < e) {
						d.ca = true;
					}*

				}
/*
				shape.setColor(Color.BLACK);
				if (!d.ab) {
					shape.line(d.circumcenter, d.midAB);
				}

				if (!d.bc) {
					shape.line(d.circumcenter, d.midBC);
				}

				if (!d.ca) {
					shape.line(d.circumcenter, d.midCA);
				}*/
			}
			
			
			//draw circumcircle
			if (!hullPoly.contains(d.circumcenter)) {
				shape.setColor(Color.MAROON);
			} else {
				shape.setColor(Color.GREEN);
			}	
			if (drawCircumcircle) shape.circle(d.circumcenter.x, d.circumcenter.y, d.circumradius);
			if (drawCircumcenter) shape.circle(d.circumcenter.x, d.circumcenter.y, dist);
		}

		
		/*
		//////////////////////////////////////////////////////////
		Vector2 p1A = new Vector2();
		Vector2 p1B = new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());	
		Vector2 p2A = new Vector2(pX, pY);
		Vector2 p2B = new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight()-Gdx.input.getY());
		shape.line(p1A, p1B);
		shape.line(p2A, p2B);
		
		Vector2 intersect = new Vector2();		
		if (Intersector.intersectLines(p1A, p1B, p2A, p2B, intersect)) {
			shape.setColor(Color.DARK_GRAY);
		} else {
			shape.setColor(Color.PINK);
		}
		shape.circle(intersect.x, intersect.y, 2);
		shape.circle(intersect.x, intersect.y, 8);
		////////////////////////////////////////////////////////////
		*/
		
		if (drawHull) {
			shape.setColor(Color.RED);
			//shape.polyline(hullPoly.getVertices());
			//shape.polyline(hull);
		}
		
		shape.end();
		
		if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
			generateNewPoints(3);
		}
		
		
		if (Gdx.input.justTouched() && Gdx.input.isButtonPressed(Buttons.LEFT)) {
			points.add(Gdx.input.getX());
			points.add(Gdx.graphics.getHeight() - Gdx.input.getY());
			//tri.computeTriangles(points, false);
			calculateDelaunay();
		}
		
		
		if (Gdx.input.isButtonPressed(Buttons.RIGHT)) {
			int x = Gdx.input.getX();
			int y = Gdx.graphics.getHeight() - Gdx.input.getY();
			boolean mod = false;
			
			for (int i = 0; i < points.size && !mod; i += 2) {
				float px = points.get(i);
				float py = points.get(i+1);
				
				if (Vector2.dst(x, y, px, py) < dist*3) {
					points.set(i, x);
					points.set(i+1, y);
					mod = true;
				}
			}
	
			if (mod) {
				calculateDelaunay();
			}
		}
		
		if (Gdx.input.isButtonPressed(Buttons.MIDDLE)) {
			pX = Gdx.input.getX();
			pY = Gdx.graphics.getHeight()-Gdx.input.getY();
		}
		
		
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
	}
	

}
