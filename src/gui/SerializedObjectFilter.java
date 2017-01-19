package gui;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * @author Mshnik
 */
public class SerializedObjectFilter extends FileFilter {

  @Override
  public boolean accept(File f) {
    return f.getName().endsWith(".ser");
  }

  @Override
  public String getDescription() {
    return "Serialized Data *.ser";
  }
}
