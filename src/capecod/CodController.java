/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package capecod;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import org.apache.log4j.Logger;

/**
 * FXML Controller class
 *
 * @author grossco
 */
public class CodController implements Initializable {
    
    //Logger
    Logger log;
    

    /* Class variables for UI nodes for runtime generation and use.
     */
    AnchorPane plateViewPane;
    AnchorPane analysisViewPane;
    AnchorPane reportViewPane;
    
    
    @FXML
    StackPane swapStack;

    // Listener to check when the DataModel hasData property has changed
    private final MainDataModelListener hasDataListener = new MainDataModelListener();
    private final WeakChangeListener<Boolean> weakHasDataListener =
            new WeakChangeListener<>(hasDataListener);
    
    /* Listener to handle when the plate panel indicates the data 
     * has been editted. Register the weak listener to avoid memory leaks from 
     * being too lazy to deal with unregistering the listners.
     */
    private final MainDataModelListener plateDataModListener = new MainDataModelListener();
    private final WeakChangeListener<Boolean> weakPlateDataModListener =
            new WeakChangeListener<>(plateDataModListener);
    
    private final MainDataModelListener analysisModListener = new MainDataModelListener();
    private final WeakChangeListener<Boolean> weakAnaDataModListener =
            new WeakChangeListener<>(analysisModListener);
    
    
    //Variables to store the sub panel controllers
    SwapPanelController plateSwapPanelControl = null;
    SwapPanelController anaSwapPanelControl = null;
    SwapPanelController reportSwapPanelControl = null;
            
    /* Class variables that are also defined/used in corrosponding FXML.
     */
    
    //Pane to be swapped for different application uses: Ingress, Request, Mods.
    
    @FXML
    TextArea ta_info;

    @FXML
    Button btn_SwitchToPlate;
    
    @FXML
    Button btn_SwitchToAnalysis;
    
    @FXML
    Button btn_SwitchToReport;
    
    @FXML 
    Button btn_execute;
    
    @FXML
    Button btn_cancel;
    
    @FXML
    ToolBar tbar_acts;
    
    @FXML
    TextArea ta_welcome;
    
    @FXML
    MenuItem fmen_OpenFiles;
    
    @FXML
    MenuItem fmen_CloseFiles;
    
    @FXML
    private void handleMenu_FileOpen(ActionEvent event){
        //Did data model previously have data before we are opening more?
        Boolean firstData = ! DataModel.getInstance().getDataLoadedProperty().getValue();
        
        FileChooser fc = new FileChooser();
        fc.setTitle("Open File");
        
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("Tab Delimeted Text","*.txt"),
                new ExtensionFilter("Tab Delimeted Text","*.tab")
                );
        List<File> listInFiles = fc.showOpenMultipleDialog(null);
        
