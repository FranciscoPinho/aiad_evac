package evacuacao;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

public class Fire {
	private Grid<Object> grid;
	private Context<Object> context;
	private int propagationProb;
	private int propagationInflation;
	public Fire(Grid<Object> grid,Context<Object> context, int x, int y,int propagationProb) {
		this.grid = grid;
		this.context=context;
		this.propagationProb=propagationProb;
		this.context.add(this);
		this.grid.moveTo(this,x,y);
		this.propagationInflation=20;
	}
	public Fire(Grid<Object> grid,Context<Object> context, int x, int y,int propagationProb,int propagationInflation) {
		this.grid = grid;
		this.context=context;
		this.propagationProb=propagationProb;
		this.propagationInflation=propagationInflation;
		this.context.add(this);
		this.grid.moveTo(this,x,y);
	}
	@ScheduledMethod(start = 5, interval = 6)
	public void step() {
		int prob= RandomHelper.nextIntFromTo(0,100);
		if(prob<=propagationProb+propagationInflation){
			if(propagationInflation>0)
				propagationInflation-=10;
			propagate();
		}
	}
	public GridPoint getLocation() {
		return grid.getLocation(this);
	}
	
	private void propagate() {
		int i = getLocation().getX();
		int j = getLocation().getY();

		if (validPosition(i, j + 1)) {
			new Fire(grid, context, i, j + 1,propagationProb,propagationInflation);
		}
		if (validPosition(i + 1, j + 1)) {
			new Fire(grid, context, i + 1, j + 1,propagationProb,propagationInflation);
		}
		if (validPosition(i + 1, j)) {
			new Fire(grid, context, i + 1, j,propagationProb,propagationInflation);
		}
		if (validPosition(i, j - 1)) {
			new Fire(grid, context, i, j - 1,propagationProb,propagationInflation);
		}

		if (validPosition(i + 1, j - 1)) {
			new Fire(grid, context, i + 1, j - 1,propagationProb,propagationInflation);
		}

		if (validPosition(i - 1, j - 1)) {
			new Fire(grid, context, i - 1, j - 1,propagationProb,propagationInflation);
		}

		if (validPosition(i - 1, j + 1)) {
			new Fire(grid, context, i - 1, j + 1,propagationProb,propagationInflation);
		}

		if (validPosition(i - 1, j)) {
			new Fire(grid, context, i - 1, j,propagationProb,propagationInflation);
		}

	}

	private boolean validPosition(int i, int j) {
		if (i < 0 || j < 0)
			return false;
		if (i >= grid.getDimensions().getWidth())
			return false;
		if (j >= grid.getDimensions().getHeight())
			return false;
		for (Object obj : grid.getObjectsAt(i, j)) {
			if (obj instanceof Wall || obj instanceof Fire) {
				return false;
			}
		}
		return true;
	}

}
