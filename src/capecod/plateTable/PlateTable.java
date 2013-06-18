/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package capecod.plateTable;

import capecod.WellData;
import capecod.plateTable.PlateTable.RowData;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * Each table view has it's own data model internal.  Will have to sort out how
 * to make calls between this data model and the application data model.
 * 
 * @author grossco
 */
public class PlateTable extends TableView<RowData>{
    
    private static Label placeHold = new Label("No Data to Display");
    
    public PlateTable(){
        super();
    }
    
    /**
     *
     * @param lwd list of well data (96 wells)
     * @return a PlateTable control
     */
    public static PlateTable plateTableFactory( List<WellData> lwd ){
        //Get nothing from nothing
        if(lwd == null){return null;}
        
        //Require 96 wellData object exactly.
        if(lwd.size() != 96){ return null;}
               
        
        //Create a PlateTable
        PlateTable pT = new PlateTable();

        //Take the well data and create 8 RowData objects out of it.      
        //Set the items (RowData) that the table will display
        pT.setItems( wellsToRows(lwd) );
        
        //Set flag to make table non-editable
        pT.setEditable(false);
        
        //selection model is multiple and allow cells to be selected.
        pT.getSelectionModel().setCellSelectionEnabled(true);
        pT.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        //set placeholder node
        pT.setPlaceholder(placeHold);
        
        //set table menu button visibility
        pT.setTableMenuButtonVisible(false);
        
        //set resize policy
        pT.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        
        //create 12 columns
        for(int i=0; i<12;i++){
            //create TableColum
            TableColumn<RowData, WellData> tc = new TableColumn<>();
            //give it a cell factory
            tc.setCellFactory(new plateCellFactory());
            //give it a cell value factory.
            tc.setCellValueFactory( new plateCellValueFactory(i) );
            //Don't allow sorting
            tc.setSortable(false);
            //Don't permit editting
            tc.setEditable(false);
            //Don't allow user to resize the columns
            tc.setResizable(false);
            //set static width
            tc.setPrefWidth(PlateCell.CELL_WIDTH);
            //set button for column header graphic to select all column cells.
            tc.setGraphic( new ColumnHeadButton(tc) );
            //add the column to the table
            pT.getColumns().add(tc);
        }
        //set static table width
        pT.setWidth(2+PlateCell.CELL_WIDTH * 12 );
        pT.setMinWidth(2+PlateCell.CELL_WIDTH * 12);
        pT.setMaxWidth(2 + PlateCell.CELL_WIDTH * 13);
        //set a static table height.
        pT.setHeight(PlateCell.CELL_HEIGHT * 8);
        pT.setMaxHeight(PlateCell.CELL_HEIGHT * 8);
        
        return pT;
        
    }
   
    public void manualUpdateSelectedRows(){
        if(this.getItems() == null){return;}
        if(this.getItems().size() == 0){return;}
        if(this.getSelectionModel() == null){return;}
        
        for(RowData rd : this.getSelectionModel().getSelectedItems() ){
            rd.manualUpdateCols();
        }
    }
    
    
    /** Given a list of well data, convert into 8 RowData objects */
    private static ObservableList<RowData> wellsToRows(List<WellData> lwd){
        ArrayList<ArrayList<WellData>> arrArr = new ArrayList<>(8);
        ArrayList<RowData> rd = new ArrayList<>();
        
        //create 8 lists of list<welldata>
        for(int i=0; i<8; i++){
            arrArr.add(new ArrayList<WellData>(12));
        }
        
        //fill in data of all 8 lists
        for(WellData wd : lwd){
            arrArr.get(wd.getRow()-1).add(wd.getCol()-1, wd);
        }
        
        //Make list of RowData
        for(int i=0; i<8; i++){
            rd.add( new RowData(arrArr.get(i)) );
        }
        
        //Turn list into observable list
        return FXCollections.observableList(rd);
    }
    
    public static class plateCellFactory 
        implements Callback< TableColumn<RowData, WellData>, TableCell<RowData, WellData> > {
    
        @Override
        public TableCell<RowData, WellData> call(TableColumn<RowData, WellData> tC) {
            return new PlateCell();
        }

    }
    
    /** Custom callback class that includes a constructor that takes an integer.
     * This integer will be used in the subsequent call() function.
     */
    public static class plateCellValueFactory implements
        Callback<CellDataFeatures<RowData, WellData>, ObservableValue<WellData>>{

        final Integer colIndex;
        
        plateCellValueFactory(){
            colIndex = new Integer(0);
        }
        
        plateCellValueFactory(Integer c){
            colIndex = new Integer(c);
        }
        
        @Override
        public ObservableValue<WellData> call(CellDataFeatures<RowData, WellData> cdf) {
            return cdf.getValue().getColumnProperty(colIndex);
        }
        
    }
        
