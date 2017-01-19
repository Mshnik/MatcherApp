package gui;

import io.ObjectIO;
import model.APPModel;
import model.Item;
import model.Person;
import common.Copyable;
import graph.matching.Matching;
import io.TextIO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Mshnik
 */
public class GUI extends JFrame {

  private static int NAME_COL = 0;
  private static int CAPACITY_COL = 1;
  private static int PRIORITY_COL = 1;

  private static int START_ITEM_COUNT = 15;
  private static int START_PEOPLE_COUNT = 20;
  private static int PEOPLE_CHOICE_COLS_TO_SHOW = 6;
  private static int PEOPLE_NON_CHOICE_COL_COUNT = 2;

  private static String NO_CHOICE_STRING = "----";

  private APPModel model;
  private boolean clearMatchingOnEdit;
  private boolean supressModelUpdateListener;
  private long timeOnStartMatching;

  private ValueFuncPane valueFuncPane;
  private ExpandingJTable itemTable;
  private ExpandingJTable peopleTable;

  private static String formatMatchingTime(long ms) {
    if (ms == -1) return "Ready to match";
    return "Matching took: " + ms + "ms";
  }

  private static String formatMatchingCompleteness(double percent) {
    if (percent == -1) {
      return "";
    }
    return ((double)(int)(percent * 10000))/100.0 + "% of people were matched";
  }

  private static String formatMatchingScore(double score, double maxScore) {
    if (score == -1) {
      return "";
    }
    double percent = ((double)(int)((score/maxScore * 10000)/100.0));
    return "Matching optimization " + percent + "% (" + score + "/" + maxScore + " points)";

  }

  private JLabel matchingTimeLabel;
  private JLabel matchingCompletenessLabel;
  private JLabel matchingScoreLabel;

  public GUI() {
    super();
    setTitle("Super Matcher");

    model = new APPModel();
    clearMatchingOnEdit = true;
    supressModelUpdateListener = false;
    timeOnStartMatching = -1;

    valueFuncPane = new ValueFuncPane();

    itemTable = ExpandingJTable.create(START_ITEM_COUNT, "Name","Capacity","Enrollment");
    ExcelAdapter.registerCopyPaste(itemTable);

    String[] choiceHeaders = new String[PEOPLE_NON_CHOICE_COL_COUNT + PEOPLE_CHOICE_COLS_TO_SHOW];
    choiceHeaders[0] = "Name";
    choiceHeaders[1] = "Priority";
    for(int i = PEOPLE_NON_CHOICE_COL_COUNT-1; i <= PEOPLE_CHOICE_COLS_TO_SHOW; i++) {
      choiceHeaders[i+PEOPLE_NON_CHOICE_COL_COUNT-1] = getStringForChoice(i);
    }
    peopleTable = ExpandingJTable.create(START_PEOPLE_COUNT, choiceHeaders);
    for(int i = 0; i < START_ITEM_COUNT - PEOPLE_CHOICE_COLS_TO_SHOW; i++) {
      peopleTable.addCol(getStringForChoice(PEOPLE_CHOICE_COLS_TO_SHOW + i + 1), true);
    }
    ExcelAdapter.registerCopyPaste(peopleTable);

    setUpNorthPanel();
    setUpCenterPanel();
    setUpWestPanel();
    setUpBottomPanel();

    itemTable.setColPredicate(0, itemTable.uniqueForCol(0), "Must be distinct from other entries");
    itemTable.setColPredicate(1, ExpandingJTable.INTS_ONLY.and(
        (s) -> s == null || s.equals("") || Integer.parseInt(s) >= 0),
        "Must be Integer and non-negative");
    itemTable.setColEditable(2, false);
    itemTable.getModel().addTableModelListener((e) -> updateModel());

    peopleTable.setColPredicate(0, peopleTable.uniqueForCol(0), "Must be distinct from other entries");
    peopleTable.setColPredicate(1, ExpandingJTable.INTS_ONLY, "Must be Integer");
    for(int c = PEOPLE_NON_CHOICE_COL_COUNT; c < peopleTable.getColumnCount(); c++) {
      peopleTable.setCellPredicate(-1, c, (s) -> s== null || s.equals(""), "Must be an Item, and distinct from others");
      for(int r = 0; r < peopleTable.getRowCount(); r++) {
        peopleTable.setCellPredicate(r, c, (s) -> {
          if (s == null || s.equals("")) return true;
          for(Item i : model.getItems()) {
            if (i.id().equals(s)) return true;
          }
          return false;
        }, "Must be an Item, and distinct from others");
      }
    }
    peopleTable.getModel().addTableModelListener((e) -> {
      if (! supressModelUpdateListener) {
        if (e.getColumn() >= PEOPLE_NON_CHOICE_COL_COUNT) {
          supressModelUpdateListener = true;
          for (int c = PEOPLE_NON_CHOICE_COL_COUNT; c < peopleTable.getColumnCount(); c++) {
            if (c != e.getColumn() && Objects.equals(peopleTable.getValueAt(e.getFirstRow(), e.getColumn()),peopleTable.getValueAt(e.getFirstRow(), c))) {
              peopleTable.setValueAt(NO_CHOICE_STRING, e.getFirstRow(), c);
            }
          }
          supressModelUpdateListener = false;
        }
        updateModel();
      }
    });

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    pack();
    setLocationRelativeTo(null);
    setVisible(true);
  }

