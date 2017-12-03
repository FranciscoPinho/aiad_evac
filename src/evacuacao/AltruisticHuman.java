package evacuacao;

import repast.simphony.context.Context;
import repast.simphony.space.grid.Grid;

public class AltruisticHuman extends Human {

	public AltruisticHuman(Grid<Object> grid, Context<Object> context, State state, Condition condition,
			int visionRadius, int fireInjuryRadius) {
		super(grid, context, state, condition, true, visionRadius, fireInjuryRadius);
	}

}
