package org.hobbit.gspb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractEvaluationModule;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.gspb.util.GSPBConstants;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GSPBEvaluationModule extends AbstractEvaluationModule {

	private static final Logger LOGGER = LoggerFactory.getLogger(GSPBEvaluationModule.class);
	
	/* Final evaluation model */
	private Model finalModel = ModelFactory.createDefaultModel();
	/* Property for query execution average time */
	private Property EVALUATION_QE_AVERAGE_TIME = null;
	
	/* Properties for query execution average time per type */
	private Property EVALUATION_Q01E_AVERAGE_TIME = null;
	private Property EVALUATION_Q02E_AVERAGE_TIME = null;
	private Property EVALUATION_Q03E_AVERAGE_TIME = null;
	private Property EVALUATION_Q04E_AVERAGE_TIME = null;
	private Property EVALUATION_Q05E_AVERAGE_TIME = null;
	private Property EVALUATION_Q06E_AVERAGE_TIME = null;
	private Property EVALUATION_Q07E_AVERAGE_TIME = null;
	private Property EVALUATION_Q08E_AVERAGE_TIME = null;
	private Property EVALUATION_Q09E_AVERAGE_TIME = null;
	private Property EVALUATION_Q10E_AVERAGE_TIME = null;
	private Property EVALUATION_Q11E_AVERAGE_TIME = null;
	private Property EVALUATION_Q12E_AVERAGE_TIME = null;
	private Property EVALUATION_Q13E_AVERAGE_TIME = null;
	private Property EVALUATION_Q14E_AVERAGE_TIME = null;
	
	/* Property for loading time" */
	private Property EVALUATION_LOADING_TIME = null;
	
	/* Property for throughput" */
	private Property EVALUATION_THROUGHPUT = null;
	
	/* Property for number of wrong answers" */
	private Property EVALUATION_NUMBER_OF_WRONG_ANSWERS = null;
	
    private ArrayList<String> wrongAnswers = new ArrayList<>();
    private Map<String, ArrayList<Long> > executionTimes = new HashMap<>();
    
    private Map<String, Long> totalTimePerQueryType = new HashMap<>();
    private Map<String, Integer> numberOfQueriesPerQueryType = new HashMap<>();
    
    private long loading_time;
    private long workload_start_time = Long.MAX_VALUE;
    private long workload_end_time = 0;

    @Override
    public void init() throws Exception {
    	LOGGER.info("Initialization begins.");
        // Always init the super class first!
        super.init();
		
		// Your initialization code comes here...
        internalInit();
        
        LOGGER.info("Initialization ends.");
    }
    
	private void internalInit() {
		totalTimePerQueryType.put("Q1", (long) 0);
		totalTimePerQueryType.put("Q2", (long) 0);
		totalTimePerQueryType.put("Q3", (long) 0);
		totalTimePerQueryType.put("Q4", (long) 0);
		totalTimePerQueryType.put("Q5", (long) 0);
		totalTimePerQueryType.put("Q6", (long) 0);
		totalTimePerQueryType.put("Q7", (long) 0);
		totalTimePerQueryType.put("Q8", (long) 0);
		totalTimePerQueryType.put("Q9", (long) 0);
		totalTimePerQueryType.put("Q10", (long) 0);
		totalTimePerQueryType.put("Q11", (long) 0);
		totalTimePerQueryType.put("Q12", (long) 0);
		totalTimePerQueryType.put("Q13", (long) 0);
		totalTimePerQueryType.put("Q14", (long) 0);
		
		numberOfQueriesPerQueryType.put("Q1", 0);
		numberOfQueriesPerQueryType.put("Q2", 0);
		numberOfQueriesPerQueryType.put("Q3", 0);
		numberOfQueriesPerQueryType.put("Q4", 0);
		numberOfQueriesPerQueryType.put("Q5", 0);
		numberOfQueriesPerQueryType.put("Q6", 0);
		numberOfQueriesPerQueryType.put("Q7", 0);
		numberOfQueriesPerQueryType.put("Q8", 0);
		numberOfQueriesPerQueryType.put("Q9", 0);
		numberOfQueriesPerQueryType.put("Q10", 0);
		numberOfQueriesPerQueryType.put("Q11", 0);
		numberOfQueriesPerQueryType.put("Q12", 0);
		numberOfQueriesPerQueryType.put("Q13", 0);
		numberOfQueriesPerQueryType.put("Q14", 0);
		
		Map<String, String> env = System.getenv();
		
        /* average query time */
        if (!env.containsKey(GSPBConstants.EVALUATION_QE_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_QE_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        EVALUATION_QE_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_QE_AVERAGE_TIME));
        
        
        /* average query times per type */
        if (!env.containsKey(GSPBConstants.EVALUATION_Q19_1_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q19_1_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q19_2_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q19_2_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q19_3_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q19_3_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q19_4_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q19_4_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q19_5_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q19_5_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q19_6_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q19_6_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q19_7_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q19_7_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q19_8_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q19_8_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q19_9_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q19_9_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_1_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_1_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_1_2E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_1_2E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_3_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_3_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_3_2E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_3_2E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_3_3E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_3_3E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_3_4E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_3_4E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_3_5E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_3_5E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_4_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_4_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_5_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_5_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_6_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_6_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(GSPBConstants.EVALUATION_Q22_7_1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_Q22_7_1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        EVALUATION_Q01E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q01E_AVERAGE_TIME));
        EVALUATION_Q02E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q02E_AVERAGE_TIME));
        EVALUATION_Q03E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q03E_AVERAGE_TIME));
        EVALUATION_Q04E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q04E_AVERAGE_TIME));
        EVALUATION_Q05E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q05E_AVERAGE_TIME));
        EVALUATION_Q06E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q06E_AVERAGE_TIME));
        EVALUATION_Q07E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q07E_AVERAGE_TIME));
        EVALUATION_Q08E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q08E_AVERAGE_TIME));
        EVALUATION_Q09E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q09E_AVERAGE_TIME));
        EVALUATION_Q10E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q10E_AVERAGE_TIME));
        EVALUATION_Q11E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q11E_AVERAGE_TIME));
        EVALUATION_Q12E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q12E_AVERAGE_TIME));
        EVALUATION_Q13E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q13E_AVERAGE_TIME));
        EVALUATION_Q14E_AVERAGE_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_Q14E_AVERAGE_TIME));
        
        /* loading time */
        if (!env.containsKey(GSPBConstants.EVALUATION_LOADING_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_LOADING_TIME + "\" from the environment. Aborting.");
        }
        EVALUATION_LOADING_TIME = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_LOADING_TIME));
        if (!env.containsKey(GSPBConstants.EVALUATION_REAL_LOADING_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_REAL_LOADING_TIME + "\" from the environment. Aborting.");
        }
        loading_time = Long.parseLong(env.get(GSPBConstants.EVALUATION_REAL_LOADING_TIME));
        
        /* throughput */
        if (!env.containsKey(GSPBConstants.EVALUATION_THROUGHPUT)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_THROUGHPUT + "\" from the environment. Aborting.");
        }
        EVALUATION_THROUGHPUT = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_THROUGHPUT));
        
        /* number of wrong answers */
        if (!env.containsKey(GSPBConstants.EVALUATION_NUMBER_OF_WRONG_ANSWERS)) {
            throw new IllegalArgumentException("Couldn't get \"" + GSPBConstants.EVALUATION_NUMBER_OF_WRONG_ANSWERS + "\" from the environment. Aborting.");
        }
        EVALUATION_NUMBER_OF_WRONG_ANSWERS = finalModel.createProperty(env.get(GSPBConstants.EVALUATION_NUMBER_OF_WRONG_ANSWERS));        
	}

	@Override
	protected void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp,
			long responseReceivedTimestamp) throws Exception {
		String eStr = RabbitMQUtils.readString(expectedData);
    	String rStr = RabbitMQUtils.readString(receivedData);
        String [] lines = eStr.split("\n\n", 2);
        String type = eStr.substring(1, 4).trim();
        
        if (type.startsWith("WRM"))
        	return;
        
        if (!type.startsWith("U")) {
        	if (!lines[1].trim().equals(rStr.trim())) {
        		wrongAnswers.add(lines[0] + " : " + lines[1].length() + " - " + rStr.length());
        		wrongAnswers.add(lines[1].trim());
        		wrongAnswers.add(rStr.trim());
        	}
        }
        
        if (!executionTimes.containsKey(type))
        	executionTimes.put(type, new ArrayList<Long>());
        executionTimes.get(type).add(responseReceivedTimestamp - taskSentTimestamp);
        
        if (taskSentTimestamp < workload_start_time)
        	workload_start_time = taskSentTimestamp;
        if (responseReceivedTimestamp > workload_end_time)
        	workload_end_time = responseReceivedTimestamp;
	}

	@Override
	protected Model summarizeEvaluation() throws Exception {
		// All tasks/responsens have been evaluated. Summarize the results,
		// write them into a Jena model and send it to the benchmark controller.
		LOGGER.info("Summarize evaluation...");
		//TODO: remove this sleeping
		try {
			TimeUnit.SECONDS.sleep(30);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        if (experimentUri == null)
            experimentUri = System.getenv().get(Constants.HOBBIT_EXPERIMENT_URI_KEY);
		
		Resource experiment = finalModel.createResource(experimentUri);
		finalModel.add(experiment, RDF.type, HOBBIT.Experiment);
		
		long totalMS = 0;
		long totalQueries = 0;
		for (Map.Entry<String, ArrayList<Long>> entry : executionTimes.entrySet()) {
    		long totalMSPerQueryType = 0;
    		for (long l : entry.getValue()) {
				totalMSPerQueryType += l;
			}
    		numberOfQueriesPerQueryType.put(entry.getKey(), entry.getValue().size());
    		totalTimePerQueryType.put(entry.getKey(), totalMSPerQueryType);
    		
    		totalMS += totalMSPerQueryType;
    		totalQueries += entry.getValue().size();
    		    		    		
    		LOGGER.info(entry.getKey() + "-" + ((double)totalMSPerQueryType)/ entry.getValue().size());
		}
		LOGGER.info("Loading time - " + loading_time);
		
		Literal qeAverageTimeLiteral = finalModel.createTypedLiteral((double)totalMS / totalQueries, XSDDatatype.XSDdouble);
		finalModel.add(experiment, EVALUATION_QE_AVERAGE_TIME, qeAverageTimeLiteral);
		
		Literal q01eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q1")/numberOfQueriesPerQueryType.get("Q1"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q1") > 0)
			finalModel.add(experiment, EVALUATION_Q01E_AVERAGE_TIME, q01eAverageTimeLiteral);
		Literal q02eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q2")/numberOfQueriesPerQueryType.get("Q2"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q2") > 0)
			finalModel.add(experiment, EVALUATION_Q02E_AVERAGE_TIME, q02eAverageTimeLiteral);
		Literal q03eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q3")/numberOfQueriesPerQueryType.get("Q3"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q3") > 0)
			finalModel.add(experiment, EVALUATION_Q03E_AVERAGE_TIME, q03eAverageTimeLiteral);
		Literal q04eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q4")/numberOfQueriesPerQueryType.get("Q4"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q4") > 0)
			finalModel.add(experiment, EVALUATION_Q04E_AVERAGE_TIME, q04eAverageTimeLiteral);
		Literal q05eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q5")/numberOfQueriesPerQueryType.get("Q5"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q5") > 0)
			finalModel.add(experiment, EVALUATION_Q05E_AVERAGE_TIME, q05eAverageTimeLiteral);
		Literal q06eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q6")/numberOfQueriesPerQueryType.get("Q6"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q6") > 0)
			finalModel.add(experiment, EVALUATION_Q06E_AVERAGE_TIME, q06eAverageTimeLiteral);
		Literal q07eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q7")/numberOfQueriesPerQueryType.get("Q7"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q7") > 0)
			finalModel.add(experiment, EVALUATION_Q07E_AVERAGE_TIME, q07eAverageTimeLiteral);
		Literal q08eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q8")/numberOfQueriesPerQueryType.get("Q8"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q8") > 0)
			finalModel.add(experiment, EVALUATION_Q08E_AVERAGE_TIME, q08eAverageTimeLiteral);
		Literal q09eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q9")/numberOfQueriesPerQueryType.get("Q9"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q9") > 0)
			finalModel.add(experiment, EVALUATION_Q09E_AVERAGE_TIME, q09eAverageTimeLiteral);
		Literal q10eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q10")/numberOfQueriesPerQueryType.get("Q10"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q10") > 0)
			finalModel.add(experiment, EVALUATION_Q10E_AVERAGE_TIME, q10eAverageTimeLiteral);
		Literal q11eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q11")/numberOfQueriesPerQueryType.get("Q11"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q11") > 0)
			finalModel.add(experiment, EVALUATION_Q11E_AVERAGE_TIME, q11eAverageTimeLiteral);
		Literal q12eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q12")/numberOfQueriesPerQueryType.get("Q12"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q12") > 0)
			finalModel.add(experiment, EVALUATION_Q12E_AVERAGE_TIME, q12eAverageTimeLiteral);
		Literal q13eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q13")/numberOfQueriesPerQueryType.get("Q13"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q13") > 0)
			finalModel.add(experiment, EVALUATION_Q13E_AVERAGE_TIME, q13eAverageTimeLiteral);
		Literal q14eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("Q14")/numberOfQueriesPerQueryType.get("Q14"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("Q14") > 0)
			finalModel.add(experiment, EVALUATION_Q14E_AVERAGE_TIME, q14eAverageTimeLiteral);
		
		Literal loadingTimeLiteral = finalModel.createTypedLiteral(loading_time, XSDDatatype.XSDlong);
		finalModel.add(experiment, EVALUATION_LOADING_TIME, loadingTimeLiteral);
		
		Literal throughputLiteral = finalModel.createTypedLiteral((double)totalQueries * 1000 / (workload_end_time - workload_start_time), XSDDatatype.XSDdouble);
		finalModel.add(experiment, EVALUATION_THROUGHPUT, throughputLiteral);
		
		Literal nbrWrngAnswrsLiteral = finalModel.createTypedLiteral(wrongAnswers.size()/3, XSDDatatype.XSDlong);
		finalModel.add(experiment, EVALUATION_NUMBER_OF_WRONG_ANSWERS, nbrWrngAnswrsLiteral);

		// Log the wrong answers
    	for (int i = 0; i < wrongAnswers.size(); i+=3) {
    		LOGGER.info("Wrong answer on query:");
    		LOGGER.info(wrongAnswers.get(i));
    		LOGGER.info("Expected:" + "(" + wrongAnswers.get(i+1) + ")");
    		LOGGER.info("Actual:" + "(" + wrongAnswers.get(i+2) + ")");
		}
    	
    	LOGGER.info(finalModel.toString());
        return finalModel;
	}
	
    @Override
	public void close() throws IOException {
		// Free the resources you requested here
    	LOGGER.info("End of evaluation.");
		
        // Always close the super class after yours!
        super.close();
    }

}
