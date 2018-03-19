package deconvolution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/*
 *  Collection of all the interaction models and their shared data (genotypes, expression etc)
 *  There are n + 1 interaction models, where n = the number of celltypes. One full model 
 *  with all celltypes, and for each celltype one model with the interaction term for that
 *  model removed
 */
public class InteractionModelCollection {
	private double[] expressionValues;
	private double[] genotypes;
	private double[] swappedGenotypes;
	private String qtlName;
	private HashMap<String, InteractionModel> interactionModels = new HashMap<String, InteractionModel>();
	private HashMap<String, Double> pvalues = new HashMap<String, Double>();
	private ArrayList<String> fullModelNames = new ArrayList<String>();
	private HashMap<String, ArrayList<String>> ctModelNames = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> fullModelNamesByCelltype = new HashMap<String, ArrayList<String>>();
	private HashMap<String, String> ctModelName = new HashMap<String, String>();
	private String bestFullModelName;
	private HashMap<String, String> bestCtModel = new HashMap<String, String>();
	private CellCount cellCount; 
	private List<String> genotypeConfigurationsFullModel = new ArrayList<String> ();
	private List<String> genotypeConfigurationsCtModel = new ArrayList<String> ();
	private HashMap<String, HashMap<String,String>> ctModelByGenotypeConfiguration = new HashMap<String, HashMap<String, String>>();
	private Double fullModelAIC;
	private HashMap<String, Double> ctModelAICs = new HashMap<String, Double>();
	private HashMap<String, String> ctModelsSameGenotypeConfigurationBestFullModel = new HashMap<String, String>();
	private HashMap<String, String> modelCelltype = new HashMap<String, String>();
	private HashMap<String, ArrayList<String>> genotypeConfigMap = new HashMap<String, ArrayList<String>>();
	private List<String> celltypes = new ArrayList<String>();
	private List<String> sampleNames = new ArrayList<String>();
	private boolean useBaseModel;
	private HashMap<String, String> bestFullModelPerCelltype = new HashMap<String, String>();
	private double[] bestBetas;
	private Boolean useOLS = false;


	/*
	 * Have to initialize instance with if NNLS or OLS will be used, and for that we need cellCounts
	 */
	public InteractionModelCollection(CellCount cellCount, String genotypeConfigurationType, boolean useBaseModel, Boolean useOLS) throws IllegalAccessException{
		this.useOLS = useOLS;
		setCellCount(cellCount);
		this.useBaseModel = useBaseModel;
		makeConfigurations(genotypeConfigurationType);
		this.bestBetas = new double[cellCount.getNumberOfCelltypes()*2];
		for(int i = 0; i < cellCount.getNumberOfCelltypes()*2; ++i){
			bestBetas[i] = 0;
		}

	}

	private boolean getUseBaseModel(){
		return(this.useBaseModel);
	}

	public String getCtModelName(String celltype){
		return(ctModelName.get(celltype));
	}

	public List<String> getAllCelltypes(){
		return(celltypes);
	}	

	public HashMap<String, String> getCtModelsByGenotypeConfiguration(String genotypeConfiguration){
		return(this.ctModelByGenotypeConfiguration.get(genotypeConfiguration));
	}

	/*
	 * Get interaction model with modelName 
	 */
	public InteractionModel getInteractionModel(String modelName) throws IllegalAccessException{
		InteractionModel interactionModel = this.interactionModels.get(modelName);
		return(interactionModel);
	}

	/*
	 * Remove interaction model with modelName
	 */
	public void removeInteractionModel(String modelName) throws IllegalAccessException{
		this.interactionModels.remove(modelName);
	}

	/*
	 * Set CellCount
	 */
	private void setCellCount(CellCount cellCount){
		this.cellCount = cellCount;
		celltypes = cellCount.getAllCelltypes();
		sampleNames = cellCount.getSampleNames();
	}

	public boolean getUseOLS(){
		return(this.useOLS);
	}

	public CellCount getCellCount() throws IllegalAccessException{
		return this.cellCount;
	}	
	/** 
	 * Set the expression values (y) for all the interaction models. 
	 */
	public void setExpressionValues(double[] expression){
		this.expressionValues = expression;
	}
	/** 
	 * Get the expression values (y) of all the interaction models. 
	 */
	public double[] getExpessionValues() throws IllegalAccessException{
		return(this.expressionValues);
	}
	/*
	 * Get the genotypes of all the interaction models
	 */
	public double[] getGenotypes() throws IllegalAccessException {
		return this.genotypes;
	}

	/*
	 * Get the genotypes of all the interaction models
	 */
	public double[] getSwappedGenotypes() throws IllegalAccessException {
		return this.swappedGenotypes;	
	}



	public void setGenotypes(double[] genotypes) {
		this.genotypes = genotypes;
		swapGenotypes();
	}
	/** 
	 * Get a list of all the celltypes given as input
	 */
	private void swapGenotypes(){
		this.swappedGenotypes = this.genotypes.clone();
		for(int i = 0; i < this.genotypes.length; i++) {
			this.swappedGenotypes[i] = 2 - this.genotypes[i];
		}
	}

