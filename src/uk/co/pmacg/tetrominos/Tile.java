package uk.co.pmacg.tetrominos;

import java.awt.Color;

public class Tile {
	private Color color;
	private int x_pos;
	private int y_pos;
	private int x_saved;
	private int y_saved;
	private int x_off;
	private int y_off;
	private int x_off_saved;
	private int y_off_saved;
	private boolean moving;
	private boolean moving_saved;
	
	public Tile(Color color, int x_pos, int y_pos, int x_off, int y_off, boolean moving) {
		this.color = color;
		this.x_pos = x_pos;
		this.y_pos = y_pos;
		this.x_off = x_off;
		this.y_off = y_off;
		this.moving = moving;
	}
	
	public void rotate_right(){
		x_pos = x_pos + x_off + y_off;
		y_pos = y_pos + y_off - x_off;
		
		int temp = x_off;
		x_off = - y_off;
		y_off = temp;
	}
	
	public void rotate_left(){
		x_pos = x_pos + x_off - y_off;
		y_pos = y_pos + y_off + x_off;
		
		int temp = x_off;
		x_off = y_off;
		y_off = - temp;
	}
	
	public void save_position(){
		x_saved = x_pos;
		y_saved = y_pos;
		moving_saved = moving;
		x_off_saved = x_off;
		y_off_saved = y_off;
	}
	
	public void load_position(){
		x_pos = x_saved;
		y_pos = y_saved;
		moving = moving_saved;
		x_off = x_off_saved;
		y_off = y_off_saved;
	}
	
	public Tile copy(){
		return new Tile(color, x_pos, y_pos, x_off, y_off, moving);
	}

	public Color getColor() {
		return color;
	}

	public int getX_pos() {
		return x_pos;
	}

	public int getY_pos() {
		return y_pos;
	}

	public boolean isMoving() {
		return moving;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setX_pos(int x_pos) {
		this.x_pos = x_pos;
	}

	public void setY_pos(int y_pos) {
		this.y_pos = y_pos;
	}

	public void setMoving(boolean moving) {
		this.moving = moving;
	}
}
