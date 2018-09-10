package com.spaceproject.screens.menuanim;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

public class TreeAnimation extends MainMenuAnimation {

	float length;
	float branchAngle;
	float tiltAngle;
	float startAngle;
	int iterations;
	float rotSpeed ;

	public TreeAnimation() {
		length = 15;
		branchAngle = 33;
		tiltAngle = 180;
		startAngle = MathUtils.PI2;
		iterations = 8;
		rotSpeed = 1;
	}

	@Override
	public void render(float delta, ShapeRenderer shape) {
		shape.begin(ShapeRenderer.ShapeType.Filled);
		tiltAngle += rotSpeed * delta;
		startAngle += rotSpeed * delta;
		shape.setColor(0,0,0,1f);
		int x = Gdx.graphics.getWidth() / 2;
		int y = Gdx.graphics.getHeight() / 2;
		drawTree(shape, x, y, startAngle, iterations);
		drawTree(shape, x, y, startAngle - 120, iterations);
		drawTree(shape, x, y, startAngle - 240, iterations);
		shape.end();
	}

	@Override
	public void resize(int width, int height) {
	}


	private void drawTree(ShapeRenderer shape, float x, float y, float angleDegrees, int depth) {
		if (depth == 0) return;

		float x2 = (float) (x + (Math.cos(Math.toRadians(angleDegrees)) * depth * length));
		float y2 = (float) (y + (Math.sin(Math.toRadians(angleDegrees)) * depth * length));
		shape.rectLine(x, y, x2, y2, depth);

		drawTree(shape, x2, y2, angleDegrees - branchAngle + tiltAngle, depth - 1);
		drawTree(shape, x2, y2, angleDegrees + branchAngle + tiltAngle, depth - 1);
	}

}
