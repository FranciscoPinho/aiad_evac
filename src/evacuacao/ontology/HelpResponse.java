package evacuacao.ontology;

import jade.content.Concept;

public class HelpResponse implements Concept {

	private static final long serialVersionUID = 1L;
	
	private String message;
	private boolean value;
	
	public HelpResponse(boolean value) {
		if(value)
			this.setMessage("Hold on I will save you!");
		else
			this.setMessage("Sorry but I have to run away!");
		this.setValue(value);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String msg) {
		this.message=msg;
	}
	
	public boolean getValue() {
		return value;
	}

	public void setValue(boolean b) {
		this.value=b;
	}
}
