
/*  Data model singleton used to stored the data used in the input, analysis,
 * and reporting of a COD run.  A single data model instance means that the 
 * application will only handle a single analysis at a time.
 * 
 */
package capecod;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.log4j.Logger;



/**
 *
 * 
 */
public class DataModel {
    
    //Make this a singleton for the project.
    private static final DataModel dm = new DataModel();
    
    //Logger
    private Logger log;
    
    //Data
    private final ArrayList<WellData> wells = new ArrayList<>();

    
    //State of Data Model
    private final SimpleBooleanProperty dataLoaded = new SimpleBooleanProperty(false);
    
    //LogModel for calculation of concentrations from OD
    private LogModel logMod;
    
      
    //Constructor
    private DataModel(){
        log = Logger.getLogger(this.getClass());
        wells.ensureCapacity(96);
        
    }
    
    //instance getter for data model.
    public static DataModel getInstance(){
            return dm;
    }
    
    //Clear out data model for reuse.
    public void clear(){
        wells.clear();
        logMod = null;
        dataLoaded.set(false);
    }
    
    /** Return a boolean property that is true if data has been loaded.
     * Toggle data loaded to false during load of data and back to true after
     * reading infile is complete.  Set to false after data cleared/canceled.
     */
    public ReadOnlyBooleanProperty getDataLoadedProperty(){
        return dataLoaded;
    }
    
    //Return read-only copy of the wells data
    public List<WellData> getWellData(){
        return Collections.unmodifiableList(wells);
    }
    
   
    //Read file and append it to the existing data.
    public Boolean openAndRead(Path in){
        
        //if input is null, return immediately.
        if(in == null){ return Boolean.FALSE; }
        
        if(wells.size() % 96 != 0){
            //if the number of wells after each read is not a multiple of 96
            //there is a problem.
            log.warn("Wells read & processed not a multiple of 96.");
        }
        
        //Calculate what plate number is assuming 96 wells per plate.
        int plateNum = (wells.size() / 96) + 1 ;
        wells.ensureCapacity(plateNum * 96);
        
        //Assume ascii which maps directly onto UTF-8, so use UTF-8
        //Read data line by line
        Charset charset = Charset.forName("UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(in, charset)) {
            String line;
            int lineNum = 0; //count normally for the read lines.
            int rowNum = 1; //start counting rows at 1.
            while ((line = reader.readLine()) != null) {
                //Process line. using String.split()
                String sArr[] = line.split("\\t");
                
                //Check that the read is in the correct line of the input file
                //Check that the line splits to the correct number of tokens
                if(lineNum > 5 && lineNum < 14 && sArr.length == 13){
                    
                    //loop through the tokens and create a well for each.
                    //Begin counting column numbers at 1.
                    for(int colNum=1; colNum < 13; colNum++){
                        //the BigDecimal parsing could throw an error.
                        BigDecimal od;
                        try{
                            od = new BigDecimal(sArr[colNum]);
                        }catch(NumberFormatException nEx){
                            //Negative indicates error case. do -34717 "EAT IT"
                            od = BigDecimal.valueOf(-34717.0);
                            log.error("Parsing input to Big Decimal failure.");
                        }
                        //Create a new WellData and add it to the wells collection
                        //default the use to EXPERIMENT
                        WellData w = new WellData(plateNum, rowNum, colNum, od);
                        w.setPurpose(WellData.WellUse.EXPERIMENT);
                        wells.add( w );
                        log.debug(w.toString());
                    }   
                    //increment plate data row number
                    rowNum = rowNum +1;
                }
                //increment number of lines read
                lineNum = lineNum + 1;
                
            }
        } catch (IOException x) {
            log.error("IOException." + x.getMessage() );
            return Boolean.FALSE;
        } finally{
            //Check if there is data
            if(wells.size() > 0){
                dataLoaded.set(true);
            }
        }
        
        //Read and process data succeeded.
        return Boolean.TRUE;
    }
    
