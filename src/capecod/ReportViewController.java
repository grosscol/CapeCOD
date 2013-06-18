/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package capecod;

import java.net.URL;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

/**
 * FXML Controller class
 *
 * @author grossco
 */
public class ReportViewController implements Initializable, SwapPanelController {

    SimpleBooleanProperty isModified = new SimpleBooleanProperty(false);
    SimpleBooleanProperty isVisible = new SimpleBooleanProperty(false);
    
    String initHeadText = "The report data will be filled in after an input "
            + "file has been opened and the analysis has been executed.";
    
    @FXML
    TextArea infoText;
    
    @FXML
    TextArea modelInfo;
    
    @FXML
    TextArea reportHeader;
    
    @FXML
    TextArea wellOutput;
    
    
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
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
        
        if(DataModel.getInstance().getWellData().size() < 96 ){return;}
        
        //Assume data has been read by now.
        
        //Report Header
        Date d = new Date();
        StringBuilder head = new StringBuilder("[Type of Experiment] run by: Tom Shelton\n");
        head.append("Data aquired by: Tom Shelton on [Plate Reader Date]\n");
        head.append("Annotation setup by: Colin Gross on ");
        head.append(d.toString());
        reportHeader.setText(head.toString());
        
        //Model Summary
        StringBuilder model = new StringBuilder(DataModel.getInstance().summarizeModel());
        model.append("\n[List of Residuals]");
        modelInfo.setText(model.toString());
        
        //Row by row well data
        StringBuilder rbr = new StringBuilder();
        Formatter formatter = new Formatter(rbr, Locale.US);
        //row anno, col anno, od, dil fact, conc
        String fmt = "%1$-20.20s %2$-20.20s %3$5.2f  %4$-5.2f  %5$-4.2E\n";
        rbr.append("Row Anno            |  Col Anno           | OD | DilFactor | Concen\n");
        for(WellData wd : DataModel.getInstance().getWellData() ){
            formatter.format(fmt, wd.getAnnotations().get("rowAnno"),
                    wd.getAnnotations().get("colAnno"),
                    wd.getOd(),
                    wd.getDilutionFactor(),
                    wd.getConcentration()
                    );
        }
        wellOutput.setText(rbr.toString());
        
    }

    @Override
    public void cancel() {
        //no-op
        reportHeader.setText(initHeadText);
    }
}
