package evacuacao.ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class EvacuationOntology extends BeanOntology {
	private static final long serialVersionUID = 1L;

	public static final String ONTOLOGY_NAME = "alter-ego";
	
	// Singleton instance of this ontology
	private static Ontology theInstance = new EvacuationOntology();
	
	// Method to access the singleton ontology object
	public static Ontology getInstance() {
		return theInstance;
	}
	
	// Private constructor
	private EvacuationOntology() {
		super(ONTOLOGY_NAME);
		
		try {
			// add all Concept, Predicate and AgentAction
			add(ExitRequest.class);
			add(HelpDenial.class);
			add(HelpRequest.class);
			add(RunToExit.class);
			
		} catch(BeanOntologyException boe) {
			boe.printStackTrace();
		}
	}
	
}
