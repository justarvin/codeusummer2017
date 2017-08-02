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

package codeu.chat.client.commandline;

import codeu.chat.client.core.*;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ServerInfo;
import codeu.chat.util.PlayInfo;
import codeu.chat.util.Tokenizer;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public final class Chat {

  // PANELS
  //
  // We are going to use a stack of panels to track where in the application
  // we are. The command will always be routed to the panel at the top of the
  // stack. When a command wants to go to another panel, it will add a new
  // panel to the top of the stack. When a command wants to go to the previous
  // panel all it needs to do is pop the top panel.
  private final Stack<Panel> panels = new Stack<>();
  private Context context;

  public Chat(Context context) {
    this.context = context;
    this.panels.push(createRootPanel(context));
  }

  // HANDLE COMMAND
  //
  // Take a single line of input and parse a command from it. If the system
  // is willing to take another command, the function will return true. If
  // the system wants to exit, the function will return false.
  //
  public boolean handleCommand(String line) throws IOException {

    final List<String> args = new ArrayList<>();
    final Tokenizer tokenizer = new Tokenizer(line);
    for (String token = tokenizer.next(); token != null; token = tokenizer.next()) {
      args.add(token);
    }
    final String command = args.get(0);
    args.remove(0);

    // Because "exit" and "back" are applicable to every panel, handle
    // those commands here to avoid having to implement them for each
    // panel.

    if ("exit".equals(command)) {
      // The user does not want to process any more commands
      context.writeRestOfQueue();
      return false;
    }

    // Do not allow the root panel to be removed.
    if ("back".equals(command) && panels.size() > 1) {
      panels.pop();
      return true;
    }

    if (panels.peek().handleCommand(command, args)) {
      // the command was handled
      return true;
    }

    // If we get to here it means that the command was not correctly handled
    // so we should let the user know. Still return true as we want to continue
    // processing future commands.
    System.out.println("ERROR: Unsupported command");
    return true;
  }

  // CREATE ROOT PANEL
  //
  // Create a panel for the root of the application. Root in this context means
  // the first panel and the only panel that should always be at the bottom of
  // the panels stack.
  //
  // The root panel is for commands that require no specific contextual information.
  // This is before a user has signed in. Most commands handled by the root panel
  // will be user selection focused.
  //
  private Panel createRootPanel(final Context context) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command to print a list of all commands and their description when
    // the user for "help" while on the root panel.
    //
    panel.register("help", new Panel.Command() {

      @Override
      public void invoke(List<String> args) {
        System.out.println("ROOT MODE");
        System.out.println("  u-list");
        System.out.println("    List all users.");
        System.out.println("  u-sign-in <name>");
        System.out.println("    Sign in as the user with the given name.");
        System.out.println("  clean");
        System.out.println("    Clear history.");
        System.out.println("  server-info");
        System.out.println("    Returns information about the server.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // U-LIST (user list)
    //
    // Add a command to print all users registered on the server when the user
    // enters "u-list" while on the root panel.
    //
    panel.register("u-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final UserContext user : context.allUsers()) {
          System.out.format(
                  "USER %s (UUID:%s)\n",
                  user.user.name,
                  user.user.id);
        }
      }
    });

    // U-SIGN-IN (sign in user)
    //
    // Add a command to sign-in as a user when the user enters "u-sign-in"
    // while on the root panel.
    //
    panel.register("u-sign-in", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.get(0);
        if (name.length() > 0) {
          final UserContext user = findUser(name);
          if (user == null) {
            System.out.format("ERROR: Failed to sign in as '%s'\n", name);
          } else {
            boolean success = false;
            Console console = System.console();
            if (context.getNewAdmins().contains(user.user.id)) {
              String input = new String(console.readPassword("Set a new password: "));
              String repeat = new String(console.readPassword("Reenter your password: "));
              System.out.println("Verifying...");
              if (!input.equals(repeat)) {
                System.out.println("Error: passwords didn't match.");
              } else {
                success = context.setPassword(user.user.id, input);
              }
            } else if (context.getAdmins().contains(user.user.id)) {
              char passwordArray[] = console.readPassword("Enter your password: ");
              success = context.authenticate(user.user.id, new String(passwordArray));
              System.out.println("Authenticating...");
            }

            if (success) {
              panels.push(createUserPanel(user));
            }
          }
        } else {
          System.out.println("ERROR: Missing <username>");
        }
      }
    });

    // CLEAN
    //
    // Add a command to clean the transaction log when the user enters
    // "clean" while on the root panel
    //
    panel.register("clean", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        context.clean();
      }
    });

    // SERVER-INFO
    //
    // Command to show info about the server.
    panel.register("server-info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final ServerInfo serverInfo = context.getInfo();
        if (serverInfo == null) {
          System.out.println("ERROR: Server did not send valid info object.");
        } else {
          // Print the server info to the user in a pretty way.
          System.out.println("  Server Info: ");
          System.out.format("   Version Number: %s\n", serverInfo.getVersion());
          System.out.println("The server has been running since " + serverInfo.getStartTime().inMs());
        }
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }

  private UserContext findUser(String name) {
    for (final UserContext user : context.allUsers()) {
      if (user.user.name.equals(name)) {
        return user;
      }
    }
    return null;
  }

  public Panel createUserPanel(final UserContext user) {

    final Panel panel = new Panel();

      // HELP
      //
      // Add a command that will print a list of all commands and their
      // descriptions when the user enters "help" while on the user panel.
      //
      panel.register("help", new Panel.Command() {
        @Override
        public void invoke(List<String> args) {
          System.out.println("USER MODE");

          if (context.getAdmins().contains(user.user.id)) {
            System.out.println("  u-list");
            System.out.println("    List all users.");
            System.out.println("  u-add <name> (<type>)");
            System.out.println("    Creates a user with given name as a regular user by default. Else append admin.");
            System.out.println("  u-delete <name>");
            System.out.println("    Deletes user with the given name");
            System.out.println("  a-add <name>");
            System.out.println("    Gives user with given name admin privileges.");
            System.out.println("  a-remove <name>");
            System.out.println("    Removes admin privileges for the given user.");
            System.out.println("  c-delete <name>");
            System.out.println("    Deletes the conversation with the given name");
          }

          System.out.println("  c-list");
          System.out.println("    List all conversations that the current user can interact with.");
          System.out.println("  c-add <title>");
          System.out.println("    Add a new conversation with the given title and join it as the current user.");
          System.out.println("  c-join <title>");
          System.out.println("    Join the conversation as the current user.");
          System.out.println("  info");
          System.out.println("    Display all info for the current user");
          System.out.println("  interest");
          System.out.println("    Display the panel for managing interests and status updates.");
          System.out.println("  play");
          System.out.println("    Display options for enacting a play with other users.");
          System.out.println("  back");
          System.out.println("    Go back to ROOT MODE.");
          System.out.println("  exit");
          System.out.println("    Exit the program.");
        }
      });

      // Only register these commands if current user is an admin
      if (context.getAdmins().contains(user.user.id)) {

        // U-LIST (user list)
        //
        // Add a command to print all users registered on the server when the admin
        // enters "u-list".
        //
        panel.register("u-list", new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            for (final UserContext user : context.allUsers()) {
              System.out.format(
                      "USER %s (UUID:%s)\n",
                      user.user.name,
                      user.user.id);
            }
          }
        });

        // ADD ADMIN
        //
        // Adds the specified user as an admin
        panel.register("a-add", new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            String name = args.get(0);
            boolean success = context.addAdmin(name, true);
            if (success) {
              System.out.println("Administrator privileges given to " + name);
            } else {
              System.out.println("Operation failed");
            }
          }
        });

        // REMOVE ADMIN
        //
        // Removes the specified user as an admin
        panel.register("a-remove", new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            String name = args.get(0);
            context.removeAdmin(name);
          }
        });

        // ADD USER
        //
        // Adds a new user with the specified name as a regular user by default.
        // If the client has specified "admin", this user will be given admin controls.
        panel.register("u-add", new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            String name = args.get(0);
            if (name.length() > 0) {
              if (context.create(name) == null) {
                System.out.println("ERROR: Failed to create new user");
              }
            } else {
              System.out.println("ERROR: Missing <username>");
            }
            if (args.size() == 2) {
              boolean success = context.addAdmin(name, false);
              if (!success) {
                System.out.format("ERROR: Couldn't add %s as admin.", name);
              }
            }
          }
        });

        panel.register("u-delete", new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            String name = args.get(0);
            if (name.equals(user.user.name)) {
              System.out.println("ERROR: Can't delete yourself");
            } else if (name.length() > 0) {
              UserContext user = findUser(name);
              if (user != null) {
                context.deleteUser(user.user);
              } else {
                System.out.println("ERROR: User does not exist");
              }
            } else {
              System.out.println("ERROR: Missing <username>");
            }
          }
        });

        panel.register("c-delete", new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            String title = args.get(0);
            ConversationHeader conversation = findConversation(title);
            if (title.length() > 0) {
              if (conversation != null) {
                context.deleteConversation(conversation);
              } else {
                System.out.println("ERROR: Conversation does not exist");
              }
            } else {
              System.out.println("ERROR: Missing <title>");
            }
          }
        });

      }

      // C-LIST (list conversations)
      //
      // Add a command that will print all conversations when the user enters
      // "c-list" while on the user panel.
      //
      panel.register("c-list", new Panel.Command() {
        @Override
        public void invoke(List<String> args) {
          for (final ConversationContext conversation : user.conversations()) {
            System.out.format(
                    "CONVERSATION %s (UUID:%s)\n",
                    conversation.conversation.title,
                    conversation.conversation.id);
          }
        }
      });

      // C-ADD (add conversation)
      //
      // Add a command that will create and join a new conversation when the user
      // enters "c-add" while on the user panel.
      //
      panel.register("c-add", new Panel.Command() {
        @Override
        public void invoke(List<String> args) {
          final String name = args.get(0);
          if (name.length() > 0) {
            final ConversationContext conversation = user.start(name);
            if (conversation == null) {
              System.out.println("ERROR: Failed to create new conversation");
            } else {
              panels.push(createConversationPanel(conversation));
            }
          } else {
            System.out.println("ERROR: Missing <title>");
          }
        }
      });

      // C-JOIN (join conversation)
      //
      // Add a command that will joing a conversation when the user enters
      // "c-join" while on the user panel.
      //
      panel.register("c-join", new Panel.Command() {
        @Override
        public void invoke(List<String> args) {
          final String name = args.get(0);
          if (name.length() > 0) {
            final ConversationContext conversation = find(name);
            if (conversation == null) {
              System.out.format("ERROR: No conversation with name '%s'\n", name);
            } else {
              panels.push(createConversationPanel(conversation));
            }
          } else {
            System.out.println("ERROR: Missing <title>");
          }
        }

        // Find the first conversation with the given name and return its context.
        // If no conversation has the given name, this will return null.
        private ConversationContext find(String title) {
          for (final ConversationContext conversation : user.conversations()) {
            if (title.equals(conversation.conversation.title)) {
              return conversation;
            }
          }
          return null;
        }
      });

      // INFO
      //
      // Add a command that will print info about the current context when the
      // user enters "info" while on the user panel.
      //
      panel.register("info", new Panel.Command() {
        @Override
        public void invoke(List<String> args) {
          System.out.println("User Info:");
          System.out.format("  Name : %s\n", user.user.name);
          System.out.format("  Id   : UUID:%s\n", user.user.id);
        }
      });

      panel.register("interest", new Panel.Command() {
        @Override
        public void invoke(List<String> args) {
          panels.push(createInterestPanel(user));
        }
      });

      panel.register("play", new Panel.Command() {
        @Override
        public void invoke(List<String> args) {
          panels.push(createPlayPanel(user));
        }
      });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }

  private ConversationHeader findConversation(String title) {
    for (final ConversationHeader c : context.allConversations()) {
      if (c.title.equals(title)) {
        return c;
      }
    }
    return null;
  }

  private Panel createPlayPanel(final UserContext user) {

    final Panel panel = new Panel();

    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("  titles");
        System.out.println("    Display titles of all available plays.");
        System.out.println("  all");
        System.out.println("    Display all ongoing plays and their statuses.");
        System.out.println("  new <title>");
        System.out.println("    Create and join a new play enactment for the given title.");
        System.out.println("  join <title>");
        System.out.println("    Join an ongoing play enactment for the given title.");
        System.out.println("  back");
        System.out.println("    Go back to the user panel.");
      }
    });

    panel.register("titles", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final String title : context.allPlayTitles()) {
          System.out.println(title);
        }
      }
    });

    panel.register("all", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final PlayInfo p : context.allPlays()) {
          System.out.format("%s - %s\n", p.getTitle(), p.getStatus());
        }
      }
    });

    panel.register("new", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
          builder.append(args.get(i));
          builder.append(" ");
        }
        builder.deleteCharAt(builder.length() - 1);
        String title = builder.toString();
        ConversationHeader play = context.newPlay(user.user.id, title);
        System.out.println("Successfully created new play. Waiting for more users to join...");
      }
    });

    panel.register("join", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
          builder.append(args.get(i));
          builder.append(" ");
        }
        builder.deleteCharAt(builder.length() - 1);
        String title = builder.toString();
        ConversationHeader play = context.joinPlay(user.user.id, title);

        boolean filled = context.checkFilled(play.id, play.title);
        if (!filled) {
          System.out.println("Successfully joined. Waiting for more users to join...");
        } else {
          panels.push(createPlayConversationPanel(new PlayContext(user.user.id, play, context.getView(), context.getController())));
        }
      }
    });

    return panel;
  }

  private Panel createPlayConversationPanel(final PlayContext play) {

    final Panel panel = new Panel();
    play.printHeading();
    play.parseLine();
    //play.printLines();

    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("  speak");
        System.out.println("    Say your character's next line.");
        System.out.println("  back");
        System.out.println("    Return to all plays.");
      }
    });

    panel.register("speak", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        play.speak();
      }
    });

    return panel;
  }

  private Panel createInterestPanel(final UserContext user) {

    final Panel panel = new Panel();

    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("  u-status <name>");
        System.out.println("    Display status update for the user with the given name.");
        System.out.println("  c-status <name>");
        System.out.println("    Display status update for the conversation with the given name.");
        System.out.println("  i-add-user <name>");
        System.out.println("    Add the user with the given name as an interest.");
        System.out.println("  i-add-conv <title>");
        System.out.println("    Add the conversation with the given title as an interest.");
        System.out.println("  i-remove-user <name>");
        System.out.println("    Remove the user with the given name as an interest.");
        System.out.println("  i-remove-conv <title>");
        System.out.println("    Remove the conversation with the given title as an interest.");
        System.out.println("  back");
        System.out.println("    Go back to the user panel.");
      }
    });

    // STATUS UPDATE
    //
    // Displays status update for a specified user.
    //
    panel.register("u-status", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        String name = args.get(0);
        System.out.format("Conversations created or added to by %s since last update\n", name);
        for (final ConversationContext conversation : user.getUserUpdate(user.user.id, name)) {
          System.out.format(
                  "CONVERSATION %s (UUID:%s)\n",
                  conversation.conversation.title,
                  conversation.conversation.id);
        }
      }
    });

    // STATUS UPDATE
    //
    // Displays status update for a specified conversation.
    //
    panel.register("c-status", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        String title = args.get(0);
        int messages = user.getConversationUpdate(user.user.id, title);
        System.out.println("Number of new messages since last update: " + messages);
      }
    });

    // ADD USER INTEREST
    //
    // Add a user interest for this user.
    panel.register("i-add-user", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.get(0);
        user.addUserInterest(name, user.user.id);
      }
    });

    // ADD CONVERSATION INTEREST
    //
    // Add a conversation interest for this user.
    panel.register("i-add-conv", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String title = args.get(0);
        user.addConversationInterest(title, user.user.id);
      }
    });

    // REMOVE USER INTEREST
    //
    // Removes the given user as an interest for this user.
    panel.register("i-remove-user", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.get(0);
        user.removeUserInterest(name, user.user.id);
      }
    });

    // REMOVE CONVERSATION INTEREST
    //
    // Removes the given conversation as an interest for this user.
    panel.register("i-remove-conv", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String title = args.get(0);
        user.removeConversationInterest(title, user.user.id);
      }
    });

    return panel;
  }

  private Panel createConversationPanel(final ConversationContext conversation) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print all the commands and their descriptions
    // when the user enters "help" while on the conversation panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("USER MODE");
        System.out.println("  m-list");
        System.out.println("    List all messages in the current conversation.");
        System.out.println("  m-add <message>");
        System.out.println("    Add a new message to the current conversation as the current user.");
        System.out.println("  info");
        System.out.println("    Display all info about the current conversation.");
        System.out.println("  back");
        System.out.println("    Go back to USER MODE.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // M-LIST (list messages)
    //
    // Add a command to print all messages in the current conversation when the
    // user enters "m-list" while on the conversation panel.
    //
    panel.register("m-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("--- start of conversation ---");
        for (MessageContext message = conversation.firstMessage();
             message != null;
             message = message.next()) {
          System.out.println();
          System.out.format("USER : %s\n", message.message.author);
          System.out.format("SENT : %s\n", message.message.creation);
          System.out.println();
          System.out.println(message.message.content);
          System.out.println();
        }
        System.out.println("---  end of conversation  ---");
      }
    });

    // M-ADD (add message)
    //
    // Add a command to add a new message to the current conversation when the
    // user enters "m-add" while on the conversation panel.
    //
    panel.register("m-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String message = args.get(0);
        if (message.length() > 0) {
          conversation.add(message);
        } else {
          System.out.println("ERROR: Messages must contain text");
        }
      }
    });

    // INFO
    //
    // Add a command to print info about the current conversation when the user
    // enters "info" while on the conversation panel.
    //
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("Conversation Info:");
        System.out.format("  Title : %s\n", conversation.conversation.title);
        System.out.format("  Id    : UUID:%s\n", conversation.conversation.id);
        System.out.format("  Owner : %s\n", conversation.conversation.owner);
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }

  // accessor method for testing purposes
  public Stack<Panel> getPanels() {
    return panels;
  }
}
