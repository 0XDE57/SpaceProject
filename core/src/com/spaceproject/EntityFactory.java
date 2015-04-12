package com.spaceproject;

import java.util.Random;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;

public class EntityFactory {

	
	public static Entity[] createPlanetarySystem(float x, float y) {
		//MathUtils.random.setSeed((long) (x + y) + worldSeed);
		Entity[] entities = new Entity[MathUtils.random(1,12) + 1];
		
		//add star to center of planetary system
		Entity star = createStar(x, y);
		entities[0] = star;
		
		float distance = 0;
		boolean rotationDirection = MathUtils.randomBoolean();
		//create planets around star
		for (int i = 1; i < entities.length; ++i) {
			distance += MathUtils.random(1600, 2100);
			entities[i] = createPlanet(star, distance, rotationDirection);
		}
		
		
		return entities;
		
	}
	
	public static Entity createStar(float x, float y) {
		Entity entity = new Entity();

		// Create components
		TextureComponent texture = new TextureComponent();
		TransformComponent transform = new TransformComponent();

		//generate random size 
		int minSize = 20;
		int maxSize = 250;
		int radius = new Random().nextInt(maxSize - minSize) + minSize;

		// generate pixmap texture
		Pixmap pixmap = new Pixmap(radius * 2, radius * 2, Format.RGBA8888);
		float scale = 4.0f;
		
		//draw circle for planet
		pixmap.setColor(1, 1, 0, 1);
		pixmap.fillCircle(radius, radius, radius - 1);
		//draw outline
		pixmap.setColor(1, 0, 1, 1);
		pixmap.drawCircle(radius, radius, radius - 1);
		Texture pixmapTex = new Texture(pixmap);//create texture from pixmap
		pixmap.dispose(); // clean up
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;
		
		// set position
		transform.pos.set(x, y, 0); 

		//add bounding box
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = radius*2*scale;
		bounds.bounds.width = radius*2*scale;
		
		//add components to entity
		entity.add(bounds);
		entity.add(transform);
		entity.add(texture);

		return entity;
	}
	
	public static Entity createPlanet(Entity parent, float distance, boolean rotationDir) {
		Entity entity = new Entity();

		// Create components
		TextureComponent texture = new TextureComponent();

		//generate random size 
		int minSize = 12;
		int maxSize = 200;
		int radius = new Random().nextInt(maxSize - minSize) + minSize;

		
		// generate pixmap texture
		Pixmap pixmap = new Pixmap(radius * 2, radius * 2, Format.RGBA8888);
		float scale = 4.0f;
		//draw circle for planet
		pixmap.setColor(0, 0, 1, 1);
		pixmap.fillCircle(radius, radius, radius - 1);
		//draw outline
		pixmap.setColor(1, 1, 1, 1);
		pixmap.drawCircle(radius, radius, radius - 1);
		
		Texture pixmapTex = new Texture(pixmap);//create texture from pixmap
		pixmap.dispose(); // clean up
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;

		//add bounding box
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = radius*2*scale;
		bounds.bounds.width = radius*2*scale;
		
		//orbit 
		OrbitComponent orbit = new OrbitComponent();
		orbit.parent = parent;
		orbit.rotSpeed = MathUtils.random(0.002f, 0.05f); //rotation speed of planet
		orbit.orbitSpeed = MathUtils.random(0.0007f, 0.004f); //orbit speed of planet	
		orbit.angle = MathUtils.random(3.14f * 2); //angle from star
		orbit.distance = distance;
		orbit.rotateClockwise = rotationDir;
		
		//add components to entity
		entity.add(bounds);
		entity.add(new TransformComponent());
		entity.add(texture);
		entity.add(orbit);

		return entity;
	}
	
	//create bullet than has a damage/weight equivalent or relative to the size(); bigger projectile = more damage
	//Enforce POT? (power of two)
	public static Entity createProjectile(Vector3 position, float dx, float dy, int size) {
		Entity entity = new Entity();
				
		//create texture
		TextureComponent texture = new TextureComponent();
		float scale = 4.0f;
		Pixmap pixmap = new Pixmap(size, size/2 == 0 ? 1 : size/2, Format.RGB888);
		pixmap.setColor(1,1,1,1);
		pixmap.fill();
		texture.texture = new Texture(pixmap);
		pixmap.dispose();
		texture.scale = scale;
		
		//bounding box
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.width = size * scale;
		bounds.bounds.height = size/2 == 0 ? 1 : size/2 * scale;
		
		//set position and orientation
		TransformComponent transfrom = new TransformComponent();
		transfrom.pos.set(position);
		transfrom.rotation = (float) Math.toRadians(new Vector2(dx, dy).angle());
		
		//set position
		MovementComponent movement = new MovementComponent();
		movement.velocity.add(dx, dy);
		
		//set expire time
		ExpireComponent expire = new ExpireComponent();
		expire.time = 5;//in seconds ~approx
		
		entity.add(expire);
		entity.add(texture);
		entity.add(bounds);
		entity.add(transfrom);
		entity.add(movement);
		
		return entity;
	}
	
