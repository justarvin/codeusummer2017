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

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.Relay;
import codeu.chat.common.Secret;
import codeu.chat.common.ServerInfo;
import codeu.chat.common.User;
import codeu.chat.util.*;
import codeu.chat.util.connections.Connection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public final class Server {

  private interface Command {
    void onMessage(InputStream in, OutputStream out) throws IOException;
  }

  private static final Logger.Log LOG = Logger.newLog(Server.class);

  private static final int RELAY_REFRESH_MS = 5000;  // 5 seconds

  private final Timeline timeline = new Timeline();

  private final Map<Integer, Command> commands = new HashMap<>();

  private final Uuid id;
  private final Secret secret;

  private final Model model = new Model();
  private final View view = new View(model);
  private final Controller controller;

  private final Relay relay;
  private Uuid lastSeen = Uuid.NULL;

  private static Queue<String> logBuffer = new ArrayDeque<>();

  public Server(final Uuid id, final Secret secret, final Relay relay, File persistentPath) {

    this.id = id;
    this.secret = secret;
    this.controller = new Controller(id, model, persistentPath);
    this.relay = relay;

    if (!model.userById().all().iterator().hasNext()) {
      User user = controller.newUser("admin");
      model.addAdmin(user.id);
    }

    //Request info version - user asks server for current info version
    this.commands.put(NetworkCode.SERVER_INFO_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        Serializers.INTEGER.write(out, NetworkCode.SERVER_INFO_RESPONSE);
        final ServerInfo serverInfo = view.getInfo();
        Uuid.SERIALIZER.write(out, serverInfo.getVersion());
        Time.SERIALIZER.write(out, serverInfo.getStartTime());
      }
    });

    // New Message - A client wants to add a new message to the back end.
    this.commands.put(NetworkCode.NEW_MESSAGE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Uuid author = Uuid.SERIALIZER.read(in);
        final Uuid conversation = Uuid.SERIALIZER.read(in);
        final String content = Serializers.STRING.read(in);

        final Message message = controller.newMessage(author, conversation, content);

        Serializers.INTEGER.write(out, NetworkCode.NEW_MESSAGE_RESPONSE);
        Serializers.nullable(Message.SERIALIZER).write(out, message);

        timeline.scheduleNow(createSendToRelayEvent(
                author,
                conversation,
                message.id));
      }
    });

    // New User - A client wants to add a new user to the back end.
    this.commands.put(NetworkCode.NEW_USER_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String name = Serializers.STRING.read(in);

        final User user = controller.newUser(name);

        Serializers.INTEGER.write(out, NetworkCode.NEW_USER_RESPONSE);
        Serializers.nullable(User.SERIALIZER).write(out, user);
      }
    });

    // New Conversation - A client wants to add a new conversation to the back end.
    this.commands.put(NetworkCode.NEW_CONVERSATION_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String title = Serializers.STRING.read(in);
        final Uuid owner = Uuid.SERIALIZER.read(in);

        final ConversationHeader conversation = controller.newConversation(title, owner);

        Serializers.INTEGER.write(out, NetworkCode.NEW_CONVERSATION_RESPONSE);
        Serializers.nullable(ConversationHeader.SERIALIZER).write(out, conversation);
      }
    });

    // Get Users - A client wants to get all the users from the back end.
    this.commands.put(NetworkCode.GET_USERS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<User> users = view.getUsers();

        Serializers.INTEGER.write(out, NetworkCode.GET_USERS_RESPONSE);
        Serializers.collection(User.SERIALIZER).write(out, users);
      }
    });

    // Get Conversations - A client wants to get all the conversations from the back end.
    this.commands.put(NetworkCode.GET_ALL_CONVERSATIONS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<ConversationHeader> conversations = view.getConversations();

        Serializers.INTEGER.write(out, NetworkCode.GET_ALL_CONVERSATIONS_RESPONSE);
        Serializers.collection(ConversationHeader.SERIALIZER).write(out, conversations);
      }
    });

    // Get Conversations By Id - A client wants to get a subset of the converations from
    //                           the back end. Normally this will be done after calling
    //                           Get Conversations to get all the headers and now the client
    //                           wants to get a subset of the payloads.
    this.commands.put(NetworkCode.GET_CONVERSATIONS_BY_ID_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<Uuid> ids = Serializers.collection(Uuid.SERIALIZER).read(in);
        final Collection<ConversationPayload> conversations = view.getConversationPayloads(ids);

        Serializers.INTEGER.write(out, NetworkCode.GET_CONVERSATIONS_BY_ID_RESPONSE);
        Serializers.collection(ConversationPayload.SERIALIZER).write(out, conversations);
      }
    });

    // Get Messages By Id - A client wants to get a subset of the messages from the back end.
    this.commands.put(NetworkCode.GET_MESSAGES_BY_ID_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<Uuid> ids = Serializers.collection(Uuid.SERIALIZER).read(in);
        final Collection<Message> messages = view.getMessages(ids);

        Serializers.INTEGER.write(out, NetworkCode.GET_MESSAGES_BY_ID_RESPONSE);
        Serializers.collection(Message.SERIALIZER).write(out, messages);
      }
    });

    // Clean - A client wants to clean the log
    this.commands.put(NetworkCode.CLEAN_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        controller.clean(persistentPath);

        Serializers.INTEGER.write(out, NetworkCode.CLEAN_RESPONSE);
      }
    });

    // Write to file - Write remaining contents of queue to file when client exits chat
    this.commands.put(NetworkCode.WRITE_REST_OF_QUEUE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        try {

          File log = new File(persistentPath, "log.txt");
          BufferedWriter writer = new BufferedWriter(new FileWriter(log, true));
          while (!logBuffer.isEmpty()) {
            writer.write(logBuffer.remove());
            writer.newLine();
          }
          writer.close();

        } catch (IOException e) {
          e.printStackTrace();
        }

        Serializers.INTEGER.write(out, NetworkCode.WRITE_REST_OF_QUEUE_RESPONSE);

      }
    });

    // Add user interest - A client wants to add a user interest
    this.commands.put(NetworkCode.NEW_USER_INTEREST_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String name = Serializers.STRING.read(in);
        final Uuid owner = Uuid.SERIALIZER.read(in);
        controller.newUserInterest(name, owner);

        Serializers.INTEGER.write(out, NetworkCode.NEW_USER_INTEREST_RESPONSE);
      }
    });

    // Add conversation interest - A client wants to add a conversation interest
    this.commands.put(NetworkCode.NEW_CONVERSATION_INTEREST_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String title = Serializers.STRING.read(in);
        final Uuid owner = Uuid.SERIALIZER.read(in);
        controller.newConversationInterest(title, owner);

        Serializers.INTEGER.write(out, NetworkCode.NEW_CONVERSATION_INTEREST_RESPONSE);
      }
    });

    // Remove user interest - A client wants to remove a user interest
    this.commands.put(NetworkCode.REMOVE_USER_INTEREST_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String name = Serializers.STRING.read(in);
        final Uuid owner = Uuid.SERIALIZER.read(in);
        controller.removeUserInterest(name, owner);

        Serializers.INTEGER.write(out, NetworkCode.REMOVE_USER_INTEREST_RESPONSE);
      }
    });

    // Remove conversation interest - A client wants to remove a conversation interest
    this.commands.put(NetworkCode.REMOVE_CONVERSATION_INTEREST_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String title = Serializers.STRING.read(in);
        final Uuid owner = Uuid.SERIALIZER.read(in);
        controller.removeConversationInterest(title, owner);

        Serializers.INTEGER.write(out, NetworkCode.REMOVE_CONVERSATION_INTEREST_RESPONSE);
      }
    });


    // Get user update -- a client wants to get the status update for a user
    this.commands.put(NetworkCode.USER_UPDATE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Uuid owner = Uuid.SERIALIZER.read(in);
        final String name = Serializers.STRING.read(in);
        Collection<ConversationHeader> conversations = view.getUserUpdate(owner, name);
        LOG.info(conversations.size() + "");

        Serializers.INTEGER.write(out, NetworkCode.USER_UPDATE_RESPONSE);
        Serializers.collection(ConversationHeader.SERIALIZER).write(out, conversations);
      }
    });

    // Get conversation update -- a client wants to get the conversation update for a user
    this.commands.put(NetworkCode.CONVERSATION_UPDATE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Uuid owner = Uuid.SERIALIZER.read(in);
        final String name = Serializers.STRING.read(in);
        int messages = view.getConversationUpdate(owner, name);

        Serializers.INTEGER.write(out, NetworkCode.CONVERSATION_UPDATE_RESPONSE);
        Serializers.INTEGER.write(out, messages);

      }
    });

    // Delete user -- an admin wants to delete the specified user
    this.commands.put(NetworkCode.DELETE_USER_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final User user = User.SERIALIZER.read(in);
        controller.removeUser(user);
        for (User u : model.userById().all()) {
          System.out.println(u.id);
        }

        Serializers.INTEGER.write(out, NetworkCode.DELETE_USER_RESPONSE);
      }
    });

    //Delete conversation -- an admin wants to delete the specified conversation
    this.commands.put(NetworkCode.DELETE_CONVERSATION_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final ConversationHeader c = ConversationHeader.SERIALIZER.read(in);
        controller.removeConversation(c);

        Serializers.INTEGER.write(out, NetworkCode.DELETE_CONVERSATION_RESPONSE);
      }
    });

    //Admin info -- get the password for the specified user id
    this.commands.put(NetworkCode.AUTH_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid id = Uuid.SERIALIZER.read(in);
        final String password = Serializers.STRING.read(in);
        String correct = view.getPassword(id);
        boolean success;
        try {
          success = PasswordUtils.verifyPassword(password, correct);
        } catch (PasswordUtils.CannotPerformOperationException | PasswordUtils.InvalidHashException e) {
          success = false;
        }

        Serializers.INTEGER.write(out, NetworkCode.AUTH_RESPONSE);
        Serializers.BOOLEAN.write(out, success);
      }
    });

    //Get admins -- get the list of admins
    this.commands.put(NetworkCode.GET_ADMINS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        Serializers.INTEGER.write(out, NetworkCode.GET_ADMINS_RESPONSE);
        Serializers.collection(Uuid.SERIALIZER).write(out, view.getAdmins());
      }
    });

    //Get new admins -- get the list of admins who have not set their passwords yet
    this.commands.put(NetworkCode.GET_NEW_ADMINS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        Serializers.INTEGER.write(out, NetworkCode.GET_NEW_ADMINS_RESPONSE);
        Serializers.collection(Uuid.SERIALIZER).write(out, view.getNewAdmins());
      }
    });

    //Write auth info -- write the information to disk
    this.commands.put(NetworkCode.SET_PASSWORD_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid id = Uuid.SERIALIZER.read(in);
        final String password = Serializers.STRING.read(in);
        boolean success = controller.setPassword(id, password);
        Serializers.INTEGER.write(out, NetworkCode.SET_PASSWORD_RESPONSE);
        Serializers.BOOLEAN.write(out, success);
      }
    });

    this.commands.put(NetworkCode.ADD_ADMIN_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final String name = Serializers.STRING.read(in);
        final boolean log = Serializers.BOOLEAN.read(in);
        controller.addAdmin(name, log);
        Serializers.INTEGER.write(out, NetworkCode.ADD_ADMIN_RESPONSE);
      }
    });

    this.commands.put(NetworkCode.REMOVE_ADMIN_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final String name = Serializers.STRING.read(in);
        controller.removeAdmin(name);
        Serializers.INTEGER.write(out, NetworkCode.REMOVE_ADMIN_RESPONSE);
      }
    });

    this.commands.put(NetworkCode.ADD_MEMBER_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid conversationId = Uuid.SERIALIZER.read(in);
        final String userName = Serializers.STRING.read(in);
        boolean success = controller.addMember(conversationId, userName);
        Serializers.INTEGER.write(out, NetworkCode.ADD_MEMBER_RESPONSE);
        Serializers.BOOLEAN.write(out, success);
      }
    });

    this.commands.put(NetworkCode.REMOVE_MEMBER_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid conversationId = Uuid.SERIALIZER.read(in);
        final String userName = Serializers.STRING.read(in);
        boolean success = controller.removeMember(conversationId, userName);
        Serializers.INTEGER.write(out, NetworkCode.REMOVE_MEMBER_RESPONSE);
        Serializers.BOOLEAN.write(out, success);
      }
    });

    this.commands.put(NetworkCode.CHECK_MEMBER_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid conversationId = Uuid.SERIALIZER.read(in);
        final Uuid memberId = Uuid.SERIALIZER.read(in);
        boolean isUserMember = view.isUserMember(conversationId, memberId);
        Serializers.INTEGER.write(out, NetworkCode.CHECK_MEMBER_RESPONSE);
        Serializers.BOOLEAN.write(out, isUserMember);
      }
    });

    this.commands.put(NetworkCode.CHECK_OWNER_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid conversationId = Uuid.SERIALIZER.read(in);
        final Uuid memberId = Uuid.SERIALIZER.read(in);
        boolean isUserOwner = view.isUserOwner(conversationId, memberId);
        Serializers.INTEGER.write(out, NetworkCode.CHECK_OWNER_RESPONSE);
        Serializers.BOOLEAN.write(out, isUserOwner);
      }
    });

    this.commands.put(NetworkCode.ADD_OWNER_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid conversationId = Uuid.SERIALIZER.read(in);
        final String userName = Serializers.STRING.read(in);
        boolean success = controller.addOwner(conversationId, userName);
        Serializers.INTEGER.write(out, NetworkCode.ADD_OWNER_RESPONSE);
        Serializers.BOOLEAN.write(out, success);
      }
    });

    this.commands.put(NetworkCode.REMOVE_OWNER_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid conversationId = Uuid.SERIALIZER.read(in);
        final String userName = Serializers.STRING.read(in);
        boolean success = controller.removeOwner(conversationId, userName);
        Serializers.INTEGER.write(out, NetworkCode.REMOVE_OWNER_RESPONSE);
        Serializers.BOOLEAN.write(out, success);
      }
    });

    this.commands.put(NetworkCode.CHECK_CREATOR_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid conversationId = Uuid.SERIALIZER.read(in);
        final Uuid memberId = Uuid.SERIALIZER.read(in);
        boolean isUserCreator = view.isUserCreator(conversationId, memberId);
        Serializers.INTEGER.write(out, NetworkCode.CHECK_CREATOR_RESPONSE);
        Serializers.BOOLEAN.write(out, isUserCreator);
      }
    });

    this.timeline.scheduleNow(new Runnable() {
      @Override
      public void run() {
        try {

          LOG.info("Reading update from relay...");

          for (final Relay.Bundle bundle : relay.read(id, secret, lastSeen, 32)) {
            onBundle(bundle);
            lastSeen = bundle.id();
          }

        } catch (Exception ex) {

          LOG.error(ex, "Failed to read update from relay.");

        }

        timeline.scheduleIn(RELAY_REFRESH_MS, this);
      }
    });
  }


  public void handleConnection(final Connection connection) {
    timeline.scheduleNow(new Runnable() {
      @Override
      public void run() {
        try {

          LOG.info("Handling connection...");

          final int type = Serializers.INTEGER.read(connection.in());
          final Command command = commands.get(type);

          if (command == null) {
            // The message type cannot be handled so return a dummy message.
            Serializers.INTEGER.write(connection.out(), NetworkCode.NO_MESSAGE);
            LOG.info("Connection rejected");
          } else {
            command.onMessage(connection.in(), connection.out());
            LOG.info("Connection accepted");
          }

        } catch (Exception ex) {

          LOG.error(ex, "Exception while handling connection.");

        }

        try {
          connection.close();
        } catch (Exception ex) {
          LOG.error(ex, "Exception while closing connection.");
        }
      }
    });
  }

  private void onBundle(Relay.Bundle bundle) {

    final Relay.Bundle.Component relayUser = bundle.user();
    final Relay.Bundle.Component relayConversation = bundle.conversation();
    final Relay.Bundle.Component relayMessage = bundle.user();

    User user = model.userById().first(relayUser.id());

    if (user == null) {
      user = controller.newUser(relayUser.id(), relayUser.text(), relayUser.time());
    }

    ConversationHeader conversation = model.conversationById().first(relayConversation.id());

    if (conversation == null) {

      // As the relay does not tell us who made the conversation - the first person who
      // has a message in the conversation will get ownership over this server's copy
      // of the conversation.
      conversation = controller.newConversation(relayConversation.id(),
              relayConversation.text(),
              user.id,
              relayConversation.time());
    }

    Message message = model.messageById().first(relayMessage.id());

    if (message == null) {
      message = controller.newMessage(relayMessage.id(),
              user.id,
              conversation.id,
              relayMessage.text(),
              relayMessage.time());
    }
  }

  private Runnable createSendToRelayEvent(final Uuid userId,
                                          final Uuid conversationId,
                                          final Uuid messageId) {
    return new Runnable() {
      @Override
      public void run() {
        final User user = view.findUser(userId);
        final ConversationHeader conversation = view.findConversation(conversationId);
        final Message message = view.findMessage(messageId);
        relay.write(id,
                secret,
                relay.pack(user.id, user.name, user.creation),
                relay.pack(conversation.id, conversation.title, conversation.creation),
                relay.pack(message.id, message.content, message.creation));
      }
    };
  }

  public static Queue<String> getLogBuffer() {
    return logBuffer;
  }
}