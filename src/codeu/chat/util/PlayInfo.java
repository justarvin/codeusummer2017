package codeu.chat.util;

import codeu.chat.common.ConversationHeader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Info to store information about an instance of a play
 */
public class PlayInfo {

  private static final File PLAYS = new File("plays");
  private Map<String, Uuid> roles;
  private Map<String, String> textToCharacter;
  private List<String> openRoles;
  private Uuid next;
  private Uuid id;
  private ConversationHeader play;
  private String title;
  private String status;
  private Queue<String> lines;
  private int parts;
  private int current_part;

  public static final Serializer<PlayInfo> SERIALIZER = new Serializer<PlayInfo>() {
    @Override
    public void write(OutputStream out, PlayInfo value) throws IOException {
      Serializers.STRING.write(out, value.getTitle());
      Serializers.STRING.write(out, value.getStatus());
    }

    @Override
    public PlayInfo read(InputStream in) throws IOException {
      String title = Serializers.STRING.read(in);
      String status = Serializers.STRING.read(in);
      return new PlayInfo(title, status, false);
    }
  };

  public PlayInfo(String title, String playTitle) {
    this.title = title;

    roles = new HashMap<>();
    textToCharacter = new HashMap<>();
    openRoles = new ArrayList<>();
    current_part = 1;
    lines = new ArrayDeque<>();
    load50Lines(playTitle, current_part);
    loadRoles(playTitle);
  }

  public PlayInfo(String title, String status, boolean b) {
    this.title = title;
    this.status = status;
  }

  public void setConversation(ConversationHeader c) {
    this.play = c;
  }

  public ConversationHeader getPlay() {
    return play;
  }

  public void setRole(Uuid user) {
    if (openRoles.size() != 0 && !roles.keySet().contains(user)) {
      String character = openRoles.remove(0);
      roles.put(character, user);
    }
    if (openRoles.size() == 0) {
      setStatus("closed");
    }
  }

  public String getRole(Uuid user) {
    for (String key : roles.keySet()) {
      if (roles.get(key).equals(user)) {
        return key;
      }
    }
    return null;
  }

  public void setNext(Uuid user) {
    next = user;
  }

  public Uuid getNext() {
    return next;
  }

  public String getTitle() {
    return title;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public void setUuid(Uuid id) {
    this.id = id;
  }

  public Uuid getID() {
    return id;
  }

  public void setTotalParts(int parts) {
    this.parts = parts;
  }

  private void loadRoles(String fileTitle) {
    File characters = new File(PLAYS, fileTitle+"-chars.txt");
    try {
      BufferedReader reader = new BufferedReader(new FileReader(characters));
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println("line:"+line);
        openRoles.add(line);
        String shortened = reader.readLine();
        System.out.println(shortened);
        textToCharacter.put(shortened, line);
      }

    } catch (Exception e) {
      System.out.println("Failed to load characters");
    }
  }

  private void load50Lines(String fileTitle, int part) {
    if (part <= parts) {
      try {
        File part_x = new File(PLAYS, fileTitle + "-" + part + ".txt");
        File temp = new File(PLAYS, "temp.txt");
        temp.createNewFile();
        BufferedReader reader = new BufferedReader(new FileReader(part_x));
        BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
        String line;
        int i = 0;
        while ((line = reader.readLine()) != null && i < 50) {
          lines.add(line);
          i++;
        }
        // file did not have at least 50 lines left, so move to next file
        if (i < 49) {
          current_part++;
        } else {
          //write the rest to a temp file and then rename the file
          while ((line = reader.readLine()) != null) {
            writer.write(line);
            writer.newLine();
          }
        }
        writer.close();
        reader.close();
        part_x.delete();
        temp.renameTo(part_x);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public String parseLine() {
    if (setNextCharacter()) {
      return "";
    } else {
      return lines.poll();
    }
  }

  // return the next line to be said and then find next speaker
  public String speak() {
    String line = lines.poll();
    setNextCharacter();
    return line;
  }

  // returns true if the next line is a user's line
  private boolean setNextCharacter() {
    String nextLine = lines.peek();
    String firstWord = nextLine.substring(0, nextLine.indexOf('.'));
    if (textToCharacter.keySet().contains(firstWord)) {
      String character = textToCharacter.get(firstWord);
      next = roles.get(character);
      return true;
    }
    return false;
  }
}
