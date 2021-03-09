# NOTE
Ports are currently set as 2296 for both Client and Server. You can adjust this by going into Server.java and Client.java and changing the port number in the main method.

# ClientServer
A client server project example written in Java.

Allows for a Server socket to process request and provide files in response to client messages

Client and Server source files can be found in their respective directories. Each directory contains a `Files/`
subdirectory that holds the files at the Server and Client respectively.

The Server is capable of handling HTTP/1.1 GET requests as well as multiple connections at the same time.

# GETTING STARTED:

In order to run, first run the command:
    `make`
from the Tapizquent_Jose directory

Once the executables have been generated, `cd` into the `Server/` directory and run
    `java Server` or `java Server DEBUG=1`
from the Server directory to start the Server at port 5000

Once the server is running, the Client can be run to upload or download files from the Server.
To do this, `cd` into `Client/` directory and run
    `java Client <filename>` or `java Client -w <filename>`
from the Client directory

You can also run client as
    `java Client <filename> -s <startByteIndex> -e <endByteIndex>` or `java Client -w <filename> -s <startByteIndex> -e <endByteIndex>`
To only read or write files on a specify range of bytes only, if the whole file is not needed.

# IMPORTANT REMARKS:

Following the project description, the flags provided must be in the right order.
That is, if writing from Client to Server, the `-w` flag must come before the `filename`

In the same manner, when specifying start and end byte indexes, `-s` must come before `e` or the Client won't know how to interpret and will print out instructions on how to call client.

NOTE: Please ensure both `-s` and `-e` are provided! If one of them is missing, it will print an error explaing that range is wrong.