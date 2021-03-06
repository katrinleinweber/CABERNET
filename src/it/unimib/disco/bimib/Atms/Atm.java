/**
 * This class creates the ATM matrix
 * 
 * @author Andrea Paroni (a.paroni@campus.unimib.it)
 * @author Giorgia Previtali (g.previtali6@campus.unimib.it)
 * @group BIMIB @ DISCo (Department of Information Technology, Systems and Communication) of Milan University - Bicocca 
 * @year 2014
 * 
 */

package it.unimib.disco.bimib.Atms;

//System imports
import java.util.ArrayList;








//GRNSim imports
import it.unimib.disco.bimib.Exceptions.*;
import it.unimib.disco.bimib.Mutations.Mutation;
import it.unimib.disco.bimib.Sampling.AttractorsFinder;
import it.unimib.disco.bimib.Statistics.DynamicPerturbationsStatistics;
import it.unimib.disco.bimib.Utility.UtilityRandom;


public class Atm {

	//References to attractor's interface and mutation's interface
	private AttractorsFinder attractorsFinder;
	private Mutation mutation;
	private boolean dynamicPerturbsComputation;
	private DynamicPerturbationsStatistics dynamicPerturbationsStatistics;

	//Initializes the atm
	private double[][] atm;
	//Initializes the frequency of the attractors
	private int[] totalFrequencyRows;
	private ArrayList<double[][]> storedAtmNotMatching = new ArrayList<double[][]>();

	/**
	 * This is the constructor where there is initialized the attractor's interface
	 * and the call for the main method
	 * @param attractorsFinder: the attractor finder object
	 * @param mutation: The mutation object as initialized for the task.
	 * @param dynamicPerturbsComputation: boolean value to specify if compute the avalanches and the sensitivity tasks. 
	 * @param nodes: number of nodes if the network
	 * @throws NotExistingAttractorsException 
	 * @throws ParamDefinitionException 
	 * @throws Exception 
	 */	
	public Atm(AttractorsFinder attractorsFinder, Mutation mutation, boolean dynamicPerturbsComputation, int nodes){
		//Parameters checking
		if(attractorsFinder == null)
			throw new NullPointerException("Attractor finder object must be not null");
		if(mutation == null)
			throw new NullPointerException("Mutation object must be not null");

		this.attractorsFinder = attractorsFinder;
		this.mutation = mutation;
		this.dynamicPerturbsComputation = dynamicPerturbsComputation;
		if(this.dynamicPerturbsComputation)
			this.dynamicPerturbationsStatistics = new DynamicPerturbationsStatistics(nodes);
		else
			this.dynamicPerturbationsStatistics = null;
	}

	/**
	 * This is the constructor where there is initialized the attractor's interface
	 * and the call for the main method
	 * @param attractorsFinder: the attractor finder object
	 * @param mutation: The mutation object as initialized for the task.
	 * @throws NotExistingAttractorsException 
	 * @throws ParamDefinitionException 
	 * @throws Exception 
	 */	
	public Atm(AttractorsFinder attractorsFinder, Mutation mutation){
		this(attractorsFinder, mutation, false, -1);
	}

	/**
	 * Generic constructor
	 * @param attractorsFinder: An attractor finder object
	 * @param mutation: A mutation object
	 * @param atmMatrix: A not normalized atm matrix 
	 */
	public Atm(AttractorsFinder attractorsFinder, Mutation mutation, double[][] atmMatrix){
		if(atmMatrix == null)
			throw new NullPointerException("Atm matrix must be not null");
		if(attractorsFinder == null)
			throw new NullPointerException("Attractor finder must be not null");
		if(mutation == null)
			throw new NullPointerException("Mutation object must be not null");

		this.atm = atmMatrix;
		this.attractorsFinder = attractorsFinder;
		this.mutation = mutation;
		//Normalizes the atm matrix
		this.normalize();

	}


