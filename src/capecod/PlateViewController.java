/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package capecod;


import capecod.plateTable.PlateViewContextMenu;
import capecod.plateTable.PlateTable;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;

/**
 * FXML Controller class
 *
 * @author grossco
 */
public class PlateViewController implements Initializable, SwapPanelController {

    //Logger
    Logger log = Logger.getLogger(this.getClass());
    
    //Lower text box to show user text information. Possibly error reports
    @FXML
    TextArea infoText;
    
    //Main text area for display OD values of plates or default setup text
    @FXML
    TextArea dispTextArea;
    
    //Main area for putting the graphical control representation of the plate annotations.
    @FXML
    VBox plateGraphicsBox;
    
    //Context Menu to pass to plate control constructor. Will be set on the table columns.
    @FXML
    static ContextMenu plateAnnoCM;
    

    
    //Variable to hold the initial text as defined in the fxml
    String initialText;
    
    //Track the number of PlateTables that have been loaded.
    Integer numPlateTables = 0;
    
    //Properties for the main controller to set listeners on.
    SimpleBooleanProperty isModified = new SimpleBooleanProperty(false);
    SimpleBooleanProperty isVisible = new SimpleBooleanProperty(false);
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //get a copy of the initial text displayed.
        if(rb.containsKey("plateIntro")){
            initialText = rb.getString("plateIntro");
        }else{
            initialText = "Initial text not saved.";
        }
    }    

    @Override
    public void setInfoTextArea(TextArea ta) {
        infoText = ta;
    }

    @Override
    public BooleanProperty getObservableIsModified() {
        return isModified;
    }

    @Override
    public BooleanProperty getObservableIsVisible() {
        return isVisible;
    }

    //Execute is a call to do the main work of the panel.
    @Override
    public void execute() {
        populateDisplays();
    }
    
    @Override
    public void cancel() {
        //Reset plate text to the inital text.
        dispTextArea.setText(initialText);
        //Remove children from graphical representation of plates
        plateGraphicsBox.getChildren().clear();
    }
    
    private void populateDisplays(){
        //Don't do anything if the data model has no data (or data is loading)
        if( DataModel.getInstance().getDataLoadedProperty().getValue() == false ){
            return;
        }
        
        //set the text area text to the plates text
        dispTextArea.setText( wellsToOdString() );
        
        //Fill in graphics plates and add them to the VBox children.
        plateGraphicsBox.getChildren()
                .addAll(wellsToPlateControls());
    }
    
    private String wellsToOdString(){
        //Calculate the number of characters required to display the data
        int numChars = DataModel.getInstance().getWellData().size() * 6;
        //add space for additional rows between plates.
        int addRowChars = (1+(DataModel.getInstance().getWellData().size()/96)) * 72;
        numChars = numChars + addRowChars;
        //Allocate the character array
        char[] disp = new char[numChars];
        
        //Fill all the positions with '-'
        Arrays.fill(disp, '-');
        //Fill the every 6th position with ' '
        for(int i=5; i<numChars;i=i+6){
            disp[i] = ' ';
        }
        //fill in every 72nd character with '\n'
        for(int i=71; i<numChars; i=i+72){
            disp[i] = '\n';
        }
        
        int wIdx; //well index
        int cIdx; //char index

        for(WellData w : DataModel.getInstance().getWellData()){
            //get the well index for this well's position
            wIdx = WellData.getIndexOfWell(w.getPlate(), w.getRow(), w.getCol());
            
            //multiply by 6 chars/well to get the starting index in the char[]
            //add additional factor of plate*72 to make an additiona row
            cIdx = wIdx * 6 + w.getPlate()*72 ;
            
            //Get the optical density reading as a formatted character array
            //Copy that array into the correct position in the display array
            System.arraycopy(
                    String.format("%1$-4.3f", w.getOd()).toCharArray(),
                    0, disp, cIdx, 5);
        }
        
        return(new String(disp));
    }
    
    private List<Node> wellsToPlateControls() {
        if( DataModel.getInstance().getWellData().size() % 96 != 0){
            //Wells are not a multiple of 96.
            log.error("Wells are not a multiple of 96. Some will not be displayed.");
        }
        
        //Check for the first well in the data set that has not been added.
        //It should be number of loaded PlateTales * 96
        int startWell = numPlateTables * 96; //0,96,182...
        
        //numPlateTables is the number of plates that have already been loaded.

        //Determine the total number of plates there will be.
        int numPlates = (DataModel.getInstance().getWellData().size() / 96);
        
        //list of plate tables that will be generated/loaded.
        List<PlateTable> lpt = new LinkedList<>();
        //list of nodes that will be added to the display container in the gui.
        List<Node> nl = new LinkedList<>();
        
        //Make a list of plates using data ( assuming data is sorted )
        for(Integer i = numPlateTables; i < numPlates; i++){
            //Create a plate table using 96 consecutive WellData objects
            PlateTable pt = PlateTable.plateTableFactory(
                    DataModel.getInstance().getWellData().subList(i*96, i*96+96)
                    );

            //Add appropriate listeners (if any) to plate table here
            
            //Add plate table to the list of plate tables.
            lpt.add( pt );
        }
                
        //Intersperse descriptive nodes around well plates
        for(int i=0; i<lpt.size();i++){
            nl.add(new Label("Plate: "+ String.valueOf(numPlateTables+1+i)));
            nl.add(lpt.get(i));
        }
             
        //increment the number of PlateTables that have been loaded.
        numPlateTables = numPlateTables + lpt.size();
        
        //return a list of nodes that will be added to the display
        return nl;
    }
    
    


}
