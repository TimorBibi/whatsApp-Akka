import java.time.Duration;
import java.io.Serializable;


public class GroupCommand extends Command implements Serializable {

    private User source;
    private String groupName;
    private User target;
    private Duration duration;

    public GroupCommand(String[] str, From from, String sourceName,  Type type){
        super(type, from);
        try {
            this.source = new User(sourceName);
            this.groupName = str[0];
            this.target = new User(str[1]);
            if (type.equals(Type.Group_Mute)) {
                this.duration = Duration.ofSeconds(Integer.parseInt(str[2]));
            }
        }
        catch (Exception e) {
            this.setType(Type.Error);
            this.setFrom(From.IO);
            this.setResult(false, "Invalid command");
        }
    }

    public User getSource(){
        return source;
    }
    public String getGroupName() {
        return groupName;
    }
    public User getTarget(){
        return target;
    }
    public void setSource(User user){
        this.source = user;
    }
    public void setTarget(User target) {this.target = target;}

    public Duration getDuration(){
        return this.duration;
    }
}
