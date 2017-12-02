package evacuacao.ontology;

import jade.content.Concept;

public class HelpDenial implements Concept {

	private static final long serialVersionUID = 1L;
	
	private String rejection;
	
	public HelpDenial() {
		rejection="Sorry but I need to run away :(";
	}
	
	public HelpDenial(String rejection) {
		this.setRejection(rejection);
	}

	public String getRejection() {
		return rejection;
	}

	public void setRejection(String rej) {
		this.rejection=rej;
	}
	
}
