package evacuacao;

import java.util.ArrayList;
import java.util.List;

import evacuacao.ontology.EvacuationOntology;
import evacuacao.ontology.ExitRequest;
import evacuacao.ontology.RunToExit;
import graph.Graph;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.SimpleBehaviour;



public class Security extends Agent{
	private Grid<Object> grid;
	private boolean moved;
	private int closestExitX,closestExitY=0;
	private Context<Object> context;
	private Codec codec;
	private Ontology evacOntology;
	int dead = 0;

	public Security(Grid<Object> grid, Context<Object> context) {
		super();
		this.grid = grid;
		this.context = context;
	}
	@SuppressWarnings("unchecked")
	@Override
	public void setup() {
		// register language and ontology
		codec = new SLCodec();
		evacOntology = EvacuationOntology.getInstance();
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(evacOntology);
		addBehaviour(new movementBehaviour(this));
		addBehaviour(new answerDoorCoordinateRequests(this));
	}

	class movementBehaviour extends SimpleBehaviour {
		private static final long serialVersionUID = 1L;
		
		public movementBehaviour(Agent a){
			super(a);
		}
	
		public void action(){
			
			/*GridCellNgh<Human> nghCreator = new GridCellNgh<Human>(grid, myLocation(), Human.class, 1, 1);
			List<GridCell<Human>> gridCells = nghCreator.getNeighborhood(true);
			SimUtilities.shuffle(gridCells, RandomHelper.getUniform());*/
	
			
			
			List<Human> humans = new ArrayList<Human>();
			for (Object obj : grid.getObjects()) {
				if (obj instanceof Human) {
					humans.add((Human) obj);
				}
			}
			
			if(humans.size()==0)
				moveTowards(myLocation());
		}

		@Override
		public boolean done() {
			List<Agent> people = new ArrayList<Agent>();
			if(checkDoorAtLocation(myLocation().getX(),myLocation().getY())){
				context.remove(this.myAgent);
				isSimulationOver();
				return true;
			}
			if(checkFireAtLocation(myLocation().getX(),myLocation().getY())){
				dead=1;
				context.remove(this.myAgent);
				isSimulationOver();
				return true;
			}
			
			return false;
		}
		
	}
	
	class answerDoorCoordinateRequests extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;
		
		private MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
																,MessageTemplate.MatchOntology(evacOntology.getName()));
		
		public answerDoorCoordinateRequests(Agent a){
			super(a);
		}
	
		public void action(){
			
			/*GridCellNgh<Human> nghCreator = new GridCellNgh<Human>(grid, myLocation(), Human.class, 1, 1);
			List<GridCell<Human>> gridCells = nghCreator.getNeighborhood(true);
			SimUtilities.shuffle(gridCells, RandomHelper.getUniform());*/
			ACLMessage msg = receive(template);
			if(msg!=null){
				if(msg.getContent().equals(new ExitRequest().getRequest())){
					if(closestExitX!=0 && closestExitY!=0){
						ACLMessage reply = new ACLMessage(ACLMessage.REQUEST);
						reply.setOntology(evacOntology.getName());
						reply.setLanguage(codec.getName());
						try {
							getContentManager().fillContent(reply, new Action(msg.getSender(), new RunToExit(closestExitX,closestExitY)));
							reply.addReceiver(msg.getSender());
							send(reply);
						}
						catch (Exception ex) { 
							ex.printStackTrace(); 
						}
					}
				}		
			}	
		}
		
	}
	private void isSimulationOver(){
		List<Agent> people = new ArrayList<Agent>();
		for (Object obj : grid.getObjects()) {
			if (obj instanceof Security || obj instanceof Human) {
				people.add((Security) obj);
			}
		}
		if (people.size() == 0){
			RunEnvironment.getInstance().endRun();
		}
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
	private boolean checkFireAtLocation(int x, int y){
		List<Object> fires = new ArrayList<Object>();
		for (Object obj : grid.getObjectsAt(x, y)) {
			if (obj instanceof Fire) {
				fires.add(obj);
			}
		}

		if (fires.size() > 0) {
			return true;
		}
		return false;
	}	
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
			closestExitX=doors.get(indexDoor).getLocation().getX();
			closestExitY=doors.get(indexDoor).getLocation().getY();
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
