import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.concurrent.TimeUnit.SECONDS;


public class ClientActor extends AbstractActor {

    private User myUser;
    private ActorSelection serverRef;
    private ActorRef ioHandler;
    private Timeout askTimeout;
    private Predicates preds;


    public ClientActor() {
        myUser = new User(getSelf());
        askTimeout = new Timeout(Duration.create(1, SECONDS));
        preds = new Predicates();
    }

    public void preStart(){
        serverRef = getContext().actorSelection(
                "akka.tcp://ServerSystem@127.0.0.1:3553/user/Server");
    }

    //set ioHandler ActorRef at user connect command
    private void setIOhandler(ActorRef sender){
        if (ioHandler == null)
            ioHandler = sender;
    }

    //sets myUser userName and boolean connected
    private void connectUserName(String name){
        myUser.setUserName(name);
        myUser.connect();
    }

    //set From command field to Client and sends the command to the wanted ActorRef
    private void sendCmd(Command cmd, ActorRef ref){
        cmd.setFrom(Command.From.Client);
        ref.tell(cmd, self());
    }

    //send invitation to ActorRef ref
    private void invite(Invitation invitation, ActorRef ref){
        ref.tell(invitation, self());
    }

    //send command to the io Actor to print it's result
    private void print(Command.Type type, String str){
        if (!str.equals(""))
            sendCmd(new Command(type, Command.From.Client, str), this.ioHandler);
    }

    private void notConnected(){
        print(Command.Type.Error,
                "you are not connected to the system");
    }

