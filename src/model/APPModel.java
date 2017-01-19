package model;

import common.Copyable;
import common.types.Tuple2;
import graph.Algorithm;
import graph.matching.Matching;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Mshnik
 */
public class APPModel implements Serializable {

  private Map<String, Person> people;
  private Map<String, Item> items;

  private ValueFunc valueFunc;
  private transient Matching<Person, Copyable<Item>> matching;

  public APPModel() {
    valueFunc = ValueFunc.DEFAULT;
    people = new LinkedHashMap<>();
    items = new LinkedHashMap<>();
  }

  public int getItemsSize() {
    return items.size();
  }

  public Collection<Item> getItems() {
    return Collections.unmodifiableCollection(items.values());
  }

  public int getPeopleSize() {
    return people.size();
  }

  public Collection<Person> getPeople() {
    return Collections.unmodifiableCollection(people.values());
  }

  public Person getPerson(String name) {
    return people.get(name);
  }

  public Item getItem(String name) {
    return items.get(name);
  }

  public void clear() {
    people.clear();
    items.clear();
  }

  public void createItem(String name, int cap) {
    items.put(name, new Item(name, cap));
  }

  public void createPerson(String personName, int priority, List<String> itemPrefs) throws RuntimeException {
    List<Item> itemPrefs2 = itemPrefs.stream().map(items::get).collect(Collectors.toList());
    if (itemPrefs.size() != itemPrefs2.size()) {
      throw new RuntimeException("Had unbound item name. Got " + itemPrefs + " but only found " + itemPrefs2);
    }
    if (! people.containsKey(personName)) {
      Person p = new Person(personName, priority).setPreferences(itemPrefs2);
      people.put(personName, p);
    } else {
      people.get(personName).setPreferences(itemPrefs2);
    }
  }

  public ValueFunc getValueFunc() {
    return valueFunc;
  }

  public void setValueFunc(ValueFunc f) {
    valueFunc = f;
    if (valueFunc == null) {
      valueFunc = ValueFunc.DEFAULT;
    }
  }

  public Matching<Person, Copyable<Item>> match() {
    Map<Item, Integer> itemAndCount = items.values().stream().collect(Collectors.toMap(Function.identity(), (i) -> i._2));
    matching = Algorithm.maxValueMaxMatching(people.values(), itemAndCount, (a,i) -> (int)(valueFunc.apply(a.getPreference(i), a.getPriority())*1000000));
    return matching;
  }

  public void clearMatching() {
    matching = null;
  }

  public Matching<Person, Copyable<Item>> getMatching() {
    return matching;
  }

  public double getMatchedPercentage() {
    if (matching == null) return 0;
    return ((double)matching.getMatchedA().size())/matching.totalSizeA();
  }

  public double getMatchingScore() {
    if (matching == null) return 0;
    double score = 0;
    for(Person p : matching.getMatchedA()) {
      score += valueFunc.apply(p.getPreference(matching.getMatchedB(p).get()), p.getPriority());
    }
    return score;
  }

  public int getMaxScore() {
    if (matching == null) return 0;
    int score = 0;
    for(Person p : matching.getA()) {
      score += valueFunc.apply(p.getHighestPref(), p.getPriority());
    }
    return score;
  }

  private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
    aInputStream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
    aOutputStream.defaultWriteObject();
  }
}
