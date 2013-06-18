/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package capecod.plateTable;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

/**
 * FXML Controller class
 *
 * @author grossco
 */
public class DataEntryDialogController implements Initializable {

    //default completely false return state. Might just want to track confirmed.
    private Boolean wasConfirmed = Boolean.FALSE;
    private Boolean wasCancelled = Boolean.FALSE;
    
    @FXML
    TextField inputField;
    
    @FXML
    Label dialogTitle;
    
    @FXML
    AnchorPane rootPane;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        //setListener for on show. Do reset on showing
        rootPane.visibleProperty().addListener( new VisibilityChangeListener() );
        
    }    
    
        @FXML
    private void handleConfirmClick(ActionEvent aEvt){
        wasCancelled = Boolean.FALSE;
        wasConfirmed = Boolean.TRUE;
        rootPane.getScene().getWindow().hide();
    }
    
    @FXML
    private void handleCancelClick(ActionEvent aEvt){
        wasCancelled = Boolean.TRUE;
        wasConfirmed = Boolean.FALSE;
        rootPane.getScene().getWindow().hide();
    }
    
    @FXML
    private void handleClearClick(ActionEvent aEvt){
        inputField.setText(null);
    }
    
    public void setTitleText(String s){
        dialogTitle.setText(s);
    }
    
    public Boolean getWasConfirmed(){
        return wasConfirmed;
    }
    
    public Boolean getWasCancelled(){
        return wasCancelled;
    }
    
    public String getInputText(){
        return inputField.getText();
    }
    
    
    
    /** Use the visibility property to determine when to reset the boolean values
     *  of the message box since this box will be re-used.
     */
    private class VisibilityChangeListener implements ChangeListener<Boolean>{

        @Override
        public void changed(ObservableValue<? extends Boolean> ov, 
            Boolean oldVal, Boolean newVal) 
        {
            if(oldVal == false & newVal == true){
                //Scene is now showing: reset values
                wasConfirmed = Boolean.FALSE;
                wasCancelled = Boolean.FALSE;
            }
        }
        
    }
}
