package evacuacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import evacuacao.ontology.EvacuationOntology;
import evacuacao.ontology.ExitRequest;
import evacuacao.ontology.HelpRequest;
import evacuacao.ontology.HelpResponse;
import evacuacao.ontology.RescueMe;
import evacuacao.ontology.RunToExit;
import graph.Graph;
import jade.content.AgentAction;
import jade.content.onto.basic.Action;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.OneShotBehaviour;
import sajas.core.behaviours.SimpleBehaviour;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

enum State {
	inRoom, wandering, knowExit, helping
}

enum Condition {
	healthy, injured, saved
}

enum Zones {
	topWall, bottomWall, RightWall, topRight, topLeft, bottomLeft, bottomRight, nowhere
}

public class Human extends Agent {
	private Grid<Object> grid;
	private boolean moved;
	private Context<Object> context;
	private State state;
	private Condition condition;
	private boolean altruism;
	private boolean fatedToDie;
	private boolean gotDoorCoordinates = false;
	private ArrayList<Zones> explored = new ArrayList<Zones>();
	private Zones nextZone = Zones.nowhere;
	private Zones fromZone = Zones.nowhere;
	private RepastEdge<Object> connectionVictim, connectionSecurity = null;
	private int visionRadius;
	private int fireInjuryRadius;
	private int exitX = -1;
	private int exitY = -1;
	private AID securityAID = null;
	private Agent savior = null;
	private Agent savingVictim = null;
	private Codec codec;
	private Ontology evacOntology;
	protected int dead = 0;
	protected int escaped = 0;
	protected int askedCoordinates = 0;
	protected int got_saved = 0;
	protected int full_save_attempt = 0;
	protected int immunityCounter = 0;
	protected queryDoorCoordinates querybehavior;
	protected receiveRequests receivebehavior;

	public Human(Grid<Object> grid, Context<Object> context, State state, Condition condition, boolean altruism,
			int visionRadius, int fireInjuryRadius) {
		super();
		this.grid = grid;
		this.context = context;
		this.state = state;
		this.condition = condition;
		this.altruism = altruism;
		this.visionRadius = visionRadius;
		this.fireInjuryRadius = fireInjuryRadius;
		this.fatedToDie = false;

	}

	@Override
	public void setup() {
		// register language and ontology
		codec = new SLCodec();
		evacOntology = EvacuationOntology.getInstance();
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(evacOntology);
		this.querybehavior = new queryDoorCoordinates(this);
		this.receivebehavior = new receiveRequests(this);
		addBehaviour(new movementBehaviour(this));
		addBehaviour(receivebehavior);
	}

	class movementBehaviour extends SimpleBehaviour {
		private static final long serialVersionUID = 1L;

		public movementBehaviour(Agent a) {
			super(a);
		}

		public void action() {
			if (checkFireAtLocation(myLocation().getX(), myLocation().getY())) {
				setDead(1);
				cleanupConnections();
				return;
			}

			if (checkDoorAtLocation(myLocation().getX(), myLocation().getY())) {
				escaped = 1;
				cleanupConnections();
				return;
			}

			if (state != State.helping && state != State.knowExit)
				if (myLocation().getX() > grid.getDimensions().getWidth() - 21 )
					state = State.wandering;

			// lookup in visionRadius to find exit or security guard or to be
			// burned
			if (state != State.helping)
				visionAndBurnDetection(myLocation());

			// if agent is injured, he has 40% chance of not moving
			if (condition == Condition.injured) {
				if(savior!=null){
					if(((Human)savior).getDead()==1){
						savior=null;
					}
				}
				if (!fatedToDie && dead == 0) {
					ArrayList<AID> rescuers = potentialRescuers(myLocation(), 12);
					addBehaviour(new sendHelpRequests(myAgent, rescuers));
				}
				int chanceMove = RandomHelper.nextIntFromTo(0, 100);
				if (chanceMove <= 40) {
					return;
				}
			}

			switch (state) {
			case inRoom:
				leaveRooms(myLocation());
				break;
			case wandering:
				moveExplore(myLocation());
				break;
			case knowExit:
				moveToExit(myLocation());
				break;
			case helping:
				moveToVictim(myLocation());
				break;
			default:
				break;
			}
		}

		@Override
		public boolean done() {
			if (checkDoorAtLocation(myLocation().getX(), myLocation().getY())) {
				escaped = 1;
				myAgent.removeBehaviour(querybehavior);
				myAgent.removeBehaviour(receivebehavior);
				cleanupConnections();
				isSimulationOver();
				return true;
			}
			if (checkFireAtLocation(myLocation().getX(), myLocation().getY())) {
				setDead(1);
				myAgent.removeBehaviour(querybehavior);
				myAgent.removeBehaviour(receivebehavior);
				cleanupConnections();
				DeadHuman dead = new DeadHuman();
				context.add(dead);
				grid.moveTo(dead, myLocation().getX(), myLocation().getY());
				isSimulationOver();
				return true;
			}

			return false;
		}
	}

