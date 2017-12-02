package evacuacao.ontology;

import jade.content.Predicate;

public class ExitRequest implements Predicate {
	private static final long serialVersionUID = 1L;
	
	private String request;

	public ExitRequest() {
		request="Sir, where is the exit?";
	}
	
	public ExitRequest(String request) {
		this.request=request;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request=request;
	}
}
