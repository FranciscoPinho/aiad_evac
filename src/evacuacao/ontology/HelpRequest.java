package evacuacao.ontology;

import jade.content.Predicate;

public class HelpRequest implements Predicate {
	private static final long serialVersionUID = 1L;
	
	private String message;

	public HelpRequest() {
	}
	
	public HelpRequest(String request) {
		this.message="Im injured, somebody save me!";
	}

	public String getMessage() {
		return message;
	} 
}
