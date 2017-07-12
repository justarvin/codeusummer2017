package codeu.chat.client.core;

import codeu.chat.client.core.View;
import codeu.chat.util.PasswordStorage;
import codeu.chat.util.PersistenceLog;
import codeu.chat.util.Uuid;

import java.io.Console;
import java.util.*;

public class Auth {

  private Set<Uuid> admins, noPasswords;
  private Map<Uuid, String> passwords;

  public Auth() {
    admins = new HashSet<>();
    noPasswords = new HashSet<>();
    passwords = new HashMap<>();
  }

  public boolean isAdmin(Uuid id) {
    return admins.contains(id);
  }

  public void addAdmin(String name) {
    Uuid id = new View().getUuid(name);
    admins.add(id);
    noPasswords.add(id);
  }

  public void addFirstAdmin(Uuid id) {
    admins.add(id);
    noPasswords.add(id);
  }

  public boolean isNewAdmin(Uuid id) {
    return noPasswords.contains(id) && admins.contains(id);
  }

  public void authenticate(Uuid id) {
    Console console = System.console();
    char passwordArray[] = console.readPassword("Enter your password: ");
    try {
      while (!PasswordStorage.verifyPassword(passwordArray, passwords.get(id))) {
        System.out.println("Login failed. Please try again");
        passwordArray = console.readPassword("Enter your password: ");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setPassword(Uuid id) {
    Console console = System.console();
    char password[] = console.readPassword("Enter a new password: ");
    char passwordConfirm[] = console.readPassword("Retype your password: ");
    while (!Arrays.equals(password, passwordConfirm)) {
      System.out.println("Passwords didn't match. Please try again.");
      password = console.readPassword("Enter a new password: ");
      passwordConfirm = console.readPassword("Retype your password: ");
    }
    try {
      String pass = PasswordStorage.createHash(password);
      passwords.put(id, pass);
      noPasswords.remove(id);
      PersistenceLog.writeAuthInfo(id, pass);
    } catch (PasswordStorage.CannotPerformOperationException e) {
      e.printStackTrace();
    }
  }

  public void addPassword(Uuid id, String password) {
    passwords.put(id, password);
  }

  public String getPassword(Uuid id) {
    return passwords.get(id);
  }

  void setAdmins(HashSet<Uuid> admins) {
    this.admins = admins;
  }

  void setNewAdmins(HashSet<Uuid> newAdmins) {
    noPasswords = newAdmins;
  }
  public Set<Uuid> getAdmins() {
    return admins;
  }

  public Set<Uuid> getNewAdmins() {
    return noPasswords;
  }
}
