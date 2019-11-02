import akka.actor.*;
import akka.routing.Router;
import javafx.util.Pair;
import java.time.Duration;
import java.io.Serializable;
import java.util.HashMap;

public class Group{

    private String groupName;
    private String admin;
    private HashMap<String,Pair<User, PType>> usersMap;
    private HashMap<String,Pair<Duration, Cancellable>> mutedMap;
    private Router router;



    public Group(CommunicationCommand cmd, User admin, Router router) {
        this.groupName = cmd.getGroupName();
        this.admin = admin.getUserName();
        this.usersMap = new HashMap<>();
        admin.setBeforeMute(PType.Admin);
        this.usersMap.put(admin.getUserName(), new Pair<>(admin, PType.Admin));
        this.mutedMap = new HashMap<>();
        this.router = router;
    }

    //Privileges type
    public enum PType implements Serializable {
        Admin, Co_Admin, User, Muted, Not_User
    }

    public Router getRouter(){
        return this.router;
    }

    //return the username PType or Not_User if
    //the target not in the group
    public PType getUserPType(String userName){
        PType userType;
        Pair<User, PType> pair = this.usersMap.get(userName);
        if (pair == null)
            userType = PType.Not_User;
        else
            userType = pair.getValue();
        return userType;
    }

    //return true result command with removal message
    private Command removeNotAdminMessage(CommunicationCommand cmd){
        cmd.setResult(true, cmd.getUser().getUserName() +
                " has left " + this.groupName + "!");
        cmd.setFrom(Command.From.Group);
        return cmd;
    }

    //remove not admin user from the group and the routes
    private Command leaveUser(CommunicationCommand cmd){
        Command newCmd = removeNotAdminMessage(cmd);
        User user = this.usersMap.remove(cmd.getUser().getUserName()).getKey();
        this.router = this.router.removeRoutee(user.getUserRef());
        return newCmd;
    }

    //remove the admin from the group and returns
    //"Admin" result to notify the server of the admin removal
    //before closing the group
    private Command leaveAdmin(CommunicationCommand cmd){
        User user = this.usersMap.remove(this.admin).getKey();
        this.router = this.router.removeRoutee(user.getUserRef());
        cmd.setResult(true, "Admin");
        cmd.setFrom(Command.From.Group);
        return cmd;
    }

    //handle user leaving the group
    // return false if userName is not this group user
    public CommunicationCommand leaveGroup(CommunicationCommand cmd) {
        PType type = getUserPType(cmd.getUser().getUserName());
        Command newCmd;
        if (type.equals(PType.Admin))
            newCmd = leaveAdmin(cmd);
        else if (!type.equals(PType.Not_User))
            newCmd = leaveUser(cmd);
        else {
            newCmd = cmd;
            newCmd.setResult(false, cmd.getUser().getUserName() +
                    " is not in " + cmd.getGroupName() + "!.");
        }
        return (CommunicationCommand) newCmd;
    }

    //add user to the group and routes
    public void addUser(User user){
        user.setBeforeMute(PType.User);
        this.usersMap.put(user.getUserName(),new Pair<>(user, PType.User));
        this.router = router.addRoutee(user.getUserRef());
    }

    //remove user from group and routes
    public void removeUser(User user){
        this.usersMap.remove(user.getUserName());
        this.router = router.removeRoutee(user.getUserRef());
    }

    //change user PType to Co-Admin
    public void promoteUser(User user){
        Pair<User, PType> pair = this.usersMap.remove(user.getUserName());
        Pair<User, PType> newPair = new Pair<>(pair.getKey(), PType.Co_Admin);
        newPair.getKey().setBeforeMute(PType.Co_Admin);
        this.usersMap.put(user.getUserName(), newPair);
    }

    //change user PType to User
    public void demoteUser(User user){
        Pair<User, PType> pair = this.usersMap.remove(user.getUserName());
        Pair<User, PType> newPair = new Pair<>(pair.getKey(), PType.User);
        newPair.getKey().setBeforeMute(PType.User);
        this.usersMap.put(user.getUserName(), newPair);
    }

    //change user PType to Mute and save the old PType in the User
    //to restore its PType at unmute
    public void muteUser(User user,Duration duration, Cancellable cancel){
        Pair<User, PType> pair = this.usersMap.remove(user.getUserName());
        Pair<User, PType> newPair = new Pair<>(pair.getKey(), PType.Muted);
        Pair<Duration, Cancellable> mutedPair = new Pair<>(duration, cancel);
        this.usersMap.put(user.getUserName(), newPair);
        this.mutedMap.put(user.getUserName(), mutedPair);
    }

    //restore the user PType its old PType
    public void unMuteUser(User user){
        Pair<User, PType> pair = this.usersMap.remove(user.getUserName());
        PType oldType = pair.getKey().getBeforeMute();
        Pair<User, PType> newPair = new Pair<>(pair.getKey(), oldType);
        Pair<Duration, Cancellable> mutedPair = this.mutedMap.remove(user.getUserName());
        if (mutedPair != null)
            mutedPair.getValue().cancel();

        this.usersMap.put(user.getUserName(), newPair);
    }

    //returns the duration time of the muted user
    public long getMutedTime(String userName){
        Pair<Duration, Cancellable> mutedPair =  this.mutedMap.get(userName);
        Duration duration = mutedPair.getKey();
        return duration.getSeconds();
    }

}
