package codeu.chat.common;

import java.io.IOException;
import codeu.chat.util.Uuid;
import codeu.chat.util.Time;

public final class ServerInfo {
  private final Time startTime;
  private final Uuid version;
  private final static String SERVER_VERSION = "1.0.0";
  
  public ServerInfo() {
    this.startTime = Time.now();
    Uuid serverVersion = Uuid.NULL;
    try {
      serverVersion = Uuid.parse(SERVER_VERSION);
    } catch (IOException e) {
      // Do nothing.
    }
    this.version = serverVersion;
  }
  
  public ServerInfo(Uuid version, Time startTime) {
    this.version = version;
    this.startTime = startTime;
  }
  
  public Uuid getVersion() {
    return version;
  }
  
  public Time getStartTime() {
    return startTime;
  }
}
