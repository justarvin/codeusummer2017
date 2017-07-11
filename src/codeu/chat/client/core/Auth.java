package codeu.chat.client.core;

import codeu.chat.common.BasicView;
import codeu.chat.util.Uuid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Auth {

  private BasicView view;
  private Set<Uuid> admins;
  private Map<Uuid, String> passwords;

  public Auth(BasicView view) {
    this.view = view;
    admins = new HashSet<>();
    passwords = new HashMap<>();
  }

  public boolean isAdmin(Uuid id) {
    return admins.contains(id);
  }

  public void addAdmin(String name) {
    admins.add(view.getUuid(name));
  }

  public String getPassword(Uuid id) {
    return passwords.get(id);
  }

}
