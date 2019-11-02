import akka.japi.pf.FI;

public class Predicates {

    public FI.TypedPredicate<CommunicationCommand> connectCmd;
    public FI.TypedPredicate<CommunicationCommand> disconnectCmd;
    public FI.TypedPredicate<CommunicationCommand> communGroupFromIO;
    public FI.TypedPredicate<Command> userTextOrFileIO;
    public FI.TypedPredicate<TextCommand> receiveTextClient;
    public FI.TypedPredicate<FileCommand> receiveFileClient;
    public FI.TypedPredicate<TextCommand> groupTextFromIO;
    public FI.TypedPredicate<FileCommand> groupFileFromIO;
    public FI.TypedPredicate<CommunicationCommand> leaveNotFromIO;
    public FI.TypedPredicate<GroupCommand> groupInviteIO;
    public FI.TypedPredicate<GroupCommand> groupRemoveIO;
    public FI.TypedPredicate<TextCommand> receiveTextGroup;
    public FI.TypedPredicate<FileCommand> receiveFileGroup;
    public FI.TypedPredicate<GroupCommand> groupPromteIO;
    public FI.TypedPredicate<GroupCommand> groupDemoteIO;
    public FI.TypedPredicate<GroupCommand> muteGroup;
    public FI.TypedPredicate<TextCommand> muteTimeIsUp;

    //Server predicate
    public FI.TypedPredicate<CommunicationCommand> createGroupCmd;
    public FI.TypedPredicate<CommunicationCommand> leaveGroupCmd;
    public FI.TypedPredicate<GroupCommand> inviteToGroup;
    public FI.TypedPredicate<GroupCommand> removeFromGroup;
    public FI.TypedPredicate<Command> gourpMessage;
    public FI.TypedPredicate<GroupCommand> groupPreviledg;
    public FI.TypedPredicate<GroupCommand> muteUser;
    public FI.TypedPredicate<GroupCommand> unmuteUser;

    public Predicates() {
        //Client predicates
        connectCmd = cmd -> cmd.getType().equals(Command.Type.Connect);
        disconnectCmd = cmd -> cmd.getType().equals(Command.Type.Disconnect);
        communGroupFromIO = cmd -> cmd.getFrom().equals(Command.From.IO)
                && (cmd.getType().equals(Command.Type.Group_Create)
                || cmd.getType().equals(Command.Type.Group_Leave));
        userTextOrFileIO = cmd -> cmd.getFrom().equals(Command.From.IO)
                && (cmd.getType().equals(Command.Type.User_Text)
                || cmd.getType().equals(Command.Type.User_File));
        receiveTextClient = cmd -> cmd.getFrom().equals(Command.From.Client);
        receiveFileClient = cmd -> cmd.getFrom().equals(Command.From.Client);
        groupTextFromIO = cmd -> cmd.getFrom().equals(Command.From.IO)
                && cmd.getType().equals(Command.Type.Group_Text);
        groupFileFromIO = cmd -> cmd.getFrom().equals(Command.From.IO)
                && cmd.getType().equals(Command.Type.Group_File);
        leaveNotFromIO = cmd -> !cmd.getFrom().equals(Command.From.IO)
                && cmd.getType().equals(Command.Type.Group_Leave);
        groupInviteIO = cmd -> cmd.getFrom().equals(Command.From.IO)
                && cmd.getType().equals(Command.Type.Group_Invite);
        groupRemoveIO = cmd -> cmd.getFrom().equals(Command.From.IO)
                && cmd.getType().equals(Command.Type.Group_Remove);
        receiveTextGroup = cmd -> cmd.getType().equals(Command.Type.Group_Text);
        receiveFileGroup = cmd -> cmd.getType().equals(Command.Type.Group_File);
        groupPromteIO = cmd -> cmd.getFrom().equals(Command.From.IO)
                && cmd.getType().equals(Command.Type.Group_Promote);
        groupDemoteIO = cmd -> cmd.getFrom().equals(Command.From.IO)
                && cmd.getType().equals(Command.Type.Group_Demote);
        muteGroup = cmd -> cmd.getFrom().equals(Command.From.IO)
                && (cmd.getType().equals(Command.Type.Group_Mute)
                || cmd.getType().equals(Command.Type.Group_UnMute));
        muteTimeIsUp = cmd -> cmd.getFrom().equals(Command.From.Server)
                && cmd.getType().equals(Command.Type.Group_UnMute);

        //Server predicate
        createGroupCmd = cmd -> cmd.getType().equals(Command.Type.Group_Create);
        leaveGroupCmd = cmd -> cmd.getType().equals(Command.Type.Group_Leave);
        inviteToGroup = cmd -> cmd.getType().equals(Command.Type.Group_Invite);
        removeFromGroup = cmd -> cmd.getType().equals(Command.Type.Group_Remove);
        gourpMessage = cmd -> cmd.getType().equals(Command.Type.Group_Text)
                || cmd.getType().equals(Command.Type.Group_File);
        groupPreviledg = cmd -> cmd.getType().equals(Command.Type.Group_Promote)
                || cmd.getType().equals(Command.Type.Group_Demote);
        muteUser = cmd -> cmd.getType().equals(Command.Type.Group_Mute);
        unmuteUser = cmd -> cmd.getType().equals(Command.Type.Group_UnMute);
    }
}