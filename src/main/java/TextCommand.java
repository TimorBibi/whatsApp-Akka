
import java.io.Serializable;
import java.util.Arrays;

public class TextCommand extends Command implements Serializable {

    private User target;
    private String message;
    private User source;

    public TextCommand(String[] str, From from, String source){
        super(Type.User_Text, from);
        this.target = new User(str[0]);
        String [] msgArr = Arrays.copyOfRange(str, 1, str.length);
        this.message = String.join(" ", msgArr);
        this.source = new User(source);
    }

    public TextCommand(String[] str, From from, String source, Type type){
        super(type, from);
        this.target = new User(str[0]);
        String [] msgArr = Arrays.copyOfRange(str, 1, str.length);
        this.message = String.join(" ", msgArr);
        this.source = new User(source);
    }

    public TextCommand(User source, User target, String message){
        super(Type.Group_Remove, From.Client);
        this.target = target;
        this.source = source;
        this.message = message;
    }

    public User getTarget(){
        return this.target;
    }

    public String getMessage(){
        return this.message;
    }

    public User getSource(){
        return this.source;
    }

}