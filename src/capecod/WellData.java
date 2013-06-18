/** Class to represent the data of a single well of a 96-well plate in an 
 * optical density to concentration experiment.
 * 
 */

package capecod;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author grossco
 */
public class WellData implements Comparable {

    
    //Enumerator for the use of the cells.
    public enum WellUse{
        STANDARD, BLANK, EXPERIMENT, NONE
    }

    //Scale for BigDecimal representations.
    static final int wdSCALE = 4;

    private final Integer plate; //1-based
    private final Integer row; // 1-based
    private final Integer col; // 1-based
    private BigDecimal od;
    private BigDecimal concentration;
    private BigDecimal dilutionFactor;
    private String mrn;
    //Map of Annotation strings: rowAnno, colAnno, plateAnno
    private HashMap<String, String> annotations = new HashMap<>();
    private WellUse purpose = WellUse.NONE;
    

    //basic constructor
    public WellData(){
        plate = 1;
        row = 1;
        col = 1;

        od = null;
        concentration = null;
        dilutionFactor = BigDecimal.ONE.setScale(wdSCALE);
    }

    public WellData(Integer plate, Integer row, Integer col, BigDecimal od ){
        this.plate = plate;
        this.row = row;
        this.col = col;
        this.od = od.setScale(wdSCALE);
        this.dilutionFactor = BigDecimal.ONE.setScale(wdSCALE);
        this.concentration = null; 
    }

    public WellData(Integer plate, Integer row, Integer col, BigDecimal od, 
            BigDecimal dil, BigDecimal conc){
        this.plate = plate;
        this.row = row;
        this.col = col;
        this.od = od.setScale(wdSCALE);
        this.dilutionFactor = dil.setScale(wdSCALE);
        this.concentration = conc.setScale(wdSCALE);
    }

    /** Return the annotation strings.  The well plate control looks for
     * keys: rowAnno, colAnno, plateAnno.  However, these will be added
     */
    public Map<String,String> getAnnotations(){
        return annotations;
    }
    
    //Spit out a string representation
    @Override
    public String toString(){
        return String.format("P:%02d (%3$02d,%2$02d) OD: %4$6.4g Conc: %5$4E", 
                plate, row, col, od, concentration);
    }
        
    /**
     * Get index into array of well data assuming 96 well plate format.
     * 
     * @param plate 1-based plate number
     * @param row 1-based row number
     * @param col 1-based column number
     * @return index into 0-based array
     */
    public static int getIndexOfWell(int plate, int row, int col){
        int index = (plate-1)*96 + (row-1)*12 + (col-1);
        
        return index;
    }
    
    @Override
    public int compareTo(Object o) {
        if( (o instanceof WellData) == false){
            throw new ClassCastException(
                    "Object being compared is not an instance of WellData");
        }
        
        
        //compare plate numbers then row then column.
        if( plate > ((WellData) o).plate){ return 1;}
        else if( plate < ((WellData) o).plate){return -1;}
        else if( row > ((WellData) o).row){return 1;}
        else if( row < ((WellData) o).row){return -1;}
        else if( col > ((WellData) o).col){return 1;}
        else if( col < ((WellData) o).col){return -1;}
        else{
            return 0;
        }

    }
    
     /** Getter and Setter Methods. **/
    
    /**
     * @return the purpose annotation
     */
    public WellUse getPurpose() {
        return purpose;
    }

    /**
     * @param purpose the purpose annotation to set
     */
    public void setPurpose(WellUse purpose) {
        this.purpose = purpose;
    }
   
    /**
     * @return the plate
     */
    public Integer getPlate() {
        return plate;
    }

    /**
     * @return the row
     */
    public Integer getRow() {
        return row;
    }

    /**
     * @return the col
     */
    public Integer getCol() {
        return col;
    }

    /**
     * @return the od
     */
    public BigDecimal getOd() {
        return od;
    }

    /**
     * @param od the od to set
     */
    public void setOd(BigDecimal od) {
        this.od = od;
    }

    /**
     * @return the concentration
     */
    public BigDecimal getConcentration() {
        return concentration;
    }

    /**
     * @param concentration the concentration to set
     */
    public void setConcentration(BigDecimal concentration) {
        this.concentration = concentration;
    }

    /**
     * @return the dilutionFactor
     */
    public BigDecimal getDilutionFactor() {
        return dilutionFactor;
    }

    /**
     * @param dilutionFactor the dilutionFactor to set
     * Implicitly convert null to BigDecimal.ONE
     */
    public void setDilutionFactor(BigDecimal df) {
        if(df == null){
            this.dilutionFactor = BigDecimal.ONE;
        }else{
            this.dilutionFactor = df;
        }
        
    }

    /**
     * @param m the MRN/ID to set
     */
    public void setMrn(String m){
        mrn = m;
    }

    /**
     * @return the MRN/ID assigned to the well
     */
    public String getMrn(){
        return mrn;
    }

}
