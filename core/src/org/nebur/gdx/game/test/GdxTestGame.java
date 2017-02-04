package org.nebur.gdx.game.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Iterator;
import java.util.Random;

public class GdxTestGame extends ApplicationAdapter {

	enum Lane {
		LEFT, RIGHT
	}

	private OrthographicCamera camera;
	private SpriteBatch spriteBatch;
	private Texture driverCarTexture;
	private Texture otherCarTexture;
	private Car driverCar;
	private Array<Car> cars;
	private float speed;
	private final Random spawnCarSeed = new Random();
	private long lastCarSpawnTime;
	private long lastCarSpawnTimeDelta = 2500000000L;

	@Override
	public void create() {
		// create the camera and the SpriteBatch
		camera = new OrthographicCamera();
		spriteBatch = new SpriteBatch();
		spriteBatch.enableBlending();
		spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA,
				GL20.GL_ONE_MINUS_SRC_ALPHA);

		// init textures
		driverCarTexture = new Texture(Gdx.files.internal("car.png"));
		otherCarTexture = new Texture(Gdx.files.internal("car2.png"));

		// set initial speed
		speed = 150;

		// configure driverCar position
		driverCar = new Car(Lane.LEFT, driverCarTexture);

		// init cars
		cars = new Array<Car>();
	}

	@Override
	public void resize(int width, int height) {
		float aspectRatio = (float) width / (float) height;
		camera = new OrthographicCamera(2f * aspectRatio, 2f);
		camera.rotate(new Vector3(1, 0, 0), 15.0f);

		driverCar = new Car(driverCar.lane, driverCarTexture);

		for (Car car : cars) {
			car.clone(new Car(car.lane, otherCarTexture));
		}
	}

	@Override
	public void render() {
		// clear screen
		clearScreen();

		// tell the camera to update its matrices.
		camera.update();

		// render lanes
		renderLanes();

		// render driverCar
		renderCar();

		// render cars
		renderCars();

		// process input
		processInput();

		// check if we need to create a new car
		if (TimeUtils.nanoTime() - lastCarSpawnTime > lastCarSpawnTimeDelta) spawnCar();

		// check if any collision happened
		moveCarsAndCheckCarsCollision();
	}

	private void spawnCar() {
		boolean rightLane = spawnCarSeed.nextBoolean();

		Lane lane = rightLane ? Lane.RIGHT : Lane.LEFT;

		Car car = new Car(lane, otherCarTexture);
		car.y = Gdx.graphics.getHeight() + Gdx.graphics.getHeight() / 5;

		cars.add(car);
		lastCarSpawnTime = TimeUtils.nanoTime();
	}

	private void clearScreen() {
		// clear the screen with a dark blue color. The
		// arguments to glClearColor are the red, green
		// blue and alpha component in the range [0,1]
		// of the color to be used to clear the screen.
		Gdx.gl.glClearColor(0, 0.2f, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	private void processInput() {
		if (Gdx.input.isTouched()) { // toggle lane
			switch (driverCar.lane) {
				case LEFT:
					driverCar.lane = Lane.RIGHT;
					break;
				case RIGHT:
					driverCar.lane = Lane.LEFT;
					break;
			}
		}

		if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) { driverCar.lane = Lane.LEFT; }
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) { driverCar.lane = Lane.RIGHT; }
	}

	private void renderLanes() {
		// draw lane
		ShapeRenderer shapeRenderer = new ShapeRenderer();
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(Color.BLACK);
		shapeRenderer.rect(Gdx.graphics.getWidth() / 4,
				0,
				Gdx.graphics.getWidth() / 2,
				Gdx.graphics.getHeight());
		shapeRenderer.end();

		// draw main lane line
		Gdx.gl.glLineWidth(4);
		shapeRenderer = new ShapeRenderer();
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.line(
				new Vector2(Gdx.graphics.getWidth() / 2, 0),
				new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight()));
		shapeRenderer.end();

		// draw first secondary lane line
		Gdx.gl.glLineWidth(2);
		shapeRenderer = new ShapeRenderer();
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.line(
				new Vector2(Gdx.graphics.getWidth() / 4, 0),
				new Vector2(Gdx.graphics.getWidth() / 4, Gdx.graphics.getHeight()));
		shapeRenderer.end();

		// draw second secondary lane line
		Gdx.gl.glLineWidth(2);
		shapeRenderer = new ShapeRenderer();
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.line(
				new Vector2(Gdx.graphics.getWidth() * 3 / 4, 0),
				new Vector2(Gdx.graphics.getWidth() * 3 / 4, Gdx.graphics.getHeight()));
		shapeRenderer.end();


		//
		Gdx.gl.glLineWidth(1);
	}

	private void renderCar() {
		driverCar.render();
	}

	private void renderCars() {
		for (Car car : cars) {
			car.render();
		}
	}

	private void moveCarsAndCheckCarsCollision() {
		// move the cars, remove any that are beneath the bottom edge of
		// the screen or that hit the driver driverCar. TODO: In the later case we play back
		// a sound effect as well.
		Iterator<Car> iterator = cars.iterator();
		while (iterator.hasNext()) {
			Car car = iterator.next();
			car.y -= speed * Gdx.graphics.getDeltaTime();
			if ((car.y + Gdx.graphics.getHeight() / 5) < 0) { iterator.remove(); }
			if (car.overlaps(driverCar)) {
				// FIXME: dropSound.play();
				// TODO: game over

				System.err.println("Should stop the game!!!");
			}
		}
	}

	@Override
	public void dispose() {
		// dispose of all allocated resources
	}

	private class Car extends Rectangle {

		public Lane lane;
		public Texture texture;

		public Car(Lane lane, Texture texture) {
			this.lane = lane;
			this.texture = texture;
			this.x = 0;
			this.y = 20;
			this.width = Gdx.graphics.getWidth() / 4 - 40;
			this.height = Gdx.graphics.getHeight() / 5;

			// updates car positions based on its lane
			updatePosition();
		}

		public void updatePosition() {
			switch (lane) {
				case LEFT:
					this.x = 20 + Gdx.graphics.getWidth() / 4;
					break;
				case RIGHT:
					this.x = 20 + Gdx.graphics.getWidth() / 2;
					break;
			}
		}

		public void render() {
			updatePosition();

			spriteBatch.begin();
			spriteBatch.draw(
					new TextureRegion(
							texture,
							0.5f,
							0.0f,
							1.0f,
							1.0f
					),
					x,
					y,
					width,
					height);
			spriteBatch.end();
		}

		public void clone(Car other) {
			this.lane = other.lane;
			this.texture = other.texture;
			this.x = other.x;
			this.y = other.y;
			this.width = other.width;
			this.height = other.height;
		}
	}
}
