package model;

import common.types.Tuple2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author Mshnik
 */
public class Item extends Tuple2<String, Integer> implements Serializable {

  public Item(String id, int capacity) {
    super(id, capacity);
  }

  public String id() {
    return _1;
  }

  private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
    aInputStream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
    aOutputStream.defaultWriteObject();
  }
}
