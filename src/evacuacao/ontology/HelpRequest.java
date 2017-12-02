package evacuacao.ontology;

import jade.content.AgentAction;

public class HelpRequest implements AgentAction {
	private static final long serialVersionUID = 1L;
	
	private String request;
	private String message;

	public HelpRequest() {
	}
	
	public HelpRequest(String request) {
		this.request=request;
		this.message="Im injured, somebody save me!";
	}

	public String getRequest() {
		return request;
	}
	
	public String getMessage() {
		return message;
	} 
	
	public void setRequest(String request) {
		this.request=request;
	}
}
