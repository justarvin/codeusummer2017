package codeu.chat.client.core;

import codeu.chat.common.ConversationHeader;
import codeu.chat.util.Uuid;

public class PlayContext {

  public final ConversationHeader conversation;
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
    controller.speak();
  }

  public ConversationHeader getPlayConversation() {
    return conversation;
  }

  public void printHeading() {
    System.out.println(conversation.title);
    System.out.format("Your role: %s", view.getRole(conversation.title, player));
  }

  public String parseLine() {
    return controller.parseLine(conversation.title);
  }

  public void printLines() {

  }
}
