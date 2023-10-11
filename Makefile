.PHONY: clean

all: clean target/scala-2.13/perftest.jar

clean:
	rm -rf target dist

target/scala-2.13/perftest.jar:
	sbt assembly
