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
import com.spaceproject.screens.menuanim.DelaunayAnimation;
import com.spaceproject.screens.menuanim.MainMenuAnimation;
import com.spaceproject.screens.menuanim.NoiseAnim;
import com.spaceproject.screens.menuanim.OrbitAnim;
import com.spaceproject.screens.menuanim.TreeAnimation;

public class MainMenuScreen extends MyScreenAdapter {

	SpaceProject game;

	private Stage stage;
	private BitmapFont fontComfortaaBold;
	private Matrix4 projectionMatrix = new Matrix4();

	MainMenuAnimation foregroundAnimation, backgroundAnimation;
	enum MenuAnimation {
		tree, delaunay, orbit
	}

	public MainMenuScreen(SpaceProject spaceProject) {
		this.game = spaceProject;

		//init scene2d and VisUI
		if (!VisUI.isLoaded())
			VisUI.load(VisUI.SkinScale.X2);
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);

		Table table = CreateMainMenu(true);
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
		foregroundAnimation.render(delta, shape);

		//draw title
		batch.begin();
		fontComfortaaBold.draw(batch, SpaceProject.TITLE, 50, Gdx.graphics.getHeight() - 50);
		batch.end();


		stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
		stage.draw();


		if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
			initForegroundAnim();
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

		TextButton btnLoad = new TextButton("load", VisUI.getSkin());
		btnLoad.getLabel().setAlignment(Align.left);
		btnLoad.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				System.out.println("placeholder");
			}
		});

		TextButton btnOption = new TextButton("options", VisUI.getSkin());
		btnOption.getLabel().setAlignment(Align.left);
		btnOption.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				System.out.println("placeholder");
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



	public static MenuAnimation randomAnim()  {
		return MenuAnimation.values()[MathUtils.random(MenuAnimation.values().length-1)];
	}
	private void initForegroundAnim() {
		switch (randomAnim()) {
			case delaunay:
				foregroundAnimation = new DelaunayAnimation();
				break;
			case tree:
				foregroundAnimation = new TreeAnimation();
				break;
			case orbit:
				foregroundAnimation = new OrbitAnim();
				break;
		}
	}

	public void dispose() {
		batch.dispose();
		shape.dispose();
		VisUI.dispose();
		stage.dispose();
	}
}
