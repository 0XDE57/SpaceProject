package com.spaceproject.screens.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;


public class Test3DScreen extends ScreenAdapter {

    CameraInputController camController;
    PerspectiveCamera cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    //OrthographicCamera cam = new OrthographicCamera();
    SpriteBatch batch = new SpriteBatch();
    ModelBatch modelBatch = new ModelBatch();

    //Entity test = EntityFactory.createPlanet(0, new Entity(),0, 0, 0, 0,false);
    Texture t = TextureFactory.generateCharacter();
    Texture tile = TextureFactory.createTile(Color.MAGENTA);
    Texture shipTex;
    Texture test = TextureFactory.createTestTile();
    Thing ship3d;
    Sprite front, back;

    Texture testtex;

    public Model model;
    public ModelInstance instance;


    DecalBatch decalBatch;
    Decal shipDecalA, shipDecalB, shipDecalC, shipDecalD;
    public Test3DScreen() {
        cam.position.set(0, 0, 100/*350*/);
        cam.lookAt(0, 0, 0);
        //cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 1f;
        cam.far = 400f;
        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);

        shipTex = TextureFactory.generateShip(123, 20);


        /*
        Pixmap shipMap = shipTex.getTextureData().consumePixmap();
        for (int x = 0; x < shipMap.getWidth(); x++) {
            for (int y = 0; y < shipMap.getHeight(); y++) {
                int pixel = shipMap.getPixel(x, y);
                System.out.print(pixel+" ");
            }
            System.out.println("");
        }
        System.out.println("");*/

        Texture shipTex2 = TextureFactory.generateShip2(123, 20);

        int width = shipTex.getWidth(), height = shipTex.getHeight();
        Pixmap pixmap = new Pixmap(width, height*2, Pixmap.Format.RGBA8888);

        /*
        for (int y = 0; y < shipTex.getHeight(); y++) {
            for (int x = 0; x < shipTex.getWidth(); x++) {
                int pixel = shipMap.getPixel(x, y);
                System.out.print(pixel+" ");
                //pixmap.drawPixel(x, y, pixel);
            }
            System.out.println("");
        }
        */

        pixmap.drawPixmap(shipTex.getTextureData().consumePixmap(), 0, 0);
        pixmap.drawPixmap(shipTex2.getTextureData().consumePixmap(),0, height);
        //shipTex.draw(pixmap, 0, 0);
        //shipTex2.draw(pixmap, 0, height);


        /*
        for (int y = 0; y < shipTex.getHeight(); y++) {
            for (int x = 0; x < shipTex.getWidth(); x++) {
                //pixmap.drawPixel(x, y, shipTex.getTextureData().consumePixmap().getPixel(x, y));
            }
        }*/

        /*
        */
        testtex = new Texture(pixmap);
        TextureAtlas ttatlas = new TextureAtlas();
        ttatlas.addRegion("front", new TextureRegion(testtex,0,0, width, height));
        ttatlas.addRegion("back", new TextureRegion(testtex, 0, height, width, height));

        //Sprite backSprite = new Sprite(shipTex2);

        //TextureAtlas testAtlas = new TextureAtlas();
        //testAtlas.addRegion("front", shipTex, 0, 0, shipTex.getWidth(), shipTex.getHeight());
        //testAtlas.addRegion("back", shipTex2, 0, 0, shipTex2.getWidth(), shipTex2.getHeight());
        Sprite a = ttatlas.createSprite("front");
        Sprite b = ttatlas.createSprite("back");

        //TextureAtlas atlas = new TextureAtlas("data/carddeck.atlas");
        front = a;//atlas.createSprite("clubs");
        back = b;//atlas.createSprite("back");

        ship3d = new Thing(front, back);

