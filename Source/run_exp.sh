./gradlew clean build
./gradlew runExperiment --stacktrace -Pdataset=cora -Pnfeat=1433 -PmodelName=GGCN -Pnhid=16 -Pnlayer=32 -Pnclass=7 -PdecayRate=0.9 -PexpName=labProp
#./gradlew runExperiment --stacktrace -Pdataset=texas -Pnfeat=1703 -PmodelName=GCN -Pnhid=16 -Pnlayer=2 -Pnclass=5 -PexpName=homProp