    public static class PlateCell extends TableCell<RowData,WellData>{
        
        public static Double CELL_HEIGHT = Double.valueOf(30.0);
        public static Double CELL_WIDTH = Double.valueOf(40.0);
        
        private final ImageView ivBkgd = new ImageView(
            new Image(PlateCell.class.getResourceAsStream("resources/well_bkgd_20x20.png")));
        private final ImageView ivBkgdRow = new ImageView(
            new Image(PlateCell.class.getResourceAsStream("resources/well_bkgd_rowAnno_20x20.png")));
        private final ImageView ivBkgdCol = new ImageView(
            new Image(PlateCell.class.getResourceAsStream("resources/well_bkgd_colAnno_20x20.png")));
        private final ImageView ivBkgdBoth = new ImageView(
            new Image(PlateCell.class.getResourceAsStream("resources/well_bkgd_bothAnno_20x20.png")));
        private final ImageView ivExp = new ImageView(
           new Image( PlateCell.class.getResourceAsStream("resources/well_exp_noAnno_20x20.png")));
        private final ImageView ivExpBoth = new ImageView(
           new Image( PlateCell.class.getResourceAsStream("resources/well_exp_bothAnno_20x20.png")));
        private final ImageView ivExpCol = new ImageView(
           new Image( PlateCell.class.getResourceAsStream("resources/well_exp_colAnno_20x20.png")));
        private final ImageView ivExpRow = new ImageView(
           new Image( PlateCell.class.getResourceAsStream("resources/well_exp_rowAnno_20x20.png")));
        private final ImageView ivStnd = new ImageView(
           new Image( PlateCell.class.getResourceAsStream("resources/well_stnd_20x20.png")));
        private final ImageView ivStndRow = new ImageView(
           new Image( PlateCell.class.getResourceAsStream("resources/well_stnd_rowAnno_20x20.png")));
        private final ImageView ivStndCol = new ImageView(
           new Image( PlateCell.class.getResourceAsStream("resources/well_stnd_colAnno_20x20.png")));
        private final ImageView ivStndBoth = new ImageView(
           new Image( PlateCell.class.getResourceAsStream("resources/well_stnd_bothAnno_20x20.png")));
    
        public PlateCell(){
            super();
            this.setAlignment(Pos.CENTER);
        }
 
        @Override 
        public void cancelEdit(){
            //Do nothing. Don't bother with isEditable
            //No-op
        }
        
        @Override
        public void startEdit(){
            //Do nothing. Don't bother with isEditable
            //No-op
        }
        
        @Override
        public void updateItem(WellData wd, boolean empty){
            super.updateItem(wd, empty);
            if(empty || wd == null){
                this.setGraphic(null);
                this.setText(null);
                return;
            }
                        
            if(this.getContextMenu() == null){
                this.setContextMenu(
                        new PlateViewContextMenu((PlateTable)this.getTableView()));
            }
            
            this.setHeight(CELL_HEIGHT);
            this.setMinHeight(CELL_HEIGHT);
            this.setWidth(CELL_WIDTH);
            this.setMinWidth(CELL_WIDTH);
            
            StringBuilder sb = new StringBuilder(wd.toString());
            sb.append("\nDilution Factor: ").append(wd.getDilutionFactor());
            sb.append("\nUse: ").append(wd.getPurpose().name());
            sb.append("\nRow Anno: ").append(wd.getAnnotations().get("rowAnno"));
            sb.append("\nCol Anno: ").append(wd.getAnnotations().get("colAnno"));
            this.setTooltip(new Tooltip(sb.toString()));
            
            //clear the text.
            setText(null);
            
            //Set graphic based on purpose & annotations
            switch(wd.getPurpose())
            {
                case STANDARD: 
                    //Check for rown and column annotation keys and set graphic
                    if(wd.getAnnotations().containsKey("rowAnno")){
                        if(wd.getAnnotations().containsKey("colAnno")){
                            this.setGraphic(ivStndBoth);
                        }else{
                            this.setGraphic(ivStndRow);
                        }
                    }else if(wd.getAnnotations().containsKey("colAnno")){
                        this.setGraphic(ivStndCol);
                    }else{
                        this.setGraphic(ivStnd);
                    }
                    //Check if the concentration is missing
                    if(wd.getConcentration() == null){
                        setText("!");
                        setTooltip( new Tooltip("Missing concentration") );
                    }
                    break;
                case BLANK: 
                    //Check for rown and column annotation keys and set graphic
                    if(wd.getAnnotations().containsKey("rowAnno")){
                        if(wd.getAnnotations().containsKey("colAnno")){
                            this.setGraphic(ivBkgdBoth);
                        }else{
                            this.setGraphic(ivBkgdRow);
                        }
                    }else if(wd.getAnnotations().containsKey("colAnno")){
                        this.setGraphic(ivBkgdCol);
                    }else{
                        this.setGraphic(ivBkgd);
                    }
                    break;
                case EXPERIMENT:
                    //Check for rown and column annotation keys and set graphic
                    if(wd.getAnnotations().containsKey("rowAnno")){
                        if(wd.getAnnotations().containsKey("colAnno")){
                            this.setGraphic(ivExpBoth);
                        }else{
                            this.setGraphic(ivExpRow);
                        }
                    }else if(wd.getAnnotations().containsKey("colAnno")){
                        this.setGraphic(ivExpCol);
                    }else{
                        this.setGraphic(ivExp);
                    }
                    break;
                case NONE: 
                    this.setGraphic(null);
                    this.setText("NA");
                    break;
                default:
                    this.setGraphic(null);
                    break;
            }
            
        }
        
        
        
    }

