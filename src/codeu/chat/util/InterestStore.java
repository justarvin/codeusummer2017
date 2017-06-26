package codeu.chat.util;

import codeu.chat.common.ConversationHeader;
import codeu.chat.server.Model;
import codeu.chat.util.store.Store;
import codeu.chat.util.store.StoreAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InterestStore {

  //maps from a conversation I'm interested in to the number of messages since the last update.
  private Map<Uuid, Integer> conversationInterests;

  //maps from a user I'm interested in to the set of conversations they created/added to.
  private Store<Uuid, ConversationHeader> userInterests;

  public InterestStore() {
    conversationInterests = new HashMap<>();
    userInterests = new Store<>(Model.UUID_COMPARE);
  }

  public void addConversationInterest(Uuid conversation) {
    conversationInterests.put(conversation, 0);
  }

  public void increaseMessageCount(Uuid conversation) {
    conversationInterests.put(conversation, conversationInterests.get(conversation) + 1);
  }

  public void addConversation(Uuid interest, ConversationHeader c) {
    userInterests.insert(interest, c);
  }

  public Iterable<ConversationHeader> getUserUpdate(Uuid id) {
    Iterable<ConversationHeader> update = userInterests.at(id);
    if (userInterests.first(id) == null) {
      update = new ArrayList<>();
    }
    userInterests.clear(id);
    return update;
  }

  public int getConversationUpdate(Uuid conversation) {
    int messages = conversationInterests.get(conversation);
    conversationInterests.put(conversation, 0);
    return messages;
  }

  public StoreAccessor<Uuid, ConversationHeader> userUpdates() {
    return userInterests;
  }

  public Map<Uuid, Integer> conversationUpdates() {
    return conversationInterests;
  }
}
