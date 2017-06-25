package codeu.chat.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Interest {

  private Uuid user;

  //sets of my interests
  private HashSet<Uuid> conversationInterests, userInterests;

  //maps from a conversation I'm interested in to the number of messages since the last update.
  private Map<Uuid, Integer> conversationUpdates;

  //maps from a user I'm interested in to the set of conversations they created/added to.
  private Map<Uuid, HashSet<Uuid>> userUpdates;

  public Interest(Uuid user) {
    this.user = user;
    conversationUpdates = new HashMap<>();
    userUpdates = new HashMap<>();
  }

  public void addConversationInterest(Uuid id) {
    conversationInterests.add(id);
    conversationUpdates.put(id, 0);
  }

  public void addUserInterest(Uuid id) {
    userInterests.add(id);
    userUpdates.put(id, new HashSet<>());
  }

  public void increaseMessageCount(Uuid id) {
    conversationUpdates.put(id, conversationUpdates.get(id) + 1);
  }

  public void addConversation(Uuid id) {
    userUpdates.get(user).add(id);
  }


}
