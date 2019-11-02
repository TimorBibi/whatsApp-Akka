import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;


public class FileCommand extends Command {

    private User target;
    private User source;
    private String sourceFilePath;
    private byte[] file;
    private String fileName;
    private String targetFilePath;

    public FileCommand(String[] str, Command.From from, String source){
        super(Command.Type.User_File, from);
        try {
            this.target = new User(str[0]);
            this.source = new User(source);
            this.sourceFilePath = str[1];
            setFileName(sourceFilePath);
            getFile(sourceFilePath);
            this.targetFilePath = "Client/src/downloads/" + this.fileName;
        }
        catch (Exception e){
            this.setType(Type.Error);
            this.setFrom(From.IO);
            this.setResult(false, "Invalid file path");
        }
    }

    public FileCommand(String[] str, Command.From from, String source, Type type){

        super(type, from);
        try {
            this.target = new User(str[0]);
            this.source = new User(source);
            this.sourceFilePath = str[1];
            setFileName(sourceFilePath);
            getFile(sourceFilePath);
            this.targetFilePath = "Client/src/downloads/" + this.fileName;
        }
        catch (Exception e){
            this.setType(Type.Error);
            this.setFrom(From.IO);
            this.setResult(false, "Invalid file path");
        }
    }

    public User getTarget(){
        return this.target;
    }

    public User getSource(){
        return this.source;
    }

    public byte[] getFile(){
        return this.file;
    }

    public String getTargetFilePath(){
        return this.targetFilePath;
    }

    private void setFileName(String path){
        try {
            String[] splitPath = path.split("/");
            String fullName = splitPath[splitPath.length - 1];
            String[] splitName = fullName.split("\\.");
            String fName = String.join(".", Arrays.copyOfRange(splitName, 0, splitName.length - 2));
            String fType = splitName[splitName.length - 1];
            File tmpFile = new File("Client/src/downloads/" + String.join(".", fullName));
            int i = 1;
            while (tmpFile.exists()) {
                fullName = fName + "(" + i++ + ")." + fType;
                tmpFile = new File("Client/src/downloads/" + fullName);
            }
            this.fileName = fullName;
        }
        catch (Exception e){
            this.setType(Type.Error);
            this.setFrom(From.IO);
            this.setResult(false, "Invalid file path");
        }
    }

    private void getFile(String path){
        try {
            byte[] bFile = Files.readAllBytes(new File(path).toPath());
            this.file = bFile;
            setResult(true, "File received: " + targetFilePath);

        }catch (Exception e){
            setResult(false,sourceFilePath + "does not exist!");
        }
    }

}
