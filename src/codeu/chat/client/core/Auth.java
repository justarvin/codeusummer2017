package codeu.chat.client.core;

import codeu.chat.common.BasicView;
import codeu.chat.util.PasswordStorage;
import codeu.chat.util.Uuid;

import java.io.Console;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Auth {

  private BasicView view;
  private Set<Uuid> admins, noPasswords;
  private Map<Uuid, String> passwords;

  public Auth(BasicView view) {
    this.view = view;
    admins = new HashSet<>();
    noPasswords = new HashSet<>();
    passwords = new HashMap<>();
  }

  public boolean isAdmin(Uuid id) {
    return admins.contains(id);
  }

  public void addAdmin(String name) {
    Uuid id = view.getUuid(name);
    admins.add(id);
    noPasswords.add(id);
  }

  public boolean isNewUser(Uuid id) {
    return noPasswords.contains(id);
  }

  public void authenticate(Uuid id) {
    Console console = System.console();
    char passwordArray[] = console.readPassword("Enter your password: ");
    try {
      while (!PasswordStorage.verifyPassword(passwordArray, passwords.get(id))) {
        System.out.println("Login failed. Please try again");
        console.readPassword("Enter your password: ");
      }
    } catch (Exception e) {
      System.out.println("Login failed. Please try again");
      console.readPassword("Enter your password: ");
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
      passwords.put(id, PasswordStorage.createHash(password));
      noPasswords.remove(id);
    } catch (PasswordStorage.CannotPerformOperationException e) {
      e.printStackTrace();
    }
  }
}
