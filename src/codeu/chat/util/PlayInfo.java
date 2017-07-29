package codeu.chat.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Info to store information about an instance of a play
 */
public class PlayInfo {

  private Map<String, Uuid> roles;
  private List<String> openRoles;
  private Uuid next;
  private String title;

  public PlayInfo(String title, List<String> openRoles) {
    roles = new HashMap<>();
    this.title = title;
    this.openRoles = openRoles;
  }

  public void setRole(Uuid user) {
    String character = openRoles.remove(0);
    roles.put(character, user);
  }

  public void setNext(Uuid user) {
    next = user;
  }

  public Uuid getNext() {
    return next;
  }

  public boolean filled() {
    return openRoles.isEmpty();
  }
}
