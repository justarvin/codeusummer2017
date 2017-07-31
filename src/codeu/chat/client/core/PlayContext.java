package codeu.chat.client.core;

import codeu.chat.common.BasicController;
import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;

public class PlayContext {

  public final ConversationHeader conversation;

  private final BasicView view;
  private final Controller controller;

  public PlayContext(ConversationHeader conversation,
                             BasicView view,
                             Controller controller) {

    this.conversation = conversation;
    this.view = view;
    this.controller = controller;
  }

  public void speak() {
    controller.speak();
  }
}