	public void setQtlName(String qtlName){
		this.qtlName = qtlName;
	}

	public String getQtlName() throws IllegalAccessException{
		return(this.qtlName);
	}

	/*
	 * Each ctModel will have a p-value from ANOVA test with fullmodel, save it per ctModel
	 */
	public void setPvalue(Double pvalue, String modelName){
		this.pvalues.put(modelName, pvalue);
	}

	public Double getPvalue(String modelName) throws IllegalAccessException{
		Double pvalue = this.pvalues.get(modelName);
		return(pvalue);
	}

	public ArrayList<String> getFullModelNames() throws IllegalAccessException{
		return this.fullModelNames;
	}
	public ArrayList<String> getCtModelNames(String celltype) throws IllegalAccessException{
		return this.ctModelNames.get(celltype);
	}

	private void setBestFullModelName(String modelName){
		this.bestFullModelName = modelName;
	}

	private void setBestFullModel(String modelName, String celltypeName){
		this.bestFullModelPerCelltype.put(celltypeName, modelName);
	}

	public InteractionModel getBestFullModel() throws IllegalAccessException{
		return(this.getInteractionModel(this.bestFullModelName));
	}

	public InteractionModel getBestFullModel(String celltype) throws IllegalAccessException{
		String bestFullModel = bestFullModelPerCelltype.get(celltype);
		return(this.getInteractionModel(bestFullModel));
	}

	private void setCtModelSameGenotypeConfigurationAsBestFullModel(String celltype, String modelName){
		this.ctModelsSameGenotypeConfigurationBestFullModel.put(celltype, modelName);
	}

	public String getCtModelSameGenotypeConfigurationAsBestFullModel(String celltype, Boolean useBaseModel) throws IllegalAccessException{
		if(!this.ctModelsSameGenotypeConfigurationBestFullModel.containsKey(celltype)){
			InteractionModel bestFullModel;
			if(useBaseModel){
				bestFullModel = this.getBestFullModel(celltype);
			}
			else{
				bestFullModel = this.getBestFullModel();
				String ctModel =  this.getCtModelsByGenotypeConfiguration(bestFullModel.getGenotypeConfiguration()).get(celltype);
				setCtModelSameGenotypeConfigurationAsBestFullModel(celltype, ctModel);
			}

		}
		return(this.ctModelsSameGenotypeConfigurationBestFullModel.get(celltype));
	}

	private void setBestCtModel(String celltype, String modelName){
		this.bestCtModel.put(celltype, modelName);
	}

	// per celltype there is one best Ct model
	public InteractionModel getBestCtModel(String celltype) throws IllegalAccessException{
		return(this.getInteractionModel(this.bestCtModel.get(celltype)));
	}

	private HashMap<String, String> getCtModelsWithConfiguration(String genotypConfiguration) throws IllegalAccessException{
		return(this.ctModelByGenotypeConfiguration.get(this.getBestFullModel().getGenotypeConfiguration()));
	}

	/*
	 * Set the AIC of full model and ct model, also calculate the delta between the two
	 * If base model == true, AIC has to be calculated for fullModel for every celltype
	 */
	public void setAIC(Boolean useBaseModel) throws IllegalAccessException{
		InteractionModel bestFullModel = null;
		HashMap<String, String> cellTypeCtModel = new HashMap<String, String>();
		if(!useBaseModel){
			bestFullModel = getBestFullModel();
			bestFullModel.setAIC();
			this.fullModelAIC = bestFullModel.getAIC();
			cellTypeCtModel = getCtModelsWithConfiguration(bestFullModel.getGenotypeConfiguration());
		}
		for(String celltype : this.getCellCount().getAllCelltypes()){
			if(useBaseModel){
				bestFullModel = getBestFullModel(celltype);
				bestFullModel.setAIC();
			}
			String modelName = cellTypeCtModel.get(celltype);

			InteractionModel ctModel = this.getInteractionModel(modelName);
			ctModel.setAIC();
			this.ctModelAICs.put(modelName,ctModel.getAIC());
			ctModel.setAICdelta(bestFullModel.getAIC());
		}
	}

	public double getFullModelAIC() throws IllegalAccessException{
		return(fullModelAIC);
	}

	public double getCtModelAIC(String ctModelName) throws IllegalAccessException{
		return(ctModelAICs.get(ctModelName));
	}

	public double[] getBestBetas(){
		return(this.bestBetas);
	}
	/*
	 * Add interaction model to the collections
	 */
	private void addInteractionModel(InteractionModel interactionModel, String modelName, Boolean isFullModel){
		this.interactionModels.put(modelName, interactionModel);
		String cellType = interactionModel.getCelltypeName();
		if(isFullModel){
			fullModelNames.add(modelName);
			fullModelNamesByCelltype.putIfAbsent(cellType, new ArrayList<String>());
			fullModelNamesByCelltype.get(cellType).add(modelName);
		}
		else{
			ctModelNames.putIfAbsent(cellType, new ArrayList<String>());
			ctModelNames.get(cellType).add(modelName);
		}
	}

