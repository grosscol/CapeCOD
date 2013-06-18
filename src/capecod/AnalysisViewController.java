/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package capecod;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import org.apache.log4j.Logger;

/**
 * FXML Controller class
 *
 * @author grossco
 */
public class AnalysisViewController implements Initializable, SwapPanelController {

    //Logger
    private Logger log;
    
    @FXML
    TextArea infoText;
    
    @FXML
    TextArea anaTextArea;
    
    String initialText;
    
    SimpleBooleanProperty isModified = new SimpleBooleanProperty(false);
    SimpleBooleanProperty isVisible = new SimpleBooleanProperty(false);
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //Setup logger for this class
        log = Logger.getLogger(this.getClass());
        
        if(rb.containsKey("analysisIntro")){
            initialText = rb.getString("analysisIntro");
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

    @Override
    public void execute() {
        StringBuilder sb = new StringBuilder("Analysis:\n");
        
        sb.append(DataModel.getInstance().summarizeModel())
                .append("\n");
        sb.append("\nConcentrations:\n");
        
        sb.append(wellsToConcString());
        
        anaTextArea.setText(sb.toString());
    }

    @Override
    public void cancel() {
        //To change body of generated methods, choose Tools | Templates.
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    String wellsToConcString(){
        //Calculate the number of characters required to display the data
        int numChars = DataModel.getInstance().getWellData().size() * 6;
        //add space for additional rows between plates.
        int addRowChars = (1+(DataModel.getInstance().getWellData().size()/96)) * 72;
        numChars = numChars + addRowChars;
        //Allocate array.
        char[] disp = new char[numChars+1];
        
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
        //fill last character with '\n'
        disp[numChars]='\n';
        
        int wIdx; //well index
        int cIdx; //char index

        for(WellData w : DataModel.getInstance().getWellData()){
            //get the well index for this well's position
            wIdx = WellData.getIndexOfWell(w.getPlate(), w.getRow(), w.getCol());
            //multiply by 6 chars/well to get the starting index in the char[]
            //add an additiona 72 characters (one row) per plate for spacing
            cIdx = wIdx * 6 + w.getPlate()*72 ;
            
            //Get the optical density reading as a formatted character array
            //Copy that array into the correct position in the display array
            try{
                //Handle null concentration
                if(w.getConcentration() == null){
                    System.arraycopy("null.".toCharArray(),0, disp, cIdx, 5);
                }
                else if(w.getConcentration().compareTo( BigDecimal.valueOf(99999L)) > 0 ){
                    //Value with more than five digits. bracket with 9's 9----9
                    disp[cIdx]='9';
                    disp[cIdx+4]='9';
                }else{
                    System.arraycopy(
                        String.format("%1$-5d", w.getConcentration().toBigInteger())
                            .toCharArray(),
                        0, disp, cIdx, 5);
                }
            }catch(Exception myEx){
                log.error(myEx);    
                
            } 
        }
        
        
        return(new String(disp));
    }
}
