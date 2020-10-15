#!/usr/bin/env bash

ProjectRoot=..
cd $ProjectRoot


## test connection between Java and C++ using basic example
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.BasicExample -Dexec.args="local"
#
####   NO-BATCHING TESTS   ###
#
#### HELIB tests ##
### generate example key pairs
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.KeyGenExample -Dexec.args="local HELIB BGV"
##
### generate example ciphertexts
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.EncDecExample -Dexec.args="local HELIB BGV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt 1000"
##
### run basic FHE arithmetic operation over encrypted data
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.BasicOPsExample -Dexec.args="local 8 HELIB BGV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt"
##
### run FHE dot product over two encrypted vectors
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.DotProductExample -Dexec.args="local 8 HELIB BGV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt"
##
### run FHE total sum over encrypted vector elements
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.TotalSumExample -Dexec.args="local 8 HELIB BGV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt"
##

####   USING BATCHING TESTS   ###

### HELIB tests ##

## BGV Scheme
# generate example key pairs
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.KeyGenExample -Dexec.args="local HELIB BGV"
##
### generate example ciphertexts
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.EncDecExample -Dexec.args="local HELIB BGV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt 1000 1"
##
### run basic FHE arithmetic operation over encrypted data
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.BasicOPsExample -Dexec.args="local 8 HELIB BGV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt 10 1"
##
### run FHE dot product over two encrypted vectors
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.DotProductExample -Dexec.args="local 8 HELIB BGV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt"
##
### run FHE total sum over encrypted vector elements
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.TotalSumExample -Dexec.args="local 8 HELIB BGV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt"


## CKKS Scheme
## generate example key pairs
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.KeyGenExample -Dexec.args="local HELIB CKKS"
#
## generate example ciphertexts
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.EncDecExample -Dexec.args="local HELIB CKKS gen/keys/my_public_key.txt gen/keys/my_secret_key.txt 10 1"
#
## run basic FHE arithmetic operation over encrypted data
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.BasicOPsExample -Dexec.args="local 4 HELIB CKKS gen/keys/my_public_key.txt gen/keys/my_secret_key.txt 10 1"
#
## run FHE dot product over two encrypted vectors
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.DotProductExample -Dexec.args="local 4 HELIB CKKS gen/keys/my_public_key.txt gen/keys/my_secret_key.txt"
#
## run FHE total sum over encrypted vector elements
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.TotalSumExample -Dexec.args="local 4 HELIB CKKS gen/keys/my_public_key.txt gen/keys/my_secret_key.txt"





### SEAL tests ##
#

### SEAL tests ##
## BFV Scheme
## generate example key pairs
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.KeyGenExample -Dexec.args="local SEAL BFV"
##
### generate example ciphertexts
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.EncDecExample -Dexec.args="local SEAL BFV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt 10"
###
## run basic FHE arithmetic operation over encrypted data
##./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.BasicOPsExample -Dexec.args="local 8 SEAL BFV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt"
###
### run FHE dot product over two encrypted vectors
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.DotProductExample -Dexec.args="local 8 SEAL BFV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt"
#
## run FHE total sum over encrypted vector elements
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.basic.TotalSumExample -Dexec.args="local 8 SEAL BFV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt"
##
#
#
## BFV Scheme
# generate example key pairs
./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.KeyGenExample -Dexec.args="local SEAL BFV"

# generate example ciphertexts
./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.EncDecExample -Dexec.args="local SEAL BFV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt 10 1"

# run basic FHE arithmetic operation over encrypted data
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.BasicOPsExample -Dexec.args="local 8 SEAL BFV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt 10 1"

# run FHE dot product over two encrypted vectors
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.DotProductExample -Dexec.args="local 8 SEAL BFV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt"

# run FHE total sum over encrypted vector elements
./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.TotalSumExample -Dexec.args="local 8 SEAL BFV gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt"


## CKKS Scheme
## generate example key pairs
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.KeyGenExample -Dexec.args="local SEAL CKKS"
####
##### generate example ciphertexts
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.EncDecExample -Dexec.args="local SEAL CKKS gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt 100000000 1"
####
##### run basic FHE arithmetic operation over encrypted data
##./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.BasicOPsExample -Dexec.args="local 4 SEAL CKKS gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt 10 1"
###
#### run FHE dot product over two encrypted vectors
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.DotProductExample -Dexec.args="local 4 SEAL CKKS gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt"
###
### run FHE total sum over encrypted vector elements
#./mvn -f pom-devel.xml exec:java -Dexec.mainClass=spiritlab.sparkfhe.example.packing.TotalSumExample -Dexec.args="local 4 SEAL CKKS gen/keys/my_public_key.txt gen/keys/my_secret_key.txt gen/keys/my_relin_keys.txt gen/keys/my_galois_keys.txt"
