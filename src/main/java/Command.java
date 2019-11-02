import java.io.Serializable;

public class Command implements Serializable {

    private Type type;
    private From from;
    private boolean succeed;
    private String result;
    private User userResult;


    public Command(Type type, From from) {
        this.type = type;
        this.from = from;
        this.result = "";
    }

    public Command(Type type, From from, String str){
        this.type = type;
        this.from = from;
        this.result = str;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type newType){
        this.type = newType;
    }

    public From getFrom() {
        return from;
    }

    public void setFrom(From from) {
        this.from = from;
    }

    public void setResult(boolean succeed, String result) {
        if (!succeed)
            this.type = Type.Error;
        this.succeed = succeed;
        this.result = result;
    }

    public void setUserResult(boolean succeed, User userRes) {
        if (!succeed)
            this.type = Type.Error;
        this.succeed = succeed;
        this.userResult = userRes;
    }

    public String getResult() {
        return result;
    }

    public User getUserResult() {
        return userResult;
    }

    public boolean isSucceed(){
        return succeed;
    }

    public enum Type implements Serializable {
        Connect, Disconnect, User_Text, User_File,
        Group_Create, Group_Leave, Group_Text, Group_File,
        Group_Invite, Group_Remove, Group_Promote, Group_Demote,
        Group_Mute, Group_UnMute,
        Answer, Invitation, Error
    }

    public enum From implements Serializable {
        Client, IO, Server, Group
    }

}
