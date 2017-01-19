package gui;

import common.types.Either;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Predicate;
import java.awt.*;


/**
 * @author Mshnik
 */
public class ExpandingJTable extends JTable {

  private Map<Integer, Either<Predicate<String>, Map<Integer, Predicate<String>>>> validatorMap;
  private Map<Integer, String> invalidMessageMap;
  private Map<Integer, Boolean> isColEditable;

  private List<Integer> comboBoxCols;

  private boolean[][] cellHighlight;
  private boolean retainSelectionWhenFocusLost = false;

  public static ExpandingJTable create(int rows, String... columnNames) {
    Object[][] data = new Object[rows][columnNames.length];
    DefaultTableModel model = new MyTableModel(data, columnNames);
    ExpandingJTable table = new ExpandingJTable(model);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    table.setPreferredScrollableViewportSize(table.getPreferredSize());
    return table;
  }

  private ExpandingJTable(DefaultTableModel model) {
    super(model);

    validatorMap = new HashMap<>();
    isColEditable = new HashMap<>();
    invalidMessageMap = new HashMap<>();
    for(int i = 0; i < getColumnCount(); i++) {
      isColEditable.put(i, true);
    }
    ((MyTableModel)getModel()).isColEditable = isColEditable;

    gridColor = Color.GRAY;
    setSelectionBackground(Color.LIGHT_GRAY);
    setShowGrid(false);
    if (getColumnCount() > 1) {
      setShowVerticalLines(true);
    }
    setShowHorizontalLines(true);

    InputVerifier iv = new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = ((JTextField) input).getText();
        int col = getSelectedColumn();
        int row = getSelectedRow();
        if (!validatorMap.containsKey(col)) return true;
        if (validatorMap.get(col).isLeft()) {
          return validatorMap.get(col).asLeft().test(text);
        } else {
          return validatorMap.get(col).asRight().containsKey(row) && validatorMap.get(col).asRight().get(row).test(text);
        }
      }

      @Override
      public boolean shouldYieldFocus(JComponent input) {
        boolean valid = verify(input);
        if (!valid) {
          int col = getSelectedColumn();
          JOptionPane.showMessageDialog(null, "Invalid Input: " + invalidMessageMap.get(col));
        }
        return valid;
      }
    };

    setDefaultEditor(Object.class, new MyCellEditor(new JTextField(), iv));

    addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {

      }

      @Override
      public void focusLost(FocusEvent e) {
        if (retainSelectionWhenFocusLost) {
          retainSelectionWhenFocusLost = false;
          removeRowSelectionInterval(0, getRowCount() - 1);
        }
      }
    });

    setDefaultRenderer(Object.class, new MyRenderer());
  }

  public static Predicate<String> INTS_ONLY = (s) -> {
    if (s == null || "".equals(s)) return true;
    try {
      Integer.parseInt(s);
      return true;
    } catch(NumberFormatException ex) {
      return false;
    }
  };

  public static Predicate<String> BOOLS_ONLY = (s) -> {
    if (s == null || "".equals(s)) return true;
    try {
      Boolean.parseBoolean(s);
      return true;
    } catch(NumberFormatException ex) {
      return false;
    }
  };

  public Predicate<String> uniqueForCol(final int col) {
    return (s) -> {
      if (s == null || s.equals("")) return true;
      for(int r = 0; r < getRowCount(); r++) {
        Object val = getValueAt(r, col);
        if (val != null && val.toString().equals(s)) {
          return false;
        }
      }
      return true;
    };
  }

  public ExpandingJTable setColPredicate(int col, Predicate<String> pred, String messageOnInvalid) {
    validatorMap.put(col, Either.createLeft(pred));
    invalidMessageMap.put(col, messageOnInvalid);
    return this;
  }

  public ExpandingJTable setCellPredicate(int row, int col, Predicate<String> pred, String messageOnInvalid) {
    if (! validatorMap.containsKey(col)) {
      validatorMap.put(col, Either.createRight(new HashMap<>()));
    }
    validatorMap.get(col).asRight().put(row, pred);
    invalidMessageMap.put(col, messageOnInvalid);
    return this;
  }

  public ExpandingJTable setColEditable(int col, boolean editable) {
    isColEditable.put(col, editable);
    return this;
  }

  public void addRow() {
    DefaultTableModel model = (DefaultTableModel)getModel();
    Object[] row = new Object[getColumnCount()];
    model.addRow(row);
  }

  public void addCol(String header, boolean isEditable) {
    isColEditable.put(getColumnCount(), isEditable);
    DefaultTableModel model = (DefaultTableModel)getModel();
    model.addColumn(header);
  }

  public ExpandingJTable setCellHighlight(boolean[][] cellHighlight) {
    this.cellHighlight = cellHighlight;
    return this;
  }

  public ExpandingJTable setComboBoxes(List<Integer> comboBoxCols, List<String> comboBoxChoices) {
    if (this.comboBoxCols != null) {
      for(int i : this.comboBoxCols) {
        getColumnModel().getColumn(i).setCellEditor(null);
      }
    }
    JComboBox<String> comboBox = new JComboBox<>();
    for(String s : comboBoxChoices) {
      comboBox.addItem(s);
    }

    MyCellEditor newEditor = new MyCellEditor(comboBox);

    this.comboBoxCols = comboBoxCols;
    ((MyTableModel)getModel()).comboBoxCols = comboBoxCols;
    for(int i : comboBoxCols) {
      getColumnModel().getColumn(i).setCellEditor(newEditor);
    }

    return this;
  }

  private class MyCellEditor extends DefaultCellEditor {

    private InputVerifier iv;
    private final boolean isJTextField;

    public MyCellEditor(JTextField textField, InputVerifier iv) {
      super(textField);
      textField.setInputVerifier(iv);
      this.iv = iv;
      isJTextField = true;
    }

    public MyCellEditor(JComboBox<String> comboBox) {
      super(comboBox);
      isJTextField = false;
    }

    @Override
    public boolean stopCellEditing() {
      if (iv != null && isJTextField) {
        if (!iv.shouldYieldFocus((JTextField)getComponent())) {
          retainSelectionWhenFocusLost = true;
          return false;
        }
      }
      return super.stopCellEditing();
    }
  }

  private static class MyTableModel extends DefaultTableModel {

    private Map<Integer, Boolean> isColEditable;
    private List<Integer> comboBoxCols;

    public MyTableModel(Object[][] data, String[] headers){
      super(data, headers);
    }

    public boolean isCellEditable(int row, int column){
      return isColEditable.get(column) && (comboBoxCols == null || ! comboBoxCols.contains(column) ||
             getValueAt(row, 0) != null);
    }
  }

  private class MyRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean   isSelected, boolean hasFocus, int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (! table.isRowSelected(row)) {
        if(cellHighlight != null && row < cellHighlight.length && column < cellHighlight[row].length && cellHighlight[row][column]) c.setBackground(Color.GREEN);
        else c.setBackground(table.getBackground());
      }
      return c;
    }
  }
}
