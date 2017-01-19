package gui;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class FrozenTablePane extends JScrollPane{

  private ExpandingJTable frozenTable;

  public FrozenTablePane(ExpandingJTable table, int colsToFreeze){
    super(table);

    String[] headers = new String[colsToFreeze];
    for(int i = 0; i < colsToFreeze; i++) {
      headers[i] = table.getColumnName(i);
    }

    //create frozen table, hook original up to this
    frozenTable = ExpandingJTable.create(table.getRowCount(), headers);
//    table.getSelectedColFromHelper = () -> frozenTable.getSelectedColumn();
//    frozenTable.syncWith(table);

    TableModel frozenModel = frozenTable.getModel();
    //populate the frozen model
    for (int i = 0; i < table.getRowCount(); i++) {
      for (int j = 0; j < colsToFreeze; j++) {
        String value = (String) table.getValueAt(i, j);
        frozenModel.setValueAt(value, i, j);
      }
    }

    //remove the frozen columns from the original table
    for (int j = 0; j < colsToFreeze; j++) {
      table.removeColumn(table.getColumnModel().getColumn(0));
    }
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    //format the frozen table
    JTableHeader header = table.getTableHeader();
    frozenTable.setBackground(header.getBackground());
    frozenTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    frozenTable.setGridColor(table.getGridColor());
    frozenTable.setShowHorizontalLines(table.getShowHorizontalLines());
    frozenTable.setShowVerticalLines(table.getShowVerticalLines());
    frozenTable.setSelectionBackground(table.getSelectionBackground());
    frozenTable.setDefaultEditor(Object.class, table.getDefaultEditor(Object.class));
    frozenTable.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {}

      @Override
      public void focusLost(FocusEvent e) {
        frozenTable.removeRowSelectionInterval(0,frozenTable.getRowCount()-1);
      }
    });

    //set frozen table as row header view
    JViewport viewport = new JViewport();
    viewport.setView(frozenTable);
    viewport.setPreferredSize(frozenTable.getPreferredSize());
    setRowHeaderView(viewport);
    setCorner(JScrollPane.UPPER_LEFT_CORNER, frozenTable.getTableHeader());
    getVerticalScrollBar().addAdjustmentListener((e) -> {
      viewport.setViewPosition(table.getVisibleRect().getLocation());
    });
  }

  public ExpandingJTable getFrozenTable() {
    return frozenTable;
  }
}