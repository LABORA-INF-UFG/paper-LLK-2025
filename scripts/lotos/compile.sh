#!/bin/sh
rm -rf ../../bin
mkdir ../../bin
javac -classpath "../../lib/cloudsim-4.0.jar:../../lib/commons-math3-3.6.1.jar:../../lib/jFuzzyLogic_v3.0.jar:../../lib/colt.jar:../../lib/json-20240303.jar" -sourcepath ../../src ../../src/edu/boun/edgecloudsim/applications/lotos/LOTOSMainApp.java -d ../../bin -Xdiags:verbose -Xlint:unchecked
