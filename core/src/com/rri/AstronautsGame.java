package com.rri;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.talosvfx.talos.runtime.ParticleEffectDescriptor;
import com.talosvfx.talos.runtime.ParticleEffectInstance;
import com.talosvfx.talos.runtime.render.SpriteBatchParticleRenderer;

import java.util.Iterator;

/**
 * Artwork from https://goodstuffnononsense.com/about/
 * https://goodstuffnononsense.com/hand-drawn-icons/space-icons/
 */
public class AstronautsGame extends ApplicationAdapter {
	private Texture astronautImage;
	private Texture rocketImage;
	private Texture asteroidImage;
	private Sound astronautSound;
	private SpriteBatch batch;
	private OrthographicCamera camera;
	private Rectangle rocket;
	private Array<Rectangle> astronauts; //special LibGDX Array
	private Array<Rectangle> asteroids;
	private long lastAstronautTime;
	private long lastAsteroidTime;
	private int astronautsRescuedScore;
	private int rocketHealth; //Starts with 100
	private ParticleEffectInstance peRocket;
	private ParticleEffectInstance peAstronaut;
	private Array<ParticleEffectInstance> peAstronauts;
	private ParticleEffectInstance peAsteroid;
	private Array<ParticleEffectInstance> peAsteroids;
	private ParticleEffectInstance peRocketExplosion;
	private ParticleEffectInstance peAsteroidExplosion;
	private SpriteBatchParticleRenderer defaultRenderer;
	ParticleEffectDescriptor EffectDescriptor;
	private PolygonSpriteBatch talosBatch;
	private Viewport viewportRocket;
	private Viewport viewportAsteroid;
	private Viewport viewportAstronaut;
	private Viewport viewportRocketExplosion;
	private Viewport viewportAsteroidExplosion;

	private TextureAtlas textureAtlas;

	private BitmapFont font;

	private boolean gameEnd = false;
	private Rectangle lastAsteroid;

	private float rotation;
	private float rotDirection;
	private float timer;
	private float rocketDirection;
	private boolean rocketReturn;

	//Values are set experimental
	private static int SPEED = 600; // pixels per second
	private static int SPEED_ASTRONAUT = 200; // pixels per second
	private static int SPEED_ASTEROID = 100; // pixels per second
	private static long CREATE_ASTRONAUT_TIME = 1000000000; //ns
	private static long CREATE_ASTEROID_TIME = 2000000000; //ns

	private void commandMoveLeft() {
		rocket.x -= SPEED * Gdx.graphics.getDeltaTime();
		if(rocket.x < 0) rocket.x = 0;
	}

	private void commandMoveRight() {
		rocket.x += SPEED * Gdx.graphics.getDeltaTime();
		if(rocket.x > Gdx.graphics.getWidth() - rocketImage.getWidth())
			rocket.x = Gdx.graphics.getWidth() - rocketImage.getWidth();
	}

	private void commandMoveLeftCorner() {
		rocket.x = 0;
	}
	private void commandMoveRightCorner() {
		rocket.x = Gdx.graphics.getWidth() - rocketImage.getWidth();
	}

	private void commandTouched() {
		Vector3 touchPos = new Vector3();
		touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
		camera.unproject(touchPos);
		rocket.x = touchPos.x - rocketImage.getWidth() / 2;
	}

	private void commandExitGame() {
		Gdx.app.exit();
	}

