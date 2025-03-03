./gradlew clean build
#./gradlew runExperiment --stacktrace -Pdataset=cora -Pnfeat=1433 -PmodelName=GGCN -Pnhid=16 -Pnlayer=32 -Pnclass=7 -PdecayRate=0.9 -PexpName=realDataset -PhomType=hom
#./gradlew runExperiment --stacktrace -Pdataset=texas -Pnfeat=1703 -PmodelName=GCN -Pnhid=16 -Pnlayer=2 -Pnclass=5 -PexpName=homProp


#./gradlew runExperiment --stacktrace -Pdataset=texas -Pnfeat=1703 -PmodelName=GCN -Pnhid=16 -Pnlayer=2 -Pnclass=5 -PdecayRate=1.0 -PexpName=realDataset -PhomType=homProp
#./gradlew runExperiment --stacktrace -Pdataset=wisconsin -Pnfeat=1703 -PmodelName=GCN -Pnhid=16 -Pnlayer=2 -Pnclass=5 -PdecayRate=1.0 -PexpName=realDataset -PhomType=homProp
#./gradlew runExperiment --stacktrace -Pdataset=texas -Pnfeat=1703 -PmodelName=GGCN -Pnhid=16 -Pnlayer=2 -Pnclass=5 -PdecayRate=1.0 -PexpName=realDataset -PhomType=homProp
./gradlew runExperiment --stacktrace -Pdataset=wisconsin -Pnfeat=1703 -PmodelName=GGCN -Pnhid=16 -Pnlayer=5 -Pnclass=5 -PdecayRate=1.0 -PexpName=realDataset -PhomType=homProp

#./gradlew runExperiment --stacktrace -Pdataset=cornell -Pnfeat=1703 -PmodelName=GCN -Pnhid=16 -Pnlayer=2 -Pnclass=5 -PdecayRate=1.0 -PexpName=realDataset -PhomType=homProp
#./gradlew runExperiment --stacktrace -Pdataset=cornell -Pnfeat=1703 -PmodelName=GGCN -Pnhid=16 -Pnlayer=6 -Pnclass=5 -PdecayRate=0.7 -PexpName=realDataset -PhomType=homProp

#./gradlew runExperiment --stacktrace -Pdataset=cora -Pnfeat=1433 -PmodelName=GCN -Pnhid=16 -Pnlayer=2 -Pnclass=7 -PdecayRate=1.0 -PexpName=realDataset -PhomType=homProp
#./gradlew runExperiment --stacktrace -Pdataset=cora -Pnfeat=1433 -PmodelName=GGCN -Pnhid=16 -Pnlayer=32 -Pnclass=7 -PdecayRate=0.9 -PexpName=realDataset -PhomType=homProp

#./gradlew runExperiment --stacktrace -Pdataset=pubmed -Pnfeat=500 -PmodelName=GCN -Pnhid=16 -Pnlayer=2 -Pnclass=3 -PdecayRate=1.0 -PexpName=realDataset -PhomType=homProp