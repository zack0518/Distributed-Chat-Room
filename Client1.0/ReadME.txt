Getting Started
To launch the client, user has to set arguments that indicates possible username, secret, server host and port.  
The arguments follows the format -h ¡®host¡¯ -p ¡®port¡¯ -u ¡®username¡¯ -s ¡¯secret¡¯ -g ¡®0 or 1¡¯.
Some related argument has default value. User doesn't have to specify every field.
*The defulat value for username is anonymous
*The defulat value for GUI switcher is 1


Switch mode
The client can be run in two modes  -  GUI and Command Line. 
Argument -g 1 indicates the client with GUI which is default mode to run the program.
Without specifying, the client turns on GUI as default. 
To close GUI, user can simply set -g 0 to switch to command line mode.

Make Connection
User can specify host and port on related prompt and click ¡°connect¡± button to connect the server. 
If the connection is successful, dialog will popped up and ¡°send¡± and ¡±disconnect¡± will be enabled.

Sending Command
In Client mode, user can select command type with the combox. 
The combox provides the template of 4 types of command (LOGIN, REGISTER,LOGOUT,ACTIVITY_MESSAGE) that convenient user to enter the command. 
For these commands, user can directly enter the values in the text boxes instead of json commands. 
To send a custom command, use will need to select CUSTOM and fil valid command in json format.


