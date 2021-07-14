package org.hobbit.gspb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.gspb.util.SNBConstants;
import org.hobbit.gspb.util.VirtuosoSystemAdapterConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNBDataGenerator extends AbstractDataGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(SNBDataGenerator.class);
	private Semaphore generateTasks = new Semaphore(0);
	private int scaleFactor;
	private int numberOfOperations;
	
    public SNBDataGenerator() {
    	
    }
    
    @Override
    public void init() throws Exception {
    	LOGGER.info("Initialization begins.");
        // Always init the super class first!
        super.init();

		// Your initialization code comes here...
        internalInit();
        LOGGER.info("Initialization is over.");
    }
    
	private void internalInit() {
    	Map<String, String> env = System.getenv();
    	
    	// Number of operations
    	if (!env.containsKey(SNBConstants.GENERATOR_SCALE_FACTOR)) {
            LOGGER.error("Couldn't get \"" + SNBConstants.GENERATOR_SCALE_FACTOR + "\" from the properties. Aborting.");
            System.exit(1);
        }
    	scaleFactor = Integer.parseInt(env.get(SNBConstants.GENERATOR_SCALE_FACTOR));
    	    	
    	// Number of operations
    	if (!env.containsKey(SNBConstants.GENERATOR_NUMBER_OF_OPERATIONS)) {
            LOGGER.error("Couldn't get \"" + SNBConstants.GENERATOR_NUMBER_OF_OPERATIONS + "\" from the properties. Aborting.");
            System.exit(1);
        }
    	numberOfOperations = Integer.parseInt(env.get(SNBConstants.GENERATOR_NUMBER_OF_OPERATIONS));
	}

	@Override
	protected void generateData() throws Exception {
		LOGGER.info("Data Generator is running...");
    	
		downloadFileAndSendData();
		
		generateTasks.acquire();
	}
	
    @Override
	public void close() throws IOException {
		// Free the resources you requested here
    	
		
        // Always close the super class after yours!
        super.close();
    }
    
    private void downloadFileAndSendData() {
    	String directory = "https://gitlab.rlp.net/timohomburg/eurosdrbenchmark/-/raw/main/";
    	String datasetFiles = directory + "dataset.ttl";
    	try { 
			InputStream is = new URL(datasetFiles).openStream();
			String [] files = IOUtils.toString(is).split("\n");
			is.close();
			
    		for (int i = 0; i < files.length; i++) {
    			String remoteFile = files[i];
    			remoteFile = directory + remoteFile;
    			LOGGER.info("Downloading file " + remoteFile);           
    			InputStream inputStream = new URL(remoteFile).openStream();

        		String graphUri = remoteFile.replaceFirst(".*/", "");
        		byte[] data = IOUtils.toByteArray(inputStream);;
        		byte[] dataForSending = RabbitMQUtils.writeByteArrays(null, new byte[][]{RabbitMQUtils.writeString(graphUri)}, data);
        		sendDataToSystemAdapter(dataForSending);
    			inputStream.close();
    			    			
    			LOGGER.info("File " + remoteFile + " has been downloaded successfully and sent.");
    		}
    		
			ByteBuffer buffer = ByteBuffer.allocate(5);
			buffer.putInt(files.length);
			buffer.put((byte) 1);
    		sendToCmdQueue(VirtuosoSystemAdapterConstants.BULK_LOAD_DATA_GEN_FINISHED_FROM_DATAGEN, buffer.array());
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
	}
    
    @Override
    public void receiveCommand(byte command, byte[] data) {
        /*if (command == VirtuosoSystemAdapterConstants.BULK_LOADING_DATA_FINISHED) {
        	String directory = "https://hobbitdata.informatik.uni-leipzig.de/MOCHA_OC/T2/sf" + scaleFactor + "/";
        	String updateFile1 = directory + "updateStream_0_0_person.csv";
        	String updateFile2 = directory + "updateStream_0_0_forum.csv";
        	LOGGER.info("Downloading updates");
        	try {            
        		InputStream inputStream1 = new URL(updateFile1).openStream();
        		InputStream inputStream2 = new URL(updateFile2).openStream();
        		byte[] bytesArray = null;
        		String fileContent1 = IOUtils.toString(inputStream1);
        		String fileContent2 = IOUtils.toString(inputStream2);
        		String [] lines1 = fileContent1.split("\n");
        		String [] lines2 = fileContent2.split("\n");
        		int i = 0, j = 0;
        		while (i < lines1.length && j < lines2.length && i+j < numberOfOperations * 10) {
        			if (lines1[i].compareTo(lines2[j]) < 0)
        				bytesArray = RabbitMQUtils.writeString(lines1[i++]);
        			else
        				bytesArray = RabbitMQUtils.writeString(lines2[j++]);
        			sendDataToTaskGenerator(bytesArray);
        		}
        		while (i < lines1.length && i+j < numberOfOperations * 10) {
        			bytesArray = RabbitMQUtils.writeString(lines1[i++]);
        			sendDataToTaskGenerator(bytesArray);
        		}
        		while (j < lines2.length && i+j < numberOfOperations * 10) {
        			bytesArray = RabbitMQUtils.writeString(lines2[j++]);
        			sendDataToTaskGenerator(bytesArray);
        		}
        		LOGGER.info("Files with updates have been downloaded successfully and sent.");
        		inputStream1.close();
        		inputStream2.close();
        	} catch (IOException ex) {
        		System.out.println("Error: " + ex.getMessage());
        		ex.printStackTrace();
        	}
        	generateTasks.release();
        }*/
        super.receiveCommand(command, data);
    }
}