    public class WellDataConverter extends StringConverter<WellData>{

        @Override
        public String toString(WellData t) {
            return(
                t.getRow().toString() +"," + t.getCol().toString()
                  );
        }

        @Override
        public WellData fromString(String string) {
            WellData wd = new WellData();
            return( wd );
        }
    
    }
    
    /** Convenience class for representing each row of the plate as an plain old
     * java object with 12 object properties that each contain well data.
     * 
     * Might be able to rewrite this using a list or map of object properties.
     */
    public static class RowData{

        
        List<SimpleObjectProperty<WellData>> colList;
        public Integer tch = 0; //variable to modify to trigger data changed event
        
        /**
         * Constructors
        */
        
        RowData(List<WellData> lwd){   
            colList = new ArrayList<>();
            if(lwd == null){return;}
            if(lwd.size() != 12){return;}
            
            for(int i=0; i < 12; i++){
                SimpleObjectProperty<WellData> opw = new SimpleObjectProperty<>();
                opw.setValue(lwd.get(i));
                colList.add(opw);
            }  
        }

        public ObservableValue<WellData> getColumnProperty(int i) {
            return colList.get(i);
        }
        
        public void manualUpdateCols(){
            //For each value in the row data, save value, set null, set back to value
            for(SimpleObjectProperty<WellData> sp : colList){
                WellData w = sp.get();
                sp.setValue(null);
                sp.setValue(w);
            }
        }
    }
    
    public static class ColumnHeadButton extends Button{
        private final TableColumn tc;
        
        private final ImageView arrowDown = new ImageView(
            new Image(PlateCell.class.getResourceAsStream("resources/icon-arrow-down_16x16.png")));
                
        public ColumnHeadButton(TableColumn tc){
            this.tc = tc;
            this.setGraphic(arrowDown);
            this.setWidth(26.0);
            this.setMaxWidth(26.0);
            this.setMaxHeight(30.0);
            this.setOnAction( new CustomAction() );
        } 
        
        private class CustomAction implements EventHandler<ActionEvent> {
            
            @Override
            public void handle(ActionEvent event) {
                if(tc == null){return;}
                
                //Need to dynamically sort out the number of rows instead
                //of hard coding them. Should be the number of items backing the table
                int size = tc.getTableView().getItems().size();
                tc.getTableView().getSelectionModel().clearSelection();
                for(int i=0;i<size;i++){
                    tc.getTableView().getSelectionModel().select(i, tc);
                }
                
                tc.getTableView().requestFocus();
            }
        }
    }
    
    /*
    //Handler tasked with showing the context menu that was passed in from 
    // the parent control into the constructor/factory
    private class HandleMouseEvent implements EventHandler<MouseEvent>{

        @Override
        public void handle(MouseEvent evt) {
            if(ctxMenu == null){return;}
            //Only fire for right click.  Fuck you if you're using a Mac.
            //Will have to figure out somethig else for the Mac. meta button?
            if(evt.getButton() == MouseButton.SECONDARY || evt.isMetaDown()){
                ctxMenu.show((Node) evt.getSource(), evt.getScreenX(), evt.getScreenY());
                
            }
        }
        
    } */
}
