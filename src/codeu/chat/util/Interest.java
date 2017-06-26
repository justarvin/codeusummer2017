package codeu.chat.util;

import codeu.chat.common.ConversationHeader;
import codeu.chat.server.Controller;
import codeu.chat.server.Model;
import codeu.chat.util.store.Store;
import codeu.chat.util.store.StoreAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Interest {

  private final static Logger.Log LOG = Logger.newLog(Interest.class);

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
    LOG.info(conversationUpdates.get(conversation)+"");
  }

  public void addConversation(Uuid interest, ConversationHeader c) {
    userUpdates.insert(interest, c);
    for (ConversationHeader conv : userUpdates.at(interest)) {
      LOG.info(conv.title);
    }
  }

  public Iterable<ConversationHeader> getUserUpdate(Uuid id) {
    Iterable<ConversationHeader> update = userUpdates.at(id);
    if (userUpdates.first(id) == null) {
      update = new ArrayList<>();
    }
    userUpdates.clear(id);
    LOG.info("first: "+userUpdates.first(id));
    return update;
  }

  public int getConversationUpdate(Uuid conversation) {
    int messages = conversationUpdates.get(conversation);
    conversationUpdates.put(conversation, 0);
    return messages;
  }

  public StoreAccessor<Uuid, ConversationHeader> getUpdates() {
    return userUpdates;
  }
}
