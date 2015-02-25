#!/bin/sh

export OPENSSL_CONF=./openssl.cnf
export CATOP=./CA

/etc/pki/tls/misc/CA -newca

openssl req -x509 -new -newkey rsa:1024 -extensions server_ext -days 3650 \
 -subj /C=DE/ST=Berlin/O=Example/OU=Systems/CN=localhost/ \
 -keyout server.key -passout pass:demo -out server.crt
 
openssl pkcs12 -export -passin pass:demo -passout pass:changeit \
 -name "jetty" -inkey server.key -in server.crt \
 -out server.p12
 
 # Convert PKCS12 keystore into a JKS keystore
keytool -importkeystore \
 -srckeystore server.p12 -srcstoretype pkcs12 -srcstorepass changeit \
 -alias jetty  -destkeystore server.jks -deststorepass changeit

# Import People CA
keytool -importcert -keystore server.jks -storepass changeit \
 -alias CA -file CA/cacert.pem

openssl req -new -newkey rsa:1024 -extensions server_ext -days 3650 \
 -subj /C=DE/ST=Berlin/O=Example/OU=People/CN=root/ \
 -keyout root.key -passout pass:demo -out root.csr
openssl ca -batch -passin pass:demo -in root.csr -out root.crt
openssl pkcs12 -export -passin pass:demo -passout pass:demo \
 -name "root" -inkey root.key -in root.crt \
 -out root.p12

# Clean
rm -vf new*.pem
rm -vf root.csr root.key root.crt
rm -vf server.p12 server.crt server.key
