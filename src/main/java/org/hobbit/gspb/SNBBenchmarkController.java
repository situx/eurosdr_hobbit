package org.hobbit.gspb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.rdf.model.NodeIterator;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.gspb.util.SNBConstants;
import org.hobbit.gspb.util.VirtuosoSystemAdapterConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNBBenchmarkController extends AbstractBenchmarkController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SNBBenchmarkController.class);
	private ArrayList<String> envVariablesEvaluationModule = new ArrayList<String>();;
	private int numberOfOperations = -1;
	private int scaleFactor = -1;
	private int seed = -1;
	private int warmupCount = -1;
	private double timeCompressionRatio = -1;
	private long loadingStarted = -1;
	private long loadingEnded;
	private boolean sequential_tasks = false;
	private String disableEnableQueryType = null;

	// TODO: Add image names of containers
	/* Data generator Docker image */
	protected static final String DATA_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/mspasic/dsb-datagenerator";
	/* Task generator Docker image */
	protected static final String TASK_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/mspasic/dsb-taskgenerator";
	protected static final String SEQ_TASK_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/mspasic/dsb-seqtaskgenerator";
	/* Evaluation module Docker image */
	protected static final String EVALUATION_MODULE_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/mspasic/dsb-evaluationmodule";

	public SNBBenchmarkController() {

	}

	@Override
	public void init() throws Exception {
		LOGGER.info("Initialization begins.");
		super.init();

		// Your initialization code comes here...

		// You might want to load parameters from the benchmarks parameter model
		//	        NodeIterator iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel
		//	                    .getProperty("http://example.org/myParameter"));

		NodeIterator iterator;
		
        /* Number of operations */
        if (numberOfOperations == -1) {

            iterator = benchmarkParamModel.listObjectsOfProperty(
                    benchmarkParamModel.getProperty("http://w3id.org/bench#numberOfOperations"));
            if (iterator.hasNext()) {
                try {
                    numberOfOperations = iterator.next().asLiteral().getInt();
                    LOGGER.info("Number of operations: " + String.valueOf(numberOfOperations));
                } catch (Exception e) {
                    LOGGER.error("Exception while parsing parameter.", e);
                }
            }
            if (numberOfOperations < 0) {
                LOGGER.error("Couldn't get the number of operations from the parameter model. Using the default value.");
                numberOfOperations = 20000;
            }
        }
        
        /* Time compression ratio */
        if (timeCompressionRatio == -1) {
            iterator = benchmarkParamModel.listObjectsOfProperty(
                    benchmarkParamModel.getProperty("http://w3id.org/bench#timeCompressionRatio"));
            if (iterator.hasNext()) {
                try {
                    timeCompressionRatio = iterator.next().asLiteral().getDouble();
                    LOGGER.info("TCR: " + String.valueOf(timeCompressionRatio));
                } catch (Exception e) {
                    LOGGER.error("Exception while parsing parameter.", e);
                }
            }
            if (timeCompressionRatio < 0) {
                LOGGER.error("Couldn't get the initial time compression ratio from the parameter model. Using the default value.");
                timeCompressionRatio = 1.0;
            }
        }
        
        /* Scale Factor */
        if (scaleFactor == -1) {

            iterator = benchmarkParamModel.listObjectsOfProperty(
                    benchmarkParamModel.getProperty("http://w3id.org/bench#hasSF"));
            if (iterator.hasNext()) {
                try {
                    scaleFactor = iterator.next().asLiteral().getInt();
                    LOGGER.info("Scale Factor: " + String.valueOf(scaleFactor));
                } catch (Exception e) {
                    LOGGER.error("Exception while parsing parameter.", e);
                }
            }
            
            if (scaleFactor != 1 && scaleFactor != 3 && scaleFactor != 10 && scaleFactor != 30 && scaleFactor != 100) {
                LOGGER.error("Scale factor can be 1, 3, 10, 30 or 100 (at the moment). Using the default value 1.");
                scaleFactor = 1;
            }
        }
        
        /* Seed */
        if (seed == -1) {

            iterator = benchmarkParamModel.listObjectsOfProperty(
                    benchmarkParamModel.getProperty("http://w3id.org/bench#hasSeed"));
            if (iterator.hasNext()) {
                try {
                    seed = iterator.next().asLiteral().getInt();
                    LOGGER.info("Seed: " + String.valueOf(seed));
                } catch (Exception e) {
                    LOGGER.error("Exception while parsing parameter.", e);
                }
            }
        }

        /* Warmup count */
        if (warmupCount == -1) {

            iterator = benchmarkParamModel.listObjectsOfProperty(
                    benchmarkParamModel.getProperty("http://w3id.org/bench#warmupPercent"));
            if (iterator.hasNext()) {
                try {
                	int warmupPercent;
                	warmupPercent = iterator.next().asLiteral().getInt();
                    if (warmupPercent < 0 || warmupPercent > 100)
                    	warmupPercent = 20;
                    warmupCount = numberOfOperations * warmupPercent / 100;
                    LOGGER.info("Warmup count: " + String.valueOf(warmupCount));
                } catch (Exception e) {
                    LOGGER.error("Exception while parsing parameter.", e);
                }
            }
        }
        
        /* Sequential tasks */
        if (sequential_tasks == false) {

            iterator = benchmarkParamModel.listObjectsOfProperty(
                    benchmarkParamModel.getProperty("http://w3id.org/bench#hasSequentialTasks"));
            if (iterator.hasNext()) {
                try {
                    //sequential_tasks = (iterator.next().asLiteral().getInt() == 0 ? false : true);
                	sequential_tasks = iterator.next().asLiteral().getBoolean();
                	LOGGER.info("Sequential task: " + String.valueOf(sequential_tasks));
                } catch (Exception e) {
                    LOGGER.error("Exception while parsing parameter.", e);
                }
            }
        }
        
        /* Disable/Enable Query types */
        if (disableEnableQueryType == null) {

            iterator = benchmarkParamModel.listObjectsOfProperty(
                    benchmarkParamModel.getProperty("http://w3id.org/bench#enableQueries"));
            if (iterator.hasNext()) {
                try {
                	disableEnableQueryType = iterator.next().asLiteral().getString();
                	LOGGER.info("String: " + disableEnableQueryType);
                } catch (Exception e) {
                    LOGGER.error("Exception while parsing parameter.", e);
                }
            }
        }

		// Create data generators
		int numberOfDataGenerators = 1;
		String[] envVariables = new String[]{
				SNBConstants.GENERATOR_SCALE_FACTOR + "=" + scaleFactor,
				SNBConstants.GENERATOR_NUMBER_OF_OPERATIONS + "=" + numberOfOperations
		};
		createDataGenerators(DATA_GENERATOR_CONTAINER_IMAGE, numberOfDataGenerators, envVariables);

		// Create task generators
		int numberOfTaskGenerators = 1;
		envVariables = new String[] {
				SNBConstants.GENERATOR_SCALE_FACTOR + "=" + scaleFactor,
				SNBConstants.GENERATOR_SEED + "=" + seed,
				SNBConstants.GENERATOR_NUMBER_OF_OPERATIONS + "=" + numberOfOperations,
				SNBConstants.WARMUP_COUNT + "=" + warmupCount,
				SNBConstants.GENERATOR_INITIAL_TIME_COMPRESSION_RATIO + "=" + timeCompressionRatio,
				SNBConstants.DISABLE_ENABLE_QUERY_TYPE + "=" + disableEnableQueryType
		};
		if (sequential_tasks == false)
			createTaskGenerators(TASK_GENERATOR_CONTAINER_IMAGE, numberOfTaskGenerators, envVariables);
		else
			createTaskGenerators(SEQ_TASK_GENERATOR_CONTAINER_IMAGE, numberOfTaskGenerators, envVariables);

		// Create evaluation storage
		envVariables = ArrayUtils.add(DEFAULT_EVAL_STORAGE_PARAMETERS,
                Constants.RABBIT_MQ_HOST_NAME_KEY + "=" + this.rabbitMQHostName);
		if (sequential_tasks == true)
			envVariables = ArrayUtils.add(envVariables, "ACKNOWLEDGEMENT_FLAG=true");
		createEvaluationStorage(DEFAULT_EVAL_STORAGE_IMAGE, envVariables);
		
		// KPIs for evaluation module
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_QE_AVERAGE_TIME + "=" + "http://w3id.org/bench#QEAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q01E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q01EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q02E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q02EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q03E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q03EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q04E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q04EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q05E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q05EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q06E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q06EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q07E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q07EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q08E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q08EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q09E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q09EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q10E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q10EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q11E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q11EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q12E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q12EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q13E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q13EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_Q14E_AVERAGE_TIME + "=" + "http://w3id.org/bench#Q14EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_S1E_AVERAGE_TIME + "=" + "http://w3id.org/bench#S1EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_S2E_AVERAGE_TIME + "=" + "http://w3id.org/bench#S2EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_S3E_AVERAGE_TIME + "=" + "http://w3id.org/bench#S3EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_S4E_AVERAGE_TIME + "=" + "http://w3id.org/bench#S4EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_S5E_AVERAGE_TIME + "=" + "http://w3id.org/bench#S5EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_S6E_AVERAGE_TIME + "=" + "http://w3id.org/bench#S6EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_S7E_AVERAGE_TIME + "=" + "http://w3id.org/bench#S7EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_U1E_AVERAGE_TIME + "=" + "http://w3id.org/bench#U1EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_U2E_AVERAGE_TIME + "=" + "http://w3id.org/bench#U2EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_U3E_AVERAGE_TIME + "=" + "http://w3id.org/bench#U3EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_U4E_AVERAGE_TIME + "=" + "http://w3id.org/bench#U4EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_U5E_AVERAGE_TIME + "=" + "http://w3id.org/bench#U5EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_U6E_AVERAGE_TIME + "=" + "http://w3id.org/bench#U6EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_U7E_AVERAGE_TIME + "=" + "http://w3id.org/bench#U7EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_U8E_AVERAGE_TIME + "=" + "http://w3id.org/bench#U8EAverageTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_LOADING_TIME + "=" + "http://w3id.org/bench#loadingTime");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_THROUGHPUT + "=" + "http://w3id.org/bench#throughput");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_NUMBER_OF_WRONG_ANSWERS + "=" + "http://w3id.org/bench#numberOfWrongAnswers");

		// Wait for all components to finish their initialization
		waitForComponentsToInitialize();

		LOGGER.info("Initialization is over.");
	}

	@Override
	protected void executeBenchmark() throws Exception {
		LOGGER.info("Executing benchmark has started.");

		// give the start signals
		LOGGER.info("Send start signal to Data and Task Generators.");
		sendToCmdQueue(Commands.TASK_GENERATOR_START_SIGNAL);
		sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);

		// wait for the data generators to finish their work
		waitForDataGenToFinish();
		// wait for the task generators to finish their work
		waitForTaskGenToFinish();
		// wait for the system to terminate
		waitForSystemToFinish();

		LOGGER.info("Evaluation in progress...");
		envVariablesEvaluationModule.add(SNBConstants.EVALUATION_REAL_LOADING_TIME + "=" + (loadingEnded - loadingStarted));
		createEvaluationModule(EVALUATION_MODULE_CONTAINER_IMAGE, envVariablesEvaluationModule.toArray(new String[0]));

		// wait for the evaluation to finish
		waitForEvalComponentsToFinish();

		sendResultModel(this.resultModel);

		LOGGER.info("Executing benchmark is over.");

	}
	
    @Override
    public void receiveCommand(byte command, byte[] data) {
    	if (VirtuosoSystemAdapterConstants.BULK_LOAD_DATA_GEN_FINISHED_FROM_DATAGEN == command) {
    		
    		loadingStarted = System.currentTimeMillis();
    		
    		try {
        		try {
        			TimeUnit.SECONDS.sleep(2);
        		} catch (InterruptedException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
				sendToCmdQueue(VirtuosoSystemAdapterConstants.BULK_LOAD_DATA_GEN_FINISHED, data);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	else if (command == VirtuosoSystemAdapterConstants.BULK_LOADING_DATA_FINISHED) {
    		loadingEnded = System.currentTimeMillis();
    	}
    	super.receiveCommand(command, data);	
    }

}
