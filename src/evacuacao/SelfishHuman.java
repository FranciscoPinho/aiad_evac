package evacuacao;

import repast.simphony.context.Context;
import repast.simphony.space.grid.Grid;

public class SelfishHuman extends Human {

	public SelfishHuman(Grid<Object> grid, Context<Object> context, State state, Condition condition, 
			int visionRadius, int fireInjuryRadius) {
		super(grid, context, state, condition, false, visionRadius, fireInjuryRadius);
		
	}

}
