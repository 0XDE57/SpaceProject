package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.screens.animations.DelaunayAnim;
import com.spaceproject.screens.animations.DropAnim;
import com.spaceproject.screens.animations.NoiseAnim;
import com.spaceproject.screens.animations.OrbitAnim;
import com.spaceproject.screens.animations.TitleAnimation;
import com.spaceproject.screens.animations.TreeAnim;
import com.spaceproject.screens.debug.Test3DScreen;
import com.spaceproject.screens.debug.TestNoiseScreen;
import com.spaceproject.screens.debug.TestShipGenerationScreen;
//import com.spaceproject.screens.debug.TestSpiralGalaxy;
import com.spaceproject.screens.debug.TestVoronoiScreen;

public class TitleScreen extends MyScreenAdapter {

	private SpaceProject game;

	private Stage stage;
	private BitmapFont fontComfortaaBold;
	private Matrix4 projectionMatrix = new Matrix4();

	private TitleAnimation foregroundAnimation, backgroundAnimation;
	enum ForegroundAnimation {
		tree, delaunay, orbit, drop, crossNoise;

		private static ForegroundAnimation random()  {
			return ForegroundAnimation.values()[MathUtils.random(ForegroundAnimation.values().length-1)];
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

		Table table = CreateMainMenu(false);
		stage.addActor(table);
		table.pack(); //force table to calculate size
		int edgePad = SpaceProject.isMobile() ? 20 : 10;
		table.setPosition(edgePad,edgePad);


		backgroundAnimation = new NoiseAnim();
		initForegroundAnim();

		// font
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = 90;
		parameter.borderColor = Color.DARK_GRAY;
		parameter.borderWidth = 1;
		fontComfortaaBold = FontFactory.createFont(FontFactory.fontComfortaaBold, parameter);

		Gdx.graphics.setVSync(true);
	}

	public void render(float delta) {
		super.render(delta);

		Gdx.gl20.glClearColor(1, 1, 1, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);


		cam.position.set(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2,0);
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		shape.setProjectionMatrix(projectionMatrix);
		batch.setProjectionMatrix(projectionMatrix);


		backgroundAnimation.render(delta, shape);

		//enable transparency
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		foregroundAnimation.render(delta, shape);

		Gdx.gl.glDisable(GL20.GL_BLEND);

		//draw title
		batch.begin();
		fontComfortaaBold.draw(batch, SpaceProject.TITLE, 50, Gdx.graphics.getHeight() - 50);
		batch.end();


		stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
		stage.draw();


		if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
			initForegroundAnim();
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
			Table table = CreateMainMenu(true);
			table.pack(); //force table to calculate size
			int edgePad = SpaceProject.isMobile() ? 20 : 10;
			table.setPosition(edgePad,edgePad);
			stage.clear();
			stage.addActor(table);
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
			Gdx.app.exit();
		}

	}

	@Override
	public void resize(int width, int height){
		super.resize(width, height);
		stage.getViewport().update(width, height, true);
		foregroundAnimation.resize(width, height);
		backgroundAnimation.resize(width, height);
	}


	private Table CreateMainMenu(boolean showDebugScreens) {

		//create buttons
		TextButton btnPlay = new TextButton("play", VisUI.getSkin());
		btnPlay.getLabel().setAlignment(Align.left);
		btnPlay.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				game.setScreen(new GameScreen(true));
			}
		});


		TextButton btnVoronoi = new TextButton("voronoi [DEBUG]", VisUI.getSkin());
		btnVoronoi.getLabel().setAlignment(Align.left);
		btnVoronoi.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				game.setScreen(new TestVoronoiScreen());
			}
		});


		TextButton btnNoise = new TextButton("noise [DEBUG]", VisUI.getSkin());
		btnNoise.getLabel().setAlignment(Align.left);
		btnNoise.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				game.setScreen(new TestNoiseScreen());
			}
		});


		TextButton btnShip = new TextButton("ship gen [DEBUG]", VisUI.getSkin());
		btnShip.getLabel().setAlignment(Align.left);
		btnShip.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				game.setScreen(new TestShipGenerationScreen());
			}
		});


		TextButton btn3D = new TextButton("3D rotate [DEBUG]", VisUI.getSkin());
		btn3D.getLabel().setAlignment(Align.left);
		btn3D.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				game.setScreen(new Test3DScreen());
			}
		});

		TextButton btnSpiral = new TextButton("Spiral Gen [DEBUG]", VisUI.getSkin());
		btnSpiral.getLabel().setAlignment(Align.left);
		btnSpiral.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				//game.setScreen(new TestSpiralGalaxy());
			}
		});


		TextButton btnLoad = new TextButton("load", VisUI.getSkin());
		btnLoad.getLabel().setAlignment(Align.left);
		btnLoad.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Gdx.app.log(this.getClass().getSimpleName(), "load placeholder");
			}
		});

		TextButton btnOption = new TextButton("options", VisUI.getSkin());
		btnOption.getLabel().setAlignment(Align.left);
		btnOption.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Gdx.app.log(this.getClass().getSimpleName(), "options placeholder");
			}
		});

		TextButton btnExit = new TextButton("exit", VisUI.getSkin());
		btnExit.getLabel().setAlignment(Align.left);
		btnExit.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Gdx.app.exit();
			}
		});


		//add buttons to table
		Table table = new Table();
		table.add(btnPlay).fillX().row();
		if (showDebugScreens) {
			table.add(btnVoronoi).fillX().row();
			table.add(btnNoise).fillX().row();
			table.add(btn3D).fillX().row();
			table.add(btnShip).fillX().row();
			table.add(btnSpiral).row();
		}
		table.add(btnLoad).left().fillX().row();
		table.add(btnOption).fillX().row();
		table.add(btnExit).fillX().row();


		//set bigger labels on mobile
		if (SpaceProject.isMobile()) {
			for (Actor button : table.getChildren()) {
				if (button instanceof TextButton) {
					//((TextButton) button).getLabel().getFont???.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
					((TextButton) button).getLabel().setFontScale(2f);
				}
			}
		}


		return table;
	}


	private void initForegroundAnim() {
		ForegroundAnimation anim = ForegroundAnimation.random();
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
			case drop:
				this.foregroundAnimation = new DropAnim();
				break;
			case crossNoise:
				this.foregroundAnimation = new NoiseAnim(0, 0.01f, 3, 0.013f, true);
				break;
				/*
			case asteroid:
				this.foregroundAnimation = new AsteroidAnim();
				break;
				*/
		}
		Gdx.app.log(this.getClass().getSimpleName(), "Animation: " + anim);
	}

	public void dispose() {
		batch.dispose();
		shape.dispose();
		VisUI.dispose(true);
		stage.dispose();
	}
}
