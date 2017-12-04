package evacuacao.ontology;

import jade.content.AgentAction;
import jade.core.AID;
import sajas.core.Agent;

public class RescueMe implements AgentAction{
	private static final long serialVersionUID = 1L;

	private String message;
	private Agent myself;


	public RescueMe(AID rescuer,Agent myself) {
		this.message="Please assist me "+rescuer.getLocalName();
		this.myself=myself;
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String msg) {
		this.message=msg;
	}
	
	public Agent getVictim(){
		return myself;
	}
	public void setMyself(Agent myself){
		this.myself=myself;
	}
}
