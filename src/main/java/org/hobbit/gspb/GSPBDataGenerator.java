package org.hobbit.gspb;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.gspb.util.GSPBConstants;
import org.hobbit.gspb.util.VirtuosoSystemAdapterConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GSPBDataGenerator extends AbstractDataGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(GSPBDataGenerator.class);
	private Semaphore generateTasks = new Semaphore(0);
	private int numberOfOperations;
	
    public GSPBDataGenerator() {
    	
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
    	if (!env.containsKey(GSPBConstants.GENERATOR_NUMBER_OF_OPERATIONS)) {
            LOGGER.error("Couldn't get \"" + GSPBConstants.GENERATOR_NUMBER_OF_OPERATIONS + "\" from the properties. Aborting.");
            System.exit(1);
        }
    	numberOfOperations = Integer.parseInt(env.get(GSPBConstants.GENERATOR_NUMBER_OF_OPERATIONS));
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
        if (command == VirtuosoSystemAdapterConstants.BULK_LOADING_DATA_FINISHED) {
            LOGGER.info("Getting queries");
            try {            
                for (int i=0; i < GSPBConstants.GSPB_QUERIES.length; i++) {
                    InputStream inputStream = new FileInputStream("gsb_queries/" + GSPBConstants.GSPB_QUERIES[i]);
                    String fileContent = IOUtils.toString(inputStream);
                    fileContent = "#Q-" + (i+1) + "\n" + fileContent; // add a comment line at the beginning of the query, to denote the query number (#Q-1, #Q-2, ...)
                    byte[] bytesArray = null;
                    bytesArray = RabbitMQUtils.writeString(fileContent);
                    sendDataToTaskGenerator(bytesArray);
                    inputStream.close();
                }
                LOGGER.info("Files with queries have been loaded and successfully sent.");
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
            generateTasks.release();
        }
        super.receiveCommand(command, data);
    }
}