	public static Entity createCharacter(int x, int y, boolean inVehicle, Entity vehicle) {
		Entity entity = new Entity();
		
		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();
		
		transform.pos.set(x, y, 0);
		
		int size = 4;
		float scale = 4.0f;
		Pixmap pixmap = new Pixmap(size, size, Format.RGB888);
		pixmap.setColor(0.5f, 0.5f, 0.5f, 1);
		pixmap.fill();
		pixmap.setColor(0, 1, 1, 1);
		pixmap.drawPixel(3, 2);
		pixmap.drawPixel(3, 1);

		texture.texture = new Texture(pixmap);
		pixmap.dispose();
		texture.scale = scale;
		
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = size * scale;
		bounds.bounds.width = size * scale;
		
		
		entity.add(bounds);
		entity.add(transform);
		entity.add(texture);
		entity.add(new MovementComponent());
			
		return entity;
	}
	
	public static Entity createShip2(int x, int y) {
		Entity entity = new Entity();

		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();

		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		//generate random even size 
		int size;
		do {
			int minSize = 8;
			int maxSize = 36;
			size = new Random().nextInt(maxSize - minSize) + minSize;
		} while (size % 2 == 1);
		
		// generate pixmap texture
		//int size = 24;
		Pixmap pixmap = new Pixmap(size, size/2, Format.RGBA8888);
		pixmap.setColor(1, 1, 1, 1);
		pixmap.fillRectangle(0, 0, size, size);
		
		pixmap.setColor(0.7f,0.7f,0.7f,1);
		pixmap.drawRectangle(0, 0, size-1, size-1/2);
		
		Texture pixmapTex = new Texture(pixmap);
		float scale = 4.0f;
		pixmap.dispose(); // clean up
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;
		
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = size * scale;
		bounds.bounds.width = size * scale;

		
		entity.add(bounds);
		entity.add(texture);
		entity.add(transform);
		entity.add(new MovementComponent());
		entity.add(new VehicleComponent());
		
		return entity;
	}
	
	public static Entity createShip3(int x, int y) {
		Entity entity = new Entity();

		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();

		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		//generate random even size 
		int size;
		do {
			int minSize = 8;
			int maxSize = 36;
			size = new Random().nextInt(maxSize - minSize) + minSize;
		} while (size % 2 == 1);
		
		//TODO: move image generation into its own class, return image
		// generate pixmap texture
		Pixmap pixmap = new Pixmap(size, size/2, Format.RGBA8888);
			
		int width = pixmap.getWidth()-1;
		int height = pixmap.getHeight()-1;	
		
		// 0-----width
		// |  
		// |
		// |
		// height
		
		/*
		//fill to see image size/visual aid---------
		pixmap.setColor(1, 1, 1, 1);
		pixmap.fill();
		
		//corner pins for visual aid----------------
		pixmap.setColor(1,0,0,1);//red: top-right
		pixmap.drawPixel(0, 0);
		
		pixmap.setColor(0,1,0,1);//green: top-left
		pixmap.drawPixel(width, 0);
		
		pixmap.setColor(0,0,1,1);//blue: bottom-left
		pixmap.drawPixel(0, height);
		
		pixmap.setColor(1,1,0,1);//yellow: bottom-right
		pixmap.drawPixel(width, height);
		*/
		
		//generation------------------------------------------------------
		Random rng = new Random();
		int edge = rng.nextInt(size/8);
		
		for (int yY = 0; yY <= width; yY++) {
			
			if (yY == 0 || yY == width) {
				//draw front and back edge
				pixmap.setColor(0.7f, 0.7f, 0.7f, 1);
				pixmap.drawLine(yY,edge+1, yY, height-edge-1);
			} else {
				
			//draw body
			pixmap.setColor(0, 0.5f, 0, 1);
			pixmap.drawLine(yY, edge, yY, height-edge);
			
			//draw edge
			pixmap.setColor(0.7f, 0.7f, 0.7f, 1);
			pixmap.drawPixel(yY, edge);//left edge			
			pixmap.drawPixel(yY, height-edge);//right edge

			}
			
			//gen next edge --------------------
			edge = rng.nextBoolean() ? (rng.nextBoolean() ? --edge: ++edge) : edge;
			if (edge < 0) edge = 0;
			if (edge > size/8) edge = size/8;
		}
		
		
		
		
		
		
		Texture pixmapTex = new Texture(pixmap);
		float scale = 4.0f;
		pixmap.dispose(); // clean up
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;
		
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = size * scale;
		bounds.bounds.width = size * scale;

		
		entity.add(bounds);
		entity.add(texture);
		entity.add(transform);
		entity.add(new MovementComponent());
		entity.add(new VehicleComponent());//vehicle makes it drive-able
		
		return entity;
	}
	
	
	public static Entity createShip(int x, int y) {
		Entity entity = new Entity();

		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();

		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		// generate pixmap texture
		int size = 16;
		Pixmap pixmap = new Pixmap(size, size, Format.RGBA8888);
		pixmap.setColor(1, 1, 1, 1);
		pixmap.fillTriangle(0, 0, 0, size-1, size-1, size/2);
		
		pixmap.setColor(0, 1, 1, 1);
		pixmap.drawLine(size, size/2, size/2, size/2);
		
		
		Texture pixmapTex = new Texture(pixmap);
		float scale = 4.0f;
		pixmap.dispose(); // clean up
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;
		
		BoundsComponent bounds = new BoundsComponent();
		bounds.bounds.height = size * scale;
		bounds.bounds.width = size * scale;

		
		entity.add(bounds);
		entity.add(texture);
		entity.add(transform);
		entity.add(new MovementComponent());
		entity.add(new VehicleComponent());
		
		return entity;
	}
}