        ModelBuilder modelBuilder = new ModelBuilder();
        /*Material testmat = new Material(
                TextureAttribute.createDiffuse(shipTex),
                new BlendingAttribute(false, 1f),
                FloatAttribute.createAlphaTest(0.5f)
        );*/
        model = modelBuilder.createBox(10f, 10f, 10f,
                new Material(TextureAttribute.createDiffuse(test)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        instance = new ModelInstance(model);


        decalBatch = new DecalBatch(new CameraGroupStrategy(cam));
        shipDecalA = Decal.newDecal(new TextureRegion(shipTex));
        shipDecalB = Decal.newDecal(new TextureRegion(shipTex));
        shipDecalC = Decal.newDecal(new TextureRegion(shipTex));
        shipDecalD = Decal.newDecal(new TextureRegion(shipTex));

        t = testtex;
    }

    float rotX = 0, rotY = 0, rotZ = 0;
    @Override
    public void render(float delta) {
        //Gdx.gl20.glClearColor(0,0,0,0);
        //Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glClear(Gdx.gl20.GL_COLOR_BUFFER_BIT | Gdx.gl20.GL_DEPTH_BUFFER_BIT);

        camController.update();
        cam.update();
        //System.out.println(cam.position + "-" + cam.direction);
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            cam.position.x += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            cam.position.x -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            cam.position.y += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            cam.position.y -= 1f;
        }



        //Texture t = test.getComponent(TextureComponent.class).texture;
        //Vector3 pos = test.getComponent(TransformComponent.class).pos;
        batch.setProjectionMatrix(cam.combined);
        //batch.setTransformMatrix(cam.combined);
        batch.begin();
        batch.draw(testtex, -100,-100,50,50);
        batch.draw(t, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);




        float width = t.getWidth();
        float height = t.getHeight();
        float originX = width * 0.5f; //center
        float originY = height * 0.5f; //center
        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight()-Gdx.input.getY();
        float scale = SpaceProject.entitycfg.renderScale;

        //draw texture
        batch.draw(t, (x - originX), (y - originY),
                originX, originY,
                width, height,
                scale, scale,
                0,
                0, 0, (int)width, (int)height, false, false);


        batch.end();

        instance.transform.rotate(Vector3.Y, 90 * delta);
        //ship3d.worldTransform.translate(0, 0, 0);
        modelBatch.begin(cam);
        //modelBatch.render(instance);
        modelBatch.render(ship3d);
        modelBatch.end();

        ship3d.worldTransform.rotate(Vector3.X, 90 * delta);
        //ship3d.worldTransform.rotate(Vector3.Y, 60 * delta);
        //ship3d.worldTransform.rotate(Vector3.Z, 90 * delta);

        /*
        shipDecalA.setPosition(0, 5, 0);
        //shipDecalA.rotateX(shipDecalA.getX() + 1f);
        //shipDecalA.rotateY(shipDecalA.getY() + 0.2f);
        //shipDecalA.rotateZ(shipDecalA.getZ() + 1f);

        shipDecalA.setRotationX(0);
        shipDecalA.setRotationY(0);
        shipDecalA.setRotationZ(0);
        shipDecalA.rotateZ(MyMath.angleTo(
                (int)shipDecalA.getPosition().x,
                (int)shipDecalA.getPosition().y,
                Gdx.input.getX(),
                Gdx.graphics.getHeight()-Gdx.input.getY())* MathUtils.radDeg
        );

        //shipDecalA.rotateY(rotY+=1f);
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            rotX+=10f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            rotX-=10f;
        }
        shipDecalA.rotateX(rotX);
        //shipDecalA.rotateX(rotX+=30f);


        decalBatch.add(shipDecalA);

        shipDecalB.setPosition(30, 5, 0);
        shipDecalB.rotateX(shipDecalB.getX() + 1f);
        decalBatch.add(shipDecalB);

        shipDecalC.setPosition(60, 5, 0);
        shipDecalC.rotateY(shipDecalC.getY() + 1f);
        decalBatch.add(shipDecalC);

        shipDecalD.setPosition(90, 5, 0);
        shipDecalD.rotateZ(shipDecalD.getZ() + 1f);
        decalBatch.add(shipDecalD);
        decalBatch.flush();
        //cam.update();
        */

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            MyScreenAdapter.game.setScreen(new TitleScreen(MyScreenAdapter.game));
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void hide() {
        super.hide();
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        batch.dispose();
    }
}


//xoppa.github.io/blog/a-simple-card-game/
class Thing extends Renderable {

    public Thing(Sprite front, Sprite back) {

        material = new Material(
                TextureAttribute.createDiffuse(front.getTexture()),
                new BlendingAttribute(false, 1f)
                //,FloatAttribute.createAlphaTest(0.5f)
        );

        float scale = 10;
        float height = scale * front.getHeight()/front.getWidth();

        front.setSize(scale, height);
        back.setSize(scale, height);

        front.setPosition(-front.getWidth() * 0.5f, -front.getHeight() * 0.5f);
        back.setPosition(-back.getWidth() * 0.5f, -back.getHeight() * 0.5f);

        float[] vertices = convert(front.getVertices(), back.getVertices());
        short[] indices = new short[] { 0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4 };

        // FIXME: this Mesh needs to be disposed
        meshPart.mesh = new Mesh(true, 8, 12, VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
        meshPart.mesh.setVertices(vertices);
        meshPart.mesh.setIndices(indices);
        meshPart.offset = 0;
        meshPart.size = meshPart.mesh.getNumIndices();
        meshPart.primitiveType = GL20.GL_TRIANGLES;
        meshPart.update();
    }

    private static float[] convert(float[] front, float[] back) {
        return new float[] {
                front[Batch.X2], front[Batch.Y2], 0, 0, 0, 1, front[Batch.U2], front[Batch.V2],
                front[Batch.X1], front[Batch.Y1], 0, 0, 0, 1, front[Batch.U1], front[Batch.V1],
                front[Batch.X4], front[Batch.Y4], 0, 0, 0, 1, front[Batch.U4], front[Batch.V4],
                front[Batch.X3], front[Batch.Y3], 0, 0, 0, 1, front[Batch.U3], front[Batch.V3],

                back[Batch.X1], back[Batch.Y1], 0, 0, 0, -1, back[Batch.U1], back[Batch.V1],
                back[Batch.X2], back[Batch.Y2], 0, 0, 0, -1, back[Batch.U2], back[Batch.V2],
                back[Batch.X3], back[Batch.Y3], 0, 0, 0, -1, back[Batch.U3], back[Batch.V3],
                back[Batch.X4], back[Batch.Y4], 0, 0, 0, -1, back[Batch.U4], back[Batch.V4]
        };
    }



}
