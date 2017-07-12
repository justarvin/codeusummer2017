// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package codeu.chat.server;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.User;
import codeu.chat.util.InterestStore;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.store.Store;
import codeu.chat.util.store.StoreAccessor;

public final class Model {

  public static final Comparator<Uuid> UUID_COMPARE = new Comparator<Uuid>() {

    @Override
    public int compare(Uuid a, Uuid b) {

      if (a == b) { return 0; }

      if (a == null && b != null) { return -1; }

      if (a != null && b == null) { return 1; }

      final int order = Integer.compare(a.id(), b.id());
      return order == 0 ? compare(a.root(), b.root()) : order;
    }
  };

  private static final Comparator<Time> TIME_COMPARE = new Comparator<Time>() {
    @Override
    public int compare(Time a, Time b) {
      return a.compareTo(b);
    }
  };

  private static final Comparator<String> STRING_COMPARE = String.CASE_INSENSITIVE_ORDER;

  private Store<Uuid, User> userById = new Store<>(UUID_COMPARE);
  private Store<Time, User> userByTime = new Store<>(TIME_COMPARE);
  private Store<String, User> userByText = new Store<>(STRING_COMPARE);

  private Store<Uuid, ConversationHeader> conversationById = new Store<>(UUID_COMPARE);
  private Store<Time, ConversationHeader> conversationByTime = new Store<>(TIME_COMPARE);
  private Store<String, ConversationHeader> conversationByText = new Store<>(STRING_COMPARE);

  private Store<Uuid, ConversationPayload> conversationPayloadById = new Store<>(UUID_COMPARE);

  private Store<Uuid, Message> messageById = new Store<>(UUID_COMPARE);
  private Store<Time, Message> messageByTime = new Store<>(TIME_COMPARE);
  private Store<String, Message> messageByText = new Store<>(STRING_COMPARE);


  private Map<Uuid, InterestStore> interestsByID = new HashMap<>();

  //from user/conversation id to the uuid's of the users who care
  private Map<Uuid, Set<Uuid>> watching = new HashMap<>();

  public void add(User user) {
    userById.insert(user.id, user);
    userByTime.insert(user.creation, user);
    userByText.insert(user.name, user);
  }

  public void remove(User user) {
    userById.clear(user.id);
    userByTime.clear(user.creation);
    userByText.clear(user.name);
  }

  public StoreAccessor<Uuid, User> userById() {
    return userById;
  }

  public StoreAccessor<Time, User> userByTime() {
    return userByTime;
  }

  public StoreAccessor<String, User> userByText() {
    return userByText;
  }

  public void add(ConversationHeader conversation) {
    conversationById.insert(conversation.id, conversation);
    conversationByTime.insert(conversation.creation, conversation);
    conversationByText.insert(conversation.title, conversation);
    conversationPayloadById.insert(conversation.id, new ConversationPayload(conversation.id));
  }

  public void remove(ConversationHeader c) {
    conversationById.clear(c.id);
    conversationByTime.clear(c.creation);
    conversationByText.clear(c.title);
    conversationPayloadById.clear(c.id);
  }

  public StoreAccessor<Uuid, ConversationHeader> conversationById() {
    return conversationById;
  }

  public StoreAccessor<Time, ConversationHeader> conversationByTime() {
    return conversationByTime;
  }

  public StoreAccessor<String, ConversationHeader> conversationByText() {
    return conversationByText;
  }

  public StoreAccessor<Uuid, ConversationPayload> conversationPayloadById() {
    return conversationPayloadById;
  }

  public void add(Message message) {
    messageById.insert(message.id, message);
    messageByTime.insert(message.creation, message);
    messageByText.insert(message.content, message);
  }

  public StoreAccessor<Uuid, Message> messageById() {
    return messageById;
  }

  public StoreAccessor<Time, Message> messageByTime() {
    return messageByTime;
  }

  public StoreAccessor<String, Message> messageByText() {
    return messageByText;
  }

  public Map<Uuid, Set<Uuid>> interestedByID() {
    return watching;
  }

  public void addWatch(Uuid interest, Uuid owner) {
    if (!watching.containsKey(interest)) {
      watching.put(interest, new HashSet<>());
    }
    watching.get(interest).add(owner);
  }

  public void removeUserWatch(Uuid interest, Uuid owner) {
    watching.get(interest).remove(owner);
    interestsByID.get(owner).userUpdates().clear(interest);
  }

  public void removeConversationWatch(Uuid interest, Uuid owner) {
    watching.get(interest).remove(owner);
    interestsByID.get(owner).conversationUpdates().put(interest, 0);
  }

  public Map<Uuid, InterestStore> userInterests() {
    return interestsByID;
  }

  public void clearStores() {
    userById = new Store<>(UUID_COMPARE);
    userByTime = new Store<>(TIME_COMPARE);
    userByText = new Store<>(STRING_COMPARE);

    conversationById = new Store<>(UUID_COMPARE);
    conversationByTime = new Store<>(TIME_COMPARE);
    conversationByText = new Store<>(STRING_COMPARE);

    conversationPayloadById = new Store<>(UUID_COMPARE);

    messageById = new Store<>(UUID_COMPARE);
    messageByTime = new Store<>(TIME_COMPARE);
    messageByText = new Store<>(STRING_COMPARE);
  }
}
