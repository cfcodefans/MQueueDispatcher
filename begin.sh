#/usr/java/jdk1.8.0_05/bin/java \
java \
-Xms512m \
-Xmx1024m \
-XX:PermSize=64m \
-XX:+UseBiasedLocking \
-XX:+UseParallelGC \
-XX:OnOutOfMemoryError='echo "fan panic" | mail -s "Possible OOM on $(hostname -s)" fan@thenetcircle.com '\
-Xdebug \
-Xrunjdwp:transport=dt_socket,address=7777,server=y,suspend=n \
-jar start.jar \
-Djetty.port=8888 \
-Dexternal_jpa_properties=data_source.properties \
-Djgroup_settings= \
STOP.PORT=8890 \
STOP.KEY=secret \
-Dcom.sun.management.jmxremote \
OPTIONS=Server,jmx etc/jetty-jmx-remote.xml \
-Dchannel.number.connection=20 \
-Dhttpclient.number=2 \
-Drespond.number=10 \
-Dhttp.client.max.connection=150 \
-Dhttp.client.max.connection.per_route=150