	/**
	 * This method executes a complete flip experiment. In this case each node of each 
	 * state of each attractor is flipped one time.
	 * @param attractors
	 * @param nodes
	 * @throws NotExistingAttractorsException
	 * @throws ParamDefinitionException
	 * @throws NotExistingNodeException
	 * @throws InputTypeException
	 * @throws AttractorNotFoundException
	 * @throws InterruptedException 
	 */
	public void createAtm(Object[] attractors, int nodes) throws NotExistingAttractorsException, 
		ParamDefinitionException, NotExistingNodeException, InputTypeException, 
		AttractorNotFoundException, InterruptedException{

		int numberOfAttractors = attractors.length;
		Object newState, attractorNewState;
		Object[] statesInAttractor;

		//Copies the attractor's array into an ArrayList
		ArrayList<Object> attractorVet = new ArrayList<Object>();
		for(int index = 0; index < numberOfAttractors; index++){
			attractorVet.add(attractors[index]);
		}
		//Initializes the atm.
		this.atm = new double[numberOfAttractors][numberOfAttractors];

		//Initializes the atm at zero
		for(int line = 0; line < numberOfAttractors; line++){
			for(int pillar = 0; pillar < numberOfAttractors; pillar++){
				this.atm[line][pillar] = 0;
			}
		}

		//Calculates the ATM entries.
		int a = 0;
		while(a < numberOfAttractors){

			//Forces the process conclusion in case of thread interruption
			if(Thread.interrupted())
				throw new InterruptedException();
			
			//Gets a permutation of the states in the selected attractor
			statesInAttractor = this.attractorsFinder.getStatesInAttractor(attractorVet.get(a));
			for(Object state : statesInAttractor){
				//Forces the process conclusion in case of thread interruption
				if(Thread.interrupted())
					throw new InterruptedException();
				
				//Perform the perturb experiments
				for(int gene = 0; gene < nodes; gene ++){
					//Forces the process conclusion in case of thread interruption
					if(Thread.interrupted())
						throw new InterruptedException();
					
					//Calls the mutation method 
					newState = this.mutation.doSingleFlip(state, gene);
					//Gets the new state's attractor
					attractorNewState = attractorsFinder.getAttractor(newState);

					//Computes the avalanches and the sensitivity if required.
					if(this.dynamicPerturbsComputation){
						dynamicPerturbationsStatistics.avalanchesAndSensitivityComputation(statesInAttractor, 
								this.attractorsFinder.getStatesInAttractor(attractorNewState));
					}

					if(attractorNewState != null){

						//Verifies if the attractor has already been discovered.
						if(attractorVet.contains(attractorNewState) == true){

							//Gets the new state's index from the attractor's ArrayList
							//and modifies the Atm matrix
							this.atm[a][attractorVet.indexOf(attractorNewState)] = this.atm[a][attractorVet.indexOf(attractorNewState)] + 1.0;
						}else{
							//Adds the new attractors and modifies the Atm matrix
							reCreateAtm(attractorsFinder.getAttractors());
							attractorVet.add(attractorNewState);
							this.atm[a][attractorVet.indexOf(attractorNewState)] = this.atm[a][attractorVet.indexOf(attractorNewState)] + 1.0;
							numberOfAttractors = numberOfAttractors + 1;
						}
					}else{
						System.out.println("No attractor found");
					}
				}
			}
			a = a + 1;
		}
		//Atm matrix normalization
		this.normalize();
		storeAtmNotMatching(this.atm);
	}