	@Override
	public void create() {

		Gdx.app.setLogLevel(Logger.DEBUG);
		font = new BitmapFont();
		font.getData().setScale(2);
		astronautsRescuedScore = 0;
		rocketHealth = 100;

		//SPREMENLJIVKE, KI JIH POTREBUJEMO ZA PREMIKANJE SLIKE RAKETE
		rotation = -15;
		rotDirection = 1;
		timer = 0;
		rocketDirection = 1;
		rocketReturn = false;

		lastAsteroid = new Rectangle();

		// default way to load texture
		rocketImage = new Texture(Gdx.files.internal("rocket64.png"));
		astronautImage = new Texture(Gdx.files.internal("astronaut48.png"));
		asteroidImage = new Texture(Gdx.files.internal("asteroid128.png"));
		astronautSound = Gdx.audio.newSound(Gdx.files.internal("pick.wav"));

		// create the camera and the SpriteBatch
		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		//USTVARIMO VIEWPORTE ZA VSAK UČINEK
		//LAHKO PONOVNO UPORABIMO ENAKE VIEWPORTE
		viewportRocket = new FitViewport(80f, 40f);
		viewportRocket.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

		viewportAsteroid = new FitViewport(10f, 5f);
		viewportAsteroid.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

		viewportAstronaut = new FitViewport(25f, 12.5f);
		viewportAstronaut.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

		viewportAsteroidExplosion = new FitViewport(10f, 5f);
		viewportAsteroidExplosion.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

		viewportRocketExplosion = new FitViewport(80f, 40f);
		viewportRocketExplosion.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

		batch = new SpriteBatch();
		talosBatch = new PolygonSpriteBatch();


		// create a Rectangle to logically represents the rocket
		rocket = new Rectangle();
		rocket.x = Gdx.graphics.getWidth() / 2 - rocketImage.getWidth() / 2; // center the rocket horizontally
		rocket.y = 20; // bottom left corner of the rocket is 20 pixels above the bottom screen edge
		rocket.width = rocketImage.getWidth();
		rocket.height = rocketImage.getHeight();


		defaultRenderer = new SpriteBatchParticleRenderer();

		//USTVARIMO TEXTURE ATLAS
		TextureRegion textureRegion1 = new TextureRegion(new Texture(Gdx.files.internal("fire.png")));
		TextureRegion textureRegion2 = new TextureRegion(new Texture(Gdx.files.internal("rocket64.png")));
		TextureRegion textureRegion3 = new TextureRegion(new Texture(Gdx.files.internal("point glow.png")));
		TextureRegion textureRegion4 = new TextureRegion(new Texture(Gdx.files.internal("circle.png")));
		TextureRegion textureRegion5 = new TextureRegion(new Texture(Gdx.files.internal("astronaut48.png")));
		TextureRegion textureRegion6 = new TextureRegion(new Texture(Gdx.files.internal("asteroid128.png")));
		TextureRegion textureRegion7 = new TextureRegion(new Texture(Gdx.files.internal("rocket_debris.png")));
		TextureRegion textureRegion8 = new TextureRegion(new Texture(Gdx.files.internal("rocket64_front.png")));
		TextureRegion textureRegion9 = new TextureRegion(new Texture(Gdx.files.internal("rocket64_left.png")));
		TextureRegion textureRegion10 = new TextureRegion(new Texture(Gdx.files.internal("rocket64_right.png")));
		TextureRegion textureRegion11 = new TextureRegion(new Texture(Gdx.files.internal("asteroid72.png")));
		textureAtlas = new TextureAtlas();
		textureAtlas.addRegion("rocket64", textureRegion2);
		textureAtlas.addRegion("asteroid128", textureRegion6);
		textureAtlas.addRegion("fire", textureRegion1);
		textureAtlas.addRegion("point glow", textureRegion3);
		textureAtlas.addRegion("circle", textureRegion4);
		textureAtlas.addRegion("astronaut48", textureRegion5);
		textureAtlas.addRegion("rocket_debris", textureRegion7);
		textureAtlas.addRegion("rocket64_front", textureRegion8);
		textureAtlas.addRegion("rocket64_left", textureRegion9);
		textureAtlas.addRegion("rocket64_right", textureRegion10);
		textureAtlas.addRegion("asteroid72", textureRegion11);

/**
		 * Creating particle effect instance from particle effect descriptor
 */
		//CREATE ROCKET
		EffectDescriptor = new ParticleEffectDescriptor(Gdx.files.internal("rocket.p"), textureAtlas);
		peRocket = EffectDescriptor.createEffectInstance();
		//Izklopimo oddajnik za sliko rakete, saj ne deluje v povezavi z našo igro
		peRocket.getEmitters().get(2).setVisible(false);

		defaultRenderer.setBatch(talosBatch);

		astronauts = new Array<Rectangle>();
		asteroids = new Array<Rectangle>();
		peAsteroids = new Array<ParticleEffectInstance>();
		peAstronauts = new Array<ParticleEffectInstance>();
		//add first astronaut and asteroid
		spawnAstronaut();
		spawnAsteroid();


	}

