package codeu.chat.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Info to store information about an instance of a play
 */
public class PlayInfo {

  private Map<String, Uuid> roles;
  private Uuid next;
  private String title;

  public PlayInfo(String title) {
    roles = new HashMap<>();
    this.title = title;
  }

  public void setRole(String character, Uuid user) {
    roles.put(character, user);
  }

  public void setNext(Uuid user) {
    next = user;
  }

  public Uuid getNext() {
    return next;
  }
}
