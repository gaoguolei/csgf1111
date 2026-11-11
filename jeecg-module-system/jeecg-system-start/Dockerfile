FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER AllThinker

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

#RUN mkdir -p /jeecg-boot/config/jeecg/

WORKDIR /opt

EXPOSE 8080

#ADD ./src/main/resources/jeecg ./config/jeecg
ADD ./target/jeecg-system-start-3.4.4.jar /opt/app.jar

CMD sleep 3;java -Djava.security.egd=file:/dev/./urandom -jar /opt/app.jar