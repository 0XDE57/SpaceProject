package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.spaceproject.utility.MyMath;

public class TouchUISystem extends EntitySystem {
	
	private Engine engine;
	
	//rendering
	private Matrix4 projectionMatrix = new Matrix4();
	private ShapeRenderer shape = new ShapeRenderer();
	
	Color white = new Color(1f, 1f, 1f, 0.5f);
	Color blue = new Color(0.5f, 0.5f, 1f, 0.7f);
	
	/////CONTROL POSITIONS/////
	//-----shoot button-----
	int shootButtonRadius = 70;
	int shootButtonPosX = Gdx.graphics.getWidth() - 80;
	int shootButtonPosY = 100;
	
	//-----vehicle button-----
	int vehicleButtonRaduis = 50;
	int vehicleButtonPosX = Gdx.graphics.getWidth() - 80;
	int vehicleButtonPosY = 300;
	
	//-----joystick-----
	int joystickRadius = 200;
	int joystickPosX = 20;
	int joystickPosY = 20;
	// padding to register touch if finger is a little bit off the joystick
	int joystickPadding = 100;
	// center of joystick
	int stickCenterX = joystickPosX + joystickRadius;
	int stickCenterY = joystickPosY + joystickRadius;
	// current position of stick
	int stickX = stickCenterX;
	int stickY = stickCenterY;
	
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;	
	}
	
	@Override
	public void update(float delta) {

		//reset stick to center
		stickX = stickCenterX;
		stickY = stickCenterY;
		
		//check controls
		checkFireButton();		
		checkVehicleButton();		
		checkJoystick();
	
		//set projection matrix so things render using correct coordinates
		// TODO: only needs to be called when screen size changes
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		shape.setProjectionMatrix(projectionMatrix);
			
		//draw buttons on screen
		drawControls();

	}

	/**
	 * Draw on-screen buttons.
	 */
	private void drawControls() { 
		//enable transparency
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		shape.begin(ShapeType.Line);
		
		//draw joystick base
		shape.setColor(white);
		shape.circle(stickCenterX, stickCenterY, joystickRadius, 12);

		shape.end();

		
		shape.begin(ShapeType.Filled);		
		
		//draw stick on joystick
		shape.setColor(engine.getSystem(PlayerControlSystem.class).moveForward ? blue : white);
		shape.circle(stickX, stickY, joystickRadius / 5, 6);
		shape.line(stickX, stickY, stickCenterX, stickCenterY);
		
		//draw shoot button
		shape.setColor(engine.getSystem(PlayerControlSystem.class).shoot ? blue : white);
		shape.circle(shootButtonPosX - shootButtonRadius, shootButtonPosY + shootButtonRadius, shootButtonRadius, 6);
			
		//draw vehicle button
		//TODO: test if player is in vehicle or can get in a vehicle;
		//if (engine.getSystem(PlayerControlSystem.class).isInVehicle() || engine.getSystem(PlayerControlSystem.class).canGetInVehicle()) {
		shape.setColor(engine.getSystem(PlayerControlSystem.class).changeVehicle ? blue : white);
		shape.circle(vehicleButtonPosX - vehicleButtonRaduis, vehicleButtonPosY + vehicleButtonRaduis, vehicleButtonRaduis, 6);
		shape.end();
		
		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	/**
	 * Check if joystick being used.
	 */
	private void checkJoystick() { 
		//check finger 0
		float distanceToJoystick0 = MyMath.distance(Gdx.input.getX(0), Gdx.graphics.getHeight() - Gdx.input.getY(0), 
				stickCenterX, stickCenterY);
		
		//check finger 1
		float distanceToJoystick1 = MyMath.distance(Gdx.input.getX(1), Gdx.graphics.getHeight() - Gdx.input.getY(1), 
				stickCenterX, stickCenterY);
		
		//finger 0 is touched
		boolean finger0 = Gdx.input.isTouched(0) && distanceToJoystick0 <= joystickRadius + joystickPadding;
		boolean finger1 = Gdx.input.isTouched(1) && distanceToJoystick1 <= joystickRadius + joystickPadding;
		
		//analog movement: how much energy to put into movement
		float powerRatio = 0;
		
		if (finger0) {
			stickX = Gdx.input.getX(0);
			stickY = Gdx.graphics.getHeight() - Gdx.input.getY(0);
			powerRatio = distanceToJoystick0 / joystickRadius;
		} 
		if (finger1) {
			stickX = Gdx.input.getX(1);
			stickY = Gdx.graphics.getHeight() - Gdx.input.getY(1);
			powerRatio = distanceToJoystick1 / joystickRadius;
		}

		if (finger0 || finger1) {
			//face finger
			float angle = MyMath.angleTo(stickX, stickY, stickCenterX, stickCenterY);
			engine.getSystem(PlayerControlSystem.class).angleFacing = angle;
						
			if (powerRatio > 1) powerRatio = 1;
			if (powerRatio < 0) powerRatio = 0;

			engine.getSystem(PlayerControlSystem.class).movementMultiplier = powerRatio;
			engine.getSystem(PlayerControlSystem.class).moveForward = true;
			/*
			System.out.println(powerRatio);
			if (powerRatio < 0.5f) {
				engine.getSystem(PlayerControlSystem.class).applyBreaks = true;
				engine.getSystem(PlayerControlSystem.class).moveForward = false;
			} else {
				engine.getSystem(PlayerControlSystem.class).moveForward = true;
				engine.getSystem(PlayerControlSystem.class).applyBreaks = false;
			}*/
			
		} else {
			engine.getSystem(PlayerControlSystem.class).moveForward = false;
			engine.getSystem(PlayerControlSystem.class).applyBreaks = false;
		}
	}

	/**
	 * Check if enter/exit vehicle button is pressed.
	 */
	private void checkVehicleButton() {
		//TODO: fix vehicle button for multitouch
		float distanceToVehicleButton = MyMath.distance(
				Gdx.input.getX(), 
				Gdx.graphics.getHeight() - Gdx.input.getY(), 
				vehicleButtonPosX - vehicleButtonRaduis,
				vehicleButtonPosY + vehicleButtonRaduis);
		
		engine.getSystem(PlayerControlSystem.class).changeVehicle = (Gdx.input.isTouched() && distanceToVehicleButton <= vehicleButtonRaduis);
		
	}

	/**
	 * Check if fire button is pressed.
	 */
	private void checkFireButton() {
		//finger 0
		float distanceToShootButton = MyMath.distance(
				Gdx.input.getX(0),
				Gdx.graphics.getHeight() - Gdx.input.getY(0), 
				shootButtonPosX - shootButtonRadius, 
				shootButtonPosY + shootButtonRadius);

		//finger 1		
		float distanceToShootButton1 = MyMath.distance(
				Gdx.input.getX(1),
				Gdx.graphics.getHeight() - Gdx.input.getY(1), 
				shootButtonPosX - shootButtonRadius, 
				shootButtonPosY + shootButtonRadius);

		//if a finger is touching the touch is on fire button
		if ((Gdx.input.isTouched(0) && distanceToShootButton <= shootButtonRadius) 
				|| (Gdx.input.isTouched(1) && distanceToShootButton1 <= shootButtonRadius)) {
			engine.getSystem(PlayerControlSystem.class).shoot = true;
		} else {
			engine.getSystem(PlayerControlSystem.class).shoot = false;
		}
	}
}
