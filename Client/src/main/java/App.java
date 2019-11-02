import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.util.Scanner;


public class App {

    public static void main(String args[]){

        boolean toQuit = false;
        String input;
        Scanner scan = new Scanner(System.in);

        ActorSystem system = ActorSystem.create("ClientWhatsApp");

        ActorRef clientHandler = system.actorOf(Props.create(ClientActor.class), "clientHandler");
        ActorRef ioHandler = system.actorOf(Props.create(ioActor.class, clientHandler), "ioHandler");


        System.out.println("Enter \"/user connect <username>\" to connect to the server");

        while(!toQuit){

            input = scan.nextLine();
            if (input.equals("quit")){
                toQuit = true;
            }
            else {
                ioHandler.tell(input, ioHandler);
            }
        }

    }
}

