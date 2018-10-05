# mis-2018-final-project

This Project consists of a client and a server project.

The client project is available at (https://github.com/JaniTomati/mis-2018-final-project)
as an android studio application.
 
The server project is available at (https://github.com/Jojooo0o/mis-2018-final-project-server)
and was created with IntelliJ IDEA.

To get full functionality you need both repositories.

# Setup

## Client side

In case you don't want to change anything at the client side you can simply install
the apk file on your android device.
The compileSdkVersion is 25. Please check the code for further information.

Once installed you can scan a QR code via the scan button. When the scan is completed
the scanned information will be stored in a member variable and can than be used with
the connect button to connect to a specific server.

To transfer the important information, the QR code needs to contain a json  with the
following scheme:
> {"server_ip" : "local_ip_of_server","server_port" : "port_of_server"}

Example:
> {"server_ip" : "192.168.0.10","server_port" : "8090"}

The default port set by the server is 8090.

After connecting to one server you may switch the server by scanning a new QR code and
repeating the process. 

## Server side

To run the server you simply have to compile the RemoteServer.java and the ClientHandler.java
class.

After compilation the server can be started by running the RemoteServer. This will launch
a local server accessable using the local ip and the default set port (8090).
To change the port, just configure the RemoteServer.java file.

The server will send images continuously to a client and waits for an input string 
as soon as a client connects and will only stop by disconnect.

Generate a QR code with the corresponding information of the server machine to connect to
the server via the client as it is described above.

## Notes

- The project still got some bugs which are described in our documentation.

- The performance of the broadcast is highly depending on the network capacity.

- This project was created as a final project for a university lecture.