    //Do default annotation of the welldata
    public void applyAnnotationTemplate(){
        if(wells.size() < 96){return;}
        
        //Set all wells to Experimental
        for(WellData wd : wells){
            wd.setPurpose(WellData.WellUse.EXPERIMENT);
        }
        
        int[] backgroundIndecies = {0,12,49,61};
        int[] standardsIndecies  = {   1,  13,  24,  25,  36,  37,   48,   60,   72,   84};
        double[] standardsConcen = {64.0,64.0,1000,25.6,1000,25.6,400.0,400.0,160.0,160.0};
        
        //Set purpose and concentration for Background/Blank wells.
        for(int i : backgroundIndecies){
            wells.get(i).setPurpose(WellData.WellUse.BLANK);
            wells.get(i).setConcentration(BigDecimal.ZERO);
            wells.get(i)
                    .getAnnotations().put("rowAnno", "media");
        }
        
        String standardsRowAnno = "Diluent media";
        String standardsColAnno = "Standard reagent";
        
        //Set purpose and concentration Standards wells.
        for(int i=0; i< standardsIndecies.length; i++){
            wells.get(standardsIndecies[i])
                    .setPurpose(WellData.WellUse.STANDARD);
            wells.get(standardsIndecies[i])
                    .setConcentration(BigDecimal.valueOf(standardsConcen[i]));
            wells.get(standardsIndecies[i])
                    .getAnnotations().put("colAnno", standardsColAnno);
            wells.get(standardsIndecies[i])
                    .getAnnotations().put("rowAnno", standardsColAnno);
        }
        
        //Set annotation for NEAT wells.
        String neatAnno = "Neat standard reagent";
        wells.get(73).getAnnotations().put("colAnno", neatAnno);
        wells.get(85).getAnnotations().put("colAnno", neatAnno);
        
    }
    

    
    /** Get the index of a WellData object (or where it should be in the set).
     * Remember that the well plate,row, & col values are 1 based.
     */
    public static Integer indexOfWellData(WellData wd){
        if(wd == null){return null;}
        //Convert indecies to 0-based.  Then multiply by offset and sum.
        return( (wd.getPlate()-1)*96 + (wd.getRow()-1)*12 + (wd.getCol()-1) );
    }
    
    //Assume first plate format is correct & inputs are all kosher.
    //Basically re-implements Don's procedure.
    public Boolean processStandardPlate(){
        
        ArrayList<WellData> backgroundWells = new ArrayList<>();
        ArrayList<WellData> standardsWells = new ArrayList<>();
    
        //Check that there is sufficient well data
        if(wells.size() < 96){
            log.error("Insufficient data to process standard plate");
            return Boolean.FALSE;
        }
        
        //Set all wells to experiment use by default
        for (Iterator<WellData> it = wells.iterator(); it.hasNext();) {
            WellData w = it.next();
            w.setPurpose(WellData.WellUse.EXPERIMENT);
        }
        
        //get background readings
        //(row 1, col 1) (row 2, col 1) (row 5, col 2) (row 6, col 2)
        backgroundWells.clear();
        backgroundWells.add(wells.get(WellData.getIndexOfWell(1,1,1)));
        backgroundWells.add(wells.get(WellData.getIndexOfWell(1,2,1)));
        backgroundWells.add(wells.get(WellData.getIndexOfWell(1,5,2)));
        backgroundWells.add(wells.get(WellData.getIndexOfWell(1,6,2)));
        
        //Set background wells concentration to 0
        for (Iterator<WellData> it = backgroundWells.iterator(); it.hasNext();) {
            WellData w = it.next();
            w.setConcentration(BigDecimal.ZERO);
            w.setPurpose(WellData.WellUse.BLANK);
        }
        
        //get Standards OD readings. //plate, row, col
        standardsWells.clear();
        standardsWells.add(wells.get(WellData.getIndexOfWell(1,3,1) ));
        standardsWells.add(wells.get(WellData.getIndexOfWell(1,4,1) ));
        standardsWells.add(wells.get(WellData.getIndexOfWell(1,5,1) ));
        standardsWells.add(wells.get(WellData.getIndexOfWell(1,6,1) ));
        standardsWells.add(wells.get(WellData.getIndexOfWell(1,7,1) ));
        standardsWells.add(wells.get(WellData.getIndexOfWell(1,8,1) ));
        standardsWells.add(wells.get(WellData.getIndexOfWell(1,1,2) ));
        standardsWells.add(wells.get(WellData.getIndexOfWell(1,2,2) ));
        standardsWells.add(wells.get(WellData.getIndexOfWell(1,3,2) ));
        standardsWells.add(wells.get(WellData.getIndexOfWell(1,4,2) ));
        
        //set default dilutions
        standardsWells.get(0).setConcentration(BigDecimal.valueOf(1000));
        standardsWells.get(1).setConcentration(BigDecimal.valueOf(1000));
        standardsWells.get(2).setConcentration(BigDecimal.valueOf(400));
        standardsWells.get(3).setConcentration(BigDecimal.valueOf(400));
        standardsWells.get(4).setConcentration(BigDecimal.valueOf(160));
        standardsWells.get(5).setConcentration(BigDecimal.valueOf(160));
        standardsWells.get(6).setConcentration(BigDecimal.valueOf(64));
        standardsWells.get(7).setConcentration(BigDecimal.valueOf(64));
        standardsWells.get(8).setConcentration(BigDecimal.valueOf(25.6));
        standardsWells.get(9).setConcentration(BigDecimal.valueOf(25.6));
        
        //Set standard wells use attribute
        for (Iterator<WellData> it = standardsWells.iterator(); it.hasNext();) {
            WellData w = it.next();
            w.setPurpose(WellData.WellUse.STANDARD);
        }
        
        buildLogModel();
        
        return Boolean.TRUE;
    }
    

    
    //Build the model
    public void buildLogModel(){
        
        List<WellData> bkgdWells = new ArrayList<>();
        List<WellData> stdWells = new ArrayList<>();
        //Get Background and Standards Wells.
        for(WellData wd : wells){
            if(wd.getPurpose()==WellData.WellUse.BLANK){
                bkgdWells.add(wd);
            }else if(wd.getPurpose()==WellData.WellUse.STANDARD){
                stdWells.add(wd);
            }
        }
        
        if(bkgdWells.size() < 1){
            log.warn("No background wells specified.");
        }
        
        if(stdWells.size() < 2){
            log.error("Insufficient standards to make a line.");
            return;
        }
        
        //Add the backgroundWells and Standard wells together
        ArrayList<WellData> al = new ArrayList<>(bkgdWells);
        al.addAll(stdWells);
        
        //set the dataModels current log model
        logMod = new LogModel( al );
        
    }
    
