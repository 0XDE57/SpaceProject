package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;


public class DelaunayAnim extends TitleAnimation {

	float velocity;
	int numPoints;
	int pad;
	FloatArray points;
	FloatArray dirs;
	ShortArray triangles;
	DelaunayTriangulator delaunay = new DelaunayTriangulator();

	public DelaunayAnim() {
		velocity = 10;
		numPoints = 20;
		pad = 10;

		points = new FloatArray();
		dirs = new FloatArray();
		for (int i = 0; i < numPoints*2; i+=2) {
			float x = MathUtils.random(pad, Gdx.graphics.getWidth()-pad);
			float y = MathUtils.random(pad, Gdx.graphics.getHeight()-pad);

			float dir = MathUtils.random(0, MathUtils.PI2);
			float dx = (float) (Math.cos(dir) * velocity);
			float dy = (float) (Math.sin(dir) * velocity);

			points.add(x);
			points.add(y);

			dirs.add(dx);
			dirs.add(dy);
		}
	}

	@Override
	public void render(float delta, ShapeRenderer shape) {
		if (Gdx.input.justTouched()) {
			points.add(Gdx.input.getX());
			points.add(Gdx.graphics.getHeight()-Gdx.input.getY());
			float dir = MathUtils.random(0, MathUtils.PI2);
			float dx = (float) (Math.cos(dir) * velocity);
			float dy = (float) (Math.sin(dir) * velocity);
			dirs.add(dx);
			dirs.add(dy);
		}

		for (int i = 0; i < points.size; i+=2) {
			//bounds check
			if (points.get(i) <= pad || points.get(i) >= Gdx.graphics.getWidth()-pad)
				dirs.set(i, -dirs.get(i));

			if (points.get(i+1) <= pad || points.get(i+1) >= Gdx.graphics.getHeight()-pad)
				dirs.set(i+1, -dirs.get(i+1));

			points.set(i,points.get(i) + dirs.get(i) * delta);
			points.set(i+1,points.get(i+1) + dirs.get(i+1) * delta);
		}

		shape.begin(ShapeRenderer.ShapeType.Line);
		shape.setColor(Color.BLACK);
		triangles = delaunay.computeTriangles(points, false);
		for (int i = 0; i < triangles.size; i += 3) {
			//get points
			int p1 = triangles.get(i) * 2;
			int p2 = triangles.get(i + 1) * 2;
			int p3 = triangles.get(i + 2) * 2;
			Vector2 a = new Vector2(points.get(p1), points.get(p1 + 1));
			Vector2 b = new Vector2(points.get(p2), points.get(p2 + 1));
			Vector2 c = new Vector2(points.get(p3), points.get(p3 + 1));
			shape.triangle(
					a.x, a.y,
					b.x, b.y,
					c.x, c.y);
			//shape.circle(a.x, a.y, 2);
			//shape.circle(b.x, b.y, 2);
			//shape.circle(c.x, c.y, 2);
		}
		shape.end();
	}

	@Override
	public void resize(int width, int height) {
	}
}
