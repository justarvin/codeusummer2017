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

package codeu.chat.client.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.User;
import codeu.chat.common.ServerInfo;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.Connection;
import codeu.chat.util.connections.ConnectionSource;

// VIEW
//
// This is the view component of the Model-View-Controller pattern used by the
// the client to reterive readonly data from the server. All methods are blocking
// calls.
final class View implements BasicView {

  private final static Logger.Log LOG = Logger.newLog(View.class);

  private final ConnectionSource source;

  public View(ConnectionSource source) {
    this.source = source;
  }

  public ServerInfo getInfo() {
    try (final Connection connection = this.source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.SERVER_INFO_REQUEST);
      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.SERVER_INFO_RESPONSE) {
        final Uuid version = Uuid.SERIALIZER.read(connection.in());
        final Time startTime = Time.SERIALIZER.read(connection.in());
        return new ServerInfo(version, startTime);
      } else {
        LOG.error("Unexpected Server Response.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception while connecting. Check log for details.");
      LOG.error(ex, "Connection error occurred");
    }
    // If we get here it means something went wrong and null should be returned
    return null;
  }

  @Override
  public Collection<User> getUsers() {

    final Collection<User> users = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_USERS_REQUEST);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_USERS_RESPONSE) {
        users.addAll(Serializers.collection(User.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }

    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return users;
  }

  @Override
  public Collection<ConversationHeader> getConversations() {

    final Collection<ConversationHeader> summaries = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_ALL_CONVERSATIONS_REQUEST);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_ALL_CONVERSATIONS_RESPONSE) {
        summaries.addAll(Serializers.collection(ConversationHeader.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }

    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return summaries;
  }

  @Override
  public Collection<ConversationPayload> getConversationPayloads(Collection<Uuid> ids) {

    final Collection<ConversationPayload> conversations = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_CONVERSATIONS_BY_ID_REQUEST);
      Serializers.collection(Uuid.SERIALIZER).write(connection.out(), ids);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_CONVERSATIONS_BY_ID_RESPONSE) {
        conversations.addAll(Serializers.collection(ConversationPayload.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return conversations;
  }

  @Override
  public Collection<Message> getMessages(Collection<Uuid> ids) {

    final Collection<Message> messages = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_MESSAGES_BY_ID_REQUEST);
      Serializers.collection(Uuid.SERIALIZER).write(connection.out(), ids);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_MESSAGES_BY_ID_RESPONSE) {
        messages.addAll(Serializers.collection(Message.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return messages;
  }

  @Override
  public Collection<ConversationHeader> getUserUpdate(Uuid owner, String name) {
    final Collection<ConversationHeader> conversations = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.USER_UPDATE_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), owner);
      Serializers.STRING.write(connection.out(), name);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.USER_UPDATE_RESPONSE) {
        conversations.addAll(Serializers.collection(ConversationHeader.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }

    } catch (Exception e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }

    return conversations;
  }

  @Override
  public int getConversationUpdate(Uuid owner, String title) {

    int messages = 0;
    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.CONVERSATION_UPDATE_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), owner);
      Serializers.STRING.write(connection.out(), title);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.CONVERSATION_UPDATE_RESPONSE) {
        messages = Serializers.INTEGER.read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }

    } catch (Exception e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }

    return messages;
  }

  @Override
  public String getAuthInfo(Uuid id) {
    String password = "";
    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.AUTH_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), id);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.AUTH_RESPONSE) {
        password = Serializers.nullable(Serializers.STRING).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }

    } catch (IOException e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }
    return password;
  }

  @Override
  public Collection<Uuid> getAdmins() {
    final HashSet<Uuid> admins = new HashSet<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_ADMINS_REQUEST);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_ADMINS_RESPONSE) {
        admins.addAll(Serializers.collection(Uuid.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }

    } catch (Exception e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }
    return admins;
  }

  @Override
  public Collection<Uuid> getNewAdmins() {
    final HashSet<Uuid> admins = new HashSet<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_NEW_ADMINS_REQUEST);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_NEW_ADMINS_RESPONSE) {
        admins.addAll(Serializers.collection(Uuid.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }

    } catch (Exception e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }
    return admins;
  }

  @Override
  public boolean isUserMember(Uuid conversationId, Uuid userId) {
    boolean isUserMember = false;
    try (final Connection connection = source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.CHECK_MEMBER_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), conversationId);
      Uuid.SERIALIZER.write(connection.out(), userId);
      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.CHECK_MEMBER_RESPONSE) {
        isUserMember = Serializers.BOOLEAN.read(connection.in());
      } else {
         LOG.error("Response from server failed.");
      } 
    } catch (IOException e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }
    return isUserMember;
  }

  @Override
  public boolean isUserOwner(Uuid conversationId, Uuid userId) {
    boolean isUserOwner = false;
    try (final Connection connection = source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.CHECK_OWNER_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), conversationId);
      Uuid.SERIALIZER.write(connection.out(), userId);
      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.CHECK_OWNER_RESPONSE) {
        isUserOwner = Serializers.BOOLEAN.read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (IOException e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }
    return isUserOwner;
  }

  @Override
  public boolean isUserCreator(Uuid conversationId, Uuid userId) {
    boolean isUserCreator = false;
    try (final Connection connection = source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.CHECK_CREATOR_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), conversationId);
      Uuid.SERIALIZER.write(connection.out(), userId);
      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.CHECK_CREATOR_RESPONSE) {
        isUserCreator = Serializers.BOOLEAN.read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (IOException e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }
    return isUserCreator;
  }
}
