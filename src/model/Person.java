package model;

import common.types.Tuple2;
import graph.matching.RankedAgent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

/**
 * @author Mshnik
 */
public class Person extends Tuple2<String,Integer> implements RankedAgent<Item>, Serializable {

  private Map<Item, Integer> prefs;
  private int highestPref;

  public Person(String name,int priority) {
    super(name,priority);
    prefs = new HashMap<>();
    highestPref = 0;
  }

  public String getName() {
    return _1;
  }

  public int getPriority() {
    return _2;
  }

  public Person withPref(Item i, Integer val) {
    prefs.put(i, val);
    highestPref = prefs.values().stream().mapToInt((x) -> x).max().getAsInt();
    return this;
  }

  public Person setPreferences(List<Item> preferences) {
    prefs.clear();
    for(int i = 0; i < preferences.size(); i++) {
      prefs.put(preferences.get(i), preferences.size() - i);
    }
    highestPref = preferences.size();
    return this;
  }

  public int getHighestPref() {
    return highestPref;
  }

  @Override
  public Map<Item, Integer> getPreferences() {
    return Collections.unmodifiableMap(prefs);
  }

  public List<Item> getPreferencesInOrder() {
    ArrayList<Item> prefsLst = new ArrayList<>(prefs.keySet());
    prefsLst.sort(Comparator.comparingInt(prefs::get));
    return Collections.unmodifiableList(prefsLst);
  }

  @Override
  public String toString() {
    return super.toString() + ">" + getPreferences();
  }

  private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
    aInputStream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
    aOutputStream.defaultWriteObject();
  }
}
