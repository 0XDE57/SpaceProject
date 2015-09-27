package com.spaceproject.utility;

import com.badlogic.ashley.core.ComponentMapper;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.MissileComponent;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;

public class Mappers {
	
	public static final ComponentMapper<TextureComponent> texture = ComponentMapper.getFor(TextureComponent.class);

	public static final ComponentMapper<TransformComponent> transform = ComponentMapper.getFor(TransformComponent.class);	

	public static final ComponentMapper<MovementComponent> movement = ComponentMapper.getFor(MovementComponent.class);	

	public static final ComponentMapper<BoundsComponent> bounds = ComponentMapper.getFor(BoundsComponent.class);
	
	public static final ComponentMapper<VehicleComponent> vehicle = ComponentMapper.getFor(VehicleComponent.class);
	
	public static final ComponentMapper<ExpireComponent> expire = ComponentMapper.getFor(ExpireComponent.class);
	
	public static final ComponentMapper<OrbitComponent> orbit = ComponentMapper.getFor(OrbitComponent.class);
	
	public static final ComponentMapper<CannonComponent> cannon = ComponentMapper.getFor(CannonComponent.class);	

	public static final ComponentMapper<MissileComponent> missile = ComponentMapper.getFor(MissileComponent.class);
	
	public static final ComponentMapper<HealthComponent> health = ComponentMapper.getFor(HealthComponent.class);
	
	public static final ComponentMapper<MapComponent> map = ComponentMapper.getFor(MapComponent.class);
}