	/*
	 * Go through all full models, calculate the regression statistics and 
	 * select the model with the highest R2 as the new full model
	 */
	public void findBestFullModel(Boolean selectMostBetas, Boolean outputBestBetas) throws IllegalAccessException, IOException{
		// set to -1 so that first loop can be initialized
		double sumOfSquares = -1;
		double nonZeroBetas = 0;
		for (String modelName : getFullModelNames()){
			InteractionModel fullModel = getInteractionModel(modelName);
			if(getUseOLS()){
				fullModel.calculateSumOfSquaresOLS(getExpessionValues(),false);
			}else{
				fullModel.calculateSumOfSquaresNNLS(getExpessionValues());
			}

			double nonZeroBetasCurrentModel = 0;
			if(selectMostBetas || outputBestBetas){
				double[] estimateRegressionParameters = fullModel.getEstimateRegressionParameters();
				for(int i = 0; i < estimateRegressionParameters.length; i++){
					// only count interaction term betas for most non zeros (interaction betas start from numberOfCelltypes +1
					if (i > cellCount.getNumberOfCelltypes() && estimateRegressionParameters[i] > 0){
						++nonZeroBetasCurrentModel;
					};
					if(estimateRegressionParameters[i] > bestBetas[i]){
						bestBetas[i] = estimateRegressionParameters[i];
					}
				}
			}

			double fullModelSumOfSquares = fullModel.getSumOfSquares();
			if (sumOfSquares == -1){
				sumOfSquares = fullModelSumOfSquares;
			}
			// first select the model with the most non-zero betas, if there is a tie select of those the ones with lowest RSS
			if(selectMostBetas){
				if (nonZeroBetasCurrentModel > nonZeroBetas || (nonZeroBetasCurrentModel == nonZeroBetas && fullModelSumOfSquares <= sumOfSquares)){
					setBestFullModelName(fullModel.getModelName());
					sumOfSquares = fullModelSumOfSquares;
					nonZeroBetas = nonZeroBetasCurrentModel;
				}
				else{
					removeInteractionModel(fullModel.getModelName());
				}
			}
			// select model with lowest RSS
			else{
				if (fullModelSumOfSquares <= sumOfSquares){
					setBestFullModelName(fullModel.getModelName());
					sumOfSquares = fullModelSumOfSquares;
				}
				else{
					removeInteractionModel(fullModel.getModelName());
				}
			}
		}
	}

	/*
	 * Go through all full models per celltype, calculate the regression statistics and 
	 * select the model with the highest R2 as the new full model for that celltype
	 */
	public void findBestFullModel(Boolean useBaseModel, Boolean selectMostBetas, Boolean outputBestBetas) throws IllegalAccessException, IOException{
		if(!useBaseModel){
			// if not using the base model, should use the other findBestFullModel() function. 
			// separated this into two functions because doing this per celltype makes it quite
			// different application from other function
			findBestFullModel(selectMostBetas, outputBestBetas);
			return;
		}


		for(String celltypeName : this.getCellCount().getAllCelltypes()){
			// set to -1 so that first loop can be initialized
			double sumOfSquares = -1;
			for (String modelName : fullModelNamesByCelltype.get(celltypeName)){
				InteractionModel fullModel = getInteractionModel(modelName);

				if(getUseOLS()){
					fullModel.calculateSumOfSquaresOLS(getExpessionValues(),false);
				}else{
					fullModel.calculateSumOfSquaresNNLS(getExpessionValues());
				}

				if (sumOfSquares == -1){
					sumOfSquares = fullModel.getSumOfSquares();
				}
				if (fullModel.getSumOfSquares() <= sumOfSquares){
					sumOfSquares = fullModel.getSumOfSquares();
					setBestFullModel(fullModel.getModelName(), celltypeName);
				}
				else{
					removeInteractionModel(fullModel.getModelName());
				}
			}
		}
	}

	/*
	 * Go through all ct models, calculate the regression statistics and select the model with the highest R2 as the new ct model
	 * TODO: merge with findBestFullModel()
	 */
	public void findBestCtModel(Boolean useBaseModel) throws IllegalAccessException, IOException{
		// set to -1 so that first loop can be initialized
		for(String celltype : celltypes){
			double sumOfSquares = -1;
			double nonZeroBetas = 0;
			for (String modelName : getCtModelNames(celltype)){
				if(modelName.contains("restModel")){
					continue;
				}
				InteractionModel ctModel = getInteractionModel(modelName);
				modelCelltype.put(modelName, celltype);

				if(getUseOLS()){
					ctModel.calculateSumOfSquaresOLS(getExpessionValues(),false);
				}
				else{
					ctModel.calculateSumOfSquaresNNLS(getExpessionValues());
				}
				double nonZeroBetasCurrentModel = 0;
				for(double d : ctModel.getEstimateRegressionParameters()){
					if (d > 0){
						++nonZeroBetasCurrentModel;
					};
				}
				double ctSumOfSquares = ctModel.getSumOfSquares();
				if (sumOfSquares == -1){
					sumOfSquares = ctSumOfSquares;
					nonZeroBetas = nonZeroBetasCurrentModel;
				}
				if(!useBaseModel){
					setCtModelByGenotypeConfiguration();
				}


				if (nonZeroBetasCurrentModel > nonZeroBetas || (nonZeroBetasCurrentModel == nonZeroBetas && ctSumOfSquares <= sumOfSquares)){
					sumOfSquares = ctSumOfSquares;
					setBestCtModel(ctModel.getCelltypeName(), ctModel.getModelName());
				}
				else{
					// if the interaction model name of the full model is not the same as the model name of the 
					// CT model, we want to remove the ct model data to preserve RAM. If it is the same,
					// we keep it so that AIC can be calculated
					if(!useBaseModel){
						if(!ctModel.getModelName().equals(this.getCtModelSameGenotypeConfigurationAsBestFullModel(celltype, useBaseModel))){
							removeInteractionModel(ctModel.getModelName());
						}
					}
				}
			}
		}
	}

