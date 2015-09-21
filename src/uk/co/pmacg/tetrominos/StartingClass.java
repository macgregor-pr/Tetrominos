package uk.co.pmacg.tetrominos;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class StartingClass extends Applet implements Runnable, KeyListener{

	private static final long serialVersionUID = 1L;
	private static int WIDTH = 660;
	private static int HEIGHT = 600;
	
	public static int NUM_COLUMNS = 10;
	public static int NUM_ROWS = 20;
	private static int TILE_HEIGHT = (HEIGHT - 60)/NUM_ROWS;
	private static int TILE_WIDTH = (WIDTH/2 - 60)/NUM_COLUMNS;
	
	private Image image;
	private Graphics second;
	private Color grid_background = new Color(220, 220, 220);
	private int total_rows_cleared = 0;
	
	private Random rand = new Random();
	
	private int speed = 50;
	
	// Variables for the Tetris AI
	private static boolean AI = true;
	private int num_rots = 0;
	private int num_xmoves = 0;
	private int num_rows = 0;
	private boolean new_tet = false;
	private double current_score = 0;
	private double worst_score = -10;
	private double best_score = -5;
	
	
	// State Variables
	private TetType next_type = TetType.I;
	public static LinkedList<Tile> tiles_list = new LinkedList<Tile>();
	private static LinkedList<TetType> future_queue = new LinkedList<TetType>();
	private boolean[][] filled = new boolean[NUM_ROWS][NUM_COLUMNS];
	private boolean game_over = false;
	
	private SavedGame save_state(){
		SavedGame save = new SavedGame(next_type, filled, tiles_list, future_queue, num_rows); 
		return save;
	}
	
	private void load_state(SavedGame save){
		next_type = save.getNext_type();
		num_rows = save.getNum_rows();
		tiles_list = new LinkedList<Tile>();
		Iterator<Tile> i = save.getTiles_list().iterator();
		while (i.hasNext()){
			tiles_list.addLast(i.next().copy());
		}
		future_queue = new LinkedList<TetType>();
		Iterator<TetType> it = save.getFuture_queue().iterator();
		while (it.hasNext()){
			future_queue.addLast(it.next());
		}
		boolean[][] filled_saved = save.getFilled();
		for (int r = 0; r < NUM_ROWS; r++){
			for (int c = 0; c < NUM_COLUMNS; c++){
				filled[r][c] = filled_saved[r][c];
			}
		}
	}
	
	@Override
	public void init() {
		setSize(WIDTH, HEIGHT);
		setBackground(Color.WHITE);
		setFocusable(true);
		Frame frame = (Frame) this.getParent().getParent();
		frame.setTitle("Tetrominos");
		addKeyListener(this);
		
		for (int i = 0; i < NUM_ROWS; i++){
			for (int j = 0; j < NUM_COLUMNS; j++){
				filled[i][j] = false;
			}
		}
	}
	
	@Override
	public void start(){
		generate_tetromino(false);
		Thread thread = new Thread(this);
		thread.start();
	}
	
	public double score_position(){
		int bumpiness = 0;
		int height = 0;
		int holes = 0;
		double total = 0;
		int last = 0;
		boolean empty_column = true;
		for (int c = 0; c < NUM_COLUMNS; c++){
			empty_column = true;
			for (int r = 0; r < NUM_ROWS; r++){
				if (filled[r][c]){
					if (empty_column){
						empty_column = false;
						if (c > 0){
							if (last - r != 0){
								bumpiness += Math.abs(last - r);
							}
						}
						last = r;
						
						// Add to the height
						height += NUM_ROWS - r;
					}
				} else {
					if (!empty_column){
						// found a hole!
						holes++;
					}
				}
			}
			if (empty_column){
				bumpiness += NUM_ROWS - last;
				last = NUM_ROWS;
			}
		}
		total = (-0.51 * height) + (0.76 * num_rows) + (-3.36 * holes) + (-0.18 * bumpiness); 
		return total;
	}
	
	private void AI_best_position(){
		int best_rot = 0;
		int best_x = 0;
		double best_score = -1000000;
		double score;
		SavedGame mainSave, midSave;
		// Save the current position
		for (int rot = 0; rot < 4; rot++){
			for (int x = -NUM_COLUMNS; x < NUM_COLUMNS; x++){
				num_rows = 0;
				mainSave = save_state();
				// Do rotations
				for (int z = 0; z < rot; z++){
					try_rotation();
				}
				// Do side movements
				if (x < 0){
					for (int z = 0; z < Math.abs(x); z++) {
						moveLeft();
					}
				} else if (x > 0){
					for (int z = 0; z < x; z++) {
						moveRight();
					}
				}
				// Drop to the bottom
				while (update_tiles(true)){
					continue;
				}
				
				// Find the best of the next tetromino
				for (int rot_sec = 0; rot_sec < 4; rot_sec++){
					for (int x_sec = -NUM_COLUMNS; x_sec < NUM_COLUMNS; x_sec++){
						midSave = save_state();
						// Do rotations
						for (int z = 0; z < rot_sec; z++){
							try_rotation();
						}
						// Do side movements
						if (x_sec < 0){
							for (int z = 0; z < Math.abs(x_sec); z++) {
								moveLeft();
							}
						} else if (x_sec > 0){
							for (int z = 0; z < x_sec; z++) {
								moveRight();
							}
						}
						// Drop to the bottom
						while (update_tiles(true)){
							continue;
						}
						
						// Check score
						score = score_position();
						if (score > best_score){
							// To show 'thought' process
//							repaint();
//							System.out.println(score);
//							try {
//								Thread.sleep(300);
//							} catch (InterruptedException e) {
//								e.printStackTrace();
//							}
							best_score = score;
							best_rot = rot;
							best_x = x;
						}
						load_state(midSave);
					}
				}
					
				// Reset position
				load_state(mainSave);
			}
		}
		//System.out.println("Score: " + best_score);
		num_rots = best_rot;
		num_xmoves = best_x;
		num_rows = 0;
		current_score = score_position();
	}
	
	private void AI_move(){
		if (num_rots > 0){
			try_rotation();
			num_rots--;
		} else if (num_xmoves < 0){
			moveLeft();
			num_xmoves++;
		} else if (num_xmoves > 0){
			moveRight();
			num_xmoves--;
		} else {
			update_tiles(false);
		}
	}
	
	private void generate_tetromino(boolean ai_run){
		// Decide on tetromino type and color.
		Color color = new Color(rand.nextInt(220), rand.nextInt(220), rand.nextInt(220));
		int x = 0;
		
		switch (next_type) {
		case I:
			// l type
			// Choose random staring x position.
			x = rand.nextInt(NUM_COLUMNS - 4) + 2;
			tiles_list.addFirst(new Tile(color, x, -4, 0, 1, true));
			tiles_list.addFirst(new Tile(color, x, -3, 0, 0, true));
			tiles_list.addFirst(new Tile(color, x, -2, 0, -1, true));
			tiles_list.addFirst(new Tile(color, x, -1, 0, -2, true));
			break;
			
		case T:
			// T type
			x = rand.nextInt(NUM_COLUMNS - 3) + 1;
			tiles_list.addFirst(new Tile(color, x, -1, 1, 0, true));
			tiles_list.addFirst(new Tile(color, x+1, -1, 0, 0, true));
			tiles_list.addFirst(new Tile(color, x+2, -1, -1, 0, true));
			tiles_list.addFirst(new Tile(color, x+1, -2, 0, 1, true));
			break;
			
		case O:
			// O
			x = rand.nextInt(NUM_COLUMNS - 1);
			tiles_list.addFirst(new Tile(color, x, -2, 0, 0, true));
			tiles_list.addFirst(new Tile(color, x+1, -2, 0, 0, true));
			tiles_list.addFirst(new Tile(color, x, -1, 0, 0, true));
			tiles_list.addFirst(new Tile(color, x+1, -1, 0, 0, true));
			break;
			
		case ZLEFT:
			// Z left
			x = rand.nextInt(NUM_COLUMNS - 3) + 1;
			tiles_list.addFirst(new Tile(color, x, -2, 1, 0, true));
			tiles_list.addFirst(new Tile(color, x+1, -2, 0, 0, true));
			tiles_list.addFirst(new Tile(color, x+1, -1, 0, -1, true));
			tiles_list.addFirst(new Tile(color, x+2, -1, -1, -1, true));
			break;
			
		case ZRIGHT:
			// Z right
			x = rand.nextInt(NUM_COLUMNS - 3) + 1;
			tiles_list.addFirst(new Tile(color, x, -1, 1, -1, true));
			tiles_list.addFirst(new Tile(color, x+1, -1, 0, -1, true));
			tiles_list.addFirst(new Tile(color, x+1, -2, 0, 0, true));
			tiles_list.addFirst(new Tile(color, x+2, -2, -1, 0, true));
			break;
			
		case LLEFT:
			// L left
			x = rand.nextInt(NUM_COLUMNS - 3) + 1;
			tiles_list.addFirst(new Tile(color, x, -2, 1, 1, true));
			tiles_list.addFirst(new Tile(color, x, -1, 1, 0, true));
			tiles_list.addFirst(new Tile(color, x+1, -1, 0, 0, true));
			tiles_list.addFirst(new Tile(color, x+2, -1, -1, 0, true));
			break;
			
		case LRIGHT:
			// L right
			x = rand.nextInt(NUM_COLUMNS - 3) + 1;
			tiles_list.addFirst(new Tile(color, x, -1, 1, 0, true));
			tiles_list.addFirst(new Tile(color, x+1, -1, 0, 0, true));
			tiles_list.addFirst(new Tile(color, x+2, -1, -1, 0, true));
			tiles_list.addFirst(new Tile(color, x+2, -2, -1, 1, true));
			break;

		default:
			break;
		}
		
		if (future_queue.isEmpty()){
			ArrayList<TetType> bag = new ArrayList<TetType>();
			bag.add(TetType.I);
			bag.add(TetType.LLEFT);
			bag.add(TetType.LRIGHT);
			bag.add(TetType.O);
			bag.add(TetType.T);
			bag.add(TetType.ZLEFT);
			bag.add(TetType.ZRIGHT);
			Collections.shuffle(bag);
			Iterator<TetType> i = bag.iterator();
			while(i.hasNext()){
				TetType t = i.next();
				future_queue.push(t);
			}
		}
		
		next_type = future_queue.pop();
		
		if (AI && !ai_run) {
			new_tet = true;
		}
	}
	
	private boolean update_tiles(boolean ai_run){
		
		if (!game_over){
			if (new_tet && !ai_run){
				AI_best_position();
				new_tet = false;
			}
			
			Iterator<Tile> i = tiles_list.iterator();
			LinkedList<Tile> moved = new LinkedList<Tile>();
			Boolean collision = false;
			while(i.hasNext()){
				Tile t = i.next();
				if (t.isMoving()){
					t.setY_pos(t.getY_pos() + 1);
					if (t.getY_pos() >= NUM_ROWS) collision = true;
					moved.add(t);
				} else {
					Iterator<Tile> mi = moved.iterator();
					while(mi.hasNext()){
						Tile tm = mi.next();
						if (t.getX_pos() == tm.getX_pos() && t.getY_pos() == tm.getY_pos()){
							collision = true;
						}
					}
				}
			}
			if (collision){
				Iterator<Tile> mi = moved.iterator();
				while(mi.hasNext()){
					Tile tm = mi.next();
					tm.setY_pos(tm.getY_pos() - 1);
					tm.setMoving(false);
					try {
						filled[tm.getY_pos()][tm.getX_pos()] = true;
					} catch (ArrayIndexOutOfBoundsException e){
						if (ai_run) {
							break;
						} else {
							game_over = true;
							return false;
						}
					}
				}
				checkForCompleteRow(ai_run);
				generate_tetromino(ai_run);
			}
			return !collision;
		} else {
			return false;
		}
	}
	
	private void try_rotation(){
		Iterator<Tile> i = tiles_list.iterator();
		LinkedList<Tile> moved = new LinkedList<Tile>();
		Boolean collision = false;
		while(i.hasNext()){
			Tile t = i.next();
			if (t.isMoving()){
				t.rotate_right();
				if (t.getY_pos() >= NUM_ROWS) collision = true;
				if (t.getX_pos() >= NUM_COLUMNS) collision = true;
				if (t.getX_pos() < 0) collision = true;
				moved.add(t);
			} else {
				Iterator<Tile> mi = moved.iterator();
				while(mi.hasNext()){
					Tile tm = mi.next();
					if (t.getX_pos() == tm.getX_pos() && t.getY_pos() == tm.getY_pos()){
						collision = true;
					}
				}
			}
		}
		if (collision){
			Iterator<Tile> mi = moved.iterator();
			while(mi.hasNext()){
				Tile tm = mi.next();
				tm.rotate_left();
			}
		}
	}
	
	private void checkForCompleteRow(boolean ai_run){
		boolean complete = true;
		for (int i = 0; i < NUM_ROWS; i++){
			complete = true;
			for (int j = 0; j < NUM_COLUMNS; j++){
				if (!filled[i][j]) complete = false; 
			}
			if (complete == true){
				// Complete row found!
				num_rows++;
				if (!ai_run) total_rows_cleared++;
				
				Iterator<Tile> ti = tiles_list.iterator();
				for (int n = 0; n < NUM_ROWS; n++) {
					for (int m = 0; m < NUM_COLUMNS; m++){
						filled[n][m] = false;
					}
				}
				while (ti.hasNext()){
					Tile t = ti.next();
					if (t.getY_pos() < i){
						t.setY_pos(t.getY_pos() + 1);
						filled[t.getY_pos()][t.getX_pos()] = true;
					} else if (t.getY_pos() == i){
						ti.remove();
					} else {
						filled[t.getY_pos()][t.getX_pos()] = true;
					}
				}
			}
		}
	}
	
	@Override
	public void run() {
		while(!game_over){
			
			if (AI){
				AI_move();
			}
			
			// Update everything
			update_tiles(false);
			
			repaint();
			
			try {
				Thread.sleep(speed);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		repaint();
	}
	
	@Override
	public void update(Graphics g) {
		if (image == null){
			image = createImage(this.getWidth(), this.getHeight());
			second = image.getGraphics();
		}
		
		second.setColor(getBackground());
		second.fillRect(0, 0, getWidth(), getHeight());
		second.setColor(getForeground());
		paint(second);
		
		g.drawImage(image, 0, 0, this);
	}
	
	@Override
	public void paint(Graphics g) {
		// Draw grid box.
		g.setColor(Color.BLACK);
		g.drawRect(30, 30, WIDTH/2 - 60, HEIGHT - 60);
		g.drawRect(WIDTH/2, 30, TILE_WIDTH * 5, TILE_HEIGHT * 5);
		if (AI){
			if (current_score < worst_score) worst_score = current_score;
			if (current_score > best_score) best_score = current_score;
			int base_level = 140;
			int extra = 255 - base_level;
			if (current_score > worst_score) {
				if (current_score < best_score){
					double diff = best_score - worst_score;
					double bad = best_score - current_score;
					g.setColor(new Color((int) (base_level + ((bad/diff)*extra)), (int) (base_level + ((1 - bad/diff)*extra)), base_level));
				} else {
					g.setColor(Color.GREEN);
				}
			} else {
				g.setColor(Color.RED);
			}
		} else {
			g.setColor(grid_background);
		}
		g.fillRect(31, 31, WIDTH/2 - 61, HEIGHT - 61);
		g.fillRect(WIDTH/2 + 1, 31, TILE_WIDTH * 5 - 1, TILE_HEIGHT * 5 - 1);
		
		// Draw filled places.
		g.setColor(Color.BLACK);
		for (int c = 0; c < NUM_COLUMNS; c++){
			for (int r = 0; r < NUM_ROWS; r++){
				if (filled[r][c]){
					g.fillRect(30 + TILE_WIDTH*c, 30 + TILE_HEIGHT*r, TILE_WIDTH, TILE_HEIGHT);
				}
			}
		}
		
		// Draw tiles from list.
		
		Iterator<Tile> i = tiles_list.iterator();
		while (i.hasNext()){
			Tile t = i.next();
			if (t.getY_pos() >= 0){
				g.setColor(t.getColor());
				g.fillRect(30 + TILE_WIDTH*t.getX_pos(), 30 + TILE_HEIGHT*t.getY_pos(), TILE_WIDTH, TILE_HEIGHT);
			}
		}
			
		// Draw next tetromino.
		g.setColor(Color.BLUE);
		int baseX = WIDTH/2;
		int baseY = 30;
		switch (next_type){
		case I:
			g.fillRect(baseX + TILE_WIDTH*2, baseY + ((int) TILE_HEIGHT/2), TILE_WIDTH, TILE_HEIGHT*4);
			break;
		case LLEFT:
			g.fillRect((int) (baseX + (TILE_WIDTH * 2.5)), baseY + TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT*3);
			g.fillRect((int) (baseX + (TILE_WIDTH * 1.5)), baseY + TILE_HEIGHT * 3, TILE_WIDTH, TILE_HEIGHT);
			break;
		case LRIGHT:
			g.fillRect((int) (baseX + TILE_WIDTH*1.5), baseY + TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT * 3);
			g.fillRect((int) (baseX + TILE_WIDTH*2.5), baseY + TILE_HEIGHT * 3, TILE_WIDTH, TILE_HEIGHT);
			break;
		case O:
			g.fillRect((int) (baseX + TILE_WIDTH*1.5), (int) (baseY + TILE_HEIGHT*1.5), TILE_WIDTH*2, TILE_HEIGHT*2);
			break;
		case T:
			g.fillRect(baseX + TILE_WIDTH*1, (int) (baseY + TILE_HEIGHT*1.5), TILE_WIDTH*3, TILE_HEIGHT);
			g.fillRect(baseX + TILE_WIDTH*2, (int) (baseY + TILE_HEIGHT*2.5), TILE_WIDTH, TILE_HEIGHT);
			break;
		case ZLEFT:
			g.fillRect(baseX + TILE_WIDTH*1, (int) (baseY + TILE_HEIGHT*1.5), TILE_WIDTH*2, TILE_HEIGHT);
			g.fillRect(baseX + TILE_WIDTH*2, (int) (baseY + TILE_HEIGHT*2.5), TILE_WIDTH*2, TILE_HEIGHT);
			break;
		case ZRIGHT:
			g.fillRect(baseX + TILE_WIDTH*2, (int) (baseY + TILE_HEIGHT*1.5), TILE_WIDTH*2, TILE_HEIGHT);
			g.fillRect(baseX + TILE_WIDTH*1, (int) (baseY + TILE_HEIGHT*2.5), TILE_WIDTH*2, TILE_HEIGHT);
			break;
		}
		
		// Draw total rows cleared
		g.setColor(Color.BLACK);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
		g.drawString(String.format("Rows Cleared: %d", total_rows_cleared), WIDTH/2, HEIGHT - 31);
		
		if (game_over){
			g.drawString("Game Over!", WIDTH/2, HEIGHT/2);
		}
	}
	
	private void moveRight(){
		Iterator<Tile> i = tiles_list.iterator();
		LinkedList<Tile> moved = new LinkedList<Tile>();
		Boolean collision = false;
		while(i.hasNext()){
			Tile t = i.next();
			if (t.isMoving()){
				t.setX_pos(t.getX_pos() + 1);
				if (t.getX_pos() >= NUM_COLUMNS) collision = true;
				moved.add(t);
			} else {
				Iterator<Tile> mi = moved.iterator();
				while(mi.hasNext()){
					Tile tm = mi.next();
					if (t.getX_pos() == tm.getX_pos() && t.getY_pos() == tm.getY_pos()){
						collision = true;
					}
				}
			}
		}
		if (collision){
			Iterator<Tile> mi = moved.iterator();
			while(mi.hasNext()){
				Tile tm = mi.next();
				tm.setX_pos(tm.getX_pos() - 1);
			}
		}
	}
	
	private void moveLeft(){
		Iterator<Tile> i = tiles_list.iterator();
		LinkedList<Tile> moved = new LinkedList<Tile>();
		Boolean collision = false;
		while(i.hasNext()){
			Tile t = i.next();
			if (t.isMoving()){
				t.setX_pos(t.getX_pos() - 1);
				if (t.getX_pos() < 0) collision = true;
				moved.add(t);
			} else {
				Iterator<Tile> mi = moved.iterator();
				while(mi.hasNext()){
					Tile tm = mi.next();
					if (t.getX_pos() == tm.getX_pos() && t.getY_pos() == tm.getY_pos()){
						collision = true;
					}
				}
			}
		}
		if (collision){
			Iterator<Tile> mi = moved.iterator();
			while(mi.hasNext()){
				Tile tm = mi.next();
				tm.setX_pos(tm.getX_pos() + 1);
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()){
		case (KeyEvent.VK_RIGHT):
			moveRight();
			repaint();
			break;
			
		case (KeyEvent.VK_LEFT):
			moveLeft();
			repaint();
			break;
		
		case (KeyEvent.VK_DOWN):
			update_tiles(false);
			repaint();
			break;
			
		case (KeyEvent.VK_UP):
			try_rotation();
			repaint();
			break;
		case (KeyEvent.VK_SPACE):
			while (update_tiles(false));
			repaint();
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
