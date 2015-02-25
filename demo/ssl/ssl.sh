#!/bin/sh

# COMPLETELY UNSAFE - FOR DEVELOPMENT ONLY
# Run this script from its directory

export OPENSSL_CONF=./openssl.cnf
export CATOP=./CA

/etc/pki/tls/misc/CA -newca

openssl req -x509 -new -newkey rsa:1024 -extensions server_ext -days 3650 \
 -subj /C=DE/ST=Berlin/O=Example/OU=Systems/CN=localhost/ \
 -keyout newkey.pem -passout pass:demo -out newcrt.pem
 
openssl pkcs12 -export -passin pass:demo -passout pass:changeit \
 -name "jetty" -inkey newkey.pem -in newcrt.pem \
 -out server.p12
 
 # Convert PKCS12 keystore into a JKS keystore
keytool -importkeystore \
 -srckeystore server.p12 -srcstoretype pkcs12 -srcstorepass changeit \
 -alias jetty  -destkeystore server.jks -deststorepass changeit
rm -f server.p12

# Import People CA
keytool -importcert -keystore server.jks -storepass changeit \
 -alias CA -file CA/cacert.pem

# root user
openssl req -new -newkey rsa:1024 -extensions server_ext -days 3650 \
 -subj /C=DE/ST=Berlin/O=Example/OU=People/CN=root/ \
 -keyout newkey.pem -passout pass:demo -out newcsr.pem
openssl ca -batch -passin pass:demo -in newcsr.pem -out newcrt.pem
openssl pkcs12 -export -passin pass:demo -passout pass:demo \
 -name "root" -inkey newkey.pem -in newcrt.pem \
 -out root.p12

# demo user
openssl req -new -newkey rsa:1024 -extensions server_ext -days 3650 \
 -subj /C=DE/ST=Berlin/O=Example/OU=People/CN=demo/ \
 -keyout newkey.pem -passout pass:demo -out newcsr.pem
openssl ca -batch -passin pass:demo -in newcsr.pem -out newcrt.pem
openssl pkcs12 -export -passin pass:demo -passout pass:demo \
 -name "demo" -inkey newkey.pem -in newcrt.pem \
 -out demo.p12

# Clean up
rm -vf new*.pem
#rm -vf root.csr root.key root.crt
#rm -vf server.p12 server.crt server.key
