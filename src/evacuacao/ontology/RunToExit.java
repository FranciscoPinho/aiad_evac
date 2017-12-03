package evacuacao.ontology;

import jade.content.AgentAction;

public class RunToExit implements AgentAction {
	
	private static final long serialVersionUID = 1L;

	private String message;
	private int x;
	private int y;

	public RunToExit() {
	}
	
	public RunToExit(int x,int y) {
		this.x=x;
		this.y=y;
		this.message="The exit is at ("+this.x+","+this.y+")"+"! Run for your life";
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String msg) {
		this.message=msg;
	}
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setX(int x) {
		this.x=x;
	}
	
	public void setY(int y) {
		this.y=y;
	}
	
	
}
