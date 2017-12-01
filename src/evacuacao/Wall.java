package evacuacao;

import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Wall {
	private Grid<Object> grid;
	public Wall(Grid<Object> grid) {
		this.grid = grid;
	}
	
	public GridPoint getLocation() {
		return grid.getLocation(this);
	}

}