    //Calculate average OD of list of wells.
    private BigDecimal aveOpticalDensity( List<WellData> lw){
        if(lw == null){return null;}
        if(lw.isEmpty()){return null;}
        
        BigDecimal sum = BigDecimal.ZERO;
        
        for( WellData w : lw){
            sum = sum.add( w.getOd() );
        }
        
        //return the sum divided by the number of elements.
        return ( sum.divide( BigDecimal.valueOf( lw.size() ), 4) );
    }
    
    /** Fill in all the concentrations of wells that currently have null.
     *  Is a no-op if the model has not been built yet.
     */
    public void calcAllConcentrations(){
        //Do nothing if the model has not been built.
        if(logMod == null){ return; }
        
        for(WellData w : wells){
            if(w.getPurpose() == WellData.WellUse.EXPERIMENT ||
                    w.getPurpose() == WellData.WellUse.NONE){
                w.setConcentration( logMod.calcConcentration(w) );
            }
        }
    }

    /** Summarize the log model.
     * @return String describing the attributes of the model
     */
    public String summarizeModel(){
        if(logMod == null){return "Model is null";}
        
        StringBuilder sb = new StringBuilder("Log-Log Model:\n");
        
        sb.append("Background Ave. O.D: ")
                .append(logMod.backgroundAve)
                .append("\n");
        sb.append("Slope: ")
                .append(logMod.slope)
                .append("\n");
        sb.append("Intercept: ")
                .append(logMod.intercept)
                .append("\n");
        sb.append("R-Squared: ")
                .append(logMod.rSquared)
                .append("\n");
        
        return sb.toString();
    }
 
    //////////// Update List of WellData Methods ///////////////////////////////
    
    //Update the annotations of the list of well data
    public void setWellsAnnotation(List<WellData> lwd, String key, String val){
        if(lwd == null || key == null){return;}
        int idx;
        for(WellData wd : lwd){
            idx = DataModel.indexOfWellData(wd);
            if(idx > wells.size()){
                log.error("Out of bounds WellData index.");
            }else{
                if(val == null || val.equals("")){
                    wells.get(idx).getAnnotations().remove(key);
                }else{
                    wells.get(idx).getAnnotations().put(key, val);
                }
                
            }
        }
    }
    
    //Update the purpose of the list of well data
    public void setWellsUse(List<WellData> lwd, WellData.WellUse use){
        if(lwd == null || use == null){return;}
        int idx;
        for(WellData wd : lwd){
            idx = DataModel.indexOfWellData(wd);
            if(idx > wells.size()){
                log.error("Out of bounds WellData index.");
            }else{
                wells.get( DataModel.indexOfWellData(wd) )
                        .setPurpose(use);
            }
        }
    }
    
