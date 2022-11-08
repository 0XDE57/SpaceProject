package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.BloomEffect;
import com.spaceproject.components.StarComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class StarRenderSystem extends IteratingSystem implements Disposable {
    
    private final OrthographicCamera cam;
    private final SpriteBatch spriteBatch;
    private final ShaderProgram starShader;
    private final VfxManager vfxManager;
    private final BloomEffect bloomEffect;
    //private final LensFlareEffect flareEffect;
    //private final VfxFrameBuffer buffer;
    
    private float shift = 0;
    private final float shiftSpeed = 0.1f;
    
    private final Array<Entity> renderQueue = new Array<>();
    float intensity = 1;

    public StarRenderSystem() {
        super(Family.all(TextureComponent.class, TransformComponent.class, StarComponent.class).get());
        
        cam = GameScreen.cam;
        spriteBatch = new SpriteBatch();
        //buffer = new VfxFrameBuffer(Pixmap.Format.RGBA8888);
        //VfxFrameBuffer.Renderer batchRenderer = new VfxFrameBuffer.BatchRendererAdapter(spriteBatch);
        //buffer.addRenderer(batchRenderer);
        //buffer.initialize(1280, 720);
        
        // VfxManager is a host for the effects.
        // It captures rendering into internal off-screen buffer and applies a chain of defined effects.
        vfxManager = new VfxManager();
        vfxManager.setBlendingEnabled(true);
        bloomEffect = new BloomEffect(new BloomEffect.Settings(
                10,
                0.85f,
                1f,
                .85f,
                1.1f,
                .85f
        ));
        vfxManager.addEffect(bloomEffect);
        /*
        flareEffect = new LensFlareEffect();
        //flareEffect.setIntensity(10);
        //vfxManager.addEffect(flareEffect);
        GaussianBlurEffect blur = new GaussianBlurEffect();
        blur.setAmount(25);
        //vfxManager.addEffect(blur);
        RadialBlurEffect radial = new RadialBlurEffect(2);
        radial.setZoom(1.0f);
        radial.setStrength(1.0f);
        //vfxManager.addEffect(radial);
        */
        
        
        
        //load shaders
        ShaderProgram.pedantic = false;
        starShader = new ShaderProgram(Gdx.files.internal("shaders/starAnimate.vert"), Gdx.files.internal("shaders/starAnimate.frag"));
        if (starShader.isCompiled()) {
            spriteBatch.setShader(starShader);
            Gdx.app.log(this.getClass().getSimpleName(), "shader compiled successfully!");
        } else {
            Gdx.app.error(this.getClass().getSimpleName(), "shader failed to compile:\n" + starShader.getLog());
        }
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        renderQueue.add(entity);
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime); //adds entities to render queue
    
        // Clean up internal buffers, render to an off-screen buffer.
        //vfxManager.cleanUpBuffers();
        vfxManager.update(deltaTime);
        vfxManager.clear();
        vfxManager.beginCapture();
        //vfxManager.getPingPongWrapper().swap();
    
        //Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        //Gdx.gl.glClearColor(Color.CLEAR.r, Color.CLEAR.g, Color.CLEAR.b, Color.CLEAR.a);
        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        
        //shift shader
        shift += shiftSpeed * deltaTime;
        starShader.bind();
        starShader.setUniformf("u_shift", (float) Math.sin(shift));
        //DebugSystem.addDebugText(MyMath.round(shift, 2) + " shifted -> " + MyMath.round(Math.sin(shift), 2), 500, 500);
    
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            intensity++;
            System.out.println(intensity);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            intensity--;
            System.out.println(intensity);
        }
        bloomEffect.setBloomIntensity(intensity);
        
        //buffer.begin();
        //render
        //spriteBatch.setProjectionMatrix(cam.combined);
        //buffer.begin();
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        
        for (Entity entity : renderQueue) {
            /*
            StarComponent star = Mappers.star.get(entity);
            if (star != null) {
                starShader.setUniformf("u_colorTemp",
                        star.colorTemp[0],  // red
                        star.colorTemp[1],  // green
                        star.colorTemp[2]); // blue
            }
            */
            render(entity);
        }
        //where center screen is 0, 0?
        float x = Gdx.input.getX() - Gdx.graphics.getWidth()/2;
        //flareEffect.setLightPosition(1f, 1f);
        //buffer.end();
    
        
        
        spriteBatch.end();
        
    
        
        
        // End render to an off-screen buffer.
        vfxManager.endCapture();
        
        vfxManager.applyEffects();
        
        vfxManager.renderToScreen();
    }
    
    private void render(Entity entity) {
        TextureComponent tex = Mappers.texture.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        
        float width = tex.texture.getWidth();
        float height = tex.texture.getHeight();
        float originX = width * 0.5f; //center
        float originY = height * 0.5f; //center
        
        //draw texture
        spriteBatch.draw(tex.texture, (transform.pos.x - originX), (transform.pos.y - originY),
                originX, originY,
                width, height,
                tex.scale, tex.scale,
                MathUtils.radiansToDegrees * transform.rotation,
                0, 0, (int) width, (int) height, false, false);
    }
    
    @Override
    public void dispose() {
        spriteBatch.dispose();
        starShader.dispose();
        vfxManager.dispose();
        bloomEffect.dispose();
    }
    
}
