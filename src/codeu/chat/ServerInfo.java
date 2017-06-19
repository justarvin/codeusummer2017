package codeu.chat.common;

import java.io.IOException;
import codeu.chat.util.Time;


public final class ServerInfo {
  public final Time startTime;
  public ServerInfo() throws IOException {
    this.startTime = Time.now();
  }
  public ServerInfo(Time startTime) {
    this.startTime = startTime;
  }
}