  private Component leftJustify(Component panel)  {
    Box  b = Box.createHorizontalBox();
    b.add(panel);
    b.add(Box.createHorizontalGlue());
    return b;
  }

  private void setUpNorthPanel() {
    JPanel northPanel = setStandardMargins(new JPanel());
    northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.X_AXIS));

    JButton loadButton = new JButton("Load Data");
    loadButton.addActionListener((e) -> load());

    JButton saveButton = new JButton("Save Data");
    saveButton.addActionListener((e) -> saveData());

    northPanel.add(loadButton);
    northPanel.add(saveButton);

    getContentPane().add(northPanel, BorderLayout.NORTH);
  }

  private void setUpCenterPanel() {
    JPanel centerPanel = setStandardMargins(new JPanel());
    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

    centerPanel.add(leftJustify(new JLabel("People")));
    JScrollPane scrollPane = new JScrollPane(peopleTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    centerPanel.add(scrollPane);
    centerPanel.add(createAddButtonsPanel(peopleTable, "Add Person", "Add People..."));

    getContentPane().add(centerPanel, BorderLayout.CENTER);
  }

  private void setUpWestPanel() {
    JPanel westPanel = setStandardMargins(new JPanel());
    westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.Y_AXIS));

    westPanel.add(leftJustify(new JLabel("Items")));
    JScrollPane scrollPane = new JScrollPane(itemTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    westPanel.add(scrollPane);
    westPanel.add(createAddButtonsPanel(itemTable, "Add Item", "Add Items..."));

    getContentPane().add(westPanel, BorderLayout.WEST);
  }

  private JPanel createAddButtonsPanel(ExpandingJTable table, String singularText, String pluralText) {
    JPanel panel = setStandardMargins(new JPanel());
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

    panel.add(leftJustify(createExpandingJButton(table, singularText, () -> 1)));
    final JSpinner numberSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
    // TODO: fix numberSpinner size here, limiting height.
    panel.add(leftJustify(createExpandingJButton(table, pluralText, () -> (Integer)numberSpinner.getValue())));
    panel.add(leftJustify(numberSpinner));

    return panel;
  }

  private void setUpBottomPanel() {
    JButton matchButton = new JButton("Match");
    matchButton.addActionListener((e) -> {
      timeOnStartMatching = System.currentTimeMillis();
      model.match();
      updateMatching();
    });
    JButton clearMatchButton = new JButton("Clear");
    clearMatchButton.addActionListener((e) -> {
      model.clearMatching();
      updateMatching();
    });
    JButton outputMatchButton = new JButton("Save Matching...");
    outputMatchButton.addActionListener((e) -> saveMatching());

    JPanel bottomPanel = setStandardMargins(new JPanel());
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
    bottomPanel.add(new JSeparator());
    bottomPanel.add(new JLabel("Value Function"));
    bottomPanel.add(valueFuncPane);
    bottomPanel.add(new JSeparator());
    bottomPanel.add(new JLabel("Matching"));

    JPanel buttonPanel = setStandardMargins(new JPanel());
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
    buttonPanel.add(matchButton);
    buttonPanel.add(clearMatchButton);
    buttonPanel.add(outputMatchButton);

    JPanel infoPanel = setStandardMargins(new JPanel());
    infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
    matchingTimeLabel = new JLabel(formatMatchingTime(-1));
    matchingCompletenessLabel = new JLabel(formatMatchingCompleteness(-1));
    matchingScoreLabel = new JLabel(formatMatchingScore(-1,1));

    infoPanel.add(matchingTimeLabel);
    infoPanel.add(matchingCompletenessLabel);
    infoPanel.add(matchingScoreLabel);

    JPanel splitPanel = new JPanel();
    splitPanel.setLayout(new BoxLayout(splitPanel, BoxLayout.X_AXIS));
    splitPanel.add(buttonPanel);
    splitPanel.add(infoPanel);

    bottomPanel.add(splitPanel);

    getContentPane().add(bottomPanel, BorderLayout.SOUTH);
  }

  private JButton createExpandingJButton(ExpandingJTable table, String buttonText, Supplier<Integer> repetitionsSupplier) {
    JButton button = new JButton(buttonText);
    button.addActionListener((e) -> {
      final int repetitions = repetitionsSupplier.get();
      for(int i = 0; i < repetitions; i++) {
        table.addRow();
        if (table == itemTable) {
          int lastNumber = peopleTable.getColumnCount() + 1 - PEOPLE_NON_CHOICE_COL_COUNT;
          peopleTable.addCol(getStringForChoice(lastNumber), true);
        }
      }
      pack();
    });
    button.setAlignmentX(Component.LEFT_ALIGNMENT);
    return button;
  }

  private JPanel setStandardMargins(JPanel panel) {
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));
    return panel;
  }

  private static String getStringForChoice(int choice) {
    if (choice <= 0) throw new IllegalArgumentException();

    if (choice / 10 == 1) return choice + "th";
    if (choice % 10 == 1) return choice + "st";
    if (choice % 10 == 2) return choice + "nd";
    if (choice % 10 == 3) return choice + "rd";
    return choice + "th";
  }

  private void updateModel() {
    model.clear();

    for(int r = 0; r < itemTable.getRowCount(); r++) {
      Object nameObj = itemTable.getModel().getValueAt(r,NAME_COL);
      if (nameObj != null) {
        Object capObj = itemTable.getModel().getValueAt(r, CAPACITY_COL);
        int capacity = 1;
        if (capObj != null) {
          try{
            capacity = Integer.parseInt(capObj.toString());
          }catch(NumberFormatException ex) {}
        }
        model.createItem(nameObj.toString(), capacity);
      }
    }
    for(int r = 0; r < peopleTable.getRowCount(); r++) {
      Object nameObj = peopleTable.getModel().getValueAt(r, NAME_COL);
      if (nameObj != null) {
        Object priorityObj = peopleTable.getModel().getValueAt(r,PRIORITY_COL);
        int priority = 0;
        if (priorityObj != null) {
          try {
            priority = Integer.parseInt(priorityObj.toString());
          } catch (NumberFormatException ex) {
          }
        }

        List<String> prefs = new ArrayList<>();

        for (int c = PEOPLE_NON_CHOICE_COL_COUNT; c < peopleTable.getColumnCount(); c++) {
          Object prefObj = peopleTable.getModel().getValueAt(r, c);
          if (prefObj != null && !prefObj.equals("") && !prefObj.equals(NO_CHOICE_STRING)) {
            prefs.add(prefObj.toString());
          }
        }
        model.createPerson(nameObj.toString(), priority, prefs);
      }
    }
    //Update item choices in table
    List<String> itemNames = new ArrayList<>();
    itemNames.add(NO_CHOICE_STRING);
    for(Item i : model.getItems()){
      itemNames.add(i.id());
    }
    //List<Integer> cols = IntStream.range(PEOPLE_NON_CHOICE_COL_COUNT, peopleTable.getColumnCount()).mapToObj(Integer::valueOf).collect(Collectors.toList());
    //peopleTable.setComboBoxes(cols, itemNames);

    if (clearMatchingOnEdit) {
      model.clearMatching();
      updateMatching();
    }
  }

  private void updateMatching() {
    long timeOnMatchingFinish = System.currentTimeMillis();
    clearMatchingOnEdit = false;
    model.setValueFunc(valueFuncPane.getValueFunc());
    Matching<Person, Copyable<Item>> mostRecentMatching = model.getMatching();

    boolean[][] highlighted = new boolean[peopleTable.getRowCount()][peopleTable.getColumnCount()];
    for(int r = 0; r < peopleTable.getRowCount(); r++) {
      Object nameObj = peopleTable.getModel().getValueAt(r,0);
      if (nameObj != null && ! nameObj.equals("")) {
        Person p = model.getPerson(nameObj.toString());
        for (int c = PEOPLE_NON_CHOICE_COL_COUNT; c < peopleTable.getColumnCount(); c++) {
          Object itemObj = peopleTable.getModel().getValueAt(r,c);
          if (itemObj != null) {
            highlighted[r][c] = mostRecentMatching != null && mostRecentMatching.isMatched(p) && mostRecentMatching.getMatchedB(p).get().id().equals(itemObj.toString());
          }
        }
        peopleTable.setValueAt(p.getPriority(),r,1);
      }
    }
    peopleTable.setCellHighlight(highlighted);

    for(int r = 0; r < itemTable.getRowCount(); r++) {
      Object itemObj = itemTable.getValueAt(r,0);
      if (itemObj != null) {
        Item item = model.getItem(itemObj.toString());
        if (item != null) {
          int count = 0;
          if (mostRecentMatching != null) {
            for (Copyable<Item> c : mostRecentMatching.getMatchedB()) {
              if (item.equals(c.get())) {
                count++;
              }
            }
          }
          itemTable.setValueAt(item._2, r,1);
          itemTable.setValueAt(count, r, 2);
        }
      }
    }

    if (mostRecentMatching == null) {
      matchingTimeLabel.setText(formatMatchingTime(-1));
      matchingCompletenessLabel.setText(formatMatchingCompleteness(-1));
      matchingScoreLabel.setText(formatMatchingScore(-1, 1));
    } else {
      matchingTimeLabel.setText(formatMatchingTime(timeOnMatchingFinish - timeOnStartMatching));
      matchingCompletenessLabel.setText(formatMatchingCompleteness(model.getMatchedPercentage()));
      matchingScoreLabel.setText(formatMatchingScore(model.getMatchingScore(), model.getMaxScore()));
    }

    repaint();
    clearMatchingOnEdit = true;
  }

  private static interface HasExtension {
    public String getExtension();
  }

  private static class TextFileFilter extends FileFilter implements HasExtension {

    @Override
    public boolean accept(File f) {
      return f != null && f.isDirectory();
    }

    @Override
    public String getDescription() {
      return "Text (*.txt)";
    }

    public String getExtension() {
      return ".txt";
    }
  }

  private static class CSVFileFilter extends FileFilter implements HasExtension {

    @Override
    public boolean accept(File f) {
      return f != null && f.isDirectory();
    }

    @Override
    public String getDescription() {
      return "Comma Separated Value (*.csv)";
    }

    public String getExtension() {
      return ".csv";
    }
  }

  private static String itemToString(Copyable<Item> c) {
    if (c == null || c.get() == null) return "UNASSIGNED";
    return c.get().id();
  }

  private void saveData() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setAcceptAllFileFilterUsed(false);
    fileChooser.setFileFilter(new SerializedObjectFilter());

    int response = fileChooser.showSaveDialog(this);
    if (response == JFileChooser.APPROVE_OPTION) {
      String path = fileChooser.getSelectedFile().getAbsolutePath();
      if (! path.endsWith(".ser")) {
        path += ".ser";
      }

      try {
        ObjectIO.write(model, path);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void saveMatching() {
    if (model.getMatching() != null) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setAcceptAllFileFilterUsed(false);
      fileChooser.setFileFilter(new TextFileFilter());
      fileChooser.addChoosableFileFilter(new CSVFileFilter());

      int response = fileChooser.showSaveDialog(this);
      if (response == JFileChooser.APPROVE_OPTION) {
        String path = fileChooser.getSelectedFile().getAbsolutePath();
        String extension = ((HasExtension) fileChooser.getFileFilter()).getExtension();
        boolean isText = extension.equals(".txt");
        File f;
        if (path.contains(".")) {
          f = new File(path.substring(0, path.indexOf('.')) + extension);
        } else {
          f = new File(path + extension);
        }

        StringBuilder data = new StringBuilder();
        String div = isText ? "" : ",";

        data.append("Name");
        if (isText) {
          data.append(" (Priority)\t");
        } else {
          data.append(div);
          data.append("Priority");
          data.append(div);
        }
        data.append("Assignment");
        if (isText) {
          data.append(" (Value)");
        } else {
          data.append(div);
          data.append("Value");
        }
        data.append('\n');

        for(Person p : model.getPeople()) {
          data.append(p.getName());
          if (isText) {
            data.append('(');
          } else {
            data.append(div);
          }
          data.append(p.getPriority());
          if(isText) {
            data.append(")\t");
          } else {
            data.append(div);
          }
          data.append(itemToString(model.getMatching().getMatchedB(p)));
          if (model.getMatching().isMatched(p)) {
            if (isText) {
              data.append(" (");
              data.append(p.getPreference(model.getMatching().getMatchedB(p).get()));
              data.append(')');
            } else {
              data.append(div);
              data.append(p.getPreference(model.getMatching().getMatchedB(p).get()));
            }
          }
          data.append('\n');
        }

        try {
          TextIO.write(f,data.toString());
        }catch(IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public void load() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setAcceptAllFileFilterUsed(false);
    fileChooser.setFileFilter(new SerializedObjectFilter());

    int response = fileChooser.showOpenDialog(this);
    if (response == JFileChooser.APPROVE_OPTION) {

      try {
        updateGuiForModel(ObjectIO.read(APPModel.class, fileChooser.getSelectedFile()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void updateGuiForModel(APPModel model) {

    // Clear existing values
    for(int r = 0; r < itemTable.getRowCount(); r++) {
      for(int c = 0; c < itemTable.getColumnCount(); c++) {
        itemTable.getModel().setValueAt(null, r,c);
      }
    }
    for(int r = 0; r < peopleTable.getRowCount(); r++) {
      for(int c = 0; c < peopleTable.getColumnCount(); c++) {
        peopleTable.setValueAt(null, r,c);
      }
    }

    // Ensure size
    for(int r = itemTable.getRowCount(); r < model.getItemsSize(); r++) {
      itemTable.addRow();
      int lastNumber = peopleTable.getColumnCount() + 1 - PEOPLE_NON_CHOICE_COL_COUNT;
      peopleTable.addCol(getStringForChoice(lastNumber), true);
    }
    for(int r = peopleTable.getRowCount(); r < model.getPeopleSize(); r++) {
      peopleTable.addRow();
    }

    // Fill values
    int r = 0;
    for(Item i : model.getItems()) {
      itemTable.setValueAt(i.id(), r, NAME_COL);
      itemTable.setValueAt(i._2, r, CAPACITY_COL);
      r++;
    }

    r = 0;
    for(Person p : model.getPeople()) {
      peopleTable.setValueAt(p.getName(), r, NAME_COL);
      peopleTable.setValueAt(p.getPriority(), r, PRIORITY_COL);

      int c = PEOPLE_NON_CHOICE_COL_COUNT;
      for(Item i : p.getPreferencesInOrder()) {
        peopleTable.setValueAt(i.id(), r, c);
        c++;
      }
      r++;
    }

    //Set value function
    valueFuncPane.setValueFunc(model.getValueFunc().getPrefScaling(), model.getValueFunc().getPrefPower(), model.getValueFunc().getPriorityScaling());

    this.model = model;
  }

  public static void main(String[] args) {
    new GUI();
  }
}
