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
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.SimpleBehaviour;

public class Security extends Agent {
	private Grid<Object> grid;
	private boolean moved;
	private int closestExitX = -1, closestExitY = -1;
	private Context<Object> context;
	private Codec codec;
	private Ontology evacOntology;
	private int dead = 0;
	protected int escaped = 0;
	protected GridPoint sentinelLocation;
	protected answerDoorCoordinateRequests answerBehavior;
	protected boolean arrived=false,sentinel = false;

	public Security(Grid<Object> grid, Context<Object> context) {
		super();
		this.grid = grid;
		this.context = context;
	}
	
	public Security(Grid<Object> grid, Context<Object> context, boolean sent, int accidentDoorY) {
		super();
		this.grid = grid;
		this.context = context;
		this.sentinel=sent;
		this.sentinelLocation = new GridPoint(18,accidentDoorY);
	}
	@SuppressWarnings("unchecked")
	@Override
	public void setup() {
		// register language and ontology
		codec = new SLCodec();
		evacOntology = EvacuationOntology.getInstance();
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(evacOntology);
		answerBehavior = new answerDoorCoordinateRequests(this);
		addBehaviour(new movementBehaviour(this));
		addBehaviour(answerBehavior);
	}

	class movementBehaviour extends SimpleBehaviour {
		private static final long serialVersionUID = 1L;

		public movementBehaviour(Agent a) {
			super(a);
		}

		public void action() {
			closestDoor();
			if(!arrived && sentinel){
				if(myLocation().getX()==sentinelLocation.getX() && myLocation().getY()==sentinelLocation.getY()){
					arrived=true;
					return;
				}
				if (fireTooStrong() || fireNearby(myLocation()))
					moveToExit(myLocation());
				else moveToPoint(myLocation(),sentinelLocation);
			}
			else{
				int counter = 0;
				for (Object obj : grid.getObjects()) {
					if (obj instanceof Human) {
						if (((Human) obj).getDead() == 0 && ((Human) obj).escaped == 0)
							counter++;
					}
				}
	
				if (counter == 0 || fireTooStrong() || fireNearby(myLocation()))
					moveToExit(myLocation());
			}
		}

		@Override
		public boolean done() {
			if (checkDoorAtLocation(myLocation().getX(), myLocation().getY())) {
				escaped = 1;
				myAgent.removeBehaviour(answerBehavior);
				isSimulationOver();
				return true;
			}
			if (checkFireAtLocation(myLocation().getX(), myLocation().getY())) {
				setDead(1);
				myAgent.removeBehaviour(answerBehavior);
				DeadHuman dead = new DeadHuman();
				context.add(dead);
				grid.moveTo(dead, myLocation().getX(), myLocation().getY());
				isSimulationOver();
				return true;
			}

			return false;
		}

	}
	