    //Update the dilution of the list of well data
    public void setWellsDilution(List<WellData> lwd, BigDecimal dil){
        if(lwd == null || dil == null){return;}
        int idx;
        for(WellData wd : lwd){
            idx = DataModel.indexOfWellData(wd);
            if(idx > wells.size()){
                log.error("Out of bounds WellData index.");
            }else{
                wells.get( DataModel.indexOfWellData(wd) )
                        .setDilutionFactor(dil);
            }
        }
    }
    
    //Update the dilution of the list of well data
    public void setWellsConcentration(List<WellData> lwd, BigDecimal conc){
        if(lwd == null){return;}
        int idx;
        for(WellData wd : lwd){
            idx = DataModel.indexOfWellData(wd);
            if(idx > wells.size()){
                log.error("Out of bounds WellData index.");
            }else{
                wells.get( DataModel.indexOfWellData(wd) )
                        .setConcentration(conc);
            }
        }
    }
 
    
    //Update the mrn of the list of well data
    public void setWellsMrn(List<WellData> lwd, String mrn){
        if(lwd == null){return;}
        int idx;
        for(WellData wd : lwd){
            idx = DataModel.indexOfWellData(wd);
            if(idx > wells.size()){
                log.error("Out of bounds WellData index.");
            }else{
                wells.get( DataModel.indexOfWellData(wd) )
                    .setMrn(mrn);
            }
        }
    }
    
    //////////// Private Inner Classes ////////////////////////////////////////
    
    /** Class to hold the model constructed from the standards and background 
     * wells data.
     * 
     * As per the original GWBASIC implementation, the X values are the 
     * concentration and the Y values are the O.D.
     */
    private class LogModel {
        
        //Scale for model objects BigDecimal calculations
        static final int lmSCALE = 4;
        //Model variable declarations.
        BigDecimal intercept;
        BigDecimal slope;
        BigDecimal rSquared;
        BigDecimal backgroundAve;
        
        //Constructor takes a list of well data and calculates model params.
        LogModel(List<WellData> lw){

            //Set background & strip out null and conc = zero wells.  
            getBkgdFromInput(lw);
            
            //Strip out any non-standards wells
            removeNonStandards(lw);
            
            //Initialize variables to hold totals.
            BigDecimal sumLogX = BigDecimal.ZERO;
            BigDecimal sumLogY = BigDecimal.ZERO;
            BigDecimal sumLogXsq = BigDecimal.ZERO;
            BigDecimal sumLogYsq = BigDecimal.ZERO;
            BigDecimal sumLogXY = BigDecimal.ZERO;
            //Intermediate variables to make calculations easier to read.
            //Observed OD - Background OD
            BigDecimal adjOD;
            //Natural log of (ObservedOD - background OD)
            BigDecimal lnAdjOD;
            //Natural log of concentration
            BigDecimal lnConc;
            //Number of samples
            BigDecimal numSamples = new BigDecimal(lw.size()).setScale(lmSCALE);
            
            //Go through remaining wells.
            log.debug("Processing "+lw.size()+" wells of standards.");
            
            for(WellData w : lw){
                //get adjusted OD: Observed OD - background OD.
                adjOD = w.getOd().subtract(backgroundAve);
                //get log of adjusted OD.
                lnAdjOD = BigFunctions.ln(adjOD, lmSCALE);
                //get ln of concentration
                lnConc = BigFunctions.ln(w.getConcentration(), lmSCALE);
                  
                //Running total of ln( concentration )
                sumLogX = sumLogX.add( lnConc );
                
                //Running total of ln( adjusted OD )
                sumLogY = sumLogY.add( lnAdjOD );
                
                //Running total of square of ln( concentration )
                sumLogXsq = sumLogXsq.add( lnConc.multiply(lnConc) );

                //Running total of square of ln( adjusted OD )
                sumLogYsq = sumLogYsq.add( lnAdjOD.multiply(lnAdjOD) );
                
                //Running total of ln(concentration) * ln(adjusted OD)
                sumLogXY = sumLogXY.add(  lnAdjOD.multiply(lnConc) );    
            }
            
            //Calculate slope. 
            // SLLL = (NS*SUMLXY - SUMLX*SUMLY)/(NS*SUMLX2 - SUMLX*SUMLX)
            BigDecimal slopeNumerator = 
                    numSamples.multiply(sumLogXY)
                    .subtract(
                        sumLogX.multiply(sumLogY)
                    );
            BigDecimal slopeDenominator = 
                    numSamples.multiply(sumLogXsq)
                    .subtract(
                        sumLogX.multiply(sumLogX)
                    );
            slope = slopeNumerator.divide(slopeDenominator, lmSCALE);
            
            //calculate intercept. (SUMLY - SLLL*SUMLX)/NS 
            intercept = 
                    sumLogY.subtract(
                        slope.multiply(sumLogX)
                    )
                    .divide(numSamples, lmSCALE);
            
            /*Calculate R-squared (rSquared)
             *  numerator / ( denominatorPartOne * denomintaorPartTwo )
             * 
             *        ((SUMLXY-SUMLX*SUMLY/NS)^2) / 
             * ((SUMLX2-SUMLX*SUMLX/NS)*(SUMLY2-SUMLY*SUMLY/NS))
            */
            BigDecimal r2Numerator =
                    sumLogXY.subtract( 
                        sumLogX.multiply(sumLogY).divide(numSamples, lmSCALE)
                    );
            //square the numerator
            r2Numerator = BigFunctions.intPower(r2Numerator, 2L, lmSCALE);
            //calculate the first part of the R-squared denominator
            BigDecimal r2DenomPartOne =
                    sumLogXsq.subtract( 
                        sumLogX.multiply(sumLogX).divide(numSamples, lmSCALE)
                    );
            //calculate the second part of the R-squared denominator
            BigDecimal r2DenomPartTwo =
                    sumLogYsq.subtract(
                        sumLogY.multiply(sumLogY).divide(numSamples, lmSCALE)
                    );
            //calculate the R-squared value using the calculated parts.
            rSquared = 
                    r2Numerator.divide(
                       r2DenomPartOne.multiply(r2DenomPartTwo), lmSCALE
                    );          
        }
     
