/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package capecod.plateTable;

import capecod.DataModel;
import capecod.WellData;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TablePosition;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;

/** subclass of context menu to build on the fly and associate with the
 *  PlateTables that are generated on the fly.  This is a workaround for 
 * the inability to get the parent node that was set from the .show() call
 * that was made from the PlateTable action handler
 */
public class PlateViewContextMenu extends ContextMenu {
    
        Logger log = Logger.getLogger(PlateViewContextMenu.class);
        
        private DataEntryDialogController dedc;
        private final Stage dataEntryStage;
        
        private final PlateTable pt;
        private Menu useMenu = new Menu("Set Use Category");
        
        private MenuItem setUseStandard = new MenuItem("STANDARD");
        private MenuItem setUseBackground = new MenuItem("BACKGROUND");
        private MenuItem setUseExperiment = new MenuItem("EXPERIMENT");
        private MenuItem setUseNone = new MenuItem("NONE");
        
        private MenuItem setRow = new MenuItem("Set Row Annotation");
        private MenuItem setCol = new MenuItem("Set Col Annotation");
        private MenuItem setDil = new MenuItem("Set Dilution");
        private MenuItem setConc = new MenuItem("Set Concentration");
        private MenuItem setMrn = new MenuItem("Set MRN");
        
        public PlateViewContextMenu(PlateTable p){
            this.pt = p;
            this.setAutoHide(true);
            
            //Set menu items
            this.getItems().addAll(useMenu, setRow, setCol, setDil, setMrn, setConc);
            
            //Set sub-menu items
            useMenu.getItems().addAll(
                    setUseStandard,setUseBackground,setUseExperiment,setUseNone);
            
            //Set actions
            setRow.setOnAction( new ActSetAnnotation("rowAnno") );
            setCol.setOnAction( new ActSetAnnotation("colAnno") );
            setDil.setOnAction( new ActSetDilution() );
            setMrn.setOnAction( new ActSetMrn() );
            setConc.setOnAction(new ActSetConcentration() );
            
            setUseStandard.setOnAction( new ActSetUse(WellData.WellUse.STANDARD) );
            setUseBackground.setOnAction( new ActSetUse(WellData.WellUse.BLANK) );
            setUseExperiment.setOnAction( new ActSetUse(WellData.WellUse.EXPERIMENT) );
            setUseNone.setOnAction( new ActSetUse(WellData.WellUse.NONE) );
            
            //Setup the stage for confirm/cancel
            dataEntryStage = new Stage(StageStyle.TRANSPARENT);
            dataEntryStage.initOwner(pt.getScene().getWindow());
            dataEntryStage.initModality(Modality.WINDOW_MODAL);
            dataEntryStage.setScene(null);
            dataEntryStage.setOnShown( new EventHandler<WindowEvent>(){
                @Override
                public void handle(WindowEvent t) {
                    Window w = dataEntryStage.getOwner();
                    dataEntryStage.setX(
                        w.getX() + w.getWidth()/2 - dataEntryStage.getWidth()/2
                        );
                    dataEntryStage.setY(
                        w.getY() + w.getHeight()/2 - dataEntryStage.getHeight()/2
                        ); 
                }   
            } );
            
            
            //Setup the dataEntry scene and controller
            try{
                //Load the FXML node heirarchy for the dataEntryScene
                FXMLLoader confirmLoader = 
                        new FXMLLoader(this.getClass().getResource("DataEntryDialog.fxml"));
                Scene s = new Scene( (Parent) confirmLoader.load() );
                dataEntryStage.setScene(s);
                dedc = confirmLoader.getController();
            }catch(Exception myEx){
                log.error(myEx);
            }
        }
        
        private List<WellData> getSelectedCells(){
            List<WellData> lwd = new LinkedList<>();
            ObservableList<TablePosition> olp =
                pt.getSelectionModel().getSelectedCells();
            for( TablePosition tp : olp){
                lwd.add( pt.getItems().get(tp.getRow())
                            .getColumnProperty(tp.getColumn()).getValue() );
            }
            
            return lwd;
        }
        
        //Setup action handler to set the use of the selected wells.
        private class ActSetUse implements EventHandler<ActionEvent>{
            
            private final WellData.WellUse use;
            
            public ActSetUse(WellData.WellUse u){
                this.use = u;
            }
                        
            @Override
            public void handle(ActionEvent t) {
                //Update the data model
                DataModel.getInstance().setWellsUse(getSelectedCells(), use);
                //Update the plate table view
                pt.manualUpdateSelectedRows();
            }
        }
        
        /** Action handler to get a string from the user and set the given
         * annotation for the selected cells.
         */
        private class ActSetAnnotation implements EventHandler<ActionEvent>{
            
            private final String key;
            
            public ActSetAnnotation(String k){
                this.key = k;
            }
            
            @Override
            public void handle(ActionEvent t) {
                //Get Annotation string from user
                dataEntryStage.setTitle("Enter value for "+key);
                dataEntryStage.showAndWait();
                if(dedc.getWasConfirmed()){
                    
                    String str = dedc.getInputText();

                    //Update the data model annotations with the string.
                    //Blank or null will remove the annotation
                    DataModel.getInstance()
                        .setWellsAnnotation(getSelectedCells(), key, str);
                    
                    //Update the plate table view
                    pt.manualUpdateSelectedRows();
                    
                }
            } 
        }
        
        private class ActSetDilution implements EventHandler<ActionEvent>{

            @Override
            public void handle(ActionEvent t) {
                //Get Dilution from the user
                //Get Annotation string from user
                dataEntryStage.setTitle("Enter Dilution Factor");
                dataEntryStage.showAndWait();
                if(dedc.getWasConfirmed()){
                    
                    String str = dedc.getInputText();
                    BigDecimal bd;
                    
                    //Try to parse the user input into a useable number
                    try{
                        bd = new BigDecimal(str);
                    }catch(NumberFormatException nfe){
                        bd = BigDecimal.ONE;
                    }
                    
                    //Update the data model annotiations with the new dilution
                    DataModel.getInstance()
                        .setWellsDilution(getSelectedCells(), bd);
                    
                    //Update the plate table view
                    pt.manualUpdateSelectedRows();   
                }
                
            } 
        }

        private class ActSetMrn implements EventHandler<ActionEvent>{
            @Override
            public void handle(ActionEvent t) {
                //Get Annotation string from user
                String mrnStr = "CC3474124";
                //Update the data model annotations with the string.
                DataModel.getInstance()
                        .setWellsMrn(getSelectedCells(), mrnStr);
                //Update the plate table view
                pt.manualUpdateSelectedRows();
            } 
        }
        
        private class ActSetConcentration implements EventHandler<ActionEvent>{

            @Override
            public void handle(ActionEvent t) {
                //Get Dilution from the user
                BigDecimal dummy = BigDecimal.TEN;
                //Update the annotiations with the new dilution
                DataModel.getInstance()
                        .setWellsConcentration(getSelectedCells(), dummy);
                //Update the plate table view
                pt.manualUpdateSelectedRows();
            } 
        }
}