    //return the current time String
    private String getTime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String time = dateFormat.format(new Date());
        return time;
    }

    //asks the server actor the wanted command
    //if the server sends back result before timeout
    //returns the result
    //else returns false command with "server is offline!" result
    private Command askServer(Command cmd){
        cmd.setFrom(Command.From.Client);
        Future<Object> ret = Patterns.ask(serverRef, cmd, askTimeout);
        try {
            Command res = (Command) Await.result(ret, askTimeout.duration());
            return res;
        } catch (Exception e) {
            cmd.setResult(false, "server is offline!");
            return cmd;
        }
    }

    //asks the server to connect new user if not connected already
    //if not succeeded then printing the false command returned
    //from the server
    private void connectUser(CommunicationCommand cmd){
        if (!myUser.isConnected()) {
            cmd.setUserRef(myUser.getUserRef());
            Command result = askServer(cmd);
            if (result.isSucceed())
                connectUserName(cmd.getUser().getUserName());

            print(cmd.getType(), result.getResult());
        }
        else
            print(Command.Type.Error, "You're already connected");
    }

    //asks the server to disconnect user, if succeeded set myUser userName to null
    //and boolean connected to false
    //if not succeeded then printing the false command returned
    //from the server
    private void disconnectUser(CommunicationCommand cmd){
        if (myUser.isConnected()) {
            cmd.setUser(myUser);
            Command result = askServer(cmd);
            if (result.isSucceed()) {
                myUser.setUserName(null);
                myUser.disconnect();
                print(Command.Type.Disconnect, result.getResult());
            } else
                print(Command.Type.Error, "server is offline! try again later!");
        }
        else
            notConnected();
    }

    //asks the server for command target ActorRef
    //if succeeded, send the result command which contains
    //the relevant message to the target user
    //else printing the false command returned from the server
    private void sendToClient(Command cmd) {
        if (myUser.isConnected()) {
            Command result = askServer(cmd);
            if (result.isSucceed()){
                sendCmd(result, result.getUserResult().getUserRef());
            }else
                print(result.getType(), result.getResult());
        }
        else
            notConnected();
    }

    //asks the server for group command
    //if succeeded, the group will send
    //the relevant message to the targets
    //else printing the false command returned from the server
    private void sendToGroup(Command cmd){
        if (myUser.isConnected()){
            Command result = askServer(cmd);
            if (!result.isSucceed())
                print(result.getType(), result.getResult());
        } else
            notConnected();
    }

    //Printing the sent text message from another user
    private void clientUserText(TextCommand cmd) {
        String message = "[" + getTime() + "][" + cmd.getTarget().getUserName() + "][" +
                        cmd.getSource().getUserName() + "] " + cmd.getMessage();
        print(Command.Type.User_Text, message);
    }

    //Printing the sent text message from group
    private void groupUserText(TextCommand cmd) {
        String message = "[" + getTime() + "][" + cmd.getTarget().getUserName() + "][" +
                cmd.getSource().getUserName() + "] " + cmd.getMessage();
        print(Command.Type.Group_Text, message);
    }

    //returns String message of file received from other user
    private String userFileMessage(FileCommand cmd){
        return "[" + getTime() + "][" + myUser.getUserName() + "][" +
                cmd.getSource().getUserName() + "] File received: " + cmd.getTargetFilePath();
    }

    //returns String message of file received from group
    private String groupFileMessage(FileCommand cmd){
        return "File received: " + cmd.getTargetFilePath();
    }

    //download file received from group or user to
    //Client/src/downloads dir
    //and print the relevant message
    private void downloadFile(FileCommand cmd) {
        Path path = Paths.get(cmd.getTargetFilePath());
        try {
            Files.write(path, cmd.getFile());
            String message = "";
            if (cmd.getType().equals(Command.Type.User_File))
                message = userFileMessage(cmd);
            else if (cmd.getType().equals(Command.Type.Group_File))
                message = groupFileMessage(cmd);

            print(Command.Type.User_File, message);
        } catch (Exception e) {
            print(Command.Type.Error, "Failed to download the sent file");
        }
    }

    //asks the server to create or leave group
    //printing the server result if succeeded or not
    private void groupConnection(CommunicationCommand cmd){
        if(myUser.isConnected()) {
            cmd.setUser(myUser);
            Command result = askServer(cmd);
            print(cmd.getType(), result.getResult());
        }
        else
            notConnected();
    }

    //asks the server for target ActorRef if target in the wanted group
    //if succeeded, send invitation to target
    //else, print the false result that returned from the server
    private void inviteUser(GroupCommand cmd){
        if(myUser.isConnected()) {
            cmd.setSource(myUser);
            GroupCommand result = (GroupCommand) askServer(cmd);
            if (result.isSucceed()){
                Invitation invitation = new Invitation(result);
                invite(invitation, invitation.getTarget().getUserRef());
            } else
                print(cmd.getType(), result.getResult());
        }
        else
            notConnected();
    }

    //returns TextCommand includes success remove message
    private TextCommand removedMessage(GroupCommand cmd){
        return new TextCommand(myUser, cmd.getTarget(),
                "You have been removed from " + cmd.getGroupName()
                        + " by " + myUser.getUserName() + "!");
    }

    //returns TextCommand includes success promote to
    // co-Admin message
    private TextCommand promoteMessage(GroupCommand cmd){
        return new TextCommand(myUser, cmd.getTarget(),
                "You have been promoted to co-admin in " + cmd.getGroupName() + "!");
    }

    //returns TextCommand includes success demote to
    // old privilege message
    private TextCommand demoteMessage(GroupCommand cmd){
        return new TextCommand(myUser, cmd.getTarget(),
                "You have been demoted to user in  " + cmd.getGroupName() + "!");
    }

    //Asks the server to remove/promote/demote
    // target from group
    //printing the server result if its false result
    //else send the relevant message to the target user
    //who print the message
    private void groupUserAction(GroupCommand cmd){
        if(myUser.isConnected()) {
            cmd.setSource(myUser);
            GroupCommand result = (GroupCommand) askServer(cmd);
            if (result.isSucceed() && result.getType().equals(Command.Type.Group_Remove)){
                TextCommand removed = removedMessage(result);
                sendCmd(removed, result.getTarget().getUserRef());
            }
            else if (result.isSucceed() && result.getType().equals(Command.Type.Group_Promote)){
                TextCommand removed = promoteMessage(result);
                sendCmd(removed, result.getTarget().getUserRef());
            }
            else if (result.isSucceed() && result.getType().equals(Command.Type.Group_Demote)){
            TextCommand removed = demoteMessage(result);
            sendCmd(removed, result.getTarget().getUserRef());
            }
            else
                print(cmd.getType(), result.getResult());
        }
        else
        notConnected();
    }

    //If invitation received from other user then, set myUser invitation to wait for answer
    //if invitation answer received:
    //if "Yes" then send back the answer to the user who invited
    //if "No" then remove the invitation from myUser
    //if other user answer received:
    //send the answer to the server who adds the user to the group
    private void answerInvite(Invitation inv, ActorRef sender){
        if (inv.getType().equals(Command.Type.Invitation)){
            if (!inv.isAnswered()) {
                myUser.setInvitation(inv);
                print(inv.getType(), inv.getInvitation());
            }
            else if (inv.getAnswer().equals("Yes")) {
                Invitation ansInv = myUser.getInvitation();
                if (ansInv != null) {
                    ansInv.setAnswer(inv.getAnswer());
                    invite(ansInv, ansInv.getSource().getUserRef());
                }
            }
            else
                myUser.setInvitation(null);
        }
        else if (inv.getType().equals(Command.Type.Answer)){
            if (inv.isAnswered() && inv.getAnswer().equals("Yes")){
                serverRef.tell(inv, self());
            } else {
                print(inv.getType(), inv.getAnswer());
            }
        }
    }

    public Receive createReceive() {

        return receiveBuilder()
                .match(CommunicationCommand.class, preds.connectCmd, (cmd) -> {setIOhandler(getSender()); connectUser(cmd);})
                .match(CommunicationCommand.class, preds.disconnectCmd, this::disconnectUser)
                .match(CommunicationCommand.class, preds.communGroupFromIO, this::groupConnection)
                .match(CommunicationCommand.class, preds.leaveNotFromIO, cmd -> print(cmd.getType(), cmd.getResult()))
                .match(TextCommand.class, preds.groupTextFromIO,  this::sendToGroup)
                .match(FileCommand.class, preds.groupFileFromIO,  this::sendToGroup)
                .match(GroupCommand.class, preds.groupPromteIO, this::groupUserAction)
                .match(GroupCommand.class, preds.groupDemoteIO, this::groupUserAction)
                .match(Command.class, preds.userTextOrFileIO,  this::sendToClient)
                .match(GroupCommand.class, preds.muteGroup, this::sendToClient)
                .match(TextCommand.class, preds.muteTimeIsUp, cmd -> print(cmd.getType(), cmd.getMessage()))
                .match(TextCommand.class, preds.receiveTextGroup, this::groupUserText)
                .match(FileCommand.class, preds.receiveFileGroup, this::downloadFile)
                .match(TextCommand.class, preds.receiveTextClient, this::clientUserText)
                .match(FileCommand.class, preds.receiveFileClient, this::downloadFile)
                .match(GroupCommand.class, preds.groupInviteIO, this::inviteUser)
                .match(GroupCommand.class, preds.groupRemoveIO, this::groupUserAction)
                .match(Invitation.class, (inv) -> answerInvite(inv, sender()))
                .build();
    }
}