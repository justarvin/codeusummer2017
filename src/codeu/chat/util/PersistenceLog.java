package codeu.chat.util;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.User;
import codeu.chat.server.Controller;
import codeu.chat.server.Server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class for writing and reading to/from the log
 */
public class PersistenceLog {

  private static final String USER = "ADD-USER";
  private static final String MESSAGE = "ADD-MESSAGE";
  private static final String CONVERSATION = "ADD-CONVERSATION";
  private static final String ADMIN = "ADMIN";
  private static final String DELETE_USER = "DELETE-USER";
  private static final String DELETE_CONVERSATION = "DELETE-CONVERSATION";
  private static final String ADD_ADMIN = "ADD-ADMIN";
  private static final String REMOVE_ADMIN = "REMOVE-ADMIN";
  private static final String SPACE = " ";

  public static void writeTransaction(String type, Uuid id, String text, long time, Uuid owner, Uuid convoId) {
    String log = "";
    switch(type) {
      case "user":
        log = USER + SPACE +
                id + SPACE +
                text + SPACE +
                time;
        break;
      case "admin":
        log = USER + SPACE +
                id + SPACE +
                text + SPACE +
                time + SPACE +
                ADMIN;
        break;
      case "message":
        log = MESSAGE + SPACE +
                id + SPACE +
                text + SPACE +
                time + SPACE +
                convoId + SPACE +
                owner;
        break;
      case "conversation":
        log = CONVERSATION + SPACE +
                id + SPACE +
                text + SPACE +
                time + SPACE +
                owner;
        break;
      case "delete-user":
        log = DELETE_USER + SPACE +
                id + SPACE +
                text + SPACE +
                time;
        break;
      case "delete-conversation":
        log = DELETE_USER + SPACE +
                id + SPACE +
                text + SPACE +
                time;
        break;
      case "add-admin":
        log = ADD_ADMIN + SPACE + id;
        break;
      case "remove-admin":
        log = REMOVE_ADMIN + SPACE + id;
        break;
    }
    Server.getLogBuffer().add(log);
  }

  public static void writeAuthInfo(File path, Uuid id, String password) {

    File passwords = new File(path, "passwords.txt");
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(passwords, true));
      writer.write(id + " " + password);
      writer.newLine();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void restore(Controller controller, File path) {
    File log = new File(path, "log.txt");
    try {

      boolean created = log.createNewFile(); // true if file created, false otherwise
      //read in and restore state if the file existed
      if (!created) {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(log));
        while ((line = reader.readLine()) != null) {
          process(controller, line);
        }
        reader.close();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    File passwords = new File(path, "passwords.txt");
    try {
      boolean created = passwords.createNewFile();
      System.out.println(created);
      if (!created) {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(passwords));
        while ((line = reader.readLine()) != null) {
          processPassword(controller, line);
        }
        reader.close();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void process(Controller controller, String line) throws IOException {
    //turns log string into an array of words
    String[] splitLog = line.split(" ");

    //uuid and time occupy the same spot in each format so they have been extracted ahead of time
    Uuid uuid = Uuid.parse(splitLog[1]);
    String text = null;
    long time = 0;
    if (splitLog.length > 2) {
      text = splitLog[2];
      time = Long.parseLong(splitLog[3]);
    }

    switch (splitLog[0]) {
      case USER:
        controller.newUser(uuid, text, Time.fromMs(time));
        try {
          String admin = splitLog[4];
          controller.addAdmin(uuid);
        } catch (ArrayIndexOutOfBoundsException ex) {
          //do nothing, it's a regular user
        }
        break;
      case CONVERSATION:
        Uuid owner = Uuid.parse(splitLog[4]);
        controller.newConversation(uuid, text, owner, Time.fromMs(time));
        break;
      case MESSAGE:
        Uuid conversationUuid = Uuid.parse(splitLog[4]);
        Uuid senderUuid = Uuid.parse(splitLog[5]);
        controller.newMessage(uuid, senderUuid, conversationUuid, text, Time.fromMs(time));
        break;
      case DELETE_USER:
        controller.removeUser(new User(uuid, text, Time.fromMs(time)));
        break;
      case DELETE_CONVERSATION:
        controller.removeConversation(new ConversationHeader(uuid, Uuid.NULL, Time.fromMs(time), text));
        break;
      case ADD_ADMIN:
        controller.addAdmin(uuid);
        break;
      case REMOVE_ADMIN:
        controller.removeAdmin(uuid);
        break;
    }
  }

  private static void processPassword(Controller controller, String line) throws IOException {
    String[] splitLog = line.split(" ");
    Uuid id = Uuid.parse(splitLog[0]);
    String password = splitLog[1];
    controller.addAuthInfo(id, password);
  }

}
