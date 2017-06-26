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

import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.User;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.Connection;
import codeu.chat.util.connections.ConnectionSource;

final class Controller implements BasicController {

  private final static Logger.Log LOG = Logger.newLog(Controller.class);

  private final ConnectionSource source;

  public Controller(ConnectionSource source) {
    this.source = source;
  }

  @Override
  public Message newMessage(Uuid author, Uuid conversation, String body) {

    Message response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_MESSAGE_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), author);
      Uuid.SERIALIZER.write(connection.out(), conversation);
      Serializers.STRING.write(connection.out(), body);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_MESSAGE_RESPONSE) {
        response = Serializers.nullable(Message.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public User newUser(String name) {

    User response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_USER_REQUEST);
      Serializers.STRING.write(connection.out(), name);
      LOG.info("newUser: Request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_USER_RESPONSE) {
        response = Serializers.nullable(User.SERIALIZER).read(connection.in());
        LOG.info("newUser: Response completed.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public ConversationHeader newConversation(String title, Uuid owner)  {

    ConversationHeader response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_CONVERSATION_REQUEST);
      Serializers.STRING.write(connection.out(), title);
      Uuid.SERIALIZER.write(connection.out(), owner);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_CONVERSATION_RESPONSE) {
        response = Serializers.nullable(ConversationHeader.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public void newUserInterest(String name, Uuid owner) {

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_USER_INTEREST_REQUEST);
      Serializers.STRING.write(connection.out(), name);
      Uuid.SERIALIZER.write(connection.out(), owner);

      LOG.info("Add interest: request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_USER_INTEREST_RESPONSE) {
        LOG.info("Add interest: response completed.");
      } else {
        LOG.error("Add interest: request failed.");
      }

    } catch (Exception e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }
  }

  @Override
  public void newConversationInterest(String title, Uuid owner) {

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_CONVERSATION_INTEREST_REQUEST);
      Serializers.STRING.write(connection.out(), title);
      Uuid.SERIALIZER.write(connection.out(), owner);

      LOG.info("Add interest: request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_CONVERSATION_INTEREST_RESPONSE) {
        LOG.info("Add interest: response completed.");
      } else {
        LOG.error("Add interest: request failed.");
      }

    } catch (Exception e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }
  }

  @Override
  public void removeUserInterest(String title, Uuid owner) {

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.REMOVE_USER_INTEREST_REQUEST);
      Serializers.STRING.write(connection.out(), title);
      Uuid.SERIALIZER.write(connection.out(), owner);

      LOG.info("Remove interest: request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.REMOVE_USER_INTEREST_RESPONSE) {
        LOG.info("Remove interest: response completed.");
      } else {
        LOG.error("Remove interest: request failed.");
      }

    } catch (Exception e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }
  }

  @Override
  public void removeConversationInterest(String title, Uuid owner) {

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.REMOVE_CONVERSATION_INTEREST_REQUEST);
      Serializers.STRING.write(connection.out(), title);
      Uuid.SERIALIZER.write(connection.out(), owner);

      LOG.info("Remove interest: request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.REMOVE_CONVERSATION_INTEREST_RESPONSE) {
        LOG.info("Remove interest: response completed.");
      } else {
        LOG.error("Remove interest: request failed.");
      }

    } catch (Exception e) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(e, "Exception during call on server.");
    }
  }

  // CLEAN
  //
  // Cleans all existing information from the server and transaction log.
  void clean() {
    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.CLEAN_REQUEST);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.CLEAN_RESPONSE) {
        System.out.println("Cleaned history.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }
  }

  // WRITE REST OF QUEUE
  //
  // When the client uses the exit command to close the application,
  // any logs that have not yet been written to the log will be written.
  void writeRestOfQueue() {
    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.WRITE_REST_OF_QUEUE_REQUEST);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.WRITE_REST_OF_QUEUE_RESPONSE) {
        LOG.info("Saved.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }
  }
}
