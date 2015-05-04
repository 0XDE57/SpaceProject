package com.spaceproject;


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
		MathUtils.random.setSeed((long)(x + y) * SpaceProject.SEED);
		
		Entity[] planetarySystemEntities = new Entity[MathUtils.random(1,12) + 1];
		boolean rotationDirection = MathUtils.randomBoolean(); //rotation of system (orbits and spins)
		
		//add star to center of planetary system
		Entity star = createStar(x, y, rotationDirection);
		planetarySystemEntities[0] = star;
		
		float distance = 0;
		
		//create planets around star
		for (int i = 1; i < planetarySystemEntities.length; ++i) {
			distance += MathUtils.random(1600, 2100);
			planetarySystemEntities[i] = createPlanet(star, distance, rotationDirection);
		}
				
		return planetarySystemEntities;
		
	}
	
	public static Entity createStar(float x, float y, boolean rotationDir) {
		MathUtils.random.setSeed((long)(x + y) * SpaceProject.SEED);
		Entity entity = new Entity();

		// Create components
		TextureComponent texture = new TextureComponent();
		TransformComponent transform = new TransformComponent();

		//generate random size 
		int minSize = 20;
		int maxSize = 250;
		int radius = MathUtils.random(minSize, maxSize);

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
		
		//Orbit for rotation of self (kinda hacky, not really orbiting. just rotating)
		OrbitComponent orbit = new OrbitComponent();
		orbit.rotateClockwise = rotationDir;
		
		//add components to entity
		entity.add(orbit);
		entity.add(bounds);
		entity.add(transform);
		entity.add(texture);

		return entity;
	}
	
	public static Entity createPlanet(Entity parent, float distance, boolean rotationDir) {
		MathUtils.random.setSeed((long)distance * SpaceProject.SEED);
		Entity entity = new Entity();

		// Create components
		TextureComponent texture = new TextureComponent();

		//generate random size 
		int minSize = 12;
		int maxSize = 200;
		int radius = MathUtils.random(minSize, maxSize);

		
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
		MathUtils.random.setSeed((long)(x + y) * SpaceProject.SEED);
		Entity entity = new Entity();

		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();

		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		//generate random even size 
		int size;
		int minSize = 8;
		int maxSize = 36;
		do {		
			size = MathUtils.random(minSize, maxSize);
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

		MathUtils.random.setSeed((long)(x + y) * SpaceProject.SEED);
		
		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();

		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		//generate random even size 
		int size;
		int minSize = 10;
		int maxSize = 36;
		
		do {
			//generate even size
			size = MathUtils.random(minSize, maxSize);
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
		
		/////////////////////////////////image generation////////////////////////////////////
		//smallest height a ship can be (4 because player is 4 pixels)
		int minEdge = 4; 
		//smallest starting point for an edge
		float initialMinimumEdge = height * 0.8f; 
		//edge to create shape of ship. initialize to random starting size
		int edge = MathUtils.random((int)initialMinimumEdge, height-1);
		
		for (int yY = 0; yY <= width; yY++) {			
			// draw body
			if (yY == 0 || yY == width) {
				//if first or last position of texture, "cap" it to complete the edging
				pixmap.setColor(0.7f, 0.7f, 0.7f, 1);
			} else {
				pixmap.setColor(0, 0.5f, 0, 1);
			}
			pixmap.drawLine(yY, edge, yY, height - edge);
			
			// draw edging 
			pixmap.setColor(0.7f, 0.7f, 0.7f, 1);
			pixmap.drawPixel(yY, edge);// bottom edge
			pixmap.drawPixel(yY, height - edge);// top edge
					
			//generate next edge---------------------------------------------------------------------
			//beginning and end of ship have special rule to not be greater than the consecutive or previous edge
			//so that the "caps" look right
			if (yY == 0) { //beginning 
				++edge;
			} else if (yY == width-1) { //end
				--edge;
			} else { //body
				//random decide to move edge. if so, move edge either up or down 1 pixel
				edge = MathUtils.randomBoolean() ? (MathUtils.randomBoolean() ? --edge: ++edge) : edge;
			}
							
			//keep edges within height and minEdge
			if (edge > height) {
				edge = height;
			}
			if (edge - (height - edge) < minEdge) {
				edge = (height + minEdge) / 2;
			}
		}		
		
		/*
		//fill to see image size/visual aid---------
		pixmap.setColor(1, 1, 1, 1);
		// pixmap.fill();

		// corner pins for visual aid----------------
		pixmap.setColor(1, 0, 0, 1);// red: top-right
		pixmap.drawPixel(0, 0);

		pixmap.setColor(0, 1, 0, 1);// green: top-left
		pixmap.drawPixel(width, 0);

		pixmap.setColor(0, 0, 1, 1);// blue: bottom-left
		pixmap.drawPixel(0, height);

		pixmap.setColor(1, 1, 0, 1);// yellow: bottom-right
		pixmap.drawPixel(width, height);
		*/
		//////////////////end image generation//////////////////////
		
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
