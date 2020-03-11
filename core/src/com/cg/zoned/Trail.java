package com.cg.zoned;

public class Trail {
    /*private GridPoint2 position = null;
    private Color startColor = null;
    private Color endColor = null;

    private float velocityX; // TODO: See Icicle thingy and complete this

    public Trail(float ) {
        this.position = new GridPoint2(-1, -1);
    }

    public void setTrailColors(Color startColor, Color endColor) {
        this.startColor = startColor;
        this.endColor = endColor;
    }

    public GridPoint2 getPosition() {
        return position;
    }

    private void offsetTrail(float delta) {
        this.velocityX  += delta * this.acceleration;
        this.position.x -= delta * this.velocityX; // TODO: Grow trail length as velocity increases?
        // TODO: Spawn trails on top/right rather than in an invisible "screen"
    }

    public void generateRandomPositionAndAcceleration(int width, int height) {
        this.position.x = width;
        this.position.y = MathUtils.random(10, height - 10);
        this.acceleration = MathUtils.random(100, 300);
        this.velocityX = MathUtils.random(15, 30);
    }

    public void render(ShapeRenderer renderer, float delta) {
        renderer.rectLine(this.position.x, this.position.y, this.position.x + TRAIL_LENGTH, this.position.y,
                TRAIL_WIDTH, this.startColor, this.endColor);

        offsetTrail(delta);
    }*/
}
