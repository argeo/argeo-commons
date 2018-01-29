#!/bin/sh

# COMPLETELY UNSAFE - FOR DEVELOPMENT ONLY
# Run this script from its directory
# all *.p12 passwords are 'demo'
# all *.jks passwords are 'changeit'

INTERMEDIATE_CA_DN="/C=DE/O=Example/OU=Certificate Authorities/CN=Intermediate CA/"
SERVER_DN=/C=DE/O=Example/OU=Systems/CN=$HOSTNAME/
USERS_BASE_DN=/DC=com/DC=example/OU=People

echo ## Init directory structure
# Root
export OPENSSL_CONF=./openssl_root.cnf
export CATOP=./rootCA
/etc/pki/tls/misc/CA -newca
# Intermediate
mkdir -p ./CA/{certs,crl,csr,newcerts,private}

echo ## Create intermediate certificate
openssl req -new -newkey rsa:4096 -extensions v3_intermediate_ca \
 -subj "$INTERMEDIATE_CA_DN" \
 -keyout ./CA/private/cakey.pem -passout pass:demo -out ica_csr.pem
openssl ca -batch -passin pass:demo -in ica_csr.pem -out ./CA/cacert.pem

# create index and serial
touch ./CA/index.txt
# (below is from openssl CA script)
openssl x509 -in ./CA/cacert.pem -noout -next_serial -out ./CA/serial

# Switch to intermediate CA		      
export OPENSSL_CONF=./openssl.cnf
export CATOP=./CA

echo ## Create server key and certificate
openssl req -new -newkey rsa:4096 -extensions server_ext \
 -subj $SERVER_DN \
 -keyout node_key.pem -passout pass:demo -out node_csr.pem
openssl ca -batch -passin pass:demo -in node_csr.pem -out node_crt.pem
cat node_crt.pem ./CA/cacert.pem ./rootCA/cacert.pem > chain.pem
openssl pkcs12 -export -passin pass:demo -passout pass:changeit \
 -name "$HOSTNAME" -inkey node_key.pem -in chain.pem \
 -out node.p12

echo ## Import Certificate Authority into keystore
keytool -importcert -noprompt -keystore node.p12 -storepass changeit \
 -alias "rootCA" -file ./rootCA/cacert.pem
keytool -importcert -noprompt -keystore node.p12 -storepass changeit \
 -alias "CA" -file ./CA/cacert.pem
cp node.p12 ../init/node/

echo ## Create 'root' user client certificate
openssl req -new -newkey rsa:4096 -extensions user_ext \
 -subj $USERS_BASE_DN/UID=root/ \
 -keyout newkey.pem -passout pass:demo -out newcsr.pem
openssl ca -preserveDN -batch -passin pass:demo -in newcsr.pem -out newcrt.pem
cat newcrt.pem ./CA/cacert.pem ./rootCA/cacert.pem > newchain.pem
openssl pkcs12 -export -passin pass:demo -passout pass:demo \
 -name "root" -inkey newkey.pem -in newchain.pem \
 -out root.p12

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
