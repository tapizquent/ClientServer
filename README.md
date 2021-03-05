# ClientServer
A client server project example written in Java.

Allows for a Server socket to process request and provide files in response to client messages

Client and Server source files can be found in their respective directories. Each directory contains a Files
subdirectory that holds the files at the Server and Client respectively.

# GETTING STARTED:

In order to run, first run the command:
    `make`
from the Source directory

Once the executables have been genenrated, run
    `java Server` or `java Server DEBUG=1`
from the Server directory

Once the server is running, the Client can be run to upload or download files from the Server.
To do this run
    `java Client <filename>` or `java Client -w <filename>`
from the Client directory