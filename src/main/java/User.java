import akka.actor.ActorRef;
import java.io.Serializable;

public class User implements Serializable {

    private String userName;
    private ActorRef userRef;
    private boolean connected;
    private Invitation invitation;
    private Group.PType beforeMute;

    public User(String name){
        this.userName = name;
    }

    public User(ActorRef myRef){
        this.userRef = myRef;
        this.connected = false;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String name){
        this.userName = name;
    }

    public ActorRef getUserRef() {
        return userRef;
    }

    public void setUserRef(ActorRef ref){
        this.userRef = ref;
    }

    public void connect(){
        this.connected = true;
    }

    public void disconnect(){
        this.connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setInvitation(Invitation inv){
        this.invitation = inv;
    }

    public Invitation getInvitation(){
        return invitation;
    }

    public void setBeforeMute(Group.PType type){
        this.beforeMute = type;
    }

    public Group.PType getBeforeMute(){
        return this.beforeMute;
    }
}
