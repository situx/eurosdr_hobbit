package org.hobbit.gspb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.AbstractSequencingTaskGenerator;
import org.hobbit.core.components.AbstractTaskGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.gspb.util.SNBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNBSeqTaskGenerator extends AbstractSequencingTaskGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(SNBSeqTaskGenerator.class);
	
    private HashMap<Long, String> placeMap;
    private HashMap<Long, String> companyMap;
    private HashMap<Long, String> universityMap;
    private HashMap<Long, String> tagMap;
    private int scaleFactor;
    private int seed;
    private int numberOfOperations;
    private int warmupCount;
    private String disableEnableQueryType;
    private boolean finished = false;
    
    private double timeCompressionRatio;
    
    String [][] params;
    String [] answers;
    Random [] rndms;
    int [] frequency;
    
    long numberOfUpdates = 0;
    int selectId = 0;
	
    public SNBSeqTaskGenerator() {
    	super();
	}
    
    @Override
    public void init() throws Exception {
        LOGGER.info("Initialization begins.");
        super.init();
        
        internalInit();
        LOGGER.info("Initialization is over.");
    }
    
    private void internalInit() {
    	placeMap = readMappings("mappings/places.txt");
    	companyMap = readMappings("mappings/companies.txt");
    	universityMap = readMappings("mappings/universities.txt");
    	tagMap = readMappings("mappings/tags.txt");
    	
    	Map<String, String> env = System.getenv();
    	timeCompressionRatio = Double.parseDouble(env.get(SNBConstants.GENERATOR_INITIAL_TIME_COMPRESSION_RATIO));
    	LOGGER.info("TCR: " + String.valueOf(timeCompressionRatio));
    	
    	scaleFactor = Integer.parseInt(env.get(SNBConstants.GENERATOR_SCALE_FACTOR));
    	seed = Integer.parseInt(env.get(SNBConstants.GENERATOR_SEED));
    	disableEnableQueryType = env.get(SNBConstants.DISABLE_ENABLE_QUERY_TYPE);
    	LOGGER.info("D/E query type: " + disableEnableQueryType);
    	
    	// reading query parameters
    	String directory = "https://hobbitdata.informatik.uni-leipzig.de/MOCHA_OC/T2/sf" + scaleFactor + "/substitution_parameters/";
    	params = new String[22][];
    	for (int i = 1; i <= 21; i++) {
	    	String paramFile = directory + "query_" + String.valueOf(i) + "_param.txt";
			try {
				InputStream inputStream = new URL(paramFile).openStream();
				String fileContent = IOUtils.toString(inputStream);
				params[i] = fileContent.replaceFirst(".*\n", "").split("\n");
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	// Number of operations
    	if (!env.containsKey(SNBConstants.GENERATOR_NUMBER_OF_OPERATIONS)) {
            LOGGER.error("Couldn't get \"" + SNBConstants.GENERATOR_NUMBER_OF_OPERATIONS + "\" from the properties. Aborting.");
            System.exit(1);
        }
    	numberOfOperations = Integer.parseInt(env.get(SNBConstants.GENERATOR_NUMBER_OF_OPERATIONS));
    	
    	/* warmUp count */
    	if (!env.containsKey(SNBConstants.WARMUP_COUNT)) {
            LOGGER.error("Couldn't get \"" + SNBConstants.WARMUP_COUNT + "\" from the properties. Aborting.");
            System.exit(1);
        }
        warmupCount = Integer.parseInt(env.get(SNBConstants.WARMUP_COUNT));
    	
    	rndms = new Random[22];
        for (int i = 1; i <= 21; i++) {
        	rndms[i] = new Random(seed + i);
        }
        
        /* Frequencies */
        frequency = new int[22];
    	try (BufferedReader br = new BufferedReader(new FileReader("workload/frequencies.txt"))) {
    	    String line;
    	    while ((line = br.readLine()) != null) {
    	       String [] parts = line.split("=");
    	       frequency[ Integer.valueOf(parts[0].replaceAll("[^0-9]", "")) ] = Integer.valueOf(parts[1]);
    	    }
    	    br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        for (int i = 1; i <= 21; i++) {
	        if (frequency[i] <= 0) {
	        	if (i > 14)
	        		LOGGER.info("Short query " + String.valueOf(i-14) + " disabled");
	        	else
	        		LOGGER.info("Query " + String.valueOf(i) + " disabled");
	        }
        }
        
    	// reading query answers
    	String answersFile = "https://hobbitdata.informatik.uni-leipzig.de/MOCHA_OC/T2/sf" + scaleFactor + "/answers.txt";
		try {
			InputStream inputStream = new URL(answersFile).openStream();
			String fileContent = IOUtils.toString(inputStream);
			answers = fileContent.split("--------------------\n");
			inputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private HashMap<Long, String> readMappings(String path) {
		HashMap<Long, String> map = new HashMap<>();
    	try (BufferedReader br = new BufferedReader(new FileReader(path))) {
    	    String line;
    	    while ((line = br.readLine()) != null) {
    	       String [] parts = line.split(" - ");
    	       map.put(Long.valueOf(parts[0]), parts[1]);
    	    }
    	    br.close();
    	} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return map;
	}

	@Override
	public void close() throws IOException {
		// Free the resources you requested here

		
        // Always close the super class after yours!
        super.close();
    }


	@Override
	protected void generateTask(byte[] data) throws Exception {
		if (finished)
			return;
			
        String dataString = RabbitMQUtils.readString(data);
        
        String [] parts = dataString.split("[|]");
        String queryText = prepareUpdateText(dataString);
        
        byte[] task = RabbitMQUtils.writeByteArrays(new byte[][] { RabbitMQUtils.writeString(queryText) });
        
        int updateQueryType = queryText.charAt(2) - '0';
        if (disableEnableQueryType.length() < 21 + updateQueryType || disableEnableQueryType.charAt(21 + updateQueryType - 1) != '0') {
        	String taskIdString = getNextTaskId();
            if (Long.valueOf(taskIdString) >= numberOfOperations) {
            	finished = true;
    			return;
            }
            if (taskIdString.endsWith("00"))
            	LOGGER.info("Generating task " + taskIdString);
            
            // DEBUG
            //LOGGER.info("### " + taskIdString + ": " + queryText.split("\n")[0].replace("#", ""));
            //LOGGER.info(queryText);

        	long timestamp = System.currentTimeMillis();
        	sendTaskToSystemAdapter(taskIdString, task);

        	if (Long.valueOf(taskIdString) < warmupCount)
        		data = RabbitMQUtils.writeString("#WRM");
        	else
        		data = RabbitMQUtils.writeString(queryText.substring(0, 4));
        	sendTaskToEvalStorage(taskIdString, timestamp, data);
        }
    	
    	numberOfUpdates++;
    	
    	for (int i = 1; i <= 21; i++) {
	    	if (frequency[i] > 0 && numberOfUpdates % frequency[i] == 0) {
	    		if (i > disableEnableQueryType.length() || disableEnableQueryType.charAt(i-1) != '0') {
		    		String taskIdString = getNextTaskId();
		            if (taskIdString.endsWith("00"))
		            	LOGGER.info("Generating task " + taskIdString);
		            
					queryText = prepareQueryText(i, params[i][rndms[i].nextInt(params[i].length)]);
					
			        // DEBUG
			        //LOGGER.info("### " + taskIdString + ": " + queryText.split("\n")[0].replace("#", ""));
			        //LOGGER.info(queryText);
					
					task = RabbitMQUtils.writeByteArrays(new byte[][] { RabbitMQUtils.writeString(queryText) });
					long timestamp = System.currentTimeMillis();
					sendTaskToSystemAdapter(taskIdString, task);
					
					String a = (answers.length <= selectId ? "TODO" : answers[selectId++]);
					if (Long.valueOf(taskIdString) < warmupCount)
		        		data = RabbitMQUtils.writeString("#WRM");
		        	else
		        		data = RabbitMQUtils.writeString(queryText + "\n\n" + a);
					sendTaskToEvalStorage(taskIdString, timestamp, data);
	    		}
	    	}
    	}

	}
	
    private String prepareUpdateText(String text) throws Exception {
    	String [] parts = text.split("[|]", -1);
    	String queryString = "#U" + parts[2] + "\n" + preparePrefixes();
    	queryString += "INSERT DATA { GRAPH <https://github.com/hobbit-project/sparql-snb> { \n" + prepareTriplets(parts) + "\n}\n}\n";
		return queryString;
	}
    
	private String preparePrefixes() {
		return  "PREFIX xsd:         <http://www.w3.org/2001/XMLSchema#>\n" +
				"PREFIX snvoc:       <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/>\n" +
				"PREFIX sn:          <http://www.ldbc.eu/ldbc_socialnet/1.0/data/>\n" +
				"PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n" +
				"PREFIX dbpedia:     <http://dbpedia.org/resource/>\n" +
				"PREFIX foaf:        <http://xmlns.com/foaf/0.1/>\n" +
				"PREFIX rdfs:        <http://www.w3.org/2000/01/rdf-schema#>\n";
	}

	private String prepareTriplets(String [] parts) throws UnsupportedEncodingException {
    	int updateType = Integer.parseInt(parts[2]);
    	// Add person
		if (updateType == 1) {
	    	long personId = Long.parseLong(parts[3]);
	    	String personFirstName = parts[4];
	    	String personLastName = parts[5];
	    	String gender = parts[6];
	        Date birthday = null;
	        if (!parts[7].equals(""))
	        	birthday = new Date(Long.parseLong(parts[7]));
	        Date creationDate = new Date(Long.parseLong(parts[8]));
	        String locationIp = parts[9];
	        String browserUsed = parts[10];
	        long cityId = Long.parseLong(parts[11]);
	        List<String> languages = Arrays.asList(parts[12].split("[;]"));
			if (languages.size() == 1 && languages.get(0).equals(""))
				languages = new ArrayList<>();
	        List<String> emails = Arrays.asList(parts[13].split("[;]"));
			if (emails.size() == 1 && emails.get(0).equals(""))
				emails = new ArrayList<>();
	        List<String> tagIdsStr = Arrays.asList(parts[14].split("[;]"));
			if (tagIdsStr.size() == 1 && tagIdsStr.get(0).equals(""))
				tagIdsStr = new ArrayList<>();
	        List<Long> tagIds = new ArrayList<Long>();
	        for (String s : tagIdsStr) {
				tagIds.add(Long.parseLong(s));
			}
	        List<String> studyAtStr = Arrays.asList(parts[15].split("[;,]"));
	        if (studyAtStr.size() == 1 && studyAtStr.get(0).equals(""))
	        	studyAtStr = new ArrayList<>();
	        List<Long> studyAt = new ArrayList<Long>();
	        for (String s : studyAtStr) {
	        	studyAt.add(Long.parseLong(s));
			}
	        List<String> workAtStr = Arrays.asList(parts[16].split("[;,]"));
	        if (workAtStr.size() == 1 && workAtStr.get(0).equals(""))
	        	workAtStr = new ArrayList<>();
	        List<Long> workAt = new ArrayList<Long>();
	        for (String s : workAtStr) {
	        	workAt.add(Long.parseLong(s));
			}
	        			
			String personUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", personId) + ">";
            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
            DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
            df1.setTimeZone(TimeZone.getTimeZone("GMT"));
            df2.setTimeZone(TimeZone.getTimeZone("GMT"));
            
			List<String> triplets = new ArrayList<String>();
			triplets.add(personUri + " a <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/Person> .");
            if (!personFirstName.equals(""))
                triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/firstName> \"" + new String(personFirstName.getBytes("UTF-8"), "ISO-8859-1") + "\" .");
            if (!personLastName.equals(""))
                triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/lastName> \"" + new String(personLastName.getBytes("UTF-8"), "ISO-8859-1") + "\" .");
            
            triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/gender> \"" + gender + "\" .");
            if (birthday != null)
                triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/birthday> \"" + df2.format(birthday) + "\"^^xsd:date .");
            triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/creationDate> \"" + df1.format(creationDate) + "\"^^xsd:dateTime .");
            triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/locationIP> \"" + locationIp + "\" .");
            triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/browserUsed> \"" + browserUsed + "\" .");
            triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/isLocatedIn> <" + placeUri(cityId) + "> .");
            triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/id> \"" + personId + "\"^^xsd:long .");
            for (int k = 0; k < languages.size(); k++)
            	triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/speaks> \"" + languages.get(k) + "\" .");
            for (int k = 0; k < emails.size(); k++)
                    triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/email> \"" + emails.get(k) + "\" .");
            for (int k = 0; k < tagIds.size(); k++)
                triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasInterest> <" + tagUri(tagIds.get(k)) + "> .");
            for (int k = 0; k < studyAt.size(); k+=2)
                triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/studyAt> [ <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasOrganisation> <" + universityUri(studyAt.get(k)) + ">; <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/classYear> \"" + studyAt.get(k+1) + "\"] .");
            for (int k = 0; k < workAt.size(); k+=2)
                triplets.add(personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/workAt> [ <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasOrganisation> <" + companyUri(workAt.get(k)) + ">; <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/workFrom> \"" + workAt.get(k+1) + "\"] .");
            return String.join("\n", triplets);
		}
		// Add PostLike
		else if (updateType == 2) {
			long personId = Long.parseLong(parts[3]);
			long postId = Long.parseLong(parts[4]);
			Date creationDate = new Date(Long.parseLong(parts[5]));
			String personUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", personId) + ">";
            String postUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/post" + String.format("%020d", postId) + ">";
            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
            df1.setTimeZone(TimeZone.getTimeZone("GMT"));
            String triplets [] = new String[1];
            triplets[0] = personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/likes> [ <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasPost> " + postUri + "; <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/creationDate> \"" + df1.format(creationDate) + "\"^^xsd:dateTime ] .";
            return String.join("\n", triplets);
		}
		// Add CommentLike
		else if (updateType == 3) {
			long personId = Long.parseLong(parts[3]);
			long commentId = Long.parseLong(parts[4]);
			Date creationDate = new Date(Long.parseLong(parts[5]));
			String personUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", personId) + ">";
            String commentUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/comm" + String.format("%020d", commentId) + ">";
            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
            df1.setTimeZone(TimeZone.getTimeZone("GMT"));
            String triplets [] = new String[1];
            triplets[0] = personUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/likes> [ <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasComment> " + commentUri + "; <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/creationDate> \"" + df1.format(creationDate) + "\"^^xsd:dateTime ] .";
            return String.join("\n", triplets);
		}
		// Add Forum
		else if (updateType == 4) {
			long forumId = Long.parseLong(parts[3]);
			String forumTitle = parts[4];
			Date creationDate = new Date(Long.parseLong(parts[5]));
			long moderatorPersonId = Long.parseLong(parts[6]);
			List<String> tagIdsStr = Arrays.asList(parts[7].split("[;]"));
			if (tagIdsStr.size() == 1 && tagIdsStr.get(0).equals(""))
				tagIdsStr = new ArrayList<>();
	        List<Long> tagIds = new ArrayList<Long>();
	        for (String s : tagIdsStr) {
				tagIds.add(Long.parseLong(s));
			}
			String forumUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/forum" + String.format("%020d", forumId) + ">";
			String moderatorUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", moderatorPersonId) + ">";
			DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
			df1.setTimeZone(TimeZone.getTimeZone("GMT"));
			String triplets [] = new String[5 + tagIds.size()];
			triplets[0] = forumUri + " a <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/Forum> .";
			triplets[1] = forumUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/title> \"" + new String(forumTitle.getBytes("UTF-8"), "ISO-8859-1") + "\" .";
			triplets[2] = forumUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/creationDate> \"" + df1.format(creationDate) + "\"^^xsd:dateTime .";
			triplets[3] = forumUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasModerator> " + moderatorUri + " .";
			triplets[4] = forumUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/id> \"" + forumId + "\"^^xsd:long . ";
			for (int k = 0; k < tagIds.size(); k++)
				triplets[5 + k] = forumUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasTag> <" + tagUri(tagIds.get(k)) + "> .";
			return String.join("\n", triplets);
		}
		// Add Forum Membership
		else if (updateType == 5) {
			long personId = Long.parseLong(parts[3]);
			long forumId = Long.parseLong(parts[4]);
			Date joinDate = new Date(Long.parseLong(parts[5]));
			String forumUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/forum" + String.format("%020d", forumId) + ">";
            String memberUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", personId) + ">";
            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
            df1.setTimeZone(TimeZone.getTimeZone("GMT"));
            String triplets [] = new String[1];
            triplets[0] = forumUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasMember> [ <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasPerson> " + memberUri + "; <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/joinDate> \"" + df1.format(joinDate) + "\"] .";
            return String.join("\n", triplets);
		}
		// Add Post
		else if (updateType == 6) {
			long postId = Long.parseLong(parts[3]);
			String imageFile = parts[4];
			Date creationDate = new Date(Long.parseLong(parts[5]));
			String locationIp = parts[6];
			String browserUsed = parts[7];
			String language = parts[8];
			String content = parts[9];
			int length = Integer.parseInt(parts[10]);
			long authorPersonId = Long.parseLong(parts[11]);
			long forumId = Long.parseLong(parts[12]);
			Long countryId = (long) -1;
			if (!parts[13].equals(""))
				countryId = Long.parseLong(parts[13]);
			List<String> tagIds1 = Arrays.asList(parts[14].split(";"));
			if (tagIds1.size() == 1 && tagIds1.get(0).equals(""))
				tagIds1 = new ArrayList<>();
			List<Long> tagIds = new ArrayList<Long>();
			for (String s : tagIds1)
				tagIds.add(Long.valueOf(s));
			List<String> mentionedIds = Arrays.asList(parts[15].split(";"));
			if (mentionedIds.size() == 1 && mentionedIds.get(0).equals(""))
				mentionedIds = new ArrayList<>();
			String privacy = parts[16];
			String link = parts[17];

            String postUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/post" + String.format("%020d", postId) + ">";
            String forumUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/forum" + String.format("%020d", forumId) + ">";
            String authorUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", authorPersonId) + ">";
            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
            df1.setTimeZone(TimeZone.getTimeZone("GMT"));

            List<String> triplets = new ArrayList<String>();
            triplets.add(postUri + " a <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/Post> .");
            triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/locationIP> \"" + locationIp + "\" .");
            triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/creationDate> \"" + df1.format(creationDate) + "\"^^xsd:dateTime .");
            triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/browserUsed> \"" + browserUsed + "\" .");
            if (imageFile.equals("")) {
            	triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/language> \"" + language + "\" .");
            	triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/content> \"" + new String(content.getBytes("UTF-8"), "ISO-8859-1") + "\" .");
            	triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/length> \"" + length + "\" .");
            }
            else {
            	triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/imageFile> \"" + imageFile + "\" .");
            }
            triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasCreator> " + authorUri + " .");
            triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/id> \"" + postId + "\"^^xsd:long .");
            triplets.add(forumUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/containerOf> " + postUri + " .");
            if (countryId >= 0)
                triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/isLocatedIn> <" + placeUri(countryId) + "> .");
            for (int k = 0; k < tagIds.size(); k++)
                triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasTag> <" + tagUri(tagIds.get(k)) + "> .");
            for (int l = 0; l < mentionedIds.size(); l++)
                triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/mentions> <http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", Long.parseLong(mentionedIds.get(l))) + "> .");
            if (privacy != null && !privacy.equals(""))
                triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/visible> \"" + privacy + "\"^^xsd:boolean .");
            if (!link.equals(""))
                triplets.add(postUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/links> \"" + link + "\" .");
            return String.join("\n", triplets);
		}
		// Add Comment
		else if (updateType == 7) {
			long commentId = Long.parseLong(parts[3]);
			Date creationDate = new Date(Long.parseLong(parts[4]));
			String locationIp = parts[5];
			String browserUsed = parts[6];
			String content = parts[7];
			int length = Integer.parseInt(parts[8]);
			long authorPersonId = Long.parseLong(parts[9]);
			Long countryId = (long) -1;
			if (!parts[10].equals(""))
				countryId = Long.parseLong(parts[10]);
			Long replyToPostId = Long.parseLong(parts[11]);
			Long replyToCommentId = Long.parseLong(parts[12]);
			List<String> tagIds1 = Arrays.asList(parts[13].split(";"));
			if (tagIds1.size() == 1 && tagIds1.get(0).equals(""))
				tagIds1 = new ArrayList<>();
			List<Long> tagIds = new ArrayList<Long>();
			for (String s : tagIds1)
				tagIds.add(Long.valueOf(s));
			List<String> mentionedIds = Arrays.asList(parts[14].split(";"));
			if (mentionedIds.size() == 1 && mentionedIds.get(0).equals(""))
				mentionedIds = new ArrayList<>();
			String privacy = parts[15];
			String link = parts[16];
			String gif = parts[17];
			
			String commentUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/comm" + String.format("%020d", commentId) + ">";
            String authorUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", authorPersonId) + ">";
            String postUri = null;
            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
            df1.setTimeZone(TimeZone.getTimeZone("GMT"));
            
			List<String> triplets = new ArrayList<String>();
			triplets.add(commentUri + " a <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/Comment> .");
            triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/locationIP> \"" + locationIp + "\" .");
            triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/creationDate> \"" + df1.format(creationDate) + "\"^^xsd:dateTime .");
            triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/browserUsed> \"" + browserUsed + "\" .");
            if (!content.equals("")) {
                triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/content> \"" + new String(content.getBytes("UTF-8"), "ISO-8859-1") + "\" .");
                triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/length> \"" + length + "\" .");
            }
            triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasCreator> " + authorUri + " .");
            if (replyToPostId == -1)
                    postUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/comm" + String.format("%020d", replyToCommentId) + ">";
            else
                    postUri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/post" + String.format("%020d", replyToPostId) + ">";
            triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/replyOf> " + postUri + " .");
            triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/id> \"" + commentId + "\"^^xsd:long .");
            if (countryId >= 0)
                triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/isLocatedIn> <" + placeUri(countryId) + "> .");
            for (int k = 0; k < tagIds.size(); k++)
                triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasTag> <" + tagUri(tagIds.get(k)) + "> .");
            for (int l = 0; l < mentionedIds.size(); l++)
                triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/mentions> <http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", Long.parseLong(mentionedIds.get(l))) + "> .");
            if (privacy != null  && !privacy.equals(""))
                triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/visible> \"" + privacy + "\"^^xsd:boolean .");
            if (!link.equals(""))
                triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/links> \"" + link + "\" .");
            if (!gif.equals(""))
                triplets.add(commentUri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/gifFile> \"" + gif + "\" .");
            return String.join("\n", triplets);
		}
		// Add friendship
		else if (updateType == 8) {
			long person1Id = Long.parseLong(parts[3]);
			long person2Id = Long.parseLong(parts[4]);
			Date creationDate = new Date(Long.parseLong(parts[5]));
			String person1Uri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", person1Id) + ">";
            String person2Uri = "<http://www.ldbc.eu/ldbc_socialnet/1.0/data/pers" + String.format("%020d", person2Id) + ">";
            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
            df1.setTimeZone(TimeZone.getTimeZone("GMT"));
            String triplets [] = new String[4];
            triplets[0] = person1Uri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/knows> [ <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasPerson> " + person2Uri + "; <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/creationDate> \"" + df1.format(creationDate) + "\"^^xsd:dateTime ] .";
            triplets[1] = person2Uri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/knows> [ <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/hasPerson> " + person1Uri + "; <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/creationDate> \"" + df1.format(creationDate) + "\"^^xsd:dateTime ] .";
            triplets[2] = person1Uri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/knows> " + person2Uri + " .";
            triplets[3] = person2Uri + " <http://www.ldbc.eu/ldbc_socialnet/1.0/vocabulary/knows> " + person1Uri + " .";
            return String.join("\n", triplets);
		}
		return "";
	}
	
    private String prepareQueryText(int queryType, String text) {
    	String [] arguments = text.split("[|]");
    	
    	String queryString;
    	if (queryType <= 14)
    		queryString = "#Q" + String.valueOf(queryType) + "\n" + preparePrefixes();
    	else
    		queryString = "#S" + String.valueOf(queryType-14) + "\n" + preparePrefixes();
    	try {
    		String name;
    		if (queryType <= 14)
    			name = "query" + String.valueOf(queryType) + ".txt";
    		else
    			name = "s" + String.valueOf(queryType-14) + ".txt";
			queryString += new String(Files.readAllBytes(Paths.get("snb_queries/" + name )));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    	
    	switch (queryType) {
		case 1:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%firstName%", arguments[1]);
			queryString = queryString.replaceAll("%limit%", "20");
			break;
		case 2:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%maxDate%", sdf.format(new Date(Long.parseLong(arguments[1]))));
			queryString = queryString.replaceAll("%limit%", "20");
			break;
		case 3:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%startDate%", sdf.format(new Date(Long.parseLong(arguments[1]))));
			queryString = queryString.replaceAll("%durationDays%", arguments[2]);
			queryString = queryString.replaceAll("%countryXName%", arguments[3]);
			queryString = queryString.replaceAll("%countryYName%", arguments[4]);
			queryString = queryString.replaceAll("%limit%", "20");
			break;
		case 4:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%startDate%", sdf.format(new Date(Long.parseLong(arguments[1]))));
			queryString = queryString.replaceAll("%durationDays%", arguments[2]);
			queryString = queryString.replaceAll("%limit%", "10");
			break;
		case 5:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%minDate%", sdf.format(new Date(Long.parseLong(arguments[1]))));
			queryString = queryString.replaceAll("%limit%", "20");
			break;
		case 6:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%tagName%", "'" + arguments[1] + "'");
			queryString = queryString.replaceAll("%limit%", "10");
			break;
		case 7:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%limit%", "20");
			break;
		case 8:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%limit%", "20");
			break;
		case 9:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%maxDate%", sdf.format(new Date(Long.parseLong(arguments[1]))));
			queryString = queryString.replaceAll("%limit%", "20");
			break;
		case 10:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%month1%", arguments[1]);
			int nextMonth = Integer.parseInt(arguments[1]) + 1;
			if (nextMonth == 13)
				nextMonth = 1;
			queryString = queryString.replaceAll("%month2%", String.valueOf(nextMonth));
			queryString = queryString.replaceAll("%limit%", "10");
			break;
		case 11:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%countryName%", "'" + arguments[1] + "'");
			queryString = queryString.replaceAll("%workFromYear%", arguments[2]);
			queryString = queryString.replaceAll("%limit%", "10");
			break;
		case 12:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%tagClassName%", arguments[1]);
			queryString = queryString.replaceAll("%limit%", "20");
			break;
		case 13:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%maxDate%", sdf.format(new Date(Long.parseLong(arguments[1]))));
			queryString = queryString.replaceAll("%limit%", "20");
			break;
		case 14:
			queryString = queryString.replaceAll("%personId%", String.format("%020d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%maxDate%", sdf.format(new Date(Long.parseLong(arguments[1]))));
			queryString = queryString.replaceAll("%limit%", "20");
			break;
    	case 15:
    	case 16:
    	case 17:
    		queryString = queryString.replaceAll("%personId%", String.format("%d", Long.parseLong(arguments[0])));
			queryString = queryString.replaceAll("%limit%", "10");
    	case 18:
    	case 19:
    	case 20:
    	case 21:
    		queryString = queryString.replaceAll("%messageId%", String.format("%d", Long.parseLong(arguments[0])));
		}
		return queryString;
    }


	private String companyUri(long long1) {
		return companyMap.get(long1);
	}

	private String universityUri(long long1) {
		return universityMap.get(long1);
	}

	private String tagUri(long long1) {
		return tagMap.get(long1);
	}

	private String placeUri(long long1) {
		return placeMap.get(long1);
	}

}
