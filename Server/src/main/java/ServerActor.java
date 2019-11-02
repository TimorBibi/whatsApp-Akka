import akka.actor.*;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import akka.actor.Scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



public class ServerActor extends AbstractActor {

    private HashMap<String, User> usersMap;
    private HashMap<String, Group> groupsMap;
    private Scheduler scheduler;
    private Predicates preds;

    public ServerActor(){

        this.usersMap = new HashMap<String, User>();
        this.groupsMap = new HashMap<String, Group>();
        this.scheduler = context().system().scheduler();
        this.preds = new Predicates();

    }

    //sets the command sender to "Server"
    //and sends back the wanted command to sender
    private void sendBack(Command cmd, ActorRef sender){
        cmd.setFrom(Command.From.Server);
        sender.tell(cmd, getSelf());
    }

    //add new user to the server users map if username
    //does not exist and return true.
    //else return false
    private boolean setUser(String name, User user){
        if (this.usersMap.get(name) == null) {
            this.usersMap.put(name, user);
            return  true;
        }
        return false;
    }

    //connect new user to the system if username does not
    //exist and return the relevant message to the sender
    private void connectUser(CommunicationCommand cmd, ActorRef sender){
        String name = cmd.getUser().getUserName();
        if (setUser(name, cmd.getUser()))
            cmd.setResult(true, name + " has connected successfully!");
        else
            cmd.setResult(false, name + " is in use!");
        sendBack(cmd, sender);
    }

    //remove user from the system if exist
    //and send back the relevant result to the sender
    private void disconnectUser(CommunicationCommand cmd, ActorRef sender){
        String name = cmd.getUser().getUserName();
        if (this.usersMap.remove(name) != null)
            // make sure all disconnected dependencies done
            cmd.setResult(true, name + " has been disconnected successfully!");
        else
            cmd.setResult(false, name + " does not exist!");

        sendBack(cmd, sender);
    }

    //return the wanted target User
    //in command User result if exist
    //else, return false command with "does not exist!" message
    private Command getTargetUser(Command cmd, User target){
        User targetUser = usersMap.get(target.getUserName());
        if (targetUser != null){
            cmd.setUserResult(true, targetUser);
        } else {
            cmd.setResult(false, target.getUserName() + " does not exist!");
        }
        return cmd;
    }

    //send textMessage command with the wanted
    //target user back to the sender if exist
    //else, sends false command with relevant result message
    private void userMessage(TextCommand cmd, ActorRef sender){
        User target = cmd.getTarget();
        sendBack(getTargetUser(cmd, target), sender);
    }

    //send fileMessage command with the wanted
    //target user back to the sender if exist
    //else, sends false command with relevant result message
    private void userFile(FileCommand cmd, ActorRef sender){
        User target = cmd.getTarget();
        sendBack(getTargetUser(cmd, target), sender);
    }

    //set new Router includes the admin of the new group
    private Router setGroupRouter(User admin){
        List<Routee> routees = new ArrayList<Routee>();
        routees.add(new ActorRefRoutee(admin.getUserRef()));
        Router newrouter = new Router(new BroadcastRoutingLogic(), routees);
        return newrouter;
    }

    //create new group according to user request
    //includes new Router with the group admin
    //if there is no other group with the same name
    private boolean setGroup(CommunicationCommand cmd){
        if (this.groupsMap.get(cmd.getGroupName()) == null){
            Command acRefCmd = getTargetUser(cmd, cmd.getUser());
            User admin;
            if (acRefCmd.isSucceed())
                admin =  acRefCmd.getUserResult();
            else
                return false;
            Group newGroup = new Group(cmd, admin, setGroupRouter(admin));
            this.groupsMap.put(cmd.getGroupName(), newGroup);

            return true;
        }
        return false;
    }

    //handle the user request to create new group
    //and sends back the server result to the user
    private void creatGroup(CommunicationCommand cmd, ActorRef sender){

        if (setGroup(cmd))
            cmd.setResult(true, cmd.getGroupName() + " created successfully!");
        else
            cmd.setResult(false, cmd.getGroupName() + " already exists!");

        sendBack(cmd, sender);
    }

