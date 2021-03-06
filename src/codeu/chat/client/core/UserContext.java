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

import java.util.ArrayList;
import java.util.Collection;

import codeu.chat.common.BasicController;
import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.User;
import codeu.chat.util.Uuid;

public final class UserContext {

  public final User user;
  private final BasicView view;
  private final BasicController controller;

  public UserContext(User user, BasicView view, BasicController controller) {
    this.user = user;
    this.view = view;
    this.controller = controller;
  }

  public ConversationContext start(String name) {
    final ConversationHeader conversation = controller.newConversation(name, user.id);
    return conversation == null ?
        null :
        new ConversationContext(user, conversation, view, controller);
  }

  public Iterable<ConversationContext> conversations() {

    // Use all the ids to get all the conversations and convert them to
    // Conversation Contexts.
    final Collection<ConversationContext> all = new ArrayList<>();
    for (final ConversationHeader conversation : view.getConversations()) {
      all.add(new ConversationContext(user, conversation, view, controller));
    }

    return all;
  }

  public void addUserInterest(String name, Uuid owner) {
    controller.newUserInterest(name, owner);
  }

  public void addConversationInterest(String title, Uuid owner) {
    controller.newConversationInterest(title, owner);
  }

  public void removeUserInterest(String name, Uuid owner) {
    controller.removeUserInterest(name, owner);
  }

  public void removeConversationInterest(String title, Uuid owner) {
    controller.removeConversationInterest(title, owner);
  }

  public Iterable<ConversationContext> getUserUpdate(Uuid owner, String name) {
    final Collection<ConversationContext> all = new ArrayList<>();
    for (final ConversationHeader conversation : view.getUserUpdate(owner, name)) {
      all.add(new ConversationContext(user, conversation, view, controller));
    }
    return all;
  }

  public int getConversationUpdate(Uuid owner, String title) {
    return view.getConversationUpdate(owner, title);
  }

  public boolean isUserMember(ConversationContext conversationContext) {
    Uuid conversationId = conversationContext.conversation.id;
    Uuid userId = user.id;
    return view.isUserMember(conversationId, userId);
  }

  public boolean isUserCreator(ConversationContext conversationContext) {
    Uuid conversationId = conversationContext.conversation.id;
    Uuid userId = user.id;
    return view.isUserCreator(conversationId, userId);
  }

  public boolean isUserOwner(ConversationContext conversationContext) {
    Uuid conversationId = conversationContext.conversation.id;
    Uuid userId = user.id;
    return view.isUserOwner(conversationId, userId);
  }
}
