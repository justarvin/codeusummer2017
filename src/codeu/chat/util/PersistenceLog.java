package codeu.chat.util;

import codeu.chat.client.core.Auth;
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
  private static final String SPACE = " ";
  private static final Logger.Log LOG = Logger.newLog(Auth.class);

  public static void writeTransaction(String type, Object[] params) {
    String log = "";
    switch(type) {
      case "user":
        log = USER + SPACE +
                params[0] + SPACE +
                params[1] + SPACE +
                params[2];
        break;
      case "admin":
        log = USER + SPACE +
                params[0] + SPACE +
                params[1] + SPACE +
                params[2] + SPACE +
                ADMIN;
        break;
      case "message":
        log = MESSAGE + SPACE +
                params[0] + SPACE +
                params[1] + SPACE +
                params[2] + SPACE +
                params[3] + SPACE +
                params[4];
        break;
      case "conversation":
        log = CONVERSATION + SPACE +
                params[0] + SPACE +
                params[1] + SPACE +
                params[2] + SPACE +
                params[3];
        break;
    }
    Server.getLogBuffer().add(log);
  }

  public static void writeAuthInfo(File path, Uuid id, String password) {

    File passwords = new File(path, "passwords.txt");
    System.out.println(path);
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(passwords));
      writer.append(id + " " + password);
      writer.newLine();
      writer.close();
      System.out.println("written");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void restore(Controller controller, File path) {
    File log = new File(path, "log.txt");
    System.out.println(path);
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
    System.out.println(path);
    System.out.println("restoring");
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

  public static void restoreAuthInfo(Controller controller) {

  }

  private static void process(Controller controller, String line) throws IOException {
    //turns log string into an array of words
    String[] splitLog = line.split(" ");

    //uuid and time occupy the same spot in each format so they have been extracted ahead of time
    Uuid uuid = Uuid.parse(splitLog[1]);
    long time = Long.parseLong(splitLog[3]);

    switch (splitLog[0]) {
      case USER:
        String name = splitLog[2];
        controller.newUser(uuid, name, Time.fromMs(time));
        try {
          String admin = splitLog[4];
          controller.addAdmin(uuid);
        } catch (ArrayIndexOutOfBoundsException ex) {
          //do nothing
        }
        break;
      case CONVERSATION:
        String title = splitLog[2];
        Uuid owner = Uuid.parse(splitLog[4]);
        controller.newConversation(uuid, title, owner, Time.fromMs(time));
        break;
      case MESSAGE:
        String message = splitLog[2];
        Uuid conversationUuid = Uuid.parse(splitLog[4]);
        Uuid senderUuid = Uuid.parse(splitLog[5]);
        controller.newMessage(uuid, senderUuid, conversationUuid, message, Time.fromMs(time));
        break;
    }
  }

  private static void processPassword(Controller controller, String line) throws IOException {
    System.out.println("processing");
    String[] splitLog = line.split(" ");
    Uuid id = Uuid.parse(splitLog[0]);
    String password = splitLog[1];
    controller.addAuthInfo(id, password);
  }

}