    //remove target user from group if the source user have the
    //needed privileges and the target user exist in the group
    //else, return false command with the relevant error
    //then sends back the result to the source user
    private void leaveGroup(CommunicationCommand cmd, ActorRef sender){

        Group group = this.groupsMap.get(cmd.getGroupName());
        if (group != null) {
            CommunicationCommand result = group.leaveGroup(cmd);
            //close the group if the admin removed
            if (result.isSucceed() && result.getResult().equals("Admin")) {
                this.groupsMap.remove(cmd.getGroupName());
                result.setResult(true, cmd.getGroupName()
                        + " admin has closed " + cmd.getGroupName() + "!");
            }
            //broadcast the relevant message to the group members
            group.getRouter().route(result, self());
            sendBack(result, sender);
        }
        else {
            cmd.setResult(false, "Group " + cmd.getGroupName() + " does not exists!");
            sendBack(cmd, sender);
        }
    }

    //return succeed command if:
    //group exist, source user exist and source user is admin or co-admin
    //else, return false command with the relevant result message
    private GroupCommand validateCmd(GroupCommand cmd, Group group){
        if (group != null){
            Group.PType sourceType = group.getUserPType(cmd.getSource().getUserName());
            if (sourceType.equals(Group.PType.Admin) || sourceType.equals(Group.PType.Co_Admin)){
                GroupCommand withRefCmd = (GroupCommand) getTargetUser(cmd, cmd.getTarget());
                return withRefCmd;
            } else
                cmd.setResult(false, "You are neither an admin nor a co-admin of " + cmd.getGroupName() + "!");
        } else
            cmd.setResult(false, cmd.getGroupName() + " does not exist!");
        return cmd;
    }

    //validate the invitation GroupCommand
    //if the command has passed the validation,
    //then, if the target exits and not in the group
    //the server adds the target user to the group
    //and send back the invitation message or false message
    //back to the sender.
    private void inviteToGroup(GroupCommand cmd, ActorRef sender){
        Group group = this.groupsMap.get(cmd.getGroupName());
        GroupCommand validCmd = validateCmd(cmd, group);
        if (validCmd.isSucceed()){
            Group.PType targetType = group.getUserPType(cmd.getTarget().getUserName());
            if (targetType.equals(Group.PType.Not_User)){
                validCmd.setResult(true, "You have been invited to " + cmd.getGroupName() + ", Accept?");
            } else
                validCmd.setResult(false, cmd.getTarget().getUserName() +
                        " is already in " + cmd.getGroupName() + "!");
        }
        sendBack(validCmd, sender);
    }

    //validate the removeUser GroupCommand
    //if the command has passed the validation,
    //then, if the target exits in the group
    //the server removes the target user from the group
    //and send back the relevant message back to the sender.
    private void removeFromGroup(GroupCommand cmd, ActorRef sender){
        Group group = this.groupsMap.get(cmd.getGroupName());
        GroupCommand validCmd = validateCmd(cmd, group);
        if (validCmd.isSucceed()){
            Group.PType targetType = group.getUserPType(cmd.getTarget().getUserName());
            if (targetType.equals(Group.PType.Admin))
                validCmd.setResult(false, "You can not remove the admin of the group!");
            else if (targetType.equals(Group.PType.Not_User)){
                validCmd.setResult(false, validCmd.getTarget().getUserName() + " is not in " +
                        validCmd.getGroupName() + " group!");
            }else {
                validCmd.setTarget(validCmd.getUserResult());
                validCmd.setResult(true, "");
                group.removeUser(validCmd.getTarget());
            }
        }
        sendBack(validCmd, sender);
    }

    //validate the promote/demote GroupCommand
    //if the command has passed the validation,
    //then, if the target exits in the group
    //the server promote/demote the target user in the group
    //and send back the relevant message back to the source user.
    private void changePreviledges(GroupCommand cmd, ActorRef sender) {
        Group group = this.groupsMap.get(cmd.getGroupName());
        GroupCommand validCmd = validateCmd(cmd, group);
        if (validCmd.isSucceed()){
            Group.PType targetType = group.getUserPType(cmd.getTarget().getUserName());
            if (targetType.equals(Group.PType.Not_User)){
                validCmd.setResult(false, validCmd.getTarget().getUserName() + " is not in " +
                        validCmd.getGroupName() + " group!");
            }else {
                validCmd.setTarget(validCmd.getUserResult());
                validCmd.setResult(true, "");

                if (cmd.getType().equals(Command.Type.Group_Promote))
                    group.promoteUser(validCmd.getTarget());
                else if (cmd.getType().equals(Command.Type.Group_Demote))
                    group.demoteUser(validCmd.getTarget());

            }
        }
        sendBack(validCmd, sender);
    }