	public void createAtm(Object[] attractors, int perturbExperiments, double perturbStatesRatio) 
			throws MissingFeaturesException, ParamDefinitionException, NotExistingAttractorsException, 
			NotExistingNodeException, InputTypeException, 
			AttractorNotFoundException, InterruptedException {

		//Param check
		if(perturbStatesRatio < 0 || perturbStatesRatio > 1)
			throw new ParamDefinitionException("The ratio of states to perturb value must be in [0, 1] interval");

		int numberOfAttractors = attractors.length;
		Object newState, attractorNewState;
		Object[] statesInAttractor;

		//Copies the attractor's array into an ArrayList
		ArrayList<Object> attractorVet = new ArrayList<Object>();
		for(int index = 0; index < numberOfAttractors; index++){
			attractorVet.add(attractors[index]);
		}
		//Initializes the atm.
		this.atm = new double[numberOfAttractors][numberOfAttractors];

		//Initializes the atm at zero
		for(int line = 0; line < numberOfAttractors; line++){
			for(int pillar = 0; pillar < numberOfAttractors; pillar++){
				this.atm[line][pillar] = 0;
			}
		}

		//Calculates the ATM entries.
		int index;
		int a  = 0;
		while(a < numberOfAttractors){

			//Forces the process conclusion in case of thread interruption
			if(Thread.interrupted())
				throw new InterruptedException();
			
			//Gets a permutation of the states in the selected attractor
			statesInAttractor = UtilityRandom.randomPermutation(
					this.attractorsFinder.getStatesInAttractor(attractorVet.get(a)));
			index = 0;
			Object state;
			do{
			
				state = statesInAttractor[index];
				//Perform the perturb experiment
				for(int exp = 0; exp < perturbExperiments; exp++){
					
					//Forces the process conclusion in case of thread interruption
					if(Thread.interrupted())
						throw new InterruptedException();
					
					//Calls the mutation method 
					newState = this.mutation.doMutation(state);
					//Gets the new state's attractor
					attractorNewState = attractorsFinder.getAttractor(newState);

					//Computes the avalanches and the sensitivity if required.
					if(this.dynamicPerturbsComputation){
						dynamicPerturbationsStatistics.avalanchesAndSensitivityComputation(statesInAttractor, 
								this.attractorsFinder.getStatesInAttractor(attractorNewState));
					}

					if(attractorNewState != null){

						//Verifies if the attractor has already been discovered.
						if(attractorVet.contains(attractorNewState) == true){

							//Gets the new state's index from the attractor's ArrayList
							//and modifies the Atm matrix
							this.atm[a][attractorVet.indexOf(attractorNewState)] = this.atm[a][attractorVet.indexOf(attractorNewState)] + 1.0;
						}else{
							//Adds the new attractors and modifies the Atm matrix
							reCreateAtm(attractorsFinder.getAttractors());
							attractorVet.add(attractorNewState);
							this.atm[a][attractorVet.indexOf(attractorNewState)] = this.atm[a][attractorVet.indexOf(attractorNewState)] + 1.0;
							numberOfAttractors = numberOfAttractors + 1;
						}
					}else{
						System.out.println("No attractor found");
					}
				}
				index ++;

				//Forces the process conclusion in case of thread interruption
				if(Thread.interrupted())
					throw new InterruptedException();
				
			}while(index < Math.floor(perturbStatesRatio * statesInAttractor.length));	
			a = a + 1;
		}

		//Atm matrix normalization
		this.normalize();
		storeAtmNotMatching(this.atm);
	}

	/**
	 * This method recreates the Atm matrix when for a state there isn't its attractor
	 * @param newAttractors : an array with all the attractors of the network
	 * @throws NotExistingAttractorsException
	 */
	private void reCreateAtm(Object[] newAttractors) throws NotExistingAttractorsException{

		int numbersOfNewAttractors = newAttractors.length;

		//Creates the new Atm matrix
		double[][] newAtm = new double[numbersOfNewAttractors][numbersOfNewAttractors];

		//Recopies all the values from the old Atm matrix
		for(int line = 0; line < numbersOfNewAttractors-1; line++){
			for(int pillar = 0; pillar < numbersOfNewAttractors-1; pillar++){
				newAtm[line][pillar] = this.atm[line][pillar];
			}
		}

		//Adds the values for the new pillar
		for(int pillar = 0; pillar < numbersOfNewAttractors; pillar++){

			newAtm[numbersOfNewAttractors -1][pillar] = 0;
		}

		//Adds the values for the new line
		for(int line = 0; line < numbersOfNewAttractors; line++){
			newAtm[line][numbersOfNewAttractors -1] = 0;
		}

		//Initializes the new Atm
		this.atm = newAtm;

	}

	/**
	 * This method return the Atm matrix
	 * @return Atm
	 */
	public double[][] getAtm(){
		return this.atm;
	}

	/**
	 * This method returns a copy of the Atm matrix
	 * @return
	 */

	public double[][] copyAtm(){
		double[][] atmForTes = new double[atm.length][atm.length];

		for(int line = 0; line < atm.length; line++){
			for(int pillar = 0; pillar < atm.length; pillar++){
				atmForTes[line][pillar] = this.atm[line][pillar];
			}
		}
		return atmForTes;
	}

