package codeu.chat.common;

import java.io.IOException;
import codeu.chat.util.Uuid;
import codeu.chat.util.Time;

public final class ServerInfo {
  private final Time startTime;
  private final Uuid version;
  private final static String SERVER_VERSION = "1.0.0";
  
  public ServerInfo() throws IOException {
    this.startTime = Time.now();
    this.version = Uuid.parse(SERVER_VERSION);
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