	class answerDoorCoordinateRequests extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		private MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
				MessageTemplate.MatchOntology(evacOntology.getName()));

		public answerDoorCoordinateRequests(Agent a) {
			super(a);
		}

		public void action() {
			ACLMessage msg = receive(template);

			if (msg != null) {
				if (msg.getContent().equals(new ExitRequest().getRequest())) {
					// System.out.println("Received Request from
					// "+msg.getSender().getLocalName());
					if (closestExitX != -1 && closestExitY != -1) {
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.REQUEST);
						try {
							getContentManager().fillContent(reply,
									new Action(msg.getSender(), new RunToExit(closestExitX, closestExitY)));
							send(reply);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		}

	}

	private boolean fireNearby(GridPoint pt) {
		int i = pt.getX();
		int j = pt.getY();

		for (int iter = 1; iter <= 2; iter++) {
			if (checkFireAtLocation(i, j + iter))
				return true;
			if (checkFireAtLocation(i + iter, j + iter))
				return true;
			if (checkFireAtLocation(i + iter, j))
				return true;
			if (checkFireAtLocation(i, j - iter))
				return true;
			if (checkFireAtLocation(i + iter, j - iter))
				return true;
			if (checkFireAtLocation(i - iter, j))
				return true;
			if (checkFireAtLocation(i - iter, j - iter))
				return true;
			if (checkFireAtLocation(i - iter, j + iter))
				return true;
		}
		
		return false;
	}

	private void isSimulationOver() {
		for (Object obj : grid.getObjects()) {
			if (obj instanceof Security) {
				if (((Security) obj).getDead() == 0 && ((Security) obj).escaped == 0)
					return;
			}
			if (obj instanceof Human) {
				if (((Human) obj).getDead() == 0 && ((Human) obj).escaped == 0)
					return;
			}
		}
		((Network<Object>) context.getProjection("Help Request Network")).removeEdges();
		RunEnvironment.getInstance().endRun();
	}

	private boolean checkDoorAtLocation(int x, int y) {
		boolean door_found = false;
		for (Object obj : grid.getObjectsAt(x, y)) {
			if (obj instanceof Door) {
				door_found = true;
				return door_found;
			}
		}

		return door_found;
	}

	private boolean checkFireAtLocation(int x, int y) {
		boolean fire_found = false;
		for (Object obj : grid.getObjectsAt(x, y)) {
			if (obj instanceof Fire) {
				fire_found = true;
				return fire_found;
			}
		}
		return fire_found;
	}

	private boolean fireTooStrong() {
		// GridPoint topRoomExit = new GridPoint(20, 20);
		// GridPoint lowerRoomExit = new GridPoint(20, 8);
		if (checkFireAtLocation(20, 20) && checkFireAtLocation(20, 8))
			return true;
		return false;
	}

	private GridPoint myLocation() {
		return grid.getLocation(this);
	}

	public void closestDoor() {
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
				double distVal = Math.hypot(myLocation().getX() - doors.get(i).getLocation().getX(),
						myLocation().getY() - doors.get(i).getLocation().getY());
				if (distVal < distToExit) {
					distToExit = distVal;
					indexDoor = i;
				}
			}
		}
		if (indexDoor != -1) {
			closestExitX = doors.get(indexDoor).getLocation().getX();
			closestExitY = doors.get(indexDoor).getLocation().getY();
		}
	}

	public int deathStatistics() {
		return dead;
	}

	public int survivalStatistics() {
		if (dead == 1)
			return 0;
		else
			return 1;
	}

	public void setDead(int dead) {
		this.dead = dead;
	}

	public int getDead() {
		return dead;
	}

	public void moveToExit(GridPoint pt) {
		GridPoint nextPoint = getNextPoint(pt, new GridPoint(closestExitX, closestExitY));
		if (nextPoint != null) {
			grid.moveTo(this, (int) nextPoint.getX(), (int) nextPoint.getY());
			setMoved(true);
		}
	}
	
	public void moveToPoint(GridPoint pt, GridPoint destination) {
		GridPoint nextPoint = getNextPoint(pt, destination);
		if (nextPoint != null) {
			grid.moveTo(this, (int) nextPoint.getX(), (int) nextPoint.getY());
			setMoved(true);
		}
	}

	private GridPoint getNextPoint(GridPoint pt, GridPoint location) {

		ArrayList<Graph.Edge> lgraph = new ArrayList<Graph.Edge>();

		for (int i = 0; i < grid.getDimensions().getWidth(); i++)
			for (int j = 0; j < grid.getDimensions().getHeight(); j++) {
				if (validPosition(i, j)) {
					// Try to add 8 Possible edge
					// -1- (i-1,j+1)
					if (validPosition(i - 1, j + 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i - 1) + "y" + Integer.toString(j + 1), 1, new GridPoint(i, j),
								new GridPoint(i - 1, j + 1));
						lgraph.add(nEdge);
					}
					// -2- (i,j+1)
					if (validPosition(i, j + 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i) + "y" + Integer.toString(j + 1), 1, new GridPoint(i, j),
								new GridPoint(i, j + 1));
						lgraph.add(nEdge);
					}
					// -3- (i+1,j+1)
					if (validPosition(i + 1, j + 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i + 1) + "y" + Integer.toString(j + 1), 1, new GridPoint(i, j),
								new GridPoint(i + 1, j + 1));
						lgraph.add(nEdge);
					}
					// -4- (i-1,j)
					if (validPosition(i - 1, j)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i - 1) + "y" + Integer.toString(j), 1, new GridPoint(i, j),
								new GridPoint(i - 1, j));
						lgraph.add(nEdge);
					}
					// -5- (i+1,j)
					if (validPosition(i + 1, j)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i + 1) + "y" + Integer.toString(j), 1, new GridPoint(i, j),
								new GridPoint(i + 1, j));
						lgraph.add(nEdge);
					}
					// -6- (i-1,j-1)
					if (validPosition(i - 1, j - 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i - 1) + "y" + Integer.toString(j - 1), 1, new GridPoint(i, j),
								new GridPoint(i - 1, j - 1));
						lgraph.add(nEdge);
					}
					// -7- (i,j-1)
					if (validPosition(i, j - 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i) + "y" + Integer.toString(j - 1), 1, new GridPoint(i, j),
								new GridPoint(i, j - 1));
						lgraph.add(nEdge);
					}
					// -8- (i+1,j-1)
					if (validPosition(i + 1, j - 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i + 1) + "y" + Integer.toString(j - 1), 1, new GridPoint(i, j),
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
			if (obj instanceof Wall || obj instanceof Fire) {
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
