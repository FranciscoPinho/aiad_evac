package evacuacao;

import sajas.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import jade.core.AID;
import sajas.sim.repasts.RepastSLauncher;
import sajas.wrapper.ContainerController;
import sajas.core.Runtime;

public class RepastSEvacuationLauncher extends RepastSLauncher {
	public static final boolean USE_RESULTS_COLLECTOR = true;
	private ContainerController mainContainer;
	private Context<Object> context;
	private Grid<Object> grid;
	
	public static Agent getAgent(Context<?> context, AID aid) {
		for(Object obj : context.getObjects(Agent.class)) {
			if(((Agent) obj).getAID().equals(aid)) {
				return (Agent) obj;
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return "Evacuation -- SAJaS RepastS";
	}

	@Override
	protected void launchJADE() {
		Runtime rt = Runtime.instance();
		Profile p1 = new ProfileImpl();
		mainContainer = rt.createMainContainer(p1);
		launchAgents();
	}
	
	private void launchAgents() {
		AID resultsCollectorAID = null;
		if(USE_RESULTS_COLLECTOR) {
			// create results collector
			//ResultsCollector resultsCollector = new ResultsCollector(N_CONSUMERS + N_CONSUMERS_FILTERING_PROVIDERS);
			//mainContainer.acceptNewAgent("ResultsCollector", resultsCollector).start();
			//resultsCollectorAID = resultsCollector.getAID();
		}
		Parameters params = RunEnvironment.getInstance().getParameters();
		buildWalls(grid, context);
		int humanCount = (Integer) params.getValue("human_count");
		int securityCount = (Integer) params.getValue("security_count");
		int doorsCount = (Integer) params.getValue("doors_count");
		int radiusVision = (Integer) params.getValue("radius_vision");
		int prob = (Integer) params.getValue("propagation_prob");
		int injuryRadius = (Integer) params.getValue("fire_injury_radius");
		int altper = (Integer) params.getValue("altruistic_probability");

		generateExits(grid,context,doorsCount);
		createHumans(grid,context,humanCount,radiusVision,injuryRadius,altper);
		createSecurity(grid,context,securityCount);
		startAccident(grid,context,0,0,prob);
	}

	private void createHumans(Grid<Object> grid, Context<Object> context, int humanCount, int radiusVision, int injuryRadius, int altruisticPercent){
		for (int i = 0; i < humanCount; i++) {
			int altruism_prob = RandomHelper.nextIntFromTo(1,100);
			Human newHuman;
			if(altruism_prob<=altruisticPercent)
				newHuman = new AltruisticHuman(grid, context,State.inRoom,Condition.healthy,radiusVision,injuryRadius);
			else newHuman = new SelfishHuman(grid, context,State.inRoom,Condition.healthy,radiusVision,injuryRadius);
			context.add(newHuman);
			int startX = RandomHelper.nextIntFromTo(1, grid.getDimensions().getWidth() - 22);
			int startY = RandomHelper.nextIntFromTo(1, grid.getDimensions().getHeight() - 2);
			while (!isValidPosition(startX, startY, grid)) {
				startX = RandomHelper.nextIntFromTo(1, grid.getDimensions().getWidth() - 22);
				startY = RandomHelper.nextIntFromTo(1, grid.getDimensions().getHeight() - 2);
			}
			grid.moveTo(newHuman, startX, startY);
			try {
				mainContainer.acceptNewAgent(newHuman.getClass().getSimpleName()+"Student " + i, newHuman).start();
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void startAccident(Grid<Object> grid, Context<Object> context,int x,int y,int propagationProb){
			int startX = RandomHelper.nextIntFromTo(1, 9);
			int startY = RandomHelper.nextIntFromTo(1, grid.getDimensions().getHeight() - 2);
			while (!isValidPosition(startX, startY, grid) || (startY>=9 && startY<=16)) {
				startX = RandomHelper.nextIntFromTo(1, 9);
				startY = RandomHelper.nextIntFromTo(1, grid.getDimensions().getHeight() - 2);
			}
			new Fire(grid,context,startX,startY,propagationProb);
	}
	
	private void createSecurity(Grid<Object> grid, Context<Object> context, int securityCount){
		for (int i = 0; i < securityCount; i++) {
			Security newSecurity = new Security(grid, context);
			context.add(newSecurity);
			int startX = RandomHelper.nextIntFromTo(grid.getDimensions().getWidth() - 20, grid.getDimensions().getWidth() - 1);
			int startY = RandomHelper.nextIntFromTo(1, grid.getDimensions().getHeight() - 2);
			while (!isValidPosition(startX, startY, grid)) {
				startX = RandomHelper.nextIntFromTo(grid.getDimensions().getWidth() - 20, grid.getDimensions().getWidth() - 1);
				startY = RandomHelper.nextIntFromTo(1, grid.getDimensions().getHeight() - 2);
			}
			grid.moveTo(newSecurity, startX, startY);
			try {
				mainContainer.acceptNewAgent("security" + i, newSecurity).start();
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void buildWalls(Grid<Object> grid, Context<Object> context) {

		// LEFT WALL
		for (int i = 0; i < grid.getDimensions().getHeight(); i++) {
			Wall wall = new Wall(grid);
			context.add(wall);
			grid.moveTo(wall, 0, i);

		}
		// RIGHT WALL
		for (int i = 0; i < grid.getDimensions().getHeight(); i++) {
			Wall wall = new Wall(grid);
			context.add(wall);
			grid.moveTo(wall, grid.getDimensions().getWidth() - 1, i);

		}
		// TOP WALL
		for (int i = 1; i < grid.getDimensions().getWidth(); i++) {
			Wall wall = new Wall(grid);
			context.add(wall);
			grid.moveTo(wall, i, grid.getDimensions().getHeight() - 1);

		}
		// BOTTOM WALL
		for (int i = 1; i < grid.getDimensions().getWidth(); i++) {
			Wall wall = new Wall(grid);
			context.add(wall);
			grid.moveTo(wall, i, 0);
		}
	

		for (int i = 1; i < grid.getDimensions().getHeight() - 1; i++) {
			if (i != 3 && i != 12) {
				Wall wall = new Wall(grid);
				context.add(wall);
				grid.moveTo(wall, 10, i);
			}

		}

		for (int i = 1; i < grid.getDimensions().getHeight() - 1; i++) {
			if (i != 8 && i != 20) {
				Wall wall = new Wall(grid);
				context.add(wall);
				grid.moveTo(wall, 19, i);
			}

		}

		for (int i = 1; i < grid.getDimensions().getWidth() - 20; i++) {
			if (i != 8) {
				Wall wall = new Wall(grid);
				context.add(wall);
				grid.moveTo(wall, i, 9);
			}

		}

		for (int i = 1; i < grid.getDimensions().getWidth() - 20; i++) {
			if (i != 2 && i != 8 && i != 14) {
				Wall wall = new Wall(grid);
				context.add(wall);
				grid.moveTo(wall, i, 16);
			}

		}

	}
	
	private void generateExits(Grid<Object> grid, Context<Object> context,int doorsCount){
		while (doorsCount > 0) {
			double chance = RandomHelper.nextDoubleFromTo(0,1);
			int doorExitX = 0,doorExitY= 0;
			//right wall
			if(chance <= 0.33){
				doorExitX = grid.getDimensions().getWidth() - 1;
				doorExitY = RandomHelper.nextIntFromTo(1, grid.getDimensions().getHeight() - 2);
			}
			//top wall
			else if(chance <= 0.66){
				doorExitX = RandomHelper.nextIntFromTo(grid.getDimensions().getWidth() - 20, grid.getDimensions().getWidth() - 2);
				doorExitY = grid.getDimensions().getHeight() - 1;
			}
			//bottom wall
			else if(chance <= 1){
				doorExitX =	RandomHelper.nextIntFromTo(grid.getDimensions().getWidth() - 20, grid.getDimensions().getWidth() - 2);
				doorExitY = 0;
			}
			Door exitDoor = new Door(grid);
			context.add(exitDoor);
			grid.moveTo(exitDoor, doorExitX, doorExitY);
			for (Object obj : grid.getObjectsAt(doorExitX, doorExitY)) {
				if (obj instanceof Wall) {
					context.remove(obj);
				}
				//if a door already exists at that location - try again
				if (obj instanceof Door){
					context.remove(exitDoor);
					continue;
				}
			
			}	
			
			doorsCount--;

		}
	}
	
	private boolean isValidPosition(int startX, int startY, Grid<Object> grid) {
		if (startX < 0 || startY < 0)
			return false;
		if (startX >= grid.getDimensions().getWidth())
			return false;
		if (startY >= grid.getDimensions().getHeight())
			return false;
		for (Object obj : grid.getObjectsAt(startX, startY)) {
			if (obj instanceof Wall) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public Context<?>build(Context<Object> context) {
		// http://repast.sourceforge.net/docs/RepastJavaGettingStarted.pdf
		
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new WrapAroundBorders(), new SimpleGridAdder<Object>(), true, 40, 25));
		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("Help Request Network", context , true );
		netBuilder.buildNetwork();

		this.grid=grid;
		this.context=context;
		return super.build(this.context);
	}

}
