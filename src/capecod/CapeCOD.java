/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package capecod;


import basefxpreloader.AppNotification;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;


/**
 *
 * @author grossco
 */
public class CapeCOD extends Application {
    
    /*Class variables */
    //Name of environment variable key that contains the cypher value.
    private final static String osEnvKey = "TILLabInvAppsCode";
    //Name of file that contains the required configuration information.
    private final static String configFile= "connectionInfo.txt";
    //setup in initialize function
    //class logger for the application
    private Logger log;
    /* Boolean value indicating if an alternate launch should be done.
     * Use this in the case of an error during initialization.*/
    private boolean doAltLaunch_Error = false;
    
    
    
    
    /* Do initialization of database and logger resources.
     * Copy most of what the base authentication and connection example does
     * into this function.
     */
    @Override
    public void init(){
        /*LOGGER SETUP*/
        //Set the property that tells jboss logger which backend to use
        System.setProperty("org.jboss.logging.provider", "log4j");
        // create logger.
        log = Logger.getLogger(CapeCOD.class);
        log.debug("Logger created.  Initialization started.");
        
        //notify the Preloader of the current progress
        notifyPreloader(new AppNotification("Init Started.",0.05) );

        //notify the Preloader of the ending progress
        notifyPreloader(new AppNotification("Init Done.",0.99) );
    }
       
    
    @Override
    public void start(Stage stage) throws Exception {
        
        //Get Resource Bundle to pass to the main controller (and down from there)
        ResourceBundle rb = 
                ResourceBundle.getBundle("capecod.resources.staticstrings");
        FXMLLoader pLoader =
                new FXMLLoader(getClass().getResource("CodPanel.fxml"),rb);
        
        //Have the FXMLLoader set up the Scene graph
        Parent root = (Parent) pLoader.load();
        
        //Need to set the controller after the FXML loader has initialized everything.
        ((CodController) pLoader.getController()).setAccelerators();

        Scene scene = new Scene(root);
         
        stage.setTitle("C.O.D.");
        //stage.getIcons();
        stage.setScene(scene);
        stage.show();
    }

    /* Override the stop function to release resources obtained in init().
     * Specifically, close the EntityManagerFactory.
     */
    @Override
    public void stop(){

    }
    
       
    /* Get the keyphrase from the user or server's environment variables.
     * This prevents us from having to store the keyphrase in plain text in 
     * the code or in an easily accesible file.
    */
    private String getJasptKeyphrase(){
        //Get environment variables as a String key, String value Map.
        Map<String,String> envVars = System.getenv();
        String keyphrase;
        
        /* Get the TIL Lab inventory applications code from the OS Environment.
         * Environment variable name should be "TILLabInvAppsCode".  If it's 
         * not set as an environment variable for the calling user, then the
         * password based encryption isn't going to work.  The digest algorithm
         * was Jasypt's PBEWITHMD5ANDDES.
         */
        try{
            //Check that the System Environment contains the required Key.
            if( envVars.containsKey(osEnvKey) ){
                log.debug(osEnvKey+" = "+envVars.get(osEnvKey));
                keyphrase= envVars.get(osEnvKey);
            }else{
                log.debug(osEnvKey+" not found in environment variables.");
                keyphrase=null;
            }
        }catch(  ClassCastException | NullPointerException myMex){
            log.error(myMex);
            keyphrase=null;
        }
        
        return keyphrase;
    }
    
    /* Parse database connection information out of the config file.
     * The config file should be connectionInfo.properties: user, password, url.
     * It's plain text that uses # as comment marks and key=value syntax.  No 
     * duplicate keys permitted.
     */
    public Map<String,String> dbConnectInfoParse(){
        HashMap<String,String> params = null;
        //If the directory is not specified in the string, will check working dir
        File cfile = new File(configFile);

        //Check if the file exists
        if(cfile.exists()){
            try{
                params = new HashMap<>();
                //load data from the text file in the fashion of properties file
                Properties prop = new Properties();
                prop.load(new FileInputStream(cfile));

                //if the specific keys exist, add them to params hashmap
                if(prop.getProperty("password") != null){
                    params.put("hibernate.connection.password", prop.getProperty("password"));
                }
                if(prop.getProperty("user") != null){
                    params.put("hibernate.connection.username", prop.getProperty("user"));
                }
                if(prop.getProperty("url") != null){
                    params.put("hibernate.connection.url", prop.getProperty("url"));
                }
            }
            catch(Exception myEx){
                log.error(myEx.getMessage());
            }
        }
        else{
            log.error("File not found: "+cfile.toString());
        }
        
        return params;
    }
    

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("MAIN CALLED!");
        launch(args);
    }
}