	private void spawnAstronaut() {
		Rectangle astronaut = new Rectangle();
		astronaut.x = MathUtils.random(0, Gdx.graphics.getWidth() - astronautImage.getWidth());
		astronaut.y = Gdx.graphics.getHeight();
		astronaut.width  = astronautImage.getWidth();
		astronaut.height = astronautImage.getHeight();
		astronauts.add(astronaut);
		lastAstronautTime = TimeUtils.nanoTime();
	}

	private void spawnAsteroid() {
		Rectangle asteroid = new Rectangle();
		asteroid.x = MathUtils.random(0, Gdx.graphics.getWidth()- astronautImage.getWidth());
		asteroid.y = Gdx.graphics.getHeight();
		asteroid.width = asteroidImage.getWidth();
		asteroid.height = asteroidImage.getHeight();
		asteroids.add(asteroid);

		//CREATE ASTEROIDS
		EffectDescriptor = new ParticleEffectDescriptor(Gdx.files.internal("asteroid_travel.p"), textureAtlas);
		peAsteroid = EffectDescriptor.createEffectInstance();
		//Izklopimo oddajnik za sliko asteroida in majhnega asteroida
		//Majhen asteroid moti vidljivost igre
		//Slika asteroida pa ne deluje z našo igro
		peAsteroid.getEmitters().get(2).setVisible(false);
		peAsteroid.getEmitters().get(4).setVisible(false);
		peAsteroids.add(peAsteroid);

		lastAsteroidTime = TimeUtils.nanoTime();
	}