        //Remove the non-standards well data from the input
        private void removeNonStandards(List<WellData> lw){
            Iterator<WellData> itt = lw.iterator();
            while(itt.hasNext() ){
                WellData w = itt.next();
                if(w.getPurpose() != WellData.WellUse.STANDARD){
                    itt.remove();
                }
            }
        }
        
        //using this model, return the concentration for the given OD
        // ugly.
        public BigDecimal calcConcentration(BigDecimal inputOD){
            //check that the background isn't greater  or equal to than the obs
            if(inputOD.compareTo(backgroundAve) < 1  ){
                return BigDecimal.ZERO;
            }
            
            //for the given OD, what is the concentration
            // CONC = E^((LOG(MEAN-BKG)-INTER)/SLOPE)
            BigDecimal outputConcentration = 
                BigFunctions.exp(
                        BigFunctions.ln(
                            inputOD.subtract(backgroundAve), lmSCALE)
                        .subtract(intercept)
                        .divide(slope,lmSCALE)
                    ,lmSCALE);

            //If the concentration calculated is less than zero, return zero.
            if(outputConcentration.compareTo(BigDecimal.ZERO) == -1 ){
                return BigDecimal.ZERO;
            }else{
                return outputConcentration;
            }
            
        }
        
        public BigDecimal calcConcentration(WellData wd){
            if(wd == null){return null;}
            BigDecimal output = calcConcentration(wd.getOd());
            output = output.multiply(wd.getDilutionFactor());
            return(output);
            
        }
        /* Get background wells & remove them from the list.
        * Drop null concentration wells.
        */
        private void getBkgdFromInput(List<WellData> lw){

            //track the number of background wells.
            int i = 0;
            //track the running total
            BigDecimal bkgdSum = BigDecimal.ZERO;
            
            Iterator itt = lw.iterator();
            //go through list of wells.
            WellData w;
            while(itt.hasNext()){
                w = (WellData) itt.next();
                if(w.getPurpose() == WellData.WellUse.BLANK){
                    //Null conc is either an experimental well or an error.
                    if(w.getConcentration() == null){
                        log.error("An input to construct model has null concentration.");
                        itt.remove();
                    }else if(w.getConcentration() == BigDecimal.ZERO){
                        //Zero conc is a background control
                        i = i + 1;
                        bkgdSum = bkgdSum.add(w.getOd());
                        //Remove the samples from the input list.
                        itt.remove();
                    } 
                }
            }
            
            //Get average of background sum to set the backgroundAVE
            if(i > 0){
                backgroundAve = bkgdSum.divide(new BigDecimal(i), lmSCALE);
            }else{
                backgroundAve = BigDecimal.ZERO.setScale(lmSCALE);
            }
        }
        
        @Override
        public String toString(){
            return (String.format("Slope: %1\tInter: %2\tR-sq:%3\tBkgd:%4",
                    slope,intercept,rSquared,backgroundAve)
                    );           
        }
        
    }
    
    
}