	/*
	 * Make the genotype configurations that will be used for the interaction terms 
	 */
	private void makeConfigurations(String genotypeConfigurationType) throws IllegalAccessException{
		if(getUseOLS()){
			// if we use OLS we just use default genotype orientation (all 0's)
			String fullModelGenotypeConfiguration = String.join("", Collections.nCopies(celltypes.size(), "0"));
			this.genotypeConfigurationsFullModel.add(fullModelGenotypeConfiguration);
			String ctModelGenotypeConfiguration = String.join("", Collections.nCopies(celltypes.size()-1, "0"));
			this.genotypeConfigurationsCtModel.add(ctModelGenotypeConfiguration);
			genotypeConfigMap.putIfAbsent(fullModelGenotypeConfiguration, new ArrayList<String>());
			for(int i = 0; i < this.getCellCount().getNumberOfCelltypes(); i++){
				genotypeConfigMap.get(fullModelGenotypeConfiguration).add("ctModel_"+this.getCellCount().getCelltype(i)+"_"+ctModelGenotypeConfiguration);
			}
			return;
		}
		else if(getUseBaseModel()){
			this.genotypeConfigurationsFullModel = Utils.binaryPermutations("",2, new ArrayList<String>());
			this.genotypeConfigurationsCtModel = Utils.binaryPermutations("",1, new ArrayList<String>());
		}else{
			if(genotypeConfigurationType.equals("all")){
				// this gets all possible combinations, e.g. if 3 celltypes: 000, 001, 010, 100, 011, 101, 110, 111
				this.genotypeConfigurationsFullModel = Utils.binaryPermutations("",celltypes.size(), new ArrayList<String>());
			}else if(genotypeConfigurationType.equals("two")){
				// this gets two possible combinations, e.g. if 3 celltypes: 000, 111
				this.genotypeConfigurationsFullModel.add(String.join("", Collections.nCopies(celltypes.size(), "0")));
				this.genotypeConfigurationsFullModel.add(String.join("", Collections.nCopies(celltypes.size(), "1")));
			}else if(genotypeConfigurationType.equals("one")){
				// similar to "two", but can have one different, e.g. : 000, 111, 001, 010, 100
				this.genotypeConfigurationsFullModel.add(String.join("", Collections.nCopies(celltypes.size(), "0")));
				this.genotypeConfigurationsFullModel.add(String.join("", Collections.nCopies(celltypes.size(), "1")));
				for(int i = 0; i < celltypes.size(); ++i){
					StringBuilder genotypeConfiguration = new StringBuilder(String.join("", Collections.nCopies(celltypes.size(), "0")));
					genotypeConfiguration.setCharAt(i, '1');
					this.genotypeConfigurationsFullModel.add(genotypeConfiguration.toString());
				}
				for(int i = 0; i < celltypes.size(); ++i){
					StringBuilder genotypeConfiguration = new StringBuilder(String.join("", Collections.nCopies(celltypes.size(), "1")));
					genotypeConfiguration.setCharAt(i, '0');
					this.genotypeConfigurationsFullModel.add(genotypeConfiguration.toString());
				}
			}else{
				throw new RuntimeException("configurationType should be either \"all\" or \"two\", was: "+genotypeConfigurationType);
			}
			this.genotypeConfigurationsCtModel = Utils.binaryPermutations("",celltypes.size()-1, new ArrayList<String>());
		}

		for(String genotypeConfiguration : genotypeConfigurationsFullModel){
			genotypeConfigMap.putIfAbsent(genotypeConfiguration, new ArrayList<String>());
			for(int i = 0; i < genotypeConfiguration.length()-1; i++){
				String s = genotypeConfiguration.substring(0, i);
				String s2 = genotypeConfiguration.substring(i+1, genotypeConfiguration.length());
				String newS = s.concat(s2);
				String ctModelName = "ctModel_"+this.getCellCount().getCelltype(i)+"_"+newS;
				genotypeConfigMap.get(genotypeConfiguration).add(ctModelName);
			}
			String newS = genotypeConfiguration.substring(0, genotypeConfiguration.length()-1);
			String ctModelName = "ctModel_"+this.getCellCount().getCelltype(genotypeConfiguration.length()-1)+"_"+newS;
			genotypeConfigMap.get(genotypeConfiguration).add(ctModelName);
		}
	}
	private List<String> getGenotypeConfigurationsFullModel() throws IllegalAccessException{
		return this.genotypeConfigurationsFullModel;
	}
	private List<String> getGenotypeConfigurationsCtModel() throws IllegalAccessException{
		return this.genotypeConfigurationsCtModel;
	}

