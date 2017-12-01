package evacuacao;

import java.util.ArrayList;
import java.util.List;
import graph.Graph;
import sajas.core.Agent;
import sajas.core.behaviours.SimpleBehaviour;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;
 enum State {
    inRoom,wandering,knowExit 
 }
 enum Condition{
	 healthy,injured,saved
 }
 enum Zones{
	 topWall,bottomWall,RightWall,topRight,topLeft,bottomLeft,bottomRight,nowhere
 }
public class Human extends Agent{
	private Grid<Object> grid;
	private boolean moved;
	private Context<Object> context;
	private State state;
	private Condition condition;
	private float altruism;
	private ArrayList<Zones> explored = new ArrayList<Zones>();
	private Zones nextZone = Zones.nowhere;
	private Zones fromZone = Zones.nowhere;
	private int visionRadius;
	private int exitX;
	private int exitY;
	
	public Human(Grid<Object> grid, Context<Object> context,State state,Condition condition,float altruism, int visionRadius) {
		this.grid = grid;
		this.context=context;
		this.state=state;
		this.condition=condition;
		this.altruism=altruism;
		this.visionRadius=visionRadius;
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
			if(myLocation().getX()>grid.getDimensions().getWidth() - 21 && state!=State.knowExit)
				state= State.wandering;
			//lookup in visionRadius to find exit or security guard
			vision(myLocation());
			
			switch(state){
			case inRoom:
				moveTowards(myLocation());
				break;
			case wandering:
				moveExplore(myLocation());
				
				break;
			case knowExit:
				moveToExit(myLocation());
				break;
			}
		}

