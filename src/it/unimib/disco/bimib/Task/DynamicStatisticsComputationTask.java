package it.unimib.disco.bimib.Task;
//GESTODifferent imports
import it.unimib.disco.bimib.Sampling.SamplingManager;
import it.unimib.disco.bimib.Tes.TesManager;
import it.unimib.disco.bimib.Tes.TesTree;
import it.unimib.disco.bimib.Utility.SimulationFeaturesConstants;
import it.unimib.disco.bimib.Atms.AtmManager;
import it.unimib.disco.bimib.Exceptions.InputFormatException;
import it.unimib.disco.bimib.Exceptions.NotExistingNodeException;
import it.unimib.disco.bimib.Exceptions.ParamDefinitionException;
import it.unimib.disco.bimib.Exceptions.TesTreeException;
import it.unimib.disco.bimib.GESTODifferent.GESTODifferentConstants;
import it.unimib.disco.bimib.GESTODifferent.Simulation;
import it.unimib.disco.bimib.GESTODifferent.SimulationsContainer;
import it.unimib.disco.bimib.IO.Input;
import it.unimib.disco.bimib.Middleware.NetworkManagment;
import it.unimib.disco.bimib.Mutations.MutationManager;
import it.unimib.disco.bimib.Networks.GraphManager;





//System imports
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Properties;





//Cytoscape imports
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class DynamicStatisticsComputationTask extends AbstractTask{

	private Properties perturbationFeatures;
	private NetworkManagment cytoscapeBridge;
	private CySwingAppAdapter adapter;
	private CyApplicationManager appManager;
	private SimulationsContainer simulationsContainer;
	private String networkId;
	private GraphManager mutatedNetworkManager;


	

	public DynamicStatisticsComputationTask(GraphManager originalNetworkManager, SamplingManager samplingManager, AtmManager atmManager,
			ArrayList<String> permanentKnockIn, ArrayList<String> permanentKnockOut,
			Properties perturbationFeatures, String networkId,
			CySwingAppAdapter adapter) throws ParamDefinitionException, NotExistingNodeException{
		

		this.perturbationFeatures = perturbationFeatures;
		this.adapter = adapter;
		this.appManager = this.adapter.getCyApplicationManager();
		this.cytoscapeBridge = new NetworkManagment(this.adapter, this.appManager);
		this.networkId = networkId;
		this.mutatedNetworkManager = originalNetworkManager.copy();
		
		//Fixes the knock-in nodes if specified
		if(permanentKnockIn != null){
			for(String geneName : permanentKnockIn)
				this.mutatedNetworkManager.perpetuallyChangeFunctionValue(geneName, true);
		}
		
		//Fixes the knock-out nodes if specified
		if(permanentKnockOut != null){
			for(String geneName : permanentKnockOut)
				this.mutatedNetworkManager.perpetuallyChangeFunctionValue(geneName, false);
		}
		
	}


	public void run(final TaskMonitor taskMonitor) throws Exception {
		/*// Give the task a title.
		taskMonitor.setTitle("GESTODifferent");
		taskMonitor.setProgress(0.0);
		String networkId;
		int requiredNetworks = Integer.parseInt(this.simulationFeatures.getProperty(SimulationFeaturesConstants.MATCHING_NETWORKS));
		GraphManager graphManager = null;
		SamplingManager samplingManager = null;
		MutationManager mutationManager = null;
		AtmManager atmManager = null;
		TesManager tesManager = null;
		Simulation newSim;
		CyNetwork parent;
		int distance;
		double[] deltas;
		boolean match;
		int net = 0;
		while(net < requiredNetworks){
			taskMonitor.setStatusMessage("Network " + (net + 1) + ": Network creation");
			match = true;
			deltas = null;
			parent = null;
			//Creates the network
			graphManager = new GraphManager();
			graphManager.createNetwork(this.simulationFeatures);
			networkId = "network_" + (net + 1);

			//Samples the network in order to find the attractors
			taskMonitor.setStatusMessage("Network " + (net + 1) + ": Attractors sampling");
			samplingManager = new SamplingManager(simulationFeatures, graphManager);

			//Creates the ATM manager and the ATM matrix (if required)
			if(this.atmComputation){
				//Defines the mutation manager
				mutationManager = new MutationManager(graphManager, samplingManager, simulationFeatures);
				taskMonitor.setStatusMessage("Network " + (net + 1) + ": Atm creation");
				atmManager = new AtmManager(simulationFeatures, samplingManager, mutationManager, graphManager.getNodesNumber());	

				if(treeMatching){
					taskMonitor.setStatusMessage("Network " + (net + 1) + ": Matching");
					//Creates the TES manager in order to match the network with the tree
					tesManager = new TesManager(atmManager, samplingManager);
					try{
						if(this.matchingType.equals(GESTODifferentConstants.PERFECT_MATCH)){
							//Tries to match the network with the given differentiation tree
							deltas = tesManager.findCorrectTesTree(this.givenTree);
						}else if(this.matchingType.equals(GESTODifferentConstants.MIN_DISTANCE)){
							//Min distance comparison
							distance = tesManager.findMinDistanceTesTree(this.givenTree);
							if(distance <= threshold)
								deltas = new double[1];
						}else{
							//Computes the histogram distance
							distance = tesManager.findMinHistogramDistanceTesTree(this.givenTree);
							if(distance <= threshold)
								deltas = new double[1];	
						}if(deltas == null){
							//Match
							match = false;
						}
					}catch(Exception ex){
						match = false;
					}
				}
			}
			//Stores the network only if it matches with the given tree (if the tree matching is required else it always match)
			if(match){
				taskMonitor.setProgress((net + 1)/((double)requiredNetworks));
				//Adds the simulation in the container
				newSim = new Simulation();
				newSim.setGraphManager(graphManager);
				newSim.setAtmManager(atmManager);
				newSim.setSamplingManager(samplingManager);
				this.simulationsContainer.addSimulation(networkId, newSim);

				//Creates the network view on Cytoscape (if required)
				if(this.requiredOutputs.getProperty(GESTODifferentConstants.NETWORK_VIEW).equals(GESTODifferentConstants.YES))
					parent = this.cytoscapeBridge.createNetwork(graphManager, networkId);

				//Creates the attractors view on Cytoscape (if required)
				if(this.requiredOutputs.getProperty(GESTODifferentConstants.ATTRACTORS_NETWORK_VIEW).equals(GESTODifferentConstants.YES))
					if(parent == null)
						this.cytoscapeBridge.createAttractorGraph(samplingManager.getAttractorFinder(), networkId);
					else
						this.cytoscapeBridge.createAttractorGraph(samplingManager.getAttractorFinder(), networkId, parent);
					
						
				net = net + 1;
			}else{
				taskMonitor.setStatusMessage("Network " + (net + 1) + ": No match");
			}
		}*/
	}
}