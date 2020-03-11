package com.cg.zoned;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.cg.zoned.screens.LoadingScreen;

public class Zoned extends Game {
	public Skin skin;

	@Override
	public void create () {
		Gdx.app.setLogLevel(Gdx.app.LOG_DEBUG);
		Gdx.input.setCatchKey(Input.Keys.BACK, true);

		this.setScreen(new LoadingScreen(this));
	}

	@Override
	public void render () {
		super.render();
	}

	@Override
	public void dispose () {
		skin.dispose();
	}
}