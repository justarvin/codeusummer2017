package codeu.chat.server;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.BasicController;
import codeu.chat.common.Secret;
import codeu.chat.common.User;
import codeu.chat.util.Uuid;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Test for persistent storage
 */
public class StorageTest {

    private Model model;
    private BasicController controller;

    @Before
    public void doBefore() {
        model = new Model();
    }

    @Test
    public void writeAfter15Logs() throws IOException {

        Secret secret = Secret.parse("16A");
        Uuid id = new Uuid(4);
        File persistentPath = new File("logfiles");
        File log = new File(persistentPath.getPath());
        controller = new Controller(id, model);
        Server server = new Server(id, secret, null, persistentPath);

        User user = controller.newUser("user");
        ConversationHeader conversation = controller.newConversation("conversation", user.id);

        //numbers are keeping track of how many log strings have
        // been written after new user and new conversation
        for (int i = 3; i <= 17; i++) {
            server.checkBuffer();
            controller.newMessage(user.id, conversation.id, "message "+i);
        }

        assertEquals(2, Server.logBuffer.size());
    }
}
