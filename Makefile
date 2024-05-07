.PHONY: benchmark-distance
benchmark-distance: DistanceBenchmark.class
	java --add-modules jdk.incubator.vector DistanceBenchmark

benchmark-distance-nosimd: DistanceBenchmark.class
	java -XX:-UseSuperWord --add-modules jdk.incubator.vector DistanceBenchmark

DistanceBenchmark.class: DistanceBenchmark.java
	javac --add-modules jdk.incubator.vector $<

clean:
	rm -f *.class
