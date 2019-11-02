import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.FI;

import java.util.Arrays;

public class ioActor extends AbstractActor {

    private final ActorRef clientHandler;
    private String userName;

    static public Props props(ActorRef acRef){
        return Props.create(ioActor.class, () -> new ioActor(acRef));
    }

    public ioActor(ActorRef acRef){
        this.clientHandler = acRef;
    }

    private void print(String str){
        System.out.println(str);
    }

    private void setUserName(String connectStr){
        this.userName = connectStr.split(" ")[0];
    }

    private boolean isValid(String[] msg){
        return msg.length > 2;
    }

    private Command getErrorCmd(){
        Command cmd = new Command(Command.Type.Error, Command.From.IO);
        cmd.setResult(false, "Invalid command");
        return cmd;
    }

    private void setCommand(String[] msg){
        switch (msg[0]){
            case "/user":
                sendToClient(userSwitch(msg));
                break;
            case "/group":
                sendToClient(groupSwitch(msg));
                break;
            case "Yes":
                sendToClient(new Invitation(this.userName, "Yes"));
                break;
            case "No":
                sendToClient(new Invitation(this.userName, "No"));
                break;
            default:
                sendToClient(new Command(Command.Type.Error, Command.From.IO));
                break;
        }
    }

    private Command userSwitch(String[] msg){
        Command cmd;
        switch (msg[1]) {
            case "connect":
                if (isValid(msg))
                    cmd = new CommunicationCommand(Arrays.copyOfRange(msg, 2, msg.length), Command.From.IO);
                else
                    cmd = getErrorCmd();
                break;
            case "text":
                if (isValid(msg))
                    cmd = new TextCommand(Arrays.copyOfRange(msg, 2, msg.length), Command.From.IO, this.userName);
                else
                    cmd = getErrorCmd();
                break;
            case "file":
                if (isValid(msg))
                    cmd = new FileCommand(Arrays.copyOfRange(msg, 2, msg.length), Command.From.IO, this.userName);
                else
                    cmd = getErrorCmd();
                break;
            case "disconnect":
                cmd = new CommunicationCommand(new String[]{userName}, Command.From.IO, Command.Type.Disconnect);
                break;
            default:
                cmd = new Command(Command.Type.Error, Command.From.IO);
                cmd.setResult(false, "Invalid command");
                break;
        }
        return cmd;
    }

    private Command groupSwitch(String[] msg){
        Command cmd;
        switch (msg[1]) {
            case "create":
                if (isValid(msg))
                    cmd = new CommunicationCommand(new String[]{userName, msg[2]}, Command.From.IO, Command.Type.Group_Create);
                else
                    cmd = getErrorCmd();
                break;
            case "leave":
                if (isValid(msg))
                    cmd = new CommunicationCommand(new String[]{userName, msg[2]}, Command.From.IO, Command.Type.Group_Leave);
                else
                    cmd = getErrorCmd();
                break;
            case "send":
                if (isValid(Arrays.copyOfRange(msg, 2, msg.length))) {
                    if (msg[2].equals("text"))
                        cmd = new TextCommand(Arrays.copyOfRange(msg, 3, msg.length), Command.From.IO, this.userName, Command.Type.Group_Text);
                    else if (msg[2].equals(("file")))
                        cmd = new FileCommand(Arrays.copyOfRange(msg, 3, msg.length), Command.From.IO, this.userName, Command.Type.Group_File);
                    else
                        cmd = getErrorCmd();
                } else
                    cmd = getErrorCmd();
                break;
            case "user":
                if (isValid(Arrays.copyOfRange(msg, 2, msg.length))) {
                    if (msg[2].equals("invite"))
                        cmd = new GroupCommand(Arrays.copyOfRange(msg, 3, msg.length), Command.From.IO, this.userName, Command.Type.Group_Invite);
                    else if (msg[2].equals(("remove")))
                        cmd = new GroupCommand(Arrays.copyOfRange(msg, 3, msg.length), Command.From.IO, this.userName, Command.Type.Group_Remove);
                    else if (msg[2].equals(("mute")))
                        cmd = new GroupCommand(Arrays.copyOfRange(msg, 3, msg.length), Command.From.IO, this.userName, Command.Type.Group_Mute);
                    else if (msg[2].equals(("unmute")))
                        cmd = new GroupCommand(Arrays.copyOfRange(msg, 3, msg.length), Command.From.IO, this.userName, Command.Type.Group_UnMute);
                    else
                        cmd = getErrorCmd();
                } else
                    cmd = getErrorCmd();
                break;
            case "coadmin":
                if (isValid(Arrays.copyOfRange(msg, 2, msg.length))) {
                    if (msg[2].equals("add"))
                        cmd = new GroupCommand(Arrays.copyOfRange(msg, 3, msg.length), Command.From.IO, this.userName, Command.Type.Group_Promote);
                    else if (msg[2].equals(("remove")))
                        cmd = new GroupCommand(Arrays.copyOfRange(msg, 3, msg.length), Command.From.IO, this.userName, Command.Type.Group_Demote);
                    else
                        cmd = getErrorCmd();
                } else
                    cmd = getErrorCmd();
                break;
            default:
                cmd = new Command(Command.Type.Error, Command.From.IO);
                cmd.setResult(false, "Invalid command");
                break;
        }
        return cmd;
    }

    //send the relevant command to the client to handle
    private void sendToClient(Command cmd){
        if (!cmd.getType().equals(Command.Type.Error)){
            clientHandler.tell(cmd, self());
        }
        else
            print("Invalid command");
    }

    //send answered invitation to client to handle
    private void sendToClient(Invitation inv){
        if (!inv.getType().equals(Command.Type.Error)){
            clientHandler.tell(inv, self());
        }
        else
            print("Invalid command");
    }

    public Receive createReceive() {
        FI.TypedPredicate<Command> connectCmd = cmd -> cmd.getType().equals(Command.Type.Connect);

        return receiveBuilder()
                .match(String.class, (msg) -> setCommand(msg.split(" ")))
                .match(Command.class, connectCmd, (cmd) -> {setUserName(cmd.getResult()); print(cmd.getResult());})
                .match(Command.class, (cmd) -> print(cmd.getResult()))
                .matchAny((cmd) -> {System.out.println("Invalid Command");})
                .build();
    }
}