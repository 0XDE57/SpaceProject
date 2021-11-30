package com.spaceproject.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.spaceproject.math.Physics;

public class ShapeRenderActor extends Actor {
    private ShapeRenderer shape;
    
    public ShapeRenderActor() {
        shape = new ShapeRenderer();
    }
    
    @Override
    public void draw(Batch batch, float parentAlpha) {
        //POC for custom drawing on actors using shaperender. normally you are limited to the batch drawing
        //TODO: bug, this breaks resizing of the menu when the tab is visible
        //TODO: also coordinates seem shifted left a bit, looks like padding
        //Note: you must take care to respect the parent alpha
        //1. break out of batch by overriding and ignoring super's draw
        //super.draw(batch, parentAlpha); //<--don't call
        //2. end the current batch
        batch.end();
        
        //3. do our custom shape rendering
        shape.setProjectionMatrix(batch.getProjectionMatrix());
        shape.begin(ShapeRenderer.ShapeType.Filled);
        //setBounds(getParent().getX(), getParent().getY(), getParent().getWidth(), getParent().getWidth());
        
        shape.setColor(Color.GREEN);
        Vector2 coords = new Vector2(getX(), getY());
        localToStageCoordinates(coords);
        shape.circle(coords.x + getWidth() / 2, coords.y + getHeight() / 2, 10);
        
        shape.circle(coords.x, coords.y, 10);
        shape.circle(coords.x, coords.y + getHeight(), 10);
        shape.circle(coords.x + getWidth(), coords.y, 10);
        shape.circle(coords.x + getWidth(), coords.y + getHeight(), 10);
    
        //shape.setColor(Color.WHITE);
        //shape.rect(coords.x + 50, coords.y + 50, 50, 50);
        //debug black body radiation, color temperature
        int x = 20;
        int y = 100;
        int width = (int) getWidth() - x;
        int height = 100;
        float lowestVisibleWavelength = (float) Physics.wavelengthToTemperature(380);
        float highestVisibleWavelength = (float) Physics.wavelengthToTemperature(780);
        for (int i = 0; i < width; i++) {
            float percent = (float) i / width;
            int border = 4;
            shape.setColor(
                    percent, // red
                    percent, // green
                    percent,  // blue
                    1); //alpha
            shape.line(x + i, y - border, x + i, y + height + border);
            
            double temperature = MathUtils.lerp(1000, 50000, percent);
            //double temperature = MathUtils.lerp(lowestVisibleWavelength, highestVisibleWavelength, percent);
            double wavelength = Physics.temperatureToWavelength(temperature) * 1000000;
            //wavelength = MathUtils.lerp(lowestVisibleWavelength, highestVisibleWavelength, percent) * 1000000;
            
            int[] colorTemp = Physics.wavelengthToRGB(wavelength);
            shape.setColor(
                    colorTemp[0] / 255.0f, // red
                    colorTemp[1] / 255.0f, // green
                    colorTemp[2] / 255.0f,  // blue
                    1); //alpha
            shape.line(x + i, y, x + i, y + height);
            
            //shape.
        }
        
        
        
        //4. end our shape
        shape.end();
        //5. we must begin the batch again for actors that are drawn after us
        batch.begin();
    }
}
