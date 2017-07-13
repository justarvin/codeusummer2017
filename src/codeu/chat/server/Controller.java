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

import codeu.chat.client.core.Auth;
import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.RandomUuidGenerator;
import codeu.chat.common.RawController;
import codeu.chat.common.User;
import codeu.chat.util.InterestStore;
import codeu.chat.util.Logger;
import codeu.chat.util.PersistenceLog;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;

public final class Controller implements RawController, BasicController {

  private final static Logger.Log LOG = Logger.newLog(Controller.class);

  private final Model model;
  private final Uuid.Generator uuidGenerator;
  private final File persistentPath;
  private final Auth auth;

  public Controller(Uuid serverId, Model model, Auth auth, File persistentPath) {
    this.model = model;
    this.uuidGenerator = new RandomUuidGenerator(serverId, System.currentTimeMillis());
    this.auth = auth;
    this.persistentPath = persistentPath;

    PersistenceLog.restore(this, persistentPath);
  }

  @Override
  public Message newMessage(Uuid author, Uuid conversation, String body) {
    Uuid id = createId();
    Time time = Time.now();

    Object[] params = new Object[]{id, body, time.inMs(), conversation, author};
    checkBuffer();
    PersistenceLog.writeTransaction("message", params);

    return newMessage(id, author, conversation, body, time);
  }

  @Override
  public User newUser(String name) {
    Uuid id = createId();
    Time time = Time.now();

    Object[] params = new Object[]{id, name, time.inMs()};
    checkBuffer();
    if (!model.userById().all().iterator().hasNext()) {
      auth.addFirstAdmin(id);
    }
    if (auth.isAdmin(id)) {
      PersistenceLog.writeTransaction("admin", params);
    } else {
      PersistenceLog.writeTransaction("user", params);
    }

    return newUser(id, name, time);
  }

  @Override
  public ConversationHeader newConversation(String title, Uuid owner) {
    Uuid id = createId();
    Time time = Time.now();

    Object[] params = new Object[]{id, title, time.inMs(), owner};
    checkBuffer();
    PersistenceLog.writeTransaction("conversation", params);

    return newConversation(id, title, owner, time);
  }

  @Override
  public void newUserInterest(String name, Uuid owner) {

    User userInterest = model.userByText().first(name);
    model.addWatch(userInterest.id, owner);

    LOG.info("InterestStore added");
  }

  @Override
  public void newConversationInterest(String title, Uuid owner) {

    ConversationHeader conversationInterest = model.conversationByText().first(title);

    InterestStore myInterests = model.userInterests().get(owner);
    myInterests.addConversationInterest(conversationInterest.id);

    // save owner as someone interested in conversationInterest
    model.addWatch(conversationInterest.id, owner);

    LOG.info("Conversation interest added");
  }

  @Override
  public void removeUserInterest(String name, Uuid owner) {
    User userInterest = model.userByText().first(name);
    model.removeUserWatch(userInterest.id, owner);
  }

  @Override
  public void removeConversationInterest(String title, Uuid owner) {
    ConversationHeader conversationInterest = model.conversationByText().first(title);
    model.removeConversationWatch(conversationInterest.id, owner);
  }