    //adds user to group while the invitation already
    //validate by the source user and the the server itself
    //and send the target "Welcome" message
    private void addUserToGroup(Invitation inv) {
        Group group = this.groupsMap.get(inv.getGroupName());
        if (group != null){
            User target = this.usersMap.get(inv.getTarget().getUserName());
            if (target != null) {
                group.addUser(inv.getTarget());
                inv.setAnswer("Welcome to " + inv.getGroupName() + "!");
                target.getUserRef().tell(inv, self());
            }
        }
    }

    //return the command group name depends on the command instance
    private String getGroupName(Command cmd){
        String name = "";

        if (cmd instanceof TextCommand)
            name = ((TextCommand) cmd).getTarget().getUserName();
        else if (cmd instanceof FileCommand)
            name =  ((FileCommand) cmd).getTarget().getUserName();

        return name;
    }

    //return the command source name depends on the command instance
    private String getsourceName(Command cmd){
        String name = "";

        if (cmd instanceof TextCommand)
            name = ((TextCommand) cmd).getSource().getUserName();
        else if (cmd instanceof FileCommand)
            name =  ((FileCommand) cmd).getSource().getUserName();

        return name;
    }

    //if the target user belongs to the group and
    //the source user isn't muted, the server
    //sends the file/text message to all the group
    //and the server send the relevant result to the sender
    private void groupMessage(Command cmd, ActorRef sender){
        String sourceName = getsourceName(cmd);
        String groupName = getGroupName(cmd);

        Group group = this.groupsMap.get(groupName);
        if (group != null){
            Group.PType sourceType = group.getUserPType(sourceName);
            if (!sourceType.equals(Group.PType.Not_User)){
                if (!sourceType.equals(Group.PType.Muted)) {
                    cmd.setResult(true, "");
                    group.getRouter().route(cmd, self());
                }else
                    cmd.setResult(false, "You are muted for " + group.getMutedTime(sourceName) +  " in " + groupName + "!");
            } else
                cmd.setResult(false, "You are not part of " + groupName + "!");
        }else
            cmd.setResult(false, groupName + " does not exist!");

        sendBack(cmd, sender);
    }

    //return "time is up!" TextCommand
    private TextCommand muteTimeIsUp(GroupCommand cmd){
        TextCommand textCmd =  new TextCommand(cmd.getSource(), cmd.getTarget(),
                "You have been unmuted! Muting time is up!");
        textCmd.setType(Command.Type.Group_UnMute);
        return textCmd;
    }

    //return muted message TextCommand
    private TextCommand mutedMessage(GroupCommand cmd, long mutedTime){
        TextCommand textCmd =  new TextCommand(cmd.getSource(), new User(cmd.getGroupName()),
                "You have been muted for " + mutedTime + " in "
                        + cmd.getGroupName() + " by " + cmd.getSource().getUserName() + "!");
        return setGroupTextProps(textCmd, cmd);
    }

    //return unmuted message TextCommand
    private TextCommand unMuteMessage(GroupCommand cmd){
        TextCommand textCmd =  new TextCommand(cmd.getSource(), new User(cmd.getGroupName()),
                "You have been unmuted in " + cmd.getGroupName() +
                        " by " +  cmd.getSource().getUserName() + "!");
        return setGroupTextProps(textCmd, cmd);
    }

    //set TextCommand User result to target User,
    //succeeded to true, and command type to Group_Text
    private TextCommand setGroupTextProps(TextCommand toSet, GroupCommand values) {
        toSet.setUserResult(true, values.getTarget());
        toSet.setResult(true, "");
        toSet.setType(Command.Type.Group_Text);
        return toSet;
    }

