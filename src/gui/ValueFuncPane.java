package gui;

import model.ValueFunc;

import javax.swing.*;
import javax.swing.event.ChangeListener;

/**
 * @author Mshnik
 */
public class ValueFuncPane extends JPanel {

  private ValueFunc valueFunc;

  private JSpinner prefScalingSpinner;
  private JSpinner prefPowerSpinner;
  private JSpinner priorityScalingSpinner;

  private JLabel toStringLabel;


  public ValueFuncPane() {
    toStringLabel = new JLabel("");

    prefScalingSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, Integer.MAX_VALUE, 0.1));
    prefPowerSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, Integer.MAX_VALUE, 0.1));
    priorityScalingSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, Integer.MAX_VALUE, 0.1));

    ChangeListener recomputeListener = (e) -> recomputeValueFunc();
    prefScalingSpinner.addChangeListener(recomputeListener);
    prefPowerSpinner.addChangeListener(recomputeListener);
    priorityScalingSpinner.addChangeListener(recomputeListener);

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    add(new JLabel("  Preference Scaling:"));
    add(prefScalingSpinner);
    add(new JLabel("      Preference Power:"));
    add(prefPowerSpinner);
    add(new JLabel("      Priority Scaling:"));
    add(priorityScalingSpinner);
    add(new JLabel("      "));
    add(toStringLabel);
    add(new JLabel("      "));

    recomputeValueFunc();
  }

  private void recomputeValueFunc() {
    valueFunc = ValueFunc.ofScalingAndPowerFactors(
        (Double)prefScalingSpinner.getValue(),
        (Double)prefPowerSpinner.getValue(),
        (Double)priorityScalingSpinner.getValue());

    toStringLabel.setText(valueFunc.toString());
  }

  public void setValueFunc(double prefScaling, double prefPower, double priorityScaling) {
    prefScalingSpinner.setValue(prefScaling);
    prefPowerSpinner.setValue(prefPower);
    priorityScalingSpinner.setValue(priorityScaling);

    recomputeValueFunc();
  }

  public ValueFunc getValueFunc() {
    return valueFunc;
  }
}
