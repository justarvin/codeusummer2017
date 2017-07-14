package codeu.chat.client.core;

import codeu.chat.util.PasswordStorage;
import codeu.chat.util.Uuid;

import java.io.Console;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class for admin-related tasks, such as managing admins and checking passwords
 */
public class Admin {

  private Set<Uuid> admins, noPasswords;
  private Map<Uuid, String> passwords;
  private Context context;
  private Controller controller;

  public Admin(Context context) {
    this.context = context;
    this.controller = context.getController();
    admins = new HashSet<>();
    noPasswords = new HashSet<>();
    passwords = new HashMap<>();
  }

  public Admin() {
    admins = new HashSet<>();
    noPasswords = new HashSet<>();
    passwords = new HashMap<>();
  }

  public boolean isAdmin(Uuid id) {
    return admins.contains(id);
  }

  public void addAdmin(Uuid id) {
    admins.add(id);
    noPasswords.add(id);
  }

  public boolean isNewAdmin(Uuid id) {
    return noPasswords.contains(id) && admins.contains(id);
  }

  public void removeAdmin(Uuid id) {
    admins.remove(id);
    passwords.remove(id);
    noPasswords.remove(id);
  }

  public void retrieveAuthInfo(Admin auth, Uuid id) {
    String password = context.getView().retrieveAuthInfo(id);
    if (password != null) {
      auth.addPassword(id, password);
    }
  }

  public void retrieveAdmins() {
    HashSet<Uuid> admins = (HashSet<Uuid>) context.getView().retrieveAdmins();
    HashSet<Uuid> newAdmins = (HashSet<Uuid>) context.getView().retrieveNewAdmins();
    setAdmins(admins);
    setNewAdmins(newAdmins);
  }

  public void authenticate(Uuid id) {
    Console console = System.console();
    char passwordArray[] = console.readPassword("Enter your password: ");
    System.out.println("Verifying...");
    try {
      while (!PasswordStorage.verifyPassword(passwordArray, passwords.get(id))) {
        System.out.println("Login failed. Please try again");
        passwordArray = console.readPassword("Enter your password: ");
        System.out.println("Verifying...");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setPassword(Uuid id) {
    Console console = System.console();
    char password[] = console.readPassword("Enter a new password: ");
    char passwordConfirm[] = console.readPassword("Retype your password: ");
    System.out.println("Verifying...");
    while (!Arrays.equals(password, passwordConfirm)) {
      System.out.println("Passwords didn't match. Please try again.");
      password = console.readPassword("Enter a new password: ");
      passwordConfirm = console.readPassword("Retype your password: ");
      System.out.println("Verifying...");
    }
    try {
      String pass = PasswordStorage.createHash(password);
      passwords.put(id, pass);
      noPasswords.remove(id);
      writeAuthInfo(id, pass);
    } catch (PasswordStorage.CannotPerformOperationException e) {
      e.printStackTrace();
    }
  }

  private void writeAuthInfo(Uuid id, String password) {
    controller.writeAuthInfo(id, password);
  }

  public void addPassword(Uuid id, String password) {
    passwords.put(id, password);
  }

  public String getPassword(Uuid id) {
    return passwords.get(id);
  }

  private void setAdmins(HashSet<Uuid> admins) {
    this.admins = admins;
  }

  private void setNewAdmins(HashSet<Uuid> newAdmins) {
    noPasswords = newAdmins;
  }

  public Set<Uuid> getAdmins() {
    return admins;
  }

  public Set<Uuid> getNewAdmins() {
    return noPasswords;
  }

  public void passwordRetrieved(Uuid id) {
    noPasswords.remove(id);
  }
}
