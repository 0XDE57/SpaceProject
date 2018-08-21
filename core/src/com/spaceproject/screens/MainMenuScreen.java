package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
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
import com.spaceproject.screens.menuanim.TreeAnimation;

public class MainMenuScreen extends MyScreenAdapter {

	SpaceProject game;

	private Stage stage;
	private BitmapFont fontComfortaaBold;


	MainMenuAnimation foregroundAnimation, backgroundAnimation;
	enum MenuAnimation {
		tree, delaunay
	}

	public static MenuAnimation randomAnim()  {
		return MenuAnimation.values()[MathUtils.random(MenuAnimation.values().length-1)];
	}


	public MainMenuScreen(SpaceProject spaceProject) {
		this.game = spaceProject;

		//init scene2d and VisUI
		if (!VisUI.isLoaded())
			VisUI.load(VisUI.SkinScale.X1);
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);

		Table table = CreateMainMenu();
		stage.addActor(table);


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

		Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);


		cam.position.set(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2,0);

		Gdx.gl.glEnable(Gdx.gl20.GL_BLEND);
		Gdx.gl.glBlendFunc(Gdx.gl20.GL_SRC_ALPHA, Gdx.gl20.GL_ONE_MINUS_SRC_ALPHA);

		backgroundAnimation.render(delta, shape);
		foregroundAnimation.render(delta, shape);

		//draw title
		batch.begin();
		fontComfortaaBold.draw(batch, "a space project", 50, Gdx.graphics.getHeight() - 50);
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

		//batch.setProjectionMatrix(cam.combined);
		//shape.setProjectionMatrix(cam.combined);
		//todo, fix title menu scaling/position
		//todo, fix animation scaling/position
		//todo, do for all debug screens...
	}


	private Table CreateMainMenu() {
		Table table = new Table();//VisUI.getSkin());
		//table.setDebug(true, true);
		//table.right();
		//table.setFillParent(true);

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
		table.add(btnPlay).fillX().row();
		table.add(btnVoronoi).fillX().row();
		table.add(btnNoise).fillX().row();
		table.add(btn3D).fillX().row();
		table.add(btnShip).fillX().row();

		//table.setBackground("blue");
		//table.setColor(0,0,1,1);
		table.setPosition(100,100);// table.getHeight()
		return table;
	}

	private void initForegroundAnim() {
		switch (randomAnim()) {
			case delaunay:
				foregroundAnimation = new DelaunayAnimation();
				break;
			case tree:
				foregroundAnimation = new TreeAnimation();
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
