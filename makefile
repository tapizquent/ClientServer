JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
<<<<<<< HEAD
	Server/Server.java \
	Client/Client.java
=======
	Server.java \
	Client.java
>>>>>>> 37158dab2bd79cf0dfa33bf538083a0e5041f728

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
