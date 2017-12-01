package evacuacao;

import java.util.ArrayList;
import java.util.List;

import evacuacao.Human.myBehaviour;
import graph.Graph;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;
import sajas.core.Agent;
import sajas.core.behaviours.SimpleBehaviour;

public class Security extends Agent{
	private Grid<Object> grid;
	private boolean moved;
	private Context<Object> context;

	public Security(Grid<Object> grid, Context<Object> context) {
		this.grid = grid;
		this.context = context;
	}
	@Override
	public void setup() {
		addBehaviour(new myBehaviour(this));
	}
	//@ScheduledMethod(start = 1, interval = 1)
	class myBehaviour extends SimpleBehaviour {
		private static final long serialVersionUID = 1L;
		
		public myBehaviour(Agent a){
			super(a);
		}

		public void action(){
			GridCellNgh<Human> nghCreator = new GridCellNgh<Human>(grid, myLocation(), Human.class, 1, 1);
			List<GridCell<Human>> gridCells = nghCreator.getNeighborhood(true);
			SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
			
			
			List<Human> humans = new ArrayList<Human>();
			for (Object obj : grid.getObjects()) {
				if (obj instanceof Human) {
					humans.add((Human) obj);
				}
			}
			
			if(humans.size()==0)
				moveTowards(myLocation());
			System.out.println("At action "+humans.size());
		}

		@Override
		public boolean done() {
			List<Object> doors = new ArrayList<Object>();
			for (Object obj : grid.getObjectsAt(myLocation().getX(), myLocation().getY())) {
				if (obj instanceof Door) {
					doors.add(obj);
				}
			}

			if (doors.size() > 0) {
				System.out.println("Security Found Door -> " + myLocation().getX() + " : " + myLocation().getY());
				context.remove(this);
				List<Security> people = new ArrayList<Security>();
				for (Object obj : grid.getObjects()) {
					if (obj instanceof Security) {
						people.add((Security) obj);
					}
				}
				//Parameters params = RunEnvironment.getInstance().getParameters();
				//int securityCount = (Integer) params.getValue("security_count");
				if (people.size() == 0){
					RunEnvironment.getInstance().endRun();
					return true;
				}
				else return true;
			}
			return false;
		}
		
	}
		
	
	//@ScheduledMethod(start = 1, interval = 1)