        //If the dialog returned with files, take action
        if(listInFiles != null){
            //Open each file and read the data into the model
            for(File f : listInFiles){
                DataModel.getInstance().openAndRead(f.toPath());
            }
            
            //Do default annotation if no data was previously read in.
            if(firstData){
                DataModel.getInstance().applyAnnotationTemplate();
            }
            
            //Update the plate panel
            plateSwapPanelControl.execute();
            //Switch to plate view
            handleBtn_SwithToPlate(new ActionEvent());
        }
        
        
    }
    
    @FXML
    private void handleMenu_FileClose(ActionEvent event){
        //This functionality has been mapped to the cancel button action
    }
    
    @FXML
    private void handleBtn_SwithToPlate(ActionEvent event){
        log.debug("Handle switch to plate event");
        //swapPaneChange(vialRequestsPane);
        swapPaneChangeTopStacked(plateViewPane);
    }
    
    @FXML
    private void handleBtn_SwitchToAnalysis(ActionEvent event){
        log.debug("Handle switch to analysis event");
        //swapPaneChange(ingressManagementPane);
        swapPaneChangeTopStacked(analysisViewPane);
    }
    
    @FXML
    private void handleBtn_SwitchToReport(ActionEvent event){
        log.debug("Handle switch to report event");
        swapPaneChangeTopStacked(reportViewPane);
    }
    
    @FXML
    private void handleBtn_Execute(ActionEvent event){
        log.debug("Handle execute.");
        
        //Execute the plate annotation panel method.  It's a traitor
        
        
        //Create a log model from standards and background
        DataModel.getInstance().buildLogModel();
        
        //Calculate all the concentrations
        DataModel.getInstance().calcAllConcentrations();
        
        //Call the analysis panel execute method to show the analysis details.  
        if(anaSwapPanelControl != null){
            anaSwapPanelControl.execute();
        }
        
        //Switch to the anaylsis view.
        handleBtn_SwitchToAnalysis(new ActionEvent());
        
        
        //Call the report panel execute method to generate/display a report.
        if(reportSwapPanelControl != null){
            reportSwapPanelControl.execute();
        }
            
    }
    
    @FXML
    private void handleBtn_Cancel(ActionEvent event){
        log.debug("Data and Analysis cancelled.");
        
        //Clear the data model
        DataModel.getInstance().clear();
        
        //Reset the controllers by calling their cancel methods
        if(plateSwapPanelControl != null){
            plateSwapPanelControl.cancel();
        }
        
        if(anaSwapPanelControl != null){
            anaSwapPanelControl.cancel();
        }
        
        if(reportSwapPanelControl != null){
            reportSwapPanelControl.cancel();
        }
        
    }
    
    

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
               
        //setup logger
        log = Logger.getLogger(CapeCOD.class);
        
        //Load the panels that will be swapped out to handle the different task
        //views of the program.
        loadManagedPanels();
        
        //setup a listener on the data model's has data property
        DataModel.getInstance().getDataLoadedProperty().addListener(weakHasDataListener);
        

    }   
    
    
    /** Set the accelerators after the FXML Loader has done it's work.
     * This can't be done from within initialize() as the components are not 
     * extant at that point.
     */
    public void setAccelerators(){
        fmen_OpenFiles.setAccelerator(
                new KeyCodeCombination(KeyCode.O,KeyCombination.CONTROL_DOWN));
        fmen_CloseFiles.setAccelerator(
                new KeyCodeCombination(KeyCode.C,KeyCombination.CONTROL_DOWN));
    }
    
    
    /* Load the panels that will be swapped to accomodate the different types
     * of requests that are going to be managed by this applicition: 
     * Ingress, VialRequests, Modifications
      */
    private void loadManagedPanels(){

        
        //Resource bundle of static strings for the controllers
        ResourceBundle resBun =
                ResourceBundle.getBundle("capecod.resources.staticstrings");
        
        //Load Plate View Panel
        try{
            log.debug("Loading the Request Vials Management pane.");

            //Load the FXML node heirarchy for the plate view
            FXMLLoader plateLoader = 
                    new FXMLLoader(this.getClass().getResource("PlateView.fxml"));
            plateLoader.setResources(resBun);
            plateViewPane = (AnchorPane) plateLoader.load();
            
            //Get the controller & store reference in local variable.
            plateSwapPanelControl = (SwapPanelController) plateLoader.getController();
            //Inject the infoTextArea information into the child controller.
            plateSwapPanelControl.setInfoTextArea(ta_info);
            //Set up a weak listener for data changed events
            plateSwapPanelControl.getObservableIsModified().addListener(weakPlateDataModListener);
            
        }catch (Exception Ex){
            log.error("Error loading Request Vials Management pane.");
            log.error(Ex);
            plateViewPane = null;
        }
        
        //Load the Analysis Panel
        try{
            log.debug("Loading the Analysis View pane.");
            
            //Load the FXML node heirarchy for the RequestsTreeView
            FXMLLoader analysisLoader = 
                    new FXMLLoader(this.getClass().getResource("AnalysisView.fxml"));
            analysisLoader.setResources(resBun);
            analysisViewPane = (AnchorPane) analysisLoader.load();
            
            //Get the controller & store reference in local variable.
            anaSwapPanelControl =  (SwapPanelController) analysisLoader.getController();
            //Inject the infoTextArea information into the child controller.
            anaSwapPanelControl.setInfoTextArea(ta_info);

            //Set up a weak listener for data changed events
            anaSwapPanelControl
                    .getObservableIsModified().addListener(weakAnaDataModListener);
            
            
        }catch (Exception Ex){
            log.error("Error loading Analysis View pane.");
            log.error(Ex);
            analysisViewPane = null;
        }
        
        
        //Load the Report Panel
        try{
            log.debug("Loading the Analysis View pane.");
            
            //Load the FXML node heirarchy for the RequestsTreeView
            FXMLLoader reportLoader = 
                    new FXMLLoader(this.getClass().getResource("ReportView.fxml"));
            reportLoader.setResources(resBun);
            reportViewPane = (AnchorPane) reportLoader.load();
            
            //Get the controller & store reference in local variable.
            reportSwapPanelControl = (SwapPanelController) reportLoader.getController();
            //Inject the infoTextArea information into the child controller.
            reportSwapPanelControl.setInfoTextArea(ta_info);

            //Set up a weak listener for data changed events
            //((SwapPanelController) reportLoader.getController())
            //        .getObservableIsModified().addListener(weakIngDataModListener);

            
        }catch (Exception Ex){
            log.error("Error loading Report View pane.");
            log.error(Ex);
            reportViewPane = null;
        }

    }

    /*Given a node, add it to the stacked pane just below the top node, and 
     * call the left swipe off stack on the top node.  The result will be the 
     * replacement node showing.
    */
    private void swapPaneChangeTopStacked(Node replacement){
        //check that the passed node is not null
        if(replacement == null){
            //inform user
            ta_info.setText("The node you want to display failed to initialize "
                    + "at startup.  Either the FXML file is missing from the "
                    + "jar, or the components of the pane could not be "
                    + "initialized.");
            log.debug("Replacement node is null.");
            //return with no changes being made to stack pane
            return;
        }
        
        //check that the stack doesn't already contain the replacement
        if(swapStack.getChildren().contains(replacement) == true){
            log.debug("The replacement node to be switched to is already  in the stacked pane.");
            return;
        }
        
        if(swapStack.getChildren().size() > 0){
            //Add the replacement just behind the existing top node
            int topnum = swapStack.getChildren().size() - 1;
            //This will shift the current top of the stack up on index.
            swapStack.getChildren().add(topnum, replacement);
            //Now do an animation that will end with the removal of the top node.
            swapPaneLeftSwipeOffStack( swapStack.getChildren().get(topnum+1) );
        }else{
            //just add the new node
            swapStack.getChildren().add(replacement);
        }
    }

    //Do animation of swipe to the left and remove the node from it's parent panel.
    private void swapPaneLeftSwipeOffStack(final Node swipee){
        //Get the bounds of the panel to be swiped off (swipee)
        double x = swipee.getBoundsInParent().getMinX();
        double y = swipee.getBoundsInParent().getMinY();
        double h = swipee.getBoundsInParent().getHeight();
        double w = swipee.getBoundsInParent().getWidth();
        //get the initial TranslateX property it can be reset after animation
        final double initialX = swipee.getTranslateX();
        
        //Use the bounds to get a rectangle
        Rectangle clipRect = new Rectangle(x,y,w,h);
        
        //Set this rectancle as the clipping node for the swipee.
        swipee.setClip(clipRect);
        
        /* Setup Keyvalue for value transions for a swipe to the left.
         * The clipping rectangle will be have the width shrunk (from right to
         * left).  The node itself will be moved to the left the same amount.
         */
        KeyValue kvLeftClip = new KeyValue(clipRect.widthProperty(), 0.0);
        KeyValue kvLeftPane = new KeyValue(swipee.translateXProperty(), -w);
        //Setup keyframe in which to do the value transition
        KeyFrame kfLeftSwipe = new KeyFrame(Duration.millis(250), kvLeftClip, kvLeftPane);
        
        //Setup timeline to do the animation
        Timeline tLeftSwipe = new Timeline();
        tLeftSwipe.setCycleCount(1);
        tLeftSwipe.setAutoReverse(true);
        
        tLeftSwipe.getKeyFrames().add(kfLeftSwipe);
        
        //Use the callback to complete the action of removing the child panels
        // and adding a new one.
        tLeftSwipe.setOnFinished(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent t) {
                //Remove the swipee from it's parent.
                Pane parent = (Pane) swipee.getParent();
                parent.getChildren().removeAll(swipee);
                
                //Reset the clipping and translateXProperty.
                swipee.setClip(null);
                swipee.setTranslateX(initialX);
            }
        } );
        
        //play the animation.
        tLeftSwipe.play();
        
        //The animation will still be playing by the time this function returns
        log.debug("Left swipe off function done.");
        
    }
    
    
    //Listener to set on the data model has data property
    private class DataModelHasDataChangeListener implements ChangeListener<Boolean>
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, 
            Boolean oldVal, Boolean newVal) 
        {
            log.debug("Data model hasData change listener called.");
            if(oldVal.booleanValue() == false && newVal.booleanValue() == true){
                //Turn buttons on by turning disable to false
                btn_execute.disableProperty().set(false);
                btn_cancel.disableProperty().set(false);
            }
            if(oldVal.booleanValue() == true && newVal.booleanValue() == false){
                //Turn buttons off
                btn_execute.disableProperty().set(true);
                btn_cancel.disableProperty().set(true);
            }
        }
    }
    
    
    /** Listener to the is data loaded property of the data model and
     * enables/disables the buttons based on the state changes.
     */
    private class MainDataModelListener implements ChangeListener<Boolean>
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, 
            Boolean oldVal, Boolean newVal) 
        {
            log.debug("Swap panel data change listener called.");
            if(oldVal.booleanValue() == false && newVal.booleanValue() == true){
                //Turn buttons on by turning disable to false
                btn_execute.disableProperty().set(false);
                btn_cancel.disableProperty().set(false);
            }
            if(oldVal.booleanValue() == true && newVal.booleanValue() == false){
                //Turn buttons off
                btn_execute.disableProperty().set(true);
                btn_cancel.disableProperty().set(true);
            }
        }
    }
    
    

}
