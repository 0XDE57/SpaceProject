package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.screens.animations.DelaunayAnim;
import com.spaceproject.screens.animations.NBodyGravityAnim;
import com.spaceproject.screens.animations.NoiseAnim;
import com.spaceproject.screens.animations.OrbitAnim;
import com.spaceproject.screens.animations.TitleAnimation;
import com.spaceproject.screens.animations.TreeAnim;
import com.spaceproject.ui.menu.TitleScreenMenu;

public class TitleScreen extends MyScreenAdapter {
    
    private SpaceProject game;
    private Stage stage;
    private Table versionTable;
    private Label titleLabel;
    private int edgePad;
    private Matrix4 projectionMatrix = new Matrix4();
    private TitleAnimation foregroundAnimation, backgroundAnimation;
    private ForegroundAnimation previousAnim;
    
    enum ForegroundAnimation {
        tree, delaunay, orbit, crossNoise, nbody;/*, asteroid*/;
        
        public static ForegroundAnimation random() {
            return ForegroundAnimation.values()[MathUtils.random(ForegroundAnimation.values().length - 1)];
        }
        
        public static ForegroundAnimation next(ForegroundAnimation e) {
            int index = (e.ordinal() + 1) % ForegroundAnimation.values().length;
            return ForegroundAnimation.values()[index];
        }
    }
    
    public TitleScreen(SpaceProject spaceProject) {
        this.game = spaceProject;
        
        //init scene2d and VisUI
        if (VisUI.isLoaded())
            VisUI.dispose(true);
        VisUI.load(VisUI.SkinScale.X2);
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        
        
        //init fonts
        String titleFont = "titleFontLarge";
        initTitleFont(titleFont);
        String menuFont = "menuFont";
        initMenuFont(menuFont);
        
        titleLabel = new Label(SpaceProject.TITLE, VisUI.getSkin(), titleFont, Color.WHITE);
        titleLabel.setPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight(), Align.top);
        stage.addActor(titleLabel);
        
        
        //menu
        Table menuTable = TitleScreenMenu.buildMenu(game, stage, false);
        stage.addActor(menuTable);
        menuTable.pack();
        edgePad = SpaceProject.isMobile() ? 20 : 10;
        menuTable.setPosition(edgePad, edgePad);
        
        
        //version note
        versionTable = new Table();
        versionTable.add(new Label(SpaceProject.VERSION, VisUI.getSkin(), menuFont, Color.WHITE));
        stage.addActor(versionTable);
        versionTable.pack();
        
        
        //init animations
        backgroundAnimation = new NoiseAnim();
        initForegroundAnim();
        
        
        Gdx.graphics.setVSync(true);
    }
    
    public void render(float delta) {
        super.render(delta);
        
        Gdx.gl20.glClearColor(1, 1, 1, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        
        cam.position.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0);
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        batch.setProjectionMatrix(projectionMatrix);
        
        
        backgroundAnimation.render(delta, shape);
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        foregroundAnimation.render(delta, shape);
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
        
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            initForegroundAnim();
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            Table table = TitleScreenMenu.buildMenu(game, stage, true);
            table.pack();
            table.setPosition(edgePad, edgePad);
            stage.clear();
            stage.addActor(table);
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (!exitPromptUp) {
                exitPromptUp = true;
                Dialogs.showOptionDialog(stage, "exit", "goodbye?", Dialogs.OptionDialogType.YES_NO, new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        Gdx.app.exit();
                    }
                    @Override
                    public void no () {
                        exitPromptUp = false;
                    }
                    @Override
                    public void cancel () {
                        exitPromptUp = false;
                    }
                });
            }
        }
    }
    boolean exitPromptUp = false;
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        
        stage.getViewport().update(width, height, true);
        versionTable.setPosition(Gdx.graphics.getWidth() - versionTable.getWidth() - edgePad, edgePad);
        titleLabel.setPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight(), Align.top);
        
        foregroundAnimation.resize(width, height);
        backgroundAnimation.resize(width, height);
    }
    
    private void initMenuFont(String menuFont) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 30;
        parameter.borderColor = Color.DARK_GRAY;
        parameter.borderWidth = 1;
        BitmapFont fontComfortaaBold = FontLoader.createFont(FontLoader.fontComfortaaBold, parameter);
        VisUI.getSkin().add(menuFont, fontComfortaaBold);
    }
    
    private void initTitleFont(String titleFont) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 90;
        parameter.borderColor = Color.DARK_GRAY;
        parameter.borderWidth = 1;
        BitmapFont fontComfortaaBold = FontLoader.createFont(FontLoader.fontComfortaaBold, parameter);
        VisUI.getSkin().add(titleFont, fontComfortaaBold);
    }
    
    private void initForegroundAnim() {
        ForegroundAnimation anim = ForegroundAnimation.random();
        
        //ensure new anim on refresh
        if (previousAnim != null && anim == previousAnim) {
            anim = ForegroundAnimation.next(anim);
        }
    
        //cleanup previous
        if (foregroundAnimation instanceof Disposable)
            ((Disposable)foregroundAnimation).dispose();
        
        switch (anim) {
            case delaunay:
                this.foregroundAnimation = new DelaunayAnim();
                break;
            case tree:
                this.foregroundAnimation = new TreeAnim();
                break;
            case orbit:
                this.foregroundAnimation = new OrbitAnim();
                break;
            case crossNoise:
                this.foregroundAnimation = new NoiseAnim(0, 0.01f, 3, 0.013f, true);
                break;
            case nbody:
                this.foregroundAnimation = new NBodyGravityAnim(stage);
                break;
                /*
			case asteroid: /
				this.foregroundAnimation = new AsteroidAnim();
				break;*/
        
        }
        previousAnim = anim;
        
        Gdx.app.debug(this.getClass().getSimpleName(), "Animation: " + anim);
    }
    
    public void dispose() {
        batch.dispose();
        shape.dispose();
    
        if (foregroundAnimation instanceof Disposable)
            ((Disposable)foregroundAnimation).dispose();
        
        VisUI.dispose(true);
        stage.dispose();
    }
}
