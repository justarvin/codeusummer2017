package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by imboo on 7/21/2017.
 */
public enum UserType {
    CREATOR(0), OWNER(1), MEMBER(2);
    public final int accessID;

    UserType(int id) {
        accessID = id;
    }
    public static int getType(UserType userType) { return userType.accessID; }
    public static UserType getAccessID (int id) { return values()[id]; }

    public static final Serializer<UserType> SERIALIZER = new Serializer<UserType>() {
                @Override
                public void write(OutputStream out, UserType value) throws IOException {
                    Serializers.INTEGER.write(out, value.accessID);
                }

                @Override
                public UserType read(InputStream in) throws IOException {
                    return UserType.getAccessID(Serializers.INTEGER.read(in));
                }
            };
}
