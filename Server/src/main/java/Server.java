import akka.actor.ActorSystem;
import akka.actor.Props;


public class Server {

    public static void main(String[] args) {
        //creating the system
        ActorSystem system = ActorSystem.create("ServerSystem");
        //creating system actors
        system.actorOf(Props.create(ServerActor.class), "Server");
    }
}