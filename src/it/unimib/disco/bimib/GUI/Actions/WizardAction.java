/**
 * WizardAction class.
 * @author Andrea Paroni (a.paroni@campus.unimib.it)
 * @group BIMIB @ DISCo (Department of Information Technology, Systems and Communication) of Milan University - Bicocca 
 * @year 2014
 */

package it.unimib.disco.bimib.GUI.Actions;

//CABERNET imports
import it.unimib.disco.bimib.CABERNET.CABERNETConstants;
import it.unimib.disco.bimib.CABERNET.SimulationsContainer;
import it.unimib.disco.bimib.GUI.Wizard;
import it.unimib.disco.bimib.Task.NetworkCreation;
import it.unimib.disco.bimib.Task.NetworkEditingFromCytoscape;

//System imports
import java.awt.event.ActionEvent;
import java.util.Properties;


//Cytoscape imports
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;


public class WizardAction extends AbstractCyAction{

	private static final long serialVersionUID = 1L;
	private SimulationsContainer simulationsContainer;
	private CyApplicationManager appManager;
	private final CySwingAppAdapter adapter;

	public WizardAction(CySwingAppAdapter adapter, SimulationsContainer simulationsContainer) {
		super("Wizard");
		this.adapter = adapter;
		this.appManager = this.adapter.getCyApplicationManager();
		this.simulationsContainer = simulationsContainer;
		setPreferredMenu("Apps.CABERNET");
	}


	/**
	 * CABERNET entry point.
	 * This method opens the wizard view.
	 */
	public void actionPerformed(ActionEvent e) {
		// Get a Cytoscape service 'DialogTaskManager' in CyActivator class
		DialogTaskManager dialogTaskManager = adapter.getCyServiceRegistrar().getService(DialogTaskManager.class);

		Wizard wizard = new Wizard(adapter);
		Properties simulationFeatures;
		Properties tasks;
		Properties outputs;
		boolean atm_computation, tree_matching;
		String matching_type;
		int threshold;

		int response = wizard.showWizard();
		if(response == 1){
			simulationFeatures = wizard.getSimulationFeatures();
			tasks = wizard.getTaskToDo();
			outputs = wizard.getOutputs();
			atm_computation = tasks.getProperty(CABERNETConstants.ATM_COMPUTATION).equals(CABERNETConstants.YES);
			tree_matching = tasks.getProperty(CABERNETConstants.TREE_MATCHING).equals(CABERNETConstants.YES);
			//Network Creation from features
			try{
				//Create the network randomly
				if(tasks.getProperty(CABERNETConstants.NETWORK_CREATION).equals(CABERNETConstants.NEW)){
					if(!tree_matching){
						dialogTaskManager.execute(new TaskIterator(new NetworkCreation(simulationFeatures, outputs, this.adapter, 
								this.appManager, this.simulationsContainer, atm_computation)));
					}else{
						matching_type = tasks.getProperty(CABERNETConstants.MATCHING_TYPE);
						if(matching_type.equals(CABERNETConstants.PERFECT_MATCH)){
							dialogTaskManager.execute(new TaskIterator(new NetworkCreation(simulationFeatures, outputs, this.adapter, 
									this.simulationsContainer, atm_computation, tree_matching, wizard.getDifferentiationTree())));
						}else{
							threshold = Integer.parseInt(tasks.getProperty(CABERNETConstants.MATCHING_THRESHOLD));
							dialogTaskManager.execute(new TaskIterator(new NetworkCreation(simulationFeatures, outputs, this.adapter, 
									this.simulationsContainer, atm_computation, tree_matching, wizard.getDifferentiationTree(),
									matching_type, threshold)));
							
						}
					}
					
				//Gets the original network from Cytoscape
				}else if(tasks.getProperty(CABERNETConstants.NETWORK_CREATION).equals(CABERNETConstants.CYTOSCAPE_EDIT)){
					if(!tree_matching){
						dialogTaskManager.execute(new TaskIterator(new NetworkEditingFromCytoscape(simulationFeatures, outputs, this.adapter, 
								this.appManager, this.simulationsContainer, atm_computation)));
					}else{
						matching_type = tasks.getProperty(CABERNETConstants.MATCHING_TYPE);
						if(matching_type.equals(CABERNETConstants.PERFECT_MATCH)){
							dialogTaskManager.execute(new TaskIterator(new NetworkEditingFromCytoscape(simulationFeatures, outputs, this.adapter, 
									this.simulationsContainer, atm_computation, tree_matching, wizard.getDifferentiationTree())));
						}else{
							threshold = Integer.parseInt(tasks.getProperty(CABERNETConstants.MATCHING_THRESHOLD));
							dialogTaskManager.execute(new TaskIterator(new NetworkEditingFromCytoscape(simulationFeatures, outputs, this.adapter, 
									this.simulationsContainer, atm_computation, tree_matching, wizard.getDifferentiationTree(),
									matching_type, threshold)));
							
						}
					}
				}
			}catch(Exception ex){

			}



		}
	}
}
