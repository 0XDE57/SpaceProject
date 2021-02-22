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
import com.spaceproject.ui.debug.DebugTextRenderer;
import com.spaceproject.utility.Misc;
import com.spaceproject.math.MyMath;

public class TestSpiralGalaxy extends MyScreenAdapter {
    enum Direction {
        UP(90), RIGHT(0), DOWN(270), LEFT(180);
        private int dir;
        Direction(int dir) {
            this.dir = dir;
        }
    }
    
    enum Origin {
        BL,
        TL,
        TR,
        BR
    }
    
    private class SpiralArm {
        
        public Array<Vector2> points;
        public Array<Vector2> approxPoints;
        public Array<Rectangle> rects;
        
        public SpiralArm(int iterations, int startAngle, boolean clockwise) {
            iterations = Math.abs(iterations);//keep positive
            
            points = new Array<>(iterations);
            approxPoints = new Array<>(iterations);
            rects = new Array<>(iterations);
            
            System.out.format("[FIB] iteration %s, start dir %s, clockwise %s \n", iterations, startAngle, clockwise);
            String format = "| %1$-5s | %2$-5s | %3$-15s | %4$-7s | %5$-7s | %6$-10s | %7$-10s\n";
            System.out.format(format, "iter", "fib", "coord", "len", "angle", "dir", "rect");
    
            //0 = right, 90 = up, 180 = left, 270 = down
            int[] angles = clockwise ? new int[]{ 270, 180, 90, 0 } : new int[]{ 0, 90, 180, 270 };
            byte[] dirr = new byte[] {0, 1, 2, 3};
            
            for (int iter = 0; iter < iterations; iter++) {
                long fib = MyMath.fibonacci(iter);
                int direction = angles[(iter + startAngle) % 4];
                
                //if iter = 1, handle first fib 1 odd case?
                
                Vector2 newVec = MyMath.vector(direction * MathUtils.degRad, fib).cpy();
                points.add(newVec);//edge
                
                //calc rectangles and spiral approximation
                Vector2 approxP = null;
                Rectangle newRect = null;// = new Rectangle(newVec.x, newVec.y, fib, fib);
                
                if (iter > 0) {
                    Vector2 previous = points.get(iter - 1);
                    newVec.add(previous);
                    
                    long prevFib = MyMath.fibonacci(iter-1);
                    int originX = (int)newVec.x;
                    int originY = (int)newVec.y;
                    switch (direction) {
                        case 0:
                            //right
                            //originX -= fib;
                            break;
                        case 90: break;
                        case 180: break;
                        case 270: break;
                    }
    
                    
                    newRect = new Rectangle(originX, originY, prevFib, prevFib);
                    if (iter != 1) {
                        rects.add(newRect);
                    }
    
                    //approxP = new Vector2(originX, originY);
                    //approxPoints.add(approxP);
                }
            
                
            
                String dir = "";
                switch (direction) {
                    case 0:   dir = ">"; break;
                    case 90:  dir = "^"; break;
                    case 180: dir = "<"; break;
                    case 270: dir = "v"; break;
                }
                System.out.format(format,
                        iter,
                        fib,
                        Misc.vecString(newVec, 2),
                        MyMath.round(newVec.len(), 2),
                        MyMath.round(newVec.angle(), 2),
                        "(" + dir + ") "+ direction,
                        newRect == null ? "null" : newRect.toString());
            }
        }
    }
    
    private Vector2 center;
    private Array<Vector2> starPoints;
    private SpiralArm spiralA, spiralB;
    
    private float scale = 60;
    private int iterations = 5;
    int dir;
    boolean clockwise;
    
    private DebugTextRenderer debugText = new DebugTextRenderer();
    
    public TestSpiralGalaxy() {
        center = new Vector2(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
        generate(1, true);
        
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
    
    private void generateRand() {
        generate(MathUtils.random(4), MathUtils.randomBoolean());
    }
    
    private void generate(int direction, boolean clockwise) {
        this.dir = direction;
        if (dir < 0) {
            dir += 4;
        }
        this.clockwise = clockwise;
        spiralA = new SpiralArm(iterations, dir, clockwise);
        //spiralB = new SpiralArm(iterations, dir+2, clockwise);//180 rotation
        starPoints = genStarPoints();
    }
    
    
    public void render(float delta) {
        Gdx.gl20.glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
    
        checkInput();
    
        shape.begin(ShapeRenderer.ShapeType.Line);
        
        shape.setColor(Color.DARK_GRAY);
        shape.line(center.x, 0, center.x, Gdx.graphics.getHeight());
        shape.line(0, center.y, Gdx.graphics.getWidth(), center.y);
        
        
        
        //drawSpiral(spiralB.points, center, scale, Color.YELLOW);
        
        drawRects(spiralA.rects, center, scale, Color.GREEN);
    
        shape.setColor(0, 1, 0, 1);
        long fibTest = MyMath.fibonacci(3);
        Rectangle test = new Rectangle(1, 1, fibTest, fibTest);
        Vector2 v1 = new Vector2(test.x, test.y).scl(scale).add(center);
        //shape.rect(v1.x, v1.y, test.width * scale, test.height * scale);
        
        
        drawSpiral(spiralA.points, center, scale, Color.RED);
        
        //shape.setColor(1, 0, 0, 1);
        //shape.circle(center.x, center.y, 3);
        
        shape.setColor(Color.DARK_GRAY);
        shape.line(center, new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY()));
        
        //drawStars();
        
        shape.end();
        
        showMousePos(center, scale);
        
        debugText.draw(batch);
        
    }
    
    private void checkInput() {
        //fullscreen toggle
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            toggleFullscreen();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            MyScreenAdapter.game.setScreen(new TitleScreen(MyScreenAdapter.game));
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            //generateRand();
            clockwise = !clockwise;
            generate(dir, clockwise);
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            iterations++;
            generate(dir, clockwise);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            iterations--;
            generate(dir, clockwise);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            generate(dir-1, clockwise);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            generate(dir+1, clockwise);
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            scale++;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            scale--;
            if (scale == 0) {
                scale = 1;
            }
        }
    }
    
    private void drawRects(Array<Rectangle> rects, Vector2 center, float scale, Color color) {
        shape.setColor(color);
        
        Vector2 v1 = new Vector2();
        for (Rectangle r : rects) {
            v1.set(r.x, r.y).scl(scale).add(center);
            shape.rect(v1.x, v1.y, r.width * scale, r.height * scale);
        }
    }
    
    private void drawSpiral(Array<Vector2> points, Vector2 center, float scale, Color color) {
        shape.setColor(color);
        
        int c = 0;
        for (Vector2 p : points) {
            Vector2 v1 = p.cpy().scl(scale).add(center);
            debugText.add(Misc.vecString(p, 1), v1.x, v1.y);
            debugText.add(Misc.vecString(v1, 1), v1.x, v1.y + debugText.getFontHeight());
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
    }
    
    private void showMousePos(Vector2 center, float scale) {
        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        String localPos = x + "," + y;
        //debugText.add(new Vector2(x, y).scl(scale).add(center).toString(), x,  y);
        
        //Vector3 worldPos = cam.unproject(new Vector3(x, y, 0));
        //debugText.add((int) worldPos.x + "," + (int) worldPos.y, x, y + (int) debugText.getFontHeight());
    
        //float angle = MyMath.angle2(center, new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY()));
        //debugText.add(MyMath.round(angle,3) + " / " + MyMath.round(angle * MathUtils.radDeg, 3), x, y + (int) debugText.getFontHeight()*2);
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