	private GridPoint myLocation() {
		return grid.getLocation(this);
	}
	public void moveTowards(GridPoint pt) {
		double distToExit = 999999;
		int indexDoor = -1;

		// Move To Shortest Exit
		List<Door> doors = new ArrayList<Door>();
		for (Object obj : grid.getObjects()) {
			if (obj instanceof Door) {
				doors.add((Door) obj);
			}
		}
		if (doors.size() > 0) {
			for (int i = 0; i < doors.size(); i++) {
				double distVal = Math.hypot(myLocation().getX() - doors.get(i).getLocation().getX(), myLocation().getY() - doors.get(i).getLocation().getY());
				//System.out.println(myLocation().getX() + " " + myLocation().getY() + " " + doors.get(i).getLocation().getX() + " "
				//		+ doors.get(i).getLocation().getX() + " " + distVal);
				if (distVal < distToExit) {
					distToExit = distVal;
					indexDoor = i;
				}
			}
		}

		if (indexDoor > -1) {
			// Go To shortest Possible Direction

			GridPoint nextPoint = getNextPoint(pt, doors.get(indexDoor).getLocation());
			if(nextPoint != null){
				grid.moveTo(this, (int) nextPoint.getX(), (int) nextPoint.getY());
			}

		}
		setMoved(true);
	}
	private GridPoint getNextPoint(GridPoint pt, GridPoint location) {

		ArrayList<Graph.Edge> lgraph = new ArrayList<Graph.Edge>();

		for (int i = 0; i < grid.getDimensions().getWidth(); i++)
			for (int j = 0; j < grid.getDimensions().getHeight(); j++) {
				if (validPosition(i, j)) {
					// Try to add 8 Possible edge
					// -1- (i-1,j+1)
					if (validPosition(i - 1, j + 1)) {
						Graph.Edge nEdge = new Graph.Edge(
								"x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i - 1) + "y" + Integer.toString(j + 1)
								, 1
								, new GridPoint(i, j)
								, new GridPoint(i - 1, j + 1));
						lgraph.add(nEdge);
					}
					// -2- (i,j+1)
					if (validPosition(i, j + 1)) {
						Graph.Edge nEdge = new Graph.Edge(
								"x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i) + "y" + Integer.toString(j + 1),
								1, 
								new GridPoint(i, j), 
								new GridPoint(i, j + 1));
						lgraph.add(nEdge);
					}
					// -3- (i+1,j+1)
					if (validPosition(i + 1, j + 1)) {
						Graph.Edge nEdge = new Graph.Edge(
								"x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i + 1) + "y" + Integer.toString(j + 1),
								1, 
								new GridPoint(i, j), 
								new GridPoint(i + 1, j + 1));
						lgraph.add(nEdge);
					}
					// -4- (i-1,j)
					if (validPosition(i - 1, j)) {
						Graph.Edge nEdge = new Graph.Edge(
								"x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i - 1) + "y" + Integer.toString(j),
								1, 
								new GridPoint(i, j), 
								new GridPoint(i - 1, j));
						lgraph.add(nEdge);
					}
					// -5- (i+1,j)
					if (validPosition(i + 1, j)) {
						Graph.Edge nEdge = new Graph.Edge(
								"x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i + 1) + "y" + Integer.toString(j), 
								1, 
								new GridPoint(i, j), 
								new GridPoint(i + 1, j));
						lgraph.add(nEdge);
					}
					// -6- (i-1,j-1)
					if (validPosition(i - 1, j - 1)) {
						Graph.Edge nEdge = new Graph.Edge(
								"x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i - 1) + "y" + Integer.toString(j - 1),
								1, 
								new GridPoint(i, j), 
								new GridPoint(i - 1, j - 1));
						lgraph.add(nEdge);
					}
					// -7- (i,j-1)
					if (validPosition(i, j - 1)) {
						Graph.Edge nEdge = new Graph.Edge(
								"x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i) + "y" + Integer.toString(j - 1),
								1, 
								new GridPoint(i, j), 
								new GridPoint(i, j - 1));
						lgraph.add(nEdge);
					}
					// -8- (i+1,j-1)
					if (validPosition(i + 1, j - 1)) {
						Graph.Edge nEdge = new Graph.Edge(
								"x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i + 1) + "y" + Integer.toString(j - 1), 
								1, 
								new GridPoint(i, j), 
								new GridPoint(i + 1, j - 1));
						lgraph.add(nEdge);
					}
				}
			}

		Graph.Edge[] GRAPH = new Graph.Edge[lgraph.size()];
		GRAPH = lgraph.toArray(GRAPH);
		
		final String START = "x" + Integer.toString(pt.getX()) + "y" + Integer.toString(pt.getY());
		final String END = "x" + Integer.toString(location.getX()) + "y" + Integer.toString(location.getY());

		Graph g = new Graph(GRAPH);
		g.dijkstra(START);
		GridPoint nextPoint = g.getNextPoint(START, END);
		return nextPoint;
	}
	private boolean validPosition(int i, int j) {
		if (i < 0 || j < 0)
			return false;
		if (i >= grid.getDimensions().getWidth())
			return false;
		if (j >= grid.getDimensions().getHeight())
			return false;
		for (Object obj : grid.getObjectsAt(i, j)) {
			if (obj instanceof Wall) {
				return false;
			}
		}
		return true;
	}
	public boolean isMoved() {
		return moved;
	}
	public void setMoved(boolean moved) {
		this.moved = moved;
	}

}
