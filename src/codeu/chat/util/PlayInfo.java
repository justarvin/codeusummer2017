package codeu.chat.util;

import codeu.chat.common.ConversationHeader;

import java.io.*;
import java.util.*;

/**
 * Info to store information about an instance of a play
 */
public class PlayInfo {

  private static final File PLAYS = new File("plays");
  private Map<Uuid, String> roles;
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
    openRoles = new ArrayList<>();
    current_part = 1;
    load50Lines(playTitle, current_part);
    lines = new ArrayDeque<>();
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

  //returns true if there was a role to fill.
  public boolean setRole(Uuid user) {
    if (openRoles.size() == 0) {
      return false;
    } else {
      String character = openRoles.remove(0);
      roles.put(user, character);
      return true;
    }
  }

  public String getRole(Uuid user) {
    return roles.get(user);
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
        openRoles.add(line);
      }

    } catch (Exception e) {
      System.out.println("Failed to load characters");
    }
  }

  private void load50Lines(String fileTitle, int part) {
    if (current_part <= parts) {
      try {
        File part_x = new File(PLAYS, fileTitle + "-" + part + ".txt");
        File temp = new File(PLAYS, "temp.txt");
        BufferedReader reader = new BufferedReader(new FileReader(part_x));
        BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
        String line;
        int i = 0;
        while ((line = reader.readLine()) != null && i < 50) {
          lines.add(line);
          i++;
        }
        // file did not have at least 50 lines left
        if (i < 49) {
          current_part++;
        } else {
          //write the rest to a temp file and then rename as current file
          while ((line = reader.readLine()) != null) {
            writer.write(line);
            writer.newLine();
          }
          temp.renameTo(part_x);
        }
        writer.close();
        reader.close();

      } catch (Exception e) {
        System.out.println("Failed to load roles");
      }
    }
  }
}
