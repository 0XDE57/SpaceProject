package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;

public class MainMenuScreen extends ScreenAdapter {

	SpaceProject game;

	private SpriteBatch batch;
	private ShapeRenderer shape;

	private FreeTypeFontGenerator generator;
	private FreeTypeFontParameter parameter;
	private BitmapFont fontComfortaaBold;
	private BitmapFont fontComfortaaBold1;

	// https://www.youtube.com/watch?v=jEmSxcr-rRc
	// tree
	private float length;// tree size
	private float branchAngle;// angle to begin new branch from
	private float tiltAngle;
	private float startAngle;
	private int iterations; // how many branches to generate

	private boolean switchScreen;
	private float time;

	public MainMenuScreen(SpaceProject spaceProject) {
		this.game = spaceProject;

		// graphics
		shape = new ShapeRenderer();
		batch = new SpriteBatch();

		switchScreen = false;
		time = 1;

		length = 15;
		branchAngle = 33;
		tiltAngle = 0;
		startAngle = 90;
		iterations = 7;

		// font
		generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/ComfortaaBold.ttf"));
		parameter = new FreeTypeFontParameter();
		parameter.size = 60;
		fontComfortaaBold = generator.generateFont(parameter);
		
		parameter.size = 30;
		fontComfortaaBold1 = generator.generateFont(parameter);
		generator.dispose();

	}

	private void drawTree(ShapeRenderer g, float x22, float y22, float angle, int depth) {
		if (depth == 0) return;
		
		float x2 = (float) (x22 + (Math.cos(Math.toRadians(angle)) * depth * length));
		float y2 = (float) (y22 + (Math.sin(Math.toRadians(angle)) * depth * length));
		g.rectLine(x22, y22, x2, y2, depth);
		
		drawTree(g, x2, y2, angle - branchAngle + tiltAngle, depth - 1);
		drawTree(g, x2, y2, angle + branchAngle + tiltAngle, depth - 1);
	}

	public void render(float delta) {
		Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
		if (!switchScreen)
			Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if (Gdx.input.justTouched())
			switchScreen = true;
		
		
		// rotate tree
		tiltAngle += 10 * delta;
		startAngle += 10 * delta;	

		//draw trees
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLUE);
		drawTree(shape, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, startAngle, iterations);
		shape.setColor(Color.RED);
		drawTree(shape, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, startAngle - 120, iterations);
		shape.setColor(Color.GREEN);
		drawTree(shape, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, startAngle - 240, iterations);
		shape.end();
		
		//draw title
		batch.begin();
		fontComfortaaBold.draw(batch, "a space project", 50, Gdx.graphics.getHeight() - 50);
		//draw stats
		fontComfortaaBold1.draw(batch, Math.round(startAngle) + " : origin angle", 50, Gdx.graphics.getHeight()/2);
		fontComfortaaBold1.draw(batch, Math.round(tiltAngle) + " : branch angle", 50, Gdx.graphics.getHeight()/2-25);
		batch.end();
		
		if (switchScreen) {
			time -= 1 * delta;
			System.out.println(time);
			if (time < 0) {
				dispose();
				game.setScreen(new SpaceScreen(game, new Vector3()));
			}
		}

	}

	public void dispose() {
		batch.dispose();
		shape.dispose();
	}
}