	class queryDoorCoordinates extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		public queryDoorCoordinates(Agent a) {
			super(a);
		}

		public void action() {
			if (securityAID != null) {
				ExitRequest req = new ExitRequest();
				ACLMessage msgSend = new ACLMessage(ACLMessage.REQUEST);
				msgSend.addReceiver(securityAID);
				msgSend.setContent(req.getRequest());
				//System.out.println("SENT MESSAGE TO: "+securityAID.getLocalName() + " - " + msgSend.getContent());
				msgSend.setLanguage(codec.getName());
				msgSend.setOntology(evacOntology.getName());
				// Send message
				send(msgSend);
			}
		}
	}

	class receiveRequests extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		public receiveRequests(Agent a) {
			super(a);
		}

		public void action() {

			ACLMessage msg = receive();
			if (msg != null) {
				try {
					switch (msg.getPerformative()) {
					case (ACLMessage.REQUEST):
						ContentElement content = getContentManager().extractContent(msg);
						AgentAction action = (AgentAction) ((Action) content).getAction();
						if (action instanceof RunToExit) {
							exitX = ((RunToExit) action).getX();
							exitY = ((RunToExit) action).getY();
							connectionSecurity = ((Network<Object>) context.getProjection("Help Request Network"))
									.addEdge(myAgent, lookupAgent(msg.getSender()));
							state = State.knowExit;
							askedCoordinates = 1;
							gotDoorCoordinates = true;
						}
						if (action instanceof RescueMe) {
							if (dead == 0 && escaped == 0) {
								// System.out.println(myAgent.getLocalName() + "
								// GOING TO SAVE VICTIM: "
								// + msg.getSender().getLocalName());
								savingVictim = lookupAgent(msg.getSender());
								if(((Human)savingVictim).getDead()==1 || ((Human)savingVictim).escaped==1  )
									return;
								connectionVictim = ((Network<Object>) context.getProjection("Help Request Network"))
										.addEdge(myAgent, savingVictim);
								condition = Condition.healthy;
								state = State.helping;
								full_save_attempt++;
								immunityCounter = 6;
							}
						}
						break;
					case (ACLMessage.ACCEPT_PROPOSAL):
						if (savior == null) {
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.REQUEST);
							try {
								getContentManager().fillContent(reply,
										new Action(msg.getSender(), new RescueMe(msg.getSender(), myAgent.getAID())));
								send(reply);
								savior = lookupAgent(msg.getSender());
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
						break;
					case (ACLMessage.REJECT_PROPOSAL):
						break;
					case (ACLMessage.PROPOSE):
						//System.out.println(myAgent.getLocalName() + " Received help request from "
						//		+ msg.getSender().getLocalName());
						ACLMessage reply = msg.createReply();
						HelpResponse resp;
						if (altruism && condition != Condition.injured && savingVictim == null && dead == 0
								&& escaped == 0) {
							// System.out.println(myAgent.getLocalName() + "
							// ACCEPTING Received help request from "
							// + msg.getSender().getLocalName());
							reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
							resp = new HelpResponse(true);
							reply.setContent(resp.getMessage());
						} else {
							// System.out.println(myAgent.getLocalName() + "
							// REJECTING Received help request from "
							// + msg.getSender().getLocalName());
							reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
							resp = new HelpResponse(false);
							reply.setContent(resp.getMessage());
						}
						send(reply);
						break;
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	class sendHelpRequests extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		ArrayList<AID> rescuers;

		public sendHelpRequests(Agent a, ArrayList<AID> potRescuers) {
			super(a);
			rescuers = potRescuers;
		}

		public void action() {
			if (((Human) myAgent).getDead() == 0) {
				HelpRequest req = new HelpRequest();
				ACLMessage msgSend = new ACLMessage(ACLMessage.PROPOSE);
				for (AID resc : rescuers)
					msgSend.addReceiver(resc);
				msgSend.setContent(req.getMessage());
				// System.out.println("SENT MESSAGE TO: "+securityAID.toString()
				// + " - " + msgSend.getContent());
				msgSend.setLanguage(codec.getName());
				msgSend.setOntology(evacOntology.getName());
				// Send message
				send(msgSend);
			}
		}
	}

	/**
	 * Removes unneeded edges from network projection
	 */
	private void cleanupConnections() {
		if (connectionSecurity != null)
			((Network<Object>) context.getProjection("Help Request Network")).removeEdge(connectionSecurity);
		if (connectionVictim != null)
			((Network<Object>) context.getProjection("Help Request Network")).removeEdge(connectionVictim);
	}
	
	/**
	 * Finds an agent based on aid
	 * 
	 * @param aid
	 * @return Agent with AID aid
	 */
	public Agent lookupAgent(AID aid) {
		for (Object obj : context.getObjects(Agent.class)) {
			if (((Agent) obj).getAID().equals(aid)) {
				return (Agent) obj;
			}
		}
		return null;
	}

	/**
	 * Collects agents that are in range of being asked for help
	 * 
	 * @param pt
	 *            center of the "circle radius" where the message will be
	 *            supposedly broadcast
	 * @param messageRadius
	 *            integer with radius of message being heard by other agents
	 * @return ArrayList of potential rescuers
	 */
	private ArrayList<AID> potentialRescuers(GridPoint pt, int messageRadius) {
		int i = pt.getX();
		int j = pt.getY();
		ArrayList<AID> potentialRescuers = new ArrayList<AID>();
		for (int iter = 1; iter <= messageRadius; iter++) {
			if (validMovementPosition(i, j + iter)) {
				potentialRescuers.addAll(checkAgentsAtLocation(i, j + iter));
			}
			if (validMovementPosition(i + iter, j + iter)) {
				potentialRescuers.addAll(checkAgentsAtLocation(i + iter, j + iter));
			}
			if (validMovementPosition(i + iter, j)) {
				potentialRescuers.addAll(checkAgentsAtLocation(i + iter, j));
			}
			if (validMovementPosition(i, j - iter)) {
				potentialRescuers.addAll(checkAgentsAtLocation(i, j - iter));
			}
			if (validMovementPosition(i + iter, j - iter)) {
				potentialRescuers.addAll(checkAgentsAtLocation(i + iter, j - iter));
			}
			if (validMovementPosition(i - iter, j)) {
				potentialRescuers.addAll(checkAgentsAtLocation(i - iter, j));
			}
			if (validMovementPosition(i - iter, j - iter)) {
				potentialRescuers.addAll(checkAgentsAtLocation(i - iter, j - iter));
			}
			if (validMovementPosition(i - iter, j + iter)) {
				potentialRescuers.addAll(checkAgentsAtLocation(i - iter, j + iter));
			}
		}
		Collections.reverse(potentialRescuers);
		return potentialRescuers;
	}

	/**
	 * Functions as agent's "vision" and physical sense detecting exits or
	 * security guards in a radius, also detects if agent is within the radius
	 * of being injured by nearby fire and updates the condition of the agent to
	 * injured if a fire is within radius(Note:agent cannot be injured in
	 * immunityCounter>0). If detects a wall in any of the 8 directions, it will
	 * ignore everything beyond the wall.
	 * 
	 * @param pt
	 *            center of the "circle radius" where the vision/physical sense
	 *            of the agent will be processed
	 * @return true if any exit or security agent is detected
	 */
	private boolean visionAndBurnDetection(GridPoint pt) {
		int i = pt.getX();
		int j = pt.getY();
		boolean wallfound1 = false, wallfound2 = false, wallfound3 = false, wallfound4 = false, wallfound5 = false,
				wallfound6 = false, wallfound7 = false, wallfound8 = false;

		int highestRadius = this.fireInjuryRadius;
		if (this.visionRadius >= this.fireInjuryRadius)
			highestRadius = this.visionRadius;
		if (immunityCounter > 0)
			immunityCounter--;
		for (int iter = 1; iter <= highestRadius; iter++) {
			if (validPosition(i, j + iter) && !wallfound1) {

				if (this.fireInjuryRadius <= iter  && immunityCounter <= 0) {
					if (checkFireAtLocation(i, j + iter))
						condition = Condition.injured;
				}

				if (this.visionRadius <= iter && state != State.knowExit) {
					if (checkDoorAtLocation(i, j + iter)) {
						exitX = i;
						exitY = j + iter;
						state = State.knowExit;
						return true;
					}
					if (checkSecurityAtLocation(i, j + iter) != null && !gotDoorCoordinates){
						addBehaviour(querybehavior);
						return true;
					}
				}

			} else
				wallfound1 = true;
			if (validPosition(i + iter, j + iter) && !wallfound2) {

				if (this.fireInjuryRadius <= iter && immunityCounter <= 0) {
					if (checkFireAtLocation(i + iter, j + iter))
						condition = Condition.injured;
				}
				if (this.visionRadius <= iter && state != State.knowExit) {
					if (checkDoorAtLocation(i + iter, j + iter)) {
						exitX = i + iter;
						exitY = j + iter;
						state = State.knowExit;
						return true;
					}
					if (checkSecurityAtLocation(i + iter, j + iter) != null && !gotDoorCoordinates){
						addBehaviour(querybehavior);
						return true;
					}
				}

			} else
				wallfound2 = true;
			if (validPosition(i + iter, j) && !wallfound3) {

				if (this.fireInjuryRadius <= iter && immunityCounter <= 0) {
					if (checkFireAtLocation(i + iter, j) )
						condition = Condition.injured;
				}
				if (this.visionRadius <= iter && state != State.knowExit) {
					if (checkDoorAtLocation(i + iter, j)) {
						exitX = i + iter;
						exitY = j;
						state = State.knowExit;
						return true;
					}
					if (checkSecurityAtLocation(i + iter, j) != null && !gotDoorCoordinates){
						addBehaviour(querybehavior);
						return true;
					}
				}

			} else
				wallfound3 = true;
			if (validPosition(i, j - iter) && !wallfound4) {
				if (this.fireInjuryRadius <= iter && immunityCounter <= 0) {
					if (checkFireAtLocation(i, j - iter) )
						condition = Condition.injured;
				}
				if (this.visionRadius <= iter && state != State.knowExit) {
					if (checkDoorAtLocation(i, j - iter)) {
						exitX = i;
						exitY = j - iter;
						state = State.knowExit;
						return true;
					}
					if (checkSecurityAtLocation(i, j - iter) != null && !gotDoorCoordinates){
						addBehaviour(querybehavior);
						return true;
					}
				}
			} else
				wallfound4 = true;
			if (validPosition(i + iter, j - iter) && !wallfound5) {
				if (this.fireInjuryRadius <= iter && immunityCounter <= 0) {
					if (checkFireAtLocation(i + iter, j - iter))
						condition = Condition.injured;
				}
				if (this.visionRadius <= iter && state != State.knowExit) {
					if (checkDoorAtLocation(i + iter, j - iter)) {
						exitX = i + iter;
						exitY = j - iter;
						state = State.knowExit;
						return true;
					}
					if (checkSecurityAtLocation(i + iter, j - iter) != null && !gotDoorCoordinates){
						addBehaviour(querybehavior);
						return true;
					}
				}
			} else
				wallfound5 = true;
			if (validPosition(i - iter, j) && !wallfound6) {
				if (this.fireInjuryRadius <= iter && immunityCounter <= 0) {
					if (checkFireAtLocation(i - iter, j))
						condition = Condition.injured;
				}
				if (this.visionRadius <= iter && state != State.knowExit) {
					if (checkDoorAtLocation(i - iter, j)) {
						exitX = i - iter;
						exitY = j;
						state = State.knowExit;
						return true;
					}
					if (checkSecurityAtLocation(i - iter, j) != null && !gotDoorCoordinates){
						addBehaviour(querybehavior);
						return true;
					}
				}
			} else
				wallfound6 = true;
			if (validPosition(i - iter, j - iter) && !wallfound7) {
				if (this.fireInjuryRadius <= iter && immunityCounter <= 0) {
					if (checkFireAtLocation(i - iter, j - iter))
						condition = Condition.injured;
				}
				if (this.visionRadius <= iter && state != State.knowExit) {
					if (checkDoorAtLocation(i - iter, j - iter)) {
						exitX = i - iter;
						exitY = j - iter;
						state = State.knowExit;
						return true;
					}
					if (checkSecurityAtLocation(i - iter, j - iter) != null && !gotDoorCoordinates){
						addBehaviour(querybehavior);
						return true;
					}
				}
			} else
				wallfound7 = true;
			if (validPosition(i - iter, j + iter) && !wallfound8) {
				if (this.fireInjuryRadius <= iter && immunityCounter <= 0) {
					if (checkFireAtLocation(i - iter, j + iter))
						condition = Condition.injured;
				}
				if (this.visionRadius <= iter && state != State.knowExit) {
					if (checkDoorAtLocation(i - iter, j + iter)) {
						exitX = i - iter;
						exitY = j + iter;
						state = State.knowExit;
						return true;
					}
					if (checkSecurityAtLocation(i - iter, j + iter) != null && !gotDoorCoordinates){
						addBehaviour(querybehavior);
						return true;
					}
				}
			} else
				wallfound8 = true;
		}
		return false;
	}

	/**
	 * Check if all agents are either dead or have successfully escaped, ends
	 * simulation if that's the case
	 */
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

	/**
	 * Check if an exit/door is at the location passed in the parameters
	 * 
	 * @param x
	 * @param y
	 * @return true if an exit/door found and false otherwise
	 */
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

	/**
	 * Check if a fire is at the location passed in the parameters
	 * 
	 * @param x
	 * @param y
	 * @return true if a fire is found and false otherwise
	 */
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

	private AID checkSecurityAtLocation(int x, int y) {
		for (Object obj : grid.getObjectsAt(x, y)) {
			if (obj instanceof Security) {
				securityAID = (AID) ((Security) obj).getAID();
				return securityAID;
			}
		}
		return null;
	}

	private ArrayList<AID> checkAgentsAtLocation(int x, int y) {
		ArrayList<AID> agents = new ArrayList<AID>();
		for (Object obj : grid.getObjectsAt(x, y)) {
			if (obj instanceof Human) {
				if (((Human) obj).getDead() == 0 && ((Human) obj).escaped == 0)
					agents.add(((Agent) obj).getAID());
			}
		}
		return agents;
	}

	private GridPoint myLocation() {
		return grid.getLocation(this);
	}

	public GridPoint getLocation() {
		return myLocation();
	}

	public void receiveHealing() {
		condition = Condition.healthy;
		//System.out.println(getLocalName() + " am healthy! was saved!");
		immunityCounter = 6;
		got_saved++;
	}

	public int times_got_saved() {
		return got_saved;
	}

	public int nr_save_attempts() {
		return full_save_attempt;
	}

	public Zones currentZone(int x, int y) {
		if (x >= 20 && x <= 22) {
			if (y >= 1 && y <= 3)
				return Zones.bottomLeft;
			if (y <= this.grid.getDimensions().getHeight() - 2 && y >= this.grid.getDimensions().getHeight() - 4)
				return Zones.topLeft;
		}
		if (x >= 25 && x <= 33) {
			if (y >= 1 && y <= 3)
				return Zones.bottomWall;
			if (y >= this.grid.getDimensions().getHeight() - 4 && y <= this.grid.getDimensions().getHeight() - 2)
				return Zones.topWall;
		}
		if (x >= this.grid.getDimensions().getWidth() - 4 && x <= this.grid.getDimensions().getWidth() - 2) {
			if (y >= 1 && y <= 3)
				return Zones.bottomRight;
			if (y >= this.grid.getDimensions().getHeight() - 4 && y <= this.grid.getDimensions().getHeight() - 2)
				return Zones.topRight;
			if (y >= 6 && y <= 18) {
				return Zones.RightWall;
			}

		}
		return Zones.nowhere;
	}

	public void nextZone(Zones currentZone) {
		ArrayList<Zones> possibleZones = new ArrayList<Zones>();
		switch (currentZone) {
		case RightWall:
			if (!explored.contains(Zones.bottomRight)) {
				possibleZones.add(Zones.bottomRight);
			}
			if (!explored.contains(Zones.topRight)) {
				possibleZones.add(Zones.topRight);
			}
			if (!possibleZones.isEmpty()) {
				int zone_index = RandomHelper.nextIntFromTo(0, possibleZones.size() - 1);
				nextZone = possibleZones.get(zone_index);
				fromZone = Zones.RightWall;
			} else
				nextZone = Zones.nowhere;
			break;
		case bottomLeft:
			if (!explored.contains(Zones.bottomRight)) {
				possibleZones.add(Zones.bottomRight);
			}
			if (!explored.contains(Zones.topLeft)) {
				possibleZones.add(Zones.topLeft);
			}
			if (!explored.contains(Zones.bottomWall)) {
				possibleZones.add(Zones.bottomWall);
			}
			if (!possibleZones.isEmpty()) {
				int zone_index = RandomHelper.nextIntFromTo(0, possibleZones.size() - 1);
				nextZone = possibleZones.get(zone_index);
				fromZone = Zones.bottomLeft;
			} else
				nextZone = Zones.nowhere;
			break;
		case bottomRight:
			if (!explored.contains(Zones.topRight)) {
				possibleZones.add(Zones.topRight);
			}
			if (!explored.contains(Zones.bottomLeft)) {
				possibleZones.add(Zones.bottomLeft);
			}
			if (!explored.contains(Zones.bottomWall)) {
				possibleZones.add(Zones.bottomWall);
			}
			if (!possibleZones.isEmpty()) {
				int zone_index = RandomHelper.nextIntFromTo(0, possibleZones.size() - 1);
				nextZone = possibleZones.get(zone_index);
				fromZone = Zones.bottomRight;
			} else
				nextZone = Zones.nowhere;
			break;
		case bottomWall:
			if (!explored.contains(Zones.bottomRight)) {
				possibleZones.add(Zones.bottomRight);
			}
			if (!explored.contains(Zones.bottomLeft)) {
				possibleZones.add(Zones.bottomLeft);
			}
			if (!explored.contains(Zones.topWall)) {
				possibleZones.add(Zones.topWall);
			}
			if (!possibleZones.isEmpty()) {
				int zone_index = RandomHelper.nextIntFromTo(0, possibleZones.size() - 1);
				nextZone = possibleZones.get(zone_index);
				fromZone = Zones.bottomWall;
			} else
				nextZone = Zones.nowhere;
			break;
		case topLeft:
			if (!explored.contains(Zones.topRight)) {
				possibleZones.add(Zones.topRight);
			}
			if (!explored.contains(Zones.bottomLeft)) {
				possibleZones.add(Zones.bottomLeft);
			}
			if (!explored.contains(Zones.topWall)) {
				possibleZones.add(Zones.topWall);
			}
			if (!possibleZones.isEmpty()) {
				int zone_index = RandomHelper.nextIntFromTo(0, possibleZones.size() - 1);
				nextZone = possibleZones.get(zone_index);
				fromZone = Zones.topLeft;
			} else
				nextZone = Zones.nowhere;
			break;
		case topRight:
			if (!explored.contains(Zones.topLeft)) {
				possibleZones.add(Zones.topLeft);
			}
			if (!explored.contains(Zones.bottomRight)) {
				possibleZones.add(Zones.bottomRight);
			}
			if (!explored.contains(Zones.topWall)) {
				possibleZones.add(Zones.topWall);
			}
			if (!possibleZones.isEmpty()) {
				int zone_index = RandomHelper.nextIntFromTo(0, possibleZones.size() - 1);
				nextZone = possibleZones.get(zone_index);
				fromZone = Zones.topRight;
			} else
				nextZone = Zones.nowhere;
			break;
		case topWall:
			if (!explored.contains(Zones.topRight)) {
				possibleZones.add(Zones.topRight);
			}
			if (!explored.contains(Zones.topLeft)) {
				possibleZones.add(Zones.topLeft);
			}
			if (!explored.contains(Zones.bottomWall)) {
				possibleZones.add(Zones.bottomWall);
			}
			if (!possibleZones.isEmpty()) {
				int zone_index = RandomHelper.nextIntFromTo(0, possibleZones.size() - 1);
				nextZone = possibleZones.get(zone_index);
				fromZone = Zones.topWall;
			} else
				nextZone = Zones.nowhere;
			break;
		case nowhere:
			break;
		default:
			break;

		}
	}

	public void moveToZone(int i, int j) {
		switch (this.nextZone) {
		case RightWall:
			if (this.fromZone == Zones.bottomRight) {
				if (!moveUp(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.topRight) {
				if (!moveDown(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			this.nextZone = Zones.nowhere;
			break;
		case bottomLeft:
			if (this.fromZone == Zones.bottomRight) {
				if (!moveLeft(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.topLeft) {
				if (!moveDown(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.bottomWall) {
				if (!moveLeft(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			this.nextZone = Zones.nowhere;
			break;
		case bottomRight:
			if (this.fromZone == Zones.topRight) {
				if (!moveDown(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.bottomLeft) {
				if (!moveRight(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}

			if (this.fromZone == Zones.bottomWall) {
				if (!moveRight(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.RightWall) {
				if (moveDown(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			this.nextZone = Zones.nowhere;
			break;
		case bottomWall:
			if (this.fromZone == Zones.bottomRight) {
				if (!moveLeft(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.bottomLeft) {
				if (!moveRight(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.topWall) {
				if (!moveDown(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			this.nextZone = Zones.nowhere;
			break;
		case topLeft:
			if (this.fromZone == Zones.topRight) {
				if (!moveLeft(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.bottomLeft) {
				if (!moveUp(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.topWall) {
				if (!moveLeft(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			this.nextZone = Zones.nowhere;
			break;
		case topRight:
			if (this.fromZone == Zones.topLeft) {
				if (!moveRight(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.RightWall) {
				if (!moveUp(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.bottomRight) {
				if (!moveUp(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.topWall) {
				if (!moveRight(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			this.nextZone = Zones.nowhere;
			break;
		case topWall:
			if (this.fromZone == Zones.topRight) {
				if (!moveDown(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.topLeft) {
				if (!moveRight(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			if (this.fromZone == Zones.bottomWall) {
				if (!moveUp(i, j)) {
					this.nextZone = Zones.nowhere;
				}
				else return;
			}
			this.nextZone = Zones.nowhere;
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
		Zones currentZone = currentZone(myLocation().getX(), myLocation().getY());
		if (!explored.contains(currentZone))
			explored.add(currentZone);
		if (this.nextZone == currentZone && this.nextZone != Zones.nowhere) {
			nextZone(currentZone);
		}
		if (this.nextZone != currentZone && this.nextZone != Zones.nowhere) {
			moveToZone(i, j);
			return;
		}
		if (this.nextZone == Zones.nowhere) {

			if (currentZone != Zones.nowhere) {
				nextZone(currentZone);
				moveToZone(i, j);
				return;
			}

			ArrayList<GridPoint> possibleMoves = new ArrayList<GridPoint>();

			if (validMovementPosition(i, j + 1)) {
				possibleMoves.add(new GridPoint(i, j + 1));
			}
			if (validMovementPosition(i + 1, j + 1)) {
				possibleMoves.add(new GridPoint(i + 1, j + 1));
			}
			if (validMovementPosition(i + 1, j)) {
				possibleMoves.add(new GridPoint(i + 1, j));
			}
			if (validMovementPosition(i, j - 1)) {
				possibleMoves.add(new GridPoint(i, j - 1));
			}

			if (validMovementPosition(i + 1, j - 1)) {
				possibleMoves.add(new GridPoint(i + 1, j - 1));
			}

			if (!possibleMoves.isEmpty()) {
				int move_index = RandomHelper.nextIntFromTo(0, possibleMoves.size() - 1);
				grid.moveTo(this, possibleMoves.get(move_index).getX(), possibleMoves.get(move_index).getY());
				setMoved(true);
			} else
				setFatedToDie(true);

		}

	}

	public void setFatedToDie(boolean v) {
		fatedToDie = v;
	}

	public void moveToVictim(GridPoint pt) {

		trySaveVictim();
		if (savingVictim == null) {
			((Network<Object>) context.getProjection("Help Request Network")).removeEdge(connectionVictim);
			switch (state) {
			case inRoom:
				leaveRooms(myLocation());
				break;
			case wandering:
				moveExplore(myLocation());
				break;
			case knowExit:
				moveToExit(myLocation());
				break;
			}
			return;
		}

		GridPoint nextPoint = getNextPoint(pt, ((Human) savingVictim).getLocation());
		if (nextPoint != null && ((Human) savingVictim).getDead() == 0) {
			grid.moveTo(this, (int) nextPoint.getX(), (int) nextPoint.getY());
			trySaveVictim();
		} else {
			System.out.println("It's impossible to save the victim " + savingVictim.getLocalName());
			((Network<Object>) context.getProjection("Help Request Network")).removeEdge(connectionVictim);
			
			if (exitX != -1 && exitY != -1) {
				state = State.knowExit;
				moveToExit(pt);
			} else {
				if (pt.getX() < 20) {
					state = State.inRoom;
					leaveRooms(pt);
				} else {
					state = State.wandering;
					moveExplore(pt);
				}
			}
			savingVictim = null;
		}
		setMoved(true);
	}

	public void trySaveVictim() {
		if (Math.abs(myLocation().getX() - ((Human) savingVictim).getLocation().getX()) < 2
				&& Math.abs(myLocation().getY() - ((Human) savingVictim).getLocation().getY()) < 2) {
			//System.out.println("saved " + ((Human) savingVictim).getLocalName());
			((Network<Object>) context.getProjection("Help Request Network")).removeEdge(connectionVictim);
			((Human) savingVictim).receiveHealing();
			if (exitX != -1 && exitY != -1) {
				state = State.knowExit;
			} else {
				if (myLocation().getX() < 20) {
					state = State.inRoom;
				} else {
					state = State.wandering;
				}
			}
			savingVictim = null;
		}
	}

	public void moveToExit(GridPoint pt) {
		GridPoint nextPoint = getNextPoint(pt, new GridPoint(this.exitX, this.exitY));
		if (nextPoint != null) {
			grid.moveTo(this, (int) nextPoint.getX(), (int) nextPoint.getY());
		} else {
			moveExplore(pt);
		}
		setMoved(true);
	}

	public void leaveRooms(GridPoint pt) {
		double distToExit = 999999;
		GridPoint topRoomExit = new GridPoint(20, 20);
		GridPoint lowerRoomExit = new GridPoint(20, 8);
		double distVal = Math.hypot(myLocation().getX() - topRoomExit.getX(), myLocation().getY() - topRoomExit.getY());
		if (distVal < distToExit) {
			distToExit = distVal;
		}
		distVal = Math.hypot(myLocation().getX() - lowerRoomExit.getX(), myLocation().getY() - lowerRoomExit.getY());
		if (distVal < distToExit) {
			distToExit = distVal;
			GridPoint nextPoint = getNextPoint(pt, lowerRoomExit);
			if (nextPoint != null) {
				grid.moveTo(this, (int) nextPoint.getX(), (int) nextPoint.getY());
				setMoved(true);
				return;
			} else {
				nextPoint = getNextPoint(pt, topRoomExit);
				if (nextPoint != null) {
					grid.moveTo(this, (int) nextPoint.getX(), (int) nextPoint.getY());
					setMoved(true);
					return;
				} else
					setFatedToDie(true);
			}
		}

		GridPoint nextPoint = getNextPoint(pt, topRoomExit);
		if (nextPoint != null) {
			grid.moveTo(this, (int) nextPoint.getX(), (int) nextPoint.getY());
			setMoved(true);
			return;
		} else {
			nextPoint = getNextPoint(pt, lowerRoomExit);
			if (nextPoint != null) {
				grid.moveTo(this, (int) nextPoint.getX(), (int) nextPoint.getY());
				setMoved(true);
				return;
			} else
				setFatedToDie(true);
		}

		return;

	}

	private GridPoint getNextPoint(GridPoint pt, GridPoint location) {

		ArrayList<Graph.Edge> lgraph = new ArrayList<Graph.Edge>();

		for (int i = 0; i < grid.getDimensions().getWidth(); i++)
			for (int j = 0; j < grid.getDimensions().getHeight(); j++) {
				if (validPosition(i, j)) {
					// Try to add 8 Possible edge
					// -1- (i-1,j+1)
					if (validMovementPosition(i - 1, j + 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i - 1) + "y" + Integer.toString(j + 1), 1, new GridPoint(i, j),
								new GridPoint(i - 1, j + 1));
						lgraph.add(nEdge);
					}
					// -2- (i,j+1)
					if (validMovementPosition(i, j + 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i) + "y" + Integer.toString(j + 1), 1, new GridPoint(i, j),
								new GridPoint(i, j + 1));
						lgraph.add(nEdge);
					}
					// -3- (i+1,j+1)
					if (validMovementPosition(i + 1, j + 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i + 1) + "y" + Integer.toString(j + 1), 1, new GridPoint(i, j),
								new GridPoint(i + 1, j + 1));
						lgraph.add(nEdge);
					}
					// -4- (i-1,j)
					if (validMovementPosition(i - 1, j)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i - 1) + "y" + Integer.toString(j), 1, new GridPoint(i, j),
								new GridPoint(i - 1, j));
						lgraph.add(nEdge);
					}
					// -5- (i+1,j)
					if (validMovementPosition(i + 1, j)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i + 1) + "y" + Integer.toString(j), 1, new GridPoint(i, j),
								new GridPoint(i + 1, j));
						lgraph.add(nEdge);
					}
					// -6- (i-1,j-1)
					if (validMovementPosition(i - 1, j - 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i - 1) + "y" + Integer.toString(j - 1), 1, new GridPoint(i, j),
								new GridPoint(i - 1, j - 1));
						lgraph.add(nEdge);
					}
					// -7- (i,j-1)
					if (validMovementPosition(i, j - 1)) {
						Graph.Edge nEdge = new Graph.Edge("x" + Integer.toString(i) + "y" + Integer.toString(j),
								"x" + Integer.toString(i) + "y" + Integer.toString(j - 1), 1, new GridPoint(i, j),
								new GridPoint(i, j - 1));
						lgraph.add(nEdge);
					}
					// -8- (i+1,j-1)
					if (validMovementPosition(i + 1, j - 1)) {
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
		// g.printPath(END);
		// g.printAllPaths();
		return nextPoint;
	}

	private boolean validMovementPosition(int i, int j) {
		boolean noWall = true;
		if (i < 0 || j < 0)
			return false;
		if (i >= grid.getDimensions().getWidth())
			return false;
		if (j >= grid.getDimensions().getHeight())
			return false;
		for (Object obj : grid.getObjectsAt(i, j)) {
			if (obj instanceof Fire) {
				return false;
			}
			if (obj instanceof Wall) {
				noWall=false;
			}
			if(obj instanceof Door){
				return true;
			}
		}
		return noWall;
	}

	private boolean validPosition(int i, int j) {
		boolean noWall = true;
		if (i < 0 || j < 0)
			return false;
		if (i >= grid.getDimensions().getWidth())
			return false;
		if (j >= grid.getDimensions().getHeight())
			return false;
		for (Object obj : grid.getObjectsAt(i, j)) {
			if (obj instanceof Wall) {
				noWall=false;
			}
			if(obj instanceof Door){
				return true;
			}
		}
		return noWall;
	}

	public boolean moveLeft(int i, int j) {
		if (validMovementPosition(i - 1, j)) {
			grid.moveTo(this, i - 1, j);
			setMoved(true);
			return true;
		}
		return false;
	}

	public boolean moveUp(int i, int j) {
		if (validMovementPosition(i, j + 1)) {
			grid.moveTo(this, i, j + 1);
			setMoved(true);
			return true;
		}
		return false;
	}

	public boolean moveDown(int i, int j) {
		if (validMovementPosition(i, j - 1)) {
			grid.moveTo(this, i, j - 1);
			setMoved(true);
			return true;
		}
		return false;
	}

	public boolean moveRight(int i, int j) {
		if (validMovementPosition(i + 1, j)) {
			grid.moveTo(this, i + 1, j);
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

	public int deathStatistics() {
		return dead;
	}

	public int survivalStatistics() {
		if (dead == 1)
			return 0;
		else
			return 1;
	}

	public int askedCoordinates() {
		return this.askedCoordinates;
	}

	public int getDead() {
		return this.dead;
	}
	
	public void setDead(int dead) {
		this.dead = dead;
	}
}
