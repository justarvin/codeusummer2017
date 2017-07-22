package codeu.chat.common;

/**
 * Created by imboo on 7/21/2017.
 */
public class UserType {
    private final int CREATOR = 1;
    private final int OWNER = 2;
    private final int MEMBER = 3;

    public UserType(int id) {
        if (id == 1) {
            //this user is a creator
        } else if ( id == 2) {
            //this user is an owner
        } else {
            //this user is a member
        }
    }
}