  @Override
  public Message newMessage(Uuid id, Uuid author, Uuid conversation, String body, Time creationTime) {

    final User foundUser = model.userById().first(author);
    final ConversationPayload foundConversation = model.conversationPayloadById().first(conversation);

    Message message = null;

    if (foundUser != null && foundConversation != null && isIdFree(id)) {

      message = new Message(id, Uuid.NULL, Uuid.NULL, creationTime, author, body);
      model.add(message);
      updateMessageCounts(conversation);

      //keeping track of conversations that this owner, who may be an interest, added to
      if (model.interestedByID().containsKey(author)) {
        for (Uuid user : model.interestedByID().get(author)) {
          ConversationHeader c = model.conversationById().first(conversation);
          model.userInterests().get(user).addConversation(author, c);
        }
      }

      LOG.info("Message added: %s", message.id);

      // Find and update the previous "last" message so that it's "next" value
      // will point to the new message.

      if (Uuid.equals(foundConversation.lastMessage, Uuid.NULL)) {

        // The conversation has no messages in it, that's why the last message is NULL (the first
        // message should be NULL too. Since there is no last message, then it is not possible
        // to update the last message's "next" value.

      } else {
        final Message lastMessage = model.messageById().first(foundConversation.lastMessage);
        lastMessage.next = message.id;
      }

      // If the first message points to NULL it means that the conversation was empty and that
      // the first message should be set to the new message. Otherwise the message should
      // not change.

      foundConversation.firstMessage =
              Uuid.equals(foundConversation.firstMessage, Uuid.NULL) ?
                      message.id :
                      foundConversation.firstMessage;

      // Update the conversation to point to the new last message as it has changed.

      foundConversation.lastMessage = message.id;
    }

    return message;
  }

  @Override
  public User newUser(Uuid id, String name, Time creationTime) {

    User user = null;

    if (isIdFree(id)) {

      user = new User(id, name, creationTime);
      model.add(user);
      model.userInterests().put(id, new InterestStore());

      LOG.info(
              "newUser success (user.id=%s user.name=%s user.time=%s)",
              id,
              name,
              creationTime);

    } else {

      LOG.info(
              "newUser fail - id in use (user.id=%s user.name=%s user.time=%s)",
              id,
              name,
              creationTime);
    }

    return user;
  }

  @Override
  public ConversationHeader newConversation(Uuid id, String title, Uuid owner, Time creationTime) {

    final User foundOwner = model.userById().first(owner);

    ConversationHeader conversation = null;

    if (foundOwner != null && isIdFree(id)) {
      conversation = new ConversationHeader(id, owner, creationTime, title);
      model.add(conversation);
      updateConversations(id, owner);

      LOG.info("Conversation added: " + id);
    }

    return conversation;
  }

  public void addAuthInfo(Uuid id, String password) {
    auth.addPassword(id, password);
    auth.passwordRetrieved(id);
  }

  public void addAdmin(Uuid id) {
    auth.addAdmin(id);
  }

  private Uuid createId() {

    Uuid candidate;

    for (candidate = uuidGenerator.make();
         isIdInUse(candidate);
         candidate = uuidGenerator.make()) {

      // Assuming that "randomUuid" is actually well implemented, this
      // loop should never be needed, but just incase make sure that the
      // Uuid is not actually in use before returning it.

    }

    return candidate;
  }

  private boolean isIdInUse(Uuid id) {
    return model.messageById().first(id) != null ||
            model.conversationById().first(id) != null ||
            model.userById().first(id) != null;
  }

  private boolean isIdFree(Uuid id) {
    return !isIdInUse(id);
  }

  public void checkBuffer() {

    File log = new File(persistentPath, "log.txt");
    Queue<String> logBuffer = Server.getLogBuffer();

    if (logBuffer.size() == 15) {
      try {

        BufferedWriter writer = new BufferedWriter(new FileWriter(log));
        for (int i = 0; i < 15; i++) {
          writer.write(logBuffer.remove());
          writer.newLine();
        }
        writer.close();

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void updateConversations(Uuid interest, Uuid owner) {
    if (model.interestedByID().containsKey(owner)) {
      for (Uuid u : model.interestedByID().get(owner)) {
        ConversationHeader conversation = model.conversationById().first(interest);
        model.userInterests().get(u).addConversation(owner, conversation);
      }
    }
  }

  public void updateMessageCounts(Uuid conversation) {
    if (model.interestedByID().containsKey(conversation)) {
      for (Uuid user : model.interestedByID().get(conversation)) {
        InterestStore myInterests = model.userInterests().get(user);
        myInterests.increaseMessageCount(conversation);
      }
    }
  }

  public void writeAuthInfo(Uuid id, String password) {
    PersistenceLog.writeAuthInfo(persistentPath, id, password);
  }
}
