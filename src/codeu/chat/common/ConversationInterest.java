package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConversationInterest extends Interest {
    public final Uuid id;
    public final Uuid convoId;
    public final Uuid owner;
    public final Time creation;

    public ConversationInterest(Uuid id, Uuid convoId, Uuid owner, Time creation) {

        this.id = id;
        this.convoId = convoId;
        this.owner = owner;
        this.creation = creation;
    }

    public static final Serializer<ConversationInterest> SERIALIZER = new Serializer<ConversationInterest>() {

        @Override
        public void write(OutputStream out, ConversationInterest value) throws IOException {

            Uuid.SERIALIZER.write(out, value.id);
            Uuid.SERIALIZER.write(out, value.convoId);
            Uuid.SERIALIZER.write(out, value.owner);
            Time.SERIALIZER.write(out, value.creation);

        }

        @Override
        public ConversationInterest read(InputStream in) throws IOException {
            return new ConversationInterest(
                    Uuid.SERIALIZER.read(in),
                    Uuid.SERIALIZER.read(in),
                    Uuid.SERIALIZER.read(in),
                    Time.SERIALIZER.read(in)
            );
        }
    };
}