	/**
	 * Construct the observed value matrices that are used for calculating the regression for the full model.
	 * Add all permutations of genotypes/swappedGenotypes (swappedGenotypes -> 0=2, 2=0)
	 * 
	 * TODO: Move this to InteractionModel class. Also, merge overlapping code with createObservedValueMatricesCtModel
	 */
	public void createObservedValueMatricesFullModel() 
			throws IllegalAccessException{
		if(this.getUseBaseModel()){
			createObservedValueMatricesBaseModel();
			return;
		}
		CellCount cellCount = getCellCount();
		int numberOfCelltypes = cellCount.getNumberOfCelltypes();
		int numberOfSamples = cellCount.getNumberOfSamples();
		int numberOfTerms = numberOfCelltypes * 2;
		// Have to test which genotype combination is the best, so 2**number of celltype loops
		for (String genotypeConfiguration : getGenotypeConfigurationsFullModel()){
			// things neded for fullModel defined outside of loop because every celltype model (ctModel) has to be compared to it
			InteractionModel fullModel = new InteractionModel(numberOfSamples, 
					numberOfTerms);
			fullModel.setGenotypeConfiguration(genotypeConfiguration);
			String modelName = String.format("fullModel_%s",genotypeConfiguration);
			fullModel.setModelName(modelName);
			addInteractionModel(fullModel, modelName, true);

			for(int celltypeIndex = 0; celltypeIndex < numberOfCelltypes; ++celltypeIndex){
				/** save the index of the variables related to current celltype so that this can be used later to calculate
				 * Beta1 celltype% + Beta2 * celltype%:GT. For fullModel not so necesarry as it's always <numberOfCelltypes> away,
				 * but for ctModel this is easiest method
				 */
				int[] index = new int[] {celltypeIndex, numberOfCelltypes + celltypeIndex};
				fullModel.addCelltypeVariablesIndex(index);
				// add the celltype name at position i so that it gets in front of the celltype:GT
				fullModel.addIndependentVariableName(celltypeIndex, cellCount.getCelltype(celltypeIndex));
				fullModel.addIndependentVariableName(cellCount.getCelltype(celltypeIndex)+":GT");

			}

			// number of terms + 1 because for full model all cell types are included
			for (int sampleIndex = 0; sampleIndex <= numberOfSamples-1; ++sampleIndex) {
				for (int celltypeIndex = 0; celltypeIndex < numberOfCelltypes; ++celltypeIndex) {

					double celltypePerc = cellCount.getCellcountPercentages()[sampleIndex][celltypeIndex];
					// if i (cell type index) is the same as m (model index), don't add the interaction term of celltype:GT
					fullModel.addObservedValue(celltypePerc, sampleIndex, celltypeIndex);
					// Have permutation of (2**number of celltypes) as binary ( so 00, 10, 01, 11 ), when 0 do normal genotype, 1 do swapped genotype
					double[] genotypes;
					char genotypeOrderAtCelltype = genotypeConfiguration.charAt(celltypeIndex);
					// Use the binary string permutation to decide if the genotype should be swapped or not
					if(genotypeOrderAtCelltype == '0'){
						genotypes = getGenotypes();
					} else{
						genotypes = getSwappedGenotypes();
					}
					try {
						fullModel.addObservedValue(celltypePerc * genotypes[sampleIndex], 
								sampleIndex, numberOfCelltypes + celltypeIndex);					
					} catch (ArrayIndexOutOfBoundsException error) {
						throw new RuntimeException(
								"The counts file and expression and/or genotype file do not have equal number of samples or QTLs",
								error);
					}
				}
			}
			fullModel.setModelLength();
		}
	}

