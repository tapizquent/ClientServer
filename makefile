JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Server/Server.java \
	Client/Client.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
