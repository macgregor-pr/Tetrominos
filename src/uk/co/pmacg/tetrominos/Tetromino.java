package uk.co.pmacg.tetrominos;

public class Tetromino {
	private Tile[] tiles;
	private TetType type;
	
	public Tetromino(Tile[] tiles, TetType type) {
		super();
		this.tiles = tiles;
		this.type = type;
	}

	public Tile[] getTiles() {
		return tiles;
	}

	public TetType getType() {
		return type;
	}

	public void setTiles(Tile[] tiles) {
		this.tiles = tiles;
	}

	public void setType(TetType type) {
		this.type = type;
	}
}
