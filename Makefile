.PHONY: clean

all: clean target/scala-2.12/perftest.jar

clean:
	rm -rf target dist

target/scala-2.12/perftest.jar:
	sbt assembly
