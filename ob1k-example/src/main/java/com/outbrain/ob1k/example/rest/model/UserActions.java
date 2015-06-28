package com.outbrain.ob1k.example.rest.model;

/**
 * Just a pojo which describes actions on specific user id
 *
 * @author marenzon
 */
public class UserActions {

  private int id;
  private Actions action;

  /**
   * Note: empty constructor for the marshallers
   */
  public UserActions() {
  }

  public UserActions(final int id, final Actions action) {
    this.id = id;
    this.action = action;
  }

  public int getId() {
    return id;
  }

  public Actions getAction() {
    return action;
  }

  public void setAction(final Actions action) {
    this.action = action;
  }

  public static enum Actions {
    CREATE,
    UPDATE,
    DELETE,
    ERROR;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }
}