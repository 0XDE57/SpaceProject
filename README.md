attempt at adding bloom shader USING
https://github.com/crashinvaders/gdx-vfx/wiki/VFX-Frame-Buffer


bloom giving render artifacts with blending on: vfxManager.setBlendingEnabled(true);
https://github.com/crashinvaders/gdx-vfx/issues/14


frame buffer:
https://github.com/crykn/libgdx-screenmanager/wiki/Custom-FrameBuffer-implementation#the-problem 


```
public void update(float deltaTime) {
        super.update(deltaTime); //adds entities to render queue
        
        //shift shader
        shift += shiftSpeed * deltaTime;
        starShader.bind();
        starShader.setUniformf("u_shift", (float) Math.sin(shift));
       
    
        // Clean up internal buffers, render to an off-screen buffer.
        //Gdx.gl20.glClearColor(0, 0, 0, 1); //wipes out background layer and doesn't solve problem
        //Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        vfxManager.cleanUpBuffers();
        vfxManager.beginInputCapture();
        
        //render
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        for (Entity entity : renderQueue) {

            render(entity);
        }
        spriteBatch.end();
    
        // End render to an off-screen buffer.
        vfxManager.endInputCapture();
        vfxManager.applyEffects();
        vfxManager.renderToScreen();
    }
```
