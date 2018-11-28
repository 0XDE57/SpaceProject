package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.utility.MyMath;

import java.util.Iterator;

public class AsteroidAnim extends TitleAnimation {

    Vector2 bullet = null;
    float bulletAngle = 0;
    Array<Asteroid> asteriods = new Array<Asteroid>();

    public AsteroidAnim() {
        asteriods.add(new Asteroid(200, 0, 0));
    }


    @Override
    public void render(float delta, ShapeRenderer shape) {

        if (bullet == null) {
            if (Gdx.input.isTouched()) {
                bullet = new Vector2(Gdx.graphics.getWidth()/2, 1);
                bulletAngle = MyMath.angleTo(new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight()-Gdx.input.getY()), bullet);
            }
        } else {
            if (bullet.x < 0 || bullet.y < 0 || bullet.x > Gdx.graphics.getWidth() || bullet.y > Gdx.graphics.getHeight()) {
                bullet = null;
            }
        }



        //shape.setColor(Color.WHITE);
        //shape.begin(ShapeRenderer.ShapeType.Filled);
        CustomShapeRenderer test = new CustomShapeRenderer(ShapeRenderer.ShapeType.Filled, shape.getRenderer());
        test.begin(ShapeRenderer.ShapeType.Filled);
        for (Asteroid a : asteriods) {
            a.renderBody(test);
        }
        test.end();
        //shape.end();

        shape.setColor(Color.BLACK);
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (Iterator<Asteroid> asteroidIterator = asteriods.iterator(); asteroidIterator.hasNext();) {
            Asteroid a = asteroidIterator.next();
            a.render(shape, delta);

            if (bullet != null) {
                if (a.hullPoly.contains(bullet)) {
                    if (a.hullPoly.getBoundingRectangle().area() > 40) {
                        asteriods.add(new Asteroid(a.size / 2, bulletAngle + (MathUtils.random(-45, 45) * MathUtils.degRad), 40));
                        asteriods.add(new Asteroid(a.size / 2, bulletAngle + (MathUtils.random(-45, 45) * MathUtils.degRad), 40));
                    }

                    bullet = null;
                    asteroidIterator.remove();
                }
            }
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        if (bullet != null) {
            bullet.add(MyMath.Vector(bulletAngle, 300 * delta));
            shape.circle(bullet.x, bullet.y, 10);
        }
        shape.end();
    }

    @Override
    public void resize(int width, int height) {

    }

    private class Asteroid {

        Vector2 position;
        float angle, velocity;
        Polygon hullPoly;
        int size;


        public Asteroid(int size, float angle, float velocity) {
            this.size = size;
            this.angle = angle;
            this.velocity = velocity;

            FloatArray points = new FloatArray();
            int numPoints = 20;
            for (int i = 0; i < numPoints*2; i+=2) {
                float x = MathUtils.random(size);
                float y = MathUtils.random(size);
                points.add(x);
                points.add(y);
            }

            ConvexHull convex = new ConvexHull();
            float[] hull = convex.computePolygon(points, false).toArray();
            hullPoly = new Polygon(hull);
            hullPoly.setOrigin(size/2,size/2);//should actually be center of mass//TODO: lookup center of mass for arbitrary poly

            position = new Vector2(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
        }

        public void render(ShapeRenderer shape, float delta) {
            //float angle = MyMath.angleTo(new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight()-Gdx.input.getY()), position);
            //position.add(MyMath.Vector(angle,10*delta));

            Rectangle bounds = hullPoly.getBoundingRectangle();
            if (bounds.y < 0) {
                //position.add(0, 2);
                angle -= 180 * MathUtils.degRad;
            } else if (bounds.y + bounds.height > Gdx.graphics.getHeight()) {
                //position.sub(0, 10);
                angle -= 180 * MathUtils.degRad;
            }
            if (bounds.x < 0) {
                //position.add(2, 0);
                angle -= 180 * MathUtils.degRad;
            } else if (bounds.x + bounds.width > Gdx.graphics.getWidth()) {
                //position.sub(2, 0);
                angle -= 180 * MathUtils.degRad;
            }

            position.add(MyMath.Vector(angle, velocity*delta));


            hullPoly.rotate(10*delta);
            hullPoly.setPosition(position.x, position.y);
            shape.polyline(hullPoly.getTransformedVertices());

            //shape.polygon(hullPoly.getBoundingRectangle());
        }

        public void renderBody(CustomShapeRenderer shape) {
            shape.fillPolygon(hullPoly.getTransformedVertices(),0, hullPoly.getVertices().length, Color.WHITE);
        }
    }

