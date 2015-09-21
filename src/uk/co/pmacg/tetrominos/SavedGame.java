package uk.co.pmacg.tetrominos;

import java.util.Iterator;
import java.util.LinkedList;

public class SavedGame {
	
	private TetType next_type;
	private LinkedList<Tile> tiles_list;
	private LinkedList<TetType> future_queue;
	private boolean[][] filled = new boolean[StartingClass.NUM_ROWS][StartingClass.NUM_COLUMNS];
	private int num_rows;
	
	public SavedGame(TetType next_type, boolean[][] filled, LinkedList<Tile> tiles_list, LinkedList<TetType> future_queue, int num_rows) {
		this.next_type = next_type;
		this.num_rows = num_rows;
		
		this.tiles_list = new LinkedList<Tile>();
		Iterator<Tile> i = tiles_list.iterator();
		while(i.hasNext()){
			this.tiles_list.addLast(i.next().copy());
		}
		this.future_queue = new LinkedList<TetType>();
		Iterator<TetType> it = future_queue.iterator();
		while (it.hasNext()){
			this.future_queue.addLast(it.next());
		}
		
		for (int r = 0; r < StartingClass.NUM_ROWS; r++){
			for (int c = 0; c < StartingClass.NUM_COLUMNS; c++){
				this.filled[r][c] = filled[r][c];
			}
		}
	}
	
	public int getNum_rows(){
		return num_rows;
	}

	public TetType getNext_type() {
		return next_type;
	}

	public LinkedList<Tile> getTiles_list() {
		return tiles_list;
	}

	public LinkedList<TetType> getFuture_queue() {
		return future_queue;
	}

	public boolean[][] getFilled() {
		return filled;
	}

}
