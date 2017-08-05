package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
import codeu.chat.util.Uuid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Info to store information about an instance of a play
 */
public class PlayInfo {

  private Map<String, Uuid> roles;
  private List<String> openRoles;
  private Uuid next;
  private String title;
  private String status;
  private Queue<String> lines;

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
      return new PlayInfo(title, status);
    }
  };

  public PlayInfo(String title, List<String> openRoles) {
    roles = new HashMap<>();
    this.title = title;
    this.openRoles = openRoles;
    lines = new ArrayDeque<>();
    load50Lines();
  }

  public PlayInfo(String title, String status) {
    this.title = title;
    this.status = status;
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

  public String getTitle() {
    return title;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public void load50Lines() {

  }
}
