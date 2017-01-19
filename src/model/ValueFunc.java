package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Mshnik
 */
public class ValueFunc implements Serializable {

  public static ValueFunc DEFAULT = new ValueFunc(10, 1, 1);

  public static ValueFunc ofScalingFactors (final double prefScaling, final double priorityScaling) {
    return new ValueFunc(prefScaling, 1, priorityScaling);
  }

  public static ValueFunc ofScalingAndPowerFactors (
      final double prefScaling,
      final double prefPower,
      final double priorityScaling) {
    return new ValueFunc(prefScaling, prefPower, priorityScaling);
  }

  private double prefScaling;
  private double prefPower;
  private double priorityScaling;
  public transient BiFunction<Double, Double, Double> f;

  private ValueFunc(double prefScaling, double prefPower, double priorityScaling) {
    this.prefScaling = prefScaling;
    this.prefPower = prefPower;
    this.priorityScaling = priorityScaling;

    createFunction();
  }

  public double getPrefScaling() {
    return prefScaling;
  }

  public double getPrefPower() {
    return prefPower;
  }

  public double getPriorityScaling() {
    return priorityScaling;
  }

  private void createFunction() {
    f = (pref, priority) ->
        prefScaling * Math.pow(pref, prefPower) + priorityScaling * pref * priority;
  }

  public double apply(int pref, int priority) {
    return f.apply((double)pref, (double)priority);
  }

  public double apply(double pref, double priority) {
    return f.apply(pref, priority);
  }

  public String toString() {
    return String.format("Value(pref,prior)=%.1f * pref^%.1f + %.1f * pref * prior", prefScaling, prefPower, priorityScaling);
  }

  private void readObject(
      ObjectInputStream aInputStream
  ) throws ClassNotFoundException, IOException {
    aInputStream.defaultReadObject();
    createFunction();
  }

  private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
    aOutputStream.defaultWriteObject();
  }
}
