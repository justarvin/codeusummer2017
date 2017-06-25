package codeu.chat.common;


import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

public class Interest {
    public final Uuid id;
    public final Uuid owner;
    public final Uuid interest;
    public final Time creation;

    public Interest(Uuid id, Uuid owner, Uuid interest, Time creation){
        this.id = id;
        this.owner = owner;
        this.interest = interest;
        this.creation = creation;
    }
}
