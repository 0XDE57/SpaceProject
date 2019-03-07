package com.spaceproject.screens.debug;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class TestSpiralGalaxy extends ScreenAdapter {
    ShapeRenderer shape = new ShapeRenderer();
    
    Array<Vector2> points = new Array<Vector2>();
    
    public TestSpiralGalaxy() {
        points = genPoints();
    }
    
    int[] fib = new int[]{1/*, 1, 2, 3, 5, 8, 13*/};//{ 1, 1, 2, 3, 5, 8, 13, 21 };
    
    public void render(float delta) {
        Gdx.gl20.glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            points = genPoints();
        }
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(1, 1, 1, 1);
        for (Vector2 p : points) {
            //shape.point(p.x, p.y, 0);
        }
        
        shape.setColor(Color.GREEN);
        int start = 1024 / 2;
        float scale = 1;
        int[] angles = new int[]{0, 90, 180, 270};
        //int[] offset = new int[] { 0, 0, 0, 0};
        int startX = start, startY = start;
        for (int i = 0; i < fib.length; i++) {
            int f = (int) (fib[i] * scale);
            int angle = angles[i % 4];
            switch (angle) {
                case 0:
                    startX -= f;
                    break;
                case 90:
                    startY -= f;
                    break;
                case 180:
                    startX += f;
                    break;
                case 270:
                    startY += f;
                    break;
            }
            //offset[i%4] += f;
            
            //shape.arc(start + f, start + f , f, angle, angle+90);
            //shape.rect(start, start, f, f);
            
            Vector2 p1 = new Vector2(startX, startY);
            Vector2 p2 = new Vector2(f, f);//.add(p1);
            //shape.rect(p1.x, p1.y, p2.x, p2.y);
            //System.out.println(i + ", " + fib[i] + ", " + f + ", " + angle + ", " + startX + ", " + startY);
        }
        //shape.arc(100, 100, 100, 0, 90);
        shape.setColor(Color.RED);
        
        
        //float c = 0.5519f;
        //(0,1),(c,1),(1,c),(1,0)
        //shape.curve(0, 1, c, 1, 1, c,1, 0, 10);
        shape.end();
        
        shape.setColor(Color.RED);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        /*
        //r = a + b*theta
        float a = 1, b = 0.5f;
        float theta = 0.0f;
        float r = a;
        int loops = 4;
        float step = 1f;

       // Vector2 prev_pos = polar_to_cartesian(r, theta);
        while (theta < 2 * loops * MathUtils.PI) {
            theta += step;
            r = a + b * theta;
            Vector2 pos = polarToCartesian(r, theta).add(200,200);

            pos.set((float)(a*Math.exp(b*theta)*Math.cos(theta)),(float)(a*Math.exp(b*theta)*Math.sin(theta)));

            shape.circle(pos.x, pos.y, 1);
            //prev_pos = pos;
            System.out.println(pos);
        }
        //shape.point(200, 200, 2);
        System.out.println();*/
        double degrees = 0.1 * MathUtils.radDeg;
        double center = 800 / 2;
        double end = 360 * 2 * 10 * degrees;
        double a = 0;
        double b = 20;
        double c = 1;
        
        for (double theta = 0; theta < end; theta += degrees) {
            double r = a + b * Math.pow(theta, 1 / c);
            double x = r * Math.cos(theta);
            double y = r * Math.sin(theta);
            shape.circle((int) (center + x), (int) (center - y), 1);
            System.out.println((int) x + ", " + (int) y);
        }
        shape.end();
    }
    
    private Vector2 polarToCartesian(float r, float theta) {
        return new Vector2((float) (r * Math.cos(theta)), (float) (r * Math.sin(theta)));
    }
    
    
    public Array<Vector2> genPoints() {
        //http://beltoforion.de/article.php?a=spiral_galaxy_renderer
        
        
        //create concentration hotspot in the center
        //two spirals 180 degrees from each other
        //drop random points, keep points weighted closer to the center and to the spiral's tendrils
        //skew horizontally and vertically
        
        int size = 1024;
        Vector2 hotSpot = new Vector2(size / 2, size / 2);
        Array<Vector2> p = new Array<Vector2>();
        p.add(new Vector2(0, 0));
        while (p.size < 2000) {
            Vector2 potentialPoint = new Vector2(MathUtils.random(size), MathUtils.random(size));
            //if (potentialPoint.)
            p.add(potentialPoint);
            
            
        }
        return p;
    }
    
}
