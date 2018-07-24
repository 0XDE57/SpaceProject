package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.ui.Button;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.MyScreenAdapter;
import com.spaceproject.utility.OpenSimplexNoise;

public class MainMenuScreen extends MyScreenAdapter {

	SpaceProject game;

	private BitmapFont fontComfortaaBold;
	private BitmapFont fontComfortaaBold1;

	// tree
	private float length;// tree size
	private float branchAngle;// angle to begin new branch from
	private float tiltAngle;
	private float startAngle;
	private int iterations; // how many branches to generate

	//noise
	float z = 0;
	int size = 10;
	float scale = 0.110f;
	OpenSimplexNoise noise = new OpenSimplexNoise();


	//delauney
	float velocity = 10;
	int numPoints = 20;
	int pad = 10;//radialDistance away from edge of screen
	FloatArray points;
	FloatArray dirs;
	ShortArray triangles;
	DelaunayTriangulator delaunay = new DelaunayTriangulator();

	MenuAnimation currentAnim;
	enum MenuAnimation {
		tree, delaunay
	}

	public static MenuAnimation randomAnim()  {
		return MenuAnimation.values()[MathUtils.random(MenuAnimation.values().length-1)];
	}


	Button btnStart, btnVoronoi, btn3D, btnNoise, btnShip;

	public MainMenuScreen(SpaceProject spaceProject) {
		this.game = spaceProject;

		//init buttons
		int menuX = 50;
		int menuY = 50;
		int width = 200;
		int height = 30;
		int pad = 10;
		btnStart = new Button("Play", menuX,menuY + (height + pad) * 4, width, height);
		btnVoronoi = new Button("DEBUG: Voronoi", 	menuX, menuY + (height + pad) * 3, width, height);
		btnNoise = new Button("DEBUG: Noise",		menuX, menuY + (height + pad) * 2, width, height);
		btn3D = new Button("DEBUG: 3D", 			menuX, menuY + (height + pad) * 1, width, height);
		btnShip = new Button("DEBUG: Ship Gen", 	menuX, menuY + (height + pad) * 0, width, height);

		//init delaunay
		points = new FloatArray();
		dirs = new FloatArray();
		for (int i = 0; i < numPoints*2; i+=2) {
			float x = MathUtils.random(pad, Gdx.graphics.getWidth()-pad);
			float y = MathUtils.random(pad, Gdx.graphics.getHeight()-pad);
			float dir = MathUtils.random(0, MathUtils.PI2);
			float dx = (float) (Math.cos(dir) * velocity);
			float dy = (float) (Math.sin(dir) * velocity);
			//ps.add(new Point(x, y, dir));
			points.add(x);
			points.add(y);
			//dirs.add(dir);
			dirs.add(dx);
			dirs.add(dy);
		}

		//init tree
		length = 15;
		branchAngle = 33;
		tiltAngle = 180;
		startAngle = MathUtils.PI2;
		iterations = 8;

		currentAnim = randomAnim();

		// font
		fontComfortaaBold = FontFactory.createFont(FontFactory.fontComfortaaBold, 90);
		fontComfortaaBold1 = FontFactory.createFont(FontFactory.fontComfortaaBold, 20);

		Gdx.graphics.setVSync(true);
	}


	public void render(float delta) {
		super.render(delta);
		Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);


		cam.position.set(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2,0);

		Gdx.gl.glEnable(Gdx.gl20.GL_BLEND);
		Gdx.gl.glBlendFunc(Gdx.gl20.GL_SRC_ALPHA, Gdx.gl20.GL_ONE_MINUS_SRC_ALPHA);
		drawNoise(delta);


		switch (currentAnim) {
			case delaunay:
				drawDelaunay(delta);
				break;
			case tree:
				drawTrees(delta);
				break;
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
			currentAnim = randomAnim();
		}

		//draw title
		batch.begin();
		fontComfortaaBold.draw(batch, "a space project", 50, Gdx.graphics.getHeight() - 50);

		btnStart.draw(batch, fontComfortaaBold1);
		btnVoronoi.draw(batch, fontComfortaaBold1);
		btnNoise.draw(batch,fontComfortaaBold1);
		btn3D.draw(batch, fontComfortaaBold1);
		btnShip.draw(batch, fontComfortaaBold1);
		batch.end();

		if (btnStart.isClicked()){
			game.setScreen(new GameScreen(true));
		} else if (btnVoronoi.isClicked()) {
			game.setScreen(new TestVoronoiScreen());
		} else if (btnNoise.isClicked()) {
			game.setScreen(new TestNoiseScreen());
		} else if (btn3D.isClicked()) {
			game.setScreen(new Test3DScreen());
		} else if (btnShip.isClicked()) {
			game.setScreen(new TestShipGenerationScreen());
		}


	}

	private void drawDelaunay(float delta) {
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

		shape.begin(ShapeType.Line);
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

	private void drawNoise(float delta) {
		shape.begin(ShapeType.Filled);
		for (int x = 0; x < Gdx.graphics.getWidth()/size; x++) {
			for (int y = 0; y < Gdx.graphics.getHeight()/size; y++) {
				float e = MyMath.inverseLerp(-1,1, (float)noise.eval(x*scale, y*scale, z));
				shape.setColor(e,e,e,1);
				shape.rect(x*size, y*size, size, size);
			}
		}
		z+= 0.2 * delta;
		if (Gdx.input.isKeyPressed(Input.Keys.V)) scale += 0.001f;
		if (Gdx.input.isKeyPressed(Input.Keys.B)) scale -= 0.001f;
		shape.end();
	}

	private void drawTree(ShapeRenderer g, float x, float y, float angle, int depth) {
		if (depth == 0) return;

		float x2 = (float) (x + (Math.cos(Math.toRadians(angle)) * depth * length));
		float y2 = (float) (y + (Math.sin(Math.toRadians(angle)) * depth * length));
		g.rectLine(x, y, x2, y2, depth);

		drawTree(g, x2, y2, angle - branchAngle + tiltAngle, depth - 1);
		drawTree(g, x2, y2, angle + branchAngle + tiltAngle, depth - 1);
	}

	private void drawTrees(float delta) {
		shape.begin(ShapeType.Filled);
		tiltAngle += 1 * delta;
		startAngle += 1 * delta;
		shape.setColor(0,0,0,1f);
		drawTree(shape, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, startAngle, iterations);
		drawTree(shape, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, startAngle - 120, iterations);
		drawTree(shape, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, startAngle - 240, iterations);
		shape.end();
	}

	public void dispose() {
		batch.dispose();
		shape.dispose();
	}
}