	/**
	 * This method returns the atm matrix without the links
	 * less than delta.
	 * @param delta
	 * @return a copy of the atm matrix with some links removed.
	 */
	public double[][] atmMatrixWithDeltaRemoval(double delta){
		//Scans the original atm matrix removing the links with the 
		// associated value less than delta
		for(int i = 0; i < atm.length; i++){
			for(int j = 0; j < atm.length; j++){
				atm[i][j] = (atm[i][j] <= delta ? 0 : atm[i][j]);
			}
		}
		return atm;
	}

	/**
	 * This method returns a copy of the atm matrix without the links
	 * less than delta.
	 * @param delta
	 * @return a copy of the atm matrix with some links removed.
	 */
	public double[][] copyAtmMatrixWithDeltaRemoval(double delta){
		double[][] copiedDelta = new double[atm.length][atm.length];
		//Scans the original atm matrix removing the links with the 
		// associated value less than delta
		for(int i = 0; i < atm.length; i++){
			for(int j = 0; j < atm.length; j++){
				copiedDelta[i][j] = (atm[i][j] <= delta ? 0 : atm[i][j]);
			}
		}
		return copiedDelta;
	}

	/**
	 * This method returns the atm matrix not normalized
	 * @return the atm matrix not normalized
	 */
	public double[][] getAtmWithoutNormalization(){
		double[][] atmNotNormalized = new double[this.atm.length][this.atm.length];

		for(int line = 0; line < this.atm.length; line++){
			//Checks if the total frequency of the specified line is 0
			if(this.totalFrequencyRows[line] == 0){
				for(int pillar = 0; pillar < this.atm.length; pillar ++){
					//Multiplies the atm value for the number of attractors
					atmNotNormalized[line][pillar] = this.atm[line][pillar] * this.attractorsFinder.getAttractors().length; 
				}
			}else{
				for(int pillar = 0; pillar < this.atm.length; pillar ++){
					//Multiplies the atm value for the total frequency of the specified line 
					atmNotNormalized[line][pillar] = this.atm[line][pillar] * this.totalFrequencyRows[line]; 
				}
			}
		}
		return atmNotNormalized;
	}

	/**
	 * This method stores the not matching atms
	 * @param atm the atm to store.
	 */
	public void storeAtmNotMatching(double[][] atm){
		storedAtmNotMatching.add(atm);
	}

	/**
	 * This method returns the stored not matching atms
	 * @return Atms
	 */
	public ArrayList<double[][]> getStoredAtmNotMaching(){
		return storedAtmNotMatching;
	}

	/***
	 * This method returns the ATM in the tsv format
	 */
	public String getCsvAtm(){
		String csvAtm = "";
		//Converts the ATM in a tsv format
		for(int i = 0; i < this.atm.length; i++){
			//New line
			if(i != 0)
				csvAtm += "\n";
			for(int j = 0; j < this.atm.length - 1; j++){
				csvAtm += this.atm[i][j] + ",";
			}
			csvAtm += this.atm[i][this.atm.length-1];
		}

		return csvAtm;
	}

	/**
	 * This method normalizes the atm matrix.
	 */
	private void normalize(){
		this.totalFrequencyRows = new int[this.atm.length];
		int numberOfAttractors =  this.atm.length;

		for(int line = 0; line < this.atm.length; line++){
			this.totalFrequencyRows[line] = 0;

			for(int pillar = 0; pillar < this.atm.length; pillar++){
				//Calculates the frequency for the specified line
				this.totalFrequencyRows[line] = this.totalFrequencyRows[line] + (int)this.atm[line][pillar];
			}

			for(int pillar = 0; pillar < this.atm.length; pillar++){
				//Normalizes the Atm matrix
				if(this.totalFrequencyRows[line] == 0)
					this.atm[line][pillar] = 1.0/numberOfAttractors;
				else
					this.atm[line][pillar] = this.atm[line][pillar]/this.totalFrequencyRows[line];
			}
		}
	}

	/**
	 * This method returns the DynamicPerturbationStatistics object.
	 * @return the DynamicPerturbationStatistics object.
	 */
	public DynamicPerturbationsStatistics getDynamicPerturbationsStatistics(){
		return this.dynamicPerturbationsStatistics;
	}

	public Atm(double[][] atm){
		this.atm = atm;
	}


}
