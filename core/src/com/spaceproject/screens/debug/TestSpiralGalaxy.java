package com.spaceproject.screens.debug;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyMath;

public class TestSpiralGalaxy extends ScreenAdapter {
    ShapeRenderer shape = new ShapeRenderer();
    
    Array<Vector2> starPoints;
    Array<Vector2> edgePoints = new Array<>();
    
    //int[] fib;//= new int[]{0, 1, 1, 2, 3, 5, 8, 13, 21};//{ 1, 1, 2, 3, 5, 8, 13, 21 };
    
    public TestSpiralGalaxy() {
        starPoints = genStarPoints();
    
        int[][] test = {
                {0, 0},
                {-1, -1},
                {-2, 0},
                {0, 2},
                {3, -1},
                {-6, -2},
                {-10, 2},
                {3, 15},
                {-6, 24},
                {-10, -40}
        };
        Vector2 vector2 = new Vector2(0, 0);
        for (int i = 0; i < test.length; i++) {
            int x = test[i][0];
            int y = test[i][1];
            vector2.set(x, y);
            System.out.println(x + ", " + y + ": " + MyMath.round(vector2.len(), 2) + ": " + MyMath.round(vector2.angle(),2));
        }
    
        generateSpiral(9, 0);
        center = new Vector2(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
    }
    
    public static long fibonacci(int n) {
        if (n == 0) {
            return 0;
        }
        if (n <= 2) {
            return 1;
        }
        
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
    
    private void generateSpiral(int iterations, int startAngle) {
        
        edgePoints.clear();
        int[] angles = new int[]{0, 90, 180, 270};
        for (int s = 0; s < iterations; s++) {
            float fib = fibonacci(s);
            int angle = angles[(s + startAngle) % 4];
            
            
            Vector2 newVec = MyMath.vector(angle * MathUtils.degRad, fib).cpy();
            if (s > 1) {
                Vector2 previous = edgePoints.get(s - 1);
                newVec.add(previous);
            }
            edgePoints.add(newVec);
        }
        
        for (Vector2 p : edgePoints) {
            System.out.println(Misc.vecString(p,2));
        }
    }
    
    Vector2 center;
    
    
    public void render(float delta) {
        Gdx.gl20.glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            //points = genPoints();
            generateSpiral(15, MathUtils.random(4));
            
            starPoints.clear();
            starPoints = genStarPoints();
        }
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        
        shape.setColor(1, 1, 0, 1);
        shape.line(center.x, 0, center.x, Gdx.graphics.getHeight());
        shape.line(0, center.y, Gdx.graphics.getWidth(), center.y);
        
        
        shape.setColor(1, 1, 1, 1);
        
        float scale = 6;
        Vector2 prev = new Vector2();
        for (Vector2 p : edgePoints) {
            Vector2 v1 = p.cpy().scl(scale).add(center);
            Vector2 v2 = prev.cpy().scl(scale).add(center);
            shape.line(v1, v2);
            shape.circle(v1.x, v1.y, 3);
            prev.set(p);
        }
        
        
        shape.setColor(1, 0, 0, 1);
        shape.circle(center.x, center.y, 3);
    
        
        shape.setColor(0, 1, 1, 1);
        int centerCluster = 512;
        for (Vector2 p : starPoints) {
            Vector2 r = p.cpy().add(center).sub(centerCluster, centerCluster);
            shape.circle(r.x, r.y, 3);
        }
        shape.end();

    }
    
    
    public Array<Vector2> genStarPoints() {
        //http://beltoforion.de/article.php?a=spiral_galaxy_renderer
        
        
        //create concentration hotspot in the center
        //two spirals 180 degrees from each other
        //drop random points, keep points weighted closer to the center and to the spiral's tendrils
        //skew horizontally and vertically
        
        int size = 1024;
        int numGen = 2000;
        Vector2 hotSpot = new Vector2(size / 2, size / 2);
        Array<Vector2> p = new Array<>();
        //p.add(new Vector2(0, 0));
        //p.add(hotSpot);
        while (p.size < numGen) {
            Vector2 potentialPoint = new Vector2(MathUtils.random(size), MathUtils.random(size));
            float chance = (potentialPoint.dst(hotSpot) / size * 0.5f);
            if (MathUtils.random(1.0f) < chance) {
                p.add(potentialPoint);
            }
            
        }
        //p.add(new Vector2(0, size));
       // p.add(new Vector2(size, 0));
        return p;
    }
    
}