		@Override
		public boolean done() {
			if(checkDoorAtLocation(myLocation().getX(),myLocation().getY())){
				context.remove(this);
				return true;
			}
			return false;
		}
		
	}
	
	
	
	/*
	 * Functions as agent's "vision" detecting exits or security guards in a circle with radius of visionRadius parameter
	 */
	private boolean vision(GridPoint pt) {
		int i = pt.getX();
		int j = pt.getY();
		for (int iter = 1; iter <= this.visionRadius; iter++) {
			if (validPosition(i, j + iter)) {
				if(checkDoorAtLocation(i, j + iter)){
					exitX=i;
					exitY=j+iter;
					state=State.knowExit;
					return true;
				}
				//check security guard and ask for exit location
			}
			if (validPosition(i + iter, j + iter)) {
				if(checkDoorAtLocation(i + iter, j + iter)){
					exitX=i+iter;
					exitY=j+iter;
					state=State.knowExit;
					return true;
				}
				//check security guard and ask for exit location
			}
			if (validPosition(i + iter, j)) {
				if(checkDoorAtLocation(i+iter, j)){
					exitX=i+iter;
					exitY=j;
					state=State.knowExit;
					return true;
				}
				//check security guard and ask for exit location
			}
			if (validPosition(i, j - iter)) {
				if(checkDoorAtLocation(i, j - iter)){
					exitX=i;
					exitY=j-iter;
					state=State.knowExit;
					return true;
				}
				//check security guard and ask for exit location
			}
			if (validPosition(i + iter, j - iter)) {
				if(checkDoorAtLocation(i + iter, j - iter)){
					exitX=i+iter;
					exitY=j-iter;
					state=State.knowExit;
					return true;
				}
				//check security guard and ask for exit location
			}
			if (validPosition(i - iter, j)) {
				if(checkDoorAtLocation(i - iter, j)){
					exitX=i-iter;
					exitY=j;
					state=State.knowExit;
					return true;
				}
				//check security guard and ask for exit location
			}
			if (validPosition(i - iter, j - iter)) {
				if(checkDoorAtLocation(i - iter, j - iter)){
					exitX=i-iter;
					exitY=j-iter;
					state=State.knowExit;
					return true;
				}
				//check security guard and ask for exit location
			}
			if (validPosition(i - iter, j + iter)) {
				if(checkDoorAtLocation(i - iter, j + iter)){
					exitX=i-iter;
					exitY=j+iter;
					state=State.knowExit;
					return true;
				}
				//check security guard and ask for exit location
			}
		}
		return false;
	}
	
	
	private boolean checkDoorAtLocation(int x, int y){
		List<Object> doors = new ArrayList<Object>();
		for (Object obj : grid.getObjectsAt(x, y)) {
			if (obj instanceof Door) {
				doors.add(obj);
			}
		}

		if (doors.size() > 0) {
			return true;
		}
		return false;
	}
	private GridPoint myLocation() {

		return grid.getLocation(this);
	}
	public Zones currentZone(int x, int y){
		if(x>=20 && x<=22){
			if(y>=1 && y<=3)
				return Zones.bottomLeft;
			if(y<=this.grid.getDimensions().getHeight()-2 && y>=this.grid.getDimensions().getHeight()-4)
				return Zones.topLeft;
		}
		if(x>=25 && x<=33){
			if(y>=1 && y<=3)
				return Zones.bottomWall;
			if(y>=this.grid.getDimensions().getHeight()-4 && y<=this.grid.getDimensions().getHeight()-2)
				return Zones.topWall;
		}
		if(x>=this.grid.getDimensions().getWidth()-4 && x<=this.grid.getDimensions().getWidth()-2){
			if(y>=1 && y<=3)
				return Zones.bottomRight;
			if(y>=this.grid.getDimensions().getHeight()-4 && y<=this.grid.getDimensions().getHeight()-2)
				return Zones.topRight;
			if(y>=6 && y<=18){
				return Zones.RightWall;
			}
			
		}	
		return Zones.nowhere;
	}
	public void nextZone(Zones currentZone){
		ArrayList<Zones> possibleZones = new ArrayList<Zones>();
		switch(currentZone){
			case RightWall:
				if(!explored.contains(Zones.bottomRight)){
					possibleZones.add(Zones.bottomRight);
				}
				if(!explored.contains(Zones.topRight)){
					possibleZones.add(Zones.topRight);
				}
				if(!possibleZones.isEmpty()){
					int zone_index = RandomHelper.nextIntFromTo(0,possibleZones.size()-1);
					nextZone=possibleZones.get(zone_index);
					fromZone=Zones.RightWall;
				}
				else nextZone = Zones.nowhere;
				break;
			case bottomLeft:
				if(!explored.contains(Zones.bottomRight)){
					possibleZones.add(Zones.bottomRight);
				}
				if(!explored.contains(Zones.topLeft)){
					possibleZones.add(Zones.topLeft);
				}
				if(!explored.contains(Zones.bottomWall)){
					possibleZones.add(Zones.bottomWall);
				}
				if(!possibleZones.isEmpty()){
					int zone_index = RandomHelper.nextIntFromTo(0,possibleZones.size()-1);
					nextZone=possibleZones.get(zone_index);
					fromZone=Zones.bottomLeft;
				}
				else nextZone = Zones.nowhere;
				break;
			case bottomRight:
				if(!explored.contains(Zones.topRight)){
					possibleZones.add(Zones.topRight);
				}
				if(!explored.contains(Zones.bottomLeft)){
					possibleZones.add(Zones.bottomLeft);
				}
				if(!explored.contains(Zones.bottomWall)){
					possibleZones.add(Zones.bottomWall);
				}
				if(!possibleZones.isEmpty()){
					int zone_index = RandomHelper.nextIntFromTo(0,possibleZones.size()-1);
					nextZone=possibleZones.get(zone_index);
					fromZone=Zones.bottomRight;
				}
				else nextZone = Zones.nowhere;
				break;
			case bottomWall:
				if(!explored.contains(Zones.bottomRight)){
					possibleZones.add(Zones.bottomRight);
				}
				if(!explored.contains(Zones.bottomLeft)){
					possibleZones.add(Zones.bottomLeft);
				}
				if(!explored.contains(Zones.topWall)){
					possibleZones.add(Zones.topWall);
				}
				if(!possibleZones.isEmpty()){
					int zone_index = RandomHelper.nextIntFromTo(0,possibleZones.size()-1);
					nextZone=possibleZones.get(zone_index);
					fromZone=Zones.bottomWall;
				}
				else nextZone = Zones.nowhere;
				break;
			case topLeft:
				if(!explored.contains(Zones.topRight)){
					possibleZones.add(Zones.topRight);
				}
				if(!explored.contains(Zones.bottomLeft)){
					possibleZones.add(Zones.bottomLeft);
				}
				if(!explored.contains(Zones.topWall)){
					possibleZones.add(Zones.topWall);
				}
				if(!possibleZones.isEmpty()){
					int zone_index = RandomHelper.nextIntFromTo(0,possibleZones.size()-1);
					nextZone=possibleZones.get(zone_index);
					fromZone=Zones.topLeft;
				}
				else nextZone = Zones.nowhere;
				break;
			case topRight:
				if(!explored.contains(Zones.topLeft)){
					possibleZones.add(Zones.topLeft);
				}
				if(!explored.contains(Zones.bottomRight)){
					possibleZones.add(Zones.bottomRight);
				}
				if(!explored.contains(Zones.topWall)){
					possibleZones.add(Zones.topWall);
				}
				if(!possibleZones.isEmpty()){
					int zone_index = RandomHelper.nextIntFromTo(0,possibleZones.size()-1);
					nextZone=possibleZones.get(zone_index);
					fromZone=Zones.topRight;
				}
				else nextZone = Zones.nowhere;
				break;
			case topWall:
				if(!explored.contains(Zones.topRight)){
					possibleZones.add(Zones.topRight);
				}
				if(!explored.contains(Zones.topLeft)){
					possibleZones.add(Zones.topLeft);
				}
				if(!explored.contains(Zones.bottomWall)){
					possibleZones.add(Zones.bottomWall);
				}
				if(!possibleZones.isEmpty()){
					int zone_index = RandomHelper.nextIntFromTo(0,possibleZones.size()-1);
					nextZone=possibleZones.get(zone_index);
					fromZone=Zones.topWall;
				}
				else nextZone = Zones.nowhere;
				break;
				case nowhere:
					break;
				default:
					break;
		
		}
	}
	public void moveToZone(int i, int j){
		switch(this.nextZone){
		case RightWall:
			if(this.fromZone==Zones.bottomRight){
				if(!moveUp(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.topRight){
				if(!moveDown(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			break;
		case bottomLeft:
			if(this.fromZone==Zones.bottomRight){
				if(!moveLeft(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.topLeft){
				if(!moveDown(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.bottomWall){
				if(!moveLeft(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			break;
		case bottomRight:
			if(this.fromZone==Zones.topRight){
				if(!moveDown(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.bottomLeft){
				if(!moveRight(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			
			if(this.fromZone==Zones.bottomWall){
				if(!moveRight(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.RightWall){
				if(moveDown(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			break;
		case bottomWall:
			if(this.fromZone==Zones.bottomRight){
				if(!moveLeft(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.bottomLeft){
				if(!moveRight(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.topWall){
				if(!moveDown(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			break;
		case topLeft:
			if(this.fromZone==Zones.topRight){
				if(!moveLeft(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.bottomLeft){
				if(!moveUp(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.topWall){
				if(!moveLeft(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			break;
		case topRight:
			if(this.fromZone==Zones.topLeft){
				if(!moveRight(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.RightWall){
				if(!moveUp(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.bottomRight){
				if(!moveUp(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.topWall){
				if(!moveRight(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			break;
		case topWall:
			if(this.fromZone==Zones.topRight){
				if(!moveDown(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.topLeft){
				if(!moveRight(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			if(this.fromZone==Zones.bottomWall){
				if(!moveUp(i,j)){
					this.nextZone=Zones.nowhere;
				}
			}
			break;
			case nowhere:
				break;
			default:
				break;
		
		}
	}
	public void moveExplore(GridPoint pt) {	
		int i = pt.getX();
		int j = pt.getY();
		Zones currentZone = currentZone(myLocation().getX(),myLocation().getY());
		if(!explored.contains(currentZone))
			explored.add(currentZone);
		if(this.nextZone==currentZone && this.nextZone!=Zones.nowhere){
			nextZone(currentZone);
		}
		if(this.nextZone!=currentZone && this.nextZone!=Zones.nowhere){
			moveToZone(i,j);
		}
		if(this.nextZone==Zones.nowhere){
			
			if(currentZone != Zones.nowhere){
				nextZone(currentZone);
				moveToZone(i,j);
				return;
			}
			
			ArrayList<GridPoint> possibleMoves = new ArrayList<GridPoint>();
			
			if (validPosition(i, j + 1)) {
				possibleMoves.add(new GridPoint(i, j+1));
			}
			if (validPosition(i + 1, j + 1)) {
				possibleMoves.add(new GridPoint(i+1, j+1));
			}
			if (validPosition(i + 1, j)) {
				possibleMoves.add(new GridPoint(i+1, j));
			}
			if (validPosition(i, j - 1)) {
				possibleMoves.add(new GridPoint(i, j-1));
			}
	
			if (validPosition(i + 1, j - 1)) {
				possibleMoves.add(new GridPoint(i+1, j-1));
			}
			
			if(!possibleMoves.isEmpty()){
				int move_index = RandomHelper.nextIntFromTo(0,possibleMoves.size()-1);
				grid.moveTo(this, possibleMoves.get(move_index).getX(), possibleMoves.get(move_index).getY());
				setMoved(true);
			}
			
			
		}
		
	}
	public void moveToExit(GridPoint pt) {
		GridPoint nextPoint = getNextPoint(pt, new GridPoint(this.exitX,this.exitY));
		if(nextPoint != null){
			grid.moveTo(this, (int) nextPoint.getX(), (int) nextPoint.getY());
		}
		setMoved(true);
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
		//g.printPath(END);
		//g.printAllPaths();
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
	public boolean moveLeft(int i, int j){
		if (validPosition(i-1, j)) {
			grid.moveTo(this,i-1, j);
			setMoved(true);
			return true;
		}
		return false;
	}
	public boolean moveUp(int i, int j){
		if (validPosition(i, j+1)) {
			grid.moveTo(this,i, j+1);
			setMoved(true);
			return true;
		}
		return false;
	}
	public boolean moveDown(int i, int j){
		if (validPosition(i, j-1)) {
			grid.moveTo(this,i, j-1);
			setMoved(true);
			return true;
		}
		return false;
	}
	public boolean moveRight(int i, int j){
		if (validPosition(i+1, j)) {
			grid.moveTo(this,i+1, j);
			setMoved(true);
			return true;
		}
		return false;
	}
	public boolean isMoved() {
		return moved;
	}
	public void setMoved(boolean moved) {
		this.moved = moved;
	}
}
