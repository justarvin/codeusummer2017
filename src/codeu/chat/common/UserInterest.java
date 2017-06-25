package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UserInterest extends Interest {
    public final Uuid id;
    public final Uuid convoId;
    public final String name;
    public final Time creation;

    public UserInterest(Uuid id, Uuid convoId, String name, Time creation) {

        this.id = id;
        this.convoId = convoId;
        this.name = name;
        this.creation = creation;
    }

    public static final Serializer<UserInterest> SERIALIZER = new Serializer<UserInterest>() {

        @Override
        public void write(OutputStream out, UserInterest value) throws IOException {

            Uuid.SERIALIZER.write(out, value.id);
            Uuid.SERIALIZER.write(out, value.convoId);
            Serializers.STRING.write(out, value.name);
            Time.SERIALIZER.write(out, value.creation);

        }

        @Override
        public UserInterest read(InputStream in) throws IOException {

            return new UserInterest(
                    Uuid.SERIALIZER.read(in),
                    Uuid.SERIALIZER.read(in),
                    Serializers.STRING.read(in),
                    Time.SERIALIZER.read(in)
            );
        }
    };
}
