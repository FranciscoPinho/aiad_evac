package evacuacao.ontology;

import jade.content.AgentAction;
import jade.core.AID;

public class RescueMe implements AgentAction{
	private static final long serialVersionUID = 1L;

	private String message;
	private AID myself;
	
	public RescueMe(){
		
	}

	public RescueMe(AID rescuer,AID myself) {
		this.message="Please assist me "+rescuer.getLocalName();
		setMyself(myself);
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String msg) {
		this.message=msg;
	}
	
	public AID getMyself(){
		return myself;
	}
	public void setMyself(AID myself){
		this.myself=myself;
	}
}
