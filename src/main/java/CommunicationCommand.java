import akka.actor.ActorRef;
import java.io.Serializable;

public class CommunicationCommand extends Command implements Serializable {

    private User user;
    private String groupName;

    public CommunicationCommand(String[] str, From from){
        super(Type.Connect, from);
        user = new User(str[0]);
    }

    public CommunicationCommand(String[] str, From from, Type type){
        super(type, from);
        user = new User(str[0]);
        if (!type.equals(Type.Disconnect))
            this.groupName  = str[1];
    }

    public User getUser() {
        return user;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setUserRef(ActorRef ref){
        this.user.setUserRef(ref);
    }
}