	/**
	 * Construct the observed value matrices that are used for calculating the regression for the full model.
	 * Add all permutations of genotypes/swappedGenotypes (swappedGenotypes -> 0=2, 2=0)
	 * 
	 * TODO: Move this to InteractionModel class. Also, merge overlapping code with createObservedValueMatricesCtModel
	 * @throws IllegalAccessException 
	 */
	public void createObservedValueMatricesBaseModel() throws IllegalAccessException{		
		CellCount cellCount = getCellCount();
		int numberOfSamples = cellCount.getNumberOfSamples();
		int numberOfCelltypes = cellCount.getNumberOfCelltypes();
		// y ~ CC1 + (100-CC) + GT*CC + GT*(100-CC) so only 4 terms
		int numberOfTerms = 4;

		/* For every celltype we need to make four models,  with GT*CC flipped and GT*(100-CC) flipped
		 * 
		 * This loops over every celltype by index (celltypeIndex), and then makes the observation matrix so that
		 * cc[celltypeIndex] + (100-cc[allIndexesExceptCelltypeIndex]) + GT*cc[celltypeIndex] + GT*(100-cc[allIndexesExceptCelltypeIndex])
		 */
		for(int celltypeIndex = 0; celltypeIndex < numberOfCelltypes; ++celltypeIndex){
			for (String genotypeConfiguration : getGenotypeConfigurationsFullModel()){
				InteractionModel fullModel = new InteractionModel(numberOfSamples, 
						numberOfTerms);
				fullModel.setGenotypeConfiguration(genotypeConfiguration);
				String celltypeName = cellCount.getCelltype(celltypeIndex);
				String modelName = String.format("fullModel_%s_%s", celltypeName, genotypeConfiguration);
				fullModel.setModelName(modelName);
				fullModel.setCelltypeName(celltypeName);

				addInteractionModel(fullModel, modelName, true);

				fullModel.addCelltypeVariablesIndex(new int[] {0,2});
				fullModel.addCelltypeVariablesIndex(new int[] {1,3});
				fullModel.addIndependentVariableName(celltypeName);
				fullModel.addIndependentVariableName("100-"+celltypeName);
				fullModel.addIndependentVariableName(celltypeName+":GT");
				fullModel.addIndependentVariableName("100-"+celltypeName+":GT");

				for (int sampleIndex = 0; sampleIndex <= numberOfSamples-1; ++sampleIndex) {
					double celltypePerc = cellCount.getCellcountPercentages()[sampleIndex][celltypeIndex];
					double celltypePercRest = 100-celltypePerc;

					// if i (cell type index) is the same as m (model index), don't add the interaction term of celltype:GT
					fullModel.addObservedValue(celltypePerc, sampleIndex, 0);
					fullModel.addObservedValue(celltypePercRest, sampleIndex, 1);

					// unlike with full model, here are only 2 GT's. For cc:GT and for (100-CC):GT
					// check for which of the two is swapped, then set genotypes
					char genotypeOrderAtCelltype = genotypeConfiguration.charAt(0);
					char genotypeOrderAtRest = genotypeConfiguration.charAt(1);

					double[] genotypesCelltype;
					double[] genotypesRest;
					if(genotypeOrderAtCelltype == '0'){
						genotypesCelltype = getGenotypes();
					} else{
						genotypesCelltype = getSwappedGenotypes();
					}
					if(genotypeOrderAtRest == '0'){
						genotypesRest = getGenotypes();
					} else{
						genotypesRest = getSwappedGenotypes();
					}
					try {
						fullModel.addObservedValue(celltypePerc * genotypesCelltype[sampleIndex], sampleIndex, 2);
						fullModel.addObservedValue(celltypePercRest * genotypesRest[sampleIndex], sampleIndex, 3);
					} catch (ArrayIndexOutOfBoundsException error) {
						throw new RuntimeException(
								"The counts file and expression and/or genotype file do not have equal number of samples or QTLs",
								error);
					}
				}
				fullModel.setModelLength();
			}
		}
	}

	public void setCtModelByGenotypeConfiguration() throws IllegalAccessException{		
		InteractionModel bestFullModel = this.getBestFullModel();
		String bestFullModelgenotypeConfiguration = bestFullModel.getGenotypeConfiguration();
		HashMap<String, String> cellTypeCtModelName = new HashMap<String, String>();
		for(String ctModelName : genotypeConfigMap.get(bestFullModelgenotypeConfiguration)){
			String celltype = ctModelName.split("ctModel_")[1].split("_")[0];
			cellTypeCtModelName.put(celltype, ctModelName);
			ctModelByGenotypeConfiguration.putIfAbsent(bestFullModelgenotypeConfiguration, cellTypeCtModelName);			
		}
	}

	public void setCtModel() throws IllegalAccessException{		
		InteractionModel bestFullModel = this.getBestFullModel();
		String bestFullModelgenotypeConfiguration = bestFullModel.getGenotypeConfiguration();
		HashMap<String, String> cellTypeCtModelName = new HashMap<String, String>();
		for(String ctModelName : genotypeConfigMap.get(bestFullModelgenotypeConfiguration)){
			cellTypeCtModelName.put(ctModelName.split("ctModel_")[1].split("_")[0], ctModelName);
			ctModelByGenotypeConfiguration.putIfAbsent(bestFullModelgenotypeConfiguration, cellTypeCtModelName);			
		}
	}


