./gradlew clean build
./gradlew runExperiment --stacktrace -Pdataset=cora -Pnfeat=1433 -Pnhid=16 -Pnlayer=2 -Pnclass=7 -PexpName=labProp
#./gradlew runExperiment --stacktrace -Pdataset=texas -Pnfeat=1703 -Pnhid=16 -Pnlayer=2 -Pnclass=5 -PexpName=homProp