	@Override
	public void render() { //runs every frame

		//clear screen
		Gdx.gl.glClearColor(0, 0, 0f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		// tell the camera to update its matrices.
		camera.update();

		// tell the SpriteBatch to render in the
		// coordinate system specified by the camera.
		batch.setProjectionMatrix(camera.combined);

		if(!gameEnd)
		{
			//ROCKET TRAVEL RENDER
			talosBatch.setProjectionMatrix(viewportRocket.getCamera().projection);
			talosBatch.begin();
			peRocket.setPosition((float) (rocket.x/12.8 -38.5),-16);
			peRocket.update(Gdx.graphics.getDeltaTime());
			peRocket.render(defaultRenderer);
			talosBatch.end();
		}


		// begin a new batch and draw the rocket, astronauts, asteroids
		batch.begin();
		{ //add brackets just for intent
			if(!gameEnd)
			{
				//premikanje rakete gor in dol usklajeno z učinkom potovanja
				//časovni zamiki so nastavljeni glede na časovne zamike v samem učinku, kadar spremenimo učinek v Talos VFX je potrebno tudi spremeniti
				//vrednosti tukaj
				timer += Gdx.graphics.getDeltaTime();
				if(timer >= 2.8)
				{
					if(rocket.y <= 20 - rocketImage.getHeight()*(float)0.2)
						rocketDirection = -1;

					float change = Gdx.graphics.getDeltaTime()*-(rocketImage.getHeight()*(float)0.8)*rocketDirection;
					rocket.y += change;

					if(rocket.y >= 20 + rocketImage.getHeight()*(float)0.2)
					{
						rocketDirection = 1;
						timer = 0;
						rocketReturn = true;
					}
				}
				if(timer >= 1 && rocketReturn)
				{
					float change = Gdx.graphics.getDeltaTime()*-(rocketImage.getHeight()*(float)0.4);
					rocket.y += change;
					if(rocket.y <= 20)
					{
						timer = 0;
						rocketReturn = false;
					}
				}
				//nihanje rakete nastavljeno da se ujema z ucinkom potovanja rakete
				float change = Gdx.graphics.getDeltaTime()*12*rotDirection;
				if(rotation >= 15)
					rotDirection = -1;
				if(rotation <= -15)
					rotDirection = 1;
				rotation += change;

				//batch.draw(rocketImage, rocket.x, rocket.y);
				batch.draw(textureAtlas.getRegions().get(0), rocket.x, rocket.y, rocketImage.getWidth()/2, rocketImage.getHeight()/2,
						rocketImage.getWidth(), rocketImage.getHeight(),1,1,rotation);
			}


			for (Rectangle asteroid : asteroids) {
				batch.draw(asteroidImage, asteroid.x, asteroid.y);

			}
			for (Rectangle astronaut : astronauts) {
				batch.draw(astronautImage, astronaut.x, astronaut.y);
			}
			font.setColor(Color.YELLOW);
			font.draw(batch, "" + astronautsRescuedScore, Gdx.graphics.getWidth() - 50, Gdx.graphics.getHeight() - 20);
			font.setColor(Color.GREEN);
			font.draw(batch, "" + rocketHealth, 20, Gdx.graphics.getHeight() - 20);



		}

		batch.end();

		if(!gameEnd)
		{
			// process user input
			if(Gdx.input.isTouched()) commandTouched(); //mouse or touch screen
			if(Gdx.input.isKeyPressed(Keys.LEFT)) commandMoveLeft();
			if(Gdx.input.isKeyPressed(Keys.RIGHT)) commandMoveRight();
			if(Gdx.input.isKeyPressed(Keys.A)) commandMoveLeftCorner();
			if(Gdx.input.isKeyPressed(Keys.S)) commandMoveRightCorner();
		}

		if(Gdx.input.isKeyPressed(Keys.ESCAPE)) commandExitGame();

		// check if we need to create a new
		if(TimeUtils.nanoTime() - lastAstronautTime > CREATE_ASTRONAUT_TIME) spawnAstronaut();
		if(TimeUtils.nanoTime() - lastAsteroidTime > CREATE_ASTEROID_TIME) spawnAsteroid();

		if (rocketHealth > 0) { //is game end?
			// move and remove any that are beneath the bottom edge of
			// the screen or that hit the rocket.
			for (Iterator<Rectangle> iter = asteroids.iterator(); iter.hasNext(); ) {
				Rectangle asteroid = iter.next();
				asteroid.y -= SPEED_ASTEROID * Gdx.graphics.getDeltaTime();

				//UPDATE ASTEROIDS POSITION
				peAsteroids.get(asteroids.indexOf(asteroid, true)).setPosition((float)(asteroid.x/102.4 -4.5), (float)(asteroid.y/96 -2.2));

				if (asteroid.y + asteroidImage.getHeight() < -400)
				{//REMOVE ASTEROIDS WHEN OUT OF BOUNDS
					peAsteroids.removeIndex(asteroids.indexOf(asteroid, true));
					iter.remove();
				}
				if (asteroid.overlaps(rocket)) {
					astronautSound.play();
					//označimo asteroid, z katerim smo nazadnje imeli stik
					lastAsteroid = asteroid;
					rocketHealth--;
				}
			}

			for (Iterator<Rectangle> iter = astronauts.iterator(); iter.hasNext(); ) {
				Rectangle astronaut = iter.next();
				astronaut.y -= SPEED_ASTRONAUT * Gdx.graphics.getDeltaTime();


				if (astronaut.y + astronautImage.getHeight() < 0) iter.remove(); //From screen
				if (astronaut.overlaps(rocket)) {

					//CREATE ASTRONAUT PICKUP
					EffectDescriptor = new ParticleEffectDescriptor(Gdx.files.internal("astronaut_pickup.p"), textureAtlas);
					peAstronaut = EffectDescriptor.createEffectInstance();
					peAstronaut.setPosition((float)(astronaut.x/40.96 - 12), (float)(astronaut.y/38.4 -5.5));
					peAstronauts.add(peAstronaut);

					astronautSound.play();
					astronautsRescuedScore++;
					if (astronautsRescuedScore %10==0) SPEED_ASTEROID +=66; //speeds up
					iter.remove(); //smart Array enables remove from Array
				}
			}



		} else { //health of rocket is 0 or less
			batch.begin();
			{
				if(!gameEnd)
				{
					gameEnd=true;

					//CREATE ROCKET EXPLOSION
					EffectDescriptor = new ParticleEffectDescriptor(Gdx.files.internal("rocket_explosion.p"), textureAtlas);
					peRocketExplosion = EffectDescriptor.createEffectInstance();
					peRocketExplosion.setPosition((float) (rocket.x/12.8 -38.5),-16);

					//CREATE ASTEROID EXPLOSION
					EffectDescriptor = new ParticleEffectDescriptor(Gdx.files.internal("asteroid.p"), textureAtlas);
					peAsteroidExplosion = EffectDescriptor.createEffectInstance();
					peAsteroidExplosion.setPosition((float)(lastAsteroid.x/102.4 -4.5), (float)(lastAsteroid.y/96 -2.2));

					//ODSTRANIMO ZADNJI ASTEROID IN GA ZAMENJAMO Z EKSPLOZIJO
					for (Iterator<Rectangle> iter = asteroids.iterator(); iter.hasNext(); )
					{
						Rectangle asteroid = iter.next();
						if (asteroid == lastAsteroid)
						{
							peAsteroids.removeIndex(asteroids.indexOf(asteroid, true));
							iter.remove();
						}
					}
				}


				//ASTEROID EXPLOSION RENDER
				talosBatch.setProjectionMatrix(viewportAsteroidExplosion.getCamera().projection);
				talosBatch.begin();
				peAsteroidExplosion.update(Gdx.graphics.getDeltaTime());
				peAsteroidExplosion.render(defaultRenderer);
				talosBatch.end();

				//ROCKET EXPLOSION RENDER
				talosBatch.setProjectionMatrix(viewportRocketExplosion.getCamera().projection);
				talosBatch.begin();
				peRocketExplosion.update(Gdx.graphics.getDeltaTime());
				peRocketExplosion.render(defaultRenderer);
				talosBatch.end();

				font.setColor(Color.RED);
				font.draw(batch, "The END", Gdx.graphics.getHeight() / 2, Gdx.graphics.getHeight() / 2);
			}
			batch.end();
		}

		//ASTEROIDS RENDER
		talosBatch.setProjectionMatrix(viewportAsteroid.getCamera().projection);
		talosBatch.begin();
		for(ParticleEffectInstance peRoid : peAsteroids)
		{
			peRoid.update(Gdx.graphics.getDeltaTime());
			peRoid.render(defaultRenderer);
		}
		talosBatch.end();

		//ASTRONAUT PICKUPS RENDER
		talosBatch.setProjectionMatrix(viewportAstronaut.getCamera().projection);
		talosBatch.begin();
		for(ParticleEffectInstance peNaut : peAstronauts)
		{
			peNaut.update(Gdx.graphics.getDeltaTime());
			peNaut.render(defaultRenderer);
		}
		talosBatch.end();

	}

	@Override
	public void dispose() {
		// dispose of all the native resources
		astronautImage.dispose();
		rocketImage.dispose();
		astronautSound.dispose();
		batch.dispose();
		font.dispose();
	}
}
