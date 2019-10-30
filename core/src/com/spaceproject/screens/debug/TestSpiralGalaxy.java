package com.spaceproject.screens.debug;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;
import com.spaceproject.utility.MyMath;

public class TestSpiralGalaxy extends MyScreenAdapter {
    //ShapeRenderer shape = new ShapeRenderer();
    
    Vector2 center;
    
    Array<Vector2> starPoints;
    Array<Vector2> edgePointsA = new Array<>();
    Array<Vector2> edgePointsB = new Array<>();
    
   
    float scale = 6;
    int iterations = 15;
    
    public TestSpiralGalaxy() {
        center = new Vector2(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
        generate();
    
    
        //test();
    
    }
    
    private void test() {
        int[] fib = new int[]{0, 1, 1, 2, 3, 5, 8, 13, 21};
        //how to get from this ^^^ to this vvv
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
    }
    
    private void generate() {
        int dir = MathUtils.random(4);
        edgePointsA = generateSpiral(iterations, dir);
        edgePointsB = generateSpiral(iterations, dir+2);//180 rotate
        
        starPoints = genStarPoints();
    }
    
    private Array<Vector2> generateSpiral(int iterations, int startAngle) {
        Array<Vector2> points = new Array<>(iterations);
        Array<Rectangle> rects = new Array<>(iterations);

        int[] angles = new int[]{0, 90, 180, 270};
        for (int s = 0; s < iterations; s++) {
            float fib = MyMath.fibonacci(s);
            int angle = angles[(s + startAngle) % 4];
            
            
            Vector2 newVec = MyMath.vector(angle * MathUtils.degRad, fib).cpy();
            if (s > 1) {
                Vector2 previous = points.get(s - 1);
                newVec.add(previous);
            }
            
            points.add(newVec);
        }
        
        //for (Vector2 p : points) { System.out.println(Misc.vecString(p,2)); }
        
        return points;
    }
    
    
    
    
    public void render(float delta) {
        Gdx.gl20.glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
    
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            MyScreenAdapter.game.setScreen(new TitleScreen(MyScreenAdapter.game));
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            generate();
        }
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        
        shape.setColor(1, 1, 0, 1);
        shape.line(center.x, 0, center.x, Gdx.graphics.getHeight());
        shape.line(0, center.y, Gdx.graphics.getWidth(), center.y);
    
        
        drawSpiral(edgePointsA, center, Color.RED);
        drawSpiral(edgePointsB, center, Color.YELLOW);

        
        shape.setColor(1, 0, 0, 1);
        shape.circle(center.x, center.y, 3);
    
    
        drawStars();
    
    }
    
    private void drawSpiral(Array<Vector2> points, Vector2 center, Color color) {
        shape.setColor(color);
        
        int c = 0;
        for (Vector2 p : points) {
            Vector2 v1 = p.cpy().scl(scale).add(center);
            if (c > 0) {
                Vector2 v2 = points.get(c-1).cpy().scl(scale).add(center);
                shape.line(v1, v2);
            }
            shape.circle(v1.x, v1.y, 3);
            
            c++;
        }
    }
    
    private void drawStars() {
        shape.setColor(1, 1, 1, 1);
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
        int numGen = 1000;
        Vector2 hotSpot = new Vector2(size / 2, size / 2);
        Array<Vector2> p = new Array<>();
        //p.add(new Vector2(0, 0));
        //p.add(hotSpot);
        while (p.size < numGen) {
            Vector2 potentialPoint = new Vector2(MathUtils.random(size), MathUtils.random(size));
            float chance = 1-(potentialPoint.dst(hotSpot) / size);
            if (MathUtils.random(1.0f) < chance) {
                p.add(potentialPoint);
            }
            
        }
        //p.add(new Vector2(0, size));
       // p.add(new Vector2(size, 0));
        return p;
    }
    
}
