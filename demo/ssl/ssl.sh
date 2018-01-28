#!/bin/sh

# COMPLETELY UNSAFE - FOR DEVELOPMENT ONLY
# Run this script from its directory
# all *.p12 passwords are 'demo'
# all *.jks passwords are 'changeit'

SERVER_DN=/C=DE/O=Example/OU=Systems/CN=$HOSTNAME/
USERS_BASE_DN=/DC=com/DC=example/OU=users

export OPENSSL_CONF=./openssl.cnf
export CATOP=./CA

/etc/pki/tls/misc/CA -newca

#openssl req -x509 -new -newkey rsa:4096 -extensions server_ext -days 365 \
# -subj $SERVER_DN \
# -keyout newkey.pem -passout pass:demo -out newcrt.pem
 
# Self-signed server certificate
#openssl pkcs12 -export -passin pass:demo -passout pass:changeit \
# -name "jetty" -inkey newkey.pem -in newcrt.pem \
# -certfile ./CA/cacert.pem \
# -out server.p12
 
 # Convert PKCS12 keystore into a JKS keystore
#keytool -importkeystore \
# -srckeystore server.p12 -srcstoretype pkcs12 -srcstorepass changeit \
# -alias jetty  -destkeystore server.jks -deststorepass changeit
#rm -f server.p12

# Import People CA
#keytool -importcert -keystore server.jks -storepass changeit \
# -alias CA -file CA/cacert.pem

openssl req -new -newkey rsa:4096 -extensions server_ext -days 365 \
 -subj $SERVER_DN \
 -keyout node_key.pem -passout pass:demo -out node_csr.pem
openssl ca -batch -passin pass:demo -in node_csr.pem -out node_crt.pem
cat node_crt.pem CA/cacert.pem > node.pem
openssl pkcs12 -export -passin pass:demo -passout pass:demo \
 -name "node" -inkey node_key.pem -in node.pem \
 -out node.p12


# root user
openssl req -new -newkey rsa:4096 -extensions user_ext -days 365 \
 -subj $USERS_BASE_DN/UID=root/ \
 -keyout newkey.pem -passout pass:demo -out newcsr.pem
openssl ca -preserveDN -batch -passin pass:demo -in newcsr.pem -out newcrt.pem
openssl pkcs12 -export -passin pass:demo -passout pass:demo \
 -name "root" -inkey newkey.pem -in newcrt.pem \
 -out root.p12

# demo user
#openssl req -new -newkey rsa:4096 -extensions user_ext -days 365 \
# -subj $USERS_BASE_DN/UID=demo/ \
# -keyout newkey.pem -passout pass:demo -out newcsr.pem
#openssl ca -preserveDN -batch -passin pass:demo -in newcsr.pem -out newcrt.pem
#openssl pkcs12 -export -passin pass:demo -passout pass:demo \
# -name "demo" -inkey newkey.pem -in newcrt.pem \
# -out demo.p12

# Clean up
#rm -vf new*.pem