	/**
	 * Construct the observed value matrices that are used for calculating the regression
	 * @param genotypeOrder 
	 * 
	 * @param InteractionModelCollection Collection of InteractionModel objects for saving the results
	 * @param genotypeOrder The order of genotypes to use, e.g. 010 means non swapped genotypes celltype 1, swapped genotypes celltype 2, non swapped genotypes celltype 3
	 * 
	 * TODO: Move this to InteractionModel class. Also, merge overlapping code with createObservedValueMatricesFullModel
	 * @throws IOException 
	 */
	public void createObservedValueMatricesCtModels() 
			throws IllegalAccessException, IOException{
		if(this.getUseBaseModel()){
			createObservedValueMatricesCtBaseModel();
			return;
		}
		CellCount cellCount = getCellCount();
		int numberOfCelltypes = cellCount.getNumberOfCelltypes();
		int numberOfSamples = cellCount.getNumberOfSamples();
		int genotypeCounter = numberOfCelltypes;
		// -1 because one interaction term is removed
		int numberOfTerms = (numberOfCelltypes * 2) - 1;
		for (String genotypeConfiguration : getGenotypeConfigurationsCtModel()){
			// m = model, there are equally many models as celltypes
			for (int modelIndex = 0; modelIndex < numberOfCelltypes; modelIndex++) {
				InteractionModel ctModel = new InteractionModel(numberOfSamples, numberOfTerms);	
				ctModel.setGenotypeConfiguration(genotypeConfiguration);
				// calculate p-value and save it, with other information, in a ctModel object. 
				// Then, add it to a list of these models to return as decon results
				String celltypeName = cellCount.getCelltype(modelIndex);
				String modelName = String.format("ctModel_%s_%s", celltypeName, genotypeConfiguration);
				ctModel.setModelName(modelName);
				ctModel.setCelltypeName(celltypeName);
				addInteractionModel(ctModel,ctModel.getModelName(), false);	
				for (int sampleIndex = 0; sampleIndex <= numberOfSamples-1; sampleIndex++) {
					int configurationIndex = 0;
					for (int celltypeIndex = 0; celltypeIndex < numberOfCelltypes; celltypeIndex++) {
						// There is one fullModel including all celltypes add values for celltypePerc and interaction term of
						// celltypePerc * genotypePerc so that you get [[0.3, 0.6], [0.4, 0.8], [0.2, 0.4], [0.1, 0.2]]
						// where numberOfSamples = 1 and numberOfCellTypes = 4 with celltypePerc = 0.3, 0.4, 0.2, and 0.1 and genotype = 2
						// for each cell type is 1 model, celltype% * genotype without 1 celltype.
						// j+1 because j==0 is header
						double celltype_perc = cellCount.getCellcountPercentages()[sampleIndex][celltypeIndex];
						ctModel.addObservedValue(celltype_perc, sampleIndex, celltypeIndex);
						if(sampleIndex == 0){
							// add the celltype name at position i so that it gets in front of the celltype:GT, but once
							try{
								ctModel.addIndependentVariableName(celltypeIndex, celltypeName);
							}
							catch(NullPointerException e){
								DeconvolutionLogger.log.info(String.format("Nullpoint exception with celltype %s", celltypeIndex));
								throw e;
							}
						}

						// if celltypeIndex is the same as m modelIndex, don't add the interaction term of celltype:GT
						if (celltypeIndex != modelIndex) {
							// Only add IndependentVariableName once per QTL (j==0)
							if(sampleIndex == 0){

								// Add the interaction term of celltype:genotype
								ctModel.addIndependentVariableName(cellCount.getCelltype(celltypeIndex)+":GT");
								// save the index of the variables related to current celltype so that this can be used later to calculate
								// Beta1 celltype% + Beta2 * celltype%:GT. For fullModel not so necesarry as it's always <numberOfCelltypes> away,
								// but for ctModel this is easiest method
								int[] index = new int[] {celltypeIndex, numberOfCelltypes-1+celltypeIndex};
								ctModel.addCelltypeVariablesIndex(index);
								// add the celltype name. This could be done with less code by getting it from IndependentVariableName, but this way 
								// it is explicit. Don't know if better.
							}
							try {
								double genotype = 0;
								// because the genotype configuration is of length (number of celltypes - 1), when a model is skipped we need to 
								// adjust all celltype indices from that point forward
								char genotypeOrderAtCelltype = genotypeConfiguration.charAt(configurationIndex);
								configurationIndex++;
								if(genotypeOrderAtCelltype == '0'){
									genotype = getGenotypes()[sampleIndex];
								}
								else if(genotypeOrderAtCelltype == '1'){
									genotype = getSwappedGenotypes()[sampleIndex];
								}
								else{
									throw new RuntimeException(String.format("Genotype order should be 0 or 1, was: %s", genotypeOrderAtCelltype));
								}
								ctModel.addObservedValue(celltype_perc * genotype, sampleIndex, genotypeCounter);

							} catch (ArrayIndexOutOfBoundsException error) {
								DeconvolutionLogger.log.info("ERROR: The counts file and expression and/or genotype file do not have equal number of samples or QTLs");
								throw error;
							}
							genotypeCounter++;
						}
						// if i==m there is not celltype:GT interaction term so only one index added to CelltypeVariables
						else if (sampleIndex == 0){
							int[] index = new int[] {celltypeIndex};
							ctModel.addCelltypeVariablesIndex(index);
						}
					}
					// because 1 of numberOfCelltypes + i needs to be skipped,
					// keeping it tracked with separate value is easier
					genotypeCounter = cellCount.getNumberOfCelltypes();
				}
				ctModel.setModelLength();

			}
		}
	}

