#!/usr/bin/env bash

dirname='902312911-jmcgehee3-hw3'


rm -r $dirname
rm -r $dirname.tar.gz

mkdir $dirname

cp -r code/src $dirname/src
cp -r code/sbt $dirname/sbt
cp -r code/project $dirname/project
cp code/build.sbt $dirname/build.sbt
cp homework3answer.pdf $dirname/homework3answer.pdf

tar -czvf $dirname.tar.gz $dirname

rm -r $dirname