    //validate the mute user GroupCommand
    //if the command has passed the validation,
    //then, if the target exits in the group
    //the server mutes the target user in the group,
    //sets lambda to send unmute the target after duration time
    //and send back the relevant message back to the source user.
    private void muteUser(GroupCommand cmd, ActorRef sender){
        Group group = this.groupsMap.get(cmd.getGroupName());
        GroupCommand validCmd = validateCmd(cmd, group);
        if (validCmd.isSucceed()){
            validCmd.setTarget(validCmd.getUserResult());
            Group.PType targetType = group.getUserPType(cmd.getTarget().getUserName());
            if (targetType.equals(Group.PType.Not_User)) {
                validCmd.setResult(false, validCmd.getTarget().getUserName() + " is not in " +
                        validCmd.getGroupName() + " group!");
            } else {
                Cancellable cancel = scheduler.scheduleOnce(validCmd.getDuration(),() -> {
                    group.unMuteUser(validCmd.getTarget()); //Unmute user
                    sendBack(muteTimeIsUp(validCmd), validCmd.getTarget().getUserRef()); //inform the target about unmute
                },context().system().dispatcher());
                //mute target to duration time;
                group.muteUser(validCmd.getTarget(),validCmd.getDuration(),cancel);
                TextCommand textCmd = mutedMessage(validCmd, group.getMutedTime(validCmd.getTarget().getUserName()));
                sendBack(textCmd, sender);
                return;
            }
        }
        //send back to sender with false result
        sendBack(validCmd, sender);
    }

    //validate the unmute GroupCommand
    //if the command has passed the validation,
    //then, if the target exits in the group and muted
    //the server set back the target user privilege in the group
    //and send back the relevant message back to the source user.
    private void unmuteUser(GroupCommand cmd, ActorRef sender){
        Group group = this.groupsMap.get(cmd.getGroupName());
        GroupCommand validCmd = validateCmd(cmd, group);
        if (validCmd.isSucceed()){
            validCmd.setTarget(validCmd.getUserResult());
            Group.PType targetType = group.getUserPType(cmd.getTarget().getUserName());
            if (targetType.equals(Group.PType.Not_User)) {
                validCmd.setResult(false, validCmd.getTarget().getUserName() + " is not in " +
                        validCmd.getGroupName() + " group!");
            }else if (!targetType.equals(Group.PType.Muted)) {
                validCmd.setResult(false, validCmd.getTarget().getUserName() + " is not muted!");
            } else {
                //unmute target
                group.unMuteUser(validCmd.getTarget());
                TextCommand textCmd = unMuteMessage(validCmd);
                sendBack(textCmd, sender);
                return;
            }
        }
        //send back to sender with false result
        sendBack(validCmd, sender);
    }



    public Receive createReceive(){

        return receiveBuilder()
                .match(CommunicationCommand.class, preds.connectCmd, (cmd) -> connectUser(cmd, sender()))
                .match(CommunicationCommand.class, preds.disconnectCmd, (cmd) -> disconnectUser(cmd, sender()))
                .match(CommunicationCommand.class, preds.createGroupCmd, (cmd) -> creatGroup(cmd, sender()))
                .match(CommunicationCommand.class, preds.leaveGroupCmd, (cmd) -> leaveGroup(cmd, sender()))
                .match(GroupCommand.class, preds.inviteToGroup, (cmd) -> inviteToGroup(cmd, sender()))
                .match(GroupCommand.class, preds.removeFromGroup, (cmd) -> removeFromGroup(cmd, sender()))
                .match(GroupCommand.class, preds.groupPreviledg, (cmd) -> changePreviledges(cmd, sender()))
                .match(GroupCommand.class, preds.muteUser, (cmd) -> muteUser(cmd, sender()))
                .match(GroupCommand.class, preds.unmuteUser, (cmd) -> unmuteUser(cmd, sender()))
                .match(Invitation.class, this::addUserToGroup)
                .match(Command.class, preds.gourpMessage, (cmd) -> groupMessage(cmd, sender()))
                .match(TextCommand.class, (cmd) -> userMessage(cmd, sender()))
                .match(FileCommand.class, (cmd) -> userFile(cmd, sender()))
                .matchAny(System.out::println)
                .build();
    }
}