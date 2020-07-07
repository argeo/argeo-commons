#!/bin/sh

# COMPLETELY UNSAFE - FOR DEVELOPMENT ONLY
# Run this script from its directory
# all *.p12 passwords are 'demo'
# all *.jks passwords are 'changeit'

# Fail if any error
set -e

ROOT_CA_DN="/C=DE/O=Example/OU=Certificate Authorities/CN=Root CA/"
INTERMEDIATE_CA_DN="/C=DE/O=Example/OU=Certificate Authorities/CN=Intermediate CA/"
SERVER_DN=/C=DE/O=Example/OU=Systems/CN=$HOSTNAME/
USERS_BASE_DN=/DC=com/DC=example/OU=People

echo -- Init directory structures
mkdir -p ./rootCA/{certs,crl,csr,newcerts,private}
mkdir -p ./CA/{certs,crl,csr,newcerts,private}

#
# Root CA
#
export OPENSSL_CONF=./openssl_root.cnf
export CATOP=./rootCA
echo -- Create root CA in $CATOP
touch $CATOP/index.txt
openssl req -new -newkey rsa:4096 -extensions v3_ca \
 -subj "$ROOT_CA_DN" \
 -keyout $CATOP/private/cakey.pem -passout pass:demo -out ca_csr.pem \
 2>/dev/null # quiet
openssl ca -create_serial -selfsign -batch -passin pass:demo -in ca_csr.pem -out $CATOP/cacert.pem \
 2>/dev/null # quiet

echo -- Create intermediate CA in ./CA
openssl req -new -newkey rsa:4096 -extensions v3_intermediate_ca \
 -subj "$INTERMEDIATE_CA_DN" \
 -keyout ./CA/private/cakey.pem -passout pass:demo -out ica_csr.pem \
 2>/dev/null # quiet
openssl ca -batch -passin pass:demo -in ica_csr.pem -out ./CA/cacert.pem \
 2>/dev/null # quiet

#
# Intermediate CA
#      
export OPENSSL_CONF=./openssl.cnf
export CATOP=./CA

# create index and serial
touch $CATOP/index.txt
openssl x509 -in $CATOP/cacert.pem -noout -next_serial -out $CATOP/serial \
 2>/dev/null # quiet

echo -- Create server key and certificate
openssl req -new -newkey rsa:4096 -extensions server_ext \
 -subj $SERVER_DN \
 -keyout node_key.pem -passout pass:demo -out node_csr.pem \
 2>/dev/null # quiet
openssl ca -batch -passin pass:demo -in node_csr.pem -out node_crt.pem \
 2>/dev/null # quiet

# create CA chain
cat node_crt.pem ./CA/cacert.pem ./rootCA/cacert.pem > chain.pem

# convert to p12
openssl pkcs12 -export -passin pass:demo -passout pass:changeit \
 -name "$HOSTNAME" -inkey node_key.pem -in chain.pem \
 -out node.p12 \
 2>/dev/null # quiet

echo -- Import Certificate Authority into keystore
keytool -importcert -noprompt -keystore node.p12 -storepass changeit \
 -alias "rootCA" -file ./rootCA/cacert.pem
keytool -importcert -noprompt -keystore node.p12 -storepass changeit \
 -alias "CA" -file ./CA/cacert.pem

echo -- Copy node.p12 to ../init/node
cp node.p12 ../init/node/

echo -- Create 'root' user client certificate root.p12
openssl req -new -newkey rsa:4096 -extensions user_ext \
 -subj $USERS_BASE_DN/UID=root/ \
 -keyout newkey.pem -passout pass:demo -out newcsr.pem \
 2>/dev/null # quiet

openssl ca -preserveDN -batch -passin pass:demo -in newcsr.pem -out newcrt.pem \
 2>/dev/null # quiet

# create new CA chain
#cat newcrt.pem ./CA/cacert.pem ./rootCA/cacert.pem > newchain.pem
openssl pkcs12 -export -passin pass:demo -passout pass:demo \
 -name "root" -inkey newkey.pem -in chain.pem \
 -out root.p12 \
 2>/dev/null # quiet

# demo user
#openssl req -new -newkey rsa:4096 -extensions user_ext -days 365 \
# -subj $USERS_BASE_DN/UID=demo/ \
# -keyout newkey.pem -passout pass:demo -out newcsr.pem
#openssl ca -preserveDN -batch -passin pass:demo -in newcsr.pem -out newcrt.pem
#openssl pkcs12 -export -passin pass:demo -passout pass:demo \
# -name "demo" -inkey newkey.pem -in newcrt.pem \
# -out demo.p12

# Self-signed
#openssl req -x509 -new -newkey rsa:4096 -extensions server_ext -days 365 \
# -subj $SERVER_DN \
# -keyout newkey.pem -passout pass:demo -out newcrt.pem
# Self-signed server certificate
#openssl pkcs12 -export -passin pass:demo -passout pass:changeit \
# -name "jetty" -inkey newkey.pem -in newcrt.pem \
# -certfile ./CA/cacert.pem \
# -out server.p12

echo ## Clean up
rm -vf *.pem
