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
import org.hobbit.gspb.util.SNBConstants;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNBEvaluationModule extends AbstractEvaluationModule {

	private static final Logger LOGGER = LoggerFactory.getLogger(SNBEvaluationModule.class);
	
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
	private Property EVALUATION_S1E_AVERAGE_TIME = null;
	private Property EVALUATION_S2E_AVERAGE_TIME = null;
	private Property EVALUATION_S3E_AVERAGE_TIME = null;
	private Property EVALUATION_S4E_AVERAGE_TIME = null;
	private Property EVALUATION_S5E_AVERAGE_TIME = null;
	private Property EVALUATION_S6E_AVERAGE_TIME = null;
	private Property EVALUATION_S7E_AVERAGE_TIME = null;
	private Property EVALUATION_U1E_AVERAGE_TIME = null;
	private Property EVALUATION_U2E_AVERAGE_TIME = null;
	private Property EVALUATION_U3E_AVERAGE_TIME = null;
	private Property EVALUATION_U4E_AVERAGE_TIME = null;
	private Property EVALUATION_U5E_AVERAGE_TIME = null;
	private Property EVALUATION_U6E_AVERAGE_TIME = null;
	private Property EVALUATION_U7E_AVERAGE_TIME = null;
	private Property EVALUATION_U8E_AVERAGE_TIME = null;
	
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
		totalTimePerQueryType.put("S1", (long) 0);
		totalTimePerQueryType.put("S2", (long) 0);
		totalTimePerQueryType.put("S3", (long) 0);
		totalTimePerQueryType.put("S4", (long) 0);
		totalTimePerQueryType.put("S5", (long) 0);
		totalTimePerQueryType.put("S6", (long) 0);
		totalTimePerQueryType.put("S7", (long) 0);
		totalTimePerQueryType.put("U1", (long) 0);
		totalTimePerQueryType.put("U2", (long) 0);
		totalTimePerQueryType.put("U3", (long) 0);
		totalTimePerQueryType.put("U4", (long) 0);
		totalTimePerQueryType.put("U5", (long) 0);
		totalTimePerQueryType.put("U6", (long) 0);
		totalTimePerQueryType.put("U7", (long) 0);
		totalTimePerQueryType.put("U8", (long) 0);
		
		
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
		numberOfQueriesPerQueryType.put("S1", 0);
		numberOfQueriesPerQueryType.put("S2", 0);
		numberOfQueriesPerQueryType.put("S3", 0);
		numberOfQueriesPerQueryType.put("S4", 0);
		numberOfQueriesPerQueryType.put("S5", 0);
		numberOfQueriesPerQueryType.put("S6", 0);
		numberOfQueriesPerQueryType.put("S7", 0);
		numberOfQueriesPerQueryType.put("U1", 0);
		numberOfQueriesPerQueryType.put("U2", 0);
		numberOfQueriesPerQueryType.put("U3", 0);
		numberOfQueriesPerQueryType.put("U4", 0);
		numberOfQueriesPerQueryType.put("U5", 0);
		numberOfQueriesPerQueryType.put("U6", 0);
		numberOfQueriesPerQueryType.put("U7", 0);
		numberOfQueriesPerQueryType.put("U8", 0);
		
		Map<String, String> env = System.getenv();
		
        /* average query time */
        if (!env.containsKey(SNBConstants.EVALUATION_QE_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_QE_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        EVALUATION_QE_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_QE_AVERAGE_TIME));
        
        
        /* average query times per type */
        if (!env.containsKey(SNBConstants.EVALUATION_Q01E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q01E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q02E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q02E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q03E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q03E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q04E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q04E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q05E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q05E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q06E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q06E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q07E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q07E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q08E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q08E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q09E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q09E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q10E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q10E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q11E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q11E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q12E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q12E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q13E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q13E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_Q14E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_Q14E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        EVALUATION_Q01E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q01E_AVERAGE_TIME));
        EVALUATION_Q02E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q02E_AVERAGE_TIME));
        EVALUATION_Q03E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q03E_AVERAGE_TIME));
        EVALUATION_Q04E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q04E_AVERAGE_TIME));
        EVALUATION_Q05E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q05E_AVERAGE_TIME));
        EVALUATION_Q06E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q06E_AVERAGE_TIME));
        EVALUATION_Q07E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q07E_AVERAGE_TIME));
        EVALUATION_Q08E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q08E_AVERAGE_TIME));
        EVALUATION_Q09E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q09E_AVERAGE_TIME));
        EVALUATION_Q10E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q10E_AVERAGE_TIME));
        EVALUATION_Q11E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q11E_AVERAGE_TIME));
        EVALUATION_Q12E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q12E_AVERAGE_TIME));
        EVALUATION_Q13E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q13E_AVERAGE_TIME));
        EVALUATION_Q14E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_Q14E_AVERAGE_TIME));
        if (!env.containsKey(SNBConstants.EVALUATION_S1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_S1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_S2E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_S2E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_S3E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_S3E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_S4E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_S4E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_S5E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_S5E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_S6E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_S6E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_S7E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_S7E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        EVALUATION_S1E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_S1E_AVERAGE_TIME));
        EVALUATION_S2E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_S2E_AVERAGE_TIME));
        EVALUATION_S3E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_S3E_AVERAGE_TIME));
        EVALUATION_S4E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_S4E_AVERAGE_TIME));
        EVALUATION_S5E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_S5E_AVERAGE_TIME));
        EVALUATION_S6E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_S6E_AVERAGE_TIME));
        EVALUATION_S7E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_S7E_AVERAGE_TIME));
        
        if (!env.containsKey(SNBConstants.EVALUATION_U1E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_U1E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_U2E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_U2E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_U3E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_U3E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_U4E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_U4E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_U5E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_U5E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_U6E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_U6E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_U7E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_U7E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        if (!env.containsKey(SNBConstants.EVALUATION_U8E_AVERAGE_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_U8E_AVERAGE_TIME + "\" from the environment. Aborting.");
        }
        EVALUATION_U1E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_U1E_AVERAGE_TIME));
        EVALUATION_U2E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_U2E_AVERAGE_TIME));
        EVALUATION_U3E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_U3E_AVERAGE_TIME));
        EVALUATION_U4E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_U4E_AVERAGE_TIME));
        EVALUATION_U5E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_U5E_AVERAGE_TIME));
        EVALUATION_U6E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_U6E_AVERAGE_TIME));
        EVALUATION_U7E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_U7E_AVERAGE_TIME));
        EVALUATION_U8E_AVERAGE_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_U8E_AVERAGE_TIME));
        
        /* loading time */
        if (!env.containsKey(SNBConstants.EVALUATION_LOADING_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_LOADING_TIME + "\" from the environment. Aborting.");
        }
        EVALUATION_LOADING_TIME = finalModel.createProperty(env.get(SNBConstants.EVALUATION_LOADING_TIME));
        if (!env.containsKey(SNBConstants.EVALUATION_REAL_LOADING_TIME)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_REAL_LOADING_TIME + "\" from the environment. Aborting.");
        }
        loading_time = Long.parseLong(env.get(SNBConstants.EVALUATION_REAL_LOADING_TIME));
        
        /* throughput */
        if (!env.containsKey(SNBConstants.EVALUATION_THROUGHPUT)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_THROUGHPUT + "\" from the environment. Aborting.");
        }
        EVALUATION_THROUGHPUT = finalModel.createProperty(env.get(SNBConstants.EVALUATION_THROUGHPUT));
        
        /* number of wrong answers */
        if (!env.containsKey(SNBConstants.EVALUATION_NUMBER_OF_WRONG_ANSWERS)) {
            throw new IllegalArgumentException("Couldn't get \"" + SNBConstants.EVALUATION_NUMBER_OF_WRONG_ANSWERS + "\" from the environment. Aborting.");
        }
        EVALUATION_NUMBER_OF_WRONG_ANSWERS = finalModel.createProperty(env.get(SNBConstants.EVALUATION_NUMBER_OF_WRONG_ANSWERS));        
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
		Literal s1eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("S1")/numberOfQueriesPerQueryType.get("S1"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("S1") > 0)
			finalModel.add(experiment, EVALUATION_S1E_AVERAGE_TIME, s1eAverageTimeLiteral);
		Literal s2eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("S2")/numberOfQueriesPerQueryType.get("S2"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("S2") > 0)
			finalModel.add(experiment, EVALUATION_S2E_AVERAGE_TIME, s2eAverageTimeLiteral);
		Literal s3eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("S3")/numberOfQueriesPerQueryType.get("S3"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("S3") > 0)
			finalModel.add(experiment, EVALUATION_S3E_AVERAGE_TIME, s3eAverageTimeLiteral);
		Literal s4eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("S4")/numberOfQueriesPerQueryType.get("S4"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("S4") > 0)
			finalModel.add(experiment, EVALUATION_S4E_AVERAGE_TIME, s4eAverageTimeLiteral);
		Literal s5eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("S5")/numberOfQueriesPerQueryType.get("S5"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("S5") > 0)
			finalModel.add(experiment, EVALUATION_S5E_AVERAGE_TIME, s5eAverageTimeLiteral);
		Literal s6eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("S6")/numberOfQueriesPerQueryType.get("S6"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("S6") > 0)
			finalModel.add(experiment, EVALUATION_S6E_AVERAGE_TIME, s6eAverageTimeLiteral);
		Literal s7eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("S7")/numberOfQueriesPerQueryType.get("S7"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("S7") > 0)
			finalModel.add(experiment, EVALUATION_S7E_AVERAGE_TIME, s7eAverageTimeLiteral);
		Literal u1eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("U1")/numberOfQueriesPerQueryType.get("U1"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("U1") > 0)
			finalModel.add(experiment, EVALUATION_U1E_AVERAGE_TIME, u1eAverageTimeLiteral);
		Literal u2eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("U2")/numberOfQueriesPerQueryType.get("U2"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("U2") > 0)
			finalModel.add(experiment, EVALUATION_U2E_AVERAGE_TIME, u2eAverageTimeLiteral);
		Literal u3eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("U3")/numberOfQueriesPerQueryType.get("U3"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("U3") > 0)
			finalModel.add(experiment, EVALUATION_U3E_AVERAGE_TIME, u3eAverageTimeLiteral);
		Literal u4eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("U4")/numberOfQueriesPerQueryType.get("U4"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("U4") > 0)
			finalModel.add(experiment, EVALUATION_U4E_AVERAGE_TIME, u4eAverageTimeLiteral);
		Literal u5eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("U5")/numberOfQueriesPerQueryType.get("U5"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("U5") > 0)
			finalModel.add(experiment, EVALUATION_U5E_AVERAGE_TIME, u5eAverageTimeLiteral);
		Literal u6eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("U6")/numberOfQueriesPerQueryType.get("U6"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("U6") > 0)
			finalModel.add(experiment, EVALUATION_U6E_AVERAGE_TIME, u6eAverageTimeLiteral);
		Literal u7eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("U7")/numberOfQueriesPerQueryType.get("U7"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("U7") > 0)
			finalModel.add(experiment, EVALUATION_U7E_AVERAGE_TIME, u7eAverageTimeLiteral);
		Literal u8eAverageTimeLiteral = finalModel.createTypedLiteral(
				(double)totalTimePerQueryType.get("U8")/numberOfQueriesPerQueryType.get("U8"), XSDDatatype.XSDdouble);
		if (numberOfQueriesPerQueryType.get("U8") > 0)
			finalModel.add(experiment, EVALUATION_U8E_AVERAGE_TIME, u8eAverageTimeLiteral);
		
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
