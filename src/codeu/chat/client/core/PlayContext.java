package codeu.chat.client.core;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.util.Uuid;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public final class PlayContext {

  private final ConversationHeader conversation;
  private final View view;
  private final Controller controller;
  private final Uuid player;

  public PlayContext(Uuid player, ConversationHeader conversation,
                             View view,
                             Controller controller) {

    this.conversation = conversation;
    this.view = view;
    this.controller = controller;
    this.player = player;
  }

  public void speak() {
    controller.speak(player, conversation.title);
  }

  public void setStatus(String status) {
    controller.setStatus(conversation.title, status);
  }

  public String getStatus() {
    return view.getStatus(conversation.title);
  }

  public void printInfo() {
    printHeading();
    printLines();
  }

  public void printHeading() {
    System.out.println(conversation.title);
    System.out.format("Your role: %s\n", view.getRole(conversation.title, player));
  }

  public void parseLine() {
    controller.parseLine(player, conversation.title);
  }

  public boolean checkMyTurn() {
    return view.checkMyTurn(player, conversation.title);
  }

  public void printLines() {
    for (MessageContext message = firstMessage();
         message != null;
         message = message.next()) {
      System.out.println(message.message.content);
    }
  }

  public MessageContext firstMessage() {

    // As it is possible for the conversation to have been updated, so fetch
    // a new copy.
    final ConversationPayload updated = getUpdated();

    return updated == null ?
            null :
            getMessage(updated.firstMessage);
  }

  private MessageContext getMessage(Uuid id) {
    final Iterator<Message> messages = view.getMessages(Arrays.asList(id)).iterator();
    return messages.hasNext() ? new MessageContext(messages.next(), view) : null;
  }

  private ConversationPayload getUpdated() {
    final Collection<Uuid> ids = Arrays.asList(conversation.id);
    final Iterator<ConversationPayload> payloads = view.getConversationPayloads(ids).iterator();
    return payloads.hasNext() ? payloads.next() : null;
  }
}
