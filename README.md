Timor Bibi

Implementation:
    There are 3 types of actors in my implementation:

1. ioActor:
    a. The actor receives incoming string messages from the main stdin,
       create the parse and analyse the string and send the relevant Command
       to the Client Actor.

    b. The actor receives Commands from the Client Actor,
       those commands includes result which the ioHandler print to stdout.

2. ClientActor:
    a. The client actor receives commands from the ioHandler actor,
       then the client actor handle the command depends on the
       command type and a matching predicate.
       Handling the command made by sending the command, using akka ask, to the server
       after manipulate it and wait to result command from the server.

    b. While getting the result command from the server, the Client actor
       works according to the result.
       If the result command from the server succeeded, the actor then use the result
       to finish the command task.
       Else the actor will print the false result that returned from the server
       which includes the reason of the command failure in the server.

3. ServerActor:
    The server actor holds the users map and groups map, each map holds the relevant class,
    User of Group.
    Those maps includes only connected users by there names or existing groups by there names.
    The server receives commands from users which includes all the required information to
    achieve the command goal.
    The server analyse the command using its type and predicate.
    a.  User Commands:
         The server handel user command by finding the user target in its maps and send the
         target ActorRef back to the user who asks for it within the result command which also
         includes all the relevant info for the user to achieve its goal.
    b. Group Commands:
        Depends on the command demands, in times the server acts like in the User commands,
        in other times, while it required, the server preform some changes on the group
        or sends the wanted message or file to the all group using the group Router.

Classes:
    Users represented by User class includes:
        userName - user name
        userRef - user Actor reference
        connected - does the user connected to the system
        invitation - if exists, invitation to group waits to the user to answer
        beforeMute - user pre privilege. use in the group system.

    Group represented by Group class includes:
        groupName - group name
        admin - the name of the only admin in the group
        usersMap - hash map which holds the users name as key and Pair<User, PType> as value
        mutedMap - hash map which holds the muted users name as key and Pair<Duration, Cancellable> as value
        router - the Router of the group, uses by the server to broadcast message or file to the group members

    Commands:
         Commands enum Types {
            Connect, Disconnect, User_Text, User_File,
            Group_Create, Group_Leave, Group_Text, Group_File,
            Group_Invite, Group_Remove, Group_Promote, Group_Demote,
            Group_Mute, Group_UnMute,
            Answer, Invitation, Error
        }

        Each command extends the basic Command class includes:
            type - holds the command type
            from - Server/ IO/ Client/ Group
            succeed - true/ false
            result - holds the string result
            userResult - holds the User class of the asked target

        CommunicationCommand: used to handle connecting and disconnecting from the server and creation / leave group
        FileCommand: download and holds the file bytes array including the target and source/ group Users/GroupName.
        TextCommand: holds the target, source/ groupName and the String message
        GroupCommand: hold the target, source, groupName, and duration and handles all the group commands
            except for text and file group messages

    Invitation: do not extends Command, independent class which holds an invitation info:
        source - the User source of the invitation
        target - the User target of the invitation
        groupName - the group name which the target invited to
        invitation - invitation string message
        answer - invitation string answer "Yes"/"No"
        answered - boolean.
        type - Invitation/Answer

Project structure:
    The extracted folder contains:
        Server folder, containing server implementation.
        Client folder, containing client implementation.
        src folder, containing all the classes mentioned above.
        pox.xml file which includes the above Server module and Client module.
        This README.





