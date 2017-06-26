package codeu.chat.util;

import codeu.chat.common.ConversationHeader;
import codeu.chat.server.Model;
import codeu.chat.util.store.Store;
import codeu.chat.util.store.StoreAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Interest {

  //maps from a conversation I'm interested in to the number of messages since the last update.
  private Map<Uuid, Integer> conversationUpdates;

  //maps from a user I'm interested in to the set of conversations they created/added to.
  private Store<Uuid, ConversationHeader> userUpdates;

  public Interest() {
    conversationUpdates = new HashMap<>();
    userUpdates = new Store<>(Model.UUID_COMPARE);
  }

  public void addConversationInterest(Uuid conversation) {
    conversationUpdates.put(conversation, 0);
  }

  public void increaseMessageCount(Uuid conversation) {
    conversationUpdates.put(conversation, conversationUpdates.get(conversation) + 1);
  }

  public void addConversation(Uuid interest, ConversationHeader c) {
    userUpdates.insert(interest, c);
  }

  public Iterable<ConversationHeader> getUserUpdate(Uuid id) {
    Iterable<ConversationHeader> update = userUpdates.at(id);
    if (userUpdates.first(id) == null) {
      update = new ArrayList<>();
    }
    userUpdates.clear(id);
    return update;
  }

  public int getConversationUpdate(Uuid conversation) {
    int messages = conversationUpdates.get(conversation);
    conversationUpdates.put(conversation, 0);
    return messages;
  }

  public StoreAccessor<Uuid, ConversationHeader> userUpdates() {
    return userUpdates;
  }

  public Map<Uuid, Integer> conversationUpdates() {
    return conversationUpdates;
  }
}
