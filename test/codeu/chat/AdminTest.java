package codeu.chat;

import codeu.chat.client.commandline.Chat;
import codeu.chat.client.core.Auth;
import codeu.chat.client.core.Context;
import codeu.chat.client.core.UserContext;
import codeu.chat.common.LinearUuidGenerator;
import codeu.chat.common.User;
import codeu.chat.server.Controller;
import codeu.chat.server.Model;
import codeu.chat.server.View;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Test class for admin accounts
 */
public class AdminTest {

  @Test
  public void testAdminPrivileges() {
    Uuid adminUuid = new Uuid(2);
    User admin = new User(adminUuid, "admin", Time.now());

    Model model = new Model();
    Controller controller = new Controller(Uuid.NULL, model, new Auth(), new File("test"));
    controller.addAdmin(adminUuid);

    // admin adds user
    Chat chat = new Chat(new Context(null));
    chat.getPanels().push(chat.createUserPanel(new UserContext(admin, new View(model), controller)));
    String command = "u-add test";
    boolean handled = false;
    try {
      handled = chat.handleCommand(command);
    } catch (IOException e) {
      e.printStackTrace();
    }
    assertEquals(true, handled);
  }

  @Test
  public void testRegularPrivileges() {
    Uuid regularUuid = new Uuid(1);
    User regular = new User(regularUuid, "regular", Time.now());

    Model model = new Model();
    Controller controller = new Controller(Uuid.NULL, model, new Auth(), new File("test"));

    // admin adds user
    Chat chat = new Chat(new Context(null));
    chat.getPanels().push(chat.createUserPanel(new UserContext(regular, new View(model), controller)));
    String command = "u-add test";
    boolean handled = false;
    try {
      handled = chat.handleCommand(command);
    } catch (IOException e) {
      e.printStackTrace();
    }
    // "ERROR: Unsupported command" was correctly printed
    assertEquals(true, handled);
  }


}