	public void createObservedValueMatricesCtBaseModel() throws IllegalAccessException, IOException{		
		CellCount cellCount = getCellCount();
		int numberOfSamples = cellCount.getNumberOfSamples();
		int numberOfCelltypes = cellCount.getNumberOfCelltypes();
		// y ~ CC1 + (100-CC) + GT*(100-CC) so only 3 terms
		int numberOfTerms = 3;

		/* For every celltype we need to make two models,  with and GT*(100-CC) flipped
		 * 
		 * This loops over every celltype by index (celltypeIndex), and then makes the observation matrix so that
		 * cc[celltypeIndex] + (100-cc[allIndexesExceptCelltypeIndex]) + GT*(100-cc[allIndexesExceptCelltypeIndex])
		 */
		for(int celltypeIndex = 0; celltypeIndex < numberOfCelltypes; ++celltypeIndex){
			for (String genotypeConfiguration : getGenotypeConfigurationsCtModel()){
				InteractionModel ctModel = new InteractionModel(numberOfSamples, 
						numberOfTerms);

				InteractionModel restModel = new InteractionModel(numberOfSamples, 
						numberOfTerms);
				InteractionModel restModelSwapped = new InteractionModel(numberOfSamples, 
						numberOfTerms);
				ctModel.setGenotypeConfiguration(genotypeConfiguration);
				String celltypeName = cellCount.getCelltype(celltypeIndex);


				String modelName = String.format("ctModel_%s_%s", celltypeName, genotypeConfiguration);
				String restModelName = String.format("ctModel_%s_%s_restModel", celltypeName, genotypeConfiguration);
				ctModel.setModelName(modelName);

				ctModel.setCelltypeName(celltypeName);
				addInteractionModel(ctModel, modelName, false);

				ctModel.addCelltypeVariablesIndex(new int[] {0});
				ctModel.addCelltypeVariablesIndex(new int[] {1,3});
				ctModel.addIndependentVariableName(celltypeName);
				ctModel.addIndependentVariableName("100-"+celltypeName);
				ctModel.addIndependentVariableName("100-"+celltypeName+":GT");


				for (int sampleIndex = 0; sampleIndex <= numberOfSamples-1; ++sampleIndex) {
					double celltypePerc = cellCount.getCellcountPercentages()[sampleIndex][celltypeIndex];
					double celltypePercRest = 100-celltypePerc;

					// if i (cell type index) is the same as m (model index), don't add the interaction term of celltype:GT
					ctModel.addObservedValue(celltypePerc, sampleIndex, 0);
					ctModel.addObservedValue(celltypePercRest, sampleIndex, 1);
					restModel.addObservedValue(celltypePerc, sampleIndex, 0);
					restModel.addObservedValue(celltypePercRest, sampleIndex, 1);
					restModelSwapped.addObservedValue(celltypePerc, sampleIndex, 0);
					restModelSwapped.addObservedValue(celltypePercRest, sampleIndex, 1);

					double[] genotypes;
					// There is only on GT for the CT model, cause y ~ cc + (100-cc) + (100-cc):GT
					char genotypeOrderAtCelltype = genotypeConfiguration.charAt(0);
					// Use the binary string permutation to decide if the genotype should be swapped or not
					if(genotypeOrderAtCelltype == '0'){
						genotypes = getGenotypes();
					} else{
						genotypes = getSwappedGenotypes();
					}

					try {
						ctModel.addObservedValue(celltypePercRest * genotypes[sampleIndex], sampleIndex, 2);


						// Two restModels cause don't know which one is the best
						genotypes = getGenotypes();
						restModel.addObservedValue(celltypePerc * genotypes[sampleIndex], sampleIndex, 2);
						genotypes = getSwappedGenotypes();
						restModelSwapped.addObservedValue(celltypePerc * genotypes[sampleIndex], sampleIndex, 2);

					} catch (ArrayIndexOutOfBoundsException error) {
						throw new RuntimeException(
								"The counts file and expression and/or genotype file do not have equal number of samples or QTLs",
								error);
					}
				}
				restModel.calculateSumOfSquaresNNLS(getExpessionValues());
				restModelSwapped.calculateSumOfSquaresNNLS(getExpessionValues());

				ctModel.setModelLength();
				if(restModel.getSumOfSquares() < restModelSwapped.getSumOfSquares()){
					addInteractionModel(restModel, restModelName, false);
				}
				else{
					addInteractionModel(restModelSwapped, restModelName, false);
				}
				ctModel.setRestModel(restModelName);

			}
		}
	}


	public void cleanUp(Boolean removePredictedValues) throws IllegalAccessException {
		this.expressionValues = null;
		this.genotypes = null;
		this.swappedGenotypes = null;
		for(InteractionModel interactionModel : this.interactionModels.values()){
			interactionModel.cleanUp(removePredictedValues);
		}
		this.genotypeConfigurationsCtModel = null;
		this.genotypeConfigurationsFullModel = null;
		this.cellCount = null;
	}

	public List<String> getSampleNames() {
		return sampleNames;
	}
}
