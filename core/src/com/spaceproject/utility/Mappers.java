package com.spaceproject.utility;

import com.badlogic.ashley.core.ComponentMapper;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AISpawnComponent;
import com.spaceproject.components.AstronomicalComponent;
import com.spaceproject.components.AttachedToComponent;
import com.spaceproject.components.BarrelRollComponent;
import com.spaceproject.components.BarycenterComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.DashComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.ShaderComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;

public class Mappers {
    
    public static final ComponentMapper<AIComponent> AI = ComponentMapper.getFor(AIComponent.class);
    public static final ComponentMapper<AISpawnComponent> spawn = ComponentMapper.getFor(AISpawnComponent.class);
    public static final ComponentMapper<AstronomicalComponent> astro = ComponentMapper.getFor(AstronomicalComponent.class);
    public static final ComponentMapper<AttachedToComponent> attachedTo = ComponentMapper.getFor(AttachedToComponent.class);
    public static final ComponentMapper<BarrelRollComponent> barrelRoll = ComponentMapper.getFor(BarrelRollComponent.class);
    public static final ComponentMapper<BarycenterComponent> barycenter = ComponentMapper.getFor(BarycenterComponent.class);
    public static final ComponentMapper<CameraFocusComponent> camFocus = ComponentMapper.getFor(CameraFocusComponent.class);
    public static final ComponentMapper<CannonComponent> cannon = ComponentMapper.getFor(CannonComponent.class);
    public static final ComponentMapper<CharacterComponent> character = ComponentMapper.getFor(CharacterComponent.class);
    public static final ComponentMapper<ChargeCannonComponent> chargeCannon = ComponentMapper.getFor(ChargeCannonComponent.class);
    public static final ComponentMapper<ControlFocusComponent> controlFocus = ComponentMapper.getFor(ControlFocusComponent.class);
    public static final ComponentMapper<ControllableComponent> controllable = ComponentMapper.getFor(ControllableComponent.class);
    public static final ComponentMapper<DamageComponent> damage = ComponentMapper.getFor(DamageComponent.class);
    public static final ComponentMapper<DashComponent> dash = ComponentMapper.getFor(DashComponent.class);
    public static final ComponentMapper<ExpireComponent> expire = ComponentMapper.getFor(ExpireComponent.class);
    public static final ComponentMapper<HealthComponent> health = ComponentMapper.getFor(HealthComponent.class);
    public static final ComponentMapper<HyperDriveComponent> hyper = ComponentMapper.getFor(HyperDriveComponent.class);
    public static final ComponentMapper<MapComponent> map = ComponentMapper.getFor(MapComponent.class);
    public static final ComponentMapper<OrbitComponent> orbit = ComponentMapper.getFor(OrbitComponent.class);
    public static final ComponentMapper<ParticleComponent> particle = ComponentMapper.getFor(ParticleComponent.class);
    public static final ComponentMapper<PhysicsComponent> physics = ComponentMapper.getFor(PhysicsComponent.class);
    public static final ComponentMapper<PlanetComponent> planet = ComponentMapper.getFor(PlanetComponent.class);
    public static final ComponentMapper<ScreenTransitionComponent> screenTrans = ComponentMapper.getFor(ScreenTransitionComponent.class);
    public static final ComponentMapper<SeedComponent> seed = ComponentMapper.getFor(SeedComponent.class);
    public static final ComponentMapper<ShieldComponent> shield = ComponentMapper.getFor(ShieldComponent.class);
    public static final ComponentMapper<ShaderComponent> shader = ComponentMapper.getFor(ShaderComponent.class);
    public static final ComponentMapper<Sprite3DComponent> sprite3D = ComponentMapper.getFor(Sprite3DComponent.class);
    public static final ComponentMapper<TextureComponent> texture = ComponentMapper.getFor(TextureComponent.class);
    public static final ComponentMapper<TransformComponent> transform = ComponentMapper.getFor(TransformComponent.class);
    public static final ComponentMapper<VehicleComponent> vehicle = ComponentMapper.getFor(VehicleComponent.class);
    
}