    private class CustomShapeRenderer extends ShapeRenderer {
        //stackoverflow.com/a/33076149
        EarClippingTriangulator ear = new EarClippingTriangulator();
        private ShapeType shapeType;
        private final ImmediateModeRenderer renderer;

        public CustomShapeRenderer(ShapeType shapeType, ImmediateModeRenderer renderer) {
            this.shapeType = shapeType;
            this.renderer = renderer;
        }


        public void fillPolygon(float[] vertices, int offset, int count, Color color)
        {
            if (shapeType != ShapeType.Filled && shapeType != ShapeType.Line)
                throw new GdxRuntimeException("Must call begin(ShapeType.Filled) or begin(ShapeType.Line)");
            if (count < 6)
                throw new IllegalArgumentException("Polygons must contain at least 3 points.");
            if (count % 2 != 0)
                throw new IllegalArgumentException("Polygons must have an even number of vertices.");

            //check(shapeType, null, count);

            final float firstX = vertices[0];
            final float firstY = vertices[1];
            if (shapeType == ShapeType.Line)
            {
                for (int i = offset, n = offset + count; i < n; i += 2)
                {
                    final float x1 = vertices[i];
                    final float y1 = vertices[i + 1];

                    final float x2;
                    final float y2;

                    if (i + 2 >= count)
                    {
                        x2 = firstX;
                        y2 = firstY;
                    } else
                    {
                        x2 = vertices[i + 2];
                        y2 = vertices[i + 3];
                    }

                    renderer.color(color);
                    renderer.vertex(x1, y1, 0);
                    renderer.color(color);
                    renderer.vertex(x2, y2, 0);

                }
            } else
            {
                ShortArray arrRes = ear.computeTriangles(vertices);

                for (int i = 0; i < arrRes.size - 2; i = i + 3)
                {
                    float x1 = vertices[arrRes.get(i) * 2];
                    float y1 = vertices[(arrRes.get(i) * 2) + 1];

                    float x2 = vertices[(arrRes.get(i + 1)) * 2];
                    float y2 = vertices[(arrRes.get(i + 1) * 2) + 1];

                    float x3 = vertices[arrRes.get(i + 2) * 2];
                    float y3 = vertices[(arrRes.get(i + 2) * 2) + 1];

                    this.triangle(x1, y1, x2, y2, x3, y3);
                }
            }
        }


        /** @param other May be null. *
        private void check (ShapeType preferred, ShapeType other, int newVertices) {
            if (shapeType == null) throw new IllegalStateException("begin must be called first.");

            if (shapeType != preferred && shapeType != other) {
                // Shape type is not valid.
                if (!autoShapeType) {
                    if (other == null)
                        throw new IllegalStateException("Must call begin(ShapeType." + preferred + ").");
                    else
                        throw new IllegalStateException("Must call begin(ShapeType." + preferred + ") or begin(ShapeType." + other + ").");
                }
                end();
                begin(preferred);
            } else if (matrixDirty) {
                // Matrix has been changed.
                ShapeType type = shapeType;
                end();
                begin(type);
            } else if (renderer.getMaxVertices() - renderer.getNumVertices() < newVertices) {
                // Not enough space.
                ShapeType type = shapeType;
                end();
                begin(type);
            }
        }
        */

    }